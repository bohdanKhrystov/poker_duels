package duels.poker.server

import duels.poker.engine.game.PlayerAction
import duels.poker.server.protocol.Act
import duels.poker.server.protocol.Hello
import duels.poker.server.protocol.PROTOCOL_VERSION
import duels.poker.server.protocol.ProtocolCodec
import duels.poker.server.protocol.ProtocolError
import duels.poker.server.protocol.ServerMessage
import duels.poker.server.protocol.protocolJson
import duels.poker.server.session.fixedDeviceIds
import duels.poker.server.session.testDeps
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.server.testing.testApplication
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.decodeFromString
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.seconds

/** Reads the next frame off [this] session as a decoded [ServerMessage]. */
private suspend fun DefaultClientWebSocketSession.nextServerMessage(): ServerMessage {
    val frame = incoming.receive() as Frame.Text
    return protocolJson.decodeFromString(frame.readText())
}

class DuelSocketHandshakeTest {
    @Test
    fun aHelloWithNoDeviceIdIsWelcomedWithAnIssuedId() = testApplication {
        application {
            module()
            duelSocket(testDeps(deviceIds = fixedDeviceIds("issued-1")))
        }
        val client = createClient { install(WebSockets) }

        withTimeout(5.seconds) {
            val session = client.webSocketSession("/ws")
            session.send(Frame.Text(ProtocolCodec.encode(Hello(deviceId = null))))

            assertEquals(ServerMessage.Welcome("issued-1", PROTOCOL_VERSION), session.nextServerMessage())
            assertFalse(session.closeReason.isCompleted)
        }
    }

    @Test
    fun aHelloWithADeviceIdIsWelcomedWithThatId() = testApplication {
        application {
            module()
            duelSocket(testDeps(deviceIds = fixedDeviceIds()))
        }
        val client = createClient { install(WebSockets) }

        withTimeout(5.seconds) {
            val session = client.webSocketSession("/ws")
            session.send(Frame.Text(ProtocolCodec.encode(Hello(deviceId = "d1"))))

            assertEquals(ServerMessage.Welcome("d1", PROTOCOL_VERSION), session.nextServerMessage())
            assertFalse(session.closeReason.isCompleted)
        }
    }

    @Test
    fun aFirstFrameThatIsNotHelloIsRefused() = testApplication {
        application {
            module()
            duelSocket(testDeps())
        }
        val client = createClient { install(WebSockets) }

        withTimeout(5.seconds) {
            val session = client.webSocketSession("/ws")
            session.send(Frame.Text(ProtocolCodec.encode(Act(1, 0, PlayerAction.Fold(0)))))

            assertEquals(
                ServerMessage.Failure(ProtocolError.MALFORMED_MESSAGE),
                session.nextServerMessage(),
            )
            assertEquals(HANDSHAKE_REQUIRED, session.closeReason.await()?.message)
        }
    }

    @Test
    fun aMalformedFirstFrameIsRefused() = testApplication {
        application {
            module()
            duelSocket(testDeps())
        }
        val client = createClient { install(WebSockets) }

        withTimeout(5.seconds) {
            val session = client.webSocketSession("/ws")
            session.send(Frame.Text("{"))

            assertEquals(
                ServerMessage.Failure(ProtocolError.MALFORMED_MESSAGE),
                session.nextServerMessage(),
            )
            assertEquals(HANDSHAKE_REQUIRED, session.closeReason.await()?.message)
        }
    }

    @Test
    fun aBinaryFirstFrameIsRefusedWithNoFailureBody() = testApplication {
        application {
            module()
            duelSocket(testDeps())
        }
        val client = createClient { install(WebSockets) }

        withTimeout(5.seconds) {
            val session = client.webSocketSession("/ws")
            session.send(Frame.Binary(true, byteArrayOf(1, 2, 3)))

            assertTrue(session.incoming.receiveCatching().isClosed)
            assertEquals(HANDSHAKE_REQUIRED, session.closeReason.await()?.message)
        }
    }

    @Test
    fun aVersionMismatchIsRefused() = testApplication {
        application {
            module()
            duelSocket(testDeps())
        }
        val client = createClient { install(WebSockets) }

        withTimeout(5.seconds) {
            val session = client.webSocketSession("/ws")
            session.send(
                Frame.Text(
                    ProtocolCodec.encode(Hello(deviceId = null, protocolVersion = PROTOCOL_VERSION + 1)),
                ),
            )

            assertEquals(
                ServerMessage.Failure(ProtocolError.VERSION_MISMATCH),
                session.nextServerMessage(),
            )
            assertEquals(PROTOCOL_VERSION_MISMATCH, session.closeReason.await()?.message)
        }
    }

    @Test
    fun aRefusedHandshakeClosesWithViolatedPolicy() = testApplication {
        application {
            module()
            duelSocket(testDeps())
        }
        val client = createClient { install(WebSockets) }

        withTimeout(5.seconds) {
            val session = client.webSocketSession("/ws")
            session.send(Frame.Text("{"))

            assertEquals(
                ServerMessage.Failure(ProtocolError.MALFORMED_MESSAGE),
                session.nextServerMessage(),
            )
            assertEquals(CloseReason.Codes.VIOLATED_POLICY.code, session.closeReason.await()?.code)
        }
    }
}
