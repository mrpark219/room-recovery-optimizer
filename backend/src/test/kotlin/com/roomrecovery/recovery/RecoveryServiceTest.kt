package com.roomrecovery.recovery

import com.roomrecovery.availability.RoomUnavailability
import com.roomrecovery.availability.RoomUnavailabilityRepository
import com.roomrecovery.inventory.*
import com.roomrecovery.reservation.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.time.Instant
import java.time.LocalDate

class RecoveryServiceTest {
    @Test
    fun `미래 예약은 전체 숙박을 계산하고 정상 예약은 객실 점유를 유지한다`() {
        // given
        val date = LocalDate.parse("2026-11-10")
        val accommodations = mock(AccommodationRepository::class.java)
        val rooms = mock(RoomRepository::class.java)
        val roomTypes = mock(RoomTypeRepository::class.java)
        val reservations = mock(ReservationRepository::class.java)
        val assignments = mock(RoomAssignmentRepository::class.java)
        val outages = mock(RoomUnavailabilityRepository::class.java)
        val start = date.plusDays(2)
        val end = date.plusDays(5)
        val bookedAt = Instant.parse("2026-01-01T00:00:00Z")
        `when`(accommodations.existsById(1)).thenReturn(true)
        `when`(rooms.findAllByAccommodationIdOrderById(1)).thenReturn(
            listOf(
                Room(1, 1, 1, "1"), Room(2, 1, 1, "2"), Room(3, 1, 1, "3")
            )
        )
        `when`(roomTypes.findAllByAccommodationId(1)).thenReturn(listOf(RoomType(1, 1, "기본", 2, 1, 100)))
        `when`(reservations.findAllByAccommodationIdAndEndDateAfter(1, date)).thenReturn(
            listOf(
                Reservation(1, 1, 1, bookedAt, 2, start, end, ReservationStatus.CONFIRMED),
                Reservation(2, 1, 1, bookedAt, 2, start, end, ReservationStatus.CONFIRMED)
            )
        )
        `when`(assignments.findAllByAccommodationIdAndEndDateAfter(1, date)).thenReturn(
            listOf(
                RoomAssignment(1, 1, 1, 1, start, end), RoomAssignment(2, 1, 2, 2, start, end)
            )
        )
        `when`(outages.findRemainingInAccommodation(1, date)).thenReturn(
            listOf(
                RoomUnavailability.create(1, start.plusDays(1), end, "수리")
            )
        )

        // when
        val result = RecoveryService(accommodations, rooms, roomTypes, reservations, assignments, outages)
            .calculate(1, date)

        // then
        assertEquals(listOf(1L), result.affectedReservations.map { it.reservationId })
        assertEquals(listOf(RecoverySegment(3, start, end)), result.placements.single().segments)
        assertEquals(0L, result.estimatedAdditionalCost)
        assertEquals(emptyList<Long>(), result.unresolvedReservationIds)
    }
}
