package duels.poker.server

import duels.poker.server.protocol.Hello
import duels.poker.server.protocol.ProtocolCodec
import duels.poker.server.protocol.ServerMessage
import duels.poker.server.protocol.protocolJson
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
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.decodeFromString
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.seconds

/** Reads the next frame off [this] session as a decoded [ServerMessage]. */
private suspend fun DefaultClientWebSocketSession.nextServerMessage(): ServerMessage {
    val frame = incoming.receive() as Frame.Text
    return protocolJson.decodeFromString(frame.readText())
}

/** Waits, without a fixed sleep, for [sessions] to reach [expected] entries. */
private suspend fun awaitSize(sessions: SessionRegistry, expected: Int) =
    withTimeout(5.seconds) {
        while (sessions.size != expected) delay(10)
    }

class DuelSocketSessionTest {
    @Test
    fun aWelcomedClientHoldsExactlyOneSession() = testApplication {
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
            session.send(Frame.Text(ProtocolCodec.encode(Hello(deviceId = "d1"))))
            session.nextServerMessage()

            // Registration precedes the Welcome reply, so no waiting is required here.
            assertEquals(1, sessions.size)
        }
    }

    @Test
    fun theSessionCarriesTheResolvedPlayer() = testApplication {
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
            session.send(Frame.Text(ProtocolCodec.encode(Hello(deviceId = "d1"))))
            session.nextServerMessage()

            val player = directory.resolve(DeviceId("d1"))
            val registered = sessions.sessionsOf(player.id).single()

            assertEquals("d1", registered.player.deviceId.value)
            assertEquals(1, directory.profileCount)
        }
    }

    @Test
    fun reconnectingWithTheSameDeviceIdReusesTheProfile() = testApplication {
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
            first.send(Frame.Text(ProtocolCodec.encode(Hello(deviceId = null))))
            val welcome = first.nextServerMessage() as ServerMessage.Welcome

            val second = client.webSocketSession("/ws")
            second.send(Frame.Text(ProtocolCodec.encode(Hello(deviceId = welcome.deviceId))))
            val secondWelcome = second.nextServerMessage() as ServerMessage.Welcome

            assertEquals(welcome.deviceId, secondWelcome.deviceId)
            assertEquals(1, directory.profileCount)
        }
    }

    @Test
    fun aCleanCloseRemovesExactlyOneSession() = testApplication {
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
            first.send(Frame.Text(ProtocolCodec.encode(Hello(deviceId = "d1"))))
            first.nextServerMessage()

            val second = client.webSocketSession("/ws")
            second.send(Frame.Text(ProtocolCodec.encode(Hello(deviceId = "d2"))))
            second.nextServerMessage()

            assertEquals(2, sessions.size)

            first.close()
            awaitSize(sessions, 1)

            val survivor = directory.resolve(DeviceId("d2"))
            assertEquals(1, sessions.sessionsOf(survivor.id).size)
        }
    }

    @Test
    fun anAbruptCloseRemovesTheSession() = testApplication {
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
            session.send(Frame.Text(ProtocolCodec.encode(Hello(deviceId = "d1"))))
            session.nextServerMessage()

            assertEquals(1, sessions.size)

            session.cancel()

            awaitSize(sessions, 0)
        }
    }

    @Test
    fun theRegistryEntryIsRemovedOnlyOnce() = testApplication {
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
            session.send(Frame.Text(ProtocolCodec.encode(Hello(deviceId = "d1"))))
            session.nextServerMessage()

            val player = directory.resolve(DeviceId("d1"))
            val id = sessions.sessionsOf(player.id).single().id

            session.close()
            awaitSize(sessions, 0)

            assertNull(sessions.remove(id))
        }
    }

    @Test
    fun aRefusedHandshakeRegistersNoSession() = testApplication {
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
            session.send(Frame.Text("{"))
            session.nextServerMessage()
            session.closeReason.await()

            assertEquals(0, sessions.size)
            assertEquals(0, directory.profileCount)
        }
    }
}
