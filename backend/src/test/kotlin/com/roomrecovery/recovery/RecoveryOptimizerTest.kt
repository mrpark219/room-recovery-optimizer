package com.roomrecovery.recovery

import com.roomrecovery.availability.RoomUnavailability
import com.roomrecovery.inventory.Room
import com.roomrecovery.inventory.RoomType
import com.roomrecovery.reservation.Reservation
import com.roomrecovery.reservation.ReservationStatus
import com.roomrecovery.reservation.RoomAssignment
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate

class RecoveryOptimizerTest {
    private val day = LocalDate.of(2026, 11, 11)
    private val type = RoomType(1, 1, "standard", 2, 1, 100_000)
    private fun room(id: Long) = Room(id, 1, 1, "$id")
    private fun booking(id: Long, start: Int, end: Int, status: ReservationStatus = ReservationStatus.CHECKED_IN) =
        Reservation(id, 1, 1, Instant.ofEpochSecond(id), 1, day.plusDays(start.toLong()), day.plusDays(end.toLong()), status)
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

    @Test fun `multiple outages compete without moving normal reservations`() {
        val result = RecoveryOptimizer.optimize(common())
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

    @Test fun `unlimited moves and consecutive days merge`() {
        val input = RecoveryInput(day, (1L..5L).map(::room), listOf(type), listOf(booking(1, 0, 4)),
            listOf(assignment(1, 1, 0, 4)),
            listOf(outage(1, 0, 4)) + (2L..5L).flatMap { id ->
                (0..3).filter { it != (id - 2).toInt() }.map { outage(id, it, it + 1) }
            })
        val placement = RecoveryOptimizer.optimize(input).placements.single()
        assertEquals(listOf(2L, 3L, 4L, 5L), placement.segments.map { it.roomId })
        assertEquals(4, placement.moves)
    }

    @Test fun `original room can be used again after outage`() {
        val input = RecoveryInput(day, listOf(room(1), room(2)), listOf(type), listOf(booking(1, 0, 4)),
            listOf(assignment(1, 1, 0, 4)), listOf(outage(1, 1, 3), outage(2, 0, 1), outage(2, 3, 4)))
        val placement = RecoveryOptimizer.optimize(input).placements.single()
        assertEquals(listOf(1L, 2L, 1L), placement.segments.map { it.roomId })
        assertEquals(2, placement.moves)
    }

    @Test fun `unavailable recovery is unresolved and unassigned booking is not demand`() {
        val input = RecoveryInput(day, listOf(room(1)), listOf(type),
            listOf(booking(1, 0, 2), booking(2, 0, 2, ReservationStatus.CONFIRMED)),
            listOf(assignment(1, 1, 0, 2)), listOf(outage(1, 0, 2)))
        val plan = RecoveryOptimizer.optimize(input)
        assertTrue(plan.placements.isEmpty())
        assertEquals(listOf(1L), plan.unresolvedReservationIds)
    }

    @Test fun `cancelled and finalized bookings do not become recovery demands`() {
        val base = common()
        val cancelled = base.reservations[0].copyForTest(ReservationStatus.CANCELLED)
        val input = base.copy(reservations = listOf(cancelled) + base.reservations.drop(1), fixedReservationIds = setOf(3))
        val plan = RecoveryOptimizer.optimize(input)
        assertEquals(listOf(2L), plan.placements.map { it.reservationId })
        assertTrue(plan.unresolvedReservationIds.isEmpty())
    }

    @Test fun `affected reservation retains its usable room until cancellation`() {
        val a = booking(1, 0, 2)
        val b = booking(2, 0, 2)
        val input = RecoveryInput(
            day, listOf(room(1), room(2), room(3)), listOf(type), listOf(a, b),
            listOf(assignment(1, 1, 0, 2), assignment(2, 3, 0, 1),
                RoomAssignment(3, 1, 2, 2, day.plusDays(1), day.plusDays(2))),
            listOf(outage(1, 0, 2), outage(3, 0, 2))
        )
        val before = RecoveryOptimizer.optimize(input)
        assertTrue(1L in before.unresolvedReservationIds)
        val after = RecoveryOptimizer.optimize(input.copy(reservations = listOf(a, b.copyForTest(ReservationStatus.CANCELLED))))
        assertEquals(listOf(1L), after.placements.map { it.reservationId })
        assertEquals(listOf(2L), after.placements.single().segments.map { it.roomId })
    }

    @Test fun `two selected plans cannot use same room on the same night`() {
        val input = RecoveryInput(day, listOf(room(1), room(2), room(3)), listOf(type),
            listOf(booking(1, 0, 2), booking(2, 0, 2)),
            listOf(assignment(1, 1, 0, 2), assignment(2, 2, 0, 2)),
            listOf(outage(1, 0, 2), outage(2, 0, 2)))
        val result = RecoveryOptimizer.optimize(input)
        assertEquals(1, result.score.served)
        assertEquals(1, result.unresolvedReservationIds.size)
    }

    @Test fun `booking order baseline uses the same eligibility and availability rules`() {
        val input = common()
        val greedy = RecoveryOptimizer.bookedAtGreedy(input)
        assertEquals(listOf(1L, 2L), greedy.urgentReservationIds)
        assertEquals(1, greedy.score.served)
        assertEquals(listOf(3L, 4L), greedy.placements.single().segments.map { it.roomId })
    }

    private fun Reservation.copyForTest(status: ReservationStatus) =
        Reservation(id, accommodationId, bookedRoomTypeId, bookedAt, guestCount, startDate, endDate, status)
}
