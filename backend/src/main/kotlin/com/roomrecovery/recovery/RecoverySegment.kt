package com.roomrecovery.recovery

import java.time.LocalDate

data class RecoverySegment(
    val roomId: Long,
    val startDate: LocalDate,
    val endDate: LocalDate // 숙박에 미포함
)
