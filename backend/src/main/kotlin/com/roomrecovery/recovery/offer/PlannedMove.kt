package com.roomrecovery.recovery.offer

import java.time.LocalDate

data class PlannedMove(val date: LocalDate, val fromRoomId: Long, val toRoomId: Long)
