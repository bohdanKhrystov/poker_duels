package duels.poker.server.db

import duels.poker.server.auth.RecoveryTokens
import duels.poker.server.session.PlayerId
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.security.SecureRandom
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
import kotlin.test.assertTrue

private val FIXED_INSTANT: Instant = Instant.parse("2026-08-20T09:00:00Z")

/**
 * Tests for [PostgresPasswordResets.issue], against the container.
 *
 * Every instance is built over a [MutableClock] rather than [Clock.fixed] directly, so a test can
 * advance the injected clock between two calls on the *same* [PostgresPasswordResets] without
 * reconstructing it — the two window tests each need a second, later instant from the same clock
 * the class was built with.
 *
 * `issue` compares the outstanding row's `issued_at` against `clock.instant()`, not SQL `now()`
 * (`PostgresPasswordResets.selectIssuedAt`'s comment), so a stale fixture is built by advancing
 * this clock forward after the first issue, never by backdating it beforehand.
 *
 * Tokens come from [RecoveryTokens] over a [PinnedSecureRandom] rather than a real
 * [SecureRandom], so every minted token in a test is deterministic and distinct from the one
 * before it — which the supersede test relies on to prove which token survived.
 */
class PostgresPasswordResetsIssueTest {
    private lateinit var dataSource: DataSource
    private lateinit var clock: MutableClock
    private lateinit var tokens: RecoveryTokens
    private lateinit var passwordResets: PostgresPasswordResets

    @BeforeEach
    fun setupDatabase() {
        dataSource = PostgresTestSupport.freshDatabase()
        Migrations.migrate(dataSource)
        clock = MutableClock(FIXED_INSTANT)
        tokens = RecoveryTokens(PinnedSecureRandom())
        passwordResets = PostgresPasswordResets(dataSource, clock, tokens)
    }

    @Test
    fun issuingStoresTheHashAndAnHoursExpiry() {
        runBlocking {
            val playerId = insertPlayer()
            val token = tokens.newResetToken()

            val result = passwordResets.issue(playerId, token)

            assertTrue(result, "a fresh issue must return true")
            val row = rowFor(playerId)
            assertContentEquals(
                recoveryTokenDigest(token),
                row.tokenHash,
                "token_hash must equal the SHA-256 digest of the plaintext token",
            )
            // Absolute instants, not a duration between issued_at and expires_at: an hour past
            // SQL now() is also an hour past issued_at, so only comparing against the injected
            // clock's own value proves the clock — not now() — was used at all.
            assertEquals(FIXED_INSTANT, row.issuedAt, "issued_at must equal the injected clock's instant")
            assertEquals(
                FIXED_INSTANT.plus(Duration.ofHours(1)),
                row.expiresAt,
                "expires_at must be exactly one hour after the injected clock's instant",
            )
        }
    }

    @Test
    fun aSecondRequestInsideAQuarterHourWritesNothing() {
        runBlocking {
            val playerId = insertPlayer()
            val firstToken = tokens.newResetToken()

            val firstResult = passwordResets.issue(playerId, firstToken)
            val firstRow = rowFor(playerId)
            clock.advance(Duration.ofMinutes(14))
            val secondResult = passwordResets.issue(playerId, tokens.newResetToken())

            assertTrue(firstResult, "the first issue must return true")
            assertFalse(secondResult, "a request fourteen minutes after a live token must return false")
            // Never a row count: a count of 1 is satisfied by a replacement too. What proves
            // nothing was written is that the surviving row is still the FIRST token's — both its
            // hash and its expires_at, unmoved by the second, suppressed call.
            val row = rowFor(playerId)
            assertContentEquals(
                recoveryTokenDigest(firstToken),
                row.tokenHash,
                "a suppressed request must leave the first token's hash live, not the second's",
            )
            assertEquals(
                firstRow.expiresAt,
                row.expiresAt,
                "a suppressed request must leave the first token's expires_at untouched",
            )
        }
    }

    @Test
    fun aSecondRequestAfterAQuarterHourSupersedesTheFirst() {
        runBlocking {
            val playerId = insertPlayer()
            val firstResult = passwordResets.issue(playerId, tokens.newResetToken())
            clock.advance(Duration.ofMinutes(16))
            val secondToken = tokens.newResetToken()

            val secondResult = passwordResets.issue(playerId, secondToken)

            assertTrue(firstResult, "the first issue must return true")
            assertTrue(secondResult, "a request sixteen minutes after a live token must return true")
            assertEquals(1, countRowsFor(playerId), "a superseding issue must leave exactly one row")
            val row = rowFor(playerId)
            assertContentEquals(
                recoveryTokenDigest(secondToken),
                row.tokenHash,
                "the surviving row must hold the second token's hash, not the first's",
            )
        }
    }

    @Test
    fun oneAccountsSilenceIsNotAnothers() {
        runBlocking {
            val alice = insertPlayer()
            val bob = insertPlayer()
            val aliceToken = tokens.newResetToken()
            val bobToken = tokens.newResetToken()

            val aliceResult = passwordResets.issue(alice, aliceToken)
            val bobResult = passwordResets.issue(bob, bobToken)

            assertTrue(aliceResult, "alice's issue must return true")
            assertTrue(
                bobResult,
                "bob's issue, inside alice's window, must still return true for his own account",
            )
            val bobRow = rowFor(bob)
            assertContentEquals(
                recoveryTokenDigest(bobToken),
                bobRow.tokenHash,
                "bob's own row must hold his own token, not be suppressed",
            )
            val aliceRow = rowFor(alice)
            assertContentEquals(
                recoveryTokenDigest(aliceToken),
                aliceRow.tokenHash,
                "bob's issue must not touch alice's token_hash",
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

    private fun countRowsFor(playerId: PlayerId): Int =
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                "SELECT count(*) FROM password_reset WHERE player_id = ?",
            ).use { statement ->
                statement.setObject(1, UUID.fromString(playerId.value))
                statement.executeQuery().use { rows ->
                    rows.next()
                    rows.getInt(1)
                }
            }
        }

    // Looked up by player_id, never by token_hash, the same reason
    // PostgresRecoveryEmailsClaimTest.rowFor is: the row under test must be findable even when
    // the digest itself is wrong.
    private fun rowFor(playerId: PlayerId): StoredResetToken =
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                "SELECT token_hash, issued_at, expires_at FROM password_reset WHERE player_id = ?",
            ).use { statement ->
                statement.setObject(1, UUID.fromString(playerId.value))
                statement.executeQuery().use { rows ->
                    check(rows.next()) { "no password_reset row for player ${playerId.value}" }
                    StoredResetToken(
                        tokenHash = rows.getBytes("token_hash"),
                        issuedAt = rows.getObject("issued_at", OffsetDateTime::class.java).toInstant(),
                        expiresAt = rows.getObject("expires_at", OffsetDateTime::class.java).toInstant(),
                    )
                }
            }
        }

    // Plain class, not a data class: nothing here compares two instances, each field is asserted
    // against directly — matching PostgresRecoveryEmailsClaimTest.PendingClaimRow.
    private class StoredResetToken(
        val tokenHash: ByteArray,
        val issuedAt: Instant,
        val expiresAt: Instant,
    )

    // A Clock whose "now" a test can move between two calls on the same PostgresPasswordResets
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

    // A SecureRandom whose output is deterministic yet distinct per call: each call fills the
    // buffer with a different, incrementing byte, so successive RecoveryTokens.newResetToken()
    // calls in one test are pinned and never collide with each other.
    private class PinnedSecureRandom : SecureRandom() {
        private var counter = 0

        override fun nextBytes(bytes: ByteArray) {
            counter += 1
            bytes.fill(counter.toByte())
        }
    }
}
