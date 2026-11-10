package com.roomrecovery.recovery

data class RecoveryPlacement(
    val reservationId: Long,
    val segments: List<RecoverySegment>,
    val additionalCost: Long,
    val moves: Int
)
