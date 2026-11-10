package com.roomrecovery.recovery.offer

import com.roomrecovery.availability.RoomUnavailabilityRepository
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
    private val unavailabilities: RoomUnavailabilityRepository,
    private val transaction: TransactionTemplate
) {

    private val sessions = mutableMapOf<Long, RecoverySession>()

    @Synchronized
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

    @Synchronized
    fun get(accommodationId: Long, sessionId: String): RecoverySessionView {
        return view(session(accommodationId, sessionId))
    }

    @Synchronized
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

        val conflictingOutages = transaction.execute {
            val reservation = reservations.findById(offer.reservationId).orElseThrow { NotFound("예약을 찾을 수 없습니다") }
            if (reservation.accommodationId != accommodationId) {
                throw NotFound("예약을 찾을 수 없습니다")
            }
            if (reservation.status == ReservationStatus.CANCELLED) {
                throw RecoveryConflict("이미 취소된 예약입니다")
            }

            if (decision == RecoveryDecision.ACCEPTED) {
                val currentOutages = unavailabilities.findRemainingInAccommodation(accommodationId, offer.referenceDate)
                if (offer.segments.any { segment ->
                        currentOutages.any { it.overlaps(segment.roomId, segment.startDate, segment.endDate) }
                    }) {
                    return@execute currentOutages
                }
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
            null
        }
        if (conflictingOutages != null) {
            val refreshed = session.coordinator.copy()
            refreshed.replanForIncident(offer.referenceDate, conflictingOutages, emptySet())
            session.coordinator = refreshed
            throw RecoveryConflict("객실 이용 불가 정보가 변경되었습니다. 복구 계획을 다시 불러와 주세요.")
        }
        session.coordinator = candidate

        return view(session)
    }

    @Synchronized
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

    @Synchronized
    fun finish(accommodationId: Long, sessionId: String) {
        val coordinator = session(accommodationId, sessionId).coordinator
        coordinator.finishBlockedReason()?.let { throw RecoveryConflict(it) }
        sessions.remove(accommodationId)
    }

    private fun session(accommodationId: Long, sessionId: String): RecoverySession {
        return sessions[accommodationId]?.takeIf { it.id == sessionId }
            ?: throw NotFound("복구 세션을 찾을 수 없습니다")
    }

    private fun view(session: RecoverySession): RecoverySessionView {
        return RecoverySessionView(
            session.id, session.accommodationId, session.date, session.coordinator.plan,
            session.coordinator.offers, session.coordinator.moveHistory, session.coordinator.finishBlockedReason()
        )
    }
}
