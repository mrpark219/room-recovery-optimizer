package com.roomrecovery.recovery

import java.time.LocalDate

data class RecoveryCalculation(
    val referenceDate: LocalDate,
    val affectedReservations: List<AffectedReservation>,
    val placements: List<RecoveryPlacement>,
    val estimatedAdditionalCost: Long,
    val unresolvedReservationIds: List<Long>,
    val urgentReservationIds: List<Long>
)
