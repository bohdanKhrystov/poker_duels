package duels.poker.server.season

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

// Year padding to 4 digits is untested because all realistic seasons use 4-digit Gregorian years.
class SeasonTest {
    @Test
    fun theIdentifierIsTheMonthZeroPaddedToTwoDigits() {
        assertEquals("2026-08", Season(2026, 8).toString())
        assertEquals("2026-12", Season(2026, 12).toString())
    }

    @Test
    fun aDecemberAndAJanuaryEachCarryTheirOwnYear() {
        assertEquals("2025-12", Season(2025, 12).toString())
        assertEquals("2026-01", Season(2026, 1).toString())
    }

    @Test
    fun aMonthOutsideOneToTwelveIsNotASeason() {
        assertFailsWith<IllegalArgumentException> {
            Season(2026, 0)
        }
        assertFailsWith<IllegalArgumentException> {
            Season(2026, 13)
        }
    }
}
