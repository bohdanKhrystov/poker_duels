package duels.poker.engine.log

import duels.poker.engine.game.PlayedHand
import duels.poker.engine.game.playRandomHand
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout

/**
 * [encodeHandLog] writes hand logs with versions, and [decodeHandLog] rejects logs with missing
 * or unknown versions, protecting against schema drift.
 */
internal class HandLogJsonTest {

    @Test
    @Timeout(60)
    fun roundTripsTenRandomHands() {
        for (seed in 1L..10L) {
            val played = playRandomHand(seed)
            val log = logOf(seed, played)

            val encoded = encodeHandLog(log)
            val decoded = decodeHandLog(encoded)

            assertEquals(log, decoded, "seed $seed: round-trip failed")
        }
    }

    @Test
    fun writesTheVersionEvenThoughItIsADefault() {
        val played = playRandomHand(1)
        val log = logOf(1, played)

        val encoded = encodeHandLog(log)

        assertTrue(encoded.contains("\"version\":1"), "encoded log should contain version:1")
    }

    @Test
    fun rejectsAVersionThisBuildDoesNotKnow() {
        val played = playRandomHand(1)
        val log = logOf(1, played)
        val encoded = encodeHandLog(log)

        // Replace version 1 with version 2
        val unknownVersionJson = encoded.replace("\"version\":1", "\"version\":2")

        val exception = assertThrows(IllegalArgumentException::class.java) {
            decodeHandLog(unknownVersionJson)
        }

        val message = exception.message ?: ""
        assertTrue(message.contains("2"), "message should contain the unknown version 2")
        assertTrue(message.contains("1"), "message should contain the expected version 1")
    }

    @Test
    fun rejectsALogWithNoVersionMember() {
        val played = playRandomHand(1)
        val log = logOf(1, played)
        val encoded = encodeHandLog(log)

        // Remove the version member
        val noVersionJson = encoded.replace(",\"version\":1", "")

        val exception = assertThrows(IllegalArgumentException::class.java) {
            decodeHandLog(noVersionJson)
        }

        assertTrue(
            exception.message?.contains("version") == true,
            "message should mention version",
        )
    }

    @Test
    fun rejectsTruncatedText() {
        val played = playRandomHand(1)
        val log = logOf(1, played)
        val encoded = encodeHandLog(log)

        val truncated = encoded.dropLast(20)

        assertThrows(IllegalArgumentException::class.java) {
            decodeHandLog(truncated)
        }
    }

    @Test
    fun rejectsTextThatIsNotAnObject() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            decodeHandLog("[]")
        }

        assertTrue(
            exception.message?.contains("object") == true || exception.message?.contains("JSON") == true,
            "message should indicate the text is not a valid object",
        )
    }
}

private fun logOf(seed: Long, played: PlayedHand) = HandLog(
    seed = seed,
    handNumber = played.opening.handNumber,
    buttonSeat = played.opening.buttonSeat,
    stacks = played.opening.seats.map { it.stack },
    smallBlind = played.opening.smallBlind,
    bigBlind = played.opening.bigBlind,
    actions = played.actions,
    events = played.events,
)
