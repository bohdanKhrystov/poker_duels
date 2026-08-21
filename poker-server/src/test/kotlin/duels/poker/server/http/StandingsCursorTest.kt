package duels.poker.server.http

import duels.poker.server.season.Season
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNull

class StandingsCursorTest {
    private val season = Season(2026, 8)

    @Test
    fun encodesToTheExactStringTheServerHandsOut() {
        val cursor = StandingsCursor(
            Instant.parse("2026-08-15T12:00:00Z"),
            3,
            UUID.fromString("00000000-0000-4000-8000-000000000001"),
        )
        assertEquals("MjAyNi0wOC0xNVQxMjowMDowMFp8M3wwMDAwMDAwMC0wMDAwLTQwMDAtODAwMC0wMDAwMDAwMDAwMDE", cursor.encoded())
    }

    @Test
    fun survivesItsOwnRoundTripIncludingANegativeStanding() {
        val original = StandingsCursor(
            Instant.parse("2026-08-15T12:00:00Z"),
            -2,
            UUID.fromString("12345678-1234-4000-8000-000000000001"),
        )
        val encoded = original.encoded()
        val decoded = standingsCursorOrNull(encoded, season)
        assertEquals(original, decoded)
    }

    @Test
    fun refusesACursorFromBeforeTheSeasonBegan() {
        val beforeSeasonStart = season.start.minusMillis(1)
        val cursor = StandingsCursor(beforeSeasonStart, 100, UUID.fromString("12345678-1234-4000-8000-000000000001"))
        val encoded = cursor.encoded()
        assertNull(standingsCursorOrNull(encoded, season))
    }

    @Test
    fun refusesACursorStampedAtTheSeasonsEnd() {
        val atSeasonEnd = season.endExclusive
        val cursorAtEnd = StandingsCursor(atSeasonEnd, 100, UUID.fromString("12345678-1234-4000-8000-000000000001"))
        val encodedAtEnd = cursorAtEnd.encoded()
        assertNull(standingsCursorOrNull(encodedAtEnd, season))

        val atSeasonStart = season.start
        val cursorAtStart = StandingsCursor(atSeasonStart, 100, UUID.fromString("12345678-1234-4000-8000-000000000001"))
        val encodedAtStart = cursorAtStart.encoded()
        assertEquals(cursorAtStart, standingsCursorOrNull(encodedAtStart, season))
    }

    @Test
    fun refusesAPayloadThatIsNotItsOwnCanonicalEncoding() {
        val cursor = StandingsCursor(
            Instant.parse("2026-08-15T12:00:00Z"),
            3,
            UUID.fromString("00000000-0000-4000-8000-000000000001"),
        )
        val canonical = cursor.encoded()

        // Padded version (canonical ends with no padding, so we add =)
        val padded = canonical + "="
        assertNull(standingsCursorOrNull(padded, season))

        // Uppercase UUID
        val upperCased = "MjAyNi0wOC0xNVQxMjowMDowMFp8M3wwMDAwMDAwMC0wMDAwLTQwMDAtODAwMC0wMDAwMDAwMDAwMDE".replace(
            "mdAwMDA",
            "MDAWMDA",
        )
        assertNull(standingsCursorOrNull(upperCased, season))

        // Different instant format (equivalent but not canonical)
        val instantPayload = "2026-08-15T12:00:00+00:00|3|00000000-0000-4000-8000-000000000001"
        val differentFormat = java.util.Base64.getUrlEncoder().withoutPadding()
            .encodeToString(instantPayload.toByteArray(Charsets.UTF_8))
        assertNull(standingsCursorOrNull(differentFormat, season))
    }

    @Test
    fun refusesWhatDoesNotDecode() {
        val notBase64 = "!!!not_base64!!!"
        assertNull(standingsCursorOrNull(notBase64, season))

        val empty = ""
        assertNull(standingsCursorOrNull(empty, season))

        val twoParts = java.util.Base64.getUrlEncoder().withoutPadding()
            .encodeToString("2026-08-15T12:00:00Z|3".toByteArray(Charsets.UTF_8))
        assertNull(standingsCursorOrNull(twoParts, season))

        val fourParts = java.util.Base64.getUrlEncoder().withoutPadding()
            .encodeToString("2026-08-15T12:00:00Z|3|00000000-0000-4000-8000-000000000001|extra".toByteArray(Charsets.UTF_8))
        assertNull(standingsCursorOrNull(fourParts, season))
    }
}
