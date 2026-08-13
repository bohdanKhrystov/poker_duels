package duels.poker.server.duel

import duels.poker.engine.duel.DuelFormat
import duels.poker.engine.game.PlayerView
import duels.poker.server.protocol.ServerMessage
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout

/**
 * Chips are conserved in what a real client is told, not just in what the engine knows about
 * itself.
 *
 * Every assertion here is built from the [ServerMessage.Snapshot] frames [playDuel] hands out —
 * never from [DuelRunner]'s internal engine state. `SimulationInvariants` already proves the
 * engine agrees with itself; the only thing worth proving here is that the projection the server
 * sends across the wire never drops or duplicates a chip on the way to a player.
 */
internal class RunnerChipConservationTest {
    private val seeds = 1L..20L
    private val totalChips = 2 * DuelFormat.DEFAULT.startingStack

    /** The chip total a single snapshot claims, per the formula in TASK-020711's `## Scope`. */
    private fun accountedFor(view: PlayerView): Int =
        view.pot + view.seats.sumOf { it.stack } + view.seats.sumOf { it.committedThisStreet }

    @Test
    @Timeout(120)
    fun everySnapshotAccountsForEveryChip() {
        for (seed in seeds) {
            val played = playDuel(seed)
            played.outbound.forEachIndexed { index, addressed ->
                val message = addressed.message
                if (message is ServerMessage.Snapshot) {
                    val accounted = accountedFor(message.view)
                    assertEquals(
                        totalChips,
                        accounted,
                        "seed $seed, frame $index: snapshot to seat ${addressed.seat} accounted for " +
                            "$accounted chips, expected $totalChips",
                    )
                }
            }
        }
    }

    @Test
    @Timeout(120)
    fun noSnapshotShowsANegativeStackOrPot() {
        for (seed in seeds) {
            val played = playDuel(seed)
            played.outbound.forEachIndexed { index, addressed ->
                val message = addressed.message
                if (message is ServerMessage.Snapshot) {
                    val view = message.view
                    assertTrue(
                        view.pot >= 0,
                        "seed $seed, frame $index: snapshot to seat ${addressed.seat} has negative pot ${view.pot}",
                    )
                    view.seats.forEach { seat ->
                        assertTrue(
                            seat.stack >= 0,
                            "seed $seed, frame $index: snapshot to seat ${addressed.seat} shows seat " +
                                "${seat.index} with negative stack ${seat.stack}",
                        )
                        assertTrue(
                            seat.committedThisStreet >= 0,
                            "seed $seed, frame $index: snapshot to seat ${addressed.seat} shows seat " +
                                "${seat.index} with negative committedThisStreet ${seat.committedThisStreet}",
                        )
                    }
                }
            }
        }
    }

    @Test
    @Timeout(120)
    fun theLastSnapshotEachSeatSawMatchesTheOutcome() {
        for (seed in seeds) {
            val played = playDuel(seed)
            val outcome =
                requireNotNull(played.runner.outcome) { "seed $seed: duel finished without an outcome" }

            for (seat in 0..1) {
                val lastSnapshot =
                    played.outbound.lastOrNull { it.seat == seat && it.message is ServerMessage.Snapshot }
                        ?: throw AssertionError("seed $seed: seat $seat never received a Snapshot")
                val view = (lastSnapshot.message as ServerMessage.Snapshot).view

                assertEquals(
                    outcome.finalStacks,
                    view.seats.map { it.stack },
                    "seed $seed: seat $seat's last snapshot stacks disagree with runner.outcome.finalStacks",
                )
            }
        }
    }

    @Test
    @Timeout(120)
    fun bothSeatsSawTheSameChipTotalThroughout() {
        for (seed in seeds) {
            val played = playDuel(seed)
            val snapshotsBySeat =
                (0..1).map { seat ->
                    played.outbound.filter { it.seat == seat && it.message is ServerMessage.Snapshot }
                }
            val (seat0Snapshots, seat1Snapshots) = snapshotsBySeat

            seat0Snapshots.zip(seat1Snapshots).forEachIndexed { index, (fromSeat0, fromSeat1) ->
                val total0 = accountedFor((fromSeat0.message as ServerMessage.Snapshot).view)
                val total1 = accountedFor((fromSeat1.message as ServerMessage.Snapshot).view)
                assertEquals(
                    total0,
                    total1,
                    "seed $seed, snapshot index $index: seat 0 saw total $total0, seat 1 saw total $total1",
                )
            }
        }
    }
}
