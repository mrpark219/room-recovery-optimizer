package com.roomrecovery.availability

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.net.URI

@RestController
@RequestMapping("/api/accommodations/{accommodationId}/unavailabilities")
class RoomUnavailabilityController(private val availabilityService: AvailabilityService) {
    @GetMapping
    fun list(@PathVariable accommodationId: Long): List<RoomUnavailability> {
        return availabilityService.list(accommodationId)
    }

    @GetMapping("/{id}")
    fun get(@PathVariable accommodationId: Long, @PathVariable id: Long): RoomUnavailability {
        return availabilityService.get(accommodationId, id)
    }

    @PostMapping
    fun create(
        @PathVariable accommodationId: Long,
        @RequestBody input: UnavailabilityInput
    ): ResponseEntity<RoomUnavailability> {
        val result = availabilityService.create(accommodationId, input)
        val location = URI.create("/api/accommodations/$accommodationId/unavailabilities/${result.id}")

        return ResponseEntity.created(location).body(result)
    }

    @PutMapping("/{id}")
    fun update(
        @PathVariable accommodationId: Long,
        @PathVariable id: Long,
        @RequestBody input: UnavailabilityInput
    ): RoomUnavailability {
        return availabilityService.update(accommodationId, id, input)
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@PathVariable accommodationId: Long, @PathVariable id: Long) {
        availabilityService.delete(accommodationId, id)
    }
}
