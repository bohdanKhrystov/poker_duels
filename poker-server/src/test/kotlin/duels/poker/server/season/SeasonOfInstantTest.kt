package duels.poker.server.season

import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.TimeZone
import kotlin.test.assertEquals

/**
 * `seasonOf` fixes a UTC boundary, per ADR-0061. These tests pin two things at once: the
 * half-open bound at the boundary, and the fact that the answer never moves with the JVM's
 * default time zone.
 */
internal class SeasonOfInstantTest {
    @Test
    fun anInstantExactlyOnABoundaryBelongsToTheNewSeason() {
        assertEquals(Season(2026, 9), seasonOf(Instant.parse("2026-09-01T00:00:00Z")))
        assertEquals(Season(2026, 8), seasonOf(Instant.parse("2026-08-31T23:59:59.999Z")))
    }

    @Test
    fun halfAnHourEitherSideOfABoundaryLandsInDifferentSeasons() {
        // The hazard ADR-0061 accepts on purpose: a reader at UTC+2 sees the first instant as
        // 1 September 01:30 and a reader at UTC-5 sees the second as 31 August 19:30, yet the
        // server disagrees with both — the boundary is UTC, not the reader's clock.
        assertEquals(Season(2026, 8), seasonOf(Instant.parse("2026-08-31T23:30:00Z")))
        assertEquals(Season(2026, 9), seasonOf(Instant.parse("2026-09-01T00:30:00Z")))
    }

    @Test
    fun twoInstantsInsideOneMonthLandInTheSameSeason() {
        assertEquals(Season(2026, 8), seasonOf(Instant.parse("2026-08-01T00:30:00Z")))
        assertEquals(Season(2026, 8), seasonOf(Instant.parse("2026-08-31T23:30:00Z")))
    }

    @Test
    fun theSeasonOfAnInstantDoesNotDependOnTheDefaultTimeZone() {
        val instant = Instant.parse("2026-09-01T00:30:00Z")
        val originalDefault = TimeZone.getDefault()
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("Pacific/Kiritimati")) // UTC+14
            assertEquals(Season(2026, 9), seasonOf(instant))

            TimeZone.setDefault(TimeZone.getTimeZone("Pacific/Niue")) // UTC-11
            assertEquals(Season(2026, 9), seasonOf(instant))
        } finally {
            TimeZone.setDefault(originalDefault)
        }
    }

    @Test
    fun aSeasonIsNamedByTheInstantItBeginsAt() {
        assertEquals(Season(2025, 12), seasonOf(Season(2025, 12).start))
        assertEquals(Season(2026, 1), seasonOf(Season(2026, 1).start))
    }
}
