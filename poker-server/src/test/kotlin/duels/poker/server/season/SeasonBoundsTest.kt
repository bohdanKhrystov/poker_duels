package duels.poker.server.season

import org.junit.jupiter.api.Test
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SeasonBoundsTest {
    @Test
    fun aSeasonBeginsAtMidnightUtcOnTheFirstOfItsMonth() {
        assertEquals(
            Instant.parse("2026-08-01T00:00:00Z"),
            Season(2026, 8).start,
            "August 2026 should begin at 2026-08-01T00:00:00Z",
        )
        assertEquals(
            Instant.parse("2026-09-01T00:00:00Z"),
            Season(2026, 9).start,
            "September 2026 should begin at 2026-09-01T00:00:00Z",
        )
    }

    @Test
    fun aSeasonEndsExactlyWhereTheNextOneBegins() {
        val august = Season(2026, 8)
        val september = Season(2026, 9)
        assertEquals(
            september.start,
            august.endExclusive,
            "August's end should equal September's start",
        )
        assertEquals(
            Instant.parse("2026-09-01T00:00:00Z"),
            august.endExclusive,
            "August's end should be 2026-09-01T00:00:00Z",
        )
    }

    @Test
    fun decemberEndsAtTheFirstInstantOfTheNextJanuary() {
        val december = Season(2025, 12)
        assertEquals(
            Instant.parse("2026-01-01T00:00:00Z"),
            december.endExclusive,
            "December 2025 should end at 2026-01-01T00:00:00Z",
        )
    }

    @Test
    fun containsIsInclusiveAtItsStartAndExclusiveAtItsEnd() {
        val august = Season(2026, 8)
        assertTrue(
            august.contains(august.start),
            "Season should contain its start instant",
        )
        assertFalse(
            august.contains(august.endExclusive),
            "Season should not contain its end instant (exclusive)",
        )
        assertTrue(
            august.contains(august.endExclusive.minusMillis(1)),
            "Season should contain the instant one millisecond before its end",
        )
    }

    @Test
    fun aSeasonDoesNotContainTheMillisecondBeforeItBegins() {
        val august = Season(2026, 8)
        assertFalse(
            august.contains(Instant.parse("2026-07-31T23:59:59.999Z")),
            "Season should not contain the instant one millisecond before it starts",
        )
        assertTrue(
            august.contains(Instant.parse("2026-08-01T00:00:00.001Z")),
            "Season should contain the instant one millisecond after it starts",
        )
    }
}
