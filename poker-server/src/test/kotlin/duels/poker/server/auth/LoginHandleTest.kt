package duels.poker.server.auth

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.util.Locale

class LoginHandleTest {
    @Test
    fun anAlreadyFoldedHandleIsReturnedUnchanged() {
        val expected = "bob.the-builder_7"
        assertEquals(expected, loginHandleOrNull(expected))
    }

    @Test
    fun uppercaseAsciiIsFolded() {
        // Test basic folding
        assertEquals("bob", loginHandleOrNull("Bob"))
        assertEquals("big", loginHandleOrNull("BIG"))

        // Test Turkish locale to ensure the fold is not locale-sensitive
        // "I" should fold to "i" on any host, not to "ı" (dotless) on Turkish locale
        val originalLocale = Locale.getDefault()
        try {
            Locale.setDefault(Locale("tr", "TR"))
            assertEquals("iii", loginHandleOrNull("III"))
        } finally {
            Locale.setDefault(originalLocale)
        }
    }

    @Test
    fun theKelvinSignIsRefusedRatherThanFoldedToK() {
        // U+212A KELVIN SIGN should not be folded to 'k', so the entire handle is refused
        // Construct the Kelvin sign using its code point (0x212A)
        val kelvinSign = Character.toChars(0x212A).joinToString("")
        assertNull(loginHandleOrNull("ab$kelvinSign"))
    }

    @Test
    fun everyPermittedCharacterIsAccepted() {
        // All 39 permitted characters: a-z (26) + 0-9 (10) + . _ - (3)
        val permittedChars = "abcdefghijklmnopqrstuvwxyz0123456789._-"
        for (char in permittedChars) {
            val handle = "ab$char"
            assertEquals("ab$char", loginHandleOrNull(handle), "Character '$char' (U+${char.code.toString(16).padStart(4, '0')}) should be permitted mid-handle")
        }
    }

    @Test
    fun everyCharacterOutsideTheSetIsRefused() {
        // Test various forbidden characters
        val forbiddenChars = listOf(
            ' ' to "space",
            '@' to "at-sign",
            '+' to "plus",
            '/' to "slash",
            '!' to "exclamation",
            'é' to "latin-small-letter-e-with-acute",
            '\t' to "tab",
            '\n' to "newline",
            '​' to "zero-width-space",
        )

        val failed = mutableListOf<String>()
        for ((char, name) in forbiddenChars) {
            val result = loginHandleOrNull("ab${char}cd")
            if (result != null) {
                failed.add("Character '$name' (U+${char.code.toString(16).padStart(4, '0')}) was not refused, got: $result")
            }
        }

        assertEquals(emptyList<String>(), failed, "Forbidden characters that got through:\n${failed.joinToString("\n")}")
    }

    @Test
    fun theLengthBoundsAreThreeAndThirtyTwo() {
        // Length 2 should be refused
        assertNull(loginHandleOrNull("ab"), "Length 2 should be refused")

        // Length 3 should be accepted
        assertEquals("abc", loginHandleOrNull("abc"), "Length 3 should be accepted")

        // Length 32 should be accepted
        val length32 = "a" + "b".repeat(31)
        assertEquals(length32, loginHandleOrNull(length32), "Length 32 should be accepted")

        // Length 33 should be refused
        val length33 = "a" + "b".repeat(32)
        assertNull(loginHandleOrNull(length33), "Length 33 should be refused")
    }

    @Test
    fun aHandleMustStartWithALetterOrDigit() {
        // Leading dot should be refused
        assertNull(loginHandleOrNull(".bob"), "Leading dot should be refused")

        // Leading underscore should be refused
        assertNull(loginHandleOrNull("_bob"), "Leading underscore should be refused")

        // Leading hyphen should be refused
        assertNull(loginHandleOrNull("-bob"), "Leading hyphen should be refused")

        // Leading digit should be accepted
        assertEquals("1bob", loginHandleOrNull("1bob"), "Leading digit should be accepted")

        // Leading letter should be accepted (already tested elsewhere, but explicit)
        assertEquals("abob", loginHandleOrNull("abob"), "Leading letter should be accepted")
    }

    @Test
    fun aHandleWithSurroundingSpacesIsRefusedRatherThanTrimmed() {
        val result = loginHandleOrNull(" bob ")
        assertNull(result, "Handle with surrounding spaces should be refused, not trimmed to 'bob'")
    }

    @Test
    fun theEmptyStringIsRefused() {
        assertNull(loginHandleOrNull(""), "Empty string should be refused")
    }
}
