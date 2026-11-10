package com.roomrecovery.reservation

import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate

interface RoomAssignmentRepository : JpaRepository<RoomAssignment, Long> {
    fun findAllByAccommodationIdAndEndDateAfter(accommodationId: Long, date: LocalDate): List<RoomAssignment>
}
