package duels.poker.server.http

import duels.poker.server.auth.AuthSessions
import duels.poker.server.auth.IdentityResolver
import duels.poker.server.auth.SessionToken
import duels.poker.server.db.PostgresAuthSessions
import duels.poker.server.db.PostgresPlayerDirectory
import duels.poker.server.protocol.http.ProfileResponse
import duels.poker.server.session.DeviceId
import duels.poker.server.session.Player
import duels.poker.server.session.PlayerDirectory
import duels.poker.server.session.PlayerId
import java.time.Clock
import javax.sql.DataSource

/**
 * A [PlayerDirectory] fixed to [profiles]' own device id -> player edges — the same map a
 * `ProfileReads` double (`FixedProfileReads`, `FakeProfileReads`, `RecordingProfileReads`, ...) is
 * built from, so a route test names its fixture once, in [identitiesFor], rather than twice.
 *
 * `resolve` mints; an HTTP route never does (`ADR-0012`), so nothing exercised through
 * [identitiesFor] should ever reach it — it throws rather than silently minting a player a wrong
 * wiring did not ask for.
 */
internal class FixedDirectory(private val profiles: Map<String, ProfileResponse>) : PlayerDirectory {
    override suspend fun resolve(deviceId: DeviceId): Player {
        throw UnsupportedOperationException("a route resolves via findOrNull; an HTTP route never mints")
    }

    override suspend fun findOrNull(deviceId: DeviceId): Player? =
        profiles[deviceId.value]?.let { Player(PlayerId(it.playerId), deviceId) }
}

/**
 * An [AuthSessions] that resolves no token, ever. No route test built on [identitiesFor] issues a
 * session, so every token presented to it — well-formed or malformed — must fail exactly the way
 * an unknown one does (`ADR-0027` §4): `Identity.Refused`, never a fall back to the device beside
 * it.
 */
internal object NoAuthSessions : AuthSessions {
    override suspend fun issue(playerId: PlayerId): SessionToken {
        throw UnsupportedOperationException("no route test built on identitiesFor issues a session")
    }

    override suspend fun playerOf(token: SessionToken): PlayerId? = null

    override suspend fun delete(token: SessionToken) {
        throw UnsupportedOperationException("no route test built on identitiesFor revokes a session")
    }
}

/**
 * Builds the [IdentityResolver] that pairs with a `ProfileReads` double built from the same
 * [profiles] map: both walk the identical device id -> player id edges, so a route test's fixture
 * names one map, not two.
 */
internal fun identitiesFor(profiles: Map<String, ProfileResponse>): IdentityResolver =
    IdentityResolver(NoAuthSessions, FixedDirectory(profiles))

/**
 * Builds the [IdentityResolver] a database-backed route test needs to install `profileRoutes`,
 * `authRoutes` or `standingsRoutes` against a real [dataSource] — the same kind of
 * [PostgresPlayerDirectory] `serverComponents` wires in production. None of these tests signs in,
 * so [AuthSessions.issue] is never called and the session clock is never read; `Clock.systemUTC()`
 * is used only because [PostgresAuthSessions] requires one.
 */
internal fun identitiesFor(dataSource: DataSource): IdentityResolver =
    IdentityResolver(PostgresAuthSessions(dataSource, Clock.systemUTC()), PostgresPlayerDirectory(dataSource))
