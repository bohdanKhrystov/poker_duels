package duels.poker.engine.card

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class RankSuitTest {
    @Test
    fun ranksAscendFromTwoToAce() {
        assertEquals(13, Rank.entries.size)
        assertEquals((2..14).toList(), Rank.entries.map { it.value })
    }

    @Test
    fun rankSymbolsAreStandardPokerNotation() {
        assertEquals("23456789TJQKA", Rank.entries.map { it.symbol }.joinToString(""))
    }

    @Test
    fun suitsAreClubsDiamondsHeartsSpades() {
        assertEquals(4, Suit.entries.size)
        assertEquals("cdhs", Suit.entries.map { it.symbol }.joinToString(""))
    }
}
