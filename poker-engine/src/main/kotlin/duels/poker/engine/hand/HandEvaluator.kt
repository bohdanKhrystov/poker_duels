package duels.poker.engine.hand

import duels.poker.engine.card.Card

/**
 * Ranks a set of cards, producing the [HandRank] they form.
 */
public interface HandEvaluator {
    /** Ranks exactly five distinct cards. */
    public fun evaluate(cards: List<Card>): HandRank
}
