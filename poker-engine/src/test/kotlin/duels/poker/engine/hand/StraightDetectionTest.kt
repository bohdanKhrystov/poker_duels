package duels.poker.engine.hand

import duels.poker.engine.card.Rank
import duels.poker.engine.card.cards
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

private fun <T> permutations(items: List<T>): List<List<T>> = if (items.size <= 1) {
    listOf(items)
} else {
    items.flatMap { head -> permutations(items - head).map { listOf(head) + it } }
}

internal class StraightDetectionTest {

    @Test
    fun everyStraightFromSixHighToAceHighIsFound() {
        val consecutiveRanks = Rank.entries
        for (windowStart in 0..(consecutiveRanks.size - 5)) {
            val window = consecutiveRanks.subList(windowStart, windowStart + 5)
            assertEquals(window.last(), straightHighOrNull(window), "window $window")
        }
    }

    @Test
    fun theWheelIsFiveHigh() {
        val wheel = cards("As 5d 4c 3h 2s").map { it.rank }
        assertEquals(Rank.FIVE, straightHighOrNull(wheel))
    }

    @Test
    fun queenKingAceTwoThreeIsNotAStraight() {
        val ranks = cards("Qs Kd Ac 2h 3s").map { it.rank }
        assertNull(straightHighOrNull(ranks))
    }

    @Test
    fun kingAceTwoThreeFourIsNotAStraight() {
        val ranks = cards("Ks Ad 2c 3h 4s").map { it.rank }
        assertNull(straightHighOrNull(ranks))
    }

    @Test
    fun aGapBreaksTheStraight() {
        val ranks = cards("9s 8d 7c 6h 4s").map { it.rank }
        assertNull(straightHighOrNull(ranks))
    }

    @Test
    fun aPairIsNeverAStraight() {
        val ranks = cards("6s 5d 5c 4h 3s").map { it.rank }
        assertNull(straightHighOrNull(ranks))
    }

    @Test
    fun inputOrderDoesNotMatter() {
        val wheel = cards("As 5d 4c 3h 2s").map { it.rank }
        for (permutation in permutations(wheel)) {
            assertEquals(Rank.FIVE, straightHighOrNull(permutation), "permutation $permutation")
        }

        val broadway = cards("Ts Jd Qc Kh As").map { it.rank }
        for (permutation in permutations(broadway)) {
            assertEquals(Rank.ACE, straightHighOrNull(permutation), "permutation $permutation")
        }
    }
}
