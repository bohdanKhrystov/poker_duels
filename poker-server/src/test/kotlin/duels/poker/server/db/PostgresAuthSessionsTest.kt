package duels.poker.server.db

import duels.poker.server.auth.SessionToken
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
import kotlin.test.assertNull

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

    @Test
    fun aLiveTokenNamesItsPlayer() {
        runBlocking {
            val alice = insertPlayer()
            val bob = insertPlayer()

            val aliceToken = authSessions.issue(alice)
            val bobToken = authSessions.issue(bob)

            assertEquals(alice, authSessions.playerOf(aliceToken), "alice's token must resolve to alice")
            assertEquals(bob, authSessions.playerOf(bobToken), "bob's token must resolve to bob")
        }
    }

    @Test
    fun anUnknownTokenIsNull() {
        runBlocking {
            assertNull(authSessions.playerOf(SessionToken("not-a-token")), "a token with no row must answer null")
        }
    }

    // The issuing clock is fixed 31 days before the real "now" the database's own now() will
    // compare against, so the row is already past its expires_at the moment it lands — no test
    // here sleeps, and no test moves the database's clock.
    @Test
    fun anExpiredTokenIsNull() {
        runBlocking {
            val playerId = insertPlayer()
            val expiredSessions = PostgresAuthSessions(dataSource, Clock.fixed(Instant.now().minus(31, DAYS), ZoneOffset.UTC))

            val token = expiredSessions.issue(playerId)

            assertNull(authSessions.playerOf(token), "a session issued 31 days ago must already be expired")
        }
    }

    // One hour on the far side of the same 30-day boundary anExpiredTokenIsNull sits inside of:
    // this proves the predicate above cannot be satisfied by one that refuses every row.
    @Test
    fun aTokenIssuedThirtyDaysAgoLessAnHourStillReads() {
        runBlocking {
            val playerId = insertPlayer()
            val almostExpired = Instant.now().minus(Duration.ofDays(30)).plus(Duration.ofHours(1))
            val almostExpiredSessions = PostgresAuthSessions(dataSource, Clock.fixed(almostExpired, ZoneOffset.UTC))

            val token = almostExpiredSessions.issue(playerId)

            assertEquals(playerId, authSessions.playerOf(token), "a session with an hour left must still read")
        }
    }

    @Test
    fun oneExpiredSessionDoesNotHideALiveOne() {
        runBlocking {
            val playerId = insertPlayer()
            val expiredSessions = PostgresAuthSessions(dataSource, Clock.fixed(Instant.now().minus(31, DAYS), ZoneOffset.UTC))

            val expiredToken = expiredSessions.issue(playerId)
            val liveToken = authSessions.issue(playerId)

            assertEquals(playerId, authSessions.playerOf(liveToken), "the live session for this player must still resolve")
            assertNull(authSessions.playerOf(expiredToken), "the expired session for the same player must not resolve")
        }
    }

    @Test
    fun deletingRemovesThatRow() {
        runBlocking {
            val playerId = insertPlayer()

            val token = authSessions.issue(playerId)
            authSessions.delete(token)

            assertNull(authSessions.playerOf(token), "after deleting a token, playerOf must return null")
            assertEquals(0, countAuthSessionRows(), "after deleting a token, the count must be 0")
        }
    }

    @Test
    fun deletingTwiceIsTheSame() {
        runBlocking {
            val playerId = insertPlayer()

            val token = authSessions.issue(playerId)
            val countAfterIssue = countAuthSessionRows()
            authSessions.delete(token)
            val countAfterFirstDelete = countAuthSessionRows()
            authSessions.delete(token)
            val countAfterSecondDelete = countAuthSessionRows()

            assertEquals(1, countAfterIssue, "issuing once must create one row")
            assertEquals(0, countAfterFirstDelete, "deleting once must remove the row")
            assertEquals(0, countAfterSecondDelete, "deleting twice must leave the count at 0")
        }
    }

    @Test
    fun deletingOneSessionLeavesTheOther() {
        runBlocking {
            val playerId = insertPlayer()

            val token1 = authSessions.issue(playerId)
            val token2 = authSessions.issue(playerId)

            authSessions.delete(token1)

            assertNull(authSessions.playerOf(token1), "the deleted token must return null")
            assertEquals(playerId, authSessions.playerOf(token2), "the other token must still resolve")
            assertEquals(1, countAuthSessionRows(), "there must be exactly one session left")
        }
    }

    @Test
    fun deletingLeavesThePlayerRowAlone() {
        runBlocking {
            val playerId = insertPlayer()

            val token = authSessions.issue(playerId)
            val playerBefore = playerRow(playerId)

            authSessions.delete(token)

            val playerAfter = playerRow(playerId)
            assertEquals(playerBefore.id, playerAfter.id, "player id must be unchanged")
            assertEquals(playerBefore.deviceId, playerAfter.deviceId, "device_id must be unchanged")
            assertEquals(playerBefore.coinBalance, playerAfter.coinBalance, "coin_balance must be unchanged")
            assertEquals(playerBefore.displayName, playerAfter.displayName, "display_name must be unchanged")
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

    private fun playerRow(playerId: PlayerId): PlayerRecord =
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                "SELECT id, device_id, coin_balance, display_name FROM player WHERE id = ?",
            ).use { statement ->
                statement.setObject(1, UUID.fromString(playerId.value))
                statement.executeQuery().use { rows ->
                    check(rows.next()) { "no player row for id ${playerId.value}" }
                    PlayerRecord(
                        id = rows.getString("id"),
                        deviceId = rows.getString("device_id"),
                        coinBalance = rows.getInt("coin_balance"),
                        displayName = rows.getString("display_name"),
                    )
                }
            }
        }

    // Plain class, not a data class: a data class over a ByteArray property gets a
    // reference-equality equals()/hashCode() the compiler warns about, and nothing here compares
    // two StoredSession instances — each field is asserted against directly.
    private class StoredSession(
        val tokenHash: ByteArray,
        val playerId: String,
        val issuedAt: OffsetDateTime,
        val expiresAt: OffsetDateTime,
    )

    private class PlayerRecord(
        val id: String,
        val deviceId: String,
        val coinBalance: Int,
        val displayName: String?,
    )
}
