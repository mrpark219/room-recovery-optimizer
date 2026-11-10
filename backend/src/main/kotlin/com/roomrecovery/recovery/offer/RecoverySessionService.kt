package com.roomrecovery.recovery.offer

import com.roomrecovery.common.NotFound
import com.roomrecovery.recovery.RecoveryService
import com.roomrecovery.reservation.ReservationRepository
import com.roomrecovery.reservation.ReservationStatus
import com.roomrecovery.reservation.RoomAssignmentRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate
import java.time.LocalDate
import java.util.*

@Service
class RecoverySessionService(
    private val recoveryService: RecoveryService,
    private val reservations: ReservationRepository,
    private val assignments: RoomAssignmentRepository,
    private val transaction: TransactionTemplate
) {

    private val sessions = mutableMapOf<Long, RecoverySession>()

    fun create(accommodationId: Long, date: LocalDate): RecoverySessionView {
        if (sessions.containsKey(accommodationId)) {
            throw RecoveryConflict("이미 진행 중인 복구 세션이 있습니다")
        }

        val coordinator = RecoveryCoordinator(recoveryService.loadInput(accommodationId, date)).copy()
        coordinator.replan()
        val session = RecoverySession(UUID.randomUUID().toString(), accommodationId, date, coordinator)
        sessions[accommodationId] = session

        return view(session)
    }

    fun get(accommodationId: Long, sessionId: String): RecoverySessionView {
        return view(session(accommodationId, sessionId))
    }

    fun decide(
        accommodationId: Long,
        sessionId: String,
        offerId: Long,
        decision: RecoveryDecision
    ): RecoverySessionView {
        val session = session(accommodationId, sessionId)
        val offer = session.coordinator.offers.singleOrNull { it.id == offerId }
            ?: throw NotFound("유효한 제안을 찾을 수 없습니다")
        val candidate = session.coordinator.copy()

        try {
            candidate.respond(offerId, decision)
        } catch (e: IllegalStateException) {
            throw RecoveryConflict(e.message ?: "제안에 응답할 수 없습니다")
        }

        transaction.executeWithoutResult {
            val reservation = reservations.findById(offer.reservationId).orElseThrow { NotFound("예약을 찾을 수 없습니다") }
            if (reservation.accommodationId != accommodationId) {
                throw NotFound("예약을 찾을 수 없습니다")
            }
            if (reservation.status == ReservationStatus.CANCELLED) {
                throw RecoveryConflict("이미 취소된 예약입니다")
            }
            if (decision == RecoveryDecision.CANCEL_REQUESTED) {
                reservation.cancelForRecovery()
            }

            val old = assignments.findAllByAccommodationIdAndReservationIdAndEndDateAfter(
                accommodationId, offer.reservationId, offer.referenceDate
            )
            old.forEach { assignment ->
                if (assignment.startDate < offer.referenceDate) {
                    assignment.shorten(offer.referenceDate)
                } else {
                    assignments.delete(assignment)
                }
            }
            assignments.flush()

            val newSegments = candidate.assignments.filter { it.reservationId == offer.reservationId && it.id == 0L }
            assignments.saveAllAndFlush(newSegments)
        }
        session.coordinator = candidate

        return view(session)
    }

    fun move(accommodationId: Long, sessionId: String, offerId: Long, request: MoveRequest): RecoverySessionView {
        val session = session(accommodationId, sessionId)
        if (session.coordinator.offers.none { it.id == offerId }) {
            throw NotFound("유효한 제안을 찾을 수 없습니다")
        }

        try {
            session.coordinator.completeMove(offerId, request.date, request.fromRoomId, request.toRoomId)
        } catch (e: IllegalStateException) {
            throw RecoveryConflict(e.message ?: "이동을 기록할 수 없습니다")
        } catch (e: IllegalArgumentException) {
            throw RecoveryConflict(e.message ?: "이동을 기록할 수 없습니다")
        }

        return view(session)
    }

    private fun session(accommodationId: Long, sessionId: String): RecoverySession {
        return sessions[accommodationId]?.takeIf { it.id == sessionId }
            ?: throw NotFound("복구 세션을 찾을 수 없습니다")
    }

    private fun view(session: RecoverySession): RecoverySessionView {
        return RecoverySessionView(
            session.id, session.accommodationId, session.date, session.coordinator.plan,
            session.coordinator.offers, session.coordinator.moveHistory
        )
    }
}
