package duels.poker.server.db

import duels.poker.server.auth.CredentialKind
import duels.poker.server.auth.EmailAddress
import duels.poker.server.auth.ResetRecipient
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
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Tests for [PostgresRecoveryEmails.hasRecoveryEmail], [PostgresRecoveryEmails.verifiedOwnerOf] and
 * [PostgresRecoveryEmails.resetRecipientOf], against the container.
 *
 * The first two reads answer from `recovery_email` alone and the third joins it onto `credential`,
 * but none has any clock-dependent behaviour of its own, so this file needs no mutable clock the
 * way [PostgresRecoveryEmailsVerifyTest] does — a fixed one is enough, and `resetRecipientOf` reads
 * no timestamp column from either table. That also settles the *expired* leg of the
 * pending/expired/somebody-else's triad this ticket names: `expires_at` lives only in
 * `email_verification`, which none of the three statements under test ever reads, so a pending row
 * that has passed its day and one that has not answer identically here — both are simply absent
 * from `recovery_email`. Backdating the clock before a claim, the trick that builds a genuinely
 * expired row for `verifyPending` (`TASK-041609`), would add a fixture with no assertion the plain
 * pending case below does not already make. That leg belongs to the `verifyPending` boundary, not
 * this one.
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

    @Test
    fun aVerifiedAddressAnswersItsOwnersIdAndHandle() {
        runBlocking {
            val playerId = insertPlayer()
            insertCredential(playerId, CredentialKind.PASSWORD, "alice-handle")
            val token = VerificationToken("reset-token")
            recoveryEmails.claimPending(playerId, EmailAddress("alice@x.test"), token)
            recoveryEmails.verifyPending(token)

            val recipient = recoveryEmails.resetRecipientOf(EmailAddress("alice@x.test"))

            val found = assertNotNull(recipient, "a verified address with a password credential must answer a recipient")
            assertEquals(playerId, found.playerId, "the recipient's id must be the address's owner")
            assertEquals("alice-handle", found.handle, "the recipient's handle must be the identifier inserted for that owner")
        }
    }

    // A constant handle, a query that ignores its parameter, or one that joins on the wrong column
    // all pass a single-owner fixture. Two owners with two different handles is what forces the
    // statement to actually read c.identifier for the row it was asked about.
    @Test
    fun oneOwnersHandleIsNotAnothers() {
        runBlocking {
            val firstPlayer = insertPlayer()
            val secondPlayer = insertPlayer()
            insertCredential(firstPlayer, CredentialKind.PASSWORD, "alice-handle")
            insertCredential(secondPlayer, CredentialKind.PASSWORD, "bob-handle")
            val firstToken = VerificationToken("first-reset-token")
            val secondToken = VerificationToken("second-reset-token")
            recoveryEmails.claimPending(firstPlayer, EmailAddress("alice@x.test"), firstToken)
            recoveryEmails.claimPending(secondPlayer, EmailAddress("bob@x.test"), secondToken)
            recoveryEmails.verifyPending(firstToken)
            recoveryEmails.verifyPending(secondToken)

            val firstRecipient = recoveryEmails.resetRecipientOf(EmailAddress("alice@x.test"))
            val secondRecipient = recoveryEmails.resetRecipientOf(EmailAddress("bob@x.test"))

            assertEquals(
                ResetRecipient(firstPlayer, "alice-handle"),
                firstRecipient,
                "the first address must answer the first owner's id and handle",
            )
            assertEquals(
                ResetRecipient(secondPlayer, "bob-handle"),
                secondRecipient,
                "the second address must answer the second owner's id and handle, not the first",
            )
        }
    }

    // The security property is that a pending address is indistinguishable from one nobody has
    // ever mentioned, exactly as aPendingAddressIsFoundByNobody argues for verifiedOwnerOf — so
    // this asserts both individually and against each other, in the same test and database.
    @Test
    fun aPendingAddressAndAnUnknownOneBothAnswerNothing() {
        runBlocking {
            val pendingPlayer = insertPlayer()
            recoveryEmails.claimPending(pendingPlayer, EmailAddress("pending@x.test"), VerificationToken("pending-reset-token"))

            val pendingRecipient = recoveryEmails.resetRecipientOf(EmailAddress("pending@x.test"))
            val unknownRecipient = recoveryEmails.resetRecipientOf(EmailAddress("never-mentioned@x.test"))

            assertNull(pendingRecipient, "a claimed-but-unverified address must answer no recipient")
            assertNull(unknownRecipient, "an address nobody has ever mentioned must answer no recipient")
            assertEquals(
                unknownRecipient,
                pendingRecipient,
                "a pending address must answer exactly what an unknown one does, not merely also null",
            )
        }
    }

    // The JOIN-not-LEFT-JOIN test and the c.kind = 'password' test, together: an owner with no
    // credential row at all catches an outer join, and an owner with only an oauth credential
    // catches a join with the kind predicate dropped.
    @Test
    fun anOwnerWithNoPasswordCredentialAnswersNothing() {
        runBlocking {
            val noCredentialPlayer = insertPlayer()
            val oauthOnlyPlayer = insertPlayer()
            insertCredential(oauthOnlyPlayer, CredentialKind("oauth:google"), "alice@gmail.com")
            val noCredentialToken = VerificationToken("no-credential-reset-token")
            val oauthToken = VerificationToken("oauth-reset-token")
            recoveryEmails.claimPending(noCredentialPlayer, EmailAddress("no-credential@x.test"), noCredentialToken)
            recoveryEmails.claimPending(oauthOnlyPlayer, EmailAddress("oauth-only@x.test"), oauthToken)
            recoveryEmails.verifyPending(noCredentialToken)
            recoveryEmails.verifyPending(oauthToken)

            val noCredentialRecipient = recoveryEmails.resetRecipientOf(EmailAddress("no-credential@x.test"))
            val oauthOnlyRecipient = recoveryEmails.resetRecipientOf(EmailAddress("oauth-only@x.test"))

            assertNull(noCredentialRecipient, "a verified address whose owner holds no credential at all must answer no recipient")
            assertNull(
                oauthOnlyRecipient,
                "a verified address whose owner holds only a non-password credential must answer no recipient",
            )
        }
    }

    // The ASCII pair proves only that some fold happens. U+0130 is the character und-x-icu and the
    // container's musl default disagree about, so DOTTED is stored and both DOTTED and its
    // und-x-icu fold, FOLDED, are asked — together they catch a COLLATE dropped from either the
    // parameter half or the column half of SELECT_RESET_RECIPIENT_SQL, which an ASCII fixture
    // cannot see. Written as unicode escapes, never literal glyphs, so the fixture survives any
    // editor.
    @Test
    fun theHandleIsFoundWhateverCaseTheAddressIsAskedIn() {
        runBlocking {
            val dotted = "\u0130"
            val folded = "i\u0307"
            val asciiPlayer = insertPlayer()
            val dottedPlayer = insertPlayer()
            insertCredential(asciiPlayer, CredentialKind.PASSWORD, "ascii-handle")
            insertCredential(dottedPlayer, CredentialKind.PASSWORD, "dotted-handle")
            val asciiToken = VerificationToken("ascii-case-reset-token")
            val dottedToken = VerificationToken("dotted-case-reset-token")
            recoveryEmails.claimPending(asciiPlayer, EmailAddress("Bob@Example.com"), asciiToken)
            recoveryEmails.claimPending(dottedPlayer, EmailAddress("$dotted@x.test"), dottedToken)
            recoveryEmails.verifyPending(asciiToken)
            recoveryEmails.verifyPending(dottedToken)

            val shoutedAscii = recoveryEmails.resetRecipientOf(EmailAddress("BOB@example.COM"))
            val exactAscii = recoveryEmails.resetRecipientOf(EmailAddress("Bob@Example.com"))
            val exactDotted = recoveryEmails.resetRecipientOf(EmailAddress("$dotted@x.test"))
            val foldedDotted = recoveryEmails.resetRecipientOf(EmailAddress("$folded@x.test"))

            val asciiRecipient = ResetRecipient(asciiPlayer, "ascii-handle")
            val dottedRecipient = ResetRecipient(dottedPlayer, "dotted-handle")
            assertEquals(asciiRecipient, shoutedAscii, "a differently-cased ASCII spelling must resolve to its owner's recipient")
            assertEquals(asciiRecipient, exactAscii, "the exact stored ASCII spelling must resolve to its owner's recipient")
            assertEquals(
                dottedRecipient,
                exactDotted,
                "the exact stored spelling must resolve to its owner's recipient under the pinned collation",
            )
            assertEquals(
                dottedRecipient,
                foldedDotted,
                "the und-x-icu fold of the stored spelling must resolve to its owner's recipient too",
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

    // The raw INSERT four other db test files already use. Never PostgresCredentials.create:
    // that runs Argon2 on every call, and resetRecipientOf reads only c.identifier, never
    // secret_hash, so a real hash would buy nothing and cost seconds.
    private fun insertCredential(playerId: PlayerId, kind: CredentialKind, identifier: String) {
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                "INSERT INTO credential (id, player_id, kind, identifier, secret_hash) VALUES (?, ?, ?, ?, ?)",
            ).use { statement ->
                statement.setObject(1, UUID.randomUUID())
                statement.setObject(2, UUID.fromString(playerId.value))
                statement.setString(3, kind.value)
                statement.setString(4, identifier)
                statement.setString(5, "not-a-real-hash")
                statement.executeUpdate()
            }
        }
    }
}
