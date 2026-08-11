package duels.poker.engine.game

import duels.poker.engine.game.ActionType.ALL_IN
import duels.poker.engine.game.ActionType.BET
import duels.poker.engine.game.ActionType.CALL
import duels.poker.engine.game.ActionType.CHECK
import duels.poker.engine.game.ActionType.FOLD
import duels.poker.engine.game.ActionType.RAISE
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class LegalActionsTest {
    @Test
    fun describesAnUnopenedStreet() {
        val actions = LegalActions(
            seat = 0,
            allowed = setOf(CHECK, BET, ALL_IN),
            minBetTo = 100,
            allInTo = 10_000,
        )

        assert(actions.allows(CHECK))
        assert(!actions.allows(CALL))
    }

    @Test
    fun describesFacingABet() {
        val actions = LegalActions(
            seat = 1,
            allowed = setOf(FOLD, CALL, RAISE, ALL_IN),
            callTo = 300,
            minRaiseTo = 600,
            allInTo = 10_000,
        )

        assert(actions.seat == 1)
        assert(FOLD in actions.allowed)
        assert(CALL in actions.allowed)
        assert(RAISE in actions.allowed)
        assert(ALL_IN in actions.allowed)
        assert(actions.callTo == 300)
        assert(actions.minRaiseTo == 600)
        assert(actions.allInTo == 10_000)
    }

    @Test
    fun describesAnUncoverableAllIn() {
        val actions = LegalActions(
            seat = 0,
            allowed = setOf(FOLD, CALL),
            callTo = 400,
            allInTo = 400,
        )

        assert(!actions.allows(RAISE))
    }

    @Test
    fun noneAllowsNothing() {
        val actions = LegalActions.none(1)

        assert(actions.allowed.isEmpty())
        assert(!actions.allows(FOLD))
    }

    @Test
    fun rejectsCheckAndCallTogether() {
        assertThrows<IllegalArgumentException> {
            LegalActions(
                seat = 0,
                allowed = setOf(CHECK, CALL),
            )
        }
    }

    @Test
    fun rejectsBetAndRaiseTogether() {
        assertThrows<IllegalArgumentException> {
            LegalActions(
                seat = 0,
                allowed = setOf(BET, RAISE),
            )
        }
    }

    @Test
    fun rejectsANegativeAmount() {
        assertThrows<IllegalArgumentException> {
            LegalActions(
                seat = 0,
                allowed = setOf(CALL),
                callTo = -1,
            )
        }
    }

    @Test
    fun rejectsABetMinimumAboveTheAllInTotal() {
        assertThrows<IllegalArgumentException> {
            LegalActions(
                seat = 0,
                allowed = setOf(BET),
                minBetTo = 500,
                allInTo = 400,
            )
        }
    }

    @Test
    fun rejectsARaiseMinimumAboveTheAllInTotal() {
        assertThrows<IllegalArgumentException> {
            LegalActions(
                seat = 0,
                allowed = setOf(RAISE),
                minRaiseTo = 900,
                allInTo = 800,
            )
        }
    }
}
