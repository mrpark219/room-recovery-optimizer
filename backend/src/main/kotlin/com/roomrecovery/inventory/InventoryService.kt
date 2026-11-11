package com.roomrecovery.inventory

import com.roomrecovery.common.NotFound
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service

data class RoomListItem(
    val id: Long,
    val accommodationId: Long,
    val roomTypeId: Long,
    val name: String,
    val roomTypeName: String,
    val capacity: Int,
    val nightlyRate: Int
)

@Service
class InventoryService(
    private val accommodations: AccommodationRepository,
    private val rooms: RoomRepository,
    private val roomTypes: RoomTypeRepository
) {
    fun accommodations(): List<Accommodation> {
        return accommodations.findAll(Sort.by("id"))
    }

    fun rooms(accommodationId: Long): List<RoomListItem> {
        if (!accommodations.existsById(accommodationId)) {
            throw NotFound("숙소를 찾을 수 없습니다")
        }

        val typesById = roomTypes.findAllByAccommodationId(accommodationId).associateBy { it.id }
        return rooms.findAllByAccommodationIdOrderById(accommodationId).map { room ->
            val type = checkNotNull(typesById[room.roomTypeId]) {
                "객실의 객실 유형을 찾을 수 없습니다: ${room.id}"
            }
            RoomListItem(room.id, room.accommodationId, room.roomTypeId, room.name, type.name, type.capacity, type.nightlyRate)
        }
    }
}
