package duels.poker.engine.log

import duels.poker.engine.game.PlayedHand
import duels.poker.engine.game.playRandomHand
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout

/**
 * `(seed, actions)` reproduces a hand exactly, over two hundred hands nobody hand-picked, not
 * just the one hand `HandReplayTest` wrote down by hand.
 */
internal class HandLogReplayPropertyTest {

    @Test
    @Timeout(60)
    fun recordAndReplayIsAnIdentityOverTwoHundredRandomHands() {
        for (seed in 1L..200L) {
            val played = playRandomHand(seed)
            val log = logOf(seed, played)
            val replay = replayHand(log)

            assertEquals(played.events, replay.events, "seed $seed: events diverged")
            assertEquals(played.finalState, replay.finalState, "seed $seed: final state diverged")
        }
    }

    @Test
    @Timeout(60)
    fun everyReplayedHandEndsWhereTheRecordedHandEnded() {
        for (seed in 1L..200L) {
            val played = playRandomHand(seed)
            val log = logOf(seed, played)
            val replay = replayHand(log)

            assertTrue(replay.finalState.isHandOver, "seed $seed: replay did not end the hand")
        }
    }

    @Test
    fun theSameLogReplaysToTheSameValueTwice() {
        for (seed in 1L..20L) {
            val played = playRandomHand(seed)
            val log = logOf(seed, played)

            assertEquals(replayHand(log), replayHand(log), "seed $seed: replay is not stable")
        }
    }

    @Test
    fun aLogMissingItsLastActionDoesNotReplayAsAnIdentity() {
        val seed = (1L..200L).first { playRandomHand(it).actions.size >= 2 }
        val played = playRandomHand(seed)
        val log = logOf(seed, played)
        val truncated = log.copy(actions = log.actions.dropLast(1))

        assertThrows(IllegalStateException::class.java) { replayHand(truncated) }
    }
}

private fun logOf(seed: Long, played: PlayedHand) = HandLog(
    seed = seed,
    handNumber = played.opening.handNumber,
    buttonSeat = played.opening.buttonSeat,
    stacks = played.opening.seats.map { it.stack },
    smallBlind = played.opening.smallBlind,
    bigBlind = played.opening.bigBlind,
    actions = played.actions,
    events = played.events,
)
