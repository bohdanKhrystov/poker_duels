package duels.poker.engine.hand

import duels.poker.engine.card.Card
import duels.poker.engine.card.Rank
import duels.poker.engine.card.Suit
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
private const val ONE = 1
private const val STRAIGHT_WINDOW = 0b11111
private const val WHEEL_LOW_FOUR = 0b1111
private const val LOWEST_STRAIGHT_HIGH_ORDINAL = 4
private const val HIGHEST_STRAIGHT_HIGH_ORDINAL = 12

private val RANK_COUNT = Rank.entries.size
private val SUIT_COUNT = Suit.entries.size

/**
 * Precomputed rank-mask patterns for every straight, each paired with its high [Rank].
 *
 * Nine masks slide a five-bit window across [Rank.SIX] through [Rank.ACE] high; the tenth is the
 * wheel (ace playing low under five, four, three, two), headed by [Rank.FIVE]. There is
 * deliberately no wraparound mask: `QUEEN, KING, ACE, TWO, THREE` is not a straight, and adding a
 * mask that would make it one is exactly the bug this evaluator exists to avoid.
 */
private val STRAIGHT_MASKS: List<Pair<Int, Rank>> = buildList {
    for (highOrdinal in LOWEST_STRAIGHT_HIGH_ORDINAL..HIGHEST_STRAIGHT_HIGH_ORDINAL) {
        add((STRAIGHT_WINDOW shl (highOrdinal - LOWEST_STRAIGHT_HIGH_ORDINAL)) to Rank.entries[highOrdinal])
    }
    add(((ONE shl Rank.ACE.ordinal) or WHEEL_LOW_FOUR) to Rank.FIVE)
}

/**
 * A second, faster [HandEvaluator] that decides a five-card hand from a rank bitmask and two
 * count tables instead of sorting or grouping.
 *
 * [ReferenceHandEvaluator] remains the oracle: it is obviously correct, this one is merely fast.
 * Any change to this file must keep `FastEvaluatorEquivalenceTest` green against it. A
 * disagreement between the two is always a defect here, never a reason to touch the reference.
 */
public object FastHandEvaluator : HandEvaluator {
    override fun evaluate(cards: List<Card>): HandRank {
        require(cards.size == HAND_SIZE) { "A five-card hand is required, got ${cards.size}" }
        require(cards.toSet().size == HAND_SIZE) { "Duplicate card in $cards" }

        val counts = IntArray(RANK_COUNT)
        val suitCounts = IntArray(SUIT_COUNT)
        var rankMask = 0
        for (card in cards) {
            val rankIndex = card.rank.ordinal
            counts[rankIndex]++
            rankMask = rankMask or (ONE shl rankIndex)
            suitCounts[card.suit.ordinal]++
        }

        val flush = suitCounts.any { it == HAND_SIZE }
        val straightHigh = STRAIGHT_MASKS.firstOrNull { it.first == rankMask }?.second

        var quadRank: Rank? = null
        var tripsRank: Rank? = null
        val pairRanks = mutableListOf<Rank>()
        val singleRanks = mutableListOf<Rank>()
        for (rankIndex in RANK_COUNT - 1 downTo 0) {
            val rank = Rank.entries[rankIndex]
            when (counts[rankIndex]) {
                QUADS -> quadRank = rank
                TRIPS -> tripsRank = rank
                PAIR_SIZE -> pairRanks.add(rank)
                ONE -> singleRanks.add(rank)
            }
        }

        return when {
            flush && straightHigh != null -> HandRank(STRAIGHT_FLUSH, listOf(straightHigh))
            quadRank != null -> HandRank(FOUR_OF_A_KIND, listOf(quadRank, singleRanks[0]))
            tripsRank != null && pairRanks.isNotEmpty() ->
                HandRank(FULL_HOUSE, listOf(tripsRank, pairRanks[0]))
            flush -> HandRank(FLUSH, singleRanks)
            straightHigh != null -> HandRank(STRAIGHT, listOf(straightHigh))
            tripsRank != null -> HandRank(THREE_OF_A_KIND, listOf(tripsRank, singleRanks[0], singleRanks[1]))
            pairRanks.size == PAIR_SIZE ->
                HandRank(TWO_PAIR, listOf(pairRanks[0], pairRanks[1], singleRanks[0]))
            pairRanks.size == ONE ->
                HandRank(PAIR, listOf(pairRanks[0], singleRanks[0], singleRanks[1], singleRanks[2]))
            else -> HandRank(HIGH_CARD, singleRanks)
        }
    }
}
