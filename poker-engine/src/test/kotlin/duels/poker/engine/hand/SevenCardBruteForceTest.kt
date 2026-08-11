package duels.poker.engine.hand

import duels.poker.engine.card.Card
import duels.poker.engine.card.Deck
import duels.poker.engine.random.SplitMix64Rng
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class SevenCardBruteForceTest {
    @Test
    fun bestOfSevenEqualsTheMaximumOfTheTwentyOneSubsets() {
        for (seed in 1..10_000) {
            val sevenCards = Deck.full().shuffled(SplitMix64Rng(seed.toLong())).deck.deal(7).cards
            val best = ReferenceHandEvaluator.bestOfSeven(sevenCards)
            val oracleMax = computeOracleMaxRank(sevenCards)

            assertEquals(
                oracleMax,
                best.rank,
                "Seed $seed: seven cards $sevenCards",
            )
        }
    }

    @Test
    fun theReturnedFiveCardsAlwaysMakeTheReturnedRank() {
        for (seed in 1..10_000) {
            val sevenCards = Deck.full().shuffled(SplitMix64Rng(seed.toLong())).deck.deal(7).cards
            val best = ReferenceHandEvaluator.bestOfSeven(sevenCards)

            assertEquals(
                5,
                best.cards.size,
                "Seed $seed: returned cards count mismatch",
            )
            assertEquals(
                5,
                best.cards.toSet().size,
                "Seed $seed: returned cards must be distinct",
            )
            assertTrue(
                sevenCards.containsAll(best.cards),
                "Seed $seed: seven cards $sevenCards, returned cards ${best.cards} are not from the seven",
            )

            val evaluatedRank = ReferenceHandEvaluator.evaluate(best.cards)
            assertEquals(
                best.rank,
                evaluatedRank,
                "Seed $seed: seven cards $sevenCards, returned cards ${best.cards}",
            )
        }
    }

    @Test
    fun noSubsetOfTheSevenBeatsTheReturnedRank() {
        for (seed in 1..10_000) {
            val sevenCards = Deck.full().shuffled(SplitMix64Rng(seed.toLong())).deck.deal(7).cards
            val best = ReferenceHandEvaluator.bestOfSeven(sevenCards)

            // Enumerate all 21 subsets using bitmask
            for (mask in 0 until 128) {
                if (Integer.bitCount(mask) != 5) continue

                val subset = extractSubset(sevenCards, mask)
                val subsetRank = ReferenceHandEvaluator.evaluate(subset)

                assertTrue(
                    subsetRank <= best.rank,
                    "Seed $seed: seven cards $sevenCards, " +
                        "subset $subset ranks ${subsetRank.category} > ${best.rank.category}",
                )
            }
        }
    }

    /**
     * Independently compute the maximum rank among all 21 five-card subsets of seven cards.
     * This oracle does NOT reuse bestOfSeven's enumeration — it uses bitmask filtering instead.
     */
    private fun computeOracleMaxRank(sevenCards: List<Card>): HandRank {
        var maxRank: HandRank? = null

        // Enumerate all 21 subsets using bitmask: 7 bits for 7 cards, filter for exactly 5 bits set
        for (mask in 0 until 128) {
            if (Integer.bitCount(mask) != 5) continue

            val subset = extractSubset(sevenCards, mask)
            val rank = ReferenceHandEvaluator.evaluate(subset)

            if (maxRank == null || rank > maxRank) {
                maxRank = rank
            }
        }

        return checkNotNull(maxRank) { "Oracle must find a maximum rank among 21 subsets" }
    }

    /**
     * Extract the five cards from a seven-card hand where the bitmask has five 1-bits.
     * Each 1-bit position corresponds to a card index.
     */
    private fun extractSubset(sevenCards: List<Card>, mask: Int): List<Card> {
        val subset = mutableListOf<Card>()
        for (i in 0 until 7) {
            if ((mask and (1 shl i)) != 0) {
                subset.add(sevenCards[i])
            }
        }
        return subset
    }
}
