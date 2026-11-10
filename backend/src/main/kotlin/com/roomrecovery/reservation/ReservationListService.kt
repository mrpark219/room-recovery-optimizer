package com.roomrecovery.reservation

import com.roomrecovery.common.NotFound
import com.roomrecovery.inventory.AccommodationRepository
import com.roomrecovery.inventory.RoomTypeRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

data class ReservationListItem(
    val id: Long,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val status: ReservationStatus,
    val roomTypeName: String,
    val assignments: List<ReservationAssignmentItem>
)

data class ReservationAssignmentItem(val roomId: Long, val startDate: LocalDate, val endDate: LocalDate)

@Service
class ReservationListService(
    private val accommodations: AccommodationRepository,
    private val reservations: ReservationRepository,
    private val roomTypes: RoomTypeRepository,
    private val assignments: RoomAssignmentRepository
) {
    @Transactional(readOnly = true)
    fun list(accommodationId: Long): List<ReservationListItem> {
        if (!accommodations.existsById(accommodationId)) throw NotFound("숙소를 찾을 수 없습니다")

        val typesById = roomTypes.findAllByAccommodationId(accommodationId).associateBy { it.id }
        val assignmentsByReservation = assignments
            .findAllByAccommodationIdOrderByStartDateAscEndDateAscIdAsc(accommodationId)
            .groupBy { it.reservationId }
        return reservations.findAllByAccommodationIdOrderByStartDateAscIdAsc(accommodationId).map { reservation ->
            ReservationListItem(
                id = reservation.id,
                startDate = reservation.startDate,
                endDate = reservation.endDate,
                status = reservation.status,
                roomTypeName = checkNotNull(typesById[reservation.bookedRoomTypeId]) {
                    "예약의 객실 유형을 찾을 수 없습니다: ${reservation.id}"
                }.name,
                assignments = assignmentsByReservation[reservation.id].orEmpty().map {
                    ReservationAssignmentItem(it.roomId, it.startDate, it.endDate)
                }
            )
        }
    }
}
