package com.roomrecovery.availability

import jakarta.persistence.*
import java.time.LocalDate

@Entity
@Table(name = "room_unavailability")
class RoomUnavailability private constructor(
    id: Long?, roomId: Long, startDate: LocalDate, endDateExclusive: LocalDate, reason: String
) {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = id
        protected set

    @Column(name = "room_id")
    var roomId: Long = roomId
        protected set

    @Column(name = "start_date")
    var startDate: LocalDate = startDate
        protected set

    @Column(name = "end_date_exclusive")
    var endDateExclusive: LocalDate = endDateExclusive
        protected set

    var reason: String = reason
        protected set

    fun update(roomId: Long, startDate: LocalDate, endDateExclusive: LocalDate, reason: String) {
        validate(roomId, startDate, endDateExclusive, reason)
        this.roomId = roomId
        this.startDate = startDate
        this.endDateExclusive = endDateExclusive
        this.reason = reason
    }

    fun overlaps(roomId: Long, startDate: LocalDate, endDateExclusive: LocalDate): Boolean {
        require(startDate < endDateExclusive) { "조회 종료일은 시작일보다 늦어야 합니다" }
        return this.roomId == roomId && this.startDate < endDateExclusive && startDate < this.endDateExclusive
    }

    companion object {
        fun create(roomId: Long, startDate: LocalDate, endDateExclusive: LocalDate, reason: String): RoomUnavailability {
            validate(roomId, startDate, endDateExclusive, reason)
            return RoomUnavailability(null, roomId, startDate, endDateExclusive, reason)
        }

        fun validate(roomId: Long, startDate: LocalDate, endDateExclusive: LocalDate, reason: String) {
            require(roomId > 0) { "객실 ID는 양수여야 합니다" }
            require(startDate < endDateExclusive) { "이용 불가 종료일은 시작일보다 늦어야 합니다" }
            require(reason.isNotBlank() && reason.length <= 500) { "사유는 공백이 아닌 500자 이하여야 합니다" }
        }
    }
}
