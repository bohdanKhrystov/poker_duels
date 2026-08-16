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
    fun oneCodePointIsAccepted() {
        // The lower bound from the accepting side: aBlankInputIsRefused only pins 0 code
        // points, so without this an off-by-one at the bottom (rejecting a single character)
        // would pass every other test in this file.
        assertEquals("a", canonicalDisplayNameOrNull("a"))
        assertEquals("\uD835\uDD04", canonicalDisplayNameOrNull("\uD835\uDD04"))
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

    @Test
    fun aControlCharacterIsRefused() {
        // U+0007 BELL, a C0 control character: Character.getType is CONTROL.
        assertNull(canonicalDisplayNameOrNull("bo\u0007b"))
        // U+001F UNIT SEPARATOR, the other end of the C0 control range: also CONTROL.
        assertNull(canonicalDisplayNameOrNull("bo\u001Fb"))
        // U+0085 NEXT LINE (NEL), a C1 control outside the two above: still CONTROL,
        // so a blocklist of only U+0007/U+001F would miss it.
        assertNull(canonicalDisplayNameOrNull("bo\u0085b"))
    }

    @Test
    fun aZeroWidthCharacterIsRefused() {
        // U+200B ZERO WIDTH SPACE, U+200D ZERO WIDTH JOINER and U+FEFF ZERO WIDTH NO-BREAK
        // SPACE (the byte-order mark): three distinct Cf characters, each asserted on its own
        // so no single one stands in for the whole category.
        assertNull(canonicalDisplayNameOrNull("bo\u200Bb"))
        assertNull(canonicalDisplayNameOrNull("bo\u200Db"))
        assertNull(canonicalDisplayNameOrNull("bo\uFEFFb"))
        // U+2060 WORD JOINER, a Cf character none of the three above stands in for:
        // still refused by the category rule, not by an enumerated list.
        assertNull(canonicalDisplayNameOrNull("bo\u2060b"))
    }

    @Test
    fun aBidirectionalOverrideIsRefused() {
        // U+202E RIGHT-TO-LEFT OVERRIDE: the spoof the Cf rule is for — it can make one
        // name render as another.
        assertNull(canonicalDisplayNameOrNull("bo\u202Eb"))
        // U+202A LEFT-TO-RIGHT EMBEDDING: a second, distinct bidi control, same category.
        assertNull(canonicalDisplayNameOrNull("bo\u202Ab"))
    }

    @Test
    fun aTabOrNewlineIsRefused() {
        assertNull(canonicalDisplayNameOrNull("bo\tb"))
        assertNull(canonicalDisplayNameOrNull("bo\nb"))
    }

    @Test
    fun anExoticSpaceIsRefused() {
        // U+00A0 NO-BREAK SPACE and U+2003 EM SPACE: neither is U+0020, and neither is
        // Cc/Cf — both are category Zs, so this exercises the whitespace rule, not the
        // control rule.
        assertNull(canonicalDisplayNameOrNull("bo\u00A0b"))
        assertNull(canonicalDisplayNameOrNull("bo\u2003b"))
        // U+3000 IDEOGRAPHIC SPACE, a Zs character neither of the two above stands in
        // for: still caught by isSpaceChar.
        assertNull(canonicalDisplayNameOrNull("bo\u3000b"))
    }

    @Test
    fun twoSpacesInARowAreRefused() {
        assertNull(canonicalDisplayNameOrNull("Bob  Smith"))
        // A second, distinct pair in a different name — the rule is not one string's
        // fluke.
        assertNull(canonicalDisplayNameOrNull("Jean  Paul"))
        // The single-space form of the same name is accepted in the same test run, so the
        // rule is shown to discriminate rather than refuse every name that contains a space.
        assertEquals("Bob Smith", canonicalDisplayNameOrNull("Bob Smith"))
    }

    @Test
    fun aSingleInteriorSpaceIsKept() {
        assertEquals("Bob Smith", canonicalDisplayNameOrNull("Bob Smith"))
        assertEquals("Jean Paul", canonicalDisplayNameOrNull("Jean Paul"))
    }

    @Test
    fun nonLatinScriptsAreAccepted() {
        // Cyrillic "Ivan": none of these code points are Cc, Cf or whitespace — the
        // refusal is of the invisible, not of the unfamiliar.
        val cyrillic = "\u0418\u0432\u0430\u043D"
        assertEquals(cyrillic, canonicalDisplayNameOrNull(cyrillic))

        // CJK "Tanaka": a second, distinct script, same claim.
        val cjk = "\u7530\u4E2D"
        assertEquals(cjk, canonicalDisplayNameOrNull(cjk))
    }

    private companion object {
        private const val MIN_CHECKED_CODE_POINTS = 1
        private const val MAX_CHECKED_CODE_POINTS = 32
    }
}
