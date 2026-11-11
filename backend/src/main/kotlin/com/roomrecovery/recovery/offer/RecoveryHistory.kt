package com.roomrecovery.recovery.offer

import jakarta.persistence.*
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Service
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper
import java.time.Instant
import java.time.LocalDate

@Entity
@Table(name = "recovery_history")
class RecoveryHistoryEntry(
    val accommodationId: Long,
    val sessionId: String,
    val referenceDate: LocalDate,
    val action: String,
    val recordedAt: Instant,
    @Column(columnDefinition = "json")
    val details: String,
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0
)

interface RecoveryHistoryRepository : JpaRepository<RecoveryHistoryEntry, Long> {
    fun findAllByAccommodationIdOrderByIdDesc(accommodationId: Long): List<RecoveryHistoryEntry>
}

data class RecoveryHistoryItem(
    val id: Long,
    val sessionId: String,
    val referenceDate: LocalDate,
    val action: String,
    val recordedAt: Instant,
    val details: JsonNode
)

internal data class RecoveryIncident(
    val roomName: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val reason: String
)

@Service
class RecoveryHistory(private val entries: RecoveryHistoryRepository, private val mapper: ObjectMapper) {
    internal fun record(session: RecoverySession, action: String, details: Map<String, Any>) {
        entries.save(
            RecoveryHistoryEntry(
                session.accommodationId, session.id, session.date, action, Instant.now(),
                mapper.writeValueAsString(details)
            )
        )
    }

    fun list(accommodationId: Long): List<RecoveryHistoryItem> {
        return entries.findAllByAccommodationIdOrderByIdDesc(accommodationId).map {
            RecoveryHistoryItem(
                it.id, it.sessionId, it.referenceDate, it.action, it.recordedAt, mapper.readTree(it.details)
            )
        }
    }
}
