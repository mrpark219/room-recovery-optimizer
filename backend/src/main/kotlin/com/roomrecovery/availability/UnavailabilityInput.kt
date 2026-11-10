package com.roomrecovery.availability

import java.time.LocalDate

data class UnavailabilityInput(
    val roomId: Long,
    val startDate: LocalDate,
    val endDate: LocalDate, // 숙박에 미포함
    val reason: String
) {
    init {
        RoomUnavailability.validate(roomId, startDate, endDate, reason)
    }
}
