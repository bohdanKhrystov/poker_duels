package duels.poker.server.season

import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import kotlin.test.assertEquals

/**
 * Every fixed instant below is outside the calendar month this suite happens to run in, so an
 * implementation that ignores [clock] and reads the system clock instead is red on every day of
 * every month rather than green by luck for four weeks.
 */
class CurrentSeasonTest {
    @Test
    fun theCurrentSeasonIsTheOneContainingTheClocksInstant() {
        val clockInFebruary2019 = Clock.fixed(Instant.parse("2019-02-15T12:00:00Z"), ZoneOffset.UTC)
        assertEquals(Season(2019, 2), currentSeason(clockInFebruary2019))

        val clockAtEndOf2025 = Clock.fixed(Instant.parse("2025-12-31T23:59:59.999Z"), ZoneOffset.UTC)
        assertEquals(Season(2025, 12), currentSeason(clockAtEndOf2025))
    }

    @Test
    fun movingTheClockAcrossABoundaryChangesTheCurrentSeason() {
        val clock = MovableClock(Instant.parse("2025-12-31T23:59:59.999Z"))
        assertEquals(Season(2025, 12), currentSeason(clock))

        clock.now = Instant.parse("2026-01-01T00:00:00Z")
        assertEquals(Season(2026, 1), currentSeason(clock))
    }
}

/**
 * A wall clock a test can move. `withZone` is never called by [currentSeason]; the season's zone
 * is `seasonOf`'s, not this clock's.
 */
private class MovableClock(var now: Instant) : Clock() {
    override fun instant(): Instant = now

    override fun getZone(): ZoneId = ZoneOffset.UTC

    override fun withZone(zone: ZoneId): Clock = this
}
