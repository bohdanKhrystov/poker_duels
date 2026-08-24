package duels.poker.server.db

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
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Tests that the no-such-account path costs exactly what the wrong-secret path costs, proven by
 * counting Argon2 verifications rather than by timing them (`ADR-0027` §6).
 *
 * [CountingSecretHasher] delegates every call to a real [Argon2Hasher] — so counting its calls
 * proves the same Argon2 work ran, never a stub standing in for it — and records, per call, the
 * [PresentedSecret] and `storedPhc` it was given, so a version that skips the dummy verification
 * on either path is caught by a count rather than a clock.
 *
 * **Stated limitation (`ADR-0054`):** counting calls proves *how many* verifications happen,
 * never *how expensive* each one is. A dummy PHC constant left stale the day the Argon2 cost
 * parameters are raised would still parse — at the old, cheaper cost — and this file would still
 * see one call on each path and pass, while the timing gap it exists to close reopens.
 */
class PostgresCredentialsEnumerationTest {
    private lateinit var dataSource: DataSource
    private lateinit var credentials: PostgresCredentials

    @BeforeEach
    fun setupDatabase() {
        dataSource = PostgresTestSupport.freshDatabase()
        Migrations.migrate(dataSource)
        credentials = PostgresCredentials(dataSource)
    }

    @Test
    fun anUnknownIdentifierRunsTheSameOneVerificationAWrongSecretDoes() {
        runBlocking {
            val playerId = insertPlayer()
            credentials.create(playerId, CredentialKind.PASSWORD, "alice@example.com", PresentedSecret("correct horse"))

            val wrongSecretHasher = CountingSecretHasher()
            val wrongSecretResult =
                PostgresCredentials(dataSource, wrongSecretHasher)
                    .verify(CredentialKind.PASSWORD, "alice@example.com", PresentedSecret("wrong horse"))

            val unknownIdentifierHasher = CountingSecretHasher()
            val unknownIdentifierResult =
                PostgresCredentials(dataSource, unknownIdentifierHasher)
                    .verify(CredentialKind.PASSWORD, "nobody@example.com", PresentedSecret("whatever"))

            assertNull(wrongSecretResult)
            assertNull(unknownIdentifierResult)
            assertEquals(1, wrongSecretHasher.matchesCalls.size, "a wrong secret must cost exactly one matches() call")
            assertEquals(0, wrongSecretHasher.hashCalls.size, "verify() must never call hash()")
            assertEquals(1, unknownIdentifierHasher.matchesCalls.size, "an unknown identifier must cost exactly one matches() call")
            assertEquals(0, unknownIdentifierHasher.hashCalls.size, "verify() must never call hash()")
            assertEquals(
                wrongSecretHasher.matchesCalls.size,
                unknownIdentifierHasher.matchesCalls.size,
                "an unknown identifier must cost the same number of verifications as a wrong secret",
            )
        }
    }

    @Test
    fun theDummyStringTheUnknownPathVerifiesAgainstIsWellFormed() {
        runBlocking {
            val hasher = CountingSecretHasher()

            PostgresCredentials(dataSource, hasher)
                .verify(CredentialKind.PASSWORD, "nobody@example.com", PresentedSecret("whatever"))

            assertEquals(1, hasher.matchesCalls.size, "verify() must call matches() exactly once for an unknown identifier")
            val recordedStoredPhc = hasher.matchesCalls.first().storedPhc

            assertNotNull(
                parseArgon2PhcOrNull(recordedStoredPhc),
                "the dummy PHC the unknown-identifier path actually verified against must parse, or matches() " +
                    "returns false before doing any Argon2 work and the enumeration defence silently stops running",
            )
        }
    }

    @Test
    fun theDummyVerificationIsGivenThePresentedSecret() {
        runBlocking {
            val presented = PresentedSecret("a secret only this test presents")
            val hasher = CountingSecretHasher()

            PostgresCredentials(dataSource, hasher)
                .verify(CredentialKind.PASSWORD, "nobody@example.com", presented)

            assertEquals(1, hasher.matchesCalls.size, "verify() must call matches() exactly once for an unknown identifier")
            assertEquals(presented, hasher.matchesCalls.first().presented)
        }
    }

    @Test
    fun aRowWhoseSecretHashIsNullStillCostsAVerification() {
        runBlocking {
            val playerId = insertPlayer()
            insertCredentialWithNullSecretHash(playerId, CredentialKind.PASSWORD, "alice@example.com")
            val hasher = CountingSecretHasher()

            val result =
                PostgresCredentials(dataSource, hasher)
                    .verify(CredentialKind.PASSWORD, "alice@example.com", PresentedSecret("whatever"))

            assertNull(result)
            assertEquals(1, hasher.matchesCalls.size, "a row whose secret_hash is NULL must still cost one matches() call")
            assertEquals(0, hasher.hashCalls.size, "verify() must never call hash()")
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
}

/**
 * A [SecretHasher] that delegates every call to a real [Argon2Hasher] — so counting its calls
 * proves the same Argon2 work ran rather than a stub standing in for it — while recording, per
 * call, the [PresentedSecret] and `storedPhc` [matches] was given.
 */
private class CountingSecretHasher(private val delegate: SecretHasher = Argon2Hasher()) : SecretHasher {
    private val recordedMatchesCalls = mutableListOf<MatchesCall>()
    private val recordedHashCalls = mutableListOf<PresentedSecret>()

    val matchesCalls: List<MatchesCall> get() = recordedMatchesCalls
    val hashCalls: List<PresentedSecret> get() = recordedHashCalls

    override suspend fun hash(secret: PresentedSecret): String {
        recordedHashCalls.add(secret)
        return delegate.hash(secret)
    }

    override suspend fun matches(secret: PresentedSecret, storedPhc: String): Boolean {
        recordedMatchesCalls.add(MatchesCall(secret, storedPhc))
        return delegate.matches(secret, storedPhc)
    }

    data class MatchesCall(val presented: PresentedSecret, val storedPhc: String)
}
