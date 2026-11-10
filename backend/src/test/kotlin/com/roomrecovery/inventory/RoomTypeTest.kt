package com.roomrecovery.inventory

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class RoomTypeTest {
    private fun type(accommodationId: Long, capacity: Int, grade: Int) =
        RoomType(1, accommodationId, "객실", capacity, grade, 100000)

    @Test
    fun `객실 유형은 같은 숙소에서 인원과 등급을 모두 만족해야 한다`() {
        // given
        val booked = type(1, 2, 2)

        // when & then
        assertTrue(type(1, 3, 3).canAccommodate(booked, 3))
        assertTrue(type(1, 2, 2).canAccommodate(booked, 2))
        assertFalse(type(2, 3, 3).canAccommodate(booked, 2))
        assertFalse(type(1, 1, 3).canAccommodate(booked, 2))
        assertFalse(type(1, 3, 1).canAccommodate(booked, 2))
        assertFalse(type(1, 3, 3).canAccommodate(booked, 0))
    }

    @Test
    fun `객실 유형의 정원과 등급 및 가격은 유효해야 한다`() {
        // given: 공통 날짜를 사용하거나 검증할 값을 직접 지정한다
        // when & then
        assertThrows(IllegalArgumentException::class.java) { type(1, 0, 1) }
        assertThrows(IllegalArgumentException::class.java) { type(1, 1, -1) }
        assertThrows(IllegalArgumentException::class.java) { RoomType(1, 1, "객실", 1, 1, -1) }
    }
}
