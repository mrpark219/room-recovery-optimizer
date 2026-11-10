package com.roomrecovery.recovery.offer

import com.roomrecovery.availability.RoomUnavailability
import com.roomrecovery.recovery.*
import com.roomrecovery.reservation.Reservation
import com.roomrecovery.reservation.ReservationStatus
import com.roomrecovery.reservation.RoomAssignment
import java.time.LocalDate

/**
 * 복구 계산은 최적화기에 위임하고, 제안과 응답 및 실제 이동 이력을 관리한다.
 * 배정의 종료일은 숙박에 포함하지 않는다.
 */
class RecoveryCoordinator(private var input: RecoveryInput) {
    private var nextOfferId = 1L
    private val currentOffers = mutableMapOf<Long, RecoveryOffer>()
    private val acceptedIds = input.fixedReservationIds.toMutableSet()
    private val history = mutableListOf<RoomMoveHistory>()
    private var currentAssignments = input.assignments.toList()

    /** 데이터베이스 커밋 전에는 원본 세션을 변경하지 않는다. */
    fun copy(): RecoveryCoordinator {
        val reservations = input.reservations.map { r ->
            Reservation(
                r.id, r.accommodationId, r.bookedRoomTypeId, r.bookedAt,
                r.guestCount, r.startDate, r.endDate, r.status
            )
        }
        return RecoveryCoordinator(input.copy(reservations = reservations)).also { copy ->
            copy.nextOfferId = nextOfferId
            currentOffers.forEach { (id, offer) ->
                copy.currentOffers[id] = RecoveryOffer(
                    offer.id, offer.reservationId, offer.referenceDate, offer.initialRoomId, offer.segments
                ).also { it.decision = offer.decision }
            }
            copy.acceptedIds.clear()
            copy.acceptedIds.addAll(acceptedIds)
            copy.history.addAll(history)
            copy.currentAssignments = currentAssignments.toList()
            copy.plan = plan
        }
    }

    val assignments: List<RoomAssignment> get() = currentAssignments.toList()
    val moveHistory: List<RoomMoveHistory> get() = history.toList()
    val offers: List<RecoveryOffer> get() = currentOffers.values.toList()
    var plan: RecoveryPlan? = null
        private set

    /** 미응답 제안을 교체하고 수락된 배정은 유지한다. */
    fun replan(referenceDate: LocalDate = input.referenceDate): RecoveryPlan {
        require(referenceDate >= input.referenceDate) { "복구 기준일은 이전으로 돌아갈 수 없습니다" }

        val result = RecoveryOptimizer.optimize(
            input.copy(
                referenceDate = referenceDate,
                assignments = currentAssignments,
                fixedReservationIds = acceptedIds.toSet()
            )
        )
        input = input.copy(referenceDate = referenceDate)

        refreshPendingOffers(result.placements, referenceDate)
        plan = result

        return result
    }

    private fun refreshPendingOffers(placements: List<RecoveryPlacement>, date: LocalDate) {
        val previousOffers = currentOffers.filterValues { it.decision == null }
        currentOffers.entries.removeIf { it.value.decision == null }

        for (placement in placements) {
            val initialRoom = initialRoom(placement.reservationId, date)
            val previous = previousOffers[placement.reservationId]
            val unchanged = previous != null && previous.referenceDate == date &&
                    previous.initialRoomId == initialRoom && previous.segments == placement.segments
            currentOffers[placement.reservationId] = if (unchanged) {
                previous
            } else {
                RecoveryOffer(nextOfferId++, placement.reservationId, date, initialRoom, placement.segments)
            }
        }
    }

    private fun initialRoom(reservationId: Long, date: LocalDate): Long? {
        val reservation = input.reservations.single { it.id == reservationId }
        if (reservation.status != ReservationStatus.CHECKED_IN) {
            return null
        }

        return currentAssignments.singleOrNull { it.reservationId == reservationId && it.contains(date) }?.roomId
    }

    /** 후속 장애에서 과거 배정과 완료된 이동을 보존한다. */
    fun replanForIncident(
        referenceDate: LocalDate,
        unavailabilities: List<RoomUnavailability>,
        affectedReservationIds: Set<Long>
    ): RecoveryPlan {
        require(referenceDate >= input.referenceDate) { "복구 기준일은 이전으로 돌아갈 수 없습니다" }
        acceptedIds.removeAll(affectedReservationIds)

        affectedReservationIds.forEach { id ->
            if (currentOffers[id]?.decision != null) {
                currentOffers.remove(id)
            }
        }

        input = input.copy(referenceDate = referenceDate, unavailabilities = unavailabilities.toList())

        return replan(referenceDate)
    }

    /** 현재 유효한 전체 제안에 대해서만 응답한다. */
    fun respond(offerId: Long, decision: RecoveryDecision): RecoveryPlan? {
        val offer = findOffer(offerId)
        check(offer.decision == null) { "이미 응답한 제안입니다" }

        val reservation = input.reservations.single { it.id == offer.reservationId }
        check(reservation.status != ReservationStatus.CANCELLED) { "이미 취소된 예약입니다" }

        return when (decision) {
            RecoveryDecision.ACCEPTED -> {
                replaceFuture(offer.reservationId, offer.referenceDate, offer.segments)
                acceptedIds.add(offer.reservationId)
                offer.decision = decision
                null
            }

            RecoveryDecision.CANCEL_REQUESTED -> {
                reservation.cancelForRecovery()
                replaceFuture(offer.reservationId, offer.referenceDate, emptyList())
                offer.decision = decision
                replan(offer.referenceDate)
            }
        }
    }

    /** 실제 이동 완료만 기록한다. */
    fun completeMove(offerId: Long, date: LocalDate, fromRoomId: Long, toRoomId: Long): RoomMoveHistory {
        val offer = findOffer(offerId)
        val move = PlannedMove(date, fromRoomId, toRoomId)
        validateMove(offer, move)

        return RoomMoveHistory(offer.reservationId, date, fromRoomId, toRoomId).also(history::add)
    }

    private fun findOffer(id: Long): RecoveryOffer {
        return currentOffers.values.singleOrNull { it.id == id }
            ?: throw IllegalArgumentException("유효한 제안을 찾을 수 없습니다")
    }

    private fun validateMove(offer: RecoveryOffer, move: PlannedMove) {
        check(offer.decision == RecoveryDecision.ACCEPTED) { "수락된 제안이 아닙니다" }
        require(move.fromRoomId != move.toRoomId && move in offer.plannedMoves) { "수락된 계획에 없는 이동입니다" }

        val previous = history.filter { it.reservationId == offer.reservationId }
        require(previous.none { it.matches(move) }) { "이미 기록된 이동입니다" }
        require(previous.lastOrNull()?.date?.let { move.date > it } != false) { "이동은 날짜 순서대로 기록해야 합니다" }
        require(offer.plannedMoves.takeWhile { it != move }.all { earlier -> previous.any { it.matches(earlier) } }) {
            "이전 이동부터 기록해야 합니다"
        }
    }

    private fun RoomMoveHistory.matches(move: PlannedMove): Boolean {
        return date == move.date && fromRoomId == move.fromRoomId && toRoomId == move.toRoomId
    }

    private fun replaceFuture(reservationId: Long, date: LocalDate, segments: List<RecoverySegment>) {
        val reservation = input.reservations.single { it.id == reservationId }
        val retained = currentAssignments.mapNotNull { assignment ->
            if (assignment.reservationId != reservationId || assignment.endDate <= date) {
                assignment
            } else if (assignment.startDate < date) {
                RoomAssignment(
                    assignment.id, assignment.accommodationId,
                    assignment.reservationId, assignment.roomId, assignment.startDate, date
                )
            } else {
                null
            }
        }

        currentAssignments = retained + segments.map { segment ->
            RoomAssignment(
                0L,
                reservation.accommodationId,
                reservationId,
                segment.roomId,
                segment.startDate,
                segment.endDate
            )
        }
    }
}
