package duels.poker.engine.game

import duels.poker.engine.random.SplitMix64Rng
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/**
 * Plays one hand end to end through [DefaultPokerEngine]: limp, check, bet, call, check, bet,
 * raise, call — from the opening blinds to [ShowdownReached]. [playScriptedHand] builds the
 * walkthrough once; every test below asserts one fact about that same run.
 */
internal class HandWalkthroughTest {

    /** The script's ten actions, in the order the story plays them. */
    private val script: List<PlayerAction> = listOf(
        PlayerAction.Call(0),
        PlayerAction.Check(1),
        PlayerAction.Check(1),
        PlayerAction.Bet(0, 200),
        PlayerAction.Call(1),
        PlayerAction.Check(1),
        PlayerAction.Check(0),
        PlayerAction.Bet(1, 400),
        PlayerAction.Raise(0, 800),
        PlayerAction.Call(1),
    )

    /** One scripted action and the [EngineResult] it produced. */
    private data class Step(val action: PlayerAction, val result: EngineResult)

    /**
     * The whole walkthrough: the state right after [startHand], every scripted step in the
     * order [script] lists them, and every event produced along the way, dense from
     * `HandStarted` through [ShowdownReached].
     */
    private data class Walkthrough(
        val opening: GameState,
        val steps: List<Step>,
        val allEvents: List<GameEvent>,
    ) {
        val finalState: GameState get() = steps.last().result.newState
    }

    /** Opens the hand and drives it through [script] to showdown. */
    private fun playScriptedHand(): Walkthrough {
        val opened = startHand(
            handNumber = 1,
            buttonSeat = 0,
            stacks = listOf(10_000, 10_000),
            smallBlind = 50,
            bigBlind = 100,
            rng = SplitMix64Rng(1L),
        )

        val steps = mutableListOf<Step>()
        var state = opened.newState
        for (action in script) {
            val result = DefaultPokerEngine.handle(state, action)
            steps += Step(action, result)
            state = result.newState
        }

        val allEvents = opened.events + steps.flatMap { it.result.events }
        return Walkthrough(opened.newState, steps, allEvents)
    }

    @Test
    fun noActionInTheScriptIsRejected() {
        playScriptedHand().steps.forEach { step ->
            assertFalse(step.result.isRejected, "${step.action} was rejected: ${step.result.rejection}")
        }
    }

    @Test
    fun theHandReachesShowdownWithAFullBoard() {
        val final = playScriptedHand().finalState
        assertEquals(Street.SHOWDOWN, final.street)
        assertEquals(5, final.board.size)
        assertNull(final.seatToAct)
    }

    @Test
    fun theChipsEndWhereTheArithmeticSaysTheyDo() {
        val final = playScriptedHand().finalState
        assertEquals(2_200, final.pot)
        assertEquals(8_900, final.seat(0).stack)
        assertEquals(8_900, final.seat(1).stack)
        assertEquals(1_100, final.seat(0).committedThisHand)
        assertEquals(1_100, final.seat(1).committedThisHand)
    }

    @Test
    fun theNonButtonActsFirstOnEveryStreetAfterTheFlop() {
        val stepsThatDealAStreet = playScriptedHand().steps.filter { step ->
            step.result.events.any { it is StreetDealt }
        }
        assertEquals(3, stepsThatDealAStreet.size)
        stepsThatDealAStreet.forEach { step -> assertEquals(1, step.result.newState.seatToAct) }
    }

    @Test
    fun theButtonActsFirstPreflop() {
        assertEquals(0, playScriptedHand().opening.seatToAct)
    }

    @Test
    fun chipsAreConservedAfterEveryAction() {
        playScriptedHand().steps.forEach { step -> assertEquals(20_000, step.result.newState.chipsInPlay) }
    }

    @Test
    fun everyEventSequenceIsDenseAndGapFree() {
        val allEvents = playScriptedHand().allEvents
        assertEquals((0 until allEvents.size).toList(), allEvents.map { it.sequence })
    }

    @Test
    fun noCardIsDealtTwice() {
        val final = playScriptedHand().finalState
        val cards = final.seat(0).holeCards + final.seat(1).holeCards + final.board.cards
        assertEquals(9, cards.size)
        assertEquals(9, cards.toSet().size)
        assertEquals(43, final.deck.remaining)
    }

    @Test
    fun theEventLogReproducesTheFinalState() {
        val walkthrough = playScriptedHand()
        val folded = StateProjection.fold(handState(), walkthrough.allEvents)
        val comparable = folded.copy(deck = walkthrough.finalState.deck, rng = walkthrough.finalState.rng)
        assertEquals(walkthrough.finalState, comparable)
    }
}
