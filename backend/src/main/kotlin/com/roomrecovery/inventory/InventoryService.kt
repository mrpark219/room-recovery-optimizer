package com.roomrecovery.inventory

import com.roomrecovery.common.NotFound
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service

@Service
class InventoryService(private val accommodations: AccommodationRepository, private val rooms: RoomRepository) {
    fun accommodations(): List<Accommodation> {
        return accommodations.findAll(Sort.by("id"))
    }

    fun rooms(accommodationId: Long): List<Room> {
        if (!accommodations.existsById(accommodationId)) {
            throw NotFound("숙소를 찾을 수 없습니다")
        }

        return rooms.findAllByAccommodationIdOrderById(accommodationId)
    }
}
