package com.roomrecovery.recovery

import java.time.LocalDate

data class AffectedReservation(
    val reservationId: Long,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val status: String
)
