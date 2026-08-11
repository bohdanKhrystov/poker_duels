package duels.poker.engine.hand

import duels.poker.engine.card.Rank.ACE
import duels.poker.engine.card.Rank.EIGHT
import duels.poker.engine.card.Rank.FIVE
import duels.poker.engine.card.Rank.JACK
import duels.poker.engine.card.Rank.KING
import duels.poker.engine.card.Rank.NINE
import duels.poker.engine.card.Rank.SEVEN
import duels.poker.engine.card.Rank.THREE
import duels.poker.engine.card.Rank.TWO
import duels.poker.engine.card.cards
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

internal class ReferenceHandEvaluatorTest {
    @Test
    fun straightFlushRanksByItsHighCard() {
        val rank = ReferenceHandEvaluator.evaluate(cards("9h 8h 7h 6h 5h"))

        assertEquals(HandCategory.STRAIGHT_FLUSH, rank.category)
        assertEquals(listOf(NINE), rank.tiebreaks)
    }

    @Test
    fun fourOfAKindRanksTheQuadThenTheKicker() {
        val rank = ReferenceHandEvaluator.evaluate(cards("9h 9d 9c 9s Ad"))

        assertEquals(HandCategory.FOUR_OF_A_KIND, rank.category)
        assertEquals(listOf(NINE, ACE), rank.tiebreaks)
    }

    @Test
    fun fullHouseRanksTheTripsThenThePair() {
        val rank = ReferenceHandEvaluator.evaluate(cards("3h 3d 3c Kh Kd"))

        assertEquals(HandCategory.FULL_HOUSE, rank.category)
        assertEquals(listOf(THREE, KING), rank.tiebreaks)
    }

    @Test
    fun flushRanksAllFiveDescending() {
        val rank = ReferenceHandEvaluator.evaluate(cards("Ah Jh 8h 5h 2h"))

        assertEquals(HandCategory.FLUSH, rank.category)
        assertEquals(listOf(ACE, JACK, EIGHT, FIVE, TWO), rank.tiebreaks)
    }

    @Test
    fun straightRanksByItsHighCard() {
        val rank = ReferenceHandEvaluator.evaluate(cards("9h 8d 7c 6s 5h"))

        assertEquals(HandCategory.STRAIGHT, rank.category)
        assertEquals(listOf(NINE), rank.tiebreaks)
    }

    @Test
    fun theWheelIsAFiveHighStraight() {
        val rank = ReferenceHandEvaluator.evaluate(cards("Ah 5d 4c 3s 2h"))

        assertEquals(HandCategory.STRAIGHT, rank.category)
        assertEquals(listOf(FIVE), rank.tiebreaks)
    }

    @Test
    fun threeOfAKindRanksTripsThenTwoKickers() {
        val rank = ReferenceHandEvaluator.evaluate(cards("9h 9d 9c Ah 5d"))

        assertEquals(HandCategory.THREE_OF_A_KIND, rank.category)
        assertEquals(listOf(NINE, ACE, FIVE), rank.tiebreaks)
    }

    @Test
    fun twoPairRanksHighPairLowPairThenKicker() {
        val rank = ReferenceHandEvaluator.evaluate(cards("Kh Kd 7c 7s Ah"))

        assertEquals(HandCategory.TWO_PAIR, rank.category)
        assertEquals(listOf(KING, SEVEN, ACE), rank.tiebreaks)
    }

    @Test
    fun onePairRanksThePairThenThreeKickers() {
        val rank = ReferenceHandEvaluator.evaluate(cards("5h 5d Ac Kh 2d"))

        assertEquals(HandCategory.PAIR, rank.category)
        assertEquals(listOf(FIVE, ACE, KING, TWO), rank.tiebreaks)
    }

    @Test
    fun highCardRanksAllFiveDescending() {
        val rank = ReferenceHandEvaluator.evaluate(cards("Ah Jd 8c 5s 2h"))

        assertEquals(HandCategory.HIGH_CARD, rank.category)
        assertEquals(listOf(ACE, JACK, EIGHT, FIVE, TWO), rank.tiebreaks)
    }

    @Test
    fun rejectsAHandThatIsNotFiveCards() {
        assertThrows(IllegalArgumentException::class.java) {
            ReferenceHandEvaluator.evaluate(cards("Ah Jd 8c 5s"))
        }
        assertThrows(IllegalArgumentException::class.java) {
            ReferenceHandEvaluator.evaluate(cards("Ah Jd 8c 5s 2h Kd"))
        }
    }
}
