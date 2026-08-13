package duels.poker.server.duel

import duels.poker.engine.duel.BlindLevel
import duels.poker.engine.duel.BlindSchedule
import duels.poker.engine.duel.DuelFormat
import duels.poker.engine.duel.DuelOutcome
import duels.poker.engine.duel.EndCondition
import duels.poker.engine.duel.MatchFinished
import duels.poker.engine.log.MatchLog
import duels.poker.server.session.PlayerId
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DuelResultSinkTest {
    private val player1 = PlayerId("player1")
    private val player2 = PlayerId("player2")
    private val seats = listOf(player1, player2)
    private val duelId = UUID.randomUUID()

    private fun emptyMatchLog(outcome: DuelOutcome): MatchLog {
        // Create a log with a single MatchFinished event and no hands
        val format = DuelFormat(
            startingStack = 1000,
            blinds = BlindSchedule(
                levels = listOf(BlindLevel(50, 100)),
                handsPerLevel = 10,
            ),
            endCondition = EndCondition.Freezeout,
        )
        return MatchLog(
            format = format,
            buttonSeat = 0,
            hands = emptyList(),
            events = listOf(MatchFinished(sequence = 0, outcome = outcome)),
        )
    }

    @Test
    fun theWinnerIsThePlayerInTheWinningSeat() {
        val outcome = DuelOutcome(winner = 1, handsPlayed = 5, finalStacks = listOf(1500, 2500))
        val log = emptyMatchLog(outcome)
        val result = DuelResult(duelId = duelId, outcome = outcome, seats = seats, log = log)

        assertEquals(player2, result.winner)
    }

    @Test
    fun adrawHasNoWinningPlayer() {
        val outcome = DuelOutcome(winner = null, handsPlayed = 10, finalStacks = listOf(2000, 2000))
        val log = emptyMatchLog(outcome)
        val result = DuelResult(duelId = duelId, outcome = outcome, seats = seats, log = log)

        assertNull(result.winner)
    }

    @Test
    fun aResultNeedsExactlyTwoSeats() {
        val outcome = DuelOutcome(winner = 0, handsPlayed = 3, finalStacks = listOf(2500, 1500))
        val log = emptyMatchLog(outcome)

        assertThrows<IllegalArgumentException> {
            DuelResult(duelId = duelId, outcome = outcome, seats = listOf(player1), log = log)
        }
    }

    @Test
    fun onePlayerCannotHoldBothSeats() {
        val outcome = DuelOutcome(winner = 0, handsPlayed = 3, finalStacks = listOf(2500, 1500))
        val log = emptyMatchLog(outcome)

        assertThrows<IllegalArgumentException> {
            DuelResult(duelId = duelId, outcome = outcome, seats = listOf(player1, player1), log = log)
        }
    }

    @Test
    fun alogRecordingAnotherOutcomeIsRejected() {
        val outcome1 = DuelOutcome(winner = 0, handsPlayed = 3, finalStacks = listOf(2500, 1500))
        val outcome2 = DuelOutcome(winner = 1, handsPlayed = 3, finalStacks = listOf(2500, 1500))
        val log = emptyMatchLog(outcome2)

        assertThrows<IllegalArgumentException> {
            DuelResult(duelId = duelId, outcome = outcome1, seats = seats, log = log)
        }
    }

    @Test
    fun theSinkIsSatisfiedByALambda() {
        var recorded: DuelResult? = null
        val sink = DuelResultSink { recorded = it }

        val outcome = DuelOutcome(winner = 0, handsPlayed = 5, finalStacks = listOf(2500, 1500))
        val log = emptyMatchLog(outcome)
        val result = DuelResult(duelId = duelId, outcome = outcome, seats = seats, log = log)

        runBlocking {
            sink.record(result)
        }

        assertEquals(result, recorded)
    }
}
