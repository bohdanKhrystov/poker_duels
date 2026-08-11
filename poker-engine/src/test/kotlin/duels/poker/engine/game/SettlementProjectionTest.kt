package duels.poker.engine.game

import duels.poker.engine.card.cards
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Pins down the three [applyDealer] branches that move chips between the pot and a stack —
 * `UncalledBetReturned`, `PotAwarded` and `HandFinished` — and proves chip conservation over a
 * whole scripted hand, not merely per event.
 */
class SettlementProjectionTest {

    @Test
    fun returningAnUncalledBetMovesChipsFromThePotToTheSeat() {
        val state = handState().copy(pot = 1_000)
        val before1 = state.seat(1)

        val after = applyDealer(state, UncalledBetReturned(1, 0, 400))

        assertEquals(state.seat(0).stack + 400, after.seat(0).stack)
        assertEquals(600, after.pot)
        assertEquals(before1, after.seat(1))
    }

    @Test
    fun awardingThePotMovesChipsFromThePotToTheSeat() {
        val state = handState().copy(pot = 600)

        val after = applyDealer(state, PotAwarded(2, 1, 600))

        assertEquals(state.seat(1).stack + 600, after.seat(1).stack)
        assertEquals(0, after.pot)
    }

    @Test
    fun aSplitPotEmptiesThePotInTwoAwards() {
        val state = handState().copy(pot = 600)
        val stack0 = state.seat(0).stack
        val stack1 = state.seat(1).stack

        val afterFirst = applyDealer(state, PotAwarded(2, 0, 300))
        val afterSecond = applyDealer(afterFirst, PotAwarded(3, 1, 300))

        assertEquals(0, afterSecond.pot)
        assertEquals(stack0 + 300, afterSecond.seat(0).stack)
        assertEquals(stack1 + 300, afterSecond.seat(1).stack)
    }

    @Test
    fun beingPaidDoesNotUndoAllIn() {
        val allInSeats = seats().mapIndexed { index, seat ->
            if (index == 0) seat.copy(isAllIn = true) else seat
        }
        val state = handState(seats = allInSeats).copy(pot = 600)

        val after = applyDealer(state, PotAwarded(2, 0, 600))

        assertTrue(after.seat(0).isAllIn)
    }

    @Test
    fun handFinishedCompletesTheHand() {
        val state = handState()

        val after = applyDealer(state, HandFinished(4))

        assertEquals(Street.COMPLETE, after.street)
        assertTrue(after.isHandOver)
        assertNull(after.seatToAct)
    }

    @Test
    fun rejectsAnAwardLargerThanThePot() {
        val state = handState().copy(pot = 600)

        assertThrows(IllegalArgumentException::class.java) {
            applyDealer(state, PotAwarded(2, 0, 700))
        }
    }

    @Test
    fun chipsAreConservedAcrossAScriptedHand() {
        val chipsInPlay = 2 * START_STACK

        var state = handState()
        assertEquals(chipsInPlay, state.chipsInPlay)

        state = applyBetting(state, PlayerBet(1, 0, 300))
        assertEquals(chipsInPlay, state.chipsInPlay)

        state = applyBetting(state, PlayerCalled(2, 1, 300))
        assertEquals(chipsInPlay, state.chipsInPlay)

        state = applyDealer(state, BettingRoundEnded(3, Street.PREFLOP))
        assertEquals(chipsInPlay, state.chipsInPlay)
        assertEquals(600, state.pot)

        state = applyDealer(state, StreetDealt(4, Street.FLOP, cards("As Kd 7c")))
        assertEquals(chipsInPlay, state.chipsInPlay)

        state = applyBetting(state, PlayerChecked(5, 1))
        assertEquals(chipsInPlay, state.chipsInPlay)

        state = applyBetting(state, PlayerBet(6, 0, 200))
        assertEquals(chipsInPlay, state.chipsInPlay)

        state = applyBetting(state, PlayerFolded(7, 1))
        assertEquals(chipsInPlay, state.chipsInPlay)

        state = applyDealer(state, BettingRoundEnded(8, Street.FLOP))
        assertEquals(chipsInPlay, state.chipsInPlay)
        assertEquals(800, state.pot)

        state = applyDealer(state, UncalledBetReturned(9, 0, 200))
        assertEquals(chipsInPlay, state.chipsInPlay)
        assertEquals(600, state.pot)

        state = applyDealer(state, PotAwarded(10, 0, 600))
        assertEquals(chipsInPlay, state.chipsInPlay)
        assertEquals(0, state.pot)

        state = applyDealer(state, HandFinished(11))
        assertEquals(chipsInPlay, state.chipsInPlay)

        assertEquals(0, state.pot)
        assertEquals(10_300, state.seat(0).stack)
        assertEquals(9_700, state.seat(1).stack)
    }
}
