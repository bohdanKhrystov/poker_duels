package duels.poker.engine.hand

import duels.poker.engine.card.Card
import duels.poker.engine.card.Rank.ACE
import duels.poker.engine.card.Rank.KING
import duels.poker.engine.card.Rank.SEVEN
import duels.poker.engine.card.cards
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Proves [ReferenceHandEvaluator.bestOfSeven] against seven-card hands: it picks the strongest of
 * the 21 five-card subsets and reports exactly which five cards make it.
 */
internal class SevenCardEvaluationTest {
    @Test
    fun picksTheBestOfTheTwentyOneSubsets() {
        val best = ReferenceHandEvaluator.bestOfSeven(cards("As Ks 7h 7d 7c 2s 3d"))

        assertEquals(HandCategory.THREE_OF_A_KIND, best.rank.category)
        assertEquals(listOf(SEVEN, ACE, KING), best.rank.tiebreaks)
    }

    @Test
    fun theBoardCanPlayInFull() {
        val best = ReferenceHandEvaluator.bestOfSeven(cards("2c 3d Ah Kh Qh Jh Th"))

        assertEquals(HandCategory.STRAIGHT_FLUSH, best.rank.category)
        assertEquals(listOf(ACE), best.rank.tiebreaks)
        assertEquals(cards("Ah Kh Qh Jh Th").toSet(), best.cards.toSet())
    }

    @Test
    fun oneHoleCardCanPlay() {
        val best = ReferenceHandEvaluator.bestOfSeven(cards("Ah 2c Kh Qh Jh Th 5d"))

        assertEquals(HandCategory.STRAIGHT_FLUSH, best.rank.category)
        assertEquals(listOf(ACE), best.rank.tiebreaks)
        assertTrue(Card.parse("Ah") in best.cards)
    }

    @Test
    fun theReturnedCardsAreFiveOfTheSevenAndMakeTheReturnedRank() {
        val hands =
            listOf(
                cards("As Ks 7h 7d 7c 2s 3d"),
                cards("2c 3d Ah Kh Qh Jh Th"),
                cards("Ah 2c Kh Qh Jh Th 5d"),
            )

        for (seven in hands) {
            val best = ReferenceHandEvaluator.bestOfSeven(seven)

            assertEquals(FIVE_CARD_HAND, best.cards.size)
            assertTrue(best.cards.all { it in seven })
            assertEquals(best.rank, ReferenceHandEvaluator.evaluate(best.cards))
        }
    }

    @Test
    fun orderOfTheSevenCardsDoesNotChangeTheRank() {
        val hands =
            listOf(
                cards("As Ks 7h 7d 7c 2s 3d"),
                cards("2c 3d Ah Kh Qh Jh Th"),
                cards("Ah 2c Kh Qh Jh Th 5d"),
            )

        for (seven in hands) {
            val rank = ReferenceHandEvaluator.bestOfSeven(seven).rank
            val reversed = ReferenceHandEvaluator.bestOfSeven(seven.reversed()).rank
            val rotated = ReferenceHandEvaluator.bestOfSeven(seven.rotated(3)).rank

            assertEquals(rank, reversed)
            assertEquals(rank, rotated)
        }
    }

    @Test
    fun rejectsAnythingOtherThanSevenDistinctCards() {
        assertThrows(IllegalArgumentException::class.java) {
            ReferenceHandEvaluator.bestOfSeven(cards("As Ks 7h 7d 7c 2s"))
        }
        assertThrows(IllegalArgumentException::class.java) {
            ReferenceHandEvaluator.bestOfSeven(cards("As Ks 7h 7d 7c 2s 3d 4d"))
        }
        assertThrows(IllegalArgumentException::class.java) {
            ReferenceHandEvaluator.bestOfSeven(
                listOf(Card.parse("As")) + cards("As Ks 7h 7d 7c 2s"),
            )
        }
    }

    private fun <T> List<T>.rotated(shift: Int): List<T> {
        val offset = shift % size
        return subList(offset, size) + subList(0, offset)
    }

    private companion object {
        private const val FIVE_CARD_HAND = 5
    }
}
