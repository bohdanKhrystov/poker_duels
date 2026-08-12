package duels.poker.engine.log

import duels.poker.engine.game.PlayedHand
import duels.poker.engine.game.PlayerAction
import duels.poker.engine.game.playRandomHand
import duels.poker.engine.game.startHand
import duels.poker.engine.random.SplitMix64Rng
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class HandReplayTest {
    /** Turns what [playRandomHand] actually produced into the [HandLog] that should reproduce it. */
    private fun logOf(seed: Long, played: PlayedHand): HandLog =
        HandLog(
            seed = seed,
            handNumber = played.opening.handNumber,
            buttonSeat = played.opening.buttonSeat,
            stacks = played.opening.seats.map { it.stack },
            smallBlind = played.opening.smallBlind,
            bigBlind = played.opening.bigBlind,
            actions = played.actions,
            events = played.events,
        )

    @Test
    fun replaysAPlayedHandToTheSameFinalState() {
        val played = playRandomHand(7L)
        val log = logOf(7L, played)

        val replay = replayHand(log)

        Assertions.assertEquals(played.finalState, replay.finalState)
    }

    @Test
    fun regeneratesTheSameEventsInTheSameOrder() {
        val played = playRandomHand(7L)
        val log = logOf(7L, played)

        val replay = replayHand(log)

        Assertions.assertEquals(played.events, replay.events)
    }

    @Test
    fun recordsOneStateForEachAction() {
        val played = playRandomHand(7L)
        val log = logOf(7L, played)

        val replay = replayHand(log)

        Assertions.assertEquals(log.actions.size, replay.statesAfterAction.size)
        Assertions.assertSame(replay.statesAfterAction.last(), replay.finalState)
    }

    @Test
    fun emptyActionsReplayToTheOpeningState() {
        val started = startHand(1, 0, listOf(10_000, 10_000), 50, 100, SplitMix64Rng(42))
        val log = HandLog(
            seed = 42L,
            handNumber = 1,
            buttonSeat = 0,
            stacks = listOf(10_000, 10_000),
            smallBlind = 50,
            bigBlind = 100,
            actions = emptyList(),
            events = started.events,
        )

        val replay = replayHand(log)

        Assertions.assertEquals(started.newState, replay.opening)
        Assertions.assertEquals(started.newState, replay.finalState)
    }

    @Test
    fun rejectsALogWhoseActionTheEngineRefuses() {
        val started = startHand(1, 0, listOf(10_000, 10_000), 50, 100, SplitMix64Rng(42))
        val log = HandLog(
            seed = 42L,
            handNumber = 1,
            buttonSeat = 0,
            stacks = listOf(10_000, 10_000),
            smallBlind = 50,
            bigBlind = 100,
            actions = listOf(PlayerAction.Check(0)),
            events = started.events,
        )

        val exception = assertThrows<IllegalStateException> { replayHand(log) }

        Assertions.assertTrue(exception.message!!.contains("action at index 0"))
    }
}
