package duels.poker.server.duel

import duels.poker.engine.log.decodeMatchLog
import duels.poker.engine.log.encodeMatchLog
import duels.poker.engine.log.replayMatch
import org.junit.jupiter.api.Timeout
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import kotlin.test.assertEquals

@Timeout(120)
class RunnerReplayTest {
    @ParameterizedTest
    @ValueSource(longs = [1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L, 11L, 12L, 13L, 14L, 15L, 16L, 17L, 18L, 19L, 20L])
    fun everyDuelReplaysFromItsLog(seed: Long) {
        val played = playDuel(seed)
        try {
            replayMatch(played.runner.log)
        } catch (e: Exception) {
            throw AssertionError("seed $seed: failed to replay log", e)
        }
    }

    @ParameterizedTest
    @ValueSource(longs = [1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L, 11L, 12L, 13L, 14L, 15L, 16L, 17L, 18L, 19L, 20L])
    fun theReplayReachesTheSameOutcome(seed: Long) {
        val played = playDuel(seed)
        val replay = try {
            replayMatch(played.runner.log)
        } catch (e: Exception) {
            throw AssertionError("seed $seed: failed to replay log", e)
        }
        assertEquals(
            played.runner.outcome,
            replay.outcome,
            "seed $seed: outcome mismatch",
        )
    }

    @ParameterizedTest
    @ValueSource(longs = [1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L, 11L, 12L, 13L, 14L, 15L, 16L, 17L, 18L, 19L, 20L])
    fun theReplayReachesTheSameMatchState(seed: Long) {
        val played = playDuel(seed)
        val replay = try {
            replayMatch(played.runner.log)
        } catch (e: Exception) {
            throw AssertionError("seed $seed: failed to replay log", e)
        }
        assertEquals(
            played.runner.match,
            replay.finalMatch,
            "seed $seed: match state mismatch",
        )
    }

    @ParameterizedTest
    @ValueSource(longs = [1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L, 11L, 12L, 13L, 14L, 15L, 16L, 17L, 18L, 19L, 20L])
    fun theReplayCoversEveryHandThatWasPlayed(seed: Long) {
        val played = playDuel(seed)
        val replay = try {
            replayMatch(played.runner.log)
        } catch (e: Exception) {
            throw AssertionError("seed $seed: failed to replay log", e)
        }
        assertEquals(
            played.runner.log.hands.size,
            replay.hands.size,
            "seed $seed: number of hands mismatch",
        )
        // Check each hand's number is correct
        for (i in replay.hands.indices) {
            val expectedHandNumber = i + 1
            val actualHandNumber = replay.hands[i].finalState.handNumber
            assertEquals(
                expectedHandNumber,
                actualHandNumber,
                "seed $seed: hand at index $i has wrong number",
            )
        }
    }

    @ParameterizedTest
    @ValueSource(longs = [1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L, 11L, 12L, 13L, 14L, 15L, 16L, 17L, 18L, 19L, 20L])
    fun theLogReplaysAfterAJsonRoundTrip(seed: Long) {
        val played = playDuel(seed)
        val encoded = encodeMatchLog(played.runner.log)
        val decoded = try {
            decodeMatchLog(encoded)
        } catch (e: Exception) {
            throw AssertionError("seed $seed: failed to decode log", e)
        }
        val replay = try {
            replayMatch(decoded)
        } catch (e: Exception) {
            throw AssertionError("seed $seed: failed to replay decoded log", e)
        }
        assertEquals(
            played.runner.outcome,
            replay.outcome,
            "seed $seed: outcome mismatch after JSON round trip",
        )
        assertEquals(
            played.runner.match,
            replay.finalMatch,
            "seed $seed: match state mismatch after JSON round trip",
        )
    }

    @ParameterizedTest
    @ValueSource(longs = [1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L, 11L, 12L, 13L, 14L, 15L, 16L, 17L, 18L, 19L, 20L])
    fun atamperedSeedIsRejected(seed: Long) {
        val played = playDuel(seed)
        val log = played.runner.log
        if (log.hands.isEmpty()) {
            // Skip if no hands were played
            return
        }
        // Tamper with the first hand's seed
        val tamperedHandLog = log.hands[0].copy(seed = log.hands[0].seed + 1)
        val tamperedLog = log.copy(hands = listOf(tamperedHandLog) + log.hands.drop(1))

        // Attempt to replay should throw
        try {
            replayMatch(tamperedLog)
            throw AssertionError("seed $seed: expected replayMatch to throw on tampered seed, but it did not")
        } catch (e: Exception) {
            if (e is AssertionError && e.message?.contains("expected replayMatch to throw") == true) {
                throw e
            }
            // Expected: replaying a log with a tampered seed should throw
        }
    }
}
