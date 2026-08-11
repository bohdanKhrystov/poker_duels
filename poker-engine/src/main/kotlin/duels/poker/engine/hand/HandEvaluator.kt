package duels.poker.engine.hand

import duels.poker.engine.card.Card

private const val SEVEN_CARDS = 7

/**
 * Ranks a set of cards, producing the [HandRank] they form.
 */
public interface HandEvaluator {
    /** Ranks exactly five distinct cards. */
    public fun evaluate(cards: List<Card>): HandRank

    /**
     * Finds the best five-card hand out of seven cards (two hole cards plus a five-card board),
     * together with which five cards make it. Neither hole card need be used; the board may play
     * in full.
     *
     * Walks all 21 five-card subsets of the seven and keeps the best by [HandRank]. On a tie, the
     * first subset found — in the fixed enumeration order below — is kept: the rank is what
     * matters for the outcome, and a stable choice makes which cards are highlighted reproducible.
     *
     * @throws IllegalArgumentException if [cards] is not exactly seven distinct cards
     */
    public fun bestOfSeven(cards: List<Card>): BestHand {
        require(cards.size == SEVEN_CARDS) { "Seven cards are required, got ${cards.size}" }
        require(cards.toSet().size == SEVEN_CARDS) { "Duplicate card in $cards" }

        var best: BestHand? = null
        for (a in 0..2) {
            for (b in a + 1..3) {
                for (c in b + 1..4) {
                    for (d in c + 1..5) {
                        for (e in d + 1..6) {
                            val subset = listOf(cards[a], cards[b], cards[c], cards[d], cards[e])
                            val rank = evaluate(subset)
                            if (best == null || rank > best.rank) {
                                best = BestHand(rank, subset)
                            }
                        }
                    }
                }
            }
        }

        return checkNotNull(best) { "Seven-card enumeration must produce a best hand" }
    }
}
