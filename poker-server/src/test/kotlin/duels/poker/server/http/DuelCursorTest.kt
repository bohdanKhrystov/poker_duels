package duels.poker.server.http

import duels.poker.server.protocol.http.DuelOutcomeLabel
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
 * The fingerprint of [DuelFilter.NONE], written as a literal rather than computed from
 * `DuelFilter.NONE.fingerprint()` — a fixture computed from the code under test cannot catch that
 * code changing.
 */
private const val NONE_FINGERPRINT = "47DEQpj8HBQ"

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
        assertEquals(cursor, duelCursorOrNull(cursor.encoded(DuelFilter.NONE), DuelFilter.NONE))
    }

    @Test
    fun aCursorRoundTripsUnderTheFilterThatEncodedIt() {
        val cursor = DuelCursor(Instant.parse("2026-08-13T10:02:03.000004Z"), UUID.fromString(SAMPLE_DUEL_ID))
        // Two filters, not one: an implementation that only appends a fingerprint when an axis is
        // set would pass the first assertion below and fail the second.
        val filtered = DuelFilter(DuelOutcomeLabel.WON, "Halvard")
        assertEquals(cursor, duelCursorOrNull(cursor.encoded(filtered), filtered))
        assertEquals(cursor, duelCursorOrNull(cursor.encoded(DuelFilter.NONE), DuelFilter.NONE))
    }

    @Test
    fun aCursorIsRefusedUnderAnyOtherFilter() {
        val cursor = DuelCursor(Instant.parse("2026-08-13T10:02:03.000004Z"), UUID.fromString(SAMPLE_DUEL_ID))
        val filterA = DuelFilter(DuelOutcomeLabel.WON, null)
        assertNull(duelCursorOrNull(cursor.encoded(filterA), DuelFilter(DuelOutcomeLabel.LOST, null)))
        assertNull(duelCursorOrNull(cursor.encoded(filterA), DuelFilter(DuelOutcomeLabel.WON, "Halvard")))
        assertNull(duelCursorOrNull(cursor.encoded(filterA), DuelFilter.NONE))
        // The other direction too: a cursor minted unfiltered must not validate under a filter
        // either — "unfiltered accepts anything" and "filtered accepts anything" are two bugs.
        assertNull(duelCursorOrNull(cursor.encoded(DuelFilter.NONE), filterA))
    }

    @Test
    fun theEncodedFormIsItsGoldenVector() {
        val cursor = DuelCursor(Instant.parse("2026-08-13T10:02:03.000004Z"), UUID.fromString(SAMPLE_DUEL_ID))
        val filter = DuelFilter(DuelOutcomeLabel.WON, "Halvard")
        assertEquals(
            "MjAyNi0wOC0xM1QxMDowMjowMy4wMDAwMDRafDBmOGZhZDViLWQ5Y2ItNDY5Zi1hMTY1LTcwODY3NzI4OTUwZXxKOHFEeDEzQ0lfbw",
            cursor.encoded(filter),
        )
    }

    @Test
    fun theEncodedFormCarriesNoPadding() {
        val cursor = DuelCursor(Instant.parse("2026-08-13T10:02:03.000004Z"), UUID.fromString(SAMPLE_DUEL_ID))
        assertFalse(cursor.encoded(DuelFilter.NONE).contains("="))
    }

    @Test
    fun theEncodedFormShowsNeitherHalfInTheClear() {
        val cursor = DuelCursor(Instant.parse("2026-08-13T10:02:03.000004Z"), UUID.fromString(SAMPLE_DUEL_ID))
        val encoded = cursor.encoded(DuelFilter.NONE)
        assertFalse(encoded.contains("2026-"))
        assertFalse(encoded.contains("0f8fad5b"))
    }

    @Test
    fun aStringThatIsNotBase64IsRefused() {
        assertNull(duelCursorOrNull("not a cursor!!", DuelFilter.NONE))
        assertNull(duelCursorOrNull("", DuelFilter.NONE))
    }

    @Test
    fun aPayloadWithoutExactlyThreePartsIsRefused() {
        // A well-formed instant and id, but no fingerprint at all: two parts.
        assertNull(duelCursorOrNull(base64Of("2026-08-13T10:00:00Z|$SAMPLE_DUEL_ID"), DuelFilter.NONE))
        // A well-formed fingerprint present, but a fourth part trailing it.
        assertNull(
            duelCursorOrNull(base64Of("2026-08-13T10:00:00Z|$SAMPLE_DUEL_ID|$NONE_FINGERPRINT|extra"), DuelFilter.NONE),
        )
    }

    @Test
    fun aPayloadWhoseHalvesDoNotParseIsRefused() {
        assertNull(duelCursorOrNull(base64Of("yesterday|$SAMPLE_DUEL_ID|$NONE_FINGERPRINT"), DuelFilter.NONE))
        assertNull(duelCursorOrNull(base64Of("2026-08-13T10:00:00Z|not-a-uuid|$NONE_FINGERPRINT"), DuelFilter.NONE))
    }

    @Test
    fun aNonCanonicalPayloadIsRefused() {
        // Same instant as "2026-08-13T10:00:00Z", but not the text encoded() would produce for it.
        assertNull(
            duelCursorOrNull(base64Of("2026-08-13T10:00:00.000Z|$SAMPLE_DUEL_ID|$NONE_FINGERPRINT"), DuelFilter.NONE),
        )
        // Same id as SAMPLE_DUEL_ID, but UUID#toString() always renders lower-case.
        assertNull(
            duelCursorOrNull(
                base64Of("2026-08-13T10:00:00Z|0F8FAD5B-D9CB-469F-A165-70867728950E|$NONE_FINGERPRINT"),
                DuelFilter.NONE,
            ),
        )
    }

    @Test
    fun aPayloadWithPaddingIsRefused() {
        // 76 bytes: 27 for the instant, 1 separator, 36 for the id, 1 separator, 11 for the
        // fingerprint. 76 is not a multiple of three (76 = 3*25 + 1), so the padded encoder still
        // emits two "=" here — a payload whose length landed on a multiple of three would prove
        // nothing.
        val payload = "2026-08-13T10:02:03.000004Z|$SAMPLE_DUEL_ID|$NONE_FINGERPRINT"
        val padded = Base64.getUrlEncoder().encodeToString(payload.toByteArray(Charsets.UTF_8))
        assertTrue(padded.endsWith("=="))
        assertNull(duelCursorOrNull(padded, DuelFilter.NONE))
    }

    @Test
    fun twoDistinctCursorsEncodeToDifferentStrings() {
        val first = DuelCursor(Instant.parse("2026-08-13T10:02:03.000004Z"), UUID.fromString(SAMPLE_DUEL_ID))
        val second =
            DuelCursor(
                Instant.parse("2025-01-01T00:00:00.000001Z"),
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
            )
        assertNotEquals(first.encoded(DuelFilter.NONE), second.encoded(DuelFilter.NONE))
    }
}
