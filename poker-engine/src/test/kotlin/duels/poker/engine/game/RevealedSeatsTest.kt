package duels.poker.engine.game

import duels.poker.engine.card.Card
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class RevealedSeatsTest {

    private fun cards(notation: String): List<Card> = notation.split(" ").map(Card::parse)

    @Test
    fun namesNoSeatForAnEmptyLog() {
        val revealed = revealedSeats(emptyList())

        assertEquals(emptySet<Int>(), revealed)
    }

    @Test
    fun namesNoSeatWhenNothingWasShown() {
        val events = listOf(
            HandStarted(0, 1, 0, 1, 2, listOf(1000, 1000)),
            HoleCardsDealt(1, 0, cards("As Kh")),
            HoleCardsDealt(2, 1, cards("Qd Jc")),
            PlayerFolded(3, 1),
            HandFinished(4),
        )

        val revealed = revealedSeats(events)

        assertEquals(emptySet<Int>(), revealed)
    }

    @Test
    fun namesTheSeatThatShowed() {
        val events = listOf(
            HandRevealed(0, 1, cards("Qd Jc")),
        )

        val revealed = revealedSeats(events)

        assertEquals(setOf(1), revealed)
    }

    @Test
    fun namesBothSeatsAtATiedShowdown() {
        val events = listOf(
            HandRevealed(0, 0, cards("As Kh")),
            HandRevealed(1, 1, cards("Qd Jc")),
        )

        val revealed = revealedSeats(events)

        assertEquals(setOf(0, 1), revealed)
    }

    @Test
    fun aDealIsNotAReveal() {
        val events = listOf(
            HoleCardsDealt(0, 0, cards("As Kh")),
            HoleCardsDealt(1, 1, cards("Qd Jc")),
            StreetDealt(2, Street.FLOP, cards("2s 3h 4d")),
        )

        val revealed = revealedSeats(events)

        assertEquals(emptySet<Int>(), revealed)
    }
}
