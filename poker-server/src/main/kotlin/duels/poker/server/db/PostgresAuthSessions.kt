package duels.poker.server.db

import duels.poker.server.auth.AuthSessions
import duels.poker.server.auth.SessionToken
import duels.poker.server.auth.SessionTokens
import duels.poker.server.session.PlayerId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.charset.StandardCharsets.UTF_8
import java.security.MessageDigest
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
 * [issue] is the one member this ticket gives a real body: it mints a fresh [SessionToken] from
 * [tokens], inserts one row keyed by the SHA-256 of that token, and hands the plaintext token back
 * to the caller — the only moment it is ever readable in plaintext again. The digest is computed
 * by a private function and is never returned, logged, or put into an exception message.
 *
 * [clock] is a [Clock], never `ServerClock`: `ServerClock` reports elapsed milliseconds from an
 * arbitrary epoch, so a `TIMESTAMPTZ` stamped from it would land every row in 1970, and every
 * session would be dead the moment it was written (`ADR-0062`).
 *
 * [playerOf] and [delete] are `TASK-040507` and `TASK-040508`'s scope and throw until those land —
 * the Kotlin compiler will not accept a class that implements [AuthSessions] only partly.
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

    override suspend fun playerOf(token: SessionToken): PlayerId? = TODO("TASK-040507")

    override suspend fun delete(token: SessionToken) = TODO("TASK-040508")

    private fun insertAuthSession(
        connection: Connection,
        token: SessionToken,
        playerId: PlayerId,
        issuedAt: Instant,
        expiresAt: Instant,
    ) {
        connection.prepareStatement(ISSUE_SQL).use { statement ->
            statement.setBytes(1, tokenHash(token))
            statement.setObject(2, UUID.fromString(playerId.value))
            statement.setObject(3, OffsetDateTime.ofInstant(issuedAt, ZoneOffset.UTC))
            statement.setObject(4, OffsetDateTime.ofInstant(expiresAt, ZoneOffset.UTC))
            statement.executeUpdate()
        }
    }

    private fun tokenHash(token: SessionToken): ByteArray =
        MessageDigest.getInstance("SHA-256").digest(token.value.toByteArray(UTF_8))

    private companion object {
        private const val SESSION_LIFETIME_DAYS = 30L

        private const val ISSUE_SQL =
            "INSERT INTO auth_session (token_hash, player_id, issued_at, expires_at) VALUES (?, ?, ?, ?)"
    }
}
