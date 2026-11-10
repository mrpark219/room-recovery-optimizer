package com.roomrecovery.reservation

import jakarta.persistence.*
import java.time.LocalDate

@Entity
@Table(name = "room_assignment")
class RoomAssignment(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long,
    @Column(name = "accommodation_id", nullable = false)
    val accommodationId: Long,
    @Column(name = "reservation_id", nullable = false)
    val reservationId: Long,
    @Column(name = "room_id", nullable = false)
    val roomId: Long,
    @Column(name = "start_date", nullable = false)
    val startDate: LocalDate,
    endDate: LocalDate // 숙박에 미포함
) {
    @Column(name = "`endDate`", nullable = false)
    var endDate: LocalDate = endDate
        protected set

    internal fun shorten(end: LocalDate) {
        require(end > startDate && end < endDate) { "배정 종료일을 확인해 주세요" }
        endDate = end
    }

    init {
        require(startDate < endDate) { "배정 종료일은 시작일보다 늦어야 합니다" }
    }

    fun contains(date: LocalDate): Boolean {
        return date >= startDate && date < endDate
    }

    fun overlaps(other: RoomAssignment): Boolean {
        return roomId == other.roomId && startDate < other.endDate && other.startDate < endDate
    }
}
