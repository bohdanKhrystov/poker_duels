package duels.poker.server.db

import duels.poker.server.session.PlayerId
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.security.MessageDigest
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit.DAYS
import java.util.UUID
import javax.sql.DataSource
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse

private val FIXED_INSTANT: Instant = Instant.parse("2026-08-20T09:00:00Z")
private val FIXED_CLOCK: Clock = Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC)

/**
 * Tests for [PostgresAuthSessions], against the container.
 *
 * `thePlaintextTokenIsNowhereInTheRow` and `theDigestIsTheSha256OfTheToken` each recompute the
 * SHA-256 independently rather than trusting one another, because a digest is exactly the shape
 * where a test can agree with a wrong implementation: reading back *a* digest proves a digest was
 * written, not that the plaintext was not written too. Both look the row up by `player_id`, never
 * by the digest under test, so a wrong `token_hash` fails the assertion below rather than making
 * the row unfindable in the first place. `expiryIsThirtyDaysAfterIssue` asserts both the interval
 * and its origin, because a right interval measured off a wrong origin — `Instant.now()` in place
 * of the injected clock — would still pass an interval-only assertion.
 */
class PostgresAuthSessionsTest {
    private lateinit var dataSource: DataSource
    private lateinit var authSessions: PostgresAuthSessions

    @BeforeEach
    fun setupDatabase() {
        dataSource = PostgresTestSupport.freshDatabase()
        Migrations.migrate(dataSource)
        authSessions = PostgresAuthSessions(dataSource, FIXED_CLOCK)
    }

    @Test
    fun issuingWritesExactlyOneRow() {
        runBlocking {
            val playerId = insertPlayer()

            authSessions.issue(playerId)
            val afterOne = countAuthSessionRows()
            authSessions.issue(playerId)
            val afterTwo = countAuthSessionRows()

            assertEquals(1, afterOne, "issuing once must write exactly one row")
            assertEquals(2, afterTwo, "a second issue() for the same player is a legal second row, not an upsert")
        }
    }

    @Test
    fun theRowNamesThePlayerItWasIssuedFor() {
        runBlocking {
            val first = insertPlayer()
            val second = insertPlayer()

            authSessions.issue(first)
            authSessions.issue(second)

            assertEquals(first.value, rowFor(first).playerId, "first player's row must name the first player")
            assertEquals(second.value, rowFor(second).playerId, "second player's row must name the second player")
        }
    }

    @Test
    fun thePlaintextTokenIsNowhereInTheRow() {
        runBlocking {
            val playerId = insertPlayer()

            val token = authSessions.issue(playerId)

            val row = rowFor(playerId)
            assertContentEquals(sha256(token.value), row.tokenHash, "token_hash must be the SHA-256 of the returned token")
            assertFalse(
                row.tokenHash.contentEquals(token.value.toByteArray(Charsets.UTF_8)),
                "token_hash must not be the raw token bytes",
            )
            assertFalse(
                tokenHashEscaped(playerId).contains(token.value),
                "token_hash, read back as escaped text, must not contain the plaintext token",
            )
        }
    }

    @Test
    fun theDigestIsTheSha256OfTheToken() {
        runBlocking {
            val playerId = insertPlayer()

            val token = authSessions.issue(playerId)

            assertContentEquals(
                sha256(token.value),
                rowFor(playerId).tokenHash,
                "token_hash must equal a SHA-256 computed independently from the token",
            )
        }
    }

    @Test
    fun expiryIsThirtyDaysAfterIssue() {
        runBlocking {
            val playerId = insertPlayer()

            authSessions.issue(playerId)

            val row = rowFor(playerId)
            assertEquals(FIXED_INSTANT, row.issuedAt.toInstant(), "issued_at must be the fixed clock's instant")
            assertEquals(
                Duration.ofDays(30),
                Duration.between(row.issuedAt.toInstant(), row.expiresAt.toInstant()),
                "expires_at must be exactly 30 days after issued_at",
            )
        }
    }

    @Test
    fun twoClocksGiveTwoExpiries() {
        runBlocking {
            val earlyPlayer = insertPlayer()
            val laterPlayer = insertPlayer()
            val laterSessions = PostgresAuthSessions(dataSource, Clock.fixed(FIXED_INSTANT.plus(10, DAYS), ZoneOffset.UTC))

            authSessions.issue(earlyPlayer)
            laterSessions.issue(laterPlayer)

            val earlyExpiry = rowFor(earlyPlayer).expiresAt.toInstant()
            val laterExpiry = rowFor(laterPlayer).expiresAt.toInstant()
            assertEquals(
                Duration.ofDays(10),
                Duration.between(earlyExpiry, laterExpiry),
                "a clock read 10 days later must write an expires_at 10 days later",
            )
        }
    }

    private fun insertPlayer(): PlayerId {
        val id = UUID.randomUUID()
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                "INSERT INTO player (id, device_id, coin_balance) VALUES (?, ?, ?)",
            ).use { statement ->
                statement.setObject(1, id)
                statement.setString(2, "device-$id")
                statement.setInt(3, 100)
                statement.executeUpdate()
            }
        }
        return PlayerId(id.toString())
    }

    private fun countAuthSessionRows(): Int =
        dataSource.connection.use { connection ->
            connection.prepareStatement("SELECT count(*) FROM auth_session").use { statement ->
                statement.executeQuery().use { rows ->
                    rows.next()
                    rows.getInt(1)
                }
            }
        }

    // Looked up by player_id, never by token_hash: the row under test must be findable even when
    // the digest itself is wrong, so a broken digest fails the assertion on tokenHash below rather
    // than an unrelated "no such row" from a lookup keyed on the very value being checked.
    private fun rowFor(playerId: PlayerId): StoredSession =
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                "SELECT token_hash, player_id, issued_at, expires_at FROM auth_session WHERE player_id = ?",
            ).use { statement ->
                statement.setObject(1, UUID.fromString(playerId.value))
                statement.executeQuery().use { rows ->
                    check(rows.next()) { "no auth_session row for player ${playerId.value}" }
                    StoredSession(
                        tokenHash = rows.getBytes("token_hash"),
                        playerId = rows.getString("player_id"),
                        issuedAt = rows.getObject("issued_at", OffsetDateTime::class.java),
                        expiresAt = rows.getObject("expires_at", OffsetDateTime::class.java),
                    )
                }
            }
        }

    private fun tokenHashEscaped(playerId: PlayerId): String =
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                "SELECT encode(token_hash, 'escape') FROM auth_session WHERE player_id = ?",
            ).use { statement ->
                statement.setObject(1, UUID.fromString(playerId.value))
                statement.executeQuery().use { rows ->
                    check(rows.next()) { "no auth_session row for player ${playerId.value}" }
                    rows.getString(1)
                }
            }
        }

    private fun sha256(value: String): ByteArray =
        MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))

    // Plain class, not a data class: a data class over a ByteArray property gets a
    // reference-equality equals()/hashCode() the compiler warns about, and nothing here compares
    // two StoredSession instances — each field is asserted against directly.
    private class StoredSession(
        val tokenHash: ByteArray,
        val playerId: String,
        val issuedAt: OffsetDateTime,
        val expiresAt: OffsetDateTime,
    )
}
