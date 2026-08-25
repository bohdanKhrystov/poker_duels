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
 * [issue] and [consume] each run as one transaction on one connection with `autoCommit = false`,
 * so a reset that succeeds can never leave the attacker's session running.
 *
 * [tokens] remains unused by both — [issue] receives its token already minted by the caller, and
 * [consume] only ever hashes a token it was handed, never mints one — and stays a constructor
 * parameter regardless, so a future ticket that does need to mint from here need not widen this
 * constructor.
 *
 * [clock] is a [Clock], never `ServerClock`, for the reason `PostgresRecoveryEmails` documents:
 * `ServerClock` reports elapsed nanoseconds from an arbitrary epoch, so a `TIMESTAMPTZ` stamped
 * from it would land every row near 1970 (`ADR-0062` §5). [consume]'s own expiry check never
 * reads [clock] at all — `expires_at > now()` is Postgres' own clock, the same way
 * `PostgresAuthSessions.playerOf` checks `auth_session`. The row being checked was already
 * stamped from [clock] at `issue` time; comparing it against anything but the database's own
 * notion of "now" would let the two diverge.
 */
internal class PostgresPasswordResets(
    private val dataSource: DataSource,
    private val clock: Clock,
    @Suppress("unused") private val tokens: RecoveryTokens,
) : PasswordResets {
    // Same hasher, same parameters as PostgresCredentials.create (ADR-0031 §4, ADR-0054): this is
    // the identical no-arg construction PostgresCredentials' public constructor defaults to, not
    // a second Argon2id path or a different parameter set, which ADR-0031 §4 rules out.
    private val hasher: SecretHasher = Argon2Hasher()

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

    /**
     * Spends [token] and rewrites the player's password credential to [secret] — one transaction
     * on one connection, so a reset that succeeds cannot leave a stolen session alive.
     *
     * Step 1 is `DELETE … RETURNING`, the single statement `ADR-0031` §4 requires: the only read
     * of `password_reset` this method performs is the one the delete itself does, so no
     * read-then-write window exists in which two concurrent calls could both find the same token
     * live. No row back — the token is unknown, already spent, or past its hour — commits the
     * (empty) transaction and answers `false`.
     *
     * Step 2 hashes [secret] through [hasher], the same hasher and parameters
     * `PostgresCredentials.create` uses. Anything but exactly one row rewritten rolls the whole
     * transaction back — undoing the token delete too — and answers `false`: a player who has
     * somehow lost their `password` credential must not have their token spent and their sessions
     * destroyed for a write that never happened.
     *
     * Step 3 deletes every `auth_session` row for that player, unconditionally but for
     * `player_id`, served by the existing `auth_session_player_id_idx` — including the session
     * used to request the reset, because the endpoint that calls this requires none.
     */
    override suspend fun consume(token: ResetToken, secret: PresentedSecret): Boolean =
        withContext(Dispatchers.IO) {
            dataSource.connection.use { connection ->
                connection.autoCommit = false
                try {
                    val playerId = deleteLiveToken(connection, token)
                    if (playerId == null) {
                        connection.commit()
                        false
                    } else {
                        val secretHash = hasher.hash(secret)
                        if (rewriteCredential(connection, playerId, secretHash) != 1) {
                            connection.rollback()
                            false
                        } else {
                            deleteSessions(connection, playerId)
                            connection.commit()
                            true
                        }
                    }
                } catch (exception: SQLException) {
                    connection.rollback()
                    throw exception
                }
            }
        }

    // The single DELETE ... RETURNING that both consumes the token and enforces expiry
    // (ADR-0031 §4), so no caller can observe the row and decide separately. No row back means
    // the token named nothing live -- unknown, expired, or already consumed by an earlier call --
    // and this function cannot and does not distinguish which.
    private fun deleteLiveToken(connection: Connection, token: ResetToken): PlayerId? =
        connection.prepareStatement(DELETE_LIVE_TOKEN_SQL).use { statement ->
            statement.setBytes(1, recoveryTokenDigest(token))
            statement.executeQuery().use { rows ->
                if (rows.next()) PlayerId(rows.getObject("player_id", UUID::class.java).toString()) else null
            }
        }

    private fun rewriteCredential(connection: Connection, playerId: PlayerId, secretHash: String): Int =
        connection.prepareStatement(REWRITE_CREDENTIAL_SQL).use { statement ->
            statement.setString(1, secretHash)
            statement.setObject(2, UUID.fromString(playerId.value))
            statement.executeUpdate()
        }

    private fun deleteSessions(connection: Connection, playerId: PlayerId) {
        connection.prepareStatement(DELETE_SESSIONS_SQL).use { statement ->
            statement.setObject(1, UUID.fromString(playerId.value))
            statement.executeUpdate()
        }
    }

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

        private const val DELETE_LIVE_TOKEN_SQL =
            "DELETE FROM password_reset WHERE token_hash = ? AND expires_at > now() " +
                "RETURNING player_id"

        private const val REWRITE_CREDENTIAL_SQL =
            "UPDATE credential SET secret_hash = ? WHERE player_id = ? AND kind = 'password'"

        private const val DELETE_SESSIONS_SQL =
            "DELETE FROM auth_session WHERE player_id = ?"
    }
}
