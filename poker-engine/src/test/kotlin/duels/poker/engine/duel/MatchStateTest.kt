package duels.poker.engine.duel

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class MatchStateTest {
    @Test
    fun startDealsBothSeatsTheFormatStack() {
        val match = MatchState.start(DuelFormat.DEFAULT)
        match.stacks shouldBe listOf(10_000, 10_000)
        match.handsPlayed shouldBe 0
        match.buttonSeat shouldBe 0
    }

    @Test
    fun theNextHandFollowsTheHandsPlayed() {
        MatchState.start(DuelFormat.DEFAULT).copy(handsPlayed = 0).nextHandNumber shouldBe 1
        MatchState.start(DuelFormat.DEFAULT).copy(handsPlayed = 10).nextHandNumber shouldBe 11
    }

    @Test
    fun theBlindsAreTheScheduleAtTheNextHand() {
        MatchState.start(DuelFormat.DEFAULT).copy(handsPlayed = 0).blinds shouldBe BlindLevel(50, 100)
        MatchState.start(DuelFormat.DEFAULT).copy(handsPlayed = 10).blinds shouldBe BlindLevel(75, 150)
        MatchState.start(DuelFormat.DEFAULT).copy(handsPlayed = 40).blinds shouldBe BlindLevel(200, 400)
    }

    @Test
    fun chipsAreTheTwoStacksTogether() {
        val match = MatchState.start(DuelFormat.DEFAULT).copy(stacks = listOf(3_000, 17_000))
        match.chips shouldBe 20_000
    }

    @Test
    fun rejectsAMalformedMatch() {
        val valid = MatchState.start(DuelFormat.DEFAULT)

        shouldThrow<IllegalArgumentException> {
            valid.copy(stacks = listOf(10_000))
        }
        shouldThrow<IllegalArgumentException> {
            valid.copy(stacks = listOf(-1, 10_000))
        }
        shouldThrow<IllegalArgumentException> {
            valid.copy(buttonSeat = 2)
        }
        shouldThrow<IllegalArgumentException> {
            valid.copy(handsPlayed = -1)
        }
    }
}
