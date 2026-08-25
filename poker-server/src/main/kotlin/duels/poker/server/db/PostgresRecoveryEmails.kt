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
     * transaction: `DELETE` then `INSERT` on the same connection, committed together, so
     * `email_verification`'s `UNIQUE (player_id)` (`ADR-0031` §3) can never refuse the insert.
     *
     * Answers [ClaimPendingResult.Claimed] unconditionally — see the comment at the return site.
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
                    deletePendingClaim(connection, playerId)
                    insertPendingClaim(connection, playerId, address, token, issuedAt, expiresAt)
                    connection.commit()
                } catch (exception: SQLException) {
                    connection.rollback()
                    throw exception
                }
            }
            // TASK-041636 adds ADR-0031 §5's fifteen-minute resend suppression inside this same
            // transaction — the first change able to answer ClaimPendingResult.Suppressed. Until
            // it merges, this writes on every call and answers Claimed on every call.
            ClaimPendingResult.Claimed
        }

    override suspend fun verifyPending(token: VerificationToken): VerifyEmailResult =
        TODO("TASK-041609 implements verifyPending")

    override suspend fun hasRecoveryEmail(playerId: PlayerId): Boolean =
        TODO("TASK-041609 implements hasRecoveryEmail")

    override suspend fun verifiedOwnerOf(address: EmailAddress): PlayerId? =
        TODO("TASK-041610 implements verifiedOwnerOf")

    override suspend fun detach(playerId: PlayerId): Unit =
        TODO("TASK-041610 implements detach")

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

    private companion object {
        private val VERIFICATION_LIFETIME: Duration = Duration.ofHours(24)

        private const val DELETE_PENDING_SQL =
            "DELETE FROM email_verification WHERE player_id = ?"

        private const val INSERT_PENDING_SQL =
            "INSERT INTO email_verification (token_hash, player_id, address, issued_at, expires_at) " +
                "VALUES (?, ?, ?, ?, ?)"
    }
}
