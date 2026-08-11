package duels.poker.engine.hand

import duels.poker.engine.card.Card
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ExhaustiveFiveCardTest {
    companion object {
        private val counts = IntArray(HandCategory.entries.size)
        private val distinct = HashSet<HandRank>()

        init {
            // Enumerate all 2,598,960 five-card combinations
            val all = Card.all
            for (a in 0..47) {
                for (b in a + 1..48) {
                    for (c in b + 1..49) {
                        for (d in c + 1..50) {
                            for (e in d + 1..51) {
                                val hand = listOf(all[a], all[b], all[c], all[d], all[e])
                                val rank = ReferenceHandEvaluator.evaluate(hand)
                                counts[rank.category.ordinal]++
                                distinct.add(rank)
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    fun everyCategoryOccursItsPublishedNumberOfTimes() {
        assertEquals(40, counts[HandCategory.STRAIGHT_FLUSH.ordinal], "STRAIGHT_FLUSH count")
        assertEquals(624, counts[HandCategory.FOUR_OF_A_KIND.ordinal], "FOUR_OF_A_KIND count")
        assertEquals(3744, counts[HandCategory.FULL_HOUSE.ordinal], "FULL_HOUSE count")
        assertEquals(5108, counts[HandCategory.FLUSH.ordinal], "FLUSH count")
        assertEquals(10200, counts[HandCategory.STRAIGHT.ordinal], "STRAIGHT count")
        assertEquals(54912, counts[HandCategory.THREE_OF_A_KIND.ordinal], "THREE_OF_A_KIND count")
        assertEquals(123552, counts[HandCategory.TWO_PAIR.ordinal], "TWO_PAIR count")
        assertEquals(1098240, counts[HandCategory.PAIR.ordinal], "PAIR count")
        assertEquals(1302540, counts[HandCategory.HIGH_CARD.ordinal], "HIGH_CARD count")
    }

    @Test
    fun theWholeSpaceCollapsesIntoExactlySevenThousandFourHundredAndSixtyTwoDistinctRanks() {
        assertEquals(7462, distinct.size)
    }
}
