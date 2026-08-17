package duels.poker.server.db

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.Base64
import kotlin.test.assertContentEquals
import kotlin.test.assertNotNull

// A valid section pair, so every malformed/wrong-parameter case below can hold everything else
// constant and vary exactly the one thing under test.
private const val VALID_SALT_B64 = "AgICAgICAgICAgICAgICAg"
private const val VALID_TAG_B64 = "AwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwM"

private fun phcWith(
    algorithm: String = "argon2id",
    version: String = "v=19",
    params: String = "m=19456,t=2,p=1",
    salt: String = VALID_SALT_B64,
    tag: String = VALID_TAG_B64,
): String = "\$$algorithm\$$version\$$params\$$salt\$$tag"

class Argon2PhcParseTest {
    @Test
    fun whatTheEncoderWroteParsesBackToTheSameBytes() {
        // Both literals are Argon2PhcEncodeTest's own `expected` strings, copied rather than
        // regenerated, so the two tests cannot drift into agreeing with each other while both
        // are wrong.
        val encodedOfAllTwos =
            "\$argon2id\$v=19\$m=19456,t=2,p=1\$AgICAgICAgICAgICAgICAg\$AwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwM"
        val saltOfAllTwos = ByteArray(16) { 0x02.toByte() }
        val tagOfAllTwos = ByteArray(32) { 0x03.toByte() }

        val parsedOfAllTwos = assertNotNull(parseArgon2PhcOrNull(encodedOfAllTwos))
        assertContentEquals(saltOfAllTwos, parsedOfAllTwos.salt)
        assertContentEquals(tagOfAllTwos, parsedOfAllTwos.tag)

        val encodedOfAlternating =
            "\$argon2id\$v=19\$m=19456,t=2,p=1\$++/77/vv++/77/vv++/77w\$++/77/vv++/77/vv++/77/vv++/77/vv++/77/vv++8"
        val saltOfAlternating = ByteArray(16) { if (it % 2 == 0) 0xFB.toByte() else 0xEF.toByte() }
        val tagOfAlternating = ByteArray(32) { if (it % 2 == 0) 0xFB.toByte() else 0xEF.toByte() }

        val parsedOfAlternating = assertNotNull(parseArgon2PhcOrNull(encodedOfAlternating))
        assertContentEquals(saltOfAlternating, parsedOfAlternating.salt)
        assertContentEquals(tagOfAlternating, parsedOfAlternating.tag)
    }

    @Test
    fun everyParameterThatIsNotOursIsRefused() {
        val cases = listOf(
            "algorithm argon2i" to phcWith(algorithm = "argon2i"),
            "algorithm argon2d" to phcWith(algorithm = "argon2d"),
            "algorithm argon2 (missing id)" to phcWith(algorithm = "argon2"),
            "version v=16" to phcWith(version = "v=16"),
            "version v=13" to phcWith(version = "v=13"),
            "params m=4096,t=2,p=1 (memory downgrade attack)" to phcWith(params = "m=4096,t=2,p=1"),
            "params m=19456,t=1,p=1 (iterations downgrade)" to phcWith(params = "m=19456,t=1,p=1"),
            "params m=19456,t=2,p=2 (parallelism changed)" to phcWith(params = "m=19456,t=2,p=2"),
            "params m=19456, t=2, p=1 (added space)" to phcWith(params = "m=19456, t=2, p=1"),
            "params t=2,m=19456,p=1 (reordered)" to phcWith(params = "t=2,m=19456,p=1"),
            "params m=019456,t=2,p=1 (leading zero, not a number parser)" to
                phcWith(params = "m=019456,t=2,p=1"),
        )

        val wronglyAccepted = cases.filter { (_, encoded) -> parseArgon2PhcOrNull(encoded) != null }.map { it.first }

        assertTrue(wronglyAccepted.isEmpty()) { "Wrongly accepted as valid: $wronglyAccepted" }
    }

    @Test
    fun everyMalformedStringIsRefused() {
        // Padded variants decode to exactly the right byte count if the '=' check is skipped —
        // that is what makes the padding cases below non-vacuous.
        val paddedSalt = Base64.getEncoder().encodeToString(ByteArray(ARGON2_SALT_BYTES) { 0x02.toByte() })
        val paddedTag = Base64.getEncoder().encodeToString(ByteArray(ARGON2_TAG_BYTES) { 0x03.toByte() })
        val shortSalt = Base64.getEncoder().withoutPadding().encodeToString(ByteArray(8) { 0x01.toByte() })
        val longSalt = Base64.getEncoder().withoutPadding().encodeToString(ByteArray(24) { 0x01.toByte() })
        val shortTag = Base64.getEncoder().withoutPadding().encodeToString(ByteArray(16) { 0x01.toByte() })
        val longTag = Base64.getEncoder().withoutPadding().encodeToString(ByteArray(48) { 0x01.toByte() })

        val cases = listOf(
            "the empty string" to "",
            "no leading \$" to phcWith().removePrefix("\$"),
            "five sections" to "\$argon2id\$v=19\$m=19456,t=2,p=1\$$VALID_SALT_B64",
            "seven sections" to phcWith() + "\$extra",
            "a prefix before the leading \$ (still six sections)" to "x" + phcWith(),
            "a padded salt (=)" to phcWith(salt = paddedSalt),
            "a padded tag (=)" to phcWith(tag = paddedTag),
            "URL-safe base64 (- and _)" to phcWith(salt = "--_77_vv--_77_vv--_77w"),
            "a non-base64 character (!)" to phcWith(salt = "!!!"),
            "a salt that decodes to 8 bytes (short)" to phcWith(salt = shortSalt),
            "a salt that decodes to 24 bytes (long)" to phcWith(salt = longSalt),
            "a tag that decodes to 16 bytes (short)" to phcWith(tag = shortTag),
            "a tag that decodes to 48 bytes (long)" to phcWith(tag = longTag),
        )

        val wronglyAccepted = cases.filter { (_, encoded) -> parseArgon2PhcOrNull(encoded) != null }.map { it.first }

        assertTrue(wronglyAccepted.isEmpty()) { "Wrongly accepted as valid: $wronglyAccepted" }
    }
}
