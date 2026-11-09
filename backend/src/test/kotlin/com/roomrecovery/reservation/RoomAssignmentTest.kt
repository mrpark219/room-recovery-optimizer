package com.roomrecovery.reservation

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDate

class RoomAssignmentTest {
    private val start = LocalDate.of(2026, 11, 10)
    private fun assignment(roomId: Long, from: LocalDate, until: LocalDate) = RoomAssignment(1, 1, 1, roomId, from, until)

    @Test
    fun `배정은 시작일을 포함하고 종료일을 제외한다`() {
        val assignment = assignment(1, start, start.plusDays(2))
        assertFalse(assignment.contains(start.minusDays(1)))
        assertTrue(assignment.contains(start))
        assertTrue(assignment.contains(start.plusDays(1)))
        assertFalse(assignment.contains(start.plusDays(2)))
        assertThrows(IllegalArgumentException::class.java) { assignment(1, start, start) }
    }

    @Test
    fun `같은 객실의 겹치는 기간만 충돌한다`() {
        val first = assignment(1, start, start.plusDays(2))
        assertTrue(first.overlaps(assignment(1, start.plusDays(1), start.plusDays(3))))
        assertFalse(first.overlaps(assignment(1, start.plusDays(2), start.plusDays(3))))
        assertFalse(first.overlaps(assignment(2, start, start.plusDays(2))))
    }
}
