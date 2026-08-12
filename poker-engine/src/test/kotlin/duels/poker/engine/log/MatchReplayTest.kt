package duels.poker.engine.log

import duels.poker.engine.duel.MatchFinished
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class MatchReplayTest {
    @Test
    fun replayingALoggedDuelReproducesItsOutcome() {
        for (seed in 1L..10L) {
            val log = playLoggedDuel(seed)
            val recordedOutcome = (log.events.single() as MatchFinished).outcome

            val replay = replayMatch(log)

            Assertions.assertEquals(recordedOutcome, replay.outcome, "seed $seed")
        }
    }

    @Test
    fun replayingReproducesEveryHandsEvents() {
        for (seed in 1L..10L) {
            val log = playLoggedDuel(seed)

            val replay = replayMatch(log)

            Assertions.assertEquals(log.hands.size, replay.hands.size, "seed $seed")
            log.hands.forEachIndexed { index, handLog ->
                Assertions.assertEquals(handLog.events, replay.hands[index].events, "seed $seed, hand ${index + 1}")
            }
        }
    }

    @Test
    fun replayIsStable() {
        val log = playLoggedDuel(1L)

        Assertions.assertEquals(replayMatch(log), replayMatch(log))
    }

    @Test
    fun aHandMissingItsLastActionFailsNamingTheHand() {
        val log = playLoggedDuel(1L)
        val secondHand = log.hands[1]
        val truncatedHand = secondHand.copy(actions = secondHand.actions.dropLast(1))
        val tamperedLog = log.copy(hands = log.hands.toMutableList().apply { this[1] = truncatedHand })

        val exception = assertThrows<IllegalStateException> { replayMatch(tamperedLog) }

        Assertions.assertTrue(exception.message!!.contains("hand 2"))
    }

    @Test
    fun aHandLogWithTheWrongBlindsIsRejected() {
        val log = playLoggedDuel(1L)
        val firstHand = log.hands[0]
        val tamperedHand = firstHand.copy(
            smallBlind = firstHand.smallBlind * 2,
            bigBlind = firstHand.bigBlind * 2,
        )
        val tamperedLog = log.copy(hands = log.hands.toMutableList().apply { this[0] = tamperedHand })

        val exception = assertThrows<IllegalArgumentException> { replayMatch(tamperedLog) }

        Assertions.assertTrue(exception.message!!.contains("hand 1"))
    }

    @Test
    fun aMatchFinishedThatDisagreesWithTheReplayIsRejected() {
        val log = playLoggedDuel(1L)
        val recordedFinished = log.events.single() as MatchFinished
        val wrongWinner = 1 - (recordedFinished.outcome.winner ?: 0)
        val tamperedFinished = recordedFinished.copy(
            outcome = recordedFinished.outcome.copy(winner = wrongWinner),
        )
        val tamperedLog = log.copy(events = listOf(tamperedFinished))

        assertThrows<IllegalArgumentException> { replayMatch(tamperedLog) }
    }
}
