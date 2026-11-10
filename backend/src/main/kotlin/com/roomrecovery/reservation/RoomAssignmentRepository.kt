package com.roomrecovery.reservation

import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate

interface RoomAssignmentRepository : JpaRepository<RoomAssignment, Long> {
    fun findAllByAccommodationIdOrderByStartDateAscEndDateAscIdAsc(accommodationId: Long): List<RoomAssignment>

    fun findAllByAccommodationIdAndReservationIdAndEndDateAfter(
        accommodationId: Long,
        reservationId: Long,
        date: LocalDate
    ): List<RoomAssignment>

    fun findAllByAccommodationIdAndEndDateAfter(accommodationId: Long, date: LocalDate): List<RoomAssignment>
}
