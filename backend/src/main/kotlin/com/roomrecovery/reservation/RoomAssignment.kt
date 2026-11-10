package com.roomrecovery.reservation

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
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
    @Column(name = "`endDate`", nullable = false)
    val endDate: LocalDate // 숙박에 미포함
) {
    init {
        require(startDate < endDate) { "배정 종료일은 시작일보다 늦어야 합니다" }
    }

    fun contains(date: LocalDate): Boolean = date >= startDate && date < endDate

    fun overlaps(other: RoomAssignment): Boolean =
        roomId == other.roomId && startDate < other.endDate && other.startDate < endDate
}
