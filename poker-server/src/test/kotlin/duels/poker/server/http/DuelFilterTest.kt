package duels.poker.server.http

import duels.poker.server.protocol.http.DuelOutcomeLabel
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DuelFilterTest {
    @Test
    fun everyOutcomeLabelParsesFromItsOwnName() {
        // The test is universal: it claims every entry parses. The assertion stops it passing
        // over an empty set.
        assertTrue(DuelOutcomeLabel.entries.isNotEmpty(), "the sweep must run over something")
        for (entry in DuelOutcomeLabel.entries) {
            assertEquals(entry, duelOutcomeOrNull(entry.name))
        }
    }

    @Test
    fun aLowerCaseOutcomeIsRefused() {
        assertNull(duelOutcomeOrNull("won"))
        assertNull(duelOutcomeOrNull("Drew"))
    }

    @Test
    fun anOutcomeThatIsNotALabelIsRefused() {
        assertNull(duelOutcomeOrNull("FOLDED"))
        assertNull(duelOutcomeOrNull("WON "))
        assertNull(duelOutcomeOrNull(""))
    }

    @Test
    fun noFilterNarrowsNeitherAxis() {
        assertEquals(null, DuelFilter.NONE.outcome)
        assertEquals(null, DuelFilter.NONE.opponent)
    }

    @Test
    fun aTermIsReturnedInItsNfcForm() {
        // Input: 'e' (U+0065) + combining acute (U+0301) + "lodie" = 7 code points
        // Expected: composed é (U+00E9) + "lodie" = 6 code points
        // Both assertions prevent the function from merely returning the input unchanged.
        val input = "e\u0301lodie"
        val result = opponentSearchOrNull(input)
        val expected = "\u00e9lodie"
        assertEquals(expected, result)
        assertNotEquals(input, result)
    }

    @Test
    fun aBlankTermIsRefused() {
        assertNull(opponentSearchOrNull(""))
        assertNull(opponentSearchOrNull("   "))
    }

    @Test
    fun aTermOfThirtyTwoCodePointsIsAcceptedAndThirtyThreeIsNot() {
        val accepted = "a".repeat(32)
        assertEquals(accepted, opponentSearchOrNull(accepted))
        assertNull(opponentSearchOrNull("a".repeat(33)))
    }

    @Test
    fun theLimitCountsCodePointsNotUtf16Units() {
        // One musical note symbol: U+1D11E, encoded as surrogate pair (2 UTF-16 units, 1 code point)
        val accepted = "𝄞".repeat(32)
        assertEquals(accepted, opponentSearchOrNull(accepted))
        assertNull(opponentSearchOrNull("𝄞".repeat(33)))
    }

    @Test
    fun aTermKeepsTheSpacesItWasGiven() {
        assertEquals(" Hal", opponentSearchOrNull(" Hal"))
    }

    @Test
    fun aStringWithJustEnoughCodePointsIsAcceptedDespiteExcessiveUtf16Units() {
        // 31 astral characters + 1 ASCII = 32 code points but 63 UTF-16 units
        // This would still be accepted even if .length was checking > 31 instead of > 32
        val thirtyOneAstralOneLetter = "𝄞".repeat(31) + "x"
        assertEquals(thirtyOneAstralOneLetter, opponentSearchOrNull(thirtyOneAstralOneLetter))
    }
}
