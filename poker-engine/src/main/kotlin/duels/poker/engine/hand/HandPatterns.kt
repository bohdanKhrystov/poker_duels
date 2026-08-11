package duels.poker.engine.hand

import duels.poker.engine.card.Rank

private const val STRAIGHT_SIZE = 5

internal data class RankGroup(val rank: Rank, val size: Int)

/**
 * Classify five ranks as a straight or not, returning the straight's high [Rank].
 *
 * Five distinct ranks with consecutive ordinals form a straight, headed by the highest of them.
 * The one exception is the wheel, `ACE, TWO, THREE, FOUR, FIVE`, where the ace plays low and the
 * straight is headed by [Rank.FIVE].
 *
 * There is no wraparound straight: ranks do not cycle past [Rank.ACE] back to [Rank.TWO], so
 * `QUEEN, KING, ACE, TWO, THREE` is not a straight, and neither is `KING, ACE, TWO, THREE, FOUR`.
 *
 * Returns `null` for anything else, including a repeated rank.
 */
internal fun straightHighOrNull(ranks: List<Rank>): Rank? {
    val distinctOrdinals = ranks.map { it.ordinal }.distinct().sorted()
    if (distinctOrdinals.size < STRAIGHT_SIZE) return null

    val lowestOrdinal = distinctOrdinals.first()
    val highestOrdinal = distinctOrdinals.last()
    val isConsecutive = highestOrdinal - lowestOrdinal == STRAIGHT_SIZE - 1

    if (isConsecutive) return Rank.entries[highestOrdinal]

    val isWheel = distinctOrdinals == listOf(
        Rank.TWO.ordinal,
        Rank.THREE.ordinal,
        Rank.FOUR.ordinal,
        Rank.FIVE.ordinal,
        Rank.ACE.ordinal,
    )
    if (isWheel) return Rank.FIVE

    return null
}

/**
 * Group ranks by count, sorted by size descending and then by rank descending.
 *
 * This ordering is exactly the tiebreak order needed for three of a kind, two pair, and one pair:
 * [groups.map { it.rank }] yields the kicker order unchanged.
 */
internal fun rankGroups(ranks: List<Rank>): List<RankGroup> {
    return ranks
        .groupingBy { it }
        .eachCount()
        .map { (rank, count) -> RankGroup(rank, count) }
        .sortedWith(compareBy({ -it.size }, { -it.rank.ordinal }))
}
