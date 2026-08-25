package duels.poker.server.http

import duels.poker.server.auth.EmailAddress
import duels.poker.server.auth.PasswordResets
import duels.poker.server.auth.PresentedSecret
import duels.poker.server.auth.ResetToken
import duels.poker.server.auth.VerificationToken
import duels.poker.server.db.Migrations
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
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Clock
import java.util.UUID
import javax.sql.DataSource

/**
 * Tests for `POST /api/auth/verify-email`, installed by [recoveryRoutes] and driven against a
 * real [PostgresRecoveryEmails] — this route has no double of its own to stand in for the port.
 *
 * `Application.kt` does not install [recoveryRoutes] yet (`TASK-041622`), so every test installs
 * it directly inside [testApplication], the same way `AuthRouteTest` first tested `authRoutes`.
 * Every fixture that needs a live token creates its pending row by calling
 * [PostgresRecoveryEmails.claimPending] directly, since `POST /api/auth/recovery-email` — the
 * route that would normally cause one — does not exist yet either (`TASK-041625`).
 */
class VerifyEmailRouteTest {
    private lateinit var dataSource: DataSource
    private lateinit var recoveryEmails: PostgresRecoveryEmails

    @BeforeEach
    fun setupDatabase() {
        dataSource = PostgresTestSupport.freshDatabase()
        Migrations.migrate(dataSource)
        recoveryEmails = PostgresRecoveryEmails(dataSource, Clock.systemUTC())
    }

    @Test
    fun aGoodTokenAnswersTwoHundredAndFour() {
        testApplication {
            application {
                module()
                recoveryRoutes(recoveryEmails, NoPasswordResets, identitiesFor(emptyMap()), RecordingCredentials())
            }
            val playerId = insertPlayer()
            recoveryEmails.claimPending(playerId, EmailAddress("bob@x.test"), VerificationToken("good-token"))

            val response = client.post("/api/auth/verify-email") {
                header(HttpHeaders.ContentType, "application/json")
                setBody("""{"token":"good-token"}""")
            }

            assertEquals(HttpStatusCode.NoContent, response.status)
            assertEquals("", response.bodyAsText())
            assertTrue(recoveryEmails.hasRecoveryEmail(playerId), "the address must now be proven")
        }
    }

    @Test
    fun anUnknownTokenAnswersFourHundred() {
        testApplication {
            application {
                module()
                recoveryRoutes(recoveryEmails, NoPasswordResets, identitiesFor(emptyMap()), RecordingCredentials())
            }

            // No claim was ever made for this token: it never named a live pending row.
            val response = client.post("/api/auth/verify-email") {
                header(HttpHeaders.ContentType, "application/json")
                setBody("""{"token":"never-claimed"}""")
            }

            assertEquals(HttpStatusCode.BadRequest, response.status)
            assertEquals("", response.bodyAsText())
        }
    }

    @Test
    fun aMalformedBodyAnswersFourHundred() {
        testApplication {
            application {
                module()
                recoveryRoutes(recoveryEmails, NoPasswordResets, identitiesFor(emptyMap()), RecordingCredentials())
            }

            // An empty body: decoding fails before anything could be asked of the port.
            val empty = client.post("/api/auth/verify-email") {
                header(HttpHeaders.ContentType, "application/json")
            }
            assertEquals(HttpStatusCode.BadRequest, empty.status)
            assertEquals("", empty.bodyAsText())

            // Valid JSON, but the required field is missing: VerifyEmailRequest.token has no
            // default, which is exactly what turns this into a 400 instead of an empty string.
            val missingField = client.post("/api/auth/verify-email") {
                header(HttpHeaders.ContentType, "application/json")
                setBody("{}")
            }
            assertEquals(HttpStatusCode.BadRequest, missingField.status)
            assertEquals("", missingField.bodyAsText())

            // A JSON number where the field's declared type is a string. This does not fail to
            // decode at all: the content negotiation here coerces it into the string "123", so
            // this case actually reaches the port as an ordinary unknown token and is answered
            // by Refused, not by the catch block above — confirmed by mutating Refused's status
            // and watching this assertion redden along with anUnknownTokenAnswersFourHundred's.
            // It still belongs in this test, and still answers 400, just by the other path.
            val wrongType = client.post("/api/auth/verify-email") {
                header(HttpHeaders.ContentType, "application/json")
                setBody("""{"token":123}""")
            }
            assertEquals(HttpStatusCode.BadRequest, wrongType.status)
            assertEquals("", wrongType.bodyAsText())
        }
    }

    @Test
    fun theRouteReadsNoIdentityHeader() {
        testApplication {
            application {
                module()
                recoveryRoutes(recoveryEmails, NoPasswordResets, identitiesFor(emptyMap()), RecordingCredentials())
            }
            val playerId = insertPlayer()
            recoveryEmails.claimPending(
                playerId,
                EmailAddress("carol@x.test"),
                VerificationToken("header-free-token"),
            )

            // Neither X-Device-Id nor Authorization is set anywhere in this request: a guard
            // added later that reads either one would answer 401 here instead of 204.
            val response = client.post("/api/auth/verify-email") {
                header(HttpHeaders.ContentType, "application/json")
                setBody("""{"token":"header-free-token"}""")
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

/**
 * A [PasswordResets] that throws on every call. `recoveryRoutes`' `verify-email` handler never
 * touches this port; a call reaching either method here would mean the handler read the wrong
 * parameter.
 */
private object NoPasswordResets : PasswordResets {
    override suspend fun issue(playerId: PlayerId, token: ResetToken): Boolean {
        throw UnsupportedOperationException("verify-email never issues a reset token")
    }

    override suspend fun consume(token: ResetToken, secret: PresentedSecret): Boolean {
        throw UnsupportedOperationException("verify-email never consumes a reset token")
    }
}
