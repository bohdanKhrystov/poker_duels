package duels.poker.engine.game

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout

/**
 * A thousand hands nobody sat down and designed, each reproducible from its seed alone: every
 * path out of a hand — a fold, a closed river, a run-out — now settles on its own, so this proves
 * the settlement invariants hold not only for the hands somebody thought to write down.
 */
class SettlementInvariantTest {

    @Test
    @Timeout(30)
    fun everyRandomHandFinishesComplete() {
        for (seed in 1L..1000L) {
            val played = playRandomHand(seed)
            val finalState = played.finalState

            assertEquals(Street.COMPLETE, finalState.street, "seed $seed: expected street COMPLETE")
            assertEquals(null, finalState.seatToAct, "seed $seed: expected seatToAct to be null")

            val handFinished = played.events.filterIsInstance<HandFinished>()
            assertEquals(1, handFinished.size, "seed $seed: expected exactly one HandFinished, got $handFinished")
            assertTrue(
                played.events.last() is HandFinished,
                "seed $seed: expected the last event to be HandFinished, was ${played.events.last()}",
            )
        }
    }

    @Test
    @Timeout(30)
    fun noRandomHandLeavesChipsInThePot() {
        for (seed in 1L..1000L) {
            val played = playRandomHand(seed)
            val finalState = played.finalState

            assertEquals(0, finalState.pot, "seed $seed: expected an empty pot")
            assertEquals(0, finalState.potTotal, "seed $seed: expected an empty potTotal")

            val openingStacks = played.opening.seats.sumOf { it.stack }
            val finalStacks = finalState.seats.sumOf { it.stack }
            assertEquals(
                openingStacks,
                finalStacks,
                "seed $seed: expected the final stacks to sum to the opening stacks",
            )
        }
    }

    @Test
    @Timeout(30)
    fun everySettledChipCameFromACommitment() {
        for (seed in 1L..1000L) {
            val played = playRandomHand(seed)

            val awarded = played.events.filterIsInstance<PotAwarded>().sumOf { it.amount }
            val returned = played.events.filterIsInstance<UncalledBetReturned>().sumOf { it.amount }
            val committed = played.finalState.seats.sumOf { it.committedThisHand }

            assertEquals(
                committed,
                awarded + returned,
                "seed $seed: expected awards ($awarded) plus returns ($returned) to equal commitments ($committed)",
            )
        }
    }

    @Test
    @Timeout(30)
    fun noHandPaysMoreThanTwoAwards() {
        for (seed in 1L..1000L) {
            val played = playRandomHand(seed)

            val awardIndices = played.events.withIndex().filter { it.value is PotAwarded }.map { it.index }
            val returnIndices = played.events.withIndex().filter { it.value is UncalledBetReturned }.map { it.index }

            assertTrue(
                awardIndices.size <= 2,
                "seed $seed: expected at most two PotAwarded events, got ${awardIndices.size}",
            )
            assertTrue(
                returnIndices.size <= 1,
                "seed $seed: expected at most one UncalledBetReturned event, got ${returnIndices.size}",
            )

            returnIndices.singleOrNull()?.let { returnIndex ->
                assertTrue(
                    awardIndices.all { it > returnIndex },
                    "seed $seed: expected the UncalledBetReturned (index $returnIndex) to precede every " +
                        "PotAwarded (indices $awardIndices)",
                )
            }
        }
    }

    @Test
    @Timeout(30)
    fun theSampleContainsBothEndings() {
        var foldedHands = 0
        var showdownHands = 0

        for (seed in 1L..1000L) {
            val played = playRandomHand(seed)
            if (played.events.any { it is PlayerFolded }) {
                foldedHands += 1
            }
            if (played.events.any { it is ShowdownReached }) {
                showdownHands += 1
            }
        }

        assertTrue(foldedHands > 100, "expected more than 100 folded hands, got $foldedHands")
        assertTrue(showdownHands > 100, "expected more than 100 showdown hands, got $showdownHands")
    }
}
