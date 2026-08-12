package duels.poker.engine.duel

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MatchEventTest {
    @Test
    fun carriesTheSequenceAndTheOutcome() {
        val outcome = DuelOutcome(0, 10, listOf(20_000, 0))
        val event = MatchFinished(3, outcome)

        assertEquals(3, event.sequence)
        assertEquals(outcome, event.outcome)
    }

    @Test
    fun defaultsToTheCurrentSchemaVersion() {
        val outcome = DuelOutcome(0, 10, listOf(20_000, 0))
        val event = MatchFinished(0, outcome)

        assertEquals(MATCH_EVENT_SCHEMA_VERSION, event.version)
        assertEquals(1, MATCH_EVENT_SCHEMA_VERSION)
    }

    @Test
    fun twoEventsWithTheSameContentAreEqual() {
        val outcome = DuelOutcome(0, 10, listOf(20_000, 0))
        val event1 = MatchFinished(5, outcome)
        val event2 = MatchFinished(5, outcome)

        assertEquals(event1, event2)
    }

    @Test
    fun rejectsANegativeSequence() {
        val outcome = DuelOutcome(0, 10, listOf(20_000, 0))

        assertThrows(IllegalArgumentException::class.java) {
            MatchFinished(-1, outcome)
        }
    }

    @Test
    fun noEventForAMatchStillRunning() {
        val format = DuelFormat.DEFAULT
        val match = MatchState(
            format = format,
            handsPlayed = 0,
            stacks = listOf(500, 500),
            buttonSeat = 0,
        )

        assertNull(matchFinishedEvent(match))
    }

    @Test
    fun namesTheWinnerOfAFinishedFreezeout() {
        val format = DuelFormat.DEFAULT
        val match = MatchState(
            format = format,
            handsPlayed = 7,
            stacks = listOf(0, 1000),
            buttonSeat = 0,
        )

        val event = matchFinishedEvent(match)

        assertEquals(1, event?.outcome?.winner)
        assertEquals(listOf(0, 1000), event?.outcome?.finalStacks)
    }

    @Test
    fun reportsADrawAsANullWinner() {
        val format = DuelFormat.DEFAULT.copy(endCondition = EndCondition.FixedHands(1))
        val match = MatchState(
            format = format,
            handsPlayed = 1,
            stacks = listOf(500, 500),
            buttonSeat = 0,
        )

        val event = matchFinishedEvent(match)

        assertTrue(event?.outcome?.isDraw == true)
    }

    @Test
    fun theSequenceIsTheCallersToChoose() {
        val format = DuelFormat.DEFAULT
        val match = MatchState(
            format = format,
            handsPlayed = 7,
            stacks = listOf(0, 1000),
            buttonSeat = 0,
        )

        val event = matchFinishedEvent(match, sequence = 7)

        assertEquals(7, event?.sequence)

        val defaultEvent = matchFinishedEvent(match)

        assertEquals(0, defaultEvent?.sequence)
    }
}
