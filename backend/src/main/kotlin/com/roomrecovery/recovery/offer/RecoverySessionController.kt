package com.roomrecovery.recovery.offer

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/accommodations/{accommodationId}/recovery-sessions")
class RecoverySessionController(private val recoverySessionService: RecoverySessionService) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@PathVariable accommodationId: Long, @RequestBody request: CreateSessionRequest): RecoverySessionView {
        return recoverySessionService.create(accommodationId, request.referenceDate)
    }

    @GetMapping("/{sessionId}")
    fun get(@PathVariable accommodationId: Long, @PathVariable sessionId: String): RecoverySessionView {
        return recoverySessionService.get(accommodationId, sessionId)
    }

    @PostMapping("/{sessionId}/offers/{offerId}/decision")
    fun decide(
        @PathVariable accommodationId: Long,
        @PathVariable sessionId: String,
        @PathVariable offerId: Long,
        @RequestBody request: DecisionRequest
    ): RecoverySessionView {
        return recoverySessionService.decide(accommodationId, sessionId, offerId, request.decision)
    }

    @PostMapping("/{sessionId}/offers/{offerId}/moves")
    fun move(
        @PathVariable accommodationId: Long,
        @PathVariable sessionId: String,
        @PathVariable offerId: Long,
        @RequestBody request: MoveRequest
    ): RecoverySessionView {
        return recoverySessionService.move(accommodationId, sessionId, offerId, request)
    }

    @ExceptionHandler(RecoveryConflict::class)
    fun conflict(e: RecoveryConflict): ResponseEntity<Map<String, String>> {
        return ResponseEntity.status(409).body(mapOf("error" to (e.message ?: "현재 상태에서 처리할 수 없습니다")))
    }
}
