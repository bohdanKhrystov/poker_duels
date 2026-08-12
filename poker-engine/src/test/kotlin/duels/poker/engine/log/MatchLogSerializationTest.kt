package duels.poker.engine.log

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout

/**
 * `MatchLog` round-trips through JSON, preserving format, hands, and events, over five real
 * duels.
 */
internal class MatchLogSerializationTest {

    @Test
    @Timeout(120)
    fun aMatchLogRoundTripsOverFiveDuels() {
        for (seed in 1..5) {
            val log = playLoggedDuel(seed.toLong())

            val encoded = encodeMatchLog(log)
            val decoded = decodeMatchLog(encoded)

            assertEquals(log, decoded, "seed $seed: round-trip failed")
        }
    }

    @Test
    @Timeout(120)
    fun theHandLogsSurviveWithTheirEvents() {
        val log = playLoggedDuel(1L)

        val encoded = encodeMatchLog(log)
        val decoded = decodeMatchLog(encoded)

        assertEquals(log.hands, decoded.hands, "hands should survive round-trip element for element")
    }

    @Test
    fun writesTheVersionEvenThoughItIsADefault() {
        val log = playLoggedDuel(1L)

        val encoded = encodeMatchLog(log)

        assertTrue(encoded.contains("\"version\":1"), "encoded match log should contain version member")
    }

    @Test
    fun rejectsAVersionThisBuildDoesNotKnow() {
        val log = playLoggedDuel(1L)
        val encoded = encodeMatchLog(log)

        // MatchLog's own version is the last occurrence (after all nested HandLog versions).
        // We must target only the outer version, not the nested ones, to test that the guard
        // reads from the MatchLog level, not accidentally from a HandLog.
        val lastIndex = encoded.lastIndexOf("\"version\":1")
        val invalidVersion = encoded.replaceRange(lastIndex, lastIndex + "\"version\":1".length, "\"version\":2")

        assertThrows(IllegalArgumentException::class.java) {
            decodeMatchLog(invalidVersion)
        }.let { e ->
            // Verify the error message names both the wrong version and the expected version
            assertTrue(e.message?.contains("2") == true, "error should mention version 2")
            assertTrue(e.message?.contains("1") == true, "error should mention version 1")
        }
    }

    @Test
    fun rejectsAMatchLogWithNoVersionMember() {
        val log = playLoggedDuel(1L)
        val encoded = encodeMatchLog(log)

        // MatchLog's own version is the last occurrence (after all nested HandLog versions).
        // We must remove only the outer version, not the nested ones, to test that the guard
        // reads from the MatchLog level.
        val lastIndex = encoded.lastIndexOf(",\"version\":1")
        val noVersion = encoded.replaceRange(lastIndex, lastIndex + ",\"version\":1".length, "")

        assertThrows(IllegalArgumentException::class.java) {
            decodeMatchLog(noVersion)
        }
    }

    @Test
    fun rejectsTruncatedText() {
        val log = playLoggedDuel(1L)
        val encoded = encodeMatchLog(log)

        val truncated = encoded.dropLast(20)

        assertThrows(IllegalArgumentException::class.java) {
            decodeMatchLog(truncated)
        }
    }
}
