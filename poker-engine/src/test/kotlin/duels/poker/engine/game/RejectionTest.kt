package duels.poker.engine.game

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

class RejectionTest {
    @Test
    fun eachRejectionCarriesItsNumbers() {
        assertEquals(1, Rejection.NotYourTurn(1).seatToAct)
        assertEquals(200, Rejection.AmountTooSmall(150, 200).minimum)
        assertEquals(4_000, Rejection.AmountTooLarge(9_000, 4_000).maximum)
        assertEquals(2, Rejection.ActionNotAllowed(ActionType.CHECK, setOf(ActionType.FOLD, ActionType.CALL)).allowed.size)
    }

    @Test
    fun notYourTurnAllowsNoActor() {
        assertEquals(null, Rejection.NotYourTurn(null).seatToAct)
    }

    @Test
    fun handCompleteIsASingleton() {
        assertSame(Rejection.HandComplete, Rejection.HandComplete)
    }

    @Test
    fun exhaustiveWhenCompilesWithoutElse() {
        val rejection1: String = code(Rejection.NotYourTurn(0))
        val rejection2: String = code(Rejection.ActionNotAllowed(ActionType.CHECK, setOf(ActionType.FOLD)))
        val rejection3: String = code(Rejection.AmountTooSmall(100, 200))
        val rejection4: String = code(Rejection.AmountTooLarge(9_000, 4_000))
        val rejection5: String = code(Rejection.HandComplete)

        // Verify all return distinct values
        val values = setOf(rejection1, rejection2, rejection3, rejection4, rejection5)
        assertEquals(5, values.size)
    }

    private fun code(rejection: Rejection): String = when (rejection) {
        is Rejection.NotYourTurn -> "notYourTurn"
        is Rejection.ActionNotAllowed -> "actionNotAllowed"
        is Rejection.AmountTooSmall -> "amountTooSmall"
        is Rejection.AmountTooLarge -> "amountTooLarge"
        Rejection.HandComplete -> "handComplete"
    }
}
