package com.roomrecovery.inventory

import org.springframework.data.jpa.repository.JpaRepository

interface RoomRepository : JpaRepository<Room, Long> {
    fun findAllByAccommodationIdOrderById(accommodationId: Long): List<Room>
    fun existsByIdAndAccommodationId(id: Long, accommodationId: Long): Boolean
}
