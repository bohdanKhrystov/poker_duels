package duels.poker.server.protocol

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * `decodeClient` must refuse a frame before handing it to the parser, both when it is too long
 * and when it is nested too deeply — see `TASK-020213`. These pin the boundaries and the exact
 * regression found by fuzzing during the `TASK-020208` review.
 */
internal class FrameLimitTest {
    @Test
    fun aFrameLongerThanTheLimitIsRefused() {
        val maxLength = 32
        // Syntactically a valid Hello frame — if the length check did not run before parsing,
        // this would decode successfully instead of being refused.
        val oversized = """{"type":"Hello","deviceId":"${"d".repeat(maxLength)}"}"""

        val decoded = ProtocolCodec.decodeClient(oversized, maxFrameLength = maxLength)

        assertEquals(Decoded.Refused(ProtocolError.FRAME_LIMIT_EXCEEDED), decoded)
    }

    @Test
    fun aFrameDeeperThanTheLimitIsRefused() {
        val maxDepth = 8
        val depth = maxDepth + 1
        val nested = "[".repeat(depth) + "]".repeat(depth)

        val decoded = ProtocolCodec.decodeClient(nested, maxNestingDepth = maxDepth)

        assertEquals(Decoded.Refused(ProtocolError.FRAME_LIMIT_EXCEEDED), decoded)
    }

    @Test
    fun theNestingBombNoLongerEscapes() {
        // The exact shape that produced StackOverflowError during TASK-020208's fuzzing review:
        // thousands of unmatched opening brackets, so the recursive-descent parser would recurse
        // itself out of stack before ever reaching a syntax error. Depth is stated explicitly so
        // this test stays meaningful if the default limit ever changes.
        val depth = 10_000
        val bomb = "[".repeat(depth)

        val decoded = ProtocolCodec.decodeClient(bomb)

        assertEquals(Decoded.Refused(ProtocolError.FRAME_LIMIT_EXCEEDED), decoded)
    }

    @Test
    fun aFrameAtExactlyTheLimitIsAccepted() {
        val maxDepth = 8
        val nested = "[".repeat(maxDepth) + "]".repeat(maxDepth)

        val decoded = ProtocolCodec.decodeClient(nested, maxNestingDepth = maxDepth)

        // Not refused for depth — the boundary is inclusive, so a frame exactly at the limit
        // clears the gate and reaches the parser, which then refuses it for the ordinary reason
        // that a bare array is not a JSON object.
        assertEquals(Decoded.Refused(ProtocolError.MALFORMED_MESSAGE), decoded)
    }

    @Test
    fun anOrdinaryFrameIsUnaffected() {
        val hello = Hello("d1")

        val decoded = ProtocolCodec.decodeClient(ProtocolCodec.encode(hello))

        assertEquals(Decoded.Message(hello), decoded)
    }
}
