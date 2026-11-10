package com.roomrecovery.recovery.offer

import java.time.LocalDate

data class MoveRequest(val date: LocalDate, val fromRoomId: Long, val toRoomId: Long)
