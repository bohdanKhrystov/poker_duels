package duels.poker.server.db

import duels.poker.server.auth.ClaimPendingResult
import duels.poker.server.auth.EmailAddress
import duels.poker.server.auth.RecoveryEmails
import duels.poker.server.auth.VerificationToken
import duels.poker.server.auth.VerifyEmailResult
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
 * Implements [RecoveryEmails] against the `email_verification` and `recovery_email` tables
 * (`ADR-0031` §2, §3).
 *
 * This ticket (`TASK-041608`) implements only [claimPending]. The other five members throw
 * [NotImplementedError] via `TODO()`, each naming the ticket that fills it in — the Kotlin
 * compiler will not accept a class that implements [RecoveryEmails] only partly, the same shape
 * [PostgresAuthSessions] used before its own `delete` landed.
 *
 * [clock] is a [Clock], never `ServerClock`: `ServerClock` reports elapsed nanoseconds from an
 * arbitrary epoch, so a `TIMESTAMPTZ` stamped from it would land every row near 1970
 * (`ADR-0062` §5, amending `ADR-0031` §3's clause for exactly this reason).
 */
internal class PostgresRecoveryEmails(
    private val dataSource: DataSource,
    private val clock: Clock,
) : RecoveryEmails {
    /**
     * Replaces whatever pending claim [playerId] already holds with a fresh one, in one
     * transaction: a `SELECT` of the outstanding row's `issued_at`, then — unless suppressed —
     * `DELETE` then `INSERT`, all on the same connection and committed together, so
     * `email_verification`'s `UNIQUE (player_id)` (`ADR-0031` §3) can never refuse the insert.
     *
     * A row issued less than fifteen minutes before [clock]'s current instant makes the call a
     * no-op: the transaction commits having run neither the `DELETE` nor the `INSERT`, and answers
     * [ClaimPendingResult.Suppressed] (`ADR-0031` §5's resend-suppression budget). The `SELECT`
     * shares the write's connection and transaction on purpose — a pre-check on a separate
     * connection is a read-then-write window in which two concurrent attaches both find no live
     * row, both write, and both mail, which is exactly what this rule caps (`ADR-0079` §2).
     *
     * Otherwise answers [ClaimPendingResult.Claimed], as before.
     */
    override suspend fun claimPending(
        playerId: PlayerId,
        address: EmailAddress,
        token: VerificationToken,
    ): ClaimPendingResult =
        withContext(Dispatchers.IO) {
            val issuedAt = clock.instant()
            val expiresAt = issuedAt.plus(VERIFICATION_LIFETIME)
            dataSource.connection.use { connection ->
                connection.autoCommit = false
                try {
                    val liveSince = selectPendingClaimIssuedAt(connection, playerId)
                    if (liveSince != null && Duration.between(liveSince, issuedAt) < RESEND_WINDOW) {
                        // Inside the window: commit the (empty) transaction and leave the
                        // outstanding row exactly as it was — same token_hash, same address, same
                        // issued_at, same expires_at (ADR-0031 §5).
                        connection.commit()
                        ClaimPendingResult.Suppressed
                    } else {
                        deletePendingClaim(connection, playerId)
                        insertPendingClaim(connection, playerId, address, token, issuedAt, expiresAt)
                        connection.commit()
                        ClaimPendingResult.Claimed
                    }
                } catch (exception: SQLException) {
                    connection.rollback()
                    throw exception
                }
            }
        }

    override suspend fun verifyPending(token: VerificationToken): VerifyEmailResult =
        TODO("TASK-041609 implements verifyPending")

    override suspend fun hasRecoveryEmail(playerId: PlayerId): Boolean =
        TODO("TASK-041610 implements hasRecoveryEmail")

    override suspend fun verifiedOwnerOf(address: EmailAddress): PlayerId? =
        TODO("TASK-041610 implements verifiedOwnerOf")

    override suspend fun detach(playerId: PlayerId): Unit =
        TODO("TASK-041611 implements detach")

    override suspend fun deleteExpiredVerifications(): Int =
        TODO("TASK-041611 implements deleteExpiredVerifications")

    private fun deletePendingClaim(connection: Connection, playerId: PlayerId) {
        connection.prepareStatement(DELETE_PENDING_SQL).use { statement ->
            statement.setObject(1, UUID.fromString(playerId.value))
            statement.executeUpdate()
        }
    }

    private fun insertPendingClaim(
        connection: Connection,
        playerId: PlayerId,
        address: EmailAddress,
        token: VerificationToken,
        issuedAt: Instant,
        expiresAt: Instant,
    ) {
        connection.prepareStatement(INSERT_PENDING_SQL).use { statement ->
            statement.setBytes(1, recoveryTokenDigest(token))
            statement.setObject(2, UUID.fromString(playerId.value))
            statement.setString(3, address.value)
            statement.setObject(4, OffsetDateTime.ofInstant(issuedAt, ZoneOffset.UTC))
            statement.setObject(5, OffsetDateTime.ofInstant(expiresAt, ZoneOffset.UTC))
            statement.executeUpdate()
        }
    }

    // issued_at, never expires_at: on this table they differ by twenty-four hours, so reading the
    // wrong column would turn a fifteen-minute silence into a day of one (ADR-0031 §5).
    private fun selectPendingClaimIssuedAt(connection: Connection, playerId: PlayerId): Instant? =
        connection.prepareStatement(SELECT_ISSUED_AT_SQL).use { statement ->
            statement.setObject(1, UUID.fromString(playerId.value))
            statement.executeQuery().use { rows ->
                if (rows.next()) rows.getObject("issued_at", OffsetDateTime::class.java).toInstant() else null
            }
        }

    private companion object {
        private val VERIFICATION_LIFETIME: Duration = Duration.ofHours(24)

        // ADR-0031 §5's resend-suppression window. Kept apart from VERIFICATION_LIFETIME
        // deliberately: one bounds how long a token stays usable, the other how often a mail may
        // be sent, and the file must never derive one duration from the other.
        private val RESEND_WINDOW: Duration = Duration.ofMinutes(15)

        private const val DELETE_PENDING_SQL =
            "DELETE FROM email_verification WHERE player_id = ?"

        private const val INSERT_PENDING_SQL =
            "INSERT INTO email_verification (token_hash, player_id, address, issued_at, expires_at) " +
                "VALUES (?, ?, ?, ?, ?)"

        private const val SELECT_ISSUED_AT_SQL =
            "SELECT issued_at FROM email_verification WHERE player_id = ?"
    }
}
