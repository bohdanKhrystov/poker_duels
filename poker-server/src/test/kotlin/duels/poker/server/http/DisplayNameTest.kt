package duels.poker.server.http

import org.junit.jupiter.api.Test
import java.text.Normalizer
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DisplayNameTest {
    @Test
    fun aPlainNameIsReturnedUnchanged() {
        assertEquals("bob", canonicalDisplayNameOrNull("bob"))
        assertEquals("Zoe42", canonicalDisplayNameOrNull("Zoe42"))
    }

    @Test
    fun surroundingSpaceIsTrimmed() {
        assertEquals("bob", canonicalDisplayNameOrNull("  bob  "))
        assertEquals("bob", canonicalDisplayNameOrNull("\t bob \n"))
    }

    @Test
    fun aDecomposedNameIsComposed() {
        // U+0065 "e" followed by U+0301 COMBINING ACUTE ACCENT: the decomposed spelling of
        // "e" with an acute. NFC composes the pair into the single code point U+00E9.
        val decomposedElodie = "e\u0301lodie"
        val elodie = canonicalDisplayNameOrNull(decomposedElodie)
        assertEquals("\u00E9lodie", elodie)
        assertEquals(6, elodie!!.codePointCount(0, elodie.length))

        val decomposedCafe = "cafe\u0301"
        val cafe = canonicalDisplayNameOrNull(decomposedCafe)
        assertEquals("caf\u00E9", cafe)
        assertEquals(4, cafe!!.codePointCount(0, cafe.length))
    }

    @Test
    fun anAlreadyComposedNameIsUnchanged() {
        // U+00E9 already precomposed: nothing for NFC to do.
        val elodie = "\u00E9lodie"
        assertEquals(elodie, canonicalDisplayNameOrNull(elodie))

        val cafe = "caf\u00E9"
        assertEquals(cafe, canonicalDisplayNameOrNull(cafe))
    }

    @Test
    fun aBlankInputIsRefused() {
        assertNull(canonicalDisplayNameOrNull(""))
        assertNull(canonicalDisplayNameOrNull("   "))
    }

    @Test
    fun thirtyTwoCodePointsAreAccepted() {
        val name = "a".repeat(32)
        assertEquals(name, canonicalDisplayNameOrNull(name))
    }

    @Test
    fun thirtyThreeCodePointsAreRefused() {
        val name = "a".repeat(33)
        assertNull(canonicalDisplayNameOrNull(name))
    }

    @Test
    fun astralCharactersCountAsOneEach() {
        // U+1D504 MATHEMATICAL FRAKTUR CAPITAL A, written as its UTF-16 surrogate pair: one
        // code point but two UTF-16 units. NFC leaves it untouched (no canonical decomposition).
        val fraktur = "\uD835\uDD04"

        val seventeen = fraktur.repeat(17)
        assertEquals(34, seventeen.length)
        val accepted = canonicalDisplayNameOrNull(seventeen)
        assertEquals(seventeen, accepted)
        assertEquals(17, accepted!!.codePointCount(0, accepted.length))

        val thirtyThree = fraktur.repeat(33)
        assertNull(canonicalDisplayNameOrNull(thirtyThree))
    }

    @Test
    fun theBoundIsMeasuredAfterNormalising() {
        // 31 plain letters plus one decomposed pair: 33 code points before normalising,
        // 32 after the pair composes into a single code point.
        val oneComposition = "a".repeat(31) + "e\u0301"
        assertEquals(33, oneComposition.codePointCount(0, oneComposition.length))
        val oneResult = canonicalDisplayNameOrNull(oneComposition)
        assertEquals("a".repeat(31) + "\u00E9", oneResult)
        assertEquals(32, oneResult!!.codePointCount(0, oneResult.length))

        // A second, independent case: two decomposed pairs, 34 code points collapsing to 32.
        val twoCompositions = "a".repeat(30) + "e\u0301" + "e\u0301"
        assertEquals(34, twoCompositions.codePointCount(0, twoCompositions.length))
        val twoResult = canonicalDisplayNameOrNull(twoCompositions)
        assertEquals("a".repeat(30) + "\u00E9\u00E9", twoResult)
        assertEquals(32, twoResult!!.codePointCount(0, twoResult.length))
    }

    @Test
    fun canonicalisingTwiceIsIdempotent() {
        val fromDecomposed = canonicalDisplayNameOrNull("e\u0301lodie")
        assertEquals(fromDecomposed, canonicalDisplayNameOrNull(fromDecomposed!!))

        val fromPadded = canonicalDisplayNameOrNull("  bob  ")
        assertEquals(fromPadded, canonicalDisplayNameOrNull(fromPadded!!))
    }

    @Test
    fun theCanonicalFormSatisfiesEveryDatabaseCheck() {
        val inputs = listOf("  bob  ", "e\u0301lodie", "a".repeat(31) + "e\u0301")
        for (raw in inputs) {
            val canonical = canonicalDisplayNameOrNull(raw)
            checkNotNull(canonical)

            // V3's player_display_name_trimmed CHECK: display_name = btrim(display_name).
            assertEquals(canonical, canonical.trim())

            // V3's player_display_name_nfc CHECK: display_name IS NFC NORMALIZED.
            assertTrue(Normalizer.isNormalized(canonical, Normalizer.Form.NFC))

            // V3's player_display_name_length CHECK, in code points, matching char_length.
            val codePoints = canonical.codePointCount(0, canonical.length)
            assertTrue(codePoints in MIN_CHECKED_CODE_POINTS..MAX_CHECKED_CODE_POINTS)
        }
    }

    private companion object {
        private const val MIN_CHECKED_CODE_POINTS = 1
        private const val MAX_CHECKED_CODE_POINTS = 32
    }
}
