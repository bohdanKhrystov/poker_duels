package duels.poker.engine.game

import duels.poker.engine.card.cards
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PlayerViewRevealTest {

    private val state = handState()
        .withSeat(0) { it.copy(holeCards = cards("As Kh")) }
        .withSeat(1) { it.copy(holeCards = cards("Qd Jc")) }

    @Test
    fun revealsNothingByDefault() {
        val view = PlayerView.of(state, 0)

        assertTrue(view.opponent.holeCards.isEmpty())
    }

    @Test
    fun aRevealedOpponentHandAppearsInTheView() {
        val view = PlayerView.of(state, 0, setOf(1))

        assertEquals(cards("Qd Jc"), view.opponent.holeCards)
    }

    @Test
    fun aRevealedHandAppearsInBothSeatsViews() {
        val viewOfSeat0 = PlayerView.of(state, 0, setOf(1))
        val viewOfSeat1 = PlayerView.of(state, 1, setOf(1))

        assertEquals(cards("Qd Jc"), viewOfSeat0.opponent.holeCards)
        assertEquals(cards("Qd Jc"), viewOfSeat1.viewer.holeCards)
    }

    @Test
    fun revealingOnlyTheViewerLeavesTheOpponentHidden() {
        val view = PlayerView.of(state, 0, setOf(0))

        assertTrue(view.opponent.holeCards.isEmpty())
    }

    @Test
    fun revealingBothShowsBothToEitherSeat() {
        val viewOfSeat0 = PlayerView.of(state, 0, setOf(0, 1))
        val viewOfSeat1 = PlayerView.of(state, 1, setOf(0, 1))

        assertEquals(cards("As Kh"), viewOfSeat0.viewer.holeCards)
        assertEquals(cards("Qd Jc"), viewOfSeat0.opponent.holeCards)
        assertEquals(cards("As Kh"), viewOfSeat1.opponent.holeCards)
        assertEquals(cards("Qd Jc"), viewOfSeat1.viewer.holeCards)
    }

    @Test
    fun theViewerStillSeesItsOwnHandWhenNothingIsRevealed() {
        val view = PlayerView.of(state, 1)

        assertEquals(cards("Qd Jc"), view.viewer.holeCards)
    }

    @Test
    fun rejectsARevealedSeatOutsideZeroOrOne() {
        assertThrows(IllegalArgumentException::class.java) { PlayerView.of(state, 0, setOf(2)) }
    }
}
