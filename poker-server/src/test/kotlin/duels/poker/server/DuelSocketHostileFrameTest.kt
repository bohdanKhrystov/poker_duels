package duels.poker.server

import duels.poker.server.protocol.Hello
import duels.poker.server.protocol.ProtocolCodec
import duels.poker.server.protocol.ServerMessage
import duels.poker.server.protocol.protocolJson
import duels.poker.server.session.InMemoryPlayerDirectory
import duels.poker.server.session.SessionRegistry
import duels.poker.server.session.testDeps
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.server.testing.testApplication
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.decodeFromString
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/** Reads the next frame off [this] session as a decoded [ServerMessage]. */
private suspend fun DefaultClientWebSocketSession.nextServerMessage(): ServerMessage {
    val frame = incoming.receive() as Frame.Text
    return protocolJson.decodeFromString(frame.readText())
}

/** Completes the handshake for [this] session and discards the [ServerMessage.Welcome]. */
private suspend fun DefaultClientWebSocketSession.completeHandshake(deviceId: String) {
    send(Frame.Text(ProtocolCodec.encode(Hello(deviceId = deviceId))))
    nextServerMessage()
}

class DuelSocketHostileFrameTest {
    @Test
    fun aNestingBombIsAnsweredAndTheSocketSurvives() = testApplication {
        val directory = InMemoryPlayerDirectory()
        val sessions = SessionRegistry()
        val deps = testDeps(directory = directory, sessions = sessions)
        application {
            module()
            duelSocket(deps)
        }
        val client = createClient { install(WebSockets) }

        withTimeout(10.seconds) {
            val session = client.webSocketSession("/ws")
            session.completeHandshake("d1")

            // Send a nesting bomb with 50,000 nested brackets
            val bomb = "[".repeat(50_000)
            session.send(Frame.Text(bomb))
            val first = session.nextServerMessage() as ServerMessage.Failure

            // Send an incomplete frame to verify the socket is still responsive
            session.send(Frame.Text("{"))
            val second = session.nextServerMessage() as ServerMessage.Failure

            assertEquals(1, sessions.size)
        }
    }

    @Test
    fun anOversizedFrameIsAnsweredAndTheSocketSurvives() = testApplication {
        val directory = InMemoryPlayerDirectory()
        val sessions = SessionRegistry()
        val deps = testDeps(directory = directory, sessions = sessions)
        application {
            module()
            duelSocket(deps)
        }
        val client = createClient { install(WebSockets) }

        withTimeout(10.seconds) {
            val session = client.webSocketSession("/ws")
            session.completeHandshake("d1")

            // Send an oversized frame: 2,000,000 character string literal
            val oversized = "\"" + "a".repeat(2_000_000) + "\""
            session.send(Frame.Text(oversized))
            val first = session.nextServerMessage() as ServerMessage.Failure

            // Send an incomplete frame to verify the socket is still responsive
            session.send(Frame.Text("{"))
            val second = session.nextServerMessage() as ServerMessage.Failure

            assertEquals(1, sessions.size)
        }
    }

    @Test
    fun aHostileFrameDoesNotDisturbAnotherSession() = testApplication {
        val directory = InMemoryPlayerDirectory()
        val sessions = SessionRegistry()
        val deps = testDeps(directory = directory, sessions = sessions)
        application {
            module()
            duelSocket(deps)
        }
        val client = createClient { install(WebSockets) }

        withTimeout(10.seconds) {
            val first = client.webSocketSession("/ws")
            first.completeHandshake("d1")
            val second = client.webSocketSession("/ws")
            second.completeHandshake("d2")

            // Send a nesting bomb on the first connection
            val bomb = "[".repeat(50_000)
            first.send(Frame.Text(bomb))
            val reply = first.nextServerMessage() as ServerMessage.Failure

            // The second connection should not receive anything within 200ms
            assertNull(withTimeoutOrNull(200.milliseconds) { second.incoming.receive() })
            assertEquals(2, sessions.size)
        }
    }
}
