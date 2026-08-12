package duels.poker.engine.game

import duels.poker.engine.card.Card
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

/**
 * [visibleTo] over the full [GameEvent] hierarchy: seat 0 holds `As Kh`, seat 1 holds `Qd Jc`.
 * The fixture list mirrors `GameEventSerializationTest.everySubtypeRoundTripsThroughTheParentSerializer`
 * with one addition — a second [HoleCardsDealt], so both seats' deals are present and the list
 * form has something to drop for either recipient.
 */
class EventRedactionTest {

    private fun cards(notation: String): List<Card> = notation.split(" ").map(Card::parse)

    private val fixture: List<GameEvent> = listOf(
        HandStarted(0, 1, 0, 1, 2, listOf(1000, 1000)),
        BlindPosted(1, 0, 1, false),
        HoleCardsDealt(2, 0, cards("As Kh")),
        HoleCardsDealt(3, 1, cards("Qd Jc")),
        ActionOn(4, 0),
        PlayerFolded(5, 1),
        PlayerChecked(6, 0),
        PlayerCalled(7, 1, 4),
        PlayerBet(8, 0, 10),
        PlayerRaised(9, 1, 30),
        PlayerAllIn(10, 0, 100),
        BettingRoundEnded(11, Street.PREFLOP),
        StreetDealt(12, Street.FLOP, cards("2s 3h 4d")),
        ShowdownReached(13),
        HandRevealed(14, 1, cards("Qd Jc")),
        UncalledBetReturned(15, 1, 50),
        PotAwarded(16, 0, 150),
        HandFinished(17),
    )

    @Test
    fun aSeatReceivesItsOwnDeal() {
        val event = HoleCardsDealt(2, 0, cards("As Kh"))

        assertSame(event, visibleTo(event, 0))
    }

    @Test
    fun aSeatNeverReceivesTheOtherSeatsDeal() {
        val event = HoleCardsDealt(2, 1, cards("Qd Jc"))

        assertNull(visibleTo(event, 0))
    }

    @Test
    fun everyOtherEventTypeIsDeliveredUnchanged() {
        val nonDeals = fixture.filterNot { it is HoleCardsDealt }

        for (event in nonDeals) {
            for (seat in 0..1) {
                assertSame(event, visibleTo(event, seat), "event $event, seat $seat")
            }
        }
    }

    @Test
    fun theListFormDropsOnlyTheOtherSeatsDeal() {
        val filtered = visibleTo(fixture, 0)

        assertEquals(fixture.size - 1, filtered.size)
        assertEquals(
            fixture - filtered.toSet(),
            listOf(HoleCardsDealt(3, 1, cards("Qd Jc"))),
        )
    }

    @Test
    fun aRevealedHandReachesBothSeats() {
        val event = HandRevealed(13, 1, cards("Qd Jc"))

        assertSame(event, visibleTo(event, 0))
        assertSame(event, visibleTo(event, 1))
    }

    @Test
    fun theFilteredStreamKeepsItsOrder() {
        val filtered = visibleTo(fixture, 1)
        val expected = fixture.filterNot { it is HoleCardsDealt && it.seat == 0 }

        assertEquals(expected, filtered)
    }

    @Test
    fun rejectsASeatOutsideZeroOrOne() {
        assertThrows(IllegalArgumentException::class.java) {
            visibleTo(HandFinished(16), 2)
        }
    }
}
