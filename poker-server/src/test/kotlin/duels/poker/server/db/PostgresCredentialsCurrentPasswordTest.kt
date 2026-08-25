package duels.poker.server.db

import duels.poker.server.auth.CredentialKind
import duels.poker.server.auth.PresentedSecret
import duels.poker.server.session.PlayerId
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID
import javax.sql.DataSource
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for [PostgresCredentials.verifyCurrent], against the container.
 *
 * `anotherPlayersPasswordIsRefused` is the load-bearing test here: it holds **two** players with
 * **two different** secrets and checks all four combinations, so a read that ignores `player_id`
 * — matching the hash against any row of the right `kind` — has somewhere to fail. A single-player
 * fixture cannot see that defect, because a wrong secret already answers `false` for the right
 * reason.
 *
 * `anOverlongSecretIsRefusedWithoutHashing` presents a secret against a real, existing credential:
 * the *without hashing* half is not observable from the return value alone (a 129-code-point
 * secret would fail to match the stored hash regardless), so it is a review criterion rather than
 * an assertion — named in the ticket rather than left to be discovered.
 */
class PostgresCredentialsCurrentPasswordTest {
    private lateinit var dataSource: DataSource
    private lateinit var credentials: PostgresCredentials

    @BeforeEach
    fun setupDatabase() {
        dataSource = PostgresTestSupport.freshDatabase()
        Migrations.migrate(dataSource)
        credentials = PostgresCredentials(dataSource)
    }

    @Test
    fun theRightPasswordIsAccepted() {
        runBlocking {
            val playerId = insertPlayer()
            val secret = PresentedSecret("correct horse")
            credentials.create(playerId, CredentialKind.PASSWORD, "alice@example.com", secret)

            val verified = credentials.verifyCurrent(playerId, CredentialKind.PASSWORD, secret)

            assertTrue(verified, "the secret used at creation must verify as the current password")
        }
    }

    @Test
    fun theWrongPasswordIsRefused() {
        runBlocking {
            val playerId = insertPlayer()
            credentials.create(playerId, CredentialKind.PASSWORD, "alice@example.com", PresentedSecret("correct horse"))

            val verified = credentials.verifyCurrent(playerId, CredentialKind.PASSWORD, PresentedSecret("wrong horse"))

            assertFalse(verified, "a different secret than the one stored must not verify")
        }
    }

    @Test
    fun anotherPlayersPasswordIsRefused() {
        runBlocking {
            val alice = insertPlayer()
            val bob = insertPlayer()
            val aliceSecret = PresentedSecret("alice's correct horse")
            val bobSecret = PresentedSecret("bob's correct battery")
            credentials.create(alice, CredentialKind.PASSWORD, "alice@example.com", aliceSecret)
            credentials.create(bob, CredentialKind.PASSWORD, "bob@example.com", bobSecret)

            assertTrue(
                credentials.verifyCurrent(alice, CredentialKind.PASSWORD, aliceSecret),
                "alice's own secret must verify as alice's current password",
            )
            assertTrue(
                credentials.verifyCurrent(bob, CredentialKind.PASSWORD, bobSecret),
                "bob's own secret must verify as bob's current password",
            )
            assertFalse(
                credentials.verifyCurrent(bob, CredentialKind.PASSWORD, aliceSecret),
                "alice's secret must not verify as bob's current password",
            )
            assertFalse(
                credentials.verifyCurrent(alice, CredentialKind.PASSWORD, bobSecret),
                "bob's secret must not verify as alice's current password",
            )
        }
    }

    @Test
    fun aPlayerWithNoCredentialIsRefused() {
        runBlocking {
            val playerId = insertPlayer()

            val verified = credentials.verifyCurrent(playerId, CredentialKind.PASSWORD, PresentedSecret("whatever"))

            assertFalse(verified, "a player who never created a password credential must answer false, not throw")
        }
    }

    @Test
    fun anOverlongSecretIsRefusedWithoutHashing() {
        runBlocking {
            val playerId = insertPlayer()
            credentials.create(playerId, CredentialKind.PASSWORD, "alice@example.com", PresentedSecret("correct horse"))
            val overlong = PresentedSecret("a".repeat(129))

            val verified = credentials.verifyCurrent(playerId, CredentialKind.PASSWORD, overlong)

            assertFalse(verified, "a secret over the 128-code-point work bound must be refused")
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
