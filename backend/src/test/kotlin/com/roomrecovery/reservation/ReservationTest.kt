package com.roomrecovery.reservation

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate

class ReservationTest {
    private val start = LocalDate.of(2026, 11, 10)
    private val end = start.plusDays(3)
    private fun reservation(status: ReservationStatus, occupiedRoomId: Long?) =
        Reservation(1, 1, 1, occupiedRoomId, Instant.EPOCH, 2, start, end, status)

    @Test
    fun `남은 숙박일은 시작 전에는 전체이며 퇴실일부터 0이다`() {
        val reservation = reservation(ReservationStatus.CHECKED_IN, 1)
        assertEquals(3, reservation.totalNights())
        assertEquals(3, reservation.remainingNights(start.minusDays(1)))
        assertEquals(3, reservation.remainingNights(start))
        assertEquals(2, reservation.remainingNights(start.plusDays(1)))
        assertEquals(0, reservation.remainingNights(end))
        assertEquals(0, reservation.remainingNights(end.plusDays(1)))
    }

    @Test
    fun `현재 객실은 투숙 중인 예약에만 존재한다`() {
        assertNotNull(reservation(ReservationStatus.CHECKED_IN, 1))
        assertNotNull(reservation(ReservationStatus.CONFIRMED, null))
        assertThrows(IllegalArgumentException::class.java) { reservation(ReservationStatus.CHECKED_IN, null) }
        assertThrows(IllegalArgumentException::class.java) { reservation(ReservationStatus.CONFIRMED, 1) }
    }
}
