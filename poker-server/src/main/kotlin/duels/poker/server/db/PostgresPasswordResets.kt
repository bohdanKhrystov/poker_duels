package duels.poker.server.db

import duels.poker.server.auth.PasswordResets
import duels.poker.server.auth.PresentedSecret
import duels.poker.server.auth.RecoveryTokens
import duels.poker.server.auth.ResetToken
import duels.poker.server.session.PlayerId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.sql.Connection
import java.sql.SQLException
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID
import javax.sql.DataSource

/**
 * Implements [PasswordResets] against the `password_reset` table (`ADR-0031` §4).
 *
 * [issue] is implemented; [consume] is `TODO()`, named for `TASK-041614`. [tokens] is unused by
 * [issue] — a fresh token always arrives as a parameter, already minted by the caller — and is
 * carried here now so `TASK-041614` need not widen this constructor later.
 *
 * [clock] is a [Clock], never `ServerClock`, for the reason `PostgresRecoveryEmails` documents:
 * `ServerClock` reports elapsed nanoseconds from an arbitrary epoch, so a `TIMESTAMPTZ` stamped
 * from it would land every row near 1970 (`ADR-0062` §5).
 */
internal class PostgresPasswordResets(
    private val dataSource: DataSource,
    private val clock: Clock,
    @Suppress("unused") private val tokens: RecoveryTokens,
) : PasswordResets {
    /**
     * Replaces whatever live reset token [playerId] already holds with [token], in one
     * transaction: a `SELECT` of the outstanding row's `issued_at`, then — unless suppressed —
     * `DELETE` then `INSERT`, all on the same connection and committed together, so
     * `password_reset`'s `UNIQUE (player_id)` (`ADR-0031` §4) can never refuse the insert.
     *
     * A row issued less than fifteen minutes before [clock]'s current instant makes the call a
     * no-op: the transaction commits having run neither the `DELETE` nor the `INSERT`, and
     * answers `false` (`ADR-0031` §5's resend-suppression budget). *"Crucially the outstanding
     * token is not invalidated, so a double-click does not destroy the link the player is about
     * to use."* The `SELECT` shares the write's connection and transaction on purpose — a
     * pre-check on a separate connection is a read-then-write window in which two concurrent
     * requests both find no live row, both write, and both mail, which is exactly what this rule
     * caps (`ADR-0079` §2).
     *
     * Otherwise deletes any existing row, inserts [token] with a one-hour expiry, and answers
     * `true`.
     */
    override suspend fun issue(playerId: PlayerId, token: ResetToken): Boolean =
        withContext(Dispatchers.IO) {
            val issuedAt = clock.instant()
            val expiresAt = issuedAt.plus(RESET_LIFETIME)
            dataSource.connection.use { connection ->
                connection.autoCommit = false
                try {
                    val liveSince = selectIssuedAt(connection, playerId)
                    if (liveSince != null && Duration.between(liveSince, issuedAt) < RESEND_WINDOW) {
                        // Inside the window: commit the (empty) transaction and leave the
                        // outstanding row exactly as it was — same token_hash, same issued_at,
                        // same expires_at (ADR-0031 §5).
                        connection.commit()
                        false
                    } else {
                        deleteExistingToken(connection, playerId)
                        insertToken(connection, playerId, token, issuedAt, expiresAt)
                        connection.commit()
                        true
                    }
                } catch (exception: SQLException) {
                    connection.rollback()
                    throw exception
                }
            }
        }

    override suspend fun consume(token: ResetToken, secret: PresentedSecret): Boolean = TODO("TASK-041614")

    // issued_at, never expires_at: the two differ by exactly RESET_LIFETIME, and reading the
    // wrong column would turn a fifteen-minute window into a forty-five-minute one (ADR-0031 §5).
    private fun selectIssuedAt(connection: Connection, playerId: PlayerId): Instant? =
        connection.prepareStatement(SELECT_ISSUED_AT_SQL).use { statement ->
            statement.setObject(1, UUID.fromString(playerId.value))
            statement.executeQuery().use { rows ->
                if (rows.next()) rows.getObject("issued_at", OffsetDateTime::class.java).toInstant() else null
            }
        }

    private fun deleteExistingToken(connection: Connection, playerId: PlayerId) {
        connection.prepareStatement(DELETE_TOKEN_SQL).use { statement ->
            statement.setObject(1, UUID.fromString(playerId.value))
            statement.executeUpdate()
        }
    }

    private fun insertToken(
        connection: Connection,
        playerId: PlayerId,
        token: ResetToken,
        issuedAt: Instant,
        expiresAt: Instant,
    ) {
        connection.prepareStatement(INSERT_TOKEN_SQL).use { statement ->
            statement.setBytes(1, recoveryTokenDigest(token))
            statement.setObject(2, UUID.fromString(playerId.value))
            statement.setObject(3, OffsetDateTime.ofInstant(issuedAt, ZoneOffset.UTC))
            statement.setObject(4, OffsetDateTime.ofInstant(expiresAt, ZoneOffset.UTC))
            statement.executeUpdate()
        }
    }

    private companion object {
        private val RESET_LIFETIME: Duration = Duration.ofHours(1)

        // ADR-0031 §5's resend-suppression window. Kept apart from RESET_LIFETIME deliberately:
        // one bounds how long a token stays usable, the other how often a mail may be sent, and
        // the file must never derive one duration from the other.
        private val RESEND_WINDOW: Duration = Duration.ofMinutes(15)

        private const val SELECT_ISSUED_AT_SQL =
            "SELECT issued_at FROM password_reset WHERE player_id = ?"

        private const val DELETE_TOKEN_SQL =
            "DELETE FROM password_reset WHERE player_id = ?"

        private const val INSERT_TOKEN_SQL =
            "INSERT INTO password_reset (token_hash, player_id, issued_at, expires_at) " +
                "VALUES (?, ?, ?, ?)"
    }
}
