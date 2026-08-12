package duels.poker.engine.duel

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class BlindScheduleTest {
    private val schedule =
        BlindSchedule(
            listOf(BlindLevel(50, 100), BlindLevel(75, 150), BlindLevel(100, 200)),
            handsPerLevel = 10,
        )

    @Test
    fun holdsEachLevelForItsBlockOfHands() {
        assertEquals(BlindLevel(50, 100), schedule.blindsFor(1))
        assertEquals(BlindLevel(50, 100), schedule.blindsFor(10))
        assertEquals(BlindLevel(75, 150), schedule.blindsFor(11))
        assertEquals(BlindLevel(75, 150), schedule.blindsFor(20))
        assertEquals(BlindLevel(100, 200), schedule.blindsFor(21))
        assertEquals(BlindLevel(100, 200), schedule.blindsFor(30))
    }

    @Test
    fun doublesTheLastLevelOncePerBlockThereafter() {
        assertEquals(BlindLevel(200, 400), schedule.blindsFor(31))
        assertEquals(BlindLevel(400, 800), schedule.blindsFor(41))
        assertEquals(BlindLevel(800, 1600), schedule.blindsFor(51))
    }

    @Test
    fun saturatesInsteadOfOverflowing() {
        val far = schedule.blindsFor(100_000)
        val farther = schedule.blindsFor(1_000_000)

        assertEquals(far, farther)
        assertTrue(far.smallBlind > 0)
        assertTrue(far.bigBlind > far.smallBlind)
    }

    @Test
    fun levelIndexCountsBlocksFromZero() {
        assertEquals(0, schedule.levelIndexFor(1))
        assertEquals(0, schedule.levelIndexFor(10))
        assertEquals(1, schedule.levelIndexFor(11))
        assertEquals(2, schedule.levelIndexFor(30))
        assertEquals(3, schedule.levelIndexFor(31))
    }

    @Test
    fun rejectsAScheduleThatIsNotAnAscendingLadder() {
        assertThrows(IllegalArgumentException::class.java) {
            BlindSchedule(emptyList(), handsPerLevel = 10)
        }
        assertThrows(IllegalArgumentException::class.java) {
            BlindSchedule(listOf(BlindLevel(50, 100)), handsPerLevel = 0)
        }
        assertThrows(IllegalArgumentException::class.java) {
            BlindSchedule(
                listOf(BlindLevel(50, 100), BlindLevel(75, 100)),
                handsPerLevel = 10,
            )
        }
    }

    @Test
    fun rejectsAHandNumberBelowOne() {
        assertThrows(IllegalArgumentException::class.java) { schedule.blindsFor(0) }
        assertThrows(IllegalArgumentException::class.java) { schedule.levelIndexFor(0) }
    }
}
