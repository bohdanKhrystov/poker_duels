package duels.poker.server.db

import duels.poker.server.auth.AuthSessions
import duels.poker.server.auth.SessionToken
import duels.poker.server.auth.SessionTokens
import duels.poker.server.session.PlayerId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.sql.Connection
import java.time.Clock
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit.DAYS
import java.util.UUID
import javax.sql.DataSource

/**
 * Implements [AuthSessions] against the `auth_session` table.
 *
 * [issue] mints a fresh [SessionToken] from [tokens], inserts one row keyed by the SHA-256 of that
 * token, and hands the plaintext token back to the caller — the only moment it is ever readable in
 * plaintext again. The digest is computed by a private function and is never returned, logged, or
 * put into an exception message.
 *
 * [playerOf] answers the player a live row names, and `null` both for a token with no row and for
 * one whose row has expired — the two are indistinguishable here, because telling them apart would
 * tell a caller which tokens once existed. Expiry is decided by the database's own `now()`,
 * compared against the same `TIMESTAMPTZ` scale the row was written on; the injected [clock] never
 * enters this read, it is only how a test manufactures an already-expired row to issue against.
 *
 * [clock] is a [Clock], never `ServerClock`: `ServerClock` reports elapsed milliseconds from an
 * arbitrary epoch, so a `TIMESTAMPTZ` stamped from it would land every row in 1970, and every
 * session would be dead the moment it was written (`ADR-0062`).
 *
 * [delete] is `TASK-040508`'s scope and throws until it lands — the Kotlin compiler will not accept
 * a class that implements [AuthSessions] only partly.
 */
public class PostgresAuthSessions(
    private val dataSource: DataSource,
    private val clock: Clock,
    private val tokens: SessionTokens = SessionTokens(),
) : AuthSessions {
    override suspend fun issue(playerId: PlayerId): SessionToken =
        withContext(Dispatchers.IO) {
            val token = tokens.newToken()
            val issuedAt = clock.instant()
            val expiresAt = issuedAt.plus(SESSION_LIFETIME_DAYS, DAYS)
            dataSource.connection.use { connection ->
                insertAuthSession(connection, token, playerId, issuedAt, expiresAt)
            }
            token
        }

    override suspend fun playerOf(token: SessionToken): PlayerId? =
        withContext(Dispatchers.IO) {
            dataSource.connection.use { connection ->
                connection.prepareStatement(PLAYER_OF_SQL).use { statement ->
                    statement.setBytes(1, sessionTokenDigest(token))
                    statement.executeQuery().use { rows ->
                        if (rows.next()) PlayerId(rows.getObject(1, UUID::class.java).toString()) else null
                    }
                }
            }
        }

    override suspend fun delete(token: SessionToken): Unit =
        withContext(Dispatchers.IO) {
            dataSource.connection.use { connection ->
                connection.prepareStatement(DELETE_SQL).use { statement ->
                    statement.setBytes(1, sessionTokenDigest(token))
                    statement.executeUpdate()
                }
            }
        }

    private fun insertAuthSession(
        connection: Connection,
        token: SessionToken,
        playerId: PlayerId,
        issuedAt: Instant,
        expiresAt: Instant,
    ) {
        connection.prepareStatement(ISSUE_SQL).use { statement ->
            statement.setBytes(1, sessionTokenDigest(token))
            statement.setObject(2, UUID.fromString(playerId.value))
            statement.setObject(3, OffsetDateTime.ofInstant(issuedAt, ZoneOffset.UTC))
            statement.setObject(4, OffsetDateTime.ofInstant(expiresAt, ZoneOffset.UTC))
            statement.executeUpdate()
        }
    }

    private companion object {
        private const val SESSION_LIFETIME_DAYS = 30L

        private const val ISSUE_SQL =
            "INSERT INTO auth_session (token_hash, player_id, issued_at, expires_at) VALUES (?, ?, ?, ?)"

        private const val PLAYER_OF_SQL =
            "SELECT player_id FROM auth_session WHERE token_hash = ? AND expires_at > now()"

        private const val DELETE_SQL =
            "DELETE FROM auth_session WHERE token_hash = ?"
    }
}
