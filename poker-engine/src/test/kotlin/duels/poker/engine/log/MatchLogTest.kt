package duels.poker.engine.log

import duels.poker.engine.duel.DuelFormat
import duels.poker.engine.duel.DuelOutcome
import duels.poker.engine.duel.MatchFinished
import duels.poker.engine.game.HandStarted
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class MatchLogTest {

    private fun handLog(handNumber: Int, buttonSeat: Int): HandLog {
        return HandLog(
            seed = 0L,
            handNumber = handNumber,
            buttonSeat = buttonSeat,
            stacks = listOf(1000, 1000),
            smallBlind = 50,
            bigBlind = 100,
            actions = emptyList(),
            events = listOf(
                HandStarted(0, handNumber, buttonSeat, 50, 100, listOf(1000, 1000)),
            ),
        )
    }

    @Test
    fun carriesTheFormatTheButtonAndItsHands() {
        val format = DuelFormat.DEFAULT
        val buttonSeat = 0
        val hands = listOf(
            handLog(1, 0),
            handLog(2, 1),
        )
        val events = emptyList<MatchFinished>()

        val log = MatchLog(
            format = format,
            buttonSeat = buttonSeat,
            hands = hands,
            events = events,
        )

        Assertions.assertEquals(format, log.format)
        Assertions.assertEquals(buttonSeat, log.buttonSeat)
        Assertions.assertEquals(hands, log.hands)
        Assertions.assertEquals(events, log.events)
    }

    @Test
    fun defaultsToTheCurrentLogVersion() {
        val log = MatchLog(
            format = DuelFormat.DEFAULT,
            buttonSeat = 0,
            hands = emptyList(),
            events = emptyList(),
        )

        Assertions.assertEquals(MATCH_LOG_VERSION, log.version)
        Assertions.assertEquals(1, MATCH_LOG_VERSION)
    }

    @Test
    fun acceptsTheLogOfADuelStillRunning() {
        val log = MatchLog(
            format = DuelFormat.DEFAULT,
            buttonSeat = 0,
            hands = emptyList(),
            events = emptyList(),
        )

        Assertions.assertEquals(emptyList<HandLog>(), log.hands)
        Assertions.assertEquals(emptyList<MatchFinished>(), log.events)
    }

    @Test
    fun rejectsHandNumbersWithAGap() {
        assertThrows<IllegalArgumentException> {
            MatchLog(
                format = DuelFormat.DEFAULT,
                buttonSeat = 0,
                hands = listOf(
                    handLog(1, 0),
                    handLog(3, 1),
                ),
                events = emptyList(),
            )
        }
    }

    @Test
    fun rejectsAButtonThatDoesNotAlternate() {
        assertThrows<IllegalArgumentException> {
            MatchLog(
                format = DuelFormat.DEFAULT,
                buttonSeat = 0,
                hands = listOf(
                    handLog(1, 0),
                    handLog(2, 0),
                ),
                events = emptyList(),
            )
        }
    }

    @Test
    fun rejectsAMatchEventSequenceWithAGap() {
        assertThrows<IllegalArgumentException> {
            MatchLog(
                format = DuelFormat.DEFAULT,
                buttonSeat = 0,
                hands = emptyList(),
                events = listOf(
                    MatchFinished(0, DuelOutcome(winner = 0, handsPlayed = 1, finalStacks = listOf(11000, 9000))),
                    MatchFinished(2, DuelOutcome(winner = 0, handsPlayed = 1, finalStacks = listOf(11000, 9000))),
                ),
            )
        }
    }

    @Test
    fun rejectsTwoMatchFinishedEvents() {
        assertThrows<IllegalArgumentException> {
            MatchLog(
                format = DuelFormat.DEFAULT,
                buttonSeat = 0,
                hands = emptyList(),
                events = listOf(
                    MatchFinished(0, DuelOutcome(winner = 0, handsPlayed = 1, finalStacks = listOf(11000, 9000))),
                    MatchFinished(1, DuelOutcome(winner = 0, handsPlayed = 1, finalStacks = listOf(11000, 9000))),
                ),
            )
        }
    }
}
