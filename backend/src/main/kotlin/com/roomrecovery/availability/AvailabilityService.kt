package com.roomrecovery.availability

import com.roomrecovery.common.NotFound
import com.roomrecovery.inventory.AccommodationRepository
import com.roomrecovery.inventory.RoomRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AvailabilityService(
    private val accommodations: AccommodationRepository,
    private val rooms: RoomRepository,
    private val unavailabilities: RoomUnavailabilityRepository
) {
    fun list(accommodationId: Long): List<RoomUnavailability> {
        requireAccommodation(accommodationId)

        return unavailabilities.findAllInAccommodation(accommodationId)
    }

    fun get(accommodationId: Long, id: Long): RoomUnavailability {
        return unavailabilities.findInAccommodation(accommodationId, id)
            ?: throw NotFound("이용 불가 정보를 찾을 수 없습니다")
    }

    @Transactional
    fun create(accommodationId: Long, input: UnavailabilityInput): RoomUnavailability {
        requireRoom(accommodationId, input.roomId)

        return unavailabilities.save(
            RoomUnavailability.create(
                roomId = input.roomId, startDate = input.startDate,
                endDate = input.endDate, reason = input.reason
            )
        )
    }

    @Transactional
    fun update(accommodationId: Long, id: Long, input: UnavailabilityInput): RoomUnavailability {
        val entry = unavailabilities.findInAccommodation(accommodationId, id)
            ?: throw NotFound("이용 불가 정보를 찾을 수 없습니다")

        requireRoom(accommodationId, input.roomId)
        entry.update(input.roomId, input.startDate, input.endDate, input.reason)

        return entry
    }

    @Transactional
    fun delete(accommodationId: Long, id: Long) {
        val entry = unavailabilities.findInAccommodation(accommodationId, id)
            ?: throw NotFound("이용 불가 정보를 찾을 수 없습니다")

        unavailabilities.delete(entry)
    }

    private fun requireAccommodation(id: Long) {
        if (!accommodations.existsById(id)) {
            throw NotFound("숙소를 찾을 수 없습니다")
        }
    }

    private fun requireRoom(accommodationId: Long, roomId: Long) {
        requireAccommodation(accommodationId)

        if (!rooms.existsByIdAndAccommodationId(
                roomId,
                accommodationId
            )
        ) {
            throw NotFound("숙소의 객실을 찾을 수 없습니다")
        }
    }
}
