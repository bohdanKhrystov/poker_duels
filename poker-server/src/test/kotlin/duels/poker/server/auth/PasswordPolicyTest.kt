package duels.poker.server.auth

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PasswordPolicyTest {
    @Test
    fun sevenCodePointsIsTooShort() {
        val secret = PresentedSecret("a".repeat(7))
        assertFalse(passwordIsLongEnough(secret))
    }

    @Test
    fun eightCodePointsIsLongEnough() {
        val secret = PresentedSecret("a".repeat(8))
        assertTrue(passwordIsLongEnough(secret))
    }

    @Test
    fun oneHundredAndTwentyEightCodePointsIsWithinTheBound() {
        val secret = PresentedSecret("a".repeat(128))
        assertTrue(passwordIsWithinTheWorkBound(secret))
    }

    @Test
    fun oneHundredAndTwentyNineCodePointsIsOverTheBound() {
        val secret = PresentedSecret("a".repeat(129))
        assertFalse(passwordIsWithinTheWorkBound(secret))
    }

    @Test
    fun fourAstralCharactersAreTooShortThoughTheyAreEightUtf16Units() {
        // U+1F600 GRINNING FACE: one code point, but a surrogate pair — two UTF-16 units.
        val astralCharacter = String(Character.toChars(0x1F600))
        val secret = PresentedSecret(astralCharacter.repeat(4))
        assertEquals(8, secret.value.length, "four surrogate pairs must measure 8 UTF-16 units")
        assertFalse(passwordIsLongEnough(secret))
    }

    @Test
    fun oneHundredAndTwentyEightAstralCharactersAreWithinTheBound() {
        val astralCharacter = String(Character.toChars(0x1F600))
        val secret = PresentedSecret(astralCharacter.repeat(128))
        assertEquals(256, secret.value.length, "128 surrogate pairs must measure 256 UTF-16 units")
        assertTrue(passwordIsWithinTheWorkBound(secret))
    }

    @Test
    fun sevenComposableCharactersAreTooShortThoughTheyAreFourteenBeforeNfc() {
        // 'e' + U+0301 COMBINING ACUTE ACCENT: two code points as typed, one after NFC ('é').
        val decomposedE = "e\u0301"
        val secret = PresentedSecret(decomposedE.repeat(7))
        assertEquals(
            14,
            secret.value.codePointCount(0, secret.value.length),
            "seven decomposed pairs must be 14 code points before NFC",
        )
        assertFalse(passwordIsLongEnough(secret))
    }

    @Test
    fun theFiLigatureStaysOneCodePointUnderNfcRatherThanBecomingTwoUnderNfkc() {
        // U+FB01 LATIN SMALL LIGATURE FI has no canonical decomposition, only a compatibility
        // one — NFC leaves it alone; NFKC would fold it to 'f' + 'i'.
        val ligature = String(Character.toChars(0xFB01))
        val normalised = nfcNormalisedSecret(PresentedSecret(ligature))
        assertEquals(1, normalised.codePointCount(0, normalised.length))
    }

    @Test
    fun nothingIsTrimmed() {
        val oneNonSpaceCharacter = PresentedSecret(" ".repeat(3) + "a" + " ".repeat(4))
        assertEquals(8, oneNonSpaceCharacter.value.length)
        assertTrue(passwordIsLongEnough(oneNonSpaceCharacter))

        val allSpaces = PresentedSecret(" ".repeat(8))
        assertTrue(passwordIsLongEnough(allSpaces))
    }

    @Test
    fun noCodePointIsRefused() {
        val nullCharacter = "\u0000"
        val rightToLeftOverride = "\u202E"
        val emoji = String(Character.toChars(0x1F600))
        val cjkCharacter = "\u6CE8"
        val secrets =
            listOf(
                PresentedSecret(nullCharacter.repeat(3) + "abcdefgh"),
                PresentedSecret(rightToLeftOverride.repeat(3) + "abcdefgh"),
                PresentedSecret(emoji.repeat(20)),
                PresentedSecret(cjkCharacter.repeat(20)),
                PresentedSecret("abcd efgh"),
            )

        assertTrue(secrets.isNotEmpty(), "an empty fixture list would pass this test vacuously")

        for (secret in secrets) {
            val codePoints = secret.value.codePointCount(0, secret.value.length)
            assertTrue(
                codePoints in 8..128,
                "fixture must be padded to between 8 and 128 code points, was $codePoints",
            )
            assertTrue(passwordIsLongEnough(secret), "code points=$codePoints should be long enough")
            assertTrue(passwordIsWithinTheWorkBound(secret), "code points=$codePoints should be within the work bound")
        }
    }
}
