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

    @Test
    fun rejectsAMatchLogWhoseNestedHandHasAnUnknownVersion() {
        val log = playLoggedDuel(1L)
        check(log.hands.size >= 3) {
            "test needs at least 3 hands, seed 1L produced ${log.hands.size}"
        }
        val encoded = encodeMatchLog(log)

        // Each HandLog writes its own "version" field before the outer MatchLog's, in hand
        // order, so the third occurrence of the version member in the document is the third
        // hand's. Splicing that one occurrence by index — not String.replace, which is global —
        // touches only the third hand's version and leaves every other version (the first two
        // hands', the rest of the hands, and the outer one) untouched.
        val thirdHandVersionIndex = nthIndexOf(encoded, "\"version\":1", occurrence = 3)
        val corrupted = encoded.replaceRange(
            thirdHandVersionIndex,
            thirdHandVersionIndex + "\"version\":1".length,
            "\"version\":2",
        )

        val exception = assertThrows(IllegalArgumentException::class.java) {
            decodeMatchLog(corrupted)
        }
        assertTrue(exception.message?.contains("3") == true, "error should name hand 3")
        assertTrue(exception.message?.contains("2") == true, "error should mention version 2")
        assertTrue(exception.message?.contains("1") == true, "error should mention version 1")
    }

    @Test
    fun rejectsAMatchLogWhoseNestedHandHasNoVersionMember() {
        val log = playLoggedDuel(1L)
        check(log.hands.size >= 3) {
            "test needs at least 3 hands, seed 1L produced ${log.hands.size}"
        }
        val encoded = encodeMatchLog(log)

        // Same targeting as above: strip only the third hand's ",\"version\":1" member, leaving
        // every other hand's and the outer version intact.
        val thirdHandVersionIndex = nthIndexOf(encoded, ",\"version\":1", occurrence = 3)
        val corrupted = encoded.replaceRange(
            thirdHandVersionIndex,
            thirdHandVersionIndex + ",\"version\":1".length,
            "",
        )

        val exception = assertThrows(IllegalArgumentException::class.java) {
            decodeMatchLog(corrupted)
        }
        assertTrue(
            exception.message?.contains("version") == true,
            "message should mention version",
        )
    }

    @Test
    fun aGoodMatchLogStillDecodes() {
        val log = playLoggedDuel(1L)
        val encoded = encodeMatchLog(log)

        val decoded = decodeMatchLog(encoded)

        assertEquals(log, decoded, "a match log with no corruption should still round-trip")
    }
}

/** The index of the [occurrence]-th appearance of [target] in [text], 1-based. */
private fun nthIndexOf(text: String, target: String, occurrence: Int): Int {
    var index = -1
    repeat(occurrence) {
        index = text.indexOf(target, index + 1)
        check(index >= 0) { "expected occurrence $occurrence of \"$target\" in text" }
    }
    return index
}
