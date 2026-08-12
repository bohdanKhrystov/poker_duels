package duels.poker.ai

import duels.poker.engine.game.ActionType
import duels.poker.engine.game.LegalActions
import duels.poker.engine.game.PlayerAction
import duels.poker.engine.game.legalActions
import duels.poker.engine.game.startHand
import duels.poker.engine.random.Rng
import duels.poker.engine.random.SplitMix64Rng
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RandomBotTest {
    @Test
    fun choosesOnlyActionsTheLegalSetAllows() {
        // Over 200 successive draws from one seed, every chosen action type is in the legal set.
        val state = startHand(1, 0, listOf(10_000, 10_000), 50, 100, SplitMix64Rng(7L)).newState
        val legal = legalActions(state)

        var rng: Rng = SplitMix64Rng(42L)
        repeat(200) {
            val choice = RandomBot.choose(state, legal, rng)
            rng = choice.rng
            assertTrue(
                choice.action.type in legal.allowed,
                "Action type ${choice.action.type} was not in legal set ${legal.allowed}",
            )
        }
    }

    @Test
    fun theSameStateAndRngAlwaysChooseTheSameAction() {
        // Two calls with identical arguments must return equal Choice values.
        val state = startHand(1, 0, listOf(10_000, 10_000), 50, 100, SplitMix64Rng(7L)).newState
        val legal = legalActions(state)
        val rng = SplitMix64Rng(99L)

        val choice1 = RandomBot.choose(state, legal, rng)
        val choice2 = RandomBot.choose(state, legal, rng)

        assertEquals(choice1, choice2, "Same inputs must produce equal choices")
    }

    @Test
    fun advancesTheRngItWasGiven() {
        // The returned Rng must be different from the input Rng.
        val state = startHand(1, 0, listOf(10_000, 10_000), 50, 100, SplitMix64Rng(7L)).newState
        val legal = legalActions(state)
        val rng = SplitMix64Rng(7L)

        val choice = RandomBot.choose(state, legal, rng)

        assertFalse(
            choice.rng == rng,
            "The returned Rng must differ from the input Rng (determinism requires advancing it)",
        )
    }

    @Test
    fun betsEitherTheMinimumOrAllIn() {
        // Over 200 draws, every Bet.to is either the minimum or all-in, and both occur.
        val legal = LegalActions(
            seat = 0,
            allowed = setOf(ActionType.FOLD, ActionType.CHECK, ActionType.BET),
            minBetTo = 100,
            allInTo = 10_000,
        )
        val state = startHand(1, 0, listOf(10_000, 10_000), 50, 100, SplitMix64Rng(7L)).newState

        var rng: Rng = SplitMix64Rng(123L)
        val betAmounts = mutableSetOf<Int>()

        repeat(200) {
            val choice = RandomBot.choose(state, legal, rng)
            rng = choice.rng

            if (choice.action is PlayerAction.Bet) {
                val betAmount = choice.action.to
                assertTrue(
                    betAmount == 100 || betAmount == 10_000,
                    "Bet amount $betAmount must be either minBetTo (100) or allInTo (10_000)",
                )
                betAmounts.add(betAmount)
            }
        }

        // Both amounts should have been selected at least once
        assertEquals(
            setOf(100, 10_000),
            betAmounts,
            "Over 200 draws, both the minimum bet (100) and all-in (10_000) should occur",
        )
    }

    @Test
    fun raisesEitherTheMinimumOrAllIn() {
        // Over 200 draws on a preflop legal set, every Raise.to is either minimum or all-in,
        // and both occur.
        val state = startHand(1, 0, listOf(10_000, 10_000), 50, 100, SplitMix64Rng(7L)).newState
        val legal = legalActions(state)

        // For this test to be meaningful, the state must allow raises.
        assertTrue(
            ActionType.RAISE in legal.allowed,
            "Test setup requires RAISE to be in legal actions",
        )

        var rng: Rng = SplitMix64Rng(456L)
        val raiseAmounts = mutableSetOf<Int>()

        repeat(200) {
            val choice = RandomBot.choose(state, legal, rng)
            rng = choice.rng

            if (choice.action is PlayerAction.Raise) {
                val raiseAmount = choice.action.to
                assertTrue(
                    raiseAmount == legal.minRaiseTo || raiseAmount == legal.allInTo,
                    "Raise amount $raiseAmount must be either minRaiseTo (${legal.minRaiseTo}) or allInTo (${legal.allInTo})",
                )
                raiseAmounts.add(raiseAmount)
            }
        }

        // Both amounts should have been selected at least once
        assertEquals(
            setOf(legal.minRaiseTo, legal.allInTo),
            raiseAmounts,
            "Over 200 draws, both the minimum raise (${legal.minRaiseTo}) and all-in (${legal.allInTo}) should occur",
        )
    }

    @Test
    fun everyAllowedActionTypeIsReachable() {
        // Over 200 draws on a preflop legal set, the chosen types cover legal.allowed exactly.
        val state = startHand(1, 0, listOf(10_000, 10_000), 50, 100, SplitMix64Rng(7L)).newState
        val legal = legalActions(state)

        var rng: Rng = SplitMix64Rng(789L)
        val chosenTypes = mutableSetOf<ActionType>()

        repeat(200) {
            val choice = RandomBot.choose(state, legal, rng)
            rng = choice.rng
            chosenTypes.add(choice.action.type)
        }

        // Over 200 draws, we should reach every action type in the legal set.
        assertEquals(
            legal.allowed,
            chosenTypes,
            "Over 200 draws, all action types in legal.allowed should be chosen at least once",
        )
    }
}
