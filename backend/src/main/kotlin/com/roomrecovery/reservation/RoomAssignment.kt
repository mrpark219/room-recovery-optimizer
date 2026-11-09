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
    id: Long,
    accommodationId: Long,
    reservationId: Long,
    roomId: Long,
    startDate: LocalDate,
    endDateExclusive: LocalDate
    ) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = id
        protected set

    @Column(name = "accommodation_id", nullable = false)
    var accommodationId: Long = accommodationId
        protected set

    @Column(name = "reservation_id", nullable = false)
    var reservationId: Long = reservationId
        protected set

    @Column(name = "room_id", nullable = false)
    var roomId: Long = roomId
        protected set

    @Column(name = "start_date", nullable = false)
    var startDate: LocalDate = startDate
        protected set

    @Column(name = "end_date_exclusive", nullable = false)
    var endDateExclusive: LocalDate = endDateExclusive
        protected set

    init {
        require(startDate < endDateExclusive) { "배정 종료일은 시작일보다 늦어야 합니다" }
    }

    fun contains(date: LocalDate): Boolean = date >= startDate && date < endDateExclusive

    fun overlaps(other: RoomAssignment): Boolean =
        roomId == other.roomId && startDate < other.endDateExclusive && other.startDate < endDateExclusive
}
