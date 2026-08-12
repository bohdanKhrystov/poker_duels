package duels.poker.ai

import duels.poker.engine.game.Street
import duels.poker.engine.game.startHand
import duels.poker.engine.random.SplitMix64Rng
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SimulationInvariantsTest {
    @Test
    fun aFreshlyDealtHandBreaksNoInvariant() {
        val state = startHand(1, 0, listOf(1000, 1000), 50, 100, SplitMix64Rng(1)).newState

        assertNull(firstViolation(state, state.chipsInPlay))
    }

    @Test
    fun reportsChipsThatDoNotAddUp() {
        val state = startHand(1, 0, listOf(1000, 1000), 50, 100, SplitMix64Rng(1)).newState
        val chipsAtOpen = state.chipsInPlay
        val broken = state.copy(pot = state.pot + 1)

        val violation = firstViolation(broken, chipsAtOpen)

        assertNotNull(violation)
        assertTrue(violation!!.contains("chips"), "Expected message to contain 'chips', was: $violation")
    }

    @Test
    fun reportsADuplicateCardInPlay() {
        val state = startHand(1, 0, listOf(1000, 1000), 50, 100, SplitMix64Rng(1)).newState
        val duplicatedCard = state.seat(0).holeCards.first()
        val broken = state.withSeat(1) { it.copy(holeCards = state.seat(0).holeCards) }

        val violation = firstViolation(broken, broken.chipsInPlay)

        assertNotNull(violation)
        assertTrue(
            violation!!.contains(duplicatedCard.toString()),
            "Expected message to name the duplicated card $duplicatedCard, was: $violation",
        )
    }

    @Test
    fun reportsASeatToActThatHasFolded() {
        val state = startHand(1, 0, listOf(1000, 1000), 50, 100, SplitMix64Rng(1)).newState
        val seatToAct = requireNotNull(state.seatToAct)
        val broken = state.withSeat(seatToAct) { it.copy(hasFolded = true) }

        val violation = firstViolation(broken, broken.chipsInPlay)

        assertNotNull(violation)
    }

    @Test
    fun reportsASeatToActThatIsAllIn() {
        val state = startHand(1, 0, listOf(1000, 1000), 50, 100, SplitMix64Rng(1)).newState
        val seatToAct = requireNotNull(state.seatToAct)
        val broken = state.withSeat(seatToAct) { it.copy(isAllIn = true) }

        val violation = firstViolation(broken, broken.chipsInPlay)

        assertNotNull(violation)
    }

    @Test
    fun reportsNoSeatToActInAHandThatIsNotOver() {
        val state = startHand(1, 0, listOf(1000, 1000), 50, 100, SplitMix64Rng(1)).newState
        val broken = state.copy(seatToAct = null)

        val violation = firstViolation(broken, broken.chipsInPlay)

        assertNotNull(violation)
    }

    @Test
    fun acceptsNoSeatToActOnceTheHandIsOver() {
        val state = startHand(1, 0, listOf(1000, 1000), 50, 100, SplitMix64Rng(1)).newState
        val over = state.copy(street = Street.COMPLETE, seatToAct = null)

        val violation = firstViolation(over, over.chipsInPlay)

        assertNull(violation)
    }

    @Test
    fun checksChipsBeforeAnythingElse() {
        val state = startHand(1, 0, listOf(1000, 1000), 50, 100, SplitMix64Rng(1)).newState
        val chipsAtOpen = state.chipsInPlay
        val broken = state.copy(pot = state.pot + 1, seatToAct = null)

        val violation = firstViolation(broken, chipsAtOpen)

        assertNotNull(violation)
        assertTrue(violation!!.contains("chips"), "Expected the chip message to win, was: $violation")
    }
}
