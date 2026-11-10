package com.roomrecovery.recovery.offer

import com.roomrecovery.availability.RoomUnavailability
import com.roomrecovery.inventory.Room
import com.roomrecovery.inventory.RoomType
import com.roomrecovery.recovery.RecoveryInput
import com.roomrecovery.reservation.Reservation
import com.roomrecovery.reservation.ReservationStatus
import com.roomrecovery.reservation.RoomAssignment
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate

class RecoveryCoordinatorTest {
    private val day = LocalDate.of(2026, 11, 11)
    private fun reservation(id: Long, room: Long, start: Int, end: Int) = Reservation(
        id, 1, 1, Instant.ofEpochSecond(id), 1, day.plusDays(start.toLong()), day.plusDays(end.toLong()),
        if (room == 201L) ReservationStatus.CONFIRMED else ReservationStatus.CHECKED_IN)
    private fun assignment(id: Long, room: Long, start: Int, end: Int) = RoomAssignment(
        id, 1, id, room, day.plusDays(start.toLong()), day.plusDays(end.toLong()))
    private fun outage(room: Long, start: Int, end: Int) = RoomUnavailability.create(
        room, day.plusDays(start.toLong()), day.plusDays(end.toLong()), "repair")
    private fun fixture(): RecoveryCoordinator {
        val rooms = listOf(101L, 102L, 201L, 202L).map { Room(it, 1, 1, "$it") }
        val reservations = listOf(reservation(1001, 101, 0, 4), reservation(1002, 102, 0, 4),
            reservation(1003, 201, 2, 4), reservation(1004, 202, 0, 2))
        val assignments = listOf(assignment(1001, 101, 0, 4), assignment(1002, 102, 0, 4),
            assignment(1003, 201, 2, 4), assignment(1004, 202, 0, 2))
        return RecoveryCoordinator(RecoveryInput(day, rooms, listOf(RoomType(1, 1, "standard", 2, 1, 100000)),
            reservations, assignments, listOf(outage(101, 0, 4), outage(102, 0, 4))))
    }

    @Test fun `accept whole offer and record only completed moves`() {
        val state = fixture()
        state.replan()
        val offer = state.offers.single()
        assertNull(offer.decision)
        assertEquals(listOf(201L, 202L), offer.segments.map { it.roomId })
        state.respond(offer.id, RecoveryDecision.ACCEPTED)
        assertEquals(RecoveryDecision.ACCEPTED, offer.decision)
        assertTrue(state.moveHistory.isEmpty())
        assertEquals(listOf(201L, 202L), state.assignments.filter { it.reservationId == 1001L }.map { it.roomId })
        assertThrows(IllegalArgumentException::class.java) { state.completeMove(offer.id, day.plusDays(2), 201, 202) }
        state.completeMove(offer.id, day, 101, 201)
        assertThrows(IllegalArgumentException::class.java) { state.completeMove(offer.id, day, 101, 201) }
        state.completeMove(offer.id, day.plusDays(2), 201, 202)
        assertEquals(2, state.moveHistory.size)
    }

    @Test fun `cancellation releases future occupancy and creates a new offer for unresolved guest`() {
        val state = fixture()
        state.replan()
        val offer = state.offers.single()
        state.respond(offer.id, RecoveryDecision.CANCEL_REQUESTED)
        assertEquals(RecoveryDecision.CANCEL_REQUESTED, offer.decision)
        assertFalse(state.assignments.any { it.reservationId == 1001L })
        assertEquals(1002L, state.offers.single { it.decision == null }.reservationId)
        assertEquals(listOf(201L, 202L), state.offers.single { it.decision == null }.segments.map { it.roomId })
        assertTrue(state.moveHistory.isEmpty())
        assertEquals(listOf(201L, 202L), state.assignments.filter { it.reservationId in setOf(1003L, 1004L) }.map { it.roomId })
    }

    @Test fun `unchanged plan retains offer while changed plan invalidates it`() {
        val state = RecoveryCoordinator(RecoveryInput(day,
            listOf(101L, 201L, 202L).map { Room(it, 1, 1, "$it") },
            listOf(RoomType(1, 1, "standard", 2, 1, 100000)),
            listOf(reservation(1, 101, 0, 4)), listOf(assignment(1, 101, 0, 4)),
            listOf(outage(101, 0, 4))))
        state.replan()
        val old = state.offers.single()
        state.replan()
        assertEquals(old.id, state.offers.single().id)
        state.replanForIncident(day, listOf(outage(101, 0, 4), outage(201, 0, 4)), setOf(1))
        val replacement = state.offers.single()
        assertNotEquals(old.id, replacement.id)
        assertEquals(202L, replacement.segments.single().roomId)
        assertThrows(IllegalArgumentException::class.java) { state.respond(old.id, RecoveryDecision.ACCEPTED) }
        assertNull(replacement.decision)
    }

    @Test fun `later incident retains earlier assignments and actual movement`() {
        val guest = reservation(1, 101, -1, 4)
        val state = RecoveryCoordinator(RecoveryInput(day,
            listOf(101L, 201L, 202L).map { Room(it, 1, 1, "$it") },
            listOf(RoomType(1, 1, "standard", 2, 1, 100000)), listOf(guest),
            listOf(assignment(1, 101, -1, 4)), listOf(outage(101, 0, 4))))
        state.replan()
        val first = state.offers.single()
        state.respond(first.id, RecoveryDecision.ACCEPTED)
        state.completeMove(first.id, day, 101, 201)
        state.replanForIncident(day.plusDays(1), listOf(outage(101, 0, 4), outage(201, 1, 4)), setOf(1))
        val next = state.offers.single { it.reservationId == 1L && it.decision == null }
        state.respond(next.id, RecoveryDecision.ACCEPTED)
        assertEquals(1, state.moveHistory.size)
        assertTrue(state.assignments.any { it.reservationId == 1L && it.roomId == 101L && it.endDate == day })
        assertTrue(state.assignments.any { it.reservationId == 1L && it.roomId == 201L && it.endDate == day.plusDays(1) })
        state.completeMove(next.id, day.plusDays(1), 201, 202)
        assertEquals(2, state.moveHistory.size)
    }
}
