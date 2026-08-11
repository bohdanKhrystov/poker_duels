package duels.poker.engine.hand

import duels.poker.engine.card.Rank
import duels.poker.engine.card.cards
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class RankGroupsTest {

    @Test
    fun quadsComeBeforeTheKicker() {
        val ranks = cards("9s 9d 9c 9h As").map { it.rank }
        val groups = rankGroups(ranks)
        assertEquals(
            listOf(
                RankGroup(Rank.NINE, 4),
                RankGroup(Rank.ACE, 1),
            ),
            groups,
        )
    }

    @Test
    fun fullHouseOrdersTripsBeforeThePair() {
        val ranks = cards("3s 3d 3c Kh Ks").map { it.rank }
        val groups = rankGroups(ranks)
        assertEquals(
            listOf(
                RankGroup(Rank.THREE, 3),
                RankGroup(Rank.KING, 2),
            ),
            groups,
        )
    }

    @Test
    fun twoPairOrdersHighPairThenLowPairThenKicker() {
        val ranks = cards("Ks Kd 7c 7h As").map { it.rank }
        val groups = rankGroups(ranks)
        assertEquals(
            listOf(
                RankGroup(Rank.KING, 2),
                RankGroup(Rank.SEVEN, 2),
                RankGroup(Rank.ACE, 1),
            ),
            groups,
        )
    }

    @Test
    fun onePairPutsThePairFirstAndKickersDescending() {
        val ranks = cards("5s 5d Ac Kh 2s").map { it.rank }
        val groups = rankGroups(ranks)
        assertEquals(
            listOf(
                RankGroup(Rank.FIVE, 2),
                RankGroup(Rank.ACE, 1),
                RankGroup(Rank.KING, 1),
                RankGroup(Rank.TWO, 1),
            ),
            groups,
        )
    }

    @Test
    fun unpairedRanksAreFiveSingletonsDescending() {
        val ranks = cards("As Qd 9c 5h 2s").map { it.rank }
        val groups = rankGroups(ranks)
        assertEquals(
            listOf(
                RankGroup(Rank.ACE, 1),
                RankGroup(Rank.QUEEN, 1),
                RankGroup(Rank.NINE, 1),
                RankGroup(Rank.FIVE, 1),
                RankGroup(Rank.TWO, 1),
            ),
            groups,
        )
    }

    @Test
    fun everyCardLandsInExactlyOneGroup() {
        val hands = listOf(
            cards("9s 9d 9c 9h As").map { it.rank },
            cards("3s 3d 3c Kh Ks").map { it.rank },
            cards("Ks Kd 7c 7h As").map { it.rank },
            cards("5s 5d Ac Kh 2s").map { it.rank },
            cards("As Qd 9c 5h 2s").map { it.rank },
        )

        for (ranks in hands) {
            val groups = rankGroups(ranks)
            // Group sizes sum to 5
            val totalSize = groups.sumOf { it.size }
            assertEquals(5, totalSize, "Group sizes should sum to 5 for $ranks")
            // All ranks are distinct
            val groupRanks = groups.map { it.rank }
            assertEquals(groupRanks.size, groupRanks.distinct().size, "All ranks should be distinct for $ranks")
        }
    }
}
