package com.roomrecovery.reservation

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@Entity
@Table(name = "reservation")
class Reservation(
    id: Long,
    accommodationId: Long,
    bookedRoomTypeId: Long,
    occupiedRoomId: Long?,
    bookedAt: Instant,
    guestCount: Int,
    startDate: LocalDate,
    endDateExclusive: LocalDate,
    status: ReservationStatus
    ) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = id
        protected set

    @Column(name = "accommodation_id", nullable = false)
    var accommodationId: Long = accommodationId
        protected set

    @Column(name = "booked_room_type_id", nullable = false)
    var bookedRoomTypeId: Long = bookedRoomTypeId
        protected set

    @Column(name = "occupied_room_id")
    var occupiedRoomId: Long? = occupiedRoomId
        protected set

    @Column(name = "booked_at", nullable = false)
    var bookedAt: Instant = bookedAt
        protected set

    @Column(name = "guest_count", nullable = false)
    var guestCount: Int = guestCount
        protected set

    @Column(name = "start_date", nullable = false)
    var startDate: LocalDate = startDate
        protected set

    @Column(name = "end_date_exclusive", nullable = false)
    var endDateExclusive: LocalDate = endDateExclusive
        protected set

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: ReservationStatus = status
        protected set

    init {
        require(guestCount > 0) { "예약 인원은 1명 이상이어야 합니다" }
        require(startDate < endDateExclusive) { "체크아웃 날짜는 체크인 날짜보다 늦어야 합니다" }
        require((status == ReservationStatus.CHECKED_IN) == (occupiedRoomId != null)) {
            "투숙 중인 예약에만 현재 객실이 있어야 합니다"
        }
    }

    fun totalNights(): Long = ChronoUnit.DAYS.between(startDate, endDateExclusive)

    fun remainingNights(onDate: LocalDate): Long = when {
        onDate >= endDateExclusive -> 0
        onDate <= startDate -> totalNights()
        else -> ChronoUnit.DAYS.between(onDate, endDateExclusive)
    }
}
