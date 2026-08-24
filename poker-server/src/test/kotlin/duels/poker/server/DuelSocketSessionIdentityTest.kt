package duels.poker.server

import duels.poker.server.auth.AuthSessions
import duels.poker.server.auth.IdentityResolver
import duels.poker.server.auth.SessionToken
import duels.poker.server.protocol.Hello
import duels.poker.server.protocol.PROTOCOL_VERSION
import duels.poker.server.protocol.ProtocolCodec
import duels.poker.server.protocol.ProtocolError
import duels.poker.server.protocol.ServerMessage
import duels.poker.server.protocol.protocolJson
import duels.poker.server.session.DeviceId
import duels.poker.server.session.InMemoryPlayerDirectory
import duels.poker.server.session.Player
import duels.poker.server.session.PlayerDirectory
import duels.poker.server.session.PlayerId
import duels.poker.server.session.fixedDeviceIds
import duels.poker.server.session.testDeps
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.server.testing.testApplication
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.decodeFromString
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.seconds

/** Reads the next frame off [this] session as a decoded [ServerMessage]. */
private suspend fun DefaultClientWebSocketSession.nextServerMessage(): ServerMessage {
    val frame = incoming.receive() as Frame.Text
    return protocolJson.decodeFromString(frame.readText())
}

/**
 * A double for [AuthSessions] that maps exactly one token to exactly one player, the way a real
 * store would for a session that was actually issued. Any other token — including one that merely
 * looks plausible — is unknown, and answered exactly like an expired one.
 */
private class FixedAuthSessions(
    private val knownToken: SessionToken,
    private val knownPlayerId: PlayerId,
) : AuthSessions {
    override suspend fun issue(playerId: PlayerId): SessionToken =
        throw UnsupportedOperationException("FixedAuthSessions does not mint tokens")

    override suspend fun playerOf(token: SessionToken): PlayerId? =
        if (token == knownToken) knownPlayerId else null

    override suspend fun delete(token: SessionToken) {
        // No-op; this double holds no tokens to delete.
    }
}

/**
 * A [PlayerDirectory] that delegates to [delegate] while recording every device id [resolve] and
 * [findOrNull] are called with — the only way to tell "ignored" from "looked up and discarded"
 * apart, which a return value alone cannot.
 */
private class RecordingPlayerDirectory(private val delegate: PlayerDirectory) : PlayerDirectory {
    val resolveCalls = mutableListOf<DeviceId>()
    val findOrNullCalls = mutableListOf<DeviceId>()

    override suspend fun resolve(deviceId: DeviceId): Player {
        resolveCalls += deviceId
        return delegate.resolve(deviceId)
    }

    override suspend fun findOrNull(deviceId: DeviceId): Player? {
        findOrNullCalls += deviceId
        return delegate.findOrNull(deviceId)
    }
}

/**
 * Proves `ADR-0027` §4's precedence at the socket's own handshake — the last of the three places
 * it has to hold. `IdentityResolverTest` proves the resolver itself never falls back;
 * `AuthRouteTest` and its kin prove it again over HTTP. `DuelSocket.serve` is the last caller.
 */
class DuelSocketSessionIdentityTest {
    @Test
    fun aTokenSeatsItsPlayerAndNamesNoDevice() = testApplication {
        val signedToken = SessionToken("t-signed")
        val signedPlayerId = PlayerId("p-signed")
        val directory = InMemoryPlayerDirectory()
        val resolver = IdentityResolver(FixedAuthSessions(signedToken, signedPlayerId), directory)
        application {
            module()
            duelSocket(testDeps(directory = directory, identities = resolver))
        }
        val client = createClient { install(WebSockets) }

        withTimeout(5.seconds) {
            val session = client.webSocketSession("/ws")
            session.send(Frame.Text(ProtocolCodec.encode(Hello(sessionToken = "t-signed"))))

            assertEquals(
                ServerMessage.Welcome("p-signed", null, PROTOCOL_VERSION),
                session.nextServerMessage(),
            )
            assertFalse(session.closeReason.isCompleted)
        }
    }

    @Test
    fun aTokenOutranksTheDeviceBesideIt() = testApplication {
        val signedToken = SessionToken("t-signed")
        val signedPlayerId = PlayerId("p-signed")
        val directory = InMemoryPlayerDirectory()
        // d-anon already owns a different profile: two players, two ids, so a leak of either
        // half of that profile through would be visible in the assertion below.
        val anonPlayer = directory.resolve(DeviceId("d-anon"))
        assertNotEquals(signedPlayerId, anonPlayer.id)
        val resolver = IdentityResolver(FixedAuthSessions(signedToken, signedPlayerId), directory)
        application {
            module()
            duelSocket(testDeps(directory = directory, identities = resolver))
        }
        val client = createClient { install(WebSockets) }

        withTimeout(5.seconds) {
            val session = client.webSocketSession("/ws")
            session.send(
                Frame.Text(ProtocolCodec.encode(Hello(deviceId = "d-anon", sessionToken = "t-signed"))),
            )

            assertEquals(
                ServerMessage.Welcome("p-signed", null, PROTOCOL_VERSION),
                session.nextServerMessage(),
            )
        }
    }

    @Test
    fun aTokenMeansTheDirectoryIsNotConsulted() = testApplication {
        val signedToken = SessionToken("t-signed")
        val signedPlayerId = PlayerId("p-signed")
        val delegate = InMemoryPlayerDirectory()
        val anonDeviceId = DeviceId("d-anon")
        // Seeded so it resolves: a fall back to the device here would visibly succeed, which is
        // exactly what must not happen.
        delegate.resolve(anonDeviceId)
        val directory = RecordingPlayerDirectory(delegate)
        val resolver = IdentityResolver(FixedAuthSessions(signedToken, signedPlayerId), directory)
        application {
            module()
            duelSocket(testDeps(directory = directory, identities = resolver))
        }
        val client = createClient { install(WebSockets) }

        withTimeout(5.seconds) {
            val session = client.webSocketSession("/ws")
            session.send(
                Frame.Text(ProtocolCodec.encode(Hello(deviceId = "d-anon", sessionToken = "t-signed"))),
            )
            session.nextServerMessage()
        }

        assertEquals(0, directory.resolveCalls.count { it == anonDeviceId })
        assertEquals(0, directory.findOrNullCalls.count { it == anonDeviceId })
    }

    @Test
    fun aTokenOnAFirstEverBrowserCreatesNoProfile() = testApplication {
        val signedToken = SessionToken("t-signed")
        val signedPlayerId = PlayerId("p-signed")
        val directory = InMemoryPlayerDirectory()
        directory.resolve(DeviceId("d-other")) // a baseline profile, so "unchanged" is not vacuous
        val seededProfileCount = directory.profileCount
        val resolver = IdentityResolver(FixedAuthSessions(signedToken, signedPlayerId), directory)
        application {
            module()
            duelSocket(testDeps(directory = directory, identities = resolver))
        }
        val client = createClient { install(WebSockets) }

        withTimeout(5.seconds) {
            val session = client.webSocketSession("/ws")
            session.send(
                Frame.Text(ProtocolCodec.encode(Hello(deviceId = null, sessionToken = "t-signed"))),
            )

            assertEquals(
                ServerMessage.Welcome("p-signed", null, PROTOCOL_VERSION),
                session.nextServerMessage(),
            )
        }
        assertEquals(seededProfileCount, directory.profileCount)
    }

    @Test
    fun anInvalidTokenIsRefusedNotDowngraded() = testApplication {
        val directory = InMemoryPlayerDirectory()
        // The device id must be one that *would* resolve, or this test cannot see a fallback.
        val anonPlayer = directory.resolve(DeviceId("d-anon"))
        val resolver = IdentityResolver(FixedAuthSessions(SessionToken("t-signed"), PlayerId("p-signed")), directory)
        application {
            module()
            duelSocket(testDeps(directory = directory, identities = resolver))
        }
        val client = createClient { install(WebSockets) }

        withTimeout(5.seconds) {
            val session = client.webSocketSession("/ws")
            session.send(
                Frame.Text(ProtocolCodec.encode(Hello(deviceId = "d-anon", sessionToken = "nonsense"))),
            )

            val response = session.nextServerMessage()
            assertEquals(ServerMessage.Failure(ProtocolError.INVALID_SESSION), response)
            assertNotEquals(ServerMessage.Welcome(anonPlayer.id.value, "d-anon", PROTOCOL_VERSION), response)
            assertEquals(INVALID_SESSION_PRESENTED, session.closeReason.await()?.message)
        }
    }

    @Test
    fun anInvalidTokenCreatesNoProfile() = testApplication {
        val directory = InMemoryPlayerDirectory()
        directory.resolve(DeviceId("d-anon"))
        val seededProfileCount = directory.profileCount
        val resolver = IdentityResolver(FixedAuthSessions(SessionToken("t-signed"), PlayerId("p-signed")), directory)
        application {
            module()
            duelSocket(testDeps(directory = directory, identities = resolver))
        }
        val client = createClient { install(WebSockets) }

        withTimeout(5.seconds) {
            val session = client.webSocketSession("/ws")
            session.send(
                Frame.Text(ProtocolCodec.encode(Hello(deviceId = "d-anon", sessionToken = "nonsense"))),
            )
            session.nextServerMessage()
        }

        assertEquals(seededProfileCount, directory.profileCount)
    }

    @Test
    fun noTokenWithAKnownDeviceIsUnchanged() = testApplication {
        val directory = InMemoryPlayerDirectory()
        val anonPlayer = directory.resolve(DeviceId("d-anon"))
        val resolver = IdentityResolver(FixedAuthSessions(SessionToken("t-signed"), PlayerId("p-signed")), directory)
        application {
            module()
            duelSocket(testDeps(directory = directory, identities = resolver))
        }
        val client = createClient { install(WebSockets) }

        withTimeout(5.seconds) {
            val session = client.webSocketSession("/ws")
            session.send(Frame.Text(ProtocolCodec.encode(Hello(deviceId = "d-anon"))))

            assertEquals(
                ServerMessage.Welcome(anonPlayer.id.value, "d-anon", PROTOCOL_VERSION),
                session.nextServerMessage(),
            )
        }
    }

    @Test
    fun noTokenAndNoDeviceStillMintsOne() = testApplication {
        val directory = InMemoryPlayerDirectory()
        val seededProfileCount = directory.profileCount
        val resolver = IdentityResolver(FixedAuthSessions(SessionToken("t-signed"), PlayerId("p-signed")), directory)
        application {
            module()
            duelSocket(
                testDeps(directory = directory, deviceIds = fixedDeviceIds("issued-1"), identities = resolver),
            )
        }
        val client = createClient { install(WebSockets) }

        withTimeout(5.seconds) {
            val session = client.webSocketSession("/ws")
            session.send(Frame.Text(ProtocolCodec.encode(Hello())))

            val welcome = session.nextServerMessage()
            assertTrue(welcome is ServerMessage.Welcome)
            if (welcome is ServerMessage.Welcome) {
                assertEquals("issued-1", welcome.deviceId)
                assertNotEquals("p-signed", welcome.playerId)
            }
        }
        assertEquals(seededProfileCount + 1, directory.profileCount)
    }
}
