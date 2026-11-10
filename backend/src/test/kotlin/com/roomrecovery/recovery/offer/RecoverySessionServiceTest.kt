package com.roomrecovery.recovery.offer

import com.roomrecovery.availability.RoomUnavailability
import com.roomrecovery.common.NotFound
import com.roomrecovery.inventory.Room
import com.roomrecovery.inventory.RoomType
import com.roomrecovery.recovery.RecoveryInput
import com.roomrecovery.recovery.RecoveryService
import com.roomrecovery.reservation.*
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionStatus
import org.springframework.transaction.support.TransactionTemplate
import java.time.Instant
import java.time.LocalDate
import java.util.*

class RecoverySessionServiceTest {
    @Test
    fun `저장 실패는 세션을 변경하지 않고 다른 숙소나 세션의 요청은 거부한다`() {
        // given
        val date = LocalDate.of(2026, 11, 10)
        val reservation =
            Reservation(1, 1, 1, Instant.EPOCH, 1, date.minusDays(1), date.plusDays(2), ReservationStatus.CHECKED_IN)
        val old = RoomAssignment(1, 1, 1, 1, date.minusDays(1), date.plusDays(2))
        val input = RecoveryInput(
            date, listOf(Room(1, 1, 1, "1"), Room(2, 1, 1, "2")),
            listOf(RoomType(1, 1, "기본", 2, 1, 100)), listOf(reservation), listOf(old),
            listOf(RoomUnavailability.create(1, date, date.plusDays(2), "수리"))
        )
        val recovery = mock(RecoveryService::class.java)
        val reservations = mock(ReservationRepository::class.java)
        val assignments = mock(RoomAssignmentRepository::class.java)
        val manager = mock(PlatformTransactionManager::class.java)
        val status = mock(TransactionStatus::class.java)
        `when`(recovery.loadInput(1, date)).thenReturn(input)
        `when`(manager.getTransaction(any())).thenReturn(status)
        `when`(reservations.findById(1)).thenReturn(Optional.of(reservation))
        `when`(assignments.findAllByAccommodationIdAndReservationIdAndEndDateAfter(1, 1, date)).thenReturn(listOf(old))
        doThrow(IllegalStateException("DB 오류")).`when`(assignments).flush()

        val service = RecoverySessionService(recovery, reservations, assignments, TransactionTemplate(manager))
        val session = service.create(1, date)
        val offerId = session.offers.single().id

        // when & then
        assertThrows(NotFound::class.java) { service.get(2, session.sessionId) }
        assertThrows(NotFound::class.java) { service.get(1, "이전 세션") }
        assertThrows(RecoveryConflict::class.java) { service.create(1, date) }
        assertThrows(IllegalStateException::class.java) {
            service.decide(
                1,
                session.sessionId,
                offerId,
                RecoveryDecision.ACCEPTED
            )
        }
        assertNull(service.get(1, session.sessionId).offers.single().decision)
        verify(manager).rollback(status)
    }
}
