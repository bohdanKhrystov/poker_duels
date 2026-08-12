package duels.poker.server

import duels.poker.engine.game.PlayerAction
import duels.poker.server.protocol.Act
import duels.poker.server.protocol.Hello
import duels.poker.server.protocol.ProtocolCodec
import duels.poker.server.protocol.ProtocolError
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
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.decodeFromString
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
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

class DuelSocketFrameLoopTest {
    @Test
    fun aMalformedFrameIsAnsweredAndTheSocketStaysOpen() = testApplication {
        val directory = InMemoryPlayerDirectory()
        val sessions = SessionRegistry()
        val deps = testDeps(directory = directory, sessions = sessions)
        application {
            module()
            duelSocket(deps)
        }
        val client = createClient { install(WebSockets) }

        withTimeout(5.seconds) {
            val session = client.webSocketSession("/ws")
            session.completeHandshake("d1")

            session.send(Frame.Text("{"))
            val first = session.nextServerMessage() as ServerMessage.Failure
            assertEquals(ProtocolError.MALFORMED_MESSAGE, first.error)

            session.send(Frame.Text("{"))
            val second = session.nextServerMessage() as ServerMessage.Failure
            assertEquals(ProtocolError.MALFORMED_MESSAGE, second.error)

            assertEquals(1, sessions.size)
        }
    }

    @Test
    fun anUnknownTypeIsAnsweredWithUnknownMessage() = testApplication {
        val directory = InMemoryPlayerDirectory()
        val sessions = SessionRegistry()
        val deps = testDeps(directory = directory, sessions = sessions)
        application {
            module()
            duelSocket(deps)
        }
        val client = createClient { install(WebSockets) }

        withTimeout(5.seconds) {
            val session = client.webSocketSession("/ws")
            session.completeHandshake("d1")

            session.send(Frame.Text("""{"type":"Nope"}"""))
            val reply = session.nextServerMessage() as ServerMessage.Failure

            assertEquals(ProtocolError.UNKNOWN_MESSAGE, reply.error)
        }
    }

    @Test
    fun anActOutsideADuelIsAnsweredWithNotInDuel() = testApplication {
        val directory = InMemoryPlayerDirectory()
        val sessions = SessionRegistry()
        val deps = testDeps(directory = directory, sessions = sessions)
        application {
            module()
            duelSocket(deps)
        }
        val client = createClient { install(WebSockets) }

        withTimeout(5.seconds) {
            val session = client.webSocketSession("/ws")
            session.completeHandshake("d1")

            val act = Act(handNumber = 1, actionSequence = 0, action = PlayerAction.Fold(seat = 0))
            session.send(Frame.Text(ProtocolCodec.encode(act)))
            val reply = session.nextServerMessage() as ServerMessage.Failure

            assertEquals(ProtocolError.NOT_IN_DUEL, reply.error)
        }
    }

    @Test
    fun aSecondHelloIsRefusedWithoutClosing() = testApplication {
        val directory = InMemoryPlayerDirectory()
        val sessions = SessionRegistry()
        val deps = testDeps(directory = directory, sessions = sessions)
        application {
            module()
            duelSocket(deps)
        }
        val client = createClient { install(WebSockets) }

        withTimeout(5.seconds) {
            val session = client.webSocketSession("/ws")
            session.completeHandshake("d1")

            session.send(Frame.Text(ProtocolCodec.encode(Hello(deviceId = "d1"))))
            val reply = session.nextServerMessage() as ServerMessage.Failure

            assertEquals(ProtocolError.MALFORMED_MESSAGE, reply.error)
            assertTrue(session.isActive)
        }
    }

    @Test
    fun aBinaryFrameIsRefusedWithoutClosing() = testApplication {
        val directory = InMemoryPlayerDirectory()
        val sessions = SessionRegistry()
        val deps = testDeps(directory = directory, sessions = sessions)
        application {
            module()
            duelSocket(deps)
        }
        val client = createClient { install(WebSockets) }

        withTimeout(5.seconds) {
            val session = client.webSocketSession("/ws")
            session.completeHandshake("d1")

            session.send(Frame.Binary(true, byteArrayOf(1, 2, 3)))
            val reply = session.nextServerMessage() as ServerMessage.Failure

            assertEquals(ProtocolError.MALFORMED_MESSAGE, reply.error)
            assertTrue(session.isActive)
        }
    }

    @Test
    fun fiftyBadFramesProduceFiftyWholeFailures() = testApplication {
        val directory = InMemoryPlayerDirectory()
        val sessions = SessionRegistry()
        val deps = testDeps(directory = directory, sessions = sessions)
        application {
            module()
            duelSocket(deps)
        }
        val client = createClient { install(WebSockets) }

        withTimeout(5.seconds) {
            val session = client.webSocketSession("/ws")
            session.completeHandshake("d1")

            repeat(50) { session.send(Frame.Text("{")) }

            repeat(50) {
                val reply = session.nextServerMessage() as ServerMessage.Failure
                assertEquals(ProtocolError.MALFORMED_MESSAGE, reply.error)
            }
        }
    }

    @Test
    fun oneClientsBadFrameDoesNotDisturbAnother() = testApplication {
        val directory = InMemoryPlayerDirectory()
        val sessions = SessionRegistry()
        val deps = testDeps(directory = directory, sessions = sessions)
        application {
            module()
            duelSocket(deps)
        }
        val client = createClient { install(WebSockets) }

        withTimeout(5.seconds) {
            val first = client.webSocketSession("/ws")
            first.completeHandshake("d1")
            val second = client.webSocketSession("/ws")
            second.completeHandshake("d2")

            first.send(Frame.Text("{"))
            val reply = first.nextServerMessage() as ServerMessage.Failure
            assertEquals(ProtocolError.MALFORMED_MESSAGE, reply.error)

            assertNull(withTimeoutOrNull(200.milliseconds) { second.incoming.receive() })
            assertEquals(2, sessions.size)
        }
    }
}
