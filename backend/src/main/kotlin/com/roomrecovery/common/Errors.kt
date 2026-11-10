package com.roomrecovery.common

import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException

@RestControllerAdvice
class Errors {
    @ExceptionHandler(NotFound::class)
    fun notFound(e: NotFound): ResponseEntity<Map<String, String>> {
        return ResponseEntity.status(404).body(mapOf("error" to (e.message ?: "대상을 찾을 수 없습니다")))
    }

    @ExceptionHandler(
        IllegalArgumentException::class,
        HttpMessageNotReadableException::class,
        MethodArgumentTypeMismatchException::class
    )
    fun badRequest(): ResponseEntity<Map<String, String>> {
        return ResponseEntity.badRequest().body(mapOf("error" to "요청 값을 확인해 주세요"))
    }
}
