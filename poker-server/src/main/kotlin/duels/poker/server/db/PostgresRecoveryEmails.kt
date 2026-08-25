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
 * [claimPending] (`TASK-041608`) and [verifyPending] (`TASK-041609`) are implemented. The other
 * four members throw [NotImplementedError] via `TODO()`, each naming the ticket that fills it
 * in — the Kotlin compiler will not accept a class that implements [RecoveryEmails] only partly,
 * the same shape [PostgresAuthSessions] used before its own `delete` landed.
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

    /**
     * Consumes [token] in one transaction: `DELETE … RETURNING` against `email_verification`,
     * then an `INSERT` into `recovery_email` stamped with [clock]'s current instant.
     *
     * The `DELETE` carries `expires_at > now()` in its `WHERE` clause (`ADR-0031` §3) — expiry is
     * enforced by the statement, never by reading the row and comparing in Kotlin, which would be
     * a read-then-write window. No row deleted — because the token is unknown, expired, or was
     * already consumed by an earlier call — commits the (empty) transaction and answers
     * [VerifyEmailResult.Refused]; the three causes are indistinguishable by construction, since
     * all three take this one branch.
     *
     * A `23505` from the `INSERT` — `recovery_email_address_unique` because somebody else verified
     * this address first, or `recovery_email_pkey` because this player already holds one — rolls
     * the transaction back and answers [VerifyEmailResult.AddressTaken]. The rollback is the
     * substance: without it the `DELETE` above still commits, burning the loser's token for a link
     * that no longer exists while leaving them without the address they were trying to prove.
     */
    override suspend fun verifyPending(token: VerificationToken): VerifyEmailResult =
        withContext(Dispatchers.IO) {
            dataSource.connection.use { connection ->
                connection.autoCommit = false
                try {
                    val pending = deletePendingVerification(connection, token)
                    if (pending == null) {
                        connection.commit()
                        VerifyEmailResult.Refused
                    } else {
                        insertRecoveryEmail(connection, pending, clock.instant())
                        connection.commit()
                        VerifyEmailResult.Verified
                    }
                } catch (exception: SQLException) {
                    connection.rollback()
                    if (exception.sqlState == ADDRESS_UNIQUE_VIOLATION_SQLSTATE) {
                        VerifyEmailResult.AddressTaken
                    } else {
                        throw exception
                    }
                }
            }
        }

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

    // The single DELETE ... RETURNING that both consumes the token and enforces expiry, so no
    // caller can observe the row and decide separately (ADR-0031 §3). No row back means the token
    // named nothing live — unknown, expired, or already consumed by an earlier call — and this
    // function cannot and does not distinguish which.
    private fun deletePendingVerification(connection: Connection, token: VerificationToken): PendingVerification? =
        connection.prepareStatement(DELETE_VERIFICATION_SQL).use { statement ->
            statement.setBytes(1, recoveryTokenDigest(token))
            statement.executeQuery().use { rows ->
                if (rows.next()) {
                    PendingVerification(
                        playerId = PlayerId(rows.getObject("player_id", UUID::class.java).toString()),
                        address = rows.getString("address"),
                    )
                } else {
                    null
                }
            }
        }

    private fun insertRecoveryEmail(connection: Connection, pending: PendingVerification, verifiedAt: Instant) {
        connection.prepareStatement(INSERT_RECOVERY_EMAIL_SQL).use { statement ->
            statement.setObject(1, UUID.fromString(pending.playerId.value))
            statement.setString(2, pending.address)
            statement.setObject(3, OffsetDateTime.ofInstant(verifiedAt, ZoneOffset.UTC))
            statement.executeUpdate()
        }
    }

    // Holds only what verifyPending needs between the DELETE and the INSERT. Never returned from
    // this class, never logged: RecoveryEmails' contract is that an address crosses this
    // package's boundary into nothing but RecoveryMailer.
    private class PendingVerification(val playerId: PlayerId, val address: String)

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

        private const val DELETE_VERIFICATION_SQL =
            "DELETE FROM email_verification WHERE token_hash = ? AND expires_at > now() " +
                "RETURNING player_id, address"

        private const val INSERT_RECOVERY_EMAIL_SQL =
            "INSERT INTO recovery_email (player_id, address, verified_at) VALUES (?, ?, ?)"

        // 23505 = unique_violation. recovery_email_address_unique (someone else verified this
        // address first) and recovery_email_pkey (this player already holds one) land here
        // undistinguished on purpose — ADR-0031 §5 gives the endpoint one 409 for both.
        private const val ADDRESS_UNIQUE_VIOLATION_SQLSTATE = "23505"
    }
}
