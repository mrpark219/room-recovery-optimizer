package com.roomrecovery.recovery

import com.roomrecovery.reservation.Reservation
import com.roomrecovery.reservation.ReservationStatus
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** 작은 단일 숙소 입력에서 남은 숙박일의 전체 객실 조합을 탐색한다.
 * 정상 배정과 확정된 복구 배정은 유지하며, 취소된 예약의 점유는 제외한다.
 * 결과는 고객별 전체 이동 계획이며 실제 동의나 이동 이력을 기록하지 않는다.
 */
object RecoveryOptimizer {
    fun optimize(input: RecoveryInput): RecoveryPlan {
        return Search(input).solve()
    }

    /** 같은 후보와 제약을 사용해 예약 접수 순서대로 최선의 후보를 확정한다. */
    fun bookedAtGreedy(input: RecoveryInput): RecoveryPlan {
        return Search(input, greedy = true).solve()
    }

    private class Search(private val input: RecoveryInput, private val greedy: Boolean = false) {
        private val reservations = input.reservations.associateBy { it.id }
        private val roomTypes = input.roomTypes.associateBy { it.id }
        private val assignments = input.assignments.groupBy { it.reservationId }

        private fun start(reservation: Reservation): LocalDate {
            return maxOf(input.referenceDate, reservation.startDate)
        }
        private fun blocked(roomId: Long, date: LocalDate): Boolean {
            return input.unavailabilities.any { it.overlaps(roomId, date, date.plusDays(1)) }
        }

        private fun currentRoom(reservation: Reservation): Long? {
            return assignments[reservation.id].orEmpty()
                .filter { it.contains(input.referenceDate) }.singleOrNull()?.roomId
        }

        private fun isUrgent(reservation: Reservation): Boolean {
            if (reservation.status != ReservationStatus.CHECKED_IN) {
                return false
            }

            val roomId = currentRoom(reservation) ?: return false

            return blocked(roomId, input.referenceDate)
        }

        // 기존 배정의 남은 기간이 이용 불가와 겹치는 예약만 복구한다.
        private val demands = input.reservations.filter { reservation ->
            reservation.status != ReservationStatus.CANCELLED &&
                    reservation.id !in input.fixedReservationIds && start(reservation) < reservation.endDate &&
                    assignments[reservation.id].orEmpty().any { assignment ->
                        val from = maxOf(start(reservation), assignment.startDate)
                        val end = minOf(reservation.endDate, assignment.endDate)
                        from < end && input.unavailabilities.any { it.overlaps(assignment.roomId, from, end) }
                    }
        }.sortedWith(compareBy<Reservation>({ it.bookedAt }, { it.id }))

        // 아직 확정되지 않은 복구 대상의 기존 배정도 다른 예약의 점유로 유지한다.
        private val activeAssignments = input.assignments.filter { assignment ->
            reservations[assignment.reservationId]?.let { it.status != ReservationStatus.CANCELLED } == true
        }
        private val selectedNights = mutableSetOf<Pair<Long, LocalDate>>()
        private var best: RecoveryPlan? = null

        fun solve(): RecoveryPlan {
            visitReservation(0, mutableListOf())

            return checkNotNull(best)
        }

        private fun visitReservation(index: Int, selected: MutableList<RecoveryPlacement>) {
            if (index == demands.size) {
                val candidate = makePlan(selected)
                if (best == null || better(candidate, best!!)) {
                    best = candidate
                }
                return
            }

            // 남은 모든 예약을 수용해도 현재 최선의 수용 건수에 미치지 못하면 중단한다.
            val bestPlan = best
            if (bestPlan != null) {
                val urgentSelected = selected.count { it.reservationId in urgentIds }
                val urgentRemaining = demands.drop(index).count { it.id in urgentIds }
                if (urgentSelected + urgentRemaining < bestPlan.score.urgentServed) {
                    return
                }
                if (urgentSelected + urgentRemaining == bestPlan.score.urgentServed &&
                    selected.size + demands.size - index < bestPlan.score.served
                ) {
                    return
                }
            }

            val reservation = demands[index]
            val current = if (reservation.status == ReservationStatus.CHECKED_IN) currentRoom(reservation) else null
            if (reservation.status != ReservationStatus.CHECKED_IN || current != null) {
                val booked = requireNotNull(roomTypes[reservation.bookedRoomTypeId])
                val eligible = input.rooms.filter { room ->
                    room.accommodationId == reservation.accommodationId &&
                            roomTypes[room.roomTypeId]?.canAccommodate(booked, reservation.guestCount) == true
                }.sortedBy { it.id }

                val nights = ChronoUnit.DAYS.between(start(reservation), reservation.endDate).toInt()
                val options = (0 until nights).map { offset ->
                    val date = start(reservation).plusDays(offset.toLong())
                    eligible.filter { room ->
                        !blocked(room.id, date) && activeAssignments.none {
                            it.reservationId != reservation.id && it.roomId == room.id && it.contains(date)
                        }
                    }
                }

                if (options.all { it.isNotEmpty() }) {
                    var greedyChoice: RecoveryPlacement? = null
                    val roomsByNight = mutableListOf<Long>()
                    fun visitNight(night: Int, cost: Long, moves: Int) {
                        if (night == nights) {
                            val segments = mutableListOf<RecoverySegment>()
                            roomsByNight.forEachIndexed { offset, roomId ->
                                val date = start(reservation).plusDays(offset.toLong())
                                val last = segments.lastOrNull()
                                if (last != null && last.roomId == roomId) {
                                    segments[segments.lastIndex] = last.copy(endDate = date.plusDays(1))
                                } else segments.add(RecoverySegment(roomId, date, date.plusDays(1)))
                            }

                            val original = booked.nightlyRate.toLong() * nights
                            val placement = RecoveryPlacement(
                                reservation.id, segments, (cost - original).coerceAtLeast(0), moves
                            )

                            if (greedy) {
                                val previous = greedyChoice
                                if (previous == null || placementOrder(placement, previous) < 0) {
                                    greedyChoice = placement
                                }
                            } else {
                                selected.add(placement)
                                visitReservation(index + 1, selected)
                                selected.removeAt(selected.lastIndex)
                            }
                            return
                        }
                        val date = start(reservation).plusDays(night.toLong())

                        for (room in options[night]) {
                            val key = room.id to date
                            if (!selectedNights.add(key)) {
                                continue
                            }
                            val previous = roomsByNight.lastOrNull() ?: current
                            roomsByNight.add(room.id)
                            val rate = roomTypes.getValue(room.roomTypeId).nightlyRate.toLong()
                            visitNight(
                                night + 1,
                                cost + rate,
                                moves + if (previous != null && previous != room.id) 1 else 0
                            )
                            roomsByNight.removeAt(roomsByNight.lastIndex)
                            selectedNights.remove(key)
                        }
                    }
                    visitNight(0, 0, 0)

                    val choice = greedyChoice
                    if (greedy && choice != null) {
                        val occupied = choice.segments.flatMap { segment ->
                            (0 until ChronoUnit.DAYS.between(segment.startDate, segment.endDate).toInt())
                                .map { segment.roomId to segment.startDate.plusDays(it.toLong()) }
                        }
                        selectedNights.addAll(occupied)
                        selected.add(choice)
                        visitReservation(index + 1, selected)
                        selected.removeAt(selected.lastIndex)
                        selectedNights.removeAll(occupied.toSet())
                        return
                    }
                }
            }

            visitReservation(index + 1, selected)
        }

        private val urgentIds = demands.filter { isUrgent(it) }.map { it.id }.toSet()

        private fun makePlan(selected: List<RecoveryPlacement>): RecoveryPlan {
            val servedIds = selected.map { it.reservationId }.toSet()

            return RecoveryPlan(
                selected.toList(), demands.filter { it.id !in servedIds }.map { it.id },
                RecoveryScore(
                    selected.count { it.reservationId in urgentIds }, selected.size,
                    selected.sumOf { it.additionalCost }, selected.sumOf { it.moves }),
                urgentIds.sorted()
            )
        }

        private fun placementOrder(a: RecoveryPlacement, b: RecoveryPlacement): Int {
            val costOrder = a.additionalCost.compareTo(b.additionalCost)
            if (costOrder != 0) {
                return costOrder
            }
            val moveOrder = a.moves.compareTo(b.moves)
            if (moveOrder != 0) {
                return moveOrder
            }
            fun key(placement: RecoveryPlacement): String {
                return placement.segments.joinToString("|") {
                    "%020d:%s".format(it.roomId, it.startDate)
                }
            }
            return key(a).compareTo(key(b))
        }

        private fun better(candidate: RecoveryPlan, best: RecoveryPlan): Boolean {
            val a = candidate.score
            val b = best.score
            if (a.urgentServed != b.urgentServed) {
                return a.urgentServed > b.urgentServed
            }
            if (a.served != b.served) {
                return a.served > b.served
            }
            if (a.additionalCost != b.additionalCost) {
                return a.additionalCost < b.additionalCost
            }
            if (a.moves != b.moves) {
                return a.moves < b.moves
            }
            fun key(plan: RecoveryPlan): String {
                return plan.placements.sortedBy { it.reservationId }.joinToString("|") {
                    "%020d:".format(it.reservationId) + it.segments.joinToString(",") { segment ->
                        "%020d:%s".format(segment.roomId, segment.startDate)
                    }
                }
            }
            return key(candidate) < key(best)
        }
    }
}
