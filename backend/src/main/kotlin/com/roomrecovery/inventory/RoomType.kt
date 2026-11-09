package com.roomrecovery.inventory

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal

@Entity
@Table(name = "room_type")
class RoomType(
    id: Long,
    accommodationId: Long,
    name: String,
    capacity: Int,
    grade: Int,
    nightlyRate: BigDecimal
    ) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = id
        protected set

    @Column(name = "accommodation_id", nullable = false)
    var accommodationId: Long = accommodationId
        protected set

    @Column(nullable = false)
    var name: String = name
        protected set

    @Column(nullable = false)
    var capacity: Int = capacity
        protected set

    @Column(nullable = false)
    var grade: Int = grade
        protected set

    @Column(name = "nightly_rate", nullable = false, precision = 12, scale = 2)
    var nightlyRate: BigDecimal = nightlyRate
        protected set

    init {
        require(capacity > 0) { "객실 정원은 1명 이상이어야 합니다" }
        require(grade >= 0) { "객실 등급은 0 이상이어야 합니다" }
        require(nightlyRate.signum() >= 0) { "객실 기준가는 0 이상이어야 합니다" }
    }

    fun canAccommodate(bookedType: RoomType, guestCount: Int): Boolean =
        guestCount > 0 && accommodationId == bookedType.accommodationId &&
            capacity >= guestCount && grade >= bookedType.grade
}
