package com.roomrecovery.recovery

import com.roomrecovery.availability.RoomUnavailability
import com.roomrecovery.inventory.Room
import com.roomrecovery.inventory.RoomType
import com.roomrecovery.reservation.Reservation
import com.roomrecovery.reservation.ReservationStatus
import com.roomrecovery.reservation.RoomAssignment
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

class RecoveryOptimizerTest {
    private val day = LocalDate.of(2026, 1, 1)
    private val type = RoomType(1, 1, "standard", 2, 1, BigDecimal("100"))
    private fun room(id: Long, typeId: Long = 1) = Room(id, 1, typeId, "room$id")
    private fun booking(id: Long, start: Int = 0, end: Int = 2, occupied: Long? = null, count: Int = 1) = Reservation(id, 1, 1, occupied, Instant.ofEpochSecond(id), count, day.plusDays(start.toLong()), day.plusDays(end.toLong()), if (occupied == null) ReservationStatus.CONFIRMED else ReservationStatus.CHECKED_IN)
    private fun outage(room: Long, start: Int, end: Int) = RoomUnavailability.create(room, day.plusDays(start.toLong()), day.plusDays(end.toLong()), "repair")
    private fun input(rooms: List<Room>, bookings: List<Reservation>, outages: List<RoomUnavailability> = emptyList(), assignments: List<RoomAssignment> = emptyList(), consent: Map<ConsentKey, ConsentOutcome> = emptyMap(), date: LocalDate = day, types: List<RoomType> = listOf(type), savings: Boolean = false) = RecoveryInput(date, rooms, types, bookings, assignments, outages, consent, savings)
    private fun assignment(id: Long, booking: Long, room: Long, start: Int, end: Int) = RoomAssignment(id, 1, booking, room, day.plusDays(start.toLong()), day.plusDays(end.toLong()))

    @Test fun `split requires consent for exact date and never double books`() {
        val key = ConsentKey(1, day.plusDays(1))
        val base = input(listOf(room(1), room(2)), listOf(booking(1)), listOf(outage(1, 1, 2), outage(2, 0, 1)))
        val result = RecoveryOptimizer.optimize(base)
        assertEquals(0, result.plan.score.served)
        assertEquals(key, result.recommendation?.consent)
        assertEquals(0, RecoveryOptimizer.optimize(base.copy(consents = mapOf(ConsentKey(1, day) to ConsentOutcome.ACCEPTED))).plan.score.served)
        val accepted = RecoveryOptimizer.optimize(base.copy(consents = mapOf(key to ConsentOutcome.ACCEPTED)))
        assertEquals(listOf(1L, 2L), accepted.plan.placements.single().segments.map { it.roomId })
        assertNull(RecoveryOptimizer.optimize(base.copy(consents = mapOf(key to ConsentOutcome.DECLINED))).recommendation)
    }

    @Test fun `checked in guest uses remaining nights and immediate move requires consent`() {
        val key = ConsentKey(1, day.plusDays(1))
        val base = input(listOf(room(1), room(2)), listOf(booking(1, end = 3, occupied = 1)), listOf(outage(1, 1, 3)), listOf(assignment(1, 1, 1, 0, 3)), date = day.plusDays(1))
        val pending = RecoveryOptimizer.optimize(base)
        assertEquals(0, pending.plan.score.served)
        assertEquals(ContactReason.URGENT, pending.recommendation?.reason)
        val p = RecoveryOptimizer.optimize(base.copy(consents = mapOf(key to ConsentOutcome.ACCEPTED))).plan.placements.single()
        assertEquals(day.plusDays(1), p.segments.single().startDate)
        assertEquals(1, p.moves)
    }

    @Test fun `fixed assignments and actual occupied room prevent collisions`() {
        val fixed = booking(2, occupied = 1)
        val base = input(listOf(room(1), room(2)), listOf(booking(1), fixed), assignments = listOf(assignment(1, 2, 2, 0, 2)))
        val result = RecoveryOptimizer.optimize(base)
        assertEquals(0, result.plan.score.served)
        val two = input(listOf(room(1)), listOf(booking(1), booking(2)))
        assertEquals(1, RecoveryOptimizer.optimize(two).plan.score.served)
    }

    @Test fun `eligibility and lower priced higher grade are handled`() {
        val upgrade = RoomType(2, 1, "upgrade", 3, 2, BigDecimal("50"))
        val small = RoomType(3, 1, "small", 1, 2, BigDecimal("10"))
        val result = RecoveryOptimizer.optimize(input(listOf(room(1, 2), room(2, 3)), listOf(booking(1, count = 2)), types = listOf(type, upgrade, small)))
        assertEquals(1L, result.plan.placements.single().segments.single().roomId)
        assertEquals(0, result.plan.score.additionalCost.compareTo(BigDecimal.ZERO))
    }

    @Test fun `savings contact is optional`() {
        val costly = RoomType(2, 1, "costly", 2, 2, BigDecimal("200"))
        val key = ConsentKey(1, day.plusDays(1))
        val base = input(listOf(room(1), room(2), room(3, 2)), listOf(booking(1)), listOf(outage(1, 1, 2), outage(2, 0, 1)), types = listOf(type, costly))
        assertNull(RecoveryOptimizer.optimize(base).recommendation)
        assertEquals(ContactReason.COST_SAVINGS, RecoveryOptimizer.optimize(base.copy(considerSavings = true)).recommendation?.reason)
        assertNull(RecoveryOptimizer.optimize(base.copy(considerSavings = true, consents = mapOf(key to ConsentOutcome.NO_ANSWER))).recommendation)
    }
    @Test fun `interchangeable callers still produce a recommendation`() {
        val base = input(
            listOf(room(1), room(2)), listOf(booking(1), booking(2)),
            listOf(outage(1, 1, 2), outage(2, 0, 1))
        )
        val result = RecoveryOptimizer.optimize(base)
        assertEquals(0, result.plan.score.served)
        assertEquals(ContactReason.MORE_RESERVATIONS, result.recommendation?.reason)
        assertEquals(ConsentKey(1, day.plusDays(1)), result.recommendation?.consent)
    }

    @Test fun `expired reservations do not create demands`() {
        val result = RecoveryOptimizer.optimize(input(listOf(room(1)), listOf(booking(1)), date = day.plusDays(2)))
        assertTrue(result.plan.placements.isEmpty())
        assertTrue(result.plan.unresolvedReservationIds.isEmpty())
    }
    @Test fun `exact search serves constrained demand that greedy blocks`() {
        val large = RoomType(2, 1, "large", 2, 1, BigDecimal("100"))
        val small = RoomType(3, 1, "small", 1, 1, BigDecimal("100"))
        val base = input(
            listOf(room(1, 2), room(2, 3)), listOf(booking(1), booking(2, count = 2)),
            types = listOf(type, large, small)
        )
        assertEquals(1, RecoveryOptimizer.bookedAtGreedy(base).score.served)
        assertEquals(2, RecoveryOptimizer.optimize(base).plan.score.served)
    }
}
