package duels.poker.server.http

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RecentDuelsLimitTest {
    @Test
    fun anAbsentLimitIsTheDefault() {
        assertEquals(DEFAULT_DUEL_LIMIT, duelLimitOrNull(null))
    }

    @Test
    fun aLimitWithinTheCapIsHonoured() {
        assertEquals(25, duelLimitOrNull("25"))
        assertEquals(1, duelLimitOrNull("1"))
    }

    @Test
    fun aLimitAboveTheCapIsClamped() {
        assertEquals(MAX_DUEL_LIMIT, duelLimitOrNull("51"))
        assertEquals(MAX_DUEL_LIMIT, duelLimitOrNull("999"))
        assertEquals(MAX_DUEL_LIMIT, duelLimitOrNull("50"))
    }

    @Test
    fun aLimitLargerThanAnIntIsClampedNotRejected() {
        assertEquals(MAX_DUEL_LIMIT, duelLimitOrNull("99999999999"))
    }

    @Test
    fun aNonNumericLimitIsRejected() {
        assertNull(duelLimitOrNull("abc"))
        assertNull(duelLimitOrNull(""))
        assertNull(duelLimitOrNull(" "))
    }

    @Test
    fun aZeroOrNegativeLimitIsRejected() {
        assertNull(duelLimitOrNull("0"))
        assertNull(duelLimitOrNull("-1"))
    }
}
