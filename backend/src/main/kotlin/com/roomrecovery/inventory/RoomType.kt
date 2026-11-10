package com.roomrecovery.inventory

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "room_type")
class RoomType(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long,
    @Column(name = "accommodation_id", nullable = false)
    val accommodationId: Long,
    @Column(nullable = false)
    val name: String,
    @Column(nullable = false)
    val capacity: Int,
    @Column(nullable = false)
    val grade: Int,
    @Column(name = "nightly_rate", nullable = false)
    val nightlyRate: Int
) {
    init {
        require(capacity > 0) { "객실 정원은 1명 이상이어야 합니다" }
        require(grade >= 0) { "객실 등급은 0 이상이어야 합니다" }
        require(nightlyRate >= 0) { "객실 기준가는 0 이상이어야 합니다" }
    }

    fun canAccommodate(bookedType: RoomType, guestCount: Int): Boolean =
        guestCount > 0 && accommodationId == bookedType.accommodationId &&
            capacity >= guestCount && grade >= bookedType.grade
}
