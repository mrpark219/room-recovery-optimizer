package com.roomrecovery.inventory

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/accommodations")
class InventoryController(private val inventoryService: InventoryService) {
    @GetMapping
    fun accommodations(): List<Accommodation> {
        return inventoryService.accommodations()
    }

    @GetMapping("/{accommodationId}/rooms")
    fun rooms(@PathVariable accommodationId: Long): List<RoomListItem> {
        return inventoryService.rooms(accommodationId)
    }
}
