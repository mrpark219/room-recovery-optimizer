package com.roomrecovery.reservation

import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate

interface ReservationRepository : JpaRepository<Reservation, Long> {
    fun findAllByAccommodationIdOrderByStartDateAscIdAsc(accommodationId: Long): List<Reservation>

    fun findAllByAccommodationIdAndEndDateAfter(accommodationId: Long, date: LocalDate): List<Reservation>
}
