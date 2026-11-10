package com.roomrecovery.inventory

import org.springframework.data.jpa.repository.JpaRepository

interface RoomTypeRepository : JpaRepository<RoomType, Long> {
    fun findAllByAccommodationId(accommodationId: Long): List<RoomType>
}
