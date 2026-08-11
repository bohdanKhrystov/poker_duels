package duels.poker.engine.game

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class SettlementEventsTest {
    @Test
    fun settlementEventsAreVersionedDealerEvents() {
        val uncalledBetReturned = UncalledBetReturned(10, 0, 500)
        assertTrue(uncalledBetReturned is GameEvent)
        assertTrue(uncalledBetReturned is DealerEvent)
        assertEquals(EVENT_SCHEMA_VERSION, uncalledBetReturned.version)

        val potAwarded = PotAwarded(11, 1, 1_000)
        assertTrue(potAwarded is GameEvent)
        assertTrue(potAwarded is DealerEvent)
        assertEquals(EVENT_SCHEMA_VERSION, potAwarded.version)

        val handFinished = HandFinished(12)
        assertTrue(handFinished is GameEvent)
        assertTrue(handFinished is DealerEvent)
        assertEquals(EVENT_SCHEMA_VERSION, handFinished.version)
    }

    @Test
    fun amountsAreChipsMoving() {
        val uncalledBetReturned = UncalledBetReturned(11, 0, 400)
        assertEquals(400, uncalledBetReturned.amount)

        val potAwarded = PotAwarded(12, 1, 1_200)
        assertEquals(1_200, potAwarded.amount)
    }

    @Test
    fun rejectsANonPositiveAmount() {
        assertThrows(IllegalArgumentException::class.java) {
            UncalledBetReturned(0, 0, 0)
        }
        assertThrows(IllegalArgumentException::class.java) {
            UncalledBetReturned(0, 0, -1)
        }
        assertThrows(IllegalArgumentException::class.java) {
            PotAwarded(0, 0, 0)
        }
        assertThrows(IllegalArgumentException::class.java) {
            PotAwarded(0, 0, -1)
        }
    }

    @Test
    fun rejectsAnInvalidSeatOrSequence() {
        assertThrows(IllegalArgumentException::class.java) {
            UncalledBetReturned(0, 2, 100)
        }
        assertThrows(IllegalArgumentException::class.java) {
            UncalledBetReturned(-1, 0, 100)
        }
        assertThrows(IllegalArgumentException::class.java) {
            PotAwarded(0, 2, 100)
        }
        assertThrows(IllegalArgumentException::class.java) {
            PotAwarded(-1, 0, 100)
        }
        assertThrows(IllegalArgumentException::class.java) {
            HandFinished(-1)
        }
    }

    @Test
    fun handFinishedCarriesOnlyItsSequence() {
        val handFinished = HandFinished(13)

        assertEquals(13, handFinished.sequence)
    }
}
