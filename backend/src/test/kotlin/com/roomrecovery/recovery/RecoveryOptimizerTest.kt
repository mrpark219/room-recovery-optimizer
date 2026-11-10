package com.roomrecovery.recovery

import com.roomrecovery.availability.RoomUnavailability
import com.roomrecovery.inventory.Room
import com.roomrecovery.inventory.RoomType
import com.roomrecovery.reservation.Reservation
import com.roomrecovery.reservation.ReservationStatus
import com.roomrecovery.reservation.RoomAssignment
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate

class RecoveryOptimizerTest {
    private val day = LocalDate.of(2026, 11, 11)
    private val type = RoomType(1, 1, "standard", 2, 1, 100_000)
    private fun room(id: Long): Room {
        return Room(id, 1, 1, "$id")
    }
    private fun booking(id: Long, start: Int, end: Int, status: ReservationStatus = ReservationStatus.CHECKED_IN) =
        Reservation(
            id,
            1,
            1,
            Instant.ofEpochSecond(id),
            1,
            day.plusDays(start.toLong()),
            day.plusDays(end.toLong()),
            status
        )

    private fun assignment(id: Long, room: Long, start: Int, end: Int) =
        RoomAssignment(id, 1, id, room, day.plusDays(start.toLong()), day.plusDays(end.toLong()))

    private fun outage(room: Long, start: Int, end: Int) =
        RoomUnavailability.create(room, day.plusDays(start.toLong()), day.plusDays(end.toLong()), "repair")

    private fun common() = RecoveryInput(
        day, (1L..4L).map(::room), listOf(type),
        listOf(booking(1, 0, 4), booking(2, 0, 4), booking(3, 2, 4, ReservationStatus.CONFIRMED), booking(4, 0, 2)),
        listOf(assignment(1, 1, 0, 4), assignment(2, 2, 0, 4), assignment(3, 3, 2, 4), assignment(4, 4, 0, 2)),
        listOf(outage(1, 0, 4), outage(2, 0, 4))
    )

    @Test
    fun `여러 장애가 발생해도 정상 예약을 이동하지 않고 객실을 배정한다`() {
        // given
        val input = common()

        // when
        val result = RecoveryOptimizer.optimize(input)

        // then
        assertEquals(listOf(1L, 2L), result.urgentReservationIds)
        assertEquals(1, result.score.urgentServed)
        assertEquals(1, result.score.served)
        assertEquals(0L, result.score.additionalCost)
        assertEquals(2, result.score.moves)
        assertEquals(listOf(2L), result.unresolvedReservationIds)
        assertEquals(1L, result.placements.single().reservationId)
        assertEquals(listOf(3L, 4L), result.placements.single().segments.map { it.roomId })
        assertEquals(listOf(day, day.plusDays(2)), result.placements.single().segments.map { it.startDate })
    }

    @Test
    fun `여러 번 이동할 수 있고 같은 객실의 연속 숙박은 합쳐진다`() {
        // given
        val input = RecoveryInput(
            day, (1L..5L).map(::room), listOf(type), listOf(booking(1, 0, 4)),
            listOf(assignment(1, 1, 0, 4)),
            listOf(outage(1, 0, 4)) + (2L..5L).flatMap { id ->
                (0..3).filter { it != (id - 2).toInt() }.map { outage(id, it, it + 1) }
            })

        // when
        val placement = RecoveryOptimizer.optimize(input).placements.single()

        // then
        assertEquals(listOf(2L, 3L, 4L, 5L), placement.segments.map { it.roomId })
        assertEquals(4, placement.moves)
    }

    @Test
    fun `이용 불가 기간이 끝나면 기존 객실을 다시 사용할 수 있다`() {
        // given
        val input = RecoveryInput(
            day, listOf(room(1), room(2)), listOf(type), listOf(booking(1, 0, 4)),
            listOf(assignment(1, 1, 0, 4)), listOf(outage(1, 1, 3), outage(2, 0, 1), outage(2, 3, 4))
        )

        // when
        val placement = RecoveryOptimizer.optimize(input).placements.single()

        // then
        assertEquals(listOf(1L, 2L, 1L), placement.segments.map { it.roomId })
        assertEquals(2, placement.moves)
    }

    @Test
    fun `대체 객실이 없으면 미해결로 남고 미배정 예약은 대상이 아니다`() {
        // given
        val input = RecoveryInput(
            day, listOf(room(1)), listOf(type),
            listOf(booking(1, 0, 2), booking(2, 0, 2, ReservationStatus.CONFIRMED)),
            listOf(assignment(1, 1, 0, 2)), listOf(outage(1, 0, 2))
        )

        // when
        val plan = RecoveryOptimizer.optimize(input)

        // then
        assertTrue(plan.placements.isEmpty())
        assertEquals(listOf(1L), plan.unresolvedReservationIds)
    }

    @Test
    fun `취소되거나 재배정이 확정된 예약은 재배정 대상에서 제외된다`() {
        // given
        val base = common()
        val cancelled = base.reservations[0].copyForTest(ReservationStatus.CANCELLED)
        val input =
            base.copy(reservations = listOf(cancelled) + base.reservations.drop(1), fixedReservationIds = setOf(3))

        // when
        val plan = RecoveryOptimizer.optimize(input)

        // then
        assertEquals(listOf(2L), plan.placements.map { it.reservationId })
        assertTrue(plan.unresolvedReservationIds.isEmpty())
    }

    @Test
    fun `영향받은 예약은 취소 전까지 이용 가능한 기존 객실을 유지한다`() {
        // given
        val a = booking(1, 0, 2)
        val b = booking(2, 0, 2)
        val input = RecoveryInput(
            day, listOf(room(1), room(2), room(3)), listOf(type), listOf(a, b),
            listOf(
                assignment(1, 1, 0, 2), assignment(2, 3, 0, 1),
                RoomAssignment(3, 1, 2, 2, day.plusDays(1), day.plusDays(2))
            ),
            listOf(outage(1, 0, 2), outage(3, 0, 2))
        )

        // when
        val before = RecoveryOptimizer.optimize(input)

        // then
        assertTrue(1L in before.unresolvedReservationIds)

        // when
        val after =
            RecoveryOptimizer.optimize(input.copy(reservations = listOf(a, b.copyForTest(ReservationStatus.CANCELLED))))

        // then
        assertEquals(listOf(1L), after.placements.map { it.reservationId })
        assertEquals(listOf(2L), after.placements.single().segments.map { it.roomId })
    }

    @Test
    fun `취소되었거나 조회 범위에서 제외된 예약의 배정은 객실을 점유하지 않는다`() {
        val affected = booking(1, 0, 2)
        val cancelled = booking(2, -2, 0, ReservationStatus.CANCELLED)
        val input = RecoveryInput(
            day, listOf(room(1), room(2)), listOf(type), listOf(affected, cancelled),
            listOf(assignment(1, 1, 0, 2), assignment(2, 2, 0, 2)),
            listOf(outage(1, 0, 2))
        )

        for (reservations in listOf(input.reservations, listOf(affected))) {
            val plan = RecoveryOptimizer.optimize(input.copy(reservations = reservations))
            assertEquals(listOf(1L), plan.placements.map { it.reservationId })
            assertEquals(listOf(2L), plan.placements.single().segments.map { it.roomId })
            assertTrue(plan.unresolvedReservationIds.isEmpty())
        }
    }

    @Test
    fun `서로 다른 재배정안은 같은 날 같은 객실을 사용할 수 없다`() {
        // given
        val input = RecoveryInput(
            day, listOf(room(1), room(2), room(3)), listOf(type),
            listOf(booking(1, 0, 2), booking(2, 0, 2)),
            listOf(assignment(1, 1, 0, 2), assignment(2, 2, 0, 2)),
            listOf(outage(1, 0, 2), outage(2, 0, 2))
        )

        // when
        val result = RecoveryOptimizer.optimize(input)

        // then
        assertEquals(1, result.score.served)
        assertEquals(1, result.unresolvedReservationIds.size)
    }

    @Test
    fun `예약 순서 기준선도 같은 객실 자격과 가용성 규칙을 사용한다`() {
        // given
        val input = common()

        // when
        val greedy = RecoveryOptimizer.bookedAtGreedy(input)

        // then
        assertEquals(listOf(1L, 2L), greedy.urgentReservationIds)
        assertEquals(1, greedy.score.served)
        assertEquals(listOf(3L, 4L), greedy.placements.single().segments.map { it.roomId })
    }

    private fun Reservation.copyForTest(status: ReservationStatus) =
        Reservation(id, accommodationId, bookedRoomTypeId, bookedAt, guestCount, startDate, endDate, status)
}
