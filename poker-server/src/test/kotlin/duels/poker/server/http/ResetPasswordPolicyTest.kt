package duels.poker.server.http

import duels.poker.server.auth.CredentialKind
import duels.poker.server.auth.PresentedSecret
import duels.poker.server.auth.RecoveryTokens
import duels.poker.server.auth.ResetToken
import duels.poker.server.db.Migrations
import duels.poker.server.db.PostgresCredentials
import duels.poker.server.db.PostgresPasswordResets
import duels.poker.server.db.PostgresRecoveryEmails
import duels.poker.server.db.PostgresTestSupport
import duels.poker.server.module
import duels.poker.server.session.PlayerId
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Clock
import java.util.UUID
import javax.sql.DataSource

/** The login handle every fixture credential in this file is created under. */
private const val IDENTIFIER = "reset-policy-test-handle"

/**
 * A minted (status, body, header names) triple, compared by [equals] rather than by any single
 * field — `ADR-0080` §3's property is about the whole answer a caller receives, not merely its
 * status line. Header **names** rather than headers: a response's `Date` differs request to
 * request by value but never by which headers are present, so comparing names is the equality
 * that a timing difference between two otherwise-identical requests cannot fail.
 */
private data class ResponseShape(
    val status: HttpStatusCode,
    val body: String,
    val headerNames: Set<String>,
)

private suspend fun HttpResponse.shape(): ResponseShape = ResponseShape(status, bodyAsText(), headers.names())

/**
 * Tests for `POST /api/auth/reset-password`'s password policy, installed by [recoveryRoutes] and
 * driven against a real [PostgresPasswordResets] and a real [PostgresCredentials] — reusing
 * [ResetPasswordRouteTest]'s fixture shape, since that file already establishes this route has no
 * double of its own to stand in for either port.
 *
 * `ADR-0080` §1 fixes the order this route now runs in: decode, judge the password with
 * [duels.poker.server.auth.passwordIsLongEnough] and
 * [duels.poker.server.auth.passwordIsWithinTheWorkBound], and only then spend the token. Every
 * token this file mints is issued through [PostgresPasswordResets.issue] over `Clock.systemUTC()`,
 * exactly as [ResetPasswordRouteTest]'s own fixture does — `ADR-0080` §6 names the hazard this
 * avoids: an `expires_at` pinned to a fixed, distant instant would turn every `204` below into a
 * `400` for a reason that has nothing to do with the policy this file tests, because the `DELETE`
 * inside [PostgresPasswordResets.consume] compares `expires_at` against the **database's** own
 * `now()`, not the injected [Clock].
 */
class ResetPasswordPolicyTest {
    private lateinit var dataSource: DataSource
    private lateinit var passwordResets: PostgresPasswordResets
    private lateinit var credentials: PostgresCredentials
    private lateinit var recoveryEmails: PostgresRecoveryEmails
    private lateinit var tokenMinter: RecoveryTokens

    @BeforeEach
    fun setupDatabase() {
        dataSource = PostgresTestSupport.freshDatabase()
        Migrations.migrate(dataSource)
        tokenMinter = RecoveryTokens()
        passwordResets = PostgresPasswordResets(dataSource, Clock.systemUTC(), tokenMinter)
        credentials = PostgresCredentials(dataSource)
        recoveryEmails = PostgresRecoveryEmails(dataSource, Clock.systemUTC())
    }

    @Test
    fun aSevenCodePointPasswordAnswersFourHundredAndTwentyTwo() {
        testApplication {
            application {
                module()
                recoveryRoutes(recoveryEmails, passwordResets, identitiesFor(emptyMap()), RecordingCredentials())
            }
            val playerId = insertPlayer()
            val oldPassword = PresentedSecret("original password, eight or more")
            credentials.create(playerId, CredentialKind.PASSWORD, IDENTIFIER, oldPassword)
            val token = issueLiveToken(playerId)

            val response = client.post("/api/auth/reset-password") {
                header(HttpHeaders.ContentType, "application/json")
                setBody("""{"token":"${token.value}","newPassword":"1234567"}""")
            }

            assertEquals(HttpStatusCode.UnprocessableEntity, response.status)
            assertEquals("", response.bodyAsText())
            assertEquals(
                playerId,
                credentials.verify(CredentialKind.PASSWORD, IDENTIFIER, oldPassword),
                "the old password must still verify after a refused password",
            )
        }
    }

    @Test
    fun anOneHundredAndTwentyNineCodePointPasswordAnswersFourHundredAndTwentyTwo() {
        testApplication {
            application {
                module()
                recoveryRoutes(recoveryEmails, passwordResets, identitiesFor(emptyMap()), RecordingCredentials())
            }
            val playerId = insertPlayer()
            val oldPassword = PresentedSecret("original password, eight or more")
            credentials.create(playerId, CredentialKind.PASSWORD, IDENTIFIER, oldPassword)
            val token = issueLiveToken(playerId)
            val tooLong = "a".repeat(129)

            val response = client.post("/api/auth/reset-password") {
                header(HttpHeaders.ContentType, "application/json")
                setBody("""{"token":"${token.value}","newPassword":"$tooLong"}""")
            }

            assertEquals(HttpStatusCode.UnprocessableEntity, response.status)
            assertEquals("", response.bodyAsText())
            assertEquals(
                playerId,
                credentials.verify(CredentialKind.PASSWORD, IDENTIFIER, oldPassword),
                "the old password must still verify after a refused password",
            )
        }
    }

    @Test
    fun anEightCodePointPasswordIsAccepted() {
        testApplication {
            application {
                module()
                recoveryRoutes(recoveryEmails, passwordResets, identitiesFor(emptyMap()), RecordingCredentials())
            }
            val playerId = insertPlayer()
            credentials.create(
                playerId,
                CredentialKind.PASSWORD,
                IDENTIFIER,
                PresentedSecret("original password, eight plus"),
            )
            val token = issueLiveToken(playerId)

            val response = client.post("/api/auth/reset-password") {
                header(HttpHeaders.ContentType, "application/json")
                setBody("""{"token":"${token.value}","newPassword":"12345678"}""")
            }

            assertEquals(HttpStatusCode.NoContent, response.status)
            assertEquals("", response.bodyAsText())
        }
    }

    @Test
    fun fourAstralCharactersAreFourCodePointsAndAreRefused() {
        testApplication {
            application {
                module()
                recoveryRoutes(recoveryEmails, passwordResets, identitiesFor(emptyMap()), RecordingCredentials())
            }
            val playerId = insertPlayer()
            credentials.create(
                playerId,
                CredentialKind.PASSWORD,
                IDENTIFIER,
                PresentedSecret("original password, eight plus"),
            )
            val token = issueLiveToken(playerId)
            // U+1F600 GRINNING FACE, a surrogate pair: four of them are 8 UTF-16 units and 4 code
            // points. String.length reads 8 here and would wrongly pass an 8-character floor.
            val fourAstralCharacters = "😀".repeat(4)

            val response = client.post("/api/auth/reset-password") {
                header(HttpHeaders.ContentType, "application/json")
                setBody("""{"token":"${token.value}","newPassword":"$fourAstralCharacters"}""")
            }

            assertEquals(HttpStatusCode.UnprocessableEntity, response.status)
            assertEquals("", response.bodyAsText())
        }
    }

    @Test
    fun aRefusedPasswordLeavesTheTokenUsableOnTheNextRequest() {
        testApplication {
            application {
                module()
                recoveryRoutes(recoveryEmails, passwordResets, identitiesFor(emptyMap()), RecordingCredentials())
            }
            val playerId = insertPlayer()
            credentials.create(
                playerId,
                CredentialKind.PASSWORD,
                IDENTIFIER,
                PresentedSecret("original password, eight plus"),
            )
            val token = issueLiveToken(playerId)

            val refused = client.post("/api/auth/reset-password") {
                header(HttpHeaders.ContentType, "application/json")
                setBody("""{"token":"${token.value}","newPassword":"1234567"}""")
            }
            assertEquals(HttpStatusCode.UnprocessableEntity, refused.status)

            // ADR-0080 §4: a 422 leaves token_hash, issued_at and expires_at exactly as they
            // were — nothing about password_reset is written on a refusal, so the same token
            // still names a live row here, and an ordinary mistake at the password field costs
            // the player nothing but a second submission.
            val accepted = client.post("/api/auth/reset-password") {
                header(HttpHeaders.ContentType, "application/json")
                setBody("""{"token":"${token.value}","newPassword":"12345678"}""")
            }
            assertEquals(HttpStatusCode.NoContent, accepted.status)
        }
    }

    @Test
    fun aFabricatedTokenAndALiveTokenAnswerTheSameFourHundredAndTwentyTwo() {
        testApplication {
            application {
                module()
                recoveryRoutes(recoveryEmails, passwordResets, identitiesFor(emptyMap()), RecordingCredentials())
            }
            val playerId = insertPlayer()
            credentials.create(
                playerId,
                CredentialKind.PASSWORD,
                IDENTIFIER,
                PresentedSecret("original password, eight plus"),
            )
            val liveToken = issueLiveToken(playerId)
            // Minted by the same generator as liveToken, but never handed to
            // passwordResets.issue: no row backs it. Sharing the generator — rather than a
            // literal string — is what gives it the same length and alphabet as liveToken, so
            // the only difference between the two requests below is whether a row exists.
            val fabricatedToken = tokenMinter.newResetToken()

            val liveResponse = client.post("/api/auth/reset-password") {
                header(HttpHeaders.ContentType, "application/json")
                setBody("""{"token":"${liveToken.value}","newPassword":"1234567"}""")
            }
            val fabricatedResponse = client.post("/api/auth/reset-password") {
                header(HttpHeaders.ContentType, "application/json")
                setBody("""{"token":"${fabricatedToken.value}","newPassword":"1234567"}""")
            }

            // ADR-0080 §3: the branch is chosen entirely by the caller's own password, so
            // 400-versus-422 reports nothing about password_reset — checked here as equality
            // first, since an equality is satisfied just as well by two identical wrong answers.
            assertEquals(liveResponse.shape(), fabricatedResponse.shape())
            assertEquals(HttpStatusCode.UnprocessableEntity, liveResponse.status)
            assertEquals(HttpStatusCode.UnprocessableEntity, fabricatedResponse.status)

            // The minted token is still alive: ADR-0080 §4 kept it untouched by the two 422s
            // above, so the comparison above was between a live token and a dead one, not
            // between two tokens that were both already dead.
            val stillLive = client.post("/api/auth/reset-password") {
                header(HttpHeaders.ContentType, "application/json")
                setBody("""{"token":"${liveToken.value}","newPassword":"12345678"}""")
            }
            assertEquals(HttpStatusCode.NoContent, stillLive.status)
        }
    }

    /**
     * Mints a token with [tokenMinter] and issues it to [playerId] over `Clock.systemUTC()`, so
     * `expires_at` lands in the future of the database's own clock (`ADR-0080` §6) exactly as
     * [ResetPasswordRouteTest]'s fixture already does.
     */
    private suspend fun issueLiveToken(playerId: PlayerId): ResetToken {
        val token = tokenMinter.newResetToken()
        passwordResets.issue(playerId, token)
        return token
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
