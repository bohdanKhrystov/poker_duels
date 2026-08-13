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
import io.ktor.websocket.readText
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.decodeFromString
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
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

/**
 * `ADR-0018` (resolving `DEC-011`): when a device id that already holds a live session opens a
 * second socket, the new socket adopts the seat and the first is closed.
 */
class DuelSocketSecondSocketTest {
    @Test
    fun aSecondSocketForTheSameDeviceIsWelcomed() = testApplication {
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
            second.send(Frame.Text(ProtocolCodec.encode(Hello(deviceId = "d1"))))
            val welcome = second.nextServerMessage() as ServerMessage.Welcome

            assertEquals("d1", welcome.deviceId)
        }
    }

    @Test
    fun theFirstSocketIsClosedWithSeatAdopted() = testApplication {
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
            second.send(Frame.Text(ProtocolCodec.encode(Hello(deviceId = "d1"))))
            second.nextServerMessage()

            val closeReason = first.closeReason.await()

            assertEquals(SEAT_ADOPTED, closeReason?.message)
        }
    }

    @Test
    fun adoptionLeavesExactlyOneSessionForThePlayer() = testApplication {
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

            val player = directory.resolve(DeviceId("d1"))
            val firstSessionId = sessions.sessionsOf(player.id).single().id

            val second = client.webSocketSession("/ws")
            second.send(Frame.Text(ProtocolCodec.encode(Hello(deviceId = "d1"))))
            second.nextServerMessage()

            awaitSize(sessions, 1)

            val survivors = sessions.sessionsOf(player.id)
            assertEquals(1, survivors.size)
            assertNotEquals(firstSessionId, survivors.single().id)
        }
    }

    @Test
    fun manySocketsAdoptingConcurrentlyLeaveExactlyOneSurvivor() = testApplication {
        val directory = InMemoryPlayerDirectory()
        val sessions = SessionRegistry()
        val deps = testDeps(directory = directory, sessions = sessions)
        application {
            module()
            duelSocket(deps)
        }
        val client = createClient { install(WebSockets) }

        withTimeout(5.seconds) {
            // Every one of these gets Welcomed independently — adoption decides who survives
            // afterwards, not who is let in. If the evict-then-register step in `SeatOwnership`
            // were not atomic, two of these racing to adopt at once could both survive.
            val opens =
                (0 until 8).map {
                    async {
                        val socket = client.webSocketSession("/ws")
                        socket.send(Frame.Text(ProtocolCodec.encode(Hello(deviceId = "d1"))))
                        socket.nextServerMessage()
                    }
                }
            opens.awaitAll()

            awaitSize(sessions, 1)

            val player = directory.resolve(DeviceId("d1"))
            assertEquals(1, sessions.sessionsOf(player.id).size)
        }
    }
}
