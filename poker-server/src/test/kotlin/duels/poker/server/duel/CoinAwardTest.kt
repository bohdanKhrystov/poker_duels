package duels.poker.server.duel

import duels.poker.engine.duel.DuelOutcome
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

class CoinAwardTest {
    @Test
    fun theWinnerAtSeatZeroGainsOneAndSeatOneLosesOne() {
        val outcome = DuelOutcome(winner = 0, handsPlayed = 12, finalStacks = listOf(20_000, 0))
        val deltas = coinDeltas(outcome)
        assertEquals(1, deltas.seat0)
        assertEquals(-1, deltas.seat1)
    }

    @Test
    fun theWinnerAtSeatOneGainsOneAndSeatZeroLosesOne() {
        val outcome = DuelOutcome(winner = 1, handsPlayed = 12, finalStacks = listOf(0, 20_000))
        val deltas = coinDeltas(outcome)
        assertEquals(-1, deltas.seat0)
        assertEquals(1, deltas.seat1)
    }

    @Test
    fun aDrawPaysNothingToEitherSeat() {
        val outcome = DuelOutcome(winner = null, handsPlayed = 20, finalStacks = listOf(10_000, 10_000))
        val deltas = coinDeltas(outcome)
        assertEquals(0, deltas.seat0)
        assertEquals(0, deltas.seat1)
    }

    @Test
    fun theTwoDeltasAlwaysSumToZero() {
        val winOutcome = DuelOutcome(winner = 0, handsPlayed = 12, finalStacks = listOf(20_000, 0))
        val winDeltas = coinDeltas(winOutcome)
        assertEquals(0, winDeltas.seat0 + winDeltas.seat1)

        val lossOutcome = DuelOutcome(winner = 1, handsPlayed = 12, finalStacks = listOf(0, 20_000))
        val lossDeltas = coinDeltas(lossOutcome)
        assertEquals(0, lossDeltas.seat0 + lossDeltas.seat1)

        val drawOutcome = DuelOutcome(winner = null, handsPlayed = 20, finalStacks = listOf(10_000, 10_000))
        val drawDeltas = coinDeltas(drawOutcome)
        assertEquals(0, drawDeltas.seat0 + drawDeltas.seat1)
    }

    @Test
    fun forSeatReturnsTheDeltaOfThatSeat() {
        val deltas = CoinDeltas(1, -1)
        assertEquals(1, deltas.forSeat(0))
        assertEquals(-1, deltas.forSeat(1))
    }

    @Test
    fun forSeatRejectsASeatThatIsNotZeroOrOne() {
        val deltas = CoinDeltas(1, -1)
        assertThrows<IllegalArgumentException> {
            deltas.forSeat(2)
        }
        assertThrows<IllegalArgumentException> {
            deltas.forSeat(-1)
        }
    }
}
