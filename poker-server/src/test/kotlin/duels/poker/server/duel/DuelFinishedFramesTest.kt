package duels.poker.server.duel

import duels.poker.engine.duel.DuelOutcome
import duels.poker.server.protocol.ProtocolCodec
import duels.poker.server.protocol.ServerMessage
import duels.poker.server.protocol.protocolJson
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class DuelFinishedFramesTest {
    @Test
    fun bothSeatsAreToldTheDuelFinished() {
        val outcome = DuelOutcome(winner = 0, handsPlayed = 10, finalStacks = listOf(2000, 500))
        val frames = finishedFrames(outcome)

        assertEquals(2, frames.size, "Should have exactly two frames")
        assertEquals(0, frames[0].seat, "First frame should be addressed to seat 0")
        assertEquals(1, frames[1].seat, "Second frame should be addressed to seat 1")
    }

    @Test
    fun bothSeatsAreToldTheSameOutcome() {
        val outcome = DuelOutcome(winner = 1, handsPlayed = 15, finalStacks = listOf(100, 2400))
        val frames = finishedFrames(outcome)

        assertEquals(2, frames.size)
        val message0 = frames[0].message as ServerMessage.DuelFinished
        val message1 = frames[1].message as ServerMessage.DuelFinished

        assertEquals(outcome, message0.outcome, "Seat 0 should receive the same outcome")
        assertEquals(outcome, message1.outcome, "Seat 1 should receive the same outcome")
    }

    @Test
    fun aDrawIsCarriedAsADraw() {
        val outcome = DuelOutcome(winner = null, handsPlayed = 20, finalStacks = listOf(1250, 1250))
        val frames = finishedFrames(outcome)

        assertEquals(2, frames.size)
        val message0 = frames[0].message as ServerMessage.DuelFinished
        val message1 = frames[1].message as ServerMessage.DuelFinished

        assertEquals(null, message0.outcome.winner, "Seat 0 should see winner as null (draw)")
        assertEquals(null, message1.outcome.winner, "Seat 1 should see winner as null (draw)")
    }

    @Test
    fun theFinishedFrameSurvivesTheCodec() {
        val outcome = DuelOutcome(winner = 0, handsPlayed = 5, finalStacks = listOf(3000, 200))
        val message = ServerMessage.DuelFinished(outcome)

        val encoded = ProtocolCodec.encode(message)
        val decoded = protocolJson.decodeFromString<ServerMessage>(encoded)

        assertEquals(message, decoded, "Encoded and decoded message should be equal")
    }
}
