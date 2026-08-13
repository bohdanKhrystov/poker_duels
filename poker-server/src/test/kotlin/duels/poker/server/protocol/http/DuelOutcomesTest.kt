package duels.poker.server.protocol.http

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class DuelOutcomesTest {
    @Test
    fun aPositiveDeltaIsAWin() {
        assertEquals(DuelOutcomeLabel.WON, outcomeOf(1))
    }

    @Test
    fun aNegativeDeltaIsALoss() {
        assertEquals(DuelOutcomeLabel.LOST, outcomeOf(-1))
    }

    @Test
    fun aZeroDeltaIsADraw() {
        assertEquals(DuelOutcomeLabel.DREW, outcomeOf(0))
    }

    @Test
    fun theLabelFollowsTheSignNotTheSizeOfTheAward() {
        assertEquals(DuelOutcomeLabel.WON, outcomeOf(7))
        assertEquals(DuelOutcomeLabel.LOST, outcomeOf(-7))
    }
}
