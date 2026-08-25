package duels.poker.server.db

import duels.poker.server.auth.EmailAddress
import duels.poker.server.auth.VerificationToken
import duels.poker.server.auth.VerifyEmailResult
import duels.poker.server.session.PlayerId
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import java.util.UUID
import javax.sql.DataSource
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

// Unlike PostgresRecoveryEmailsClaimTest's FIXED_INSTANT, this one cannot be an arbitrary
// historical literal: verifyPending's expiry is enforced by Postgres's own now() (ADR-0031 §3),
// never by comparing the injected Clock in Kotlin, so a "live" row needs an expires_at genuinely
// ahead of the database's real wall clock. Truncated to seconds so the round trip through a
// TIMESTAMPTZ column, which stores microseconds, can never disagree with the value asserted here.
private val FIXED_INSTANT: Instant = Instant.now().truncatedTo(ChronoUnit.SECONDS)

/**
 * Tests for [PostgresRecoveryEmails.verifyPending], against the container.
 *
 * Reuses [PostgresRecoveryEmailsClaimTest]'s fixture shape — a fresh migrated database and a
 * mutable [Clock] a test can advance between two calls on the same [PostgresRecoveryEmails]
 * instance — but cannot reuse its private `MutableClock`, so an equivalent lives here too.
 */
class PostgresRecoveryEmailsVerifyTest {
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
    fun verifyingMovesTheAddressIntoTheProvenTable() {
        runBlocking {
            val playerId = insertPlayer()
            val token = VerificationToken("verify-token")
            recoveryEmails.claimPending(playerId, EmailAddress("Bob@Example.com"), token)

            val result = recoveryEmails.verifyPending(token)

            assertEquals(VerifyEmailResult.Verified, result, "a live token must verify")
            assertEquals(0, countPendingRowsFor(playerId), "email_verification must hold no row for the player")
            val row = recoveryEmailRowFor(playerId)
            assertNotNull(row, "recovery_email must hold a row for the player")
            assertEquals("Bob@Example.com", row.address, "address must be stored exactly as typed, unfolded")
            assertEquals(FIXED_INSTANT, row.verifiedAt, "verified_at must equal the injected clock's instant")
        }
    }

    @Test
    fun theSecondUseOfATokenIsRefused() {
        runBlocking {
            val playerId = insertPlayer()
            val token = VerificationToken("reuse-token")
            recoveryEmails.claimPending(playerId, EmailAddress("a@x.test"), token)
            val firstResult = recoveryEmails.verifyPending(token)

            val secondResult = recoveryEmails.verifyPending(token)

            assertEquals(VerifyEmailResult.Verified, firstResult, "the first use of a live token must verify")
            assertEquals(VerifyEmailResult.Refused, secondResult, "reusing an already-consumed token must be refused")
            assertEquals(1, countRecoveryEmailRows(), "recovery_email must still hold exactly one row")
        }
    }

    @Test
    fun aTokenPastItsDayIsRefused() {
        runBlocking {
            val playerId = insertPlayer()
            val token = VerificationToken("stale-token")
            // The DELETE's expiry check is expires_at > now(), Postgres's own clock — advancing
            // the injected Clock after the claim cannot touch an expires_at already written, and
            // there is no real 24 hours to wait out. Moving the clock behind real time before the
            // claim is what lets a token be born already past its lifetime, with no sleep at all.
            clock.advance(Duration.ofHours(24).plusSeconds(1).negated())
            recoveryEmails.claimPending(playerId, EmailAddress("a@x.test"), token)

            val result = recoveryEmails.verifyPending(token)

            assertEquals(VerifyEmailResult.Refused, result, "a token past its 24-hour lifetime must be refused")
            assertEquals(0, countRecoveryEmailRows(), "recovery_email must stay empty")
            assertEquals(1, countPendingRowsFor(playerId), "a failed, expired attempt must not consume the pending row")
        }
    }

    @Test
    fun theSecondPlayerToVerifyOneAddressIsToldItIsTaken() {
        runBlocking {
            val firstPlayer = insertPlayer()
            val secondPlayer = insertPlayer()
            val firstToken = VerificationToken("first-token")
            val secondToken = VerificationToken("second-token")
            recoveryEmails.claimPending(firstPlayer, EmailAddress("bob@x.test"), firstToken)
            recoveryEmails.claimPending(secondPlayer, EmailAddress("bob@x.test"), secondToken)

            val firstResult = recoveryEmails.verifyPending(firstToken)
            val secondResult = recoveryEmails.verifyPending(secondToken)

            assertEquals(VerifyEmailResult.Verified, firstResult, "the first player to verify must win the address")
            assertEquals(
                VerifyEmailResult.AddressTaken,
                secondResult,
                "the second player to verify the same address must be told it is taken",
            )
            assertEquals(1, countRecoveryEmailRows(), "recovery_email must still hold exactly one row")
            assertNotNull(recoveryEmailRowFor(firstPlayer), "the surviving row must belong to the first player")
            assertNull(recoveryEmailRowFor(secondPlayer), "the second player must not have won a row")
            assertEquals(
                1,
                countPendingRowsFor(secondPlayer),
                "the rollback must leave the loser's pending row in place, not burn it on a losing race",
            )
        }
    }

    @Test
    fun aFoldedCollisionIsToldTheSameThing() {
        runBlocking {
            val firstPlayer = insertPlayer()
            val secondPlayer = insertPlayer()
            val firstToken = VerificationToken("first-token")
            val secondToken = VerificationToken("second-token")
            recoveryEmails.claimPending(firstPlayer, EmailAddress("bob@x.test"), firstToken)
            recoveryEmails.claimPending(secondPlayer, EmailAddress("BOB@X.TEST"), secondToken)

            val firstResult = recoveryEmails.verifyPending(firstToken)
            val secondResult = recoveryEmails.verifyPending(secondToken)

            assertEquals(VerifyEmailResult.Verified, firstResult, "the first player to verify must win the address")
            assertEquals(
                VerifyEmailResult.AddressTaken,
                secondResult,
                "a collision differing only in case must be told the same thing as an exact one",
            )
            assertEquals(1, countRecoveryEmailRows(), "recovery_email must still hold exactly one row")
        }
    }

    // The security property under test is that Refused gives no way to tell the three causes
    // apart, which is a claim about all three answering the same value, not each merely being
    // Refused on its own. Constructed here: unknownToken names a token no claim ever produced;
    // expiredToken comes from a real claim whose lifetime the injected clock is then advanced
    // past; usedToken comes from a real claim consumed by a genuine prior successful verify. All
    // three are reachable this way and none is simulated by writing a row directly.
    @Test
    fun theThreeCausesOfRefusalAreIndistinguishable() {
        runBlocking {
            val unknownToken = VerificationToken("never-claimed")

            val expiredPlayer = insertPlayer()
            val expiredToken = VerificationToken("expires-token")
            // Same reasoning as aTokenPastItsDayIsRefused: back the clock up before the claim, so
            // this row is born already past its lifetime, then restore it so the claim below is a
            // normal, live one.
            val pastLifetime = Duration.ofHours(24).plusSeconds(1)
            clock.advance(pastLifetime.negated())
            recoveryEmails.claimPending(expiredPlayer, EmailAddress("expired@x.test"), expiredToken)
            clock.advance(pastLifetime)

            val usedPlayer = insertPlayer()
            val usedToken = VerificationToken("used-token")
            recoveryEmails.claimPending(usedPlayer, EmailAddress("used@x.test"), usedToken)
            recoveryEmails.verifyPending(usedToken)

            val unknownResult = recoveryEmails.verifyPending(unknownToken)
            val expiredResult = recoveryEmails.verifyPending(expiredToken)
            val usedResult = recoveryEmails.verifyPending(usedToken)

            assertEquals(VerifyEmailResult.Refused, unknownResult, "an unknown token must be refused")
            assertEquals(
                unknownResult,
                expiredResult,
                "an expired token must answer the identical value as an unknown one, not merely also Refused",
            )
            assertEquals(
                unknownResult,
                usedResult,
                "an already-used token must answer the identical value as an unknown one, not merely also Refused",
            )
        }
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

    private fun countRecoveryEmailRows(): Int =
        dataSource.connection.use { connection ->
            connection.prepareStatement("SELECT count(*) FROM recovery_email").use { statement ->
                statement.executeQuery().use { rows ->
                    rows.next()
                    rows.getInt(1)
                }
            }
        }

    private fun recoveryEmailRowFor(playerId: PlayerId): RecoveryEmailRow? =
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                "SELECT address, verified_at FROM recovery_email WHERE player_id = ?",
            ).use { statement ->
                statement.setObject(1, UUID.fromString(playerId.value))
                statement.executeQuery().use { rows ->
                    if (rows.next()) {
                        RecoveryEmailRow(
                            address = rows.getString("address"),
                            verifiedAt = rows.getObject("verified_at", OffsetDateTime::class.java).toInstant(),
                        )
                    } else {
                        null
                    }
                }
            }
        }

    // Plain class, not a data class: matching PostgresRecoveryEmailsClaimTest.PendingClaimRow —
    // nothing here compares two instances, each field is asserted against directly.
    private class RecoveryEmailRow(val address: String, val verifiedAt: Instant)

    // A Clock whose "now" a test can move between two calls on the same PostgresRecoveryEmails
    // instance, instead of reconstructing it — byte-identical to
    // PostgresRecoveryEmailsClaimTest.MutableClock, which is private to that file and so cannot
    // be shared rather than duplicated.
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
