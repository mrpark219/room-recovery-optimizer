package com.roomrecovery.availability

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDate

@Entity
@Table(name = "room_unavailability")
class RoomUnavailability private constructor(
    roomId: Long,
    startDate: LocalDate,
    endDate: LocalDate, // 숙박에 미포함
    reason: String
) {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        protected set

    @Column(name = "room_id", nullable = false)
    var roomId: Long = roomId
        protected set

    @Column(name = "start_date", nullable = false)
    var startDate: LocalDate = startDate
        protected set

    @Column(name = "`endDate`", nullable = false)
    var endDate: LocalDate = endDate // 숙박에 미포함
        protected set

    @Column(nullable = false)
    var reason: String = reason
        protected set

    init {
        validate(roomId, startDate, endDate, reason)
    }

    fun update(roomId: Long, startDate: LocalDate, endDate: LocalDate, reason: String) {
        validate(roomId, startDate, endDate, reason)
        this.roomId = roomId
        this.startDate = startDate
        this.endDate = endDate
        this.reason = reason
    }

    fun overlaps(roomId: Long, startDate: LocalDate, endDate: LocalDate): Boolean {
        require(startDate < endDate) { "조회 종료일은 시작일보다 늦어야 합니다" }
        return this.roomId == roomId && this.startDate < endDate && startDate < this.endDate
    }

    companion object {
        fun create(roomId: Long, startDate: LocalDate, endDate: LocalDate, reason: String): RoomUnavailability =
            RoomUnavailability(roomId, startDate, endDate, reason)

        fun validate(roomId: Long, startDate: LocalDate, endDate: LocalDate, reason: String) {
            require(roomId > 0) { "객실 ID는 양수여야 합니다" }
            require(startDate < endDate) { "이용 불가 종료일은 시작일보다 늦어야 합니다" }
            require(reason.isNotBlank() && reason.length <= 500) { "사유는 공백이 아닌 500자 이하여야 합니다" }
        }
    }
}
