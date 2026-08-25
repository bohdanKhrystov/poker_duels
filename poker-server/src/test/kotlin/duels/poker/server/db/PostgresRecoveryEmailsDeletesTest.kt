package duels.poker.server.db

import duels.poker.server.auth.EmailAddress
import duels.poker.server.auth.VerificationToken
import duels.poker.server.session.PlayerId
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import java.util.UUID
import javax.sql.DataSource
import kotlin.test.assertEquals

// Unlike PostgresRecoveryEmailsClaimTest's FIXED_INSTANT, this one cannot be an arbitrary
// historical literal: deleteExpiredVerifications's expiry is enforced by Postgres's own now()
// (ADR-0031 §3), never by comparing the injected Clock in Kotlin, so a "live" row needs an
// expires_at genuinely ahead of the database's real wall clock. Truncated to seconds so the round
// trip through a TIMESTAMPTZ column, which stores microseconds, can never disagree with the value
// asserted here.
private val FIXED_INSTANT: Instant = Instant.now().truncatedTo(ChronoUnit.SECONDS)

/**
 * Tests for [PostgresRecoveryEmails.detach] and [PostgresRecoveryEmails.deleteExpiredVerifications],
 * against the container.
 *
 * Reuses [PostgresRecoveryEmailsClaimTest]'s fixture shape — a fresh migrated database and a
 * mutable [Clock] a test can advance between two calls on the same [PostgresRecoveryEmails]
 * instance.
 */
class PostgresRecoveryEmailsDeletesTest {
    private lateinit var dataSource: DataSource
    private lateinit var clock: MutableClock
    private lateinit var recoveryEmails: PostgresRecoveryEmails

    @BeforeEach
    fun setupDatabase() {
        dataSource = PostgresTestSupport.freshDatabase()
        Migrations.migrate(dataSource)
        clock = MutableClock(FIXED_INSTANT)
        recoveryEmails = PostgresRecoveryEmails(dataSource, clock)
    }

    @Test
    fun detachingLeavesNoAddressBehind(): Unit = runBlocking {
        val playerId = insertPlayer()
        val token = VerificationToken("verify-token")
        recoveryEmails.claimPending(playerId, EmailAddress("bob@x.test"), token)
        recoveryEmails.verifyPending(token)

        recoveryEmails.detach(playerId)

        assertEquals(false, recoveryEmails.hasRecoveryEmail(playerId), "after detach, hasRecoveryEmail must be false")
        assertEquals(0, countRecoveryEmailRows(), "after detach, recovery_email must hold no rows")
    }

    @Test
    fun detachingOnePlayerLeavesTheOtherAlone(): Unit = runBlocking {
        val firstPlayer = insertPlayer()
        val secondPlayer = insertPlayer()
        val firstToken = VerificationToken("first-token")
        val secondToken = VerificationToken("second-token")
        recoveryEmails.claimPending(firstPlayer, EmailAddress("first@x.test"), firstToken)
        recoveryEmails.claimPending(secondPlayer, EmailAddress("second@x.test"), secondToken)
        recoveryEmails.verifyPending(firstToken)
        recoveryEmails.verifyPending(secondToken)

        recoveryEmails.detach(firstPlayer)

        assertEquals(false, recoveryEmails.hasRecoveryEmail(firstPlayer), "the detached player must read hasRecoveryEmail false")
        assertEquals(true, recoveryEmails.hasRecoveryEmail(secondPlayer), "the other player must still read hasRecoveryEmail true")
        assertEquals(1, countRecoveryEmailRows(), "exactly one row must remain")
        val secondRow = recoveryEmailRowFor(secondPlayer)
        assertEquals("second@x.test", secondRow?.address, "the surviving row must hold the second player's address unchanged")
    }

    @Test
    fun detachingNothingIsNotAnError(): Unit = runBlocking {
        val playerId = insertPlayer()

        // Should complete without throwing
        recoveryEmails.detach(playerId)

        assertEquals(false, recoveryEmails.hasRecoveryEmail(playerId), "detaching a player with no email must not error")
    }

    @Test
    fun detachingLeavesALivePendingClaimAlone(): Unit = runBlocking {
        val playerId = insertPlayer()
        val verifyToken = VerificationToken("verify-token")
        val claimToken = VerificationToken("claim-token")
        // Verify an address
        recoveryEmails.claimPending(playerId, EmailAddress("verified@x.test"), verifyToken)
        recoveryEmails.verifyPending(verifyToken)
        // Then claim a second, unverified address
        recoveryEmails.claimPending(playerId, EmailAddress("pending@x.test"), claimToken)

        recoveryEmails.detach(playerId)

        assertEquals(false, recoveryEmails.hasRecoveryEmail(playerId), "after detach, hasRecoveryEmail must be false")
        assertEquals(1, countPendingRowsFor(playerId), "the pending row must still exist in email_verification")
    }

    @Test
    fun theSweepTakesOnlyRowsPastTheirDay(): Unit = runBlocking {
        val stalePlayer = insertPlayer()
        val freshPlayer = insertPlayer()
        val staleToken = VerificationToken("stale-token")
        val freshToken = VerificationToken("fresh-token")

        // Claim the stale row 25 hours in the past
        val pastLifetime = Duration.ofHours(25)
        clock.advance(pastLifetime.negated())
        recoveryEmails.claimPending(stalePlayer, EmailAddress("stale@x.test"), staleToken)
        clock.advance(pastLifetime)

        // Claim the fresh row now (within 24 hours)
        recoveryEmails.claimPending(freshPlayer, EmailAddress("fresh@x.test"), freshToken)

        val deletedCount = recoveryEmails.deleteExpiredVerifications()

        assertEquals(1, deletedCount, "exactly one expired row must be deleted")
        assertEquals(0, countPendingRowsFor(stalePlayer), "the stale row must be gone")
        assertEquals(1, countPendingRowsFor(freshPlayer), "the fresh row must still exist")
    }

    @Test
    fun theSweepReportsZeroWhenNothingIsStale(): Unit = runBlocking {
        val freshPlayer = insertPlayer()
        val freshToken = VerificationToken("fresh-token")
        recoveryEmails.claimPending(freshPlayer, EmailAddress("fresh@x.test"), freshToken)

        val deletedCount = recoveryEmails.deleteExpiredVerifications()

        assertEquals(0, deletedCount, "when nothing is stale, zero rows must be deleted")
        assertEquals(1, countPendingRowsFor(freshPlayer), "the fresh row must still exist")
    }

    private fun insertPlayer(): PlayerId {
        val id = UUID.randomUUID()
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                "INSERT INTO player (id, coin_balance) VALUES (?, ?)",
            ).use { statement ->
                statement.setObject(1, id)
                statement.setInt(2, 100)
                statement.executeUpdate()
            }
        }
        return PlayerId(id.toString())
    }

    private fun countRecoveryEmailRows(): Int =
        dataSource.connection.use { connection ->
            connection.prepareStatement("SELECT count(*) FROM recovery_email").use { statement ->
                statement.executeQuery().use { rows ->
                    rows.next()
                    rows.getInt(1)
                }
            }
        }

    private fun countPendingRowsFor(playerId: PlayerId): Int =
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                "SELECT count(*) FROM email_verification WHERE player_id = ?",
            ).use { statement ->
                statement.setObject(1, UUID.fromString(playerId.value))
                statement.executeQuery().use { rows ->
                    rows.next()
                    rows.getInt(1)
                }
            }
        }

    private fun recoveryEmailRowFor(playerId: PlayerId): RecoveryEmailRow? =
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                "SELECT address FROM recovery_email WHERE player_id = ?",
            ).use { statement ->
                statement.setObject(1, UUID.fromString(playerId.value))
                statement.executeQuery().use { rows ->
                    if (rows.next()) {
                        RecoveryEmailRow(address = rows.getString("address"))
                    } else {
                        null
                    }
                }
            }
        }

    // Plain class, not a data class: matching PostgresRecoveryEmailsVerifyTest.RecoveryEmailRow —
    // nothing here compares two instances, each field is asserted against directly.
    private class RecoveryEmailRow(val address: String)

    // A Clock whose "now" a test can move between two calls on the same PostgresRecoveryEmails
    // instance, instead of reconstructing it — byte-identical to
    // PostgresRecoveryEmailsVerifyTest.MutableClock.
    private class MutableClock(instant: Instant) : Clock() {
        private var fixed: Clock = Clock.fixed(instant, ZoneOffset.UTC)

        override fun getZone(): ZoneId = fixed.zone

        override fun withZone(zone: ZoneId): Clock = fixed.withZone(zone)

        override fun instant(): Instant = fixed.instant()

        fun advance(duration: Duration) {
            fixed = Clock.fixed(fixed.instant().plus(duration), fixed.zone)
        }
    }
}
