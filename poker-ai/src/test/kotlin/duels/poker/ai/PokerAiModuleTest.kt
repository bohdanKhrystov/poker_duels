package duels.poker.ai

import duels.poker.engine.card.Card
import duels.poker.engine.game.startHand
import duels.poker.engine.random.SplitMix64Rng
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PokerAiModuleTest {
    @Test
    fun theEngineIsOnTheModulesClasspath() {
        assertEquals(52, Card.all.size)
    }

    @Test
    fun theModuleCanOpenAHand() {
        val newState = startHand(1, 0, listOf(10_000, 10_000), 50, 100, SplitMix64Rng(1L)).newState
        assertEquals(0, newState.seatToAct)
    }
}
