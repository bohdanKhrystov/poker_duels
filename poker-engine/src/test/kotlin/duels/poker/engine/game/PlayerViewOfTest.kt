package duels.poker.engine.game

import duels.poker.engine.card.cards
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PlayerViewOfTest {

    @Test
    fun theViewerSeesItsOwnHoleCards() {
        val state = handState().withSeat(0) { it.copy(holeCards = cards("As Kh")) }

        val view = PlayerView.of(state, 0)

        assertEquals(cards("As Kh"), view.viewer.holeCards)
    }

    @Test
    fun theOpponentsHoleCardsAreAbsent() {
        val state = handState()
            .withSeat(0) { it.copy(holeCards = cards("As Kh")) }
            .withSeat(1) { it.copy(holeCards = cards("Qd Jc")) }

        val view = PlayerView.of(state, 0)

        assertTrue(view.opponent.holeCards.isEmpty())
    }

    @Test
    fun eachSeatSeesOnlyItsOwn() {
        val state = handState()
            .withSeat(0) { it.copy(holeCards = cards("As Kh")) }
            .withSeat(1) { it.copy(holeCards = cards("Qd Jc")) }

        val view = PlayerView.of(state, 1)

        assertEquals(cards("Qd Jc"), view.viewer.holeCards)
        assertTrue(view.opponent.holeCards.isEmpty())
    }

    @Test
    fun carriesTheBoardPotAndBlinds() {
        val state = handState().copy(
            street = Street.FLOP,
            board = Board.EMPTY.dealt(cards("2c 7d 9h")),
            pot = 300,
        )

        val view = PlayerView.of(state, 0)

        assertEquals(state.board, view.board)
        assertEquals(state.pot, view.pot)
        assertEquals(state.smallBlind, view.smallBlind)
        assertEquals(state.bigBlind, view.bigBlind)
    }

    @Test
    fun carriesStacksCommitmentsAndStatus() {
        val state = handState()
            .withSeat(0) { it.copy(stack = 9_500, committedThisStreet = 100, committedThisHand = 100) }
            .withSeat(1) { it.copy(stack = 9_950, committedThisStreet = 50, committedThisHand = 50, hasFolded = true) }

        val view = PlayerView.of(state, 0)

        val expectedSeat0 = state.seat(0)
        val expectedSeat1 = state.seat(1)

        assertEquals(expectedSeat0.stack, view.seats[0].stack)
        assertEquals(expectedSeat0.committedThisStreet, view.seats[0].committedThisStreet)
        assertEquals(expectedSeat0.committedThisHand, view.seats[0].committedThisHand)
        assertEquals(expectedSeat0.hasFolded, view.seats[0].hasFolded)
        assertEquals(expectedSeat0.isAllIn, view.seats[0].isAllIn)

        assertEquals(expectedSeat1.stack, view.seats[1].stack)
        assertEquals(expectedSeat1.committedThisStreet, view.seats[1].committedThisStreet)
        assertEquals(expectedSeat1.committedThisHand, view.seats[1].committedThisHand)
        assertEquals(expectedSeat1.hasFolded, view.seats[1].hasFolded)
        assertEquals(expectedSeat1.isAllIn, view.seats[1].isAllIn)
    }

    @Test
    fun carriesTheButtonStreetHandNumberAndSeatToAct() {
        val state = handState().copy(handNumber = 3, buttonSeat = 1, street = Street.PREFLOP, seatToAct = 1)

        val view = PlayerView.of(state, 0)

        assertEquals(state.buttonSeat, view.buttonSeat)
        assertEquals(state.street, view.street)
        assertEquals(state.handNumber, view.handNumber)
        assertEquals(state.seatToAct, view.seatToAct)
        assertEquals(0, view.viewerSeat)
    }

    @Test
    fun showsNoHoleCardsBeforeTheDeal() {
        val state = handState()

        val view = PlayerView.of(state, 0)

        assertTrue(view.seats[0].holeCards.isEmpty())
        assertTrue(view.seats[1].holeCards.isEmpty())
    }

    @Test
    fun rejectsASeatOutsideZeroOrOne() {
        val state = handState()

        assertThrows(IllegalArgumentException::class.java) { PlayerView.of(state, 2) }
    }
}
