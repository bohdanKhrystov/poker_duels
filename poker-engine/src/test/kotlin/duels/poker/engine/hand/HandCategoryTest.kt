package duels.poker.engine.hand

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class HandCategoryTest {
    @Test
    fun thereAreExactlyNineCategories() {
        assertEquals(9, HandCategory.entries.size)
    }

    @Test
    fun categoriesAreDeclaredWeakestFirst() {
        val expected = listOf(
            HandCategory.HIGH_CARD,
            HandCategory.PAIR,
            HandCategory.TWO_PAIR,
            HandCategory.THREE_OF_A_KIND,
            HandCategory.STRAIGHT,
            HandCategory.FLUSH,
            HandCategory.FULL_HOUSE,
            HandCategory.FOUR_OF_A_KIND,
            HandCategory.STRAIGHT_FLUSH,
        )
        assertEquals(expected, HandCategory.entries)
    }

    @Test
    fun strongerCategoriesCompareGreater() {
        val entries = HandCategory.entries
        // Check adjacent pairs
        for (i in 0 until entries.size - 1) {
            assertTrue(entries[i] < entries[i + 1], "${entries[i]} should be < ${entries[i + 1]}")
        }

        // Check specific comparisons in both directions
        assertTrue(HandCategory.STRAIGHT_FLUSH > HandCategory.FOUR_OF_A_KIND)
        assertTrue(HandCategory.FOUR_OF_A_KIND > HandCategory.FULL_HOUSE)
        assertTrue(HandCategory.FULL_HOUSE > HandCategory.FLUSH)
        assertTrue(HandCategory.FLUSH > HandCategory.STRAIGHT)
        assertTrue(HandCategory.STRAIGHT > HandCategory.THREE_OF_A_KIND)
        assertTrue(HandCategory.THREE_OF_A_KIND > HandCategory.TWO_PAIR)
        assertTrue(HandCategory.TWO_PAIR > HandCategory.PAIR)
        assertTrue(HandCategory.PAIR > HandCategory.HIGH_CARD)
    }
}
