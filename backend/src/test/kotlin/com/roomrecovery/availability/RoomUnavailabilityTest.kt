package com.roomrecovery.availability

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDate

class RoomUnavailabilityTest {
    private val start = LocalDate.of(2026, 11, 10)
    private val end = start.plusDays(2)

    @Test
    fun `객실과 반열린 구간이 모두 겹쳐야 이용 불가와 충돌한다`() {
        // given
        val entry = RoomUnavailability.create(1, start, end, "점검")

        // when & then
        assertTrue(entry.overlaps(1, start.plusDays(1), end.plusDays(1)))
        assertFalse(entry.overlaps(1, end, end.plusDays(1)))
        assertFalse(entry.overlaps(1, start.minusDays(1), start))
        assertFalse(entry.overlaps(2, start, end))
        assertThrows(IllegalArgumentException::class.java) { entry.overlaps(1, start, start) }
    }

    @Test
    fun `잘못된 수정은 기존 값을 변경하지 않는다`() {
        // given
        val entry = RoomUnavailability.create(1, start, end, "점검")

        // when
        assertThrows(IllegalArgumentException::class.java) { entry.update(2, start, start, "누수") }

        // then
        assertEquals(1, entry.roomId)
        assertEquals(start, entry.startDate)
        assertEquals(end, entry.endDate)
        assertEquals("점검", entry.reason)
        assertThrows(IllegalArgumentException::class.java) { entry.update(2, start, end, " ") }
        assertEquals(1, entry.roomId)

        // when
        entry.update(2, start.plusDays(1), end.plusDays(1), "누수")

        // then
        assertEquals(2, entry.roomId)
        assertEquals("누수", entry.reason)
    }
}
