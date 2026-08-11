package duels.poker.engine.card

import duels.poker.engine.random.SplitMix64Rng
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Evidence that the shuffle is unbiased: over fixed seeds, no card favours any position
 * and no card is over- or under-represented at the top.
 */
class ShuffleDistributionTest {

    @Test
    fun everyCardReachesEveryPositionAtCloseToUniformFrequency() {
        val deck = Deck.full()
        // 52x52 table: counts[cardIndex][position]
        val counts = Array(Card.all.size) { IntArray(deck.remaining) }

        // Sample across 20,000 fixed seeds to build the distribution table
        for (seed in 1..20_000) {
            var rng: duels.poker.engine.random.Rng = SplitMix64Rng(seed.toLong())
            val shuffleResult = deck.shuffled(rng)
            rng = shuffleResult.rng
            val shuffled = shuffleResult.deck

            // Deal all 52 cards and record their positions
            val dealt = shuffled.deal(shuffled.remaining)
            for ((position, card) in dealt.cards.withIndex()) {
                val cardIndex = Card.all.indexOf(card)
                counts[cardIndex][position]++
            }
        }

        // Expected count per cell: 20_000 / 52 ≈ 384.6
        // Assert all 2704 cells lie within 25% of expected: 288..481
        val expectedCount = 20_000.0 / Card.all.size
        val minBound = 288
        val maxBound = 481

        var failedCells = 0
        val failureMessages = mutableListOf<String>()

        for ((cardIndex, positionCounts) in counts.withIndex()) {
            val card = Card.all[cardIndex]
            for ((position, count) in positionCounts.withIndex()) {
                if (count !in minBound..maxBound) {
                    failedCells++
                    if (failureMessages.size < 20) { // Limit messages to first 20 failures
                        failureMessages.add(
                            "Card $card at position $position: count=$count " +
                                "(expected ~${expectedCount.toInt()}, bounds $minBound..$maxBound)",
                        )
                    }
                }
            }
        }

        if (failedCells > 0) {
            val summary = "Found $failedCells out-of-bounds cells (showing first 20):\n" +
                failureMessages.joinToString("\n")
            assertTrue(false) { summary }
        }
    }

    @Test
    fun theTopCardIsUniformAcrossSeeds() {
        val deck = Deck.full()
        // Count how often each card appears at position 0 across all seeds
        val topCardCounts = IntArray(Card.all.size)

        // Sample across 52,000 fixed seeds
        for (seed in 1..52_000) {
            var rng: duels.poker.engine.random.Rng = SplitMix64Rng(seed.toLong())
            val shuffleResult = deck.shuffled(rng)
            val shuffled = shuffleResult.deck

            // Deal just the top card
            val dealt = shuffled.deal(1)
            val topCard = dealt.cards[0]
            val cardIndex = Card.all.indexOf(topCard)
            topCardCounts[cardIndex]++
        }

        // Expected count per card: 52_000 / 52 = 1000
        // Assert all cards' counts lie within 20%: 800..1200
        val expectedCount = 52_000 / Card.all.size
        val minBound = 800
        val maxBound = 1200

        var failedCards = 0
        val failureMessages = mutableListOf<String>()

        for ((cardIndex, count) in topCardCounts.withIndex()) {
            if (count !in minBound..maxBound) {
                failedCards++
                if (failureMessages.size < 20) { // Limit messages to first 20 failures
                    val card = Card.all[cardIndex]
                    failureMessages.add(
                        "Card $card: appeared $count times at top " +
                            "(expected $expectedCount, bounds $minBound..$maxBound)",
                    )
                }
            }
        }

        if (failedCards > 0) {
            val summary = "Found $failedCards cards out of distribution (showing first 20):\n" +
                failureMessages.joinToString("\n")
            assertTrue(false) { summary }
        }
    }
}
