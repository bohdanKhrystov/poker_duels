package duels.poker.server.db

import duels.poker.server.auth.ClaimPendingResult
import duels.poker.server.auth.EmailAddress
import duels.poker.server.auth.VerificationToken
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
import java.util.UUID
import javax.sql.DataSource
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse

private val FIXED_INSTANT: Instant = Instant.parse("2026-08-20T09:00:00Z")

/**
 * Tests for [PostgresRecoveryEmails.claimPending], against the container.
 *
 * Every instance is built over a [MutableClock] rather than [Clock.fixed] directly, so a test can
 * advance the injected clock between two calls on the *same* [PostgresRecoveryEmails] without
 * reconstructing it — `aClaimExpiresTwentyFourHoursAfterTheInjectedClock` and
 * `aSecondClaimLeavesExactlyOnePendingRow` each need a second, later instant from the same clock
 * the class was built with.
 *
 * `aSecondClaimLeavesExactlyOnePendingRow` advances the clock sixteen minutes between its two
 * claims — one minute past `ADR-0031` §5's fifteen-minute resend window, which `TASK-041636` adds
 * inside this same transaction. This ticket answers `Claimed` unconditionally, so the gap proves
 * nothing about suppression *yet*; it exists so the test still proves "a second claim replaces
 * the first" once `TASK-041636` lands, without this file changing.
 */
class PostgresRecoveryEmailsClaimTest {
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
    fun aClaimStoresTheAddressExactlyAsTyped() {
        runBlocking {
            val playerId = insertPlayer()
            val token = VerificationToken("claim-token")

            val result = recoveryEmails.claimPending(playerId, EmailAddress("Bob@Example.com"), token)

            val row = rowFor(playerId)
            assertEquals(ClaimPendingResult.Claimed, result, "a fresh claim must answer Claimed")
            assertEquals("Bob@Example.com", row.address, "address must be stored exactly as typed, unfolded")
            assertContentEquals(
                recoveryTokenDigest(token),
                row.tokenHash,
                "token_hash must equal the SHA-256 digest of the plaintext token",
            )
            assertFalse(
                row.tokenHash.contentEquals(token.value.toByteArray(Charsets.UTF_8)),
                "token_hash must not be the raw token bytes",
            )
            assertFalse(
                escapedTokenHash(playerId).contains(token.value),
                "token_hash, read back as escaped text, must not contain the plaintext token",
            )
        }
    }

    @Test
    fun aClaimExpiresTwentyFourHoursAfterTheInjectedClock() {
        runBlocking {
            // Two different players, not one claiming twice: replacement is
            // aSecondClaimLeavesExactlyOnePendingRow's property to prove, and this test must stay
            // green even if the DELETE below the INSERT were missing entirely.
            val firstPlayer = insertPlayer()
            val secondPlayer = insertPlayer()

            val firstResult =
                recoveryEmails.claimPending(firstPlayer, EmailAddress("a@x.test"), VerificationToken("token-1"))
            val firstRow = rowFor(firstPlayer)

            clock.advance(Duration.ofHours(1))
            val secondResult =
                recoveryEmails.claimPending(secondPlayer, EmailAddress("b@x.test"), VerificationToken("token-2"))
            val secondRow = rowFor(secondPlayer)

            assertEquals(ClaimPendingResult.Claimed, firstResult, "the first claim must answer Claimed")
            assertEquals(ClaimPendingResult.Claimed, secondResult, "the second claim must answer Claimed")
            assertEquals(FIXED_INSTANT, firstRow.issuedAt, "issued_at must equal the fixed clock's first instant")
            assertEquals(
                Duration.ofHours(24),
                Duration.between(firstRow.issuedAt, firstRow.expiresAt),
                "expires_at must be exactly 24 hours after issued_at",
            )
            assertEquals(
                FIXED_INSTANT.plus(Duration.ofHours(1)),
                secondRow.issuedAt,
                "issued_at must move by exactly the clock's advance",
            )
            assertEquals(
                firstRow.expiresAt.plus(Duration.ofHours(1)),
                secondRow.expiresAt,
                "expires_at must move by exactly the clock's advance too",
            )
            assertEquals(
                Duration.ofHours(24),
                Duration.between(secondRow.issuedAt, secondRow.expiresAt),
                "expires_at must still be exactly 24 hours after issued_at once the clock has moved",
            )
        }
    }

    @Test
    fun aSecondClaimLeavesExactlyOnePendingRow() {
        runBlocking {
            val playerId = insertPlayer()

            val firstResult =
                recoveryEmails.claimPending(playerId, EmailAddress("a@x.test"), VerificationToken("token-a"))
            clock.advance(Duration.ofMinutes(16))
            val secondResult =
                recoveryEmails.claimPending(playerId, EmailAddress("b@x.test"), VerificationToken("token-b"))

            assertEquals(ClaimPendingResult.Claimed, firstResult, "the first claim must answer Claimed")
            assertEquals(ClaimPendingResult.Claimed, secondResult, "the second claim must answer Claimed")
            assertEquals(1, countPendingRowsFor(playerId), "a second claim must leave exactly one pending row")
            assertEquals("b@x.test", rowFor(playerId).address, "the surviving row must be the second address")
        }
    }

    @Test
    fun oneClaimNeverDisturbsAnotherPlayers() {
        runBlocking {
            val alice = insertPlayer()
            val bob = insertPlayer()
            val aliceToken = VerificationToken("alice-token")

            val aliceResult = recoveryEmails.claimPending(alice, EmailAddress("alice@x.test"), aliceToken)
            val bobResult =
                recoveryEmails.claimPending(bob, EmailAddress("bob@x.test"), VerificationToken("bob-token"))

            assertEquals(ClaimPendingResult.Claimed, aliceResult, "alice's claim must answer Claimed")
            assertEquals(ClaimPendingResult.Claimed, bobResult, "bob's claim must answer Claimed")
            val aliceRow = rowFor(alice)
            assertEquals("alice@x.test", aliceRow.address, "bob's claim must not change alice's address")
            assertContentEquals(
                recoveryTokenDigest(aliceToken),
                aliceRow.tokenHash,
                "bob's claim must not change alice's token_hash",
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

    // Looked up by player_id, never by token_hash, the same reason PostgresAuthSessionsTest.rowFor
    // is: the row under test must be findable even when the digest itself is wrong.
    private fun rowFor(playerId: PlayerId): PendingClaimRow =
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                "SELECT token_hash, address, issued_at, expires_at FROM email_verification WHERE player_id = ?",
            ).use { statement ->
                statement.setObject(1, UUID.fromString(playerId.value))
                statement.executeQuery().use { rows ->
                    check(rows.next()) { "no email_verification row for player ${playerId.value}" }
                    PendingClaimRow(
                        tokenHash = rows.getBytes("token_hash"),
                        address = rows.getString("address"),
                        issuedAt = rows.getObject("issued_at", OffsetDateTime::class.java).toInstant(),
                        expiresAt = rows.getObject("expires_at", OffsetDateTime::class.java).toInstant(),
                    )
                }
            }
        }

    private fun escapedTokenHash(playerId: PlayerId): String =
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                "SELECT encode(token_hash, 'escape') FROM email_verification WHERE player_id = ?",
            ).use { statement ->
                statement.setObject(1, UUID.fromString(playerId.value))
                statement.executeQuery().use { rows ->
                    check(rows.next()) { "no email_verification row for player ${playerId.value}" }
                    rows.getString(1)
                }
            }
        }

    // Plain class, not a data class: nothing here compares two instances, each field is asserted
    // against directly — matching PostgresAuthSessionsTest.StoredSession.
    private class PendingClaimRow(
        val tokenHash: ByteArray,
        val address: String,
        val issuedAt: Instant,
        val expiresAt: Instant,
    )

    // A Clock whose "now" a test can move between two calls on the same PostgresRecoveryEmails
    // instance, instead of reconstructing it. Holds a Clock.fixed(...) value that advance()
    // replaces with a later one: a mutable holder over Clock.fixed(...).
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
