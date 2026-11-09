package com.roomrecovery.recovery

import com.roomrecovery.availability.RoomUnavailability
import com.roomrecovery.inventory.Room
import com.roomrecovery.inventory.RoomType
import com.roomrecovery.reservation.Reservation
import com.roomrecovery.reservation.RoomAssignment
import java.math.BigDecimal
import java.time.LocalDate

enum class ConsentOutcome { ACCEPTED, DECLINED, NO_ANSWER }
