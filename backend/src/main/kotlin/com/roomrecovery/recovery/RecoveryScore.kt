package com.roomrecovery.recovery

import com.roomrecovery.availability.RoomUnavailability
import com.roomrecovery.inventory.Room
import com.roomrecovery.inventory.RoomType
import com.roomrecovery.reservation.Reservation
import com.roomrecovery.reservation.RoomAssignment
import java.math.BigDecimal
import java.time.LocalDate

data class RecoveryScore(val urgentServed: Int, val served: Int, val additionalCost: BigDecimal, val moves: Int)
