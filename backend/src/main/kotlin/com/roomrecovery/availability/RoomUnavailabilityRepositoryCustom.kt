package com.roomrecovery.availability

import java.time.LocalDate

interface RoomUnavailabilityRepositoryCustom {
    fun findAllInAccommodation(accommodationId: Long): List<RoomUnavailability>
    fun findInAccommodation(accommodationId: Long, id: Long): RoomUnavailability?
    fun findRemainingInAccommodation(accommodationId: Long, referenceDate: LocalDate): List<RoomUnavailability>
}
