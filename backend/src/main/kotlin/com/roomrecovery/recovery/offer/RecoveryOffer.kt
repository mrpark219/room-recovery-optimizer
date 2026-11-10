package com.roomrecovery.recovery.offer

import com.roomrecovery.recovery.RecoverySegment
import java.time.LocalDate

/** 남은 숙박 전체에 대한 하나의 제안이다. */
class RecoveryOffer internal constructor(
    val id: Long,
    val reservationId: Long,
    val referenceDate: LocalDate,
    val initialRoomId: Long?,
    segments: List<RecoverySegment>
) {
    val segments: List<RecoverySegment> = segments.toList()
    var decision: RecoveryDecision? = null
        internal set

    val plannedMoves: List<PlannedMove> = buildList {
        var previous = initialRoomId
        for (segment in this@RecoveryOffer.segments) {
            if (previous != null && previous != segment.roomId) {
                add(PlannedMove(segment.startDate, previous, segment.roomId))
            }
            previous = segment.roomId
        }
    }
}
