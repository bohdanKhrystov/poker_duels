package duels.poker.engine.duel

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class OutcomeOfTest {
    @Test
    fun aFreezeoutHasNoOutcomeWhileBothSeatsHaveChips() {
        val match = MatchState.start(DuelFormat.DEFAULT).copy(
            handsPlayed = 7,
            stacks = listOf(5_000, 15_000),
        )

        outcomeOf(match) shouldBe null
    }

    @Test
    fun aFreezeoutEndsWhenOneSeatHoldsEveryChip() {
        val match = MatchState.start(DuelFormat.DEFAULT).copy(
            handsPlayed = 31,
            stacks = listOf(0, 20_000),
        )

        val outcome = outcomeOf(match)

        outcome shouldBe DuelOutcome(1, 31, listOf(0, 20_000))
        outcome?.isDraw shouldBe false
    }

    @Test
    fun aFixedLengthDuelRunsToItsLastHand() {
        val format = DuelFormat.DEFAULT.copy(endCondition = EndCondition.FixedHands(25))

        val stillRunning = MatchState.start(format).copy(
            handsPlayed = 24,
            stacks = listOf(9_000, 11_000),
        )
        outcomeOf(stillRunning) shouldBe null

        val finished = MatchState.start(format).copy(
            handsPlayed = 25,
            stacks = listOf(9_000, 11_000),
        )
        outcomeOf(finished)?.winner shouldBe 1
    }

    @Test
    fun aLevelFixedLengthDuelIsADraw() {
        val format = DuelFormat.DEFAULT.copy(endCondition = EndCondition.FixedHands(25))
        val match = MatchState.start(format).copy(
            handsPlayed = 25,
            stacks = listOf(10_000, 10_000),
        )

        val outcome = outcomeOf(match)

        outcome?.winner shouldBe null
        outcome?.isDraw shouldBe true
    }

    @Test
    fun aFixedLengthDuelStillEndsWhenASeatIsBroke() {
        val format = DuelFormat.DEFAULT.copy(endCondition = EndCondition.FixedHands(25))
        val match = MatchState.start(format).copy(
            handsPlayed = 6,
            stacks = listOf(20_000, 0),
        )

        outcomeOf(match)?.winner shouldBe 0
    }
}
