package duels.poker.server.http

import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.Base64
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Reused across fixtures that don't specifically exercise UUID parsing or its casing. */
private const val SAMPLE_DUEL_ID = "0f8fad5b-d9cb-469f-a165-70867728950e"

/**
 * Encodes [payload] the way the server does, independent of [DuelCursor.encoded] — several
 * fixtures below are deliberately malformed and could never come from a real cursor.
 */
private fun base64Of(payload: String): String =
    Base64.getUrlEncoder().withoutPadding().encodeToString(payload.toByteArray(Charsets.UTF_8))

class DuelCursorTest {
    @Test
    fun aCursorEncodesAndDecodesBackToItself() {
        // Microsecond precision, because that is what PostgreSQL stores.
        val cursor = DuelCursor(Instant.parse("2026-08-13T10:02:03.000004Z"), UUID.fromString(SAMPLE_DUEL_ID))
        assertEquals(cursor, duelCursorOrNull(cursor.encoded()))
    }

    @Test
    fun theEncodedFormCarriesNoPadding() {
        val cursor = DuelCursor(Instant.parse("2026-08-13T10:02:03.000004Z"), UUID.fromString(SAMPLE_DUEL_ID))
        assertFalse(cursor.encoded().contains("="))
    }

    @Test
    fun theEncodedFormShowsNeitherHalfInTheClear() {
        val cursor = DuelCursor(Instant.parse("2026-08-13T10:02:03.000004Z"), UUID.fromString(SAMPLE_DUEL_ID))
        val encoded = cursor.encoded()
        assertFalse(encoded.contains("2026-"))
        assertFalse(encoded.contains("0f8fad5b"))
    }

    @Test
    fun aStringThatIsNotBase64IsRefused() {
        assertNull(duelCursorOrNull("not a cursor!!"))
        assertNull(duelCursorOrNull(""))
    }

    @Test
    fun aPayloadWithoutExactlyTwoPartsIsRefused() {
        assertNull(duelCursorOrNull(base64Of("2026-08-13T10:00:00Z")))
        assertNull(duelCursorOrNull(base64Of("2026-08-13T10:00:00Z|x|y")))
    }

    @Test
    fun aPayloadWhoseHalvesDoNotParseIsRefused() {
        assertNull(duelCursorOrNull(base64Of("yesterday|$SAMPLE_DUEL_ID")))
        assertNull(duelCursorOrNull(base64Of("2026-08-13T10:00:00Z|not-a-uuid")))
    }

    @Test
    fun aNonCanonicalPayloadIsRefused() {
        // Same instant as "2026-08-13T10:00:00Z", but not the text encoded() would produce for it.
        assertNull(duelCursorOrNull(base64Of("2026-08-13T10:00:00.000Z|$SAMPLE_DUEL_ID")))
        // Same id as SAMPLE_DUEL_ID, but UUID#toString() always renders lower-case.
        assertNull(duelCursorOrNull(base64Of("2026-08-13T10:00:00Z|0F8FAD5B-D9CB-469F-A165-70867728950E")))
    }

    @Test
    fun aPayloadWithPaddingIsRefused() {
        // 64 bytes, not a multiple of three, so the padded encoder emits two "=" here — a
        // whole-second payload would pad to nothing and this fixture would prove nothing.
        val payload = "2026-08-13T10:02:03.000004Z|$SAMPLE_DUEL_ID"
        val padded = Base64.getUrlEncoder().encodeToString(payload.toByteArray(Charsets.UTF_8))
        assertTrue(padded.endsWith("=="))
        assertNull(duelCursorOrNull(padded))
    }

    @Test
    fun twoDistinctCursorsEncodeToDifferentStrings() {
        val first = DuelCursor(Instant.parse("2026-08-13T10:02:03.000004Z"), UUID.fromString(SAMPLE_DUEL_ID))
        val second =
            DuelCursor(
                Instant.parse("2025-01-01T00:00:00.000001Z"),
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
            )
        assertNotEquals(first.encoded(), second.encoded())
    }
}
