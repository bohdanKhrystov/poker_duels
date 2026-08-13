package duels.poker.server.duel

import duels.poker.engine.duel.DuelFormat
import duels.poker.engine.duel.DuelOutcome
import duels.poker.engine.duel.MatchState
import duels.poker.engine.duel.startNextHand
import duels.poker.engine.log.HandLog
import duels.poker.engine.log.MatchLog
import duels.poker.engine.random.SplitMix64Rng
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class DuelRunnerTest {
    private fun buildFixture(): Pair<MatchState, LiveHand> {
        val match = MatchState.start(DuelFormat.DEFAULT, 0)
        val rng = SplitMix64Rng(7)
        val engineResult = startNextHand(match, rng)
        val state = engineResult.newState

        val blinds = match.blinds
        val handLog = HandLog(
            seed = 7,
            handNumber = state.handNumber,
            buttonSeat = match.buttonSeat,
            stacks = match.stacks,
            smallBlind = blinds.smallBlind,
            bigBlind = blinds.bigBlind,
            actions = emptyList(),
            events = engineResult.events,
        )

        val liveHand = LiveHand(state, handLog)
        return Pair(match, liveHand)
    }

    @Test
    fun aRunningDuelHoldsALiveHandAndNoOutcome() {
        val (match, hand) = buildFixture()
        val emptyMatchLog = MatchLog(DuelFormat.DEFAULT, 0, emptyList(), emptyList())

        val runner = DuelRunner(
            match = match,
            hand = hand,
            log = emptyMatchLog,
            outcome = null,
        )

        assertNotNull(runner.hand)
        assertNull(runner.outcome)
        assertEquals(hand.state.handNumber, runner.hand!!.state.handNumber)
    }

    @Test
    fun aLiveHandWithAnOutcomeIsRejected() {
        val (match, hand) = buildFixture()
        val emptyMatchLog = MatchLog(DuelFormat.DEFAULT, 0, emptyList(), emptyList())
        val outcome = DuelOutcome(winner = 0, handsPlayed = 0, finalStacks = listOf(200, 0))

        assertThrows(IllegalArgumentException::class.java) {
            DuelRunner(
                match = match,
                hand = hand,
                log = emptyMatchLog,
                outcome = outcome,
            )
        }
    }

    @Test
    fun aDuelWithNeitherHandNorOutcomeIsRejected() {
        val match = MatchState.start(DuelFormat.DEFAULT, 0)
        val emptyMatchLog = MatchLog(DuelFormat.DEFAULT, 0, emptyList(), emptyList())

        assertThrows(IllegalArgumentException::class.java) {
            DuelRunner(
                match = match,
                hand = null,
                log = emptyMatchLog,
                outcome = null,
            )
        }
    }

    @Test
    fun aLiveHandMustBeTheHandTheMatchExpectsNext() {
        val (_, hand) = buildFixture()
        // Create a match that has already played one hand (handsPlayed = 1)
        // This means it expects hand 2, but we'll give it hand 1
        val match = MatchState(
            format = DuelFormat.DEFAULT,
            handsPlayed = 1,
            stacks = listOf(150, 250),
            buttonSeat = 1,
        )
        val emptyMatchLog = MatchLog(DuelFormat.DEFAULT, 1, emptyList(), emptyList())

        assertThrows(IllegalArgumentException::class.java) {
            DuelRunner(
                match = match,
                hand = hand, // hand.state.handNumber == 1, but match.nextHandNumber == 2
                log = emptyMatchLog,
                outcome = null,
            )
        }
    }

    @Test
    fun aRecordedOutcomeMustBeTheEnginesOwn() {
        val match = MatchState(
            format = DuelFormat.DEFAULT,
            handsPlayed = 1,
            stacks = listOf(200, 0),
            buttonSeat = 0,
        )
        val emptyMatchLog = MatchLog(DuelFormat.DEFAULT, 0, emptyList(), emptyList())
        // The engine would say seat 0 won, but we'll provide a different outcome
        val wrongOutcome = DuelOutcome(winner = 1, handsPlayed = 1, finalStacks = listOf(200, 0))

        assertThrows(IllegalArgumentException::class.java) {
            DuelRunner(
                match = match,
                hand = null,
                log = emptyMatchLog,
                outcome = wrongOutcome,
            )
        }
    }

    @Test
    fun aLogWhoseEventsDoNotMatchTheStateIsRejected() {
        val (match, hand) = buildFixture()
        val emptyMatchLog = MatchLog(DuelFormat.DEFAULT, 0, emptyList(), emptyList())

        // Create a LiveHand with mismatched log events
        val mutatedLog = hand.log.copy(events = hand.log.events.dropLast(1))

        assertThrows(IllegalArgumentException::class.java) {
            LiveHand(hand.state, mutatedLog)
        }
    }
}
