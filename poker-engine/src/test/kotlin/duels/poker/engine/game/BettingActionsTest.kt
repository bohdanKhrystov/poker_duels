package duels.poker.engine.game

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class BettingActionsTest {

    @Test
    fun aFoldBecomesAPlayerFolded() {
        val state = handState().copy(betToMatch = 300, minRaiseTo = 600)

        val event = eventFor(state, PlayerAction.Fold(seat = 0))

        assertEquals(PlayerFolded(sequence = 0, seat = 0), event)
    }

    @Test
    fun aCheckBecomesAPlayerChecked() {
        val state = handState()

        val event = eventFor(state, PlayerAction.Check(seat = 0))

        assertEquals(PlayerChecked(sequence = 0, seat = 0), event)
    }

    @Test
    fun aCallCarriesTheStreetTotal() {
        val committed = seats()[0].copy(stack = 900, committedThisStreet = 100, committedThisHand = 100)
        val state = handState(listOf(committed, seats()[1])).copy(betToMatch = 300, minRaiseTo = 600)

        val event = eventFor(state, PlayerAction.Call(seat = 0))

        assertEquals(PlayerCalled(sequence = 0, seat = 0, to = 300), event)
    }

    @Test
    fun aCallForLessCarriesTheWholeStack() {
        val short = seats()[0].copy(stack = 150)
        val state = handState(listOf(short, seats()[1])).copy(betToMatch = 300, minRaiseTo = 600)

        val event = eventFor(state, PlayerAction.Call(seat = 0))

        assertEquals(PlayerCalled(sequence = 0, seat = 0, to = 150), event)
    }

    @Test
    fun anAllInCarriesTheWholeStack() {
        val committed = seats()[0].copy(stack = 900, committedThisStreet = 100, committedThisHand = 100)
        val state = handState(listOf(committed, seats()[1])).copy(betToMatch = 100, minRaiseTo = 200)

        val event = eventFor(state, PlayerAction.AllIn(seat = 0))

        assertEquals(PlayerAllIn(sequence = 0, seat = 0, to = 1_000), event)
    }

    @Test
    fun anOrdinaryBetBecomesAPlayerBet() {
        val state = handState()

        val event = eventFor(state, PlayerAction.Bet(seat = 0, to = 300))

        assertEquals(PlayerBet(sequence = 0, seat = 0, to = 300), event)
    }

    @Test
    fun aRaiseForTheWholeStackIsLoggedAsAnAllIn() {
        val short = seats()[0].copy(stack = 500)
        val state = handState(listOf(short, seats()[1])).copy(betToMatch = 300, minRaiseTo = 600)

        val event = eventFor(state, PlayerAction.Raise(seat = 0, to = 500))

        assertEquals(PlayerAllIn(sequence = 0, seat = 0, to = 500), event)
    }

    @Test
    fun aShortShoveDoesNotLowerTheMinimumRaise() {
        val short = seats()[0].copy(stack = 500)
        val state = handState(listOf(short, seats()[1])).copy(betToMatch = 300, minRaiseTo = 600)

        val event = eventFor(state, PlayerAction.Raise(seat = 0, to = 500))
        val after = applyBetting(state, event)

        assertEquals(600, after.minRaiseTo)
    }

    @Test
    fun everyEventContinuesTheHandSequence() {
        val state = handState().copy(eventCount = 7)

        val event = eventFor(state, PlayerAction.Check(seat = 0))

        assertEquals(7, event.sequence)
    }

    @Test
    fun refusesAnActionThatIsNotLegal() {
        val state = handState().copy(betToMatch = 300, minRaiseTo = 600)

        assertThrows(IllegalArgumentException::class.java) {
            eventFor(state, PlayerAction.Check(seat = 0))
        }
    }
}
