package duels.poker.server

import duels.poker.server.protocol.Hello
import duels.poker.server.protocol.ProtocolCodec
import duels.poker.server.protocol.ServerMessage
import duels.poker.server.protocol.protocolJson
import duels.poker.server.session.ConnectionDirectory
import duels.poker.server.session.DeviceId
import duels.poker.server.session.InMemoryPlayerDirectory
import duels.poker.server.session.SessionRegistry
import duels.poker.server.session.testDeps
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.server.testing.testApplication
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.decodeFromString
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.seconds

/** Reads the next frame off [this] session as a decoded [ServerMessage]. */
private suspend fun DefaultClientWebSocketSession.nextServerMessage(): ServerMessage {
    val frame = incoming.receive() as Frame.Text
    return protocolJson.decodeFromString(frame.readText())
}

/** Waits, without a fixed sleep, for [connections] to reach [expected] entries. */
private suspend fun awaitSize(connections: ConnectionDirectory, expected: Int) =
    withTimeout(5.seconds) {
        while (connections.size != expected) delay(10)
    }

/** Waits, without a fixed sleep, for [sessions] to reach [expected] entries. */
private suspend fun awaitSize(sessions: SessionRegistry, expected: Int) =
    withTimeout(5.seconds) {
        while (sessions.size != expected) delay(10)
    }

/**
 * `deps.connections` is what lets one connection's coroutine find the *other* seat's writer
 * without either one touching the other's socket directly — see [ConnectionDirectory]. These
 * tests prove only that a live connection's writer is registered under its player and gone once
 * the connection no longer is; sending a frame through a found writer is `TASK-020730`.
 */
class DuelSocketWriterDirectoryTest {
    @Test
    fun aliveConnectionIsFindableByItsPlayer() = testApplication {
        val directory = InMemoryPlayerDirectory()
        val connections = ConnectionDirectory()
        val deps = testDeps(directory = directory, connections = connections)
        application {
            module()
            duelSocket(deps)
        }
        val client = createClient { install(WebSockets) }

        withTimeout(5.seconds) {
            val session = client.webSocketSession("/ws")
            session.send(Frame.Text(ProtocolCodec.encode(Hello(deviceId = "d1"))))
            session.nextServerMessage()

            val player = directory.resolve(DeviceId("d1"))

            // Registration precedes the Welcome reply, so no waiting is required here.
            assertNotNull(connections.writerFor(player.id))
            assertEquals(1, connections.size)
        }
    }

    @Test
    fun aconnectionThatNeverHandshakesRegistersNothing() = testApplication {
        val connections = ConnectionDirectory()
        val deps = testDeps(connections = connections)
        application {
            module()
            duelSocket(deps)
        }
        val client = createClient { install(WebSockets) }

        withTimeout(5.seconds) {
            val session = client.webSocketSession("/ws")
            session.send(Frame.Text("{"))
            session.nextServerMessage()
            session.closeReason.await()

            assertEquals(0, connections.size)
        }
    }

    @Test
    fun aclosedConnectionLeavesNoWriterBehind() = testApplication {
        val directory = InMemoryPlayerDirectory()
        val connections = ConnectionDirectory()
        val deps = testDeps(directory = directory, connections = connections)
        application {
            module()
            duelSocket(deps)
        }
        val client = createClient { install(WebSockets) }

        withTimeout(5.seconds) {
            val session = client.webSocketSession("/ws")
            session.send(Frame.Text(ProtocolCodec.encode(Hello(deviceId = "d1"))))
            session.nextServerMessage()

            val player = directory.resolve(DeviceId("d1"))
            assertNotNull(connections.writerFor(player.id))

            session.close()
            awaitSize(connections, 0)

            assertNull(connections.writerFor(player.id))
        }
    }

    /**
     * `ADR-0018`: a newer socket for the same device adopts the seat and the older one closes
     * afterwards. The older connection's cleanup must remove nothing from [connections] — that is
     * exactly what [ConnectionDirectory.forget]'s two-argument shape exists to guarantee.
     */
    @Test
    fun theadoptedSocketsCloseDoesNotEvictTheNewOne() = testApplication {
        val directory = InMemoryPlayerDirectory()
        val sessions = SessionRegistry()
        val connections = ConnectionDirectory()
        val deps = testDeps(directory = directory, sessions = sessions, connections = connections)
        application {
            module()
            duelSocket(deps)
        }
        val client = createClient { install(WebSockets) }

        withTimeout(5.seconds) {
            val first = client.webSocketSession("/ws")
            first.send(Frame.Text(ProtocolCodec.encode(Hello(deviceId = "d1"))))
            first.nextServerMessage()

            val second = client.webSocketSession("/ws")
            second.send(Frame.Text(ProtocolCodec.encode(Hello(deviceId = "d1"))))
            second.nextServerMessage()

            // sessions.remove and connections.forget for the evicted first connection are
            // sequential statements in the same finally block, so once the eviction has dropped
            // the session count back to one, the older writer's forget attempt has already run.
            awaitSize(sessions, 1)

            val player = directory.resolve(DeviceId("d1"))
            assertNotNull(connections.writerFor(player.id))
            assertEquals(1, connections.size)
        }
    }
}
