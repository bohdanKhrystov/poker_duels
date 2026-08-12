package duels.poker.engine.duel

import duels.poker.engine.game.Seat
import duels.poker.engine.game.Street
import duels.poker.engine.game.handState
import duels.poker.engine.game.seats
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

/** Tests for [recordHand]: it folds a settled hand's outcome back into the match. */
class RecordHandTest {
    @Test
    fun carriesTheFinalStacksIntoTheMatch() {
        val match = MatchState.start(DuelFormat.DEFAULT)
        val finished = handState(seats(13_000, 7_000)).copy(
            street = Street.COMPLETE,
            seatToAct = null,
            handNumber = 1,
        )

        val recorded = recordHand(match, finished)

        assertEquals(listOf(13_000, 7_000), recorded.stacks)
    }

    @Test
    fun countsTheHandAndPassesTheButton() {
        val match = MatchState.start(DuelFormat.DEFAULT)
        val finished = handState(seats()).copy(
            street = Street.COMPLETE,
            seatToAct = null,
            handNumber = 1,
        )

        val recorded = recordHand(match, finished)

        assertEquals(1, recorded.handsPlayed)
        assertEquals(1, recorded.buttonSeat)

        val secondFinished = handState(seats()).copy(
            street = Street.COMPLETE,
            seatToAct = null,
            handNumber = 2,
        )
        val recordedAgain = recordHand(recorded, secondFinished)

        assertEquals(0, recordedAgain.buttonSeat)
    }

    @Test
    fun theNextBlindLevelFollowsTheNewHandCount() {
        val match = MatchState.start(DuelFormat.DEFAULT).copy(handsPlayed = 9)
        val finished = handState(seats()).copy(
            street = Street.COMPLETE,
            seatToAct = null,
            handNumber = 10,
        )

        val recorded = recordHand(match, finished)

        assertEquals(BlindLevel(75, 150), recorded.blinds)
    }

    @Test
    fun refusesAHandThatIsNotOver() {
        val match = MatchState.start(DuelFormat.DEFAULT)
        val finished = handState(seats()).copy(handNumber = 1)

        assertThrows(IllegalArgumentException::class.java) {
            recordHand(match, finished)
        }
    }

    @Test
    fun refusesAHandOutOfSequence() {
        val match = MatchState.start(DuelFormat.DEFAULT)
        val finished = handState(seats()).copy(
            street = Street.COMPLETE,
            seatToAct = null,
            handNumber = 2,
        )

        assertThrows(IllegalArgumentException::class.java) {
            recordHand(match, finished)
        }
    }

    @Test
    fun refusesAHandThatChangedTheChipTotal() {
        val match = MatchState.start(DuelFormat.DEFAULT)
        val finished = handState(
            listOf(
                Seat(index = 0, stack = 9_999),
                Seat(index = 1, stack = 10_000),
            ),
        ).copy(
            street = Street.COMPLETE,
            seatToAct = null,
            handNumber = 1,
        )

        assertThrows(IllegalArgumentException::class.java) {
            recordHand(match, finished)
        }
    }
}
