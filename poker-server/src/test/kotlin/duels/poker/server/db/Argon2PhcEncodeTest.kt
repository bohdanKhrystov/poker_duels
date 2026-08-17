package duels.poker.server.db

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

class Argon2PhcEncodeTest {
    @Test
    fun theEncodedStringIsExactlyTheShapeAdr0027Names() {
        val salt = ByteArray(16) { 0x02.toByte() }
        val tag = ByteArray(32) { 0x03.toByte() }
        val phc = Argon2Phc(salt, tag)
        val encoded = phc.encode()
        val expected = "\$argon2id\$v=19\$m=19456,t=2,p=1\$AgICAgICAgICAgICAgICAg\$AwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwM"
        assertEquals(expected, encoded)
    }

    @Test
    fun theEncodingIsStandardBase64NotUrlSafe() {
        val salt = ByteArray(16) { if (it % 2 == 0) 0xFB.toByte() else 0xEF.toByte() }
        val tag = ByteArray(32) { if (it % 2 == 0) 0xFB.toByte() else 0xEF.toByte() }
        val phc = Argon2Phc(salt, tag)
        val encoded = phc.encode()
        val expected = "\$argon2id\$v=19\$m=19456,t=2,p=1\$++/77/vv++/77/vv++/77w\$++/77/vv++/77/vv++/77/vv++/77/vv++/77/vv++8"
        assertEquals(expected, encoded)
        // Verify it contains + and / which are standard base64, not - and _ which are URL-safe
        assertTrue(encoded.contains('+')) { "Encoded string should contain + for standard base64" }
        assertTrue(encoded.contains('/')) { "Encoded string should contain / for standard base64" }
    }

    @Test
    fun theEncodedStringCarriesNoPadding() {
        val salt = ByteArray(16) { 0x02.toByte() }
        val tag = ByteArray(32) { 0x03.toByte() }
        val phc = Argon2Phc(salt, tag)
        val encoded = phc.encode()

        // Extract the base64 parts (everything after the last two $ signs)
        val parts = encoded.split('$')
        val saltB64 = parts.getOrNull(4) ?: ""
        val tagB64 = parts.getOrNull(5) ?: ""

        assertFalse(saltB64.contains('=')) { "Salt base64 should not contain padding character =" }
        assertFalse(tagB64.contains('=')) { "Tag base64 should not contain padding character =" }

        // Also test with the URL-safe case to ensure no padding there either
        val salt2 = ByteArray(16) { if (it % 2 == 0) 0xFB.toByte() else 0xEF.toByte() }
        val tag2 = ByteArray(32) { if (it % 2 == 0) 0xFB.toByte() else 0xEF.toByte() }
        val phc2 = Argon2Phc(salt2, tag2)
        val encoded2 = phc2.encode()

        val parts2 = encoded2.split('$')
        val saltB64_2 = parts2.getOrNull(4) ?: ""
        val tagB64_2 = parts2.getOrNull(5) ?: ""

        assertFalse(saltB64_2.contains('=')) { "Salt base64 should not contain padding character =" }
        assertFalse(tagB64_2.contains('=')) { "Tag base64 should not contain padding character =" }
    }

    @Test
    fun aDifferentTagEncodesToADifferentString() {
        val salt = ByteArray(16) { 0x02.toByte() }
        val tag1 = ByteArray(32) { 0x03.toByte() }
        val tag2 = ByteArray(32) { if (it == 0) 0x04.toByte() else 0x03.toByte() }

        val phc1 = Argon2Phc(salt, tag1)
        val phc2 = Argon2Phc(salt, tag2)

        val encoded1 = phc1.encode()
        val encoded2 = phc2.encode()

        assertFalse(encoded1 == encoded2) { "Different tags should produce different encodings" }
    }

    @Test
    fun aSaltOrTagOfTheWrongLengthIsRefused() {
        // Test salt too short
        val shortSalt = ByteArray(15)
        val validTag = ByteArray(32) { 0x03.toByte() }
        val exceptionSalt = assertFailsWith<IllegalArgumentException> {
            Argon2Phc(shortSalt, validTag)
        }
        // Check that the message only reveals the expected and actual lengths, not the bytes
        assertTrue(!exceptionSalt.message.isNullOrEmpty()) { "Exception should have a message" }
        assertTrue(exceptionSalt.message!!.contains("15")) { "Message should contain the actual length 15" }
        assertTrue(exceptionSalt.message!!.contains("$ARGON2_SALT_BYTES")) { "Message should contain the expected length" }
        // Ensure the bytes are not exposed in the message
        assertFalse(exceptionSalt.message!!.contains("0x")) { "Message should not contain hex bytes" }

        // Test tag too short
        val validSalt = ByteArray(16) { 0x02.toByte() }
        val shortTag = ByteArray(31)
        val exceptionTag = assertFailsWith<IllegalArgumentException> {
            Argon2Phc(validSalt, shortTag)
        }
        // Check that the message only reveals the expected and actual lengths, not the bytes
        assertTrue(!exceptionTag.message.isNullOrEmpty()) { "Exception should have a message" }
        assertTrue(exceptionTag.message!!.contains("31")) { "Message should contain the actual length 31" }
        assertTrue(exceptionTag.message!!.contains("$ARGON2_TAG_BYTES")) { "Message should contain the expected length" }
        // Ensure the bytes are not exposed in the message
        assertFalse(exceptionTag.message!!.contains("0x")) { "Message should not contain hex bytes" }
    }
}
