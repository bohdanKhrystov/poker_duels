package duels.poker.engine.hand

import duels.poker.engine.card.Rank

private const val MAX_TIEBREAKS = 5

/**
 * A total ordering over all poker hands.
 *
 * [tiebreaks] is ordered by **descending significance**: the most important rank first.
 * Suits are absent by design because they never break ties. Equal ranks mean a split pot.
 *
 * @property category the hand category, compared first
 * @property tiebreaks the ranks that break ties within the category, in order of importance (most to least)
 */
public data class HandRank(
    val category: HandCategory,
    val tiebreaks: List<Rank>,
) : Comparable<HandRank> {
    init {
        require(tiebreaks.isNotEmpty()) { "A hand rank needs at least one tiebreak rank" }
        require(tiebreaks.size <= MAX_TIEBREAKS) { "At most five tiebreak ranks: $tiebreaks" }
    }

    /**
     * Compares this hand rank to another.
     *
     * Category is compared first. If categories are equal, tiebreaks are compared one by one
     * in order of importance (most to least). If all compared tiebreaks are equal but one hand
     * has more tiebreaks, the hand with more tiebreaks is considered greater.
     *
     * @param other the other hand rank to compare to
     * @return a negative integer if this hand rank is weaker, zero if equal, or a positive integer if stronger
     */
    override fun compareTo(other: HandRank): Int {
        val byCategory = category.compareTo(other.category)
        if (byCategory != 0) return byCategory
        for (i in 0 until minOf(tiebreaks.size, other.tiebreaks.size)) {
            val byRank = tiebreaks[i].compareTo(other.tiebreaks[i])
            if (byRank != 0) return byRank
        }
        return tiebreaks.size.compareTo(other.tiebreaks.size)
    }
}
