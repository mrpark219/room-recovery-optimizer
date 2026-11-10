package com.roomrecovery.recovery.offer

import com.roomrecovery.availability.RoomUnavailability
import com.roomrecovery.recovery.RecoveryInput
import com.roomrecovery.recovery.RecoveryOptimizer
import com.roomrecovery.recovery.RecoveryPlan
import com.roomrecovery.recovery.RecoverySegment
import com.roomrecovery.reservation.ReservationStatus
import com.roomrecovery.reservation.RoomAssignment
import java.time.LocalDate

/** 단일 실행 환경의 복구 상태. 종료일은 숙박에 포함하지 않는다. */
class RecoveryCoordinator(private var input: RecoveryInput) {
    private var nextOfferId = 1L
    private val currentOffers = mutableMapOf<Long, RecoveryOffer>()
    private val acceptedIds = input.fixedReservationIds.toMutableSet()
    private val history = mutableListOf<RoomMoveHistory>()
    private var currentAssignments = input.assignments.toList()

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

        val previousOffers = currentOffers.filterValues { it.decision == null }
        currentOffers.entries.removeIf { it.value.decision == null }

        for (placement in result.placements) {
            val reservation = input.reservations.single { it.id == placement.reservationId }
            val initialRoom = if (reservation.status == ReservationStatus.CHECKED_IN) {
                currentAssignments.singleOrNull { it.reservationId == reservation.id && it.contains(referenceDate) }?.roomId
            } else null
            val previous = previousOffers[reservation.id]
            currentOffers[reservation.id] = if (previous != null && previous.referenceDate == referenceDate &&
                previous.initialRoomId == initialRoom && previous.segments == placement.segments
            ) previous
            else RecoveryOffer(nextOfferId++, reservation.id, referenceDate, initialRoom, placement.segments)
        }

        plan = result

        return result
    }

    /** 후속 장애에서 과거 배정과 완료된 이동을 보존한다. */
    fun replanForIncident(
        referenceDate: LocalDate,
        unavailabilities: List<RoomUnavailability>,
        affectedReservationIds: Set<Long>
    ): RecoveryPlan {
        require(referenceDate >= input.referenceDate) { "Recovery date cannot move backwards" }
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
        val offer = currentOffers.values.singleOrNull { it.id == offerId }
            ?: throw IllegalArgumentException("Offer is stale or does not exist")
        check(offer.decision == null) { "Offer already answered" }

        val reservation = input.reservations.single { it.id == offer.reservationId }
        check(reservation.status != ReservationStatus.CANCELLED) { "Reservation is cancelled" }

        when (decision) {
            RecoveryDecision.ACCEPTED -> {
                replaceFuture(offer.reservationId, offer.referenceDate, offer.segments)
                acceptedIds.add(offer.reservationId)
                offer.decision = decision
                return null
            }

            RecoveryDecision.CANCEL_REQUESTED -> {
                reservation.cancelForRecovery()
                replaceFuture(offer.reservationId, offer.referenceDate, emptyList())
                offer.decision = decision
                return replan(offer.referenceDate)
            }
        }
    }

    /** 실제 이동 완료만 기록한다. */
    fun completeMove(offerId: Long, date: LocalDate, fromRoomId: Long, toRoomId: Long): RoomMoveHistory {
        val offer = currentOffers.values.singleOrNull { it.id == offerId }
            ?: throw IllegalArgumentException("Offer is stale or does not exist")
        check(offer.decision == RecoveryDecision.ACCEPTED) { "Offer has not been accepted" }

        val move = PlannedMove(date, fromRoomId, toRoomId)
        require(fromRoomId != toRoomId && move in offer.plannedMoves) { "Move is not in the accepted plan" }

        val previous = history.filter { it.reservationId == offer.reservationId }
        require(previous.none { it.date == date && it.fromRoomId == fromRoomId && it.toRoomId == toRoomId }) { "Move already recorded" }
        require(previous.lastOrNull()?.date?.let { date > it } != false) { "Moves must be recorded chronologically" }

        val preceding = offer.plannedMoves.takeWhile { it != move }
        require(preceding.all { p -> previous.any { it.date == p.date && it.fromRoomId == p.fromRoomId && it.toRoomId == p.toRoomId } }) { "Earlier planned moves must be completed first" }

        return RoomMoveHistory(offer.reservationId, date, fromRoomId, toRoomId).also(history::add)
    }

    private fun replaceFuture(reservationId: Long, date: LocalDate, segments: List<RecoverySegment>) {
        val reservation = input.reservations.single { it.id == reservationId }
        val retained = currentAssignments.mapNotNull { assignment ->
            if (assignment.reservationId != reservationId || assignment.endDate <= date) assignment
            else if (assignment.startDate < date) RoomAssignment(
                assignment.id, assignment.accommodationId,
                assignment.reservationId, assignment.roomId, assignment.startDate, date
            )
            else null
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
