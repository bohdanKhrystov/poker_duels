package duels.poker.engine.game

import duels.poker.engine.card.cards
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

internal class GameEventTest {
    @Test
    fun everyLifecycleEventCarriesTheSchemaVersion() {
        val handStarted = HandStarted(0, 7, 1, 50, 100, listOf(9_000, 11_000))
        val blindPosted = BlindPosted(1, seat = 0, to = 50, isBigBlind = false)
        val holeCardsDealt = HoleCardsDealt(2, 1, cards("As Kd"))
        val actionOn = ActionOn(3, 1)

        assertEquals(EVENT_SCHEMA_VERSION, handStarted.version)
        assertEquals(EVENT_SCHEMA_VERSION, blindPosted.version)
        assertEquals(EVENT_SCHEMA_VERSION, holeCardsDealt.version)
        assertEquals(EVENT_SCHEMA_VERSION, actionOn.version)
    }

    @Test
    fun versionDoesNotAffectEquality() {
        val a = ActionOn(3, 1)
        val b = ActionOn(3, 1)

        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun handStartedDescribesTheOpeningPosition() {
        val event = HandStarted(0, 7, 1, 50, 100, listOf(9_000, 11_000))

        assertEquals(0, event.sequence)
        assertEquals(7, event.handNumber)
        assertEquals(1, event.buttonSeat)
        assertEquals(50, event.smallBlind)
        assertEquals(100, event.bigBlind)
        assertEquals(listOf(9_000, 11_000), event.stacks)
    }

    @Test
    fun blindPostedNamesTheBlindAndTheTotal() {
        val smallBlind = BlindPosted(1, seat = 0, to = 50, isBigBlind = false)

        assertEquals(1, smallBlind.sequence)
        assertEquals(0, smallBlind.seat)
        assertEquals(50, smallBlind.to)
        assertEquals(false, smallBlind.isBigBlind)

        val bigBlind = BlindPosted(2, seat = 1, to = 100, isBigBlind = true)

        assertEquals(2, bigBlind.sequence)
        assertEquals(1, bigBlind.seat)
        assertEquals(100, bigBlind.to)
        assertEquals(true, bigBlind.isBigBlind)
    }

    @Test
    fun holeCardsDealtIsAddressedToOneSeat() {
        val dealtCards = cards("As Kd")
        val event = HoleCardsDealt(3, 1, dealtCards)

        assertEquals(1, event.seat)
        assertEquals(dealtCards, event.cards)
    }

    @Test
    fun rejectsAnInvalidEvent() {
        assertThrows(IllegalArgumentException::class.java) {
            HandStarted(-1, 7, 1, 50, 100, listOf(9_000, 11_000))
        }
        assertThrows(IllegalArgumentException::class.java) {
            HandStarted(0, 7, 1, 50, 100, listOf(9_000))
        }
        assertThrows(IllegalArgumentException::class.java) {
            HoleCardsDealt(3, 1, cards("As Kd").take(1))
        }
        assertThrows(IllegalArgumentException::class.java) {
            ActionOn(3, 2)
        }
    }
}
