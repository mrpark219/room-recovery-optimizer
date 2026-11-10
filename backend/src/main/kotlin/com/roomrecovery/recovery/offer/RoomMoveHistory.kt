package com.roomrecovery.recovery.offer

import java.time.LocalDate

data class RoomMoveHistory(val reservationId: Long, val date: LocalDate, val fromRoomId: Long, val toRoomId: Long)
