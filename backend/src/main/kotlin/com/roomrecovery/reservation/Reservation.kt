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
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long,
    @Column(name = "accommodation_id", nullable = false)
    val accommodationId: Long,
    @Column(name = "booked_room_type_id", nullable = false)
    val bookedRoomTypeId: Long,
    @Column(name = "booked_at", nullable = false)
    val bookedAt: Instant,
    @Column(name = "guest_count", nullable = false)
    val guestCount: Int,
    @Column(name = "start_date", nullable = false)
    val startDate: LocalDate,
    @Column(name = "`endDate`", nullable = false)
    val endDate: LocalDate, // 숙박에 미포함
    status: ReservationStatus
) {
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    var status: ReservationStatus = status
        protected set

    internal fun cancelForRecovery() {
        check(status != ReservationStatus.CANCELLED) { "이미 취소된 예약입니다" }
        status = ReservationStatus.CANCELLED
    }

    init {
        require(guestCount > 0) { "예약 인원은 1명 이상이어야 합니다" }
        require(startDate < endDate) { "체크아웃 날짜는 체크인 날짜보다 늦어야 합니다" }
    }

    fun totalNights(): Long = ChronoUnit.DAYS.between(startDate, endDate)

    fun remainingNights(onDate: LocalDate): Long = when {
        onDate >= endDate -> 0
        onDate <= startDate -> totalNights()
        else -> ChronoUnit.DAYS.between(onDate, endDate)
    }
}
