package duels.poker.engine.duel

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class DuelFormatTest {
    @Test
    fun theDefaultMatchesTheRulesDocument() {
        DuelFormat.DEFAULT.startingStack shouldBe 10_000
        DuelFormat.DEFAULT.endCondition shouldBe EndCondition.Freezeout
        DuelFormat.DEFAULT.blinds.blindsFor(1) shouldBe BlindLevel(50, 100)
        DuelFormat.DEFAULT.blinds.blindsFor(11) shouldBe BlindLevel(75, 150)
        DuelFormat.DEFAULT.blinds.blindsFor(21) shouldBe BlindLevel(100, 200)
        DuelFormat.DEFAULT.blinds.blindsFor(31) shouldBe BlindLevel(150, 300)
        DuelFormat.DEFAULT.blinds.blindsFor(41) shouldBe BlindLevel(200, 400)
    }

    @Test
    fun theDefaultKeepsDoublingPastTheLastLevel() {
        DuelFormat.DEFAULT.blinds.blindsFor(51) shouldBe BlindLevel(400, 800)
    }

    @Test
    fun theFixedLengthAlternativeIsExpressible() {
        val format = DuelFormat(DuelFormat.DEFAULT.startingStack, DuelFormat.DEFAULT.blinds, EndCondition.FixedHands(25))
        format.endCondition shouldBe EndCondition.FixedHands(25)
    }

    @Test
    fun rejectsAStartingStackBelowTheFirstBigBlind() {
        shouldThrow<IllegalArgumentException> {
            DuelFormat(99, DuelFormat.DEFAULT.blinds, EndCondition.Freezeout)
        }
    }
}
