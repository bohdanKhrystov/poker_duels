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
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Clock
import java.util.UUID
import javax.sql.DataSource

/** The login handle every fixture credential in this file is created under. */
private const val IDENTIFIER = "reset-route-test-handle"

/**
 * Tests for `POST /api/auth/reset-password`, installed by [recoveryRoutes] and driven against a
 * real [PostgresPasswordResets] and a real [PostgresCredentials] — this route has no double of
 * its own to stand in for either port.
 *
 * `Application.kt` does not install [recoveryRoutes] yet (`TASK-041622`), so every test installs
 * it directly inside [testApplication], the same way [VerifyEmailRouteTest] tests the sibling
 * route this function also installs. Every fixture that needs a live token mints it by calling
 * [PostgresPasswordResets.issue] directly, since `POST /api/auth/forgot-password` — the route
 * that would normally cause one — does not exist yet (`TASK-041626`).
 *
 * Every `newPassword` this file sends in a request body, or writes into a fixture's own
 * [PresentedSecret], is 8 to 128 code points — including in [aBadTokenAnswersFourHundred] and
 * [theTokenIsNotAcceptedAsAQueryParameter], the two that expect `400`. This route runs no
 * password policy of its own (`ADR-0080` §7: that check lands in front of
 * [duels.poker.server.auth.PasswordResets.consume] with `TASK-041629`, and this file must then
 * pass unchanged), so nothing here asserts on length today — the constraint exists only so that
 * day does not turn one of this file's `400`s into a `422`.
 */
class ResetPasswordRouteTest {
    private lateinit var dataSource: DataSource
    private lateinit var passwordResets: PostgresPasswordResets
    private lateinit var credentials: PostgresCredentials
    private lateinit var recoveryEmails: PostgresRecoveryEmails

    @BeforeEach
    fun setupDatabase() {
        dataSource = PostgresTestSupport.freshDatabase()
        Migrations.migrate(dataSource)
        passwordResets = PostgresPasswordResets(dataSource, Clock.systemUTC(), RecoveryTokens())
        credentials = PostgresCredentials(dataSource)
        recoveryEmails = PostgresRecoveryEmails(dataSource, Clock.systemUTC())
    }

    @Test
    fun aGoodTokenAnswersTwoHundredAndFour() {
        testApplication {
            application {
                module()
                recoveryRoutes(recoveryEmails, passwordResets, identitiesFor(emptyMap()), RecordingCredentials())
            }
            val playerId = insertPlayer()
            val oldPassword = PresentedSecret("original password, eight or more")
            credentials.create(playerId, CredentialKind.PASSWORD, IDENTIFIER, oldPassword)
            passwordResets.issue(playerId, ResetToken("reset-good-token"))
            val newPassword = "brand new password"

            val response = client.post("/api/auth/reset-password") {
                header(HttpHeaders.ContentType, "application/json")
                setBody("""{"token":"reset-good-token","newPassword":"$newPassword"}""")
            }
            val body = response.bodyAsText()

            assertEquals(HttpStatusCode.NoContent, response.status)
            assertEquals("", body)
            assertNull(response.headers[HttpHeaders.SetCookie], "expected no Set-Cookie header on a reset response")
            assertFalse(body.contains("sessionToken"), "expected no sessionToken substring in the response body")
            assertEquals(
                playerId,
                credentials.verify(CredentialKind.PASSWORD, IDENTIFIER, PresentedSecret(newPassword)),
                "the new password must now sign in",
            )
            assertNull(
                credentials.verify(CredentialKind.PASSWORD, IDENTIFIER, oldPassword),
                "the old password must no longer sign in",
            )
        }
    }

    @Test
    fun aBadTokenAnswersFourHundred() {
        testApplication {
            application {
                module()
                recoveryRoutes(recoveryEmails, passwordResets, identitiesFor(emptyMap()), RecordingCredentials())
            }
            val playerId = insertPlayer()
            val originalPassword = PresentedSecret("original password, nine or more")
            credentials.create(playerId, CredentialKind.PASSWORD, IDENTIFIER, originalPassword)
            // No token was ever issued for this player: "fabricated-token" never names a live row.

            val response = client.post("/api/auth/reset-password") {
                header(HttpHeaders.ContentType, "application/json")
                setBody("""{"token":"fabricated-token","newPassword":"a perfectly fine new password"}""")
            }

            assertEquals(HttpStatusCode.BadRequest, response.status)
            assertEquals("", response.bodyAsText())
            assertEquals(
                playerId,
                credentials.verify(CredentialKind.PASSWORD, IDENTIFIER, originalPassword),
                "the original password must still verify after a refused reset",
            )
        }
    }

    @Test
    fun theTokenIsNotAcceptedAsAQueryParameter() {
        testApplication {
            application {
                module()
                recoveryRoutes(recoveryEmails, passwordResets, identitiesFor(emptyMap()), RecordingCredentials())
            }
            val playerId = insertPlayer()
            val originalPassword = PresentedSecret("original password, ten or more")
            credentials.create(playerId, CredentialKind.PASSWORD, IDENTIFIER, originalPassword)
            val goodToken = ResetToken("reset-query-good-token")
            passwordResets.issue(playerId, goodToken)

            // The good token travels only in the query string; the body names a different, unknown
            // token. A handler that reads the query parameter before (or instead of) the body would
            // answer 204 here.
            val response = client.post("/api/auth/reset-password?token=${goodToken.value}") {
                header(HttpHeaders.ContentType, "application/json")
                setBody("""{"token":"a-different-unknown-token","newPassword":"another perfectly fine password"}""")
            }

            assertEquals(HttpStatusCode.BadRequest, response.status)
            assertEquals("", response.bodyAsText())
            assertEquals(
                playerId,
                credentials.verify(CredentialKind.PASSWORD, IDENTIFIER, originalPassword),
                "the original password must still verify — the good token in the query string must do nothing",
            )
        }
    }

    @Test
    fun theRouteReadsNoIdentityHeader() {
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
                PresentedSecret("original password, header free"),
            )
            passwordResets.issue(playerId, ResetToken("reset-no-identity-token"))

            // Neither X-Device-Id nor Authorization is set anywhere in this request: a guard
            // added later that reads either one would answer 401 here instead of 204.
            val response = client.post("/api/auth/reset-password") {
                header(HttpHeaders.ContentType, "application/json")
                setBody("""{"token":"reset-no-identity-token","newPassword":"another perfectly fine password"}""")
            }

            assertEquals(HttpStatusCode.NoContent, response.status)
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
