package duels.poker.engine.hand

import duels.poker.engine.card.Rank.FIVE
import duels.poker.engine.card.cards
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class ReferenceEvaluatorRulesTest {
    @Test
    fun theWheelIsTheLowestStraight() {
        val wheel = ReferenceHandEvaluator.evaluate(cards("Ah 5d 4c 3s 2h"))

        // The wheel should be a STRAIGHT
        assertEquals(HandCategory.STRAIGHT, wheel.category)
        // with FIVE as the high card
        assertEquals(listOf(FIVE), wheel.tiebreaks)

        // It should rank below the six-high straight
        val sixHigh = ReferenceHandEvaluator.evaluate(cards("6h 5d 4c 3s 2h"))
        assertTrue(wheel < sixHigh)

        // It should rank below every other straight
        val sevenHigh = ReferenceHandEvaluator.evaluate(cards("7h 6d 5c 4s 3h"))
        val eightHigh = ReferenceHandEvaluator.evaluate(cards("8h 7d 6c 5s 4h"))
        val nineHigh = ReferenceHandEvaluator.evaluate(cards("9h 8d 7c 6s 5h"))
        val tenHigh = ReferenceHandEvaluator.evaluate(cards("Th 9d 8c 7s 6h"))
        val jackHigh = ReferenceHandEvaluator.evaluate(cards("Jh Td 9c 8s 7h"))
        val queenHigh = ReferenceHandEvaluator.evaluate(cards("Qh Jd Tc 9s 8h"))
        val kingHigh = ReferenceHandEvaluator.evaluate(cards("Kh Qd Jc Ts 9h"))
        val aceHigh = ReferenceHandEvaluator.evaluate(cards("Ah Kd Qc Js Th"))

        assertTrue(wheel < sevenHigh)
        assertTrue(wheel < eightHigh)
        assertTrue(wheel < nineHigh)
        assertTrue(wheel < tenHigh)
        assertTrue(wheel < jackHigh)
        assertTrue(wheel < queenHigh)
        assertTrue(wheel < kingHigh)
        assertTrue(wheel < aceHigh)
    }

    @Test
    fun theSteelWheelIsTheLowestStraightFlush() {
        val steelWheel = ReferenceHandEvaluator.evaluate(cards("Ah 5h 4h 3h 2h"))

        // The steel wheel should be a STRAIGHT_FLUSH
        assertEquals(HandCategory.STRAIGHT_FLUSH, steelWheel.category)
        // with FIVE as the high card
        assertEquals(listOf(FIVE), steelWheel.tiebreaks)

        // It should rank below the six-high straight flush
        val sixHighFlush = ReferenceHandEvaluator.evaluate(cards("6h 5h 4h 3h 2h"))
        assertTrue(steelWheel < sixHighFlush)
    }

    @Test
    fun queenKingAceTwoThreeIsNotAStraight() {
        val unsuited = ReferenceHandEvaluator.evaluate(cards("Qh Ks Ad 2c 3h"))

        // Q-K-A-2-3 is not a straight (Ace acts as 1 only in A-2-3-4-5)
        assertEquals(HandCategory.HIGH_CARD, unsuited.category)

        val suited = ReferenceHandEvaluator.evaluate(cards("Qh Kh Ah 2h 3h"))

        // Q-K-A-2-3 all the same suit is a flush, not a straight flush
        assertEquals(HandCategory.FLUSH, suited.category)
    }

    @Test
    fun theRoyalFlushBeatsOneHandFromEveryOtherCategory() {
        val royalFlush = ReferenceHandEvaluator.evaluate(cards("As Ks Qs Js Ts"))

        // One representative hand from each other category
        val highCard = ReferenceHandEvaluator.evaluate(cards("Ah Kd Qh Jc 9s"))
        val pair = ReferenceHandEvaluator.evaluate(cards("Kh Kd 9c 8s 7h"))
        val twoPair = ReferenceHandEvaluator.evaluate(cards("Kh Kd 9c 9s 7h"))
        val threeOfAKind = ReferenceHandEvaluator.evaluate(cards("Kh Kd Kc 9s 8h"))
        val straight = ReferenceHandEvaluator.evaluate(cards("9h 8d 7c 6s 5h"))
        val flush = ReferenceHandEvaluator.evaluate(cards("Ah Kh Qh Jh 9h"))
        val fullHouse = ReferenceHandEvaluator.evaluate(cards("Kh Kd Kc 9s 9h"))
        val fourOfAKind = ReferenceHandEvaluator.evaluate(cards("Kh Kd Kc Ks 9h"))

        assertTrue(royalFlush > highCard)
        assertTrue(royalFlush > pair)
        assertTrue(royalFlush > twoPair)
        assertTrue(royalFlush > threeOfAKind)
        assertTrue(royalFlush > straight)
        assertTrue(royalFlush > flush)
        assertTrue(royalFlush > fullHouse)
        assertTrue(royalFlush > fourOfAKind)
    }

    @Test
    fun suitsNeverBreakTies() {
        val hand1 = ReferenceHandEvaluator.evaluate(cards("As Kd Qh 7c 2s"))
        val hand2 = ReferenceHandEvaluator.evaluate(cards("Ah Kc Qs 7d 2h"))

        // They should compare as equal
        assertEquals(0, hand1.compareTo(hand2))
        // And be equal
        assertEquals(hand1, hand2)

        // Same for two aces-full-of-kings in different suits
        val acesFull1 = ReferenceHandEvaluator.evaluate(cards("Ah Ad Ac Kh Kd"))
        val acesFull2 = ReferenceHandEvaluator.evaluate(cards("As Ac Ah Kc Ks"))

        assertEquals(0, acesFull1.compareTo(acesFull2))
        assertEquals(acesFull1, acesFull2)
    }

    @Test
    fun permutingTheSameFiveCardsNeverChangesTheRank() {
        val wheelCards = listOf("Ah", "5d", "4c", "3s", "2h")
        val twoPairCards = listOf("Kh", "Kd", "7c", "7s", "Ah")
        val flushCards = listOf("Ah", "Jh", "8h", "5h", "2h")
        val quadsCards = listOf("9h", "9d", "9c", "9s", "Ad")

        val testCases = listOf(
            wheelCards,
            twoPairCards,
            flushCards,
            quadsCards,
        )

        for (cards in testCases) {
            val perms = permutations(cards)
            val ranks = perms.map { perm ->
                ReferenceHandEvaluator.evaluate(
                    duels.poker.engine.card.cards(perm.joinToString(" ")),
                )
            }

            // All permutations should evaluate to the same rank
            val firstRank = ranks.first()
            for (rank in ranks) {
                assertEquals(firstRank, rank, "Permutation produced different rank for cards: ${cards.joinToString(" ")}")
            }
        }
    }

    private fun <T> permutations(items: List<T>): List<List<T>> =
        if (items.size <= 1) {
            listOf(items)
        } else {
            items.flatMap { head -> permutations(items - head).map { listOf(head) + it } }
        }
}
