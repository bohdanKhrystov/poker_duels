package duels.poker.server.db

import duels.poker.server.auth.EmailAddress
import duels.poker.server.auth.VerificationToken
import duels.poker.server.session.PlayerId
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID
import javax.sql.DataSource
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Tests for [PostgresRecoveryEmails.hasRecoveryEmail] and [PostgresRecoveryEmails.verifiedOwnerOf],
 * against the container.
 *
 * Both reads answer from `recovery_email` alone and have no clock-dependent behaviour of their
 * own, so this file needs no mutable clock the way [PostgresRecoveryEmailsVerifyTest] does — a
 * fixed one is enough. That also settles the *expired* leg of the pending/expired/somebody-else's
 * triad this ticket names: `expires_at` lives only in `email_verification`, which neither
 * statement under test ever reads, so a pending row that has passed its day and one that has not
 * answer identically here — both are simply absent from `recovery_email`. Backdating the clock
 * before a claim, the trick that builds a genuinely expired row for `verifyPending`
 * (`TASK-041609`), would add a fixture with no assertion the plain pending case below does not
 * already make. That leg belongs to the `verifyPending` boundary, not this one.
 */
class PostgresRecoveryEmailsReadsTest {
    private lateinit var dataSource: DataSource
    private lateinit var recoveryEmails: PostgresRecoveryEmails

    @BeforeEach
    fun setupDatabase() {
        dataSource = PostgresTestSupport.freshDatabase()
        Migrations.migrate(dataSource)
        recoveryEmails = PostgresRecoveryEmails(dataSource, Clock.fixed(Instant.now(), ZoneOffset.UTC))
    }

    @Test
    fun aVerifiedAddressIsFoundByItsOwner() {
        runBlocking {
            val playerId = insertPlayer()
            val token = VerificationToken("verify-token")
            recoveryEmails.claimPending(playerId, EmailAddress("bob@x.test"), token)
            recoveryEmails.verifyPending(token)

            val hasEmail = recoveryEmails.hasRecoveryEmail(playerId)
            val owner = recoveryEmails.verifiedOwnerOf(EmailAddress("bob@x.test"))

            assertEquals(true, hasEmail, "a verified player must read hasRecoveryEmail true")
            assertEquals(playerId, owner, "the verified address must resolve to its owner")
        }
    }

    // The security property is that a pending address is indistinguishable from one nobody has
    // ever mentioned — a claim about two answers agreeing, not each merely happening to be the
    // "empty" value. So this asserts both individually (pinned to false/null) and against a
    // second, never-claimed player and address, in the same test and the same database.
    @Test
    fun aPendingAddressIsFoundByNobody() {
        runBlocking {
            val pendingPlayer = insertPlayer()
            val neverMentionedPlayer = insertPlayer()
            val pendingAddress = EmailAddress("pending@x.test")
            val neverMentionedAddress = EmailAddress("never-mentioned@x.test")
            recoveryEmails.claimPending(pendingPlayer, pendingAddress, VerificationToken("pending-token"))

            val pendingHasEmail = recoveryEmails.hasRecoveryEmail(pendingPlayer)
            val pendingOwner = recoveryEmails.verifiedOwnerOf(pendingAddress)
            val neverMentionedHasEmail = recoveryEmails.hasRecoveryEmail(neverMentionedPlayer)
            val neverMentionedOwner = recoveryEmails.verifiedOwnerOf(neverMentionedAddress)

            assertEquals(false, pendingHasEmail, "a claimed-but-unverified player must read hasRecoveryEmail false")
            assertNull(pendingOwner, "a claimed-but-unverified address must resolve to no owner")
            assertEquals(
                neverMentionedHasEmail,
                pendingHasEmail,
                "a pending player's hasRecoveryEmail must equal a never-mentioned player's, not merely also be false",
            )
            assertEquals(
                neverMentionedOwner,
                pendingOwner,
                "a pending address's owner must equal a never-mentioned address's, not merely also be null",
            )
        }
    }

    @Test
    fun anAddressIsFoundWhateverCaseItIsAskedIn() {
        runBlocking {
            val playerId = insertPlayer()
            val token = VerificationToken("case-token")
            recoveryEmails.claimPending(playerId, EmailAddress("Bob@Example.com"), token)
            recoveryEmails.verifyPending(token)

            val shoutedCase = recoveryEmails.verifiedOwnerOf(EmailAddress("BOB@example.COM"))
            val exactCase = recoveryEmails.verifiedOwnerOf(EmailAddress("Bob@Example.com"))

            assertEquals(playerId, shoutedCase, "a differently-cased spelling must still resolve to the owner")
            assertEquals(playerId, exactCase, "the exact stored spelling must resolve to the owner")
        }
    }

    // Guards against a query that ignores its parameter: a verifiedOwnerOf that returns the first
    // row in the table would pass every other test in this file with just one verified row.
    @Test
    fun onePlayersAddressIsNotAnothers() {
        runBlocking {
            val firstPlayer = insertPlayer()
            val secondPlayer = insertPlayer()
            val firstToken = VerificationToken("first-token")
            val secondToken = VerificationToken("second-token")
            recoveryEmails.claimPending(firstPlayer, EmailAddress("first@x.test"), firstToken)
            recoveryEmails.claimPending(secondPlayer, EmailAddress("second@x.test"), secondToken)
            recoveryEmails.verifyPending(firstToken)
            recoveryEmails.verifyPending(secondToken)

            val firstOwner = recoveryEmails.verifiedOwnerOf(EmailAddress("first@x.test"))
            val secondOwner = recoveryEmails.verifiedOwnerOf(EmailAddress("second@x.test"))

            assertEquals(firstPlayer, firstOwner, "the first address must resolve to the first player, not the second")
            assertEquals(secondPlayer, secondOwner, "the second address must resolve to the second player, not the first")
            assertEquals(
                true,
                recoveryEmails.hasRecoveryEmail(firstPlayer),
                "the first player must read hasRecoveryEmail true",
            )
            assertEquals(
                true,
                recoveryEmails.hasRecoveryEmail(secondPlayer),
                "the second player must read hasRecoveryEmail true",
            )
        }
    }

    // The correlated-versus-uncorrelated trap: an uncorrelated EXISTS (SELECT 1 FROM
    // recovery_email), with no WHERE, would answer true for every player once any one row exists.
    // Two players in one database is what a single verified fixture cannot catch.
    @Test
    fun aDetachedPlayerReadsFalseAgain() {
        runBlocking {
            val verifiedPlayer = insertPlayer()
            val neverClaimedPlayer = insertPlayer()
            val token = VerificationToken("detach-token")
            recoveryEmails.claimPending(verifiedPlayer, EmailAddress("verified@x.test"), token)
            recoveryEmails.verifyPending(token)

            val verifiedHasEmail = recoveryEmails.hasRecoveryEmail(verifiedPlayer)
            val neverClaimedHasEmail = recoveryEmails.hasRecoveryEmail(neverClaimedPlayer)

            assertEquals(true, verifiedHasEmail, "the verified player must read hasRecoveryEmail true")
            assertEquals(false, neverClaimedHasEmail, "a player who never claimed must read hasRecoveryEmail false")
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
