package com.roomrecovery

import com.roomrecovery.availability.UnavailabilityInput
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.time.LocalDate

class UnavailabilityInputTest {
    private val start = LocalDate.parse("2026-11-09")
    @Test
    fun `이용 불가 종료일은 시작일보다 늦어야 한다`() {
        // given: 공통 날짜를 사용하거나 검증할 값을 직접 지정한다
        // when & then
        assertThrows(IllegalArgumentException::class.java) { UnavailabilityInput(1, start, start, "repair") }
        assertThrows(IllegalArgumentException::class.java) {
            UnavailabilityInput(
                1,
                start,
                start.minusDays(1),
                "repair"
            )
        }
        assertDoesNotThrow { UnavailabilityInput(1, start, start.plusDays(1), "repair") }
    }

    @Test
    fun `이용 불가 사유와 객실은 유효해야 한다`() {
        // given: 공통 날짜를 사용하거나 검증할 값을 직접 지정한다
        // when & then
        assertThrows(IllegalArgumentException::class.java) { UnavailabilityInput(1, start, start.plusDays(1), "  ") }
        assertThrows(IllegalArgumentException::class.java) {
            UnavailabilityInput(
                0,
                start,
                start.plusDays(1),
                "repair"
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            UnavailabilityInput(
                1,
                start,
                start.plusDays(1),
                "x".repeat(501)
            )
        }
    }
}
