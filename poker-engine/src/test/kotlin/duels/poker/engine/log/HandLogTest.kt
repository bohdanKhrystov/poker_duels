package duels.poker.engine.log

import duels.poker.engine.game.ActionOn
import duels.poker.engine.game.GameEvent
import duels.poker.engine.game.HandStarted
import duels.poker.engine.game.PlayerAction
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class HandLogTest {
    @Test
    fun carriesTheSeedAndTheOpeningParameters() {
        val seed = 12345L
        val handNumber = 5
        val buttonSeat = 0
        val stacks = listOf(1000, 2000)
        val smallBlind = 10
        val bigBlind = 20
        val events: List<GameEvent> = listOf(HandStarted(0, handNumber, buttonSeat, smallBlind, bigBlind, stacks))
        val actions: List<PlayerAction> = emptyList()

        val log = HandLog(seed, handNumber, buttonSeat, stacks, smallBlind, bigBlind, actions, events)

        Assertions.assertEquals(seed, log.seed)
        Assertions.assertEquals(handNumber, log.handNumber)
        Assertions.assertEquals(buttonSeat, log.buttonSeat)
        Assertions.assertEquals(stacks, log.stacks)
        Assertions.assertEquals(smallBlind, log.smallBlind)
        Assertions.assertEquals(bigBlind, log.bigBlind)
    }

    @Test
    fun defaultsToTheCurrentLogVersion() {
        val seed = 12345L
        val handNumber = 5
        val buttonSeat = 0
        val stacks = listOf(1000, 2000)
        val smallBlind = 10
        val bigBlind = 20
        val events: List<GameEvent> = listOf(HandStarted(0, handNumber, buttonSeat, smallBlind, bigBlind, stacks))
        val actions: List<PlayerAction> = emptyList()

        val log = HandLog(seed, handNumber, buttonSeat, stacks, smallBlind, bigBlind, actions, events)

        Assertions.assertEquals(HAND_LOG_VERSION, log.version)
        Assertions.assertEquals(1, HAND_LOG_VERSION)
    }

    @Test
    fun twoLogsWithTheSameContentAreEqual() {
        val seed = 12345L
        val handNumber = 5
        val buttonSeat = 0
        val stacks = listOf(1000, 2000)
        val smallBlind = 10
        val bigBlind = 20
        val events: List<GameEvent> = listOf(HandStarted(0, handNumber, buttonSeat, smallBlind, bigBlind, stacks))
        val actions: List<PlayerAction> = emptyList()

        val log1 = HandLog(seed, handNumber, buttonSeat, stacks, smallBlind, bigBlind, actions, events)
        val log2 = HandLog(seed, handNumber, buttonSeat, stacks, smallBlind, bigBlind, actions, events)

        Assertions.assertEquals(log1, log2)
    }

    @Test
    fun rejectsAnEventSequenceWithAGap() {
        val seed = 12345L
        val handNumber = 5
        val buttonSeat = 0
        val stacks = listOf(1000, 2000)
        val smallBlind = 10
        val bigBlind = 20
        val events: List<GameEvent> = listOf(
            HandStarted(0, handNumber, buttonSeat, smallBlind, bigBlind, stacks),
            ActionOn(2, 0), // Gap: should be 1
        )
        val actions: List<PlayerAction> = emptyList()

        assertThrows<IllegalArgumentException> {
            HandLog(seed, handNumber, buttonSeat, stacks, smallBlind, bigBlind, actions, events)
        }
    }

    @Test
    fun rejectsALogThatDoesNotStartWithHandStarted() {
        val seed = 12345L
        val handNumber = 5
        val buttonSeat = 0
        val stacks = listOf(1000, 2000)
        val smallBlind = 10
        val bigBlind = 20
        val events: List<GameEvent> = listOf(ActionOn(0, 0)) // Should start with HandStarted
        val actions: List<PlayerAction> = emptyList()

        assertThrows<IllegalArgumentException> {
            HandLog(seed, handNumber, buttonSeat, stacks, smallBlind, bigBlind, actions, events)
        }
    }

    @Test
    fun rejectsBlindsThatAreNotAscending() {
        val seed = 12345L
        val handNumber = 5
        val buttonSeat = 0
        val stacks = listOf(1000, 2000)
        val smallBlind = 20
        val bigBlind = 20 // Equal to smallBlind, should be strictly greater
        val events: List<GameEvent> = listOf(HandStarted(0, handNumber, buttonSeat, smallBlind, bigBlind, stacks))
        val actions: List<PlayerAction> = emptyList()

        assertThrows<IllegalArgumentException> {
            HandLog(seed, handNumber, buttonSeat, stacks, smallBlind, bigBlind, actions, events)
        }
    }

    @Test
    fun rejectsStacksThatAreNotTwoSeats() {
        val seed = 12345L
        val handNumber = 5
        val buttonSeat = 0
        val stacks = listOf(1000) // Only one entry, should have exactly 2
        val smallBlind = 10
        val bigBlind = 20
        val events: List<GameEvent> = listOf(HandStarted(0, handNumber, buttonSeat, smallBlind, bigBlind, listOf(1000, 2000)))
        val actions: List<PlayerAction> = emptyList()

        assertThrows<IllegalArgumentException> {
            HandLog(seed, handNumber, buttonSeat, stacks, smallBlind, bigBlind, actions, events)
        }
    }
}
