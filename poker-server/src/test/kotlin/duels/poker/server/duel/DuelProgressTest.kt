package duels.poker.server.duel

import duels.poker.engine.duel.BlindLevel
import duels.poker.engine.duel.BlindSchedule
import duels.poker.engine.duel.DuelFormat
import duels.poker.engine.duel.EndCondition
import duels.poker.engine.duel.MatchFinished
import duels.poker.engine.duel.MatchState
import duels.poker.engine.duel.outcomeOf
import duels.poker.engine.game.DefaultPokerEngine
import duels.poker.engine.game.PlayerAction
import duels.poker.engine.log.MatchLog
import duels.poker.engine.random.SplitMix64Rng
import duels.poker.server.protocol.ServerMessage
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

private fun DuelRunner.play(action: PlayerAction): DuelRunner {
    val live = hand!!
    val result = DefaultPokerEngine.handle(live.state, action)
    val log = live.log.copy(actions = live.log.actions + action, events = live.log.events + result.events)
    return copy(hand = LiveHand(result.newState, log))
}

private fun seeds(first: Long): HandSeedSource {
    var rng = SplitMix64Rng(first)
    return HandSeedSource {
        val draw = rng.nextLong()
        rng = draw.next
        draw.value
    }
}

private val flat = DuelFormat(
    startingStack = 10_000,
    blinds = BlindSchedule(listOf(BlindLevel(50, 100)), 10),
    endCondition = EndCondition.Freezeout,
)

private val oneHand = flat.copy(endCondition = EndCondition.FixedHands(1))

/** Starts a duel and folds hand 1 preflop, on the seat on turn, so the hand is over. */
private fun foldedFirstHand(format: DuelFormat = flat, buttonSeat: Int = 0, seed: Long = 1L): DuelRunner {
    val started = startDuel(format, buttonSeat, seed)
    val toActSeat = started.runner.hand!!.state.seatToAct!!
    return started.runner.play(PlayerAction.Fold(toActSeat))
}

class DuelProgressTest {

    @Test
    fun theFinishedHandJoinsTheMatchLog() {
        val folded = foldedFirstHand()
        val step = advance(folded, seeds(42L))

        assertEquals(1, step.runner.log.hands.size)
        val recorded = step.runner.log.hands.single()
        assertEquals(1, recorded.handNumber)
        assertEquals(folded.hand!!.log.actions, recorded.actions)
        assertEquals(1, recorded.actions.size)
        assertTrue(recorded.actions.single() is PlayerAction.Fold)
    }

    @Test
    fun theNextHandIsDealtWithTheButtonMoved() {
        val folded = foldedFirstHand()
        val step = advance(folded, seeds(42L))

        assertEquals(2, step.runner.hand!!.log.handNumber)
        assertEquals(1, step.runner.hand!!.log.buttonSeat)
    }

    @Test
    fun theNextHandCarriesTheStacksTheLastOneLeft() {
        val folded = foldedFirstHand()
        val finishedStacks = folded.hand!!.state.seats.map { it.stack }
        val step = advance(folded, seeds(42L))

        assertEquals(finishedStacks, step.runner.hand!!.log.stacks)
        assertEquals(20_000, step.runner.hand!!.log.stacks.sum())
    }

    @Test
    fun theNextHandRecordsTheSeedItWasGiven() {
        val folded = foldedFirstHand()
        val step = advance(folded, HandSeedSource { 99L })

        assertEquals(99L, step.runner.hand!!.log.seed)
    }

    @Test
    fun bothSeatsSeeTheNextHandOpen() {
        val folded = foldedFirstHand()
        val step = advance(folded, seeds(42L))

        val snapshots = step.outbound.count { it.message is ServerMessage.Snapshot }
        val yourTurns = step.outbound.count { it.message is ServerMessage.YourTurn }

        assertEquals(2, snapshots)
        assertEquals(1, yourTurns)
        assertEquals(setOf(0, 1), step.outbound.filter { it.message is ServerMessage.Snapshot }.map { it.seat }.toSet())
    }

    @Test
    fun theReturnedHandAlwaysHasASeatToAct() {
        val fixtures = listOf(
            advance(foldedFirstHand(), seeds(1L)).runner,
            advance(foldedFirstHand(oneHand), seeds(2L)).runner,
            advance(dealtOutSecondHandFixture(), seeds(3L)).runner,
        )

        for (runner in fixtures) {
            assertTrue(runner.hand == null || runner.hand!!.state.seatToAct != null)
        }
    }

    /**
     * Builds a runner whose live hand, once folded, leaves seat 0 with only 20 chips holding the
     * button for hand 2 — short enough that hand 2's small blind posts all-in and the hand deals
     * itself out.
     *
     * The engine only allows a fold when facing a bet (`BettingRules.kt`): seat 0's big-blind
     * option after a limp has nothing to call, so it cannot fold there. Seat 1 shoves all-in
     * first instead, which both faces seat 0 with a real bet and leaves seat 0's untouched
     * 20-chip stack exactly where the fold needs it.
     */
    private fun dealtOutSecondHandFixture(): DuelRunner {
        val match = MatchState(flat, handsPlayed = 0, stacks = listOf(120, 300), buttonSeat = 1)
        val hand = openHand(match, seed = 7L)
        val runner = DuelRunner(
            match = match,
            hand = hand,
            log = MatchLog(flat, buttonSeat = 1, hands = emptyList(), events = emptyList()),
            outcome = null,
        )
        return runner.play(PlayerAction.AllIn(1)).play(PlayerAction.Fold(0))
    }

    @Test
    fun ahandThatDealsItselfOutIsRecordedToo() {
        val folded = dealtOutSecondHandFixture()
        val step = advance(folded, seeds(11L))

        assertTrue(step.runner.log.hands.size >= 2)
        assertTrue(
            step.outbound.any { addressed ->
                val message = addressed.message
                message is ServerMessage.Snapshot && message.view.handNumber == 2
            },
        )
        assertTrue(step.runner.hand == null || step.runner.hand!!.state.seatToAct != null)
    }

    @Test
    fun afinishedDuelKeepsNoLiveHand() {
        val folded = foldedFirstHand(oneHand)
        val step = advance(folded, seeds(5L))

        assertNull(step.runner.hand)
        assertEquals(outcomeOf(step.runner.match), step.runner.outcome)
    }

    @Test
    fun afinishedDuelRecordsMatchFinishedOnce() {
        val folded = foldedFirstHand(oneHand)
        val step = advance(folded, seeds(5L))

        val finished = step.runner.log.events.filterIsInstance<MatchFinished>()
        assertEquals(1, step.runner.log.events.size)
        assertEquals(1, finished.size)
        assertEquals(0, finished.single().sequence)
        assertEquals(step.runner.outcome, finished.single().outcome)
    }

    @Test
    fun advancingALiveHandFailsLoudly() {
        val started = startDuel(flat, buttonSeat = 0, seed = 1L)

        assertThrows(IllegalArgumentException::class.java) {
            advance(started.runner, seeds(1L))
        }
    }
}
