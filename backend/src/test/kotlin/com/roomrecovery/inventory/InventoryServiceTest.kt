package com.roomrecovery.inventory

import com.roomrecovery.common.NotFound
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`

class InventoryServiceTest {
    private val accommodations = mock(AccommodationRepository::class.java)
    private val rooms = mock(RoomRepository::class.java)
    private val roomTypes = mock(RoomTypeRepository::class.java)
    private val service = InventoryService(accommodations, rooms, roomTypes)

    @Test
    fun `returns room details using one room type lookup`() {
        `when`(accommodations.existsById(3)).thenReturn(true)
        `when`(roomTypes.findAllByAccommodationId(3)).thenReturn(listOf(
            RoomType(4, 3, "Standard", 2, 1, 100),
            RoomType(5, 3, "Suite", 4, 2, 200)
        ))
        `when`(rooms.findAllByAccommodationIdOrderById(3)).thenReturn(listOf(
            Room(101, 3, 4, "101"), Room(102, 3, 5, "102")
        ))

        assertEquals(listOf(
            RoomListItem(101, 3, 4, "101", "Standard", 2, 100),
            RoomListItem(102, 3, 5, "102", "Suite", 4, 200)
        ), service.rooms(3))
        verify(roomTypes).findAllByAccommodationId(3)
    }

    @Test
    fun `unknown accommodation fails before querying rooms and types`() {
        assertThrows(NotFound::class.java) { service.rooms(999) }
        verifyNoInteractions(rooms, roomTypes)
    }
}
