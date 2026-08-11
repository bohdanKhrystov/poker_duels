package duels.poker.engine.hand

import duels.poker.engine.card.Card
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class FastEvaluatorEquivalenceTest {
    @Test
    fun agreesWithTheReferenceOnEveryFiveCardHand() {
        val all = Card.all
        for (a in 0..47) {
            for (b in a + 1..48) {
                for (c in b + 1..49) {
                    for (d in c + 1..50) {
                        for (e in d + 1..51) {
                            val hand = listOf(all[a], all[b], all[c], all[d], all[e])
                            val expected = ReferenceHandEvaluator.evaluate(hand)
                            val actual = FastHandEvaluator.evaluate(hand)
                            assertEquals(
                                expected,
                                actual,
                                "hand=$hand reference=$expected fast=$actual",
                            )
                        }
                    }
                }
            }
        }
    }

    @Test
    fun rejectsAHandThatIsNotFiveDistinctCards() {
        val fourCards = listOf(
            Card.parse("As"),
            Card.parse("Kd"),
            Card.parse("Qh"),
            Card.parse("Jc"),
        )
        assertThrows(IllegalArgumentException::class.java) { FastHandEvaluator.evaluate(fourCards) }

        val sixCards = listOf(
            Card.parse("As"),
            Card.parse("Kd"),
            Card.parse("Qh"),
            Card.parse("Jc"),
            Card.parse("Ts"),
            Card.parse("9d"),
        )
        assertThrows(IllegalArgumentException::class.java) { FastHandEvaluator.evaluate(sixCards) }

        val duplicateCard = listOf(
            Card.parse("As"),
            Card.parse("Kd"),
            Card.parse("Qh"),
            Card.parse("Jc"),
            Card.parse("As"),
        )
        assertThrows(IllegalArgumentException::class.java) { FastHandEvaluator.evaluate(duplicateCard) }
    }
}
