package duels.poker.server.protocol

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class ProtocolCodecJunkTest {
    @Test
    fun unparseableFramesAreMalformed() {
        val inputs = listOf(
            "",
            "   ",
            "<html>",
            "{",
            "{\"type\":\"Hello\"",
            "a".repeat(100_000),
        )

        for (input in inputs) {
            val decoded = ProtocolCodec.decodeClient(input)
            assertEquals(
                Decoded.Refused(ProtocolError.MALFORMED_MESSAGE),
                decoded,
                "Input should produce MALFORMED_MESSAGE: $input",
            )
        }
    }

    @Test
    fun framesThatAreNotJsonObjectsAreMalformed() {
        val inputs = listOf(
            "[1,2,3]",
            "\"just a string\"",
            "42",
            "null",
            "true",
        )

        for (input in inputs) {
            val decoded = ProtocolCodec.decodeClient(input)
            assertEquals(
                Decoded.Refused(ProtocolError.MALFORMED_MESSAGE),
                decoded,
                "Input should produce MALFORMED_MESSAGE: $input",
            )
        }
    }

    @Test
    fun framesWithNoUsableDiscriminatorAreMalformed() {
        val inputs = listOf(
            "{}",
            """{"deviceId":"d1"}""",
            """{"type":7}""",
            """{"type":null}""",
            """{"type":{"a":1}}""",
        )

        for (input in inputs) {
            val decoded = ProtocolCodec.decodeClient(input)
            assertEquals(
                Decoded.Refused(ProtocolError.MALFORMED_MESSAGE),
                decoded,
                "Input should produce MALFORMED_MESSAGE: $input",
            )
        }
    }

    @Test
    fun wellNamedButUnusableBodiesAreMalformed() {
        val inputs = listOf(
            """{"type":"Act"}""",
            """{"type":"Act","handNumber":0,"actionSequence":0,"action":{"type":"Fold","seat":0}}""",
            """{"type":"Act","handNumber":1,"actionSequence":0,"action":{"type":"Bet","seat":0,"to":0}}""",
            """{"type":"Hello","protocolVersion":1,"extra":true}""",
        )

        for (input in inputs) {
            val decoded = ProtocolCodec.decodeClient(input)
            assertEquals(
                Decoded.Refused(ProtocolError.MALFORMED_MESSAGE),
                decoded,
                "Input should produce MALFORMED_MESSAGE: $input",
            )
        }
    }

    @Test
    fun unknownAndWrongDirectionFramesAreUnknown() {
        val inputs = listOf(
            """{"type":"Surrender"}""",
            """{"type":"act"}""",
            """{"type":""}""",
            """{"type":"Welcome","deviceId":"d1"}""",
            """{"type":"Snapshot"}""",
        )

        for (input in inputs) {
            val decoded = ProtocolCodec.decodeClient(input)
            assertEquals(
                Decoded.Refused(ProtocolError.UNKNOWN_MESSAGE),
                decoded,
                "Input should produce UNKNOWN_MESSAGE: $input",
            )
        }
    }

    @Test
    fun aGoodFrameStillDecodesAfterAJunkOne() {
        // Feed all the junk inputs through the codec to ensure it holds no state
        val junkInputs = listOf(
            "",
            "   ",
            "<html>",
            "{",
            "{\"type\":\"Hello\"",
            "a".repeat(100_000),
            "[1,2,3]",
            "\"just a string\"",
            "42",
            "null",
            "true",
            "{}",
            """{"deviceId":"d1"}""",
            """{"type":7}""",
            """{"type":null}""",
            """{"type":{"a":1}}""",
            """{"type":"Act"}""",
            """{"type":"Act","handNumber":0,"actionSequence":0,"action":{"type":"Fold","seat":0}}""",
            """{"type":"Act","handNumber":1,"actionSequence":0,"action":{"type":"Bet","seat":0,"to":0}}""",
            """{"type":"Hello","protocolVersion":1,"extra":true}""",
            """{"type":"Surrender"}""",
            """{"type":"act"}""",
            """{"type":""}""",
            """{"type":"Welcome","deviceId":"d1"}""",
            """{"type":"Snapshot"}""",
        )

        for (junk in junkInputs) {
            ProtocolCodec.decodeClient(junk)
        }

        // Now decode a good frame
        val goodFrame = ProtocolCodec.encode(Hello("d1"))
        val decoded = ProtocolCodec.decodeClient(goodFrame)

        assertEquals(
            Decoded.Message(Hello("d1")),
            decoded,
            "A good frame should still decode after processing junk",
        )
    }
}
