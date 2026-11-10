package com.roomrecovery.recovery.offer

import com.roomrecovery.recovery.RecoveryPlan
import java.time.LocalDate

data class RecoverySessionView(
    val sessionId: String,
    val accommodationId: Long,
    val referenceDate: LocalDate,
    val plan: RecoveryPlan?,
    val offers: List<RecoveryOffer>,
    val moveHistory: List<RoomMoveHistory>
)
