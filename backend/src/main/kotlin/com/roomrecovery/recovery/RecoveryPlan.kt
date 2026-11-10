package com.roomrecovery.recovery

data class RecoveryPlan(
    val placements: List<RecoveryPlacement>,
    val unresolvedReservationIds: List<Long>,
    val score: RecoveryScore,
    val urgentReservationIds: List<Long> = emptyList()
)
