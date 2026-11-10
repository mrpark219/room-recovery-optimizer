package com.roomrecovery.availability

import com.querydsl.core.types.dsl.BooleanExpression
import com.querydsl.jpa.JPAExpressions
import com.querydsl.jpa.impl.JPAQueryFactory
import com.roomrecovery.availability.QRoomUnavailability.roomUnavailability
import com.roomrecovery.inventory.QRoom.room
import java.time.LocalDate

class RoomUnavailabilityRepositoryCustomImpl(private val query: JPAQueryFactory) : RoomUnavailabilityRepositoryCustom {

    private fun inAccommodation(accommodationId: Long): BooleanExpression {
        return roomUnavailability.roomId.`in`(
            JPAExpressions.select(room.id)
                .from(room).where(room.accommodationId.eq(accommodationId))
        )
    }

    override fun findAllInAccommodation(accommodationId: Long): List<RoomUnavailability> {
        return query.selectFrom(roomUnavailability).where(inAccommodation(accommodationId))
            .orderBy(roomUnavailability.id.asc()).fetch()
    }

    override fun findInAccommodation(accommodationId: Long, id: Long): RoomUnavailability? {
        return query.selectFrom(roomUnavailability).where(inAccommodation(accommodationId), roomUnavailability.id.eq(id))
            .fetchOne()
    }

    override fun findRemainingInAccommodation(
        accommodationId: Long,
        referenceDate: LocalDate
    ): List<RoomUnavailability> {
        return query.selectFrom(roomUnavailability)
            .where(inAccommodation(accommodationId), roomUnavailability.endDate.gt(referenceDate)).fetch()
    }
}
