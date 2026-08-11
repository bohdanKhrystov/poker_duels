package duels.poker.engine.game

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class PlayerActionTest {
    @Test
    fun everyActionReportsItsType() {
        assertEquals(ActionType.FOLD, PlayerAction.Fold(0).type)
        assertEquals(ActionType.CHECK, PlayerAction.Check(0).type)
        assertEquals(ActionType.CALL, PlayerAction.Call(0).type)
        assertEquals(ActionType.BET, PlayerAction.Bet(0, to = 300).type)
        assertEquals(ActionType.RAISE, PlayerAction.Raise(0, to = 700).type)
        assertEquals(ActionType.ALL_IN, PlayerAction.AllIn(0).type)
    }

    @Test
    fun everyActionCarriesItsSeat() {
        assertEquals(1, PlayerAction.Fold(1).seat)
        assertEquals(1, PlayerAction.Check(1).seat)
        assertEquals(1, PlayerAction.Call(1).seat)
        assertEquals(1, PlayerAction.Bet(1, to = 300).seat)
        assertEquals(1, PlayerAction.Raise(1, to = 700).seat)
        assertEquals(1, PlayerAction.AllIn(1).seat)
    }

    @Test
    fun amountsAreStreetTotals() {
        assertEquals(300, PlayerAction.Bet(0, to = 300).to)
        assertEquals(700, PlayerAction.Raise(1, to = 700).to)
    }

    @Test
    fun rejectsANonPositiveBet() {
        assertThrows(IllegalArgumentException::class.java) { PlayerAction.Bet(0, to = 0) }
        assertThrows(IllegalArgumentException::class.java) { PlayerAction.Bet(0, to = -1) }
    }

    @Test
    fun rejectsANonPositiveRaise() {
        assertThrows(IllegalArgumentException::class.java) { PlayerAction.Raise(0, to = 0) }
        assertThrows(IllegalArgumentException::class.java) { PlayerAction.Raise(0, to = -50) }
    }

    @Test
    fun exhaustiveWhenCompilesWithoutElse() {
        assertEquals(ActionType.FOLD, describe(PlayerAction.Fold(0)))
        assertEquals(ActionType.CHECK, describe(PlayerAction.Check(0)))
        assertEquals(ActionType.CALL, describe(PlayerAction.Call(0)))
        assertEquals(ActionType.BET, describe(PlayerAction.Bet(0, to = 300)))
        assertEquals(ActionType.RAISE, describe(PlayerAction.Raise(0, to = 700)))
        assertEquals(ActionType.ALL_IN, describe(PlayerAction.AllIn(0)))
    }

    private fun describe(action: PlayerAction): ActionType =
        when (action) {
            is PlayerAction.Fold -> ActionType.FOLD
            is PlayerAction.Check -> ActionType.CHECK
            is PlayerAction.Call -> ActionType.CALL
            is PlayerAction.Bet -> ActionType.BET
            is PlayerAction.Raise -> ActionType.RAISE
            is PlayerAction.AllIn -> ActionType.ALL_IN
        }

    @Test
    fun equalActionsAreEqual() {
        assertEquals(PlayerAction.Bet(0, to = 300), PlayerAction.Bet(0, to = 300))
        assertNotEquals(PlayerAction.Bet(0, to = 300), PlayerAction.Bet(1, to = 300))
    }
}
