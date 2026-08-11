package duels.poker.engine.hand

import duels.poker.engine.card.Card
import duels.poker.engine.hand.HandCategory.FLUSH
import duels.poker.engine.hand.HandCategory.FOUR_OF_A_KIND
import duels.poker.engine.hand.HandCategory.FULL_HOUSE
import duels.poker.engine.hand.HandCategory.HIGH_CARD
import duels.poker.engine.hand.HandCategory.PAIR
import duels.poker.engine.hand.HandCategory.STRAIGHT
import duels.poker.engine.hand.HandCategory.STRAIGHT_FLUSH
import duels.poker.engine.hand.HandCategory.THREE_OF_A_KIND
import duels.poker.engine.hand.HandCategory.TWO_PAIR

private const val HAND_SIZE = 5
private const val QUADS = 4
private const val TRIPS = 3
private const val PAIR_SIZE = 2

/**
 * The test oracle for five-card hand evaluation: obviously correct, not fast.
 *
 * Every later, faster evaluator is measured against this one, so keep it readable rather than
 * clever — speed is a different ticket's problem, and cleverness here destroys its only reason
 * to exist.
 */
public object ReferenceHandEvaluator : HandEvaluator {
    override fun evaluate(cards: List<Card>): HandRank {
        require(cards.size == HAND_SIZE) { "A five-card hand is required, got ${cards.size}" }
        require(cards.toSet().size == HAND_SIZE) { "Duplicate card in $cards" }

        val ranks = cards.map { it.rank }
        val descending = ranks.sortedDescending()
        val groups = rankGroups(ranks)
        val flush = cards.all { it.suit == cards.first().suit }
        val straightHigh = straightHighOrNull(ranks)

        return when {
            flush && straightHigh != null -> HandRank(STRAIGHT_FLUSH, listOf(straightHigh))
            groups[0].size == QUADS -> HandRank(FOUR_OF_A_KIND, groups.map { it.rank })
            groups[0].size == TRIPS && groups[1].size == PAIR_SIZE ->
                HandRank(FULL_HOUSE, groups.map { it.rank })
            flush -> HandRank(FLUSH, descending)
            straightHigh != null -> HandRank(STRAIGHT, listOf(straightHigh))
            groups[0].size == TRIPS -> HandRank(THREE_OF_A_KIND, groups.map { it.rank })
            groups[0].size == PAIR_SIZE && groups[1].size == PAIR_SIZE ->
                HandRank(TWO_PAIR, groups.map { it.rank })
            groups[0].size == PAIR_SIZE -> HandRank(PAIR, groups.map { it.rank })
            else -> HandRank(HIGH_CARD, descending)
        }
    }
}
