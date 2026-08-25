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
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import java.time.Clock
import java.util.UUID
import java.util.concurrent.CountDownLatch
import javax.sql.DataSource

/** The login handle every fixture credential in this file is created under. */
private const val IDENTIFIER = "single-use-test-handle"

/**
 * Proves single use of a reset token **at the wire** — `POST /api/auth/reset-password` — rather
 * than at the port. `TASK-041614` proves the mechanism directly over `PostgresPasswordResets`:
 * consumption is one `DELETE … RETURNING`, so no interleaving of two concurrent callers can both
 * read the row before either deletes it (`ADR-0031` §4). This file proves the contract the route
 * built on top of that mechanism exposes to a caller — a route is free to retry a failed consume,
 * wrap it in its own transaction, or catch an exception and call again, and none of those would
 * fail a test that calls the port once.
 *
 * The `testApplication` fixture and the `recoveryRoutes(...)` install below are the same shape
 * [ResetPasswordRouteTest] uses. Every `newPassword` this file sends is 8 to 128 code points, the
 * same constraint that file documents: this route runs no password policy of its own yet, and a
 * shorter or longer password here would silently turn a `400` this file expects into a `422` once
 * a policy check lands in front of consumption (`ADR-0080` §7).
 */
class ResetPasswordIsSingleUseTest {
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
    fun theSecondSubmissionOfOneLinkIsRefused() {
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
                PresentedSecret("original password before any reset"),
            )
            val token = ResetToken("single-use-sequential-token")
            passwordResets.issue(playerId, token)

            val firstResponse = client.post("/api/auth/reset-password") {
                header(HttpHeaders.ContentType, "application/json")
                setBody("""{"token":"${token.value}","newPassword":"the winning sequential password"}""")
            }
            assertEquals(
                HttpStatusCode.NoContent,
                firstResponse.status,
                "the first submission of a live token must succeed",
            )

            // Same token, a second time: this must be refused, but the refusal must not be
            // distinguishable from a token that was never issued in the first place (§5's
            // three-indistinguishable-cases rule, proven at this route by TASK-041619).
            val spentTokenResponse = client.post("/api/auth/reset-password") {
                header(HttpHeaders.ContentType, "application/json")
                setBody("""{"token":"${token.value}","newPassword":"the refused sequential password"}""")
            }

            val neverIssuedResponse = client.post("/api/auth/reset-password") {
                header(HttpHeaders.ContentType, "application/json")
                setBody(
                    """{"token":"a-token-nobody-ever-issued","newPassword":"another refused password here"}""",
                )
            }
            assertEquals(
                HttpStatusCode.BadRequest,
                neverIssuedResponse.status,
                "a never-issued token must be refused",
            )

            assertEquals(
                fingerprintOf(neverIssuedResponse),
                fingerprintOf(spentTokenResponse),
                "a spent token and a never-issued token must be indistinguishable at the wire",
            )
            assertEquals(
                playerId,
                credentials.verify(
                    CredentialKind.PASSWORD,
                    IDENTIFIER,
                    PresentedSecret("the winning sequential password"),
                ),
                "the first, successful submission's password must still verify",
            )
        }
    }

    @Test
    @Timeout(60)
    fun twoSimultaneousSubmissionsYieldExactlyOneSuccess() {
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
                PresentedSecret("original password before any race"),
            )
            val token = ResetToken("single-use-concurrent-token-one")
            passwordResets.issue(playerId, token)

            // Two different passwords is the whole design: with one password, both submissions
            // succeeding would leave a database indistinguishable from one submission succeeding.
            val passwordA = "the password thread A is racing with"
            val passwordB = "the password thread B is racing with"
            val (responseA, responseB) = raceTwoSubmissions(client, token.value, passwordA, passwordB)
            val statuses = listOf(responseA.status, responseB.status)

            assertEquals(
                1,
                statuses.count { it == HttpStatusCode.NoContent },
                "expected exactly one 204 across both concurrent submissions, got $statuses",
            )
            assertEquals(
                1,
                statuses.count { it == HttpStatusCode.BadRequest },
                "expected exactly one 400 across both concurrent submissions, got $statuses",
            )

            val winningPassword = if (responseA.status == HttpStatusCode.NoContent) passwordA else passwordB
            assertEquals(
                playerId,
                credentials.verify(CredentialKind.PASSWORD, IDENTIFIER, PresentedSecret(winningPassword)),
                "the stored password must be the one whose request answered 204",
            )
        }
    }

    @Test
    @Timeout(60)
    fun theLoserOfTheRaceChangedNothing() {
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
                PresentedSecret("original password before this race"),
            )
            val token = ResetToken("single-use-concurrent-token-two")
            passwordResets.issue(playerId, token)

            val passwordA = "the first candidate of the second race"
            val passwordB = "the second candidate of the second race"
            val (responseA, responseB) = raceTwoSubmissions(client, token.value, passwordA, passwordB)

            // Identify the loser by its own 400, rather than by elimination from a count assertion
            // made elsewhere: if neither response is 400 (both raced to success), there is no
            // loser to identify, and that must fail this test rather than silently mislabel one.
            val losingPassword = when {
                responseA.status == HttpStatusCode.BadRequest -> passwordA
                responseB.status == HttpStatusCode.BadRequest -> passwordB
                else -> throw AssertionError(
                    "expected one of the two concurrent responses to be 400, " +
                        "got ${responseA.status} and ${responseB.status}",
                )
            }

            assertNull(
                credentials.verify(CredentialKind.PASSWORD, IDENTIFIER, PresentedSecret(losingPassword)),
                "the losing submission's password must not verify — it must not have partially applied",
            )
        }
    }

    /**
     * Fires two `POST`s at [token] from two real threads, both released by the same
     * [CountDownLatch], and returns both responses only once both threads have finished — this is
     * what puts the requests in flight together rather than one after the other. Each thread's
     * response is read by the caller only after [Thread.join] on that thread, which is what makes
     * the read safe without any explicit `volatile` or atomic wrapper.
     */
    private fun raceTwoSubmissions(
        client: HttpClient,
        token: String,
        passwordA: String,
        passwordB: String,
    ): Pair<HttpResponse, HttpResponse> {
        val latch = CountDownLatch(1)
        var responseA: HttpResponse? = null
        var responseB: HttpResponse? = null

        val threadA = Thread {
            latch.await()
            responseA = runBlocking { postReset(client, token, passwordA) }
        }
        val threadB = Thread {
            latch.await()
            responseB = runBlocking { postReset(client, token, passwordB) }
        }

        threadA.start()
        threadB.start()
        latch.countDown()
        threadA.join()
        threadB.join()

        return (responseA ?: error("thread A never produced a response")) to
            (responseB ?: error("thread B never produced a response"))
    }

    private suspend fun postReset(
        client: HttpClient,
        token: String,
        newPassword: String,
    ): HttpResponse {
        return client.post("/api/auth/reset-password") {
            header(HttpHeaders.ContentType, "application/json")
            setBody("""{"token":"$token","newPassword":"$newPassword"}""")
        }
    }

    private suspend fun fingerprintOf(response: HttpResponse): ResponseFingerprint =
        ResponseFingerprint(response.status, response.bodyAsText(), response.headers.names())

    private data class ResponseFingerprint(
        val status: HttpStatusCode,
        val body: String,
        val headerNames: Set<String>,
    )

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
