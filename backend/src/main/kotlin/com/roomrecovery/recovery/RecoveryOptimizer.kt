package com.roomrecovery.recovery

import com.roomrecovery.availability.RoomUnavailability
import com.roomrecovery.inventory.Room
import com.roomrecovery.inventory.RoomType
import com.roomrecovery.reservation.Reservation
import com.roomrecovery.reservation.ReservationStatus
import com.roomrecovery.reservation.RoomAssignment
import java.math.BigDecimal
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** Exhaustive search intended for a small, already property- and window-scoped input.
 * Original assignments remain holds for all other reservations, including affected ones; this
 * deliberately excludes chains. Estimated extra cost is clamped at zero because grades do not
 * imply any ordering of rates. Acceptance is specific to a reservation and move date;
 * a decline or no answer suppresses that reservation's proposed moves for this round.
 * Contact recommendations compare one-step alternatives, not an optimal call sequence.
 */
object RecoveryOptimizer {
    fun optimize(input: RecoveryInput): RecoveryResult {
        val problem = Problem(input)
        val confirmed = problem.solve(false)!!
        val optimistic = problem.solve(true)!!
        val confirmedIds = confirmed.placements.map { it.reservationId }.toSet()
        val coverageReason = when {
            optimistic.score.urgentServed > confirmed.score.urgentServed -> ContactReason.URGENT
            optimistic.score.served > confirmed.score.served -> ContactReason.MORE_RESERVATIONS
            else -> null
        }
        val sameSet = if (coverageReason == null && input.considerSavings) {
            problem.solve(true, requiredIds = confirmedIds)
        } else null
        val improvement = coverageReason ?: if (
            sameSet != null && sameSet.score.additionalCost < confirmed.score.additionalCost
        ) ContactReason.COST_SAVINGS else null
        val target = if (improvement == ContactReason.COST_SAVINGS) sameSet!! else optimistic
        val recommendation = if (improvement == null) null else {
            target.placements.mapNotNull { it.consent }.filter { it !in input.consents }.distinct()
                .map { key ->
                    val required = if (improvement == ContactReason.COST_SAVINGS) confirmedIds else null
                    key to problem.solve(true, key, requiredIds = required)?.score
                }
                .sortedWith(compareBy<Pair<ConsentKey, RecoveryScore?>>(
                    { if (it.first.reservationId in problem.urgentIds) 0 else 1 },
                    { it.second?.urgentServed ?: -1 },
                    { it.second?.served ?: -1 },
                    { if (improvement == ContactReason.COST_SAVINGS) {
                        it.second?.additionalCost?.negate() ?: BigDecimal.ZERO
                    } else BigDecimal.ZERO },
                    { it.first.moveDate }, { it.first.reservationId }
                )).firstOrNull()?.let { ContactRecommendation(it.first, improvement) }
        }
        val urgentUnresolved = problem.urgentIds.intersect(confirmed.unresolvedReservationIds.toSet())
        val attention = (problem.immediateAttention + urgentUnresolved).distinct().sorted()
        return RecoveryResult(confirmed, recommendation, attention)
    }

    /** Same candidates and constraints as optimize, but irrevocably chooses in booking order. */
    fun bookedAtGreedy(input: RecoveryInput): RecoveryPlan = Problem(input).solve(false, greedy = true)!!

    private class Problem(val input: RecoveryInput) {
        private val types = input.roomTypes.associateBy { it.id }
        private val rooms = input.rooms.associateBy { it.id }
        private val assignments = input.assignments.groupBy { it.reservationId }
        private fun overlaps(a: LocalDate, b: LocalDate, c: LocalDate, d: LocalDate) = a < d && c < b
        private fun blocked(room: Long, start: LocalDate, end: LocalDate) =
            input.unavailabilities.any { it.overlaps(room, start, end) }

        // Stale confirmed bookings cannot be moved in the past.
        private fun start(r: Reservation) = maxOf(input.referenceDate, r.startDate)

        private fun urgent(r: Reservation) = r.status == ReservationStatus.CHECKED_IN &&
            input.referenceDate >= r.startDate && input.referenceDate < r.endDateExclusive &&
            blocked(r.occupiedRoomId!!, input.referenceDate, input.referenceDate.plusDays(1))

        val immediateAttention = input.reservations.filter {
            it.status == ReservationStatus.CHECKED_IN && input.referenceDate >= it.endDateExclusive &&
                blocked(it.occupiedRoomId!!, input.referenceDate, input.referenceDate.plusDays(1))
        }.map { it.id }.sorted()
        val urgentIds = input.reservations.filter { urgent(it) }.map { it.id }.toSet()
        private val demands = input.reservations.filter { r ->
            val affected = assignments[r.id].orEmpty().any { assignment ->
                val from = maxOf(start(r), assignment.startDate)
                val to = minOf(r.endDateExclusive, assignment.endDateExclusive)
                from < to && blocked(assignment.roomId, from, to)
            }
            val unassigned = assignments[r.id].isNullOrEmpty() && r.status == ReservationStatus.CONFIRMED
            r.endDateExclusive > input.referenceDate && start(r) < r.endDateExclusive &&
                (urgent(r) || unassigned || affected)
        }.sortedWith(compareBy<Reservation>({ it.bookedAt }, { it.id }))

        private fun candidates(r: Reservation): List<RecoveryPlacement> {
            val from = start(r)
            val end = r.endDateExclusive
            val booked = requireNotNull(types[r.bookedRoomTypeId]) { "Missing booked room type ${r.bookedRoomTypeId}" }
            val eligible = input.rooms.filter { room ->
                room.accommodationId == r.accommodationId &&
                    types[room.roomTypeId]?.canAccommodate(booked, r.guestCount) == true
            }.sortedBy { it.id }
            fun free(s: RecoverySegment): Boolean {
                if (blocked(s.roomId, s.startDate, s.endDateExclusive)) return false
                if (input.assignments.any {
                    it.reservationId != r.id && it.roomId == s.roomId &&
                        overlaps(s.startDate, s.endDateExclusive, it.startDate, it.endDateExclusive)
                }) return false
                return input.reservations.none { other ->
                    other.id != r.id && other.status == ReservationStatus.CHECKED_IN &&
                        other.occupiedRoomId == s.roomId && input.referenceDate >= other.startDate &&
                        input.referenceDate < other.endDateExclusive &&
                        overlaps(s.startDate, s.endDateExclusive, input.referenceDate, input.referenceDate.plusDays(1))
                }
            }
            fun placement(segments: List<RecoverySegment>): RecoveryPlacement? {
                if (segments.any { !free(it) }) return null
                val checked = r.status == ReservationStatus.CHECKED_IN
                val consent = when {
                    checked && segments.first().roomId != r.occupiedRoomId -> ConsentKey(r.id, from)
                    segments.size == 2 -> ConsentKey(r.id, segments[1].startDate)
                    else -> null
                }
                val moves = if (checked && segments.first().roomId != r.occupiedRoomId) 1 else segments.size - 1
                val candidateRate = segments.fold(BigDecimal.ZERO) { sum, segment ->
                    val rate = types.getValue(rooms.getValue(segment.roomId).roomTypeId).nightlyRate
                    val nights = ChronoUnit.DAYS.between(segment.startDate, segment.endDateExclusive)
                    sum + rate.multiply(BigDecimal.valueOf(nights))
                }
                val originalRate = booked.nightlyRate.multiply(BigDecimal.valueOf(ChronoUnit.DAYS.between(from, end)))
                return RecoveryPlacement(r.id, segments, (candidateRate - originalRate).max(BigDecimal.ZERO), moves, consent)
            }
            val result = mutableListOf<RecoveryPlacement>()
            for (room in eligible) placement(listOf(RecoverySegment(room.id, from, end)))?.let(result::add)
            var date = from.plusDays(1)
            while (date < end) {
                for (first in eligible) for (second in eligible) {
                    if (first.id == second.id ||
                        (r.status == ReservationStatus.CHECKED_IN && first.id != r.occupiedRoomId)
                    ) continue
                    val segments = listOf(RecoverySegment(first.id, from, date), RecoverySegment(second.id, date, end))
                    placement(segments)?.let(result::add)
                }
                date = date.plusDays(1)
            }
            return result.sortedWith(compareBy<RecoveryPlacement>(
                { it.additionalCost }, { it.moves },
                { it.segments.joinToString("|") { s -> "%020d:%s".format(s.roomId, s.startDate) } }
            ))
        }
        private val choices = demands.associate { it.id to candidates(it) }
        fun solve(
            optimistic: Boolean,
            refused: ConsentKey? = null,
            greedy: Boolean = false,
            requiredIds: Set<Long>? = null
        ): RecoveryPlan? {
            var best: RecoveryPlan? = null
            val searchDemands = if (requiredIds == null) demands else demands.filter { it.id in requiredIds }
            if (requiredIds != null && searchDemands.size != requiredIds.size) return null

            fun validConsent(p: RecoveryPlacement): Boolean {
                val key = p.consent ?: return true
                if (refused?.reservationId == key.reservationId) return false
                if (input.consents.any { (k, v) ->
                    k.reservationId == key.reservationId && v != ConsentOutcome.ACCEPTED
                }) return false
                return when (input.consents[key]) {
                    ConsentOutcome.ACCEPTED -> true
                    null -> optimistic
                    else -> false
                }
            }
            fun compatible(p: RecoveryPlacement, selected: List<RecoveryPlacement>) = p.segments.none { segment ->
                selected.any { placement -> placement.segments.any { other ->
                    segment.roomId == other.roomId && overlaps(
                        segment.startDate, segment.endDateExclusive, other.startDate, other.endDateExclusive
                    )
                } }
            }
            fun plan(selected: List<RecoveryPlacement>): RecoveryPlan {
                val ids = selected.map { it.reservationId }.toSet()
                val score = RecoveryScore(
                    demands.count { urgent(it) && it.id in ids }, selected.size,
                    selected.fold(BigDecimal.ZERO) { sum, p -> sum + p.additionalCost },
                    selected.sumOf { it.moves }
                )
                return RecoveryPlan(selected.toList(), demands.filter { it.id !in ids }.map { it.id }, score)
            }
            fun tieKey(plan: RecoveryPlan) = plan.placements.sortedBy { it.reservationId }.joinToString("|") {
                val segments = it.segments.joinToString(",") { s -> "%020d:%s".format(s.roomId, s.startDate) }
                "%020d:%s".format(it.reservationId, segments)
            }
            fun better(a: RecoveryPlan, b: RecoveryPlan): Boolean {
                val x = a.score
                val y = b.score
                if (x.urgentServed != y.urgentServed) return x.urgentServed > y.urgentServed
                if (x.served != y.served) return x.served > y.served
                if (x.additionalCost.compareTo(y.additionalCost) != 0) return x.additionalCost < y.additionalCost
                if (x.moves != y.moves) return x.moves < y.moves
                return tieKey(a) < tieKey(b)
            }
            fun visit(index: Int, selected: MutableList<RecoveryPlacement>) {
                if (index == searchDemands.size) {
                    val candidate = plan(selected)
                    if (best == null || better(candidate, best!!)) best = candidate
                    return
                }
                val options = choices.getValue(searchDemands[index].id)
                    .filter { validConsent(it) && compatible(it, selected) }
                for (placement in options) {
                    selected.add(placement)
                    visit(index + 1, selected)
                    selected.removeAt(selected.lastIndex)
                    if (greedy) return
                }
                if (requiredIds == null) visit(index + 1, selected)
            }
            visit(0, mutableListOf())
            return best
        }
    }
}
