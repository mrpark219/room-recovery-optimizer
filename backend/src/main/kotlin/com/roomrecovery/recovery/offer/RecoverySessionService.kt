package com.roomrecovery.recovery.offer

import com.roomrecovery.availability.RoomUnavailabilityRepository
import com.roomrecovery.common.NotFound
import com.roomrecovery.recovery.RecoveryInput
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
    private val transaction: TransactionTemplate,
    private val history: RecoveryHistory
) {

    private val sessions = mutableMapOf<Long, RecoverySession>()

    @Synchronized
    fun create(accommodationId: Long, date: LocalDate): RecoverySessionView {
        if (sessions.containsKey(accommodationId)) {
            throw RecoveryConflict("이미 진행 중인 복구 세션이 있습니다")
        }

        val input = recoveryService.loadInput(accommodationId, date)
        val coordinator = RecoveryCoordinator(input).copy()
        coordinator.replan()
        val roomNames = input.rooms.associate { it.id to it.name }
        val incidents = input.unavailabilities.map {
            RecoveryIncident(roomNames[it.roomId] ?: "객실 ${it.roomId}", it.startDate, it.endDate, it.reason)
        }
        val session = RecoverySession(UUID.randomUUID().toString(), accommodationId, date, coordinator, roomNames, incidents)
        sessions[accommodationId] = session

        return view(session)
    }

    @Synchronized
    fun get(accommodationId: Long, sessionId: String): RecoverySessionView {
        val session = session(accommodationId, sessionId)
        return view(session, recoveryService.loadInput(accommodationId, session.date))
    }

    @Synchronized
    fun decide(
        accommodationId: Long,
        sessionId: String,
        offerId: Long,
        decision: RecoveryDecision,
        roomId: Long? = null
    ): RecoverySessionView {
        val session = session(accommodationId, sessionId)
        val offer = session.coordinator.offers.singleOrNull { it.id == offerId }
            ?: throw NotFound("유효한 제안을 찾을 수 없습니다")
        val candidate = session.coordinator.copy()

        if (roomId != null && decision != RecoveryDecision.ACCEPTED) {
            throw RecoveryConflict("객실 선택은 제안을 수락할 때만 할 수 있습니다.")
        }

        val conflictingOutages = transaction.execute {
            val reservation = reservations.findById(offer.reservationId).orElseThrow { NotFound("예약을 찾을 수 없습니다") }
            if (reservation.accommodationId != accommodationId) {
                throw NotFound("예약을 찾을 수 없습니다")
            }
            if (reservation.status == ReservationStatus.CANCELLED) {
                throw RecoveryConflict("이미 취소된 예약입니다")
            }

            try {
                if (roomId != null) {
                    candidate.selectRoom(offerId, roomId, recoveryService.loadInput(accommodationId, offer.referenceDate))
                }
                candidate.respond(offerId, decision)
            } catch (e: IllegalStateException) {
                throw RecoveryConflict(e.message ?: "제안에 응답할 수 없습니다")
            }

            if (decision == RecoveryDecision.ACCEPTED && roomId == null) {
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
            val previousSchedule = old.map {
                schedule(session, it.roomId, it.startDate, it.endDate)
            }
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
            history.record(session, decision.name, mapOf(
                "reservationId" to reservation.id,
                "previousSchedule" to previousSchedule,
                "confirmedSchedule" to newSegments.map { schedule(session, it.roomId, it.startDate, it.endDate) },
                "incidents" to session.incidents
            ))
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

        val candidate = session.coordinator.copy()
        val completed = try {
            candidate.completeMove(offerId, request.date, request.fromRoomId, request.toRoomId)
        } catch (e: IllegalStateException) {
            throw RecoveryConflict(e.message ?: "이동을 기록할 수 없습니다")
        } catch (e: IllegalArgumentException) {
            throw RecoveryConflict(e.message ?: "이동을 기록할 수 없습니다")
        }

        history.record(session, "MOVE_COMPLETED", mapOf(
            "reservationId" to completed.reservationId,
            "date" to completed.date,
            "fromRoomName" to (session.roomNames[completed.fromRoomId] ?: "객실 ${completed.fromRoomId}"),
            "toRoomName" to (session.roomNames[completed.toRoomId] ?: "객실 ${completed.toRoomId}")
        ))
        session.coordinator = candidate
        return view(session)
    }

    @Synchronized
    fun finish(accommodationId: Long, sessionId: String) {
        val session = session(accommodationId, sessionId)
        val coordinator = session.coordinator
        coordinator.finishBlockedReason()?.let { throw RecoveryConflict(it) }
        history.record(session, "FINISHED", mapOf(
            "unresolvedReservationIds" to coordinator.plan?.unresolvedReservationIds.orEmpty(),
            "incidents" to session.incidents
        ))
        sessions.remove(accommodationId)
    }

    private fun schedule(session: RecoverySession, roomId: Long, startDate: LocalDate, endDate: LocalDate): Map<String, Any> {
        return mapOf(
            "roomName" to (session.roomNames[roomId] ?: "객실 $roomId"),
            "startDate" to startDate,
            "endDate" to endDate
        )
    }

    private fun session(accommodationId: Long, sessionId: String): RecoverySession {
        return sessions[accommodationId]?.takeIf { it.id == sessionId }
            ?: throw NotFound("복구 세션을 찾을 수 없습니다")
    }

    private fun view(session: RecoverySession, availability: RecoveryInput? = null): RecoverySessionView {
        val alternatives = availability?.let(session.coordinator::roomAlternatives)
            ?: session.coordinator.roomAlternatives()
        return RecoverySessionView(
            session.id, session.accommodationId, session.date, session.coordinator.plan,
            session.coordinator.offers, session.coordinator.moveHistory, session.coordinator.finishBlockedReason(),
            alternatives
        )
    }
}
