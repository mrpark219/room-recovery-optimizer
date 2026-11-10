package com.roomrecovery.reservation

import com.roomrecovery.common.NotFound
import com.roomrecovery.inventory.AccommodationRepository
import com.roomrecovery.inventory.RoomType
import com.roomrecovery.inventory.RoomTypeRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import java.time.Instant
import java.time.LocalDate

class ReservationListServiceTest {
    private val accommodations = mock(AccommodationRepository::class.java)
    private val reservations = mock(ReservationRepository::class.java)
    private val roomTypes = mock(RoomTypeRepository::class.java)
    private val assignments = mock(RoomAssignmentRepository::class.java)
    private val service = ReservationListService(accommodations, reservations, roomTypes, assignments)

    @Test
    fun `returns reservations with grouped assignments including unassigned and cancelled`() {
        val start = LocalDate.parse("2026-11-10")
        val end = start.plusDays(3)
        val bookedAt = Instant.parse("2026-01-01T00:00:00Z")
        `when`(accommodations.existsById(3)).thenReturn(true)
        `when`(roomTypes.findAllByAccommodationId(3)).thenReturn(listOf(RoomType(4, 3, "Standard", 2, 1, 100)))
        `when`(reservations.findAllByAccommodationIdOrderByStartDateAscIdAsc(3)).thenReturn(listOf(
            Reservation(1, 3, 4, bookedAt, 2, start, end, ReservationStatus.CHECKED_IN),
            Reservation(2, 3, 4, bookedAt, 2, start, end, ReservationStatus.CANCELLED)
        ))
        `when`(assignments.findAllByAccommodationIdOrderByStartDateAscEndDateAscIdAsc(3)).thenReturn(listOf(
            RoomAssignment(1, 3, 1, 101, start, start.plusDays(1)),
            RoomAssignment(2, 3, 1, 102, start.plusDays(1), end)
        ))

        assertEquals(listOf(
            ReservationListItem(1, start, end, ReservationStatus.CHECKED_IN, "Standard", listOf(
                ReservationAssignmentItem(101, start, start.plusDays(1)),
                ReservationAssignmentItem(102, start.plusDays(1), end)
            )),
            ReservationListItem(2, start, end, ReservationStatus.CANCELLED, "Standard", emptyList())
        ), service.list(3))
    }

    @Test
    fun `unknown accommodation fails before querying its data`() {
        assertThrows(NotFound::class.java) { service.list(999) }
        verifyNoInteractions(reservations, roomTypes, assignments)
    }
}
