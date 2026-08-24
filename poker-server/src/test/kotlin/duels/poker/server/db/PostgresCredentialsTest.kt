package duels.poker.server.db

import duels.poker.server.auth.CreateCredentialResult
import duels.poker.server.auth.CredentialKind
import duels.poker.server.auth.PresentedSecret
import duels.poker.server.session.PlayerId
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.sql.Types
import java.util.UUID
import javax.sql.DataSource
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Tests for [PostgresCredentials], against the container.
 *
 * `anUnknownIdentifierAnswersNothing` and `aWrongSecretAnswersNothing` both assert plain `null` —
 * the same value, not a distinguishable type or message — because `ADR-0027` §6 requires the two
 * to be indistinguishable to a caller. `aRowWithNoSecretHashAnswersNothingAndStillRunsTheDummy`
 * covers the third, easy-to-miss outcome: a row can exist with nothing to compare against, and
 * that path must cost what the other two null-returning paths cost.
 * `theDummyPhcUsedOnAMissingRowParsesAsAValidArgon2Hash` guards the countermeasure itself: every
 * behavioural test here would still pass if `DUMMY_PHC` were malformed — `matches` would just
 * return `false` faster — so this asserts the actual string handed to it parses.
 */
class PostgresCredentialsTest {
    private lateinit var dataSource: DataSource
    private lateinit var credentials: PostgresCredentials

    @BeforeEach
    fun setupDatabase() {
        dataSource = PostgresTestSupport.freshDatabase()
        Migrations.migrate(dataSource)
        credentials = PostgresCredentials(dataSource)
    }

    @Test
    fun aCredentialCreatedThenProvedAnswersThePlayerId() {
        runBlocking {
            val playerId = insertPlayer()
            val secret = PresentedSecret("correct horse")

            val created = credentials.create(playerId, CredentialKind.PASSWORD, "alice@example.com", secret)
            val verified = credentials.verify(CredentialKind.PASSWORD, "alice@example.com", secret)

            assertEquals(CreateCredentialResult.Created, created)
            assertEquals(1, countCredentialRows(CredentialKind.PASSWORD, "alice@example.com"), "create must write exactly one row")
            assertEquals(playerId, verified)
        }
    }

    @Test
    fun aWrongSecretAnswersNothing() {
        runBlocking {
            val playerId = insertPlayer()
            credentials.create(playerId, CredentialKind.PASSWORD, "alice@example.com", PresentedSecret("correct horse"))

            val verified = credentials.verify(CredentialKind.PASSWORD, "alice@example.com", PresentedSecret("wrong horse"))

            assertNull(verified)
        }
    }

    @Test
    fun anUnknownIdentifierAnswersNothing() {
        runBlocking {
            val verified = credentials.verify(CredentialKind.PASSWORD, "nobody@example.com", PresentedSecret("whatever"))

            assertNull(verified)
        }
    }

    @Test
    fun theSameIdentifierUnderAnotherKindAnswersNothing() {
        runBlocking {
            val playerId = insertPlayer()
            credentials.create(playerId, CredentialKind.PASSWORD, "alice@example.com", PresentedSecret("correct horse"))

            val verified = credentials.verify(CredentialKind("email"), "alice@example.com", PresentedSecret("correct horse"))

            assertNull(verified)
        }
    }

    @Test
    fun aSecondCredentialWithTheSameIdentifierAnswersIdentifierTaken() {
        runBlocking {
            val first = insertPlayer()
            val second = insertPlayer()
            val firstSecret = PresentedSecret("first secret")
            credentials.create(first, CredentialKind.PASSWORD, "alice@example.com", firstSecret)

            val result = credentials.create(second, CredentialKind.PASSWORD, "alice@example.com", PresentedSecret("second secret"))
            val stillFirst = credentials.verify(CredentialKind.PASSWORD, "alice@example.com", firstSecret)

            assertEquals(CreateCredentialResult.IdentifierTaken, result)
            assertEquals(first, stillFirst)
        }
    }

    @Test
    fun theStoredRowHoldsAPhcStringAndNotThePassword() {
        runBlocking {
            val playerId = insertPlayer()
            val secret = "correct horse battery staple"
            credentials.create(playerId, CredentialKind.PASSWORD, "alice@example.com", PresentedSecret(secret))

            val storedHash = requireNotNull(storedSecretHash(CredentialKind.PASSWORD, "alice@example.com"))

            assertNotEquals(secret, storedHash)
            assertFalse(storedHash.contains(secret), "stored hash must not contain the raw secret")
            assertNotNull(parseArgon2PhcOrNull(storedHash), "stored hash must be a PHC string this project's own parser accepts")
        }
    }

    @Test
    fun twoPlayersWithTheSamePasswordGetDifferentStoredHashes() {
        runBlocking {
            val first = insertPlayer()
            val second = insertPlayer()
            val secret = "correct horse battery staple"
            credentials.create(first, CredentialKind.PASSWORD, "alice@example.com", PresentedSecret(secret))
            credentials.create(second, CredentialKind.PASSWORD, "bob@example.com", PresentedSecret(secret))

            val firstHash = requireNotNull(storedSecretHash(CredentialKind.PASSWORD, "alice@example.com"))
            val secondHash = requireNotNull(storedSecretHash(CredentialKind.PASSWORD, "bob@example.com"))

            assertNotEquals(firstHash, secondHash)
            assertEquals(first, credentials.verify(CredentialKind.PASSWORD, "alice@example.com", PresentedSecret(secret)))
            assertEquals(second, credentials.verify(CredentialKind.PASSWORD, "bob@example.com", PresentedSecret(secret)))
        }
    }

    // The fourth outcome verify() has, distinct from "no row": a row exists but there is nothing
    // in it to compare against yet. An implementation that special-cases "no row" for the dummy
    // but returns early here would pass every test above and still leak this path's timing.
    @Test
    fun aRowWithNoSecretHashAnswersNothingAndStillRunsTheDummy() {
        runBlocking {
            val playerId = insertPlayer()
            insertCredentialWithNullSecretHash(playerId, CredentialKind.PASSWORD, "alice@example.com")
            val capturingHasher = CapturingMatchesHasher(Argon2Hasher())
            val credentialsWithCapture = PostgresCredentials(dataSource, capturingHasher)

            val verified = credentialsWithCapture.verify(CredentialKind.PASSWORD, "alice@example.com", PresentedSecret("whatever"))

            assertNull(verified)
            assertNotNull(
                capturingHasher.lastStoredPhc,
                "a row with a NULL secret_hash must still run the dummy verification, not answer null for free",
            )
        }
    }

    // Not one of the seven behavioural tests above: this one guards the countermeasure itself.
    // ADR-0027 §6 requires DUMMY_PHC to cost verify() one real Argon2 verification on the
    // no-such-account path; parseArgon2PhcOrNull(DUMMY_PHC) not being null is what makes that
    // true. A malformed constant makes hasher.matches short-circuit to false doing no Argon2 work
    // at all, and every behavioural test in this file would still go green.
    @Test
    fun theDummyPhcUsedOnAMissingRowParsesAsAValidArgon2Hash() {
        runBlocking {
            val capturingHasher = CapturingMatchesHasher(Argon2Hasher())
            val credentialsWithCapture = PostgresCredentials(dataSource, capturingHasher)

            credentialsWithCapture.verify(CredentialKind.PASSWORD, "nobody@example.com", PresentedSecret("whatever"))

            val dummyPhc =
                assertNotNull(
                    capturingHasher.lastStoredPhc,
                    "verify() must call hasher.matches() for a missing row, not answer null without running it",
                )
            assertNotNull(
                parseArgon2PhcOrNull(dummyPhc),
                "DUMMY_PHC must parse, or matches() returns false immediately with no Argon2 work done, and the " +
                    "enumeration timing defence silently stops running",
            )
        }
    }

    @Test
    fun aPlayerWithNoCredentialHoldsNone() {
        runBlocking {
            val playerId = insertPlayer()

            val holds = credentials.holdsCredential(playerId, CredentialKind.PASSWORD)

            assertFalse(holds, "a freshly inserted player row must answer false")
        }
    }

    @Test
    fun aPlayerHoldsThePasswordItJustCreated() {
        runBlocking {
            val playerId = insertPlayer()
            credentials.create(playerId, CredentialKind.PASSWORD, "alice@example.com", PresentedSecret("correct horse"))

            val holds = credentials.holdsCredential(playerId, CredentialKind.PASSWORD)

            assertEquals(true, holds, "after create succeeds, the same query must answer true")
        }
    }

    @Test
    fun aCredentialOfAnotherKindIsNotHeld() {
        runBlocking {
            val playerId = insertPlayer()
            credentials.create(playerId, CredentialKind.PASSWORD, "alice@example.com", PresentedSecret("correct horse"))

            val holdsPassword = credentials.holdsCredential(playerId, CredentialKind.PASSWORD)
            val holdsPasskey = credentials.holdsCredential(playerId, CredentialKind("passkey"))

            assertEquals(true, holdsPassword, "player holds password")
            assertEquals(false, holdsPasskey, "player does not hold passkey, so query for passkey must answer false")
        }
    }

    @Test
    fun onePlayersCredentialIsNotAnothers() {
        runBlocking {
            val firstPlayer = insertPlayer()
            val secondPlayer = insertPlayer()
            credentials.create(firstPlayer, CredentialKind.PASSWORD, "alice@example.com", PresentedSecret("correct horse"))

            val firstPlayerHolds = credentials.holdsCredential(firstPlayer, CredentialKind.PASSWORD)
            val secondPlayerHolds = credentials.holdsCredential(secondPlayer, CredentialKind.PASSWORD)

            assertEquals(true, firstPlayerHolds, "first player holds password")
            assertEquals(false, secondPlayerHolds, "second player holds nothing, so answer must be false even though first player has a password credential")
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

    private fun insertCredentialWithNullSecretHash(playerId: PlayerId, kind: CredentialKind, identifier: String) {
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                "INSERT INTO credential (id, player_id, kind, identifier, secret_hash) VALUES (?, ?, ?, ?, ?)",
            ).use { statement ->
                statement.setObject(1, UUID.randomUUID())
                statement.setObject(2, UUID.fromString(playerId.value))
                statement.setString(3, kind.value)
                statement.setString(4, identifier)
                statement.setNull(5, Types.VARCHAR)
                statement.executeUpdate()
            }
        }
    }

    private fun storedSecretHash(kind: CredentialKind, identifier: String): String? =
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                "SELECT secret_hash FROM credential WHERE kind = ? AND identifier = ?",
            ).use { statement ->
                statement.setString(1, kind.value)
                statement.setString(2, identifier)
                statement.executeQuery().use { rows ->
                    check(rows.next()) { "no credential row for kind=${kind.value} identifier=$identifier" }
                    rows.getString(1)
                }
            }
        }

    private fun countCredentialRows(kind: CredentialKind, identifier: String): Int =
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                "SELECT COUNT(*) FROM credential WHERE kind = ? AND identifier = ?",
            ).use { statement ->
                statement.setString(1, kind.value)
                statement.setString(2, identifier)
                statement.executeQuery().use { rows ->
                    rows.next()
                    rows.getInt(1)
                }
            }
        }
}

/**
 * A [SecretHasher] that delegates to a real one but remembers the last `storedPhc` passed to
 * [matches], so a test can inspect what [PostgresCredentials] actually compared against without
 * reading its private `DUMMY_PHC` constant directly.
 */
private class CapturingMatchesHasher(private val delegate: SecretHasher) : SecretHasher {
    var lastStoredPhc: String? = null
        private set

    override suspend fun hash(secret: PresentedSecret): String = delegate.hash(secret)

    override suspend fun matches(secret: PresentedSecret, storedPhc: String): Boolean {
        lastStoredPhc = storedPhc
        return delegate.matches(secret, storedPhc)
    }
}
