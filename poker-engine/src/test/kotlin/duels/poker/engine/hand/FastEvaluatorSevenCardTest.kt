package duels.poker.engine.hand

import duels.poker.engine.card.Deck
import duels.poker.engine.random.SplitMix64Rng
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class FastEvaluatorSevenCardTest {
    @Test
    fun agreesWithTheReferenceOnOneHundredThousandDealtHands() {
        for (seed in 1..100_000) {
            val sevenCards = Deck.full().shuffled(SplitMix64Rng(seed.toLong())).deck.deal(7).cards

            val referenceBest = ReferenceHandEvaluator.bestOfSeven(sevenCards)
            val fastBest = FastHandEvaluator.bestOfSeven(sevenCards)

            assertEquals(
                referenceBest.rank,
                fastBest.rank,
                "Seed $seed: seven cards $sevenCards, reference rank ${referenceBest.rank}, fast rank ${fastBest.rank}",
            )

            assertEquals(
                referenceBest.cards,
                fastBest.cards,
                "Seed $seed: seven cards $sevenCards, reference cards ${referenceBest.cards}, fast cards ${fastBest.cards}",
            )
        }
    }
}
