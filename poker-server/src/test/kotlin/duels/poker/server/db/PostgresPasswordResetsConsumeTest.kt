package duels.poker.server.db

import duels.poker.server.auth.CredentialKind
import duels.poker.server.auth.PresentedSecret
import duels.poker.server.auth.RecoveryTokens
import duels.poker.server.session.PlayerId
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID
import javax.sql.DataSource
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

private const val IDENTIFIER = "alice@example.com"

/**
 * Tests for [PostgresPasswordResets.consume], against the container.
 *
 * `consume`'s expiry check is `expires_at > now()` — Postgres' own clock, never the [Clock]
 * injected into [PostgresPasswordResets] (see that class's KDoc, and `PostgresAuthSessions
 * .playerOf`'s). `aTokenPastItsHourIsRefused` therefore cannot manufacture a stale row by
 * advancing an injected clock *after* `issue` has already written `expires_at` from it — a row
 * already committed to the database cannot observe a later advance of some unrelated JVM object.
 * Instead it issues against a fresh [Clock] already more than an hour behind real time, so the
 * row is expired by Postgres' own `now()` from the moment it exists.
 *
 * `twoConcurrentUsesOfOneTokenYieldExactlyOneSuccess` copies the two-threads-one-latch shape of
 * `PostgresDuelResultStoreConcurrencyTest`.
 *
 * `aRefusedCredentialWriteLeavesTheTokenSpendable` gates the first of `consume`'s two transaction
 * boundaries — the token-spend/credential-write boundary — by using a player with no `password`
 * credential, so the `UPDATE` fails and the transaction rolls back, leaving the token unspent. The
 * second boundary, between the credential write and the session delete, stays ungated until
 * `TASK-041640`.
 */
class PostgresPasswordResetsConsumeTest {
    private lateinit var dataSource: DataSource
    private lateinit var tokens: RecoveryTokens
    private lateinit var passwordResets: PostgresPasswordResets
    private lateinit var credentials: PostgresCredentials
    private lateinit var authSessions: PostgresAuthSessions

    @BeforeEach
    fun setupDatabase() {
        PostgresTestSupport.requireDocker()
        dataSource = PostgresTestSupport.freshDatabase()
        Migrations.migrate(dataSource)
        tokens = RecoveryTokens()
        passwordResets = PostgresPasswordResets(dataSource, Clock.systemUTC(), tokens)
        credentials = PostgresCredentials(dataSource)
        authSessions = PostgresAuthSessions(dataSource, Clock.systemUTC())
    }

    @Test
    fun aGoodTokenRewritesThePasswordAndReturnsTrue() {
        runBlocking {
            val playerId = insertPlayer()
            val oldSecret = PresentedSecret("old correct horse")
            val newSecret = PresentedSecret("new correct horse battery")
            credentials.create(playerId, CredentialKind.PASSWORD, IDENTIFIER, oldSecret)
            val token = tokens.newResetToken()
            passwordResets.issue(playerId, token)

            val result = passwordResets.consume(token, newSecret)

            assertTrue(result, "a good token must return true")
            assertEquals(
                playerId,
                credentials.verify(CredentialKind.PASSWORD, IDENTIFIER, newSecret),
                "the new secret must verify after consume",
            )
            assertNull(
                credentials.verify(CredentialKind.PASSWORD, IDENTIFIER, oldSecret),
                "the old secret must no longer verify after consume — a no-op UPDATE would pass the first half alone",
            )
        }
    }

    @Test
    fun theSecondUseOfATokenIsRefused() {
        runBlocking {
            val playerId = insertPlayer()
            credentials.create(playerId, CredentialKind.PASSWORD, IDENTIFIER, PresentedSecret("original secret"))
            val token = tokens.newResetToken()
            passwordResets.issue(playerId, token)

            val first = passwordResets.consume(token, PresentedSecret("second secret"))
            val second = passwordResets.consume(token, PresentedSecret("third secret"))

            assertTrue(first, "the first, sequential use of a live token must succeed")
            assertFalse(second, "a second, sequential use of the same token must be refused")
            assertNull(
                credentials.verify(CredentialKind.PASSWORD, IDENTIFIER, PresentedSecret("third secret")),
                "the refused call's secret must never have been written",
            )
        }
    }

    // The DELETE ... RETURNING is what makes this hold; a SELECT-then-DELETE fails here and
    // nowhere else, because the sequential test above finds the row already gone by the time a
    // second, later call would read it -- concurrency is the only thing that can observe the
    // read-then-write window a SELECT would open.
    @Test
    @Timeout(60)
    fun twoConcurrentUsesOfOneTokenYieldExactlyOneSuccess() = runBlocking(Dispatchers.Default) {
        val playerId = insertPlayer()
        credentials.create(playerId, CredentialKind.PASSWORD, IDENTIFIER, PresentedSecret("original secret"))
        val token = tokens.newResetToken()
        passwordResets.issue(playerId, token)
        val firstSecret = PresentedSecret("first new secret")
        val secondSecret = PresentedSecret("second new secret")

        val gate = CompletableDeferred<Unit>()
        val jobs = listOf(
            async {
                gate.await()
                passwordResets.consume(token, firstSecret)
            },
            async {
                gate.await()
                passwordResets.consume(token, secondSecret)
            },
        )
        gate.complete(Unit)
        val results = jobs.awaitAll()

        assertEquals(1, results.count { it }, "exactly one of the two concurrent consumes must succeed")
        val firstVerifies = credentials.verify(CredentialKind.PASSWORD, IDENTIFIER, firstSecret) != null
        val secondVerifies = credentials.verify(CredentialKind.PASSWORD, IDENTIFIER, secondSecret) != null
        assertNotEquals(
            firstVerifies,
            secondVerifies,
            "the stored hash must match exactly one of the two new secrets, never neither and never both",
        )
    }

    @Test
    fun aTokenPastItsHourIsRefused() {
        runBlocking {
            val playerId = insertPlayer()
            credentials.create(playerId, CredentialKind.PASSWORD, IDENTIFIER, PresentedSecret("original secret"))
            val token = tokens.newResetToken()
            // See the class KDoc: consume checks Postgres' now(), not this clock, so the row must
            // already be expired at the moment issue() writes it.
            val staleClock = Clock.fixed(Instant.now().minus(Duration.ofMinutes(61)), ZoneOffset.UTC)
            PostgresPasswordResets(dataSource, staleClock, tokens).issue(playerId, token)
            val session = authSessions.issue(playerId)

            val result = passwordResets.consume(token, PresentedSecret("new secret"))

            assertFalse(result, "a token past its hour must be refused")
            assertEquals(
                playerId,
                credentials.verify(CredentialKind.PASSWORD, IDENTIFIER, PresentedSecret("original secret")),
                "a refused reset must leave the password unchanged",
            )
            assertEquals(
                playerId,
                authSessions.playerOf(session),
                "a refused reset must leave the player's session alive — a refused reset ends nothing",
            )
        }
    }

    @Test
    fun aSuccessfulResetEndsEverySessionThePlayerHeld() {
        runBlocking {
            val playerId = insertPlayer()
            val otherPlayerId = insertPlayer()
            credentials.create(playerId, CredentialKind.PASSWORD, IDENTIFIER, PresentedSecret("original secret"))
            val token = tokens.newResetToken()
            passwordResets.issue(playerId, token)
            val firstSession = authSessions.issue(playerId)
            val secondSession = authSessions.issue(playerId)
            val otherSession = authSessions.issue(otherPlayerId)

            val result = passwordResets.consume(token, PresentedSecret("new secret"))

            assertTrue(result, "a good token must succeed")
            assertNull(
                authSessions.playerOf(firstSession),
                "the first of the resetting player's two sessions must be gone",
            )
            assertNull(
                authSessions.playerOf(secondSession),
                "the second of the resetting player's two sessions must be gone",
            )
            assertEquals(
                otherPlayerId,
                authSessions.playerOf(otherSession),
                "a different player's session must survive — a WHERE-less DELETE passes with only one player",
            )
        }
    }

    @Test
    fun aRefusedCredentialWriteLeavesTheTokenSpendable() {
        runBlocking {
            val playerId = insertPlayer()
            val token = tokens.newResetToken()
            passwordResets.issue(playerId, token)
            val session = authSessions.issue(playerId)
            val newSecret = PresentedSecret("new secret")

            val firstResult = passwordResets.consume(token, newSecret)

            assertFalse(firstResult, "consume must return false when the player has no password credential")
            assertEquals(
                playerId,
                authSessions.playerOf(session),
                "the player's session must survive a refused consume — the transaction rolled back",
            )

            credentials.create(playerId, CredentialKind.PASSWORD, IDENTIFIER, newSecret)
            val secondResult = passwordResets.consume(token, newSecret)

            assertTrue(secondResult, "the same token must work after the credential is created — it was never spent")
            assertEquals(
                playerId,
                credentials.verify(CredentialKind.PASSWORD, IDENTIFIER, newSecret),
                "the secret must verify after the second consume",
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
}
