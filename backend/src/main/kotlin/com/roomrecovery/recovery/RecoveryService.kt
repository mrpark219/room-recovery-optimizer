package com.roomrecovery.recovery

import com.roomrecovery.availability.RoomUnavailabilityRepository
import com.roomrecovery.common.NotFound
import com.roomrecovery.inventory.AccommodationRepository
import com.roomrecovery.inventory.RoomRepository
import com.roomrecovery.inventory.RoomTypeRepository
import com.roomrecovery.reservation.ReservationRepository
import com.roomrecovery.reservation.RoomAssignmentRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class RecoveryService(
    private val accommodations: AccommodationRepository,
    private val rooms: RoomRepository,
    private val roomTypes: RoomTypeRepository,
    private val reservations: ReservationRepository,
    private val assignments: RoomAssignmentRepository,
    private val unavailabilities: RoomUnavailabilityRepository
) {
    @Transactional(readOnly = true)
    fun calculate(accommodationId: Long, referenceDate: LocalDate): RecoveryCalculation {
        if (!accommodations.existsById(accommodationId)) {
            throw NotFound("숙소를 찾을 수 없습니다")
        }

        val input = RecoveryInput(
            referenceDate = referenceDate,
            rooms = rooms.findAllByAccommodationIdOrderById(accommodationId),
            roomTypes = roomTypes.findAllByAccommodationId(accommodationId),
            reservations = reservations.findAllByAccommodationIdAndEndDateAfter(accommodationId, referenceDate),
            assignments = assignments.findAllByAccommodationIdAndEndDateAfter(accommodationId, referenceDate),
            unavailabilities = unavailabilities.findRemainingInAccommodation(accommodationId, referenceDate)
        )

        val plan = RecoveryOptimizer.optimize(input)
        val affectedIds = (plan.placements.map { it.reservationId } + plan.unresolvedReservationIds).toSet()

        return RecoveryCalculation(
            referenceDate = referenceDate,
            affectedReservations = input.reservations.filter { it.id in affectedIds }.sortedBy { it.id }
                .map { AffectedReservation(it.id, it.startDate, it.endDate, it.status.name) },
            placements = plan.placements,
            estimatedAdditionalCost = plan.score.additionalCost,
            unresolvedReservationIds = plan.unresolvedReservationIds,
            urgentReservationIds = plan.urgentReservationIds
        )
    }
}
