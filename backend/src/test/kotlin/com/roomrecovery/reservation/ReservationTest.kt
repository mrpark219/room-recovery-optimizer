package com.roomrecovery.reservation

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate

class ReservationTest {
    private val start = LocalDate.of(2026, 11, 10)
    private val end = start.plusDays(3)
    private fun reservation(status: ReservationStatus) =
        Reservation(1, 1, 1, Instant.EPOCH, 2, start, end, status)

    @Test
    fun `남은 숙박일은 시작 전에는 전체이며 퇴실일부터 0이다`() {
        // given
        val reservation = reservation(ReservationStatus.CHECKED_IN)

        // when & then
        assertEquals(3, reservation.totalNights())
        assertEquals(3, reservation.remainingNights(start.minusDays(1)))
        assertEquals(3, reservation.remainingNights(start))
        assertEquals(2, reservation.remainingNights(start.plusDays(1)))
        assertEquals(0, reservation.remainingNights(end))
        assertEquals(0, reservation.remainingNights(end.plusDays(1)))
    }

    @Test
    fun `복구 취소는 예약 상태를 변경하고 중복 취소를 거부한다`() {
        for (status in listOf(ReservationStatus.CONFIRMED, ReservationStatus.CHECKED_IN)) {
            // given
            val reservation = reservation(status)

            // when
            reservation.cancelForRecovery()

            // then
            assertEquals(ReservationStatus.CANCELLED, reservation.status)
            assertThrows(IllegalStateException::class.java) { reservation.cancelForRecovery() }
        }
    }

    @Test
    fun `취소된 예약도 유효한 숙박 기간을 보존한다`() {
        // given: 공통 날짜를 사용하거나 검증할 값을 직접 지정한다
        // when & then
        assertEquals(3, reservation(ReservationStatus.CANCELLED).totalNights())
    }

    @Test
    fun `확정 예약과 투숙 중 예약 모두 유효한 기간을 가진다`() {
        // given: 공통 날짜를 사용하거나 검증할 값을 직접 지정한다
        // when & then
        assertEquals(3, reservation(ReservationStatus.CHECKED_IN).totalNights())
        assertEquals(3, reservation(ReservationStatus.CONFIRMED).totalNights())
        assertThrows(IllegalArgumentException::class.java) {
            Reservation(1, 1, 1, Instant.EPOCH, 2, start, start, ReservationStatus.CONFIRMED)
        }
    }
}
