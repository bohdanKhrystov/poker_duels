package duels.poker.engine.log

import duels.poker.engine.game.BlindPosted
import duels.poker.engine.game.HandFinished
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

    @Test
    fun divergingEventIsReportedWithItsIndex() {
        val played = playRandomHand(7L)
        val log = logOf(7L, played)
        val blind = log.events[1] as BlindPosted
        val tamperedEvents = log.events.toMutableList().apply { this[1] = blind.copy(to = blind.to + 1) }
        val tamperedLog = log.copy(events = tamperedEvents)

        val exception = assertThrows<IllegalStateException> { replayHand(tamperedLog) }

        Assertions.assertTrue(exception.message!!.contains("event index 1"))
    }

    @Test
    fun aTruncatedLogIsRejected() {
        val played = playRandomHand(7L)
        val log = logOf(7L, played)
        val truncatedLog = log.copy(events = log.events.dropLast(1))

        val exception = assertThrows<IllegalStateException> { replayHand(truncatedLog) }

        Assertions.assertTrue(exception.message!!.contains(truncatedLog.events.size.toString()))
        Assertions.assertTrue(exception.message!!.contains(log.events.size.toString()))
    }

    @Test
    fun anExtraEventAtTheEndIsRejected() {
        val played = playRandomHand(7L)
        val log = logOf(7L, played)
        val extendedEvents = log.events + HandFinished(sequence = log.events.size)
        val extendedLog = log.copy(events = extendedEvents)

        val exception = assertThrows<IllegalStateException> { replayHand(extendedLog) }

        Assertions.assertTrue(exception.message!!.contains(extendedLog.events.size.toString()))
        Assertions.assertTrue(exception.message!!.contains(log.events.size.toString()))
    }
}
