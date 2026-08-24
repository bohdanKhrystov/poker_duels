package duels.poker.server.auth

import duels.poker.server.session.DeviceId
import duels.poker.server.session.InMemoryPlayerDirectory
import duels.poker.server.session.Player
import duels.poker.server.session.PlayerDirectory
import duels.poker.server.session.PlayerId
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

/**
 * A double for [AuthSessions] that maps exactly one token to exactly one player, the way a real
 * store would for a session that was actually issued. Any other token is unknown.
 */
private class FixedTokenAuthSessions(
    private val knownToken: SessionToken,
    private val knownPlayerId: PlayerId,
) : AuthSessions {
    override suspend fun issue(playerId: PlayerId): SessionToken =
        throw UnsupportedOperationException("FixedTokenAuthSessions does not mint tokens")

    override suspend fun playerOf(token: SessionToken): PlayerId? =
        if (token == knownToken) knownPlayerId else null

    override suspend fun delete(token: SessionToken) {
        // No-op; this double holds no tokens to delete.
    }
}

/**
 * A [PlayerDirectory] that delegates to [delegate] while counting calls to [findOrNull] — the
 * only way to tell "ignored" from "looked up and discarded" apart.
 */
private class CountingPlayerDirectory(private val delegate: PlayerDirectory) : PlayerDirectory {
    private var calls = 0

    val findOrNullCallCount: Int
        get() = calls

    override suspend fun resolve(deviceId: DeviceId) = delegate.resolve(deviceId)

    override suspend fun findOrNull(deviceId: DeviceId): Player? {
        calls += 1
        return delegate.findOrNull(deviceId)
    }
}

class IdentityResolverTest {
    @Test
    fun aValidSessionAnswersItsPlayer(): Unit = runBlocking {
        val token = SessionToken("session-token")
        val sessionPlayerId = PlayerId("p-session")
        val resolver = IdentityResolver(
            sessions = FixedTokenAuthSessions(token, sessionPlayerId),
            players = InMemoryPlayerDirectory(),
        )

        val identity = resolver.resolve(token, deviceId = null)

        assertEquals(Identity.Session(sessionPlayerId), identity)
    }

    @Test
    fun aValidSessionBeatsTheDeviceBesideIt(): Unit = runBlocking {
        val token = SessionToken("session-token")
        val sessionPlayerId = PlayerId("p-session")
        val directory = InMemoryPlayerDirectory()
        val deviceId = DeviceId("d-device")
        val devicePlayer = directory.resolve(deviceId)
        val resolver = IdentityResolver(
            sessions = FixedTokenAuthSessions(token, sessionPlayerId),
            players = directory,
        )
        // Two players, two ids: an implementation that let the device's player id leak through
        // would answer with devicePlayer.id here, which only differs from the expected answer
        // because the two are distinct.
        assertNotEquals(sessionPlayerId, devicePlayer.id)

        val identity = resolver.resolve(token, deviceId)

        assertEquals(Identity.Session(sessionPlayerId), identity)
    }

    @Test
    fun aValidSessionDoesNotEvenLookAtTheDevice(): Unit = runBlocking {
        val token = SessionToken("session-token")
        val sessionPlayerId = PlayerId("p-session")
        val directory = CountingPlayerDirectory(InMemoryPlayerDirectory())
        val resolver = IdentityResolver(
            sessions = FixedTokenAuthSessions(token, sessionPlayerId),
            players = directory,
        )

        resolver.resolve(token, DeviceId("d-present"))

        assertEquals(0, directory.findOrNullCallCount)
    }

    @Test
    fun anUnknownTokenIsRefusedAndNotDowngraded(): Unit = runBlocking {
        val knownToken = SessionToken("session-token")
        val sessionPlayerId = PlayerId("p-session")
        val forgedToken = SessionToken("forged-token")
        val directory = InMemoryPlayerDirectory()
        val deviceId = DeviceId("d-device")
        // Seeded so it resolves: a fall back to the device here would visibly succeed, which is
        // exactly what must not happen.
        directory.resolve(deviceId)
        val resolver = IdentityResolver(
            sessions = FixedTokenAuthSessions(knownToken, sessionPlayerId),
            players = directory,
        )

        val identity = resolver.resolve(forgedToken, deviceId)

        assertEquals(Identity.Refused, identity)
    }

    @Test
    fun aKnownDeviceWithNoTokenAnswersItsPlayer(): Unit = runBlocking {
        val directory = InMemoryPlayerDirectory()
        val d1 = DeviceId("d1")
        val d2 = DeviceId("d2")
        val p1 = directory.resolve(d1)
        val p2 = directory.resolve(d2)
        val resolver = IdentityResolver(sessions = NoAuthSessions, players = directory)

        val identity1 = resolver.resolve(token = null, deviceId = d1)
        val identity2 = resolver.resolve(token = null, deviceId = d2)

        assertEquals(Identity.Device(p1.id, d1), identity1)
        assertEquals(Identity.Device(p2.id, d2), identity2)
    }

    @Test
    fun anUnknownDeviceWithNoTokenIsUnknownNotAnonymous(): Unit = runBlocking {
        val resolver = IdentityResolver(sessions = NoAuthSessions, players = InMemoryPlayerDirectory())
        val ghost = DeviceId("ghost")

        val identity = resolver.resolve(token = null, deviceId = ghost)

        assertEquals(Identity.UnknownDevice(ghost), identity)
    }

    @Test
    fun neitherCredentialIsAnonymous(): Unit = runBlocking {
        val resolver = IdentityResolver(sessions = NoAuthSessions, players = InMemoryPlayerDirectory())

        val identity = resolver.resolve(token = null, deviceId = null)

        assertEquals(Identity.Anonymous, identity)
    }

    @Test
    fun resolvingCreatesNoProfile(): Unit = runBlocking {
        val directory = InMemoryPlayerDirectory()
        val knownDevice = DeviceId("d-known")
        directory.resolve(knownDevice)
        val seededProfileCount = directory.profileCount
        val token = SessionToken("session-token")
        val sessionPlayerId = PlayerId("p-session")
        val resolver = IdentityResolver(
            sessions = FixedTokenAuthSessions(token, sessionPlayerId),
            players = directory,
        )

        resolver.resolve(token, deviceId = null)
        resolver.resolve(token = null, deviceId = knownDevice)
        resolver.resolve(token = null, deviceId = DeviceId("d-ghost"))
        resolver.resolve(SessionToken("forged-token"), deviceId = knownDevice)
        resolver.resolve(token = null, deviceId = null)

        assertEquals(seededProfileCount, directory.profileCount)
    }
}
