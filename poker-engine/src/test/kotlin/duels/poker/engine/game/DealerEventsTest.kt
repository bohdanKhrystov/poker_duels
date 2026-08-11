package duels.poker.engine.game

import duels.poker.engine.card.cards
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class DealerEventsTest {
    @Test
    fun everyDealerEventIsAVersionedGameEvent() {
        val roundEnded = BettingRoundEnded(0, Street.FLOP)
        assertTrue(roundEnded is GameEvent)
        assertTrue(roundEnded is DealerEvent)
        assertEquals(EVENT_SCHEMA_VERSION, roundEnded.version)

        val streetDealt = StreetDealt(1, Street.FLOP, cards("As Kd 7c"))
        assertTrue(streetDealt is GameEvent)
        assertTrue(streetDealt is DealerEvent)
        assertEquals(EVENT_SCHEMA_VERSION, streetDealt.version)

        val showdownReached = ShowdownReached(2)
        assertTrue(showdownReached is GameEvent)
        assertTrue(showdownReached is DealerEvent)
        assertEquals(EVENT_SCHEMA_VERSION, showdownReached.version)

        val handRevealed = HandRevealed(3, 1, cards("As Kd"))
        assertTrue(handRevealed is GameEvent)
        assertTrue(handRevealed is DealerEvent)
        assertEquals(EVENT_SCHEMA_VERSION, handRevealed.version)
    }

    @Test
    fun bettingRoundEndedNamesItsStreet() {
        val event = BettingRoundEnded(4, Street.FLOP)

        assertEquals(Street.FLOP, event.street)
    }

    @Test
    fun bettingRoundEndedRejectsANonBettingStreet() {
        assertThrows(IllegalArgumentException::class.java) {
            BettingRoundEnded(0, Street.SHOWDOWN)
        }
        assertThrows(IllegalArgumentException::class.java) {
            BettingRoundEnded(0, Street.COMPLETE)
        }
    }

    @Test
    fun streetDealtCarriesThreeCardsForTheFlop() {
        val event = StreetDealt(5, Street.FLOP, cards("As Kd 7c"))

        assertEquals(Street.FLOP, event.street)
        assertEquals(cards("As Kd 7c"), event.cards)
    }

    @Test
    fun streetDealtCarriesOneCardForTheTurnAndRiver() {
        val turn = StreetDealt(6, Street.TURN, cards("2h"))
        assertEquals(Street.TURN, turn.street)
        assertEquals(cards("2h"), turn.cards)

        val river = StreetDealt(7, Street.RIVER, cards("2h"))
        assertEquals(Street.RIVER, river.street)
        assertEquals(cards("2h"), river.cards)
    }

    @Test
    fun streetDealtRejectsAWrongCardCount() {
        assertThrows(IllegalArgumentException::class.java) {
            StreetDealt(0, Street.FLOP, cards("2h"))
        }
        assertThrows(IllegalArgumentException::class.java) {
            StreetDealt(0, Street.TURN, cards("As Kd 7c"))
        }
    }

    @Test
    fun streetDealtRejectsAStreetThatDealsNothing() {
        assertThrows(IllegalArgumentException::class.java) {
            StreetDealt(0, Street.PREFLOP, emptyList())
        }
        assertThrows(IllegalArgumentException::class.java) {
            StreetDealt(0, Street.SHOWDOWN, emptyList())
        }
    }

    @Test
    fun handRevealedShowsTwoCardsForOneSeat() {
        val event = HandRevealed(9, 1, cards("As Kd"))

        assertEquals(1, event.seat)
        assertEquals(cards("As Kd"), event.cards)

        assertThrows(IllegalArgumentException::class.java) {
            HandRevealed(9, 1, cards("As Kd").take(1))
        }
        assertThrows(IllegalArgumentException::class.java) {
            HandRevealed(9, 1, listOf(cards("As").first(), cards("As").first()))
        }
    }
}
