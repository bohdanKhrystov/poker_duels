package duels.poker.engine.hand

import duels.poker.engine.card.Rank.ACE
import duels.poker.engine.card.Rank.EIGHT
import duels.poker.engine.card.Rank.FIVE
import duels.poker.engine.card.Rank.FOUR
import duels.poker.engine.card.Rank.JACK
import duels.poker.engine.card.Rank.KING
import duels.poker.engine.card.Rank.NINE
import duels.poker.engine.card.Rank.QUEEN
import duels.poker.engine.card.Rank.SEVEN
import duels.poker.engine.card.Rank.SIX
import duels.poker.engine.card.Rank.TEN
import duels.poker.engine.card.Rank.THREE
import duels.poker.engine.card.Rank.TWO
import duels.poker.engine.hand.HandCategory.FLUSH
import duels.poker.engine.hand.HandCategory.FOUR_OF_A_KIND
import duels.poker.engine.hand.HandCategory.FULL_HOUSE
import duels.poker.engine.hand.HandCategory.HIGH_CARD
import duels.poker.engine.hand.HandCategory.PAIR
import duels.poker.engine.hand.HandCategory.STRAIGHT
import duels.poker.engine.hand.HandCategory.STRAIGHT_FLUSH
import duels.poker.engine.hand.HandCategory.THREE_OF_A_KIND
import duels.poker.engine.hand.HandCategory.TWO_PAIR
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class HandRankTest {
    @Test
    fun categoryDecidesBeforeAnyTiebreak() {
        val straight = HandRank(STRAIGHT, listOf(FIVE))
        val threeOfAKind = HandRank(THREE_OF_A_KIND, listOf(ACE, KING, QUEEN))
        assertTrue(straight > threeOfAKind)
    }

    @Test
    fun tiebreaksDecideWithinACategory() {
        val aceFullHouse = HandRank(FULL_HOUSE, listOf(ACE, TWO))
        val kingFullHouse = HandRank(FULL_HOUSE, listOf(KING, ACE))
        assertTrue(aceFullHouse > kingFullHouse)
    }

    @Test
    fun laterTiebreaksOnlyMatterWhenEarlierOnesTie() {
        val kingHighPair = HandRank(PAIR, listOf(KING, ACE, THREE, TWO))
        val kingQueenPair = HandRank(PAIR, listOf(KING, QUEEN, JACK, TEN))
        assertTrue(kingHighPair > kingQueenPair)
    }

    @Test
    fun identicalRanksCompareEqual() {
        val rank1 = HandRank(TWO_PAIR, listOf(KING, SEVEN, ACE))
        val rank2 = HandRank(TWO_PAIR, listOf(KING, SEVEN, ACE))
        assertEquals(0, rank1.compareTo(rank2))
        assertEquals(rank1, rank2)
        assertEquals(rank1.hashCode(), rank2.hashCode())
    }

    @Test
    fun theOrderingIsAntisymmetricAndTransitive() {
        // Create at least one hand per category in ascending order
        val ranks = listOf(
            HandRank(HIGH_CARD, listOf(ACE)),
            HandRank(PAIR, listOf(TWO)),
            HandRank(TWO_PAIR, listOf(THREE, TWO)),
            HandRank(THREE_OF_A_KIND, listOf(FOUR)),
            HandRank(STRAIGHT, listOf(FIVE)),
            HandRank(FLUSH, listOf(SIX)),
            HandRank(FULL_HOUSE, listOf(SEVEN, EIGHT)),
            HandRank(FOUR_OF_A_KIND, listOf(NINE)),
            HandRank(STRAIGHT_FLUSH, listOf(TEN)),
        )

        // Verify antisymmetry: for i < j, a[i] < a[j] and a[j] > a[i]
        for (i in ranks.indices) {
            for (j in i + 1 until ranks.size) {
                assertTrue(ranks[i] < ranks[j]) { "ranks[$i] should be < ranks[$j]" }
                assertTrue(ranks[j] > ranks[i]) { "ranks[$j] should be > ranks[$i]" }
            }
        }

        // Verify transitivity: sort a shuffled copy and ensure it matches the original order
        val shuffled = ranks.shuffled()
        val sorted = shuffled.sorted()
        assertEquals(ranks, sorted)
    }

    @Test
    fun rejectsAnEmptyTiebreakList() {
        assertThrows(IllegalArgumentException::class.java) {
            HandRank(FLUSH, emptyList())
        }
    }
}
