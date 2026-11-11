package com.roomrecovery.recovery.offer

import java.time.LocalDate

internal data class RecoverySession(
    val id: String,
    val accommodationId: Long,
    val date: LocalDate,
    var coordinator: RecoveryCoordinator,
    val roomNames: Map<Long, String>,
    val incidents: List<RecoveryIncident>
)
