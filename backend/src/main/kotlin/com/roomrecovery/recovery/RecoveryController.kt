package com.roomrecovery.recovery

import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/accommodations/{accommodationId}/recovery-calculations")
class RecoveryController(private val recoveryService: RecoveryService) {
    @PostMapping
    fun calculate(
        @PathVariable accommodationId: Long,
        @RequestBody request: RecoveryCalculationRequest
    ): RecoveryCalculation {
        return recoveryService.calculate(accommodationId, request.referenceDate)
    }
}
