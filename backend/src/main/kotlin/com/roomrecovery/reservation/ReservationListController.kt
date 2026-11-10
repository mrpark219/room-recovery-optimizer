package com.roomrecovery.reservation

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/accommodations/{accommodationId}/reservations")
class ReservationListController(private val service: ReservationListService) {
    @GetMapping
    fun list(@PathVariable accommodationId: Long): List<ReservationListItem> = service.list(accommodationId)
}
