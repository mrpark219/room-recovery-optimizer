package com.roomrecovery.recovery

import com.roomrecovery.availability.RoomUnavailability
import com.roomrecovery.inventory.Room
import com.roomrecovery.inventory.RoomType
import com.roomrecovery.reservation.Reservation
import com.roomrecovery.reservation.RoomAssignment
import java.math.BigDecimal
import java.time.LocalDate

data class RecoveryInput(
    val referenceDate: LocalDate,
    val rooms: List<Room>,
    val roomTypes: List<RoomType>,
    val reservations: List<Reservation>,
    val assignments: List<RoomAssignment>,
    val unavailabilities: List<RoomUnavailability>,
    val consents: Map<ConsentKey, ConsentOutcome> = emptyMap(),
    val considerSavings: Boolean = false
)
