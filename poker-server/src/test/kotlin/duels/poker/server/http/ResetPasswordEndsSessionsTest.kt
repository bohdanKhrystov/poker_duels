package duels.poker.server.http

import duels.poker.server.ServerComponents
import duels.poker.server.auth.CredentialKind
import duels.poker.server.auth.PresentedSecret
import duels.poker.server.auth.ResetToken
import duels.poker.server.config.ServerConfig
import duels.poker.server.db.DatabaseCoordinates
import duels.poker.server.db.Migrations
import duels.poker.server.db.PostgresTestSupport
import duels.poker.server.duelServer
import duels.poker.server.module
import duels.poker.server.protocol.http.SignInResponse
import duels.poker.server.protocol.protocolJson
import duels.poker.server.serverComponents
import duels.poker.server.session.PlayerId
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlinx.serialization.decodeFromString
import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.postgresql.ds.PGSimpleDataSource
import java.util.UUID
import javax.sql.DataSource

/**
 * Proves `ADR-0031` §4's reset sweep at the wire, and proves `recoveryRoutes` is wired into the
 * real server rather than only into a bespoke `testApplication` block.
 *
 * **The contrast with `ADR-0050` is the point.** `DELETE /api/me/device` requires a session and
 * spares the caller's own — *"everywhere except here"*. `POST /api/auth/reset-password` requires
 * **no** session at all — there is no "here" to spare — and the usual reason a player reaches for
 * it is that somebody else now holds the password. A reset ends every session that player held,
 * full stop, and every test name below says so.
 *
 * The first four tests each install [module], [authRoutes], [profileRoutes] and [recoveryRoutes]
 * directly, the same shape `ResetPasswordIsSingleUseTest` uses — never [duelServer]. That is
 * deliberate: [everyRecoveryRouteAnswersOnTheRealServer] is the one test in this file that boots
 * through [duelServer], so it alone reddens if `Application.kt`'s installed line is ever removed.
 * If every test here went through [duelServer] instead, that install could vanish and every test
 * but the socket-only ones would stay green — exactly the blind spot this file exists to close.
 *
 * A live session is proven dead by a refused request, not a table read: every "ends" assertion
 * below authenticates `GET /api/me` with `Authorization: Bearer <token>` and reads the wire answer
 * — `401` for dead, `200` for alive — the same route [DuelServerRoutesTest] uses to prove the route
 * is installed at all.
 */
class ResetPasswordEndsSessionsTest {
    private lateinit var dataSource: DataSource
    private lateinit var config: ServerConfig

    @BeforeEach
    fun setUp() {
        PostgresTestSupport.requireDocker()
        val coordinates: DatabaseCoordinates = PostgresTestSupport.containerCoordinates()
        dataSource = PGSimpleDataSource().apply {
            setUrl(coordinates.url)
            user = coordinates.user
            password = coordinates.password
        }
        Migrations.migrate(dataSource)
        config = ServerConfig(
            port = 8080,
            maxFrameLength = ServerConfig.DEFAULT_MAX_FRAME_LENGTH,
            maxFrameNestingDepth = ServerConfig.DEFAULT_MAX_FRAME_NESTING_DEPTH,
            databaseUrl = coordinates.url,
            databaseUser = coordinates.user,
            databasePassword = coordinates.password,
            databasePoolSize = 10,
            roomWaitingTimeoutMillis = 60_000,
            roomFinishedTimeoutMillis = 60_000,
        )
    }

    @Test
    fun aResetEndsEverySessionThePlayerHeld(): Unit = testApplication {
        val components: ServerComponents = serverComponents(config, dataSource)
        application {
            module()
            authRoutes(
                components.reads,
                components.credentials,
                components.identities,
                components.sessions,
                components.signUpBudget,
                components.signInBudget,
            )
            profileRoutes(components.reads, components.writes, components.identities)
            recoveryRoutes(
                components.recoveryEmails,
                components.passwordResets,
                components.identities,
                components.credentials,
            )
        }

        val playerId = insertPlayer()
        components.credentials.create(
            playerId,
            CredentialKind.PASSWORD,
            "alice",
            PresentedSecret("the original password before any reset"),
        )
        // Two tokens, deliberately: deleting only the newest row would still pass a fixture that
        // held one.
        val firstToken = client.signIn("alice", "the original password before any reset")
        val secondToken = client.signIn("alice", "the original password before any reset")
        assertNotEquals(firstToken, secondToken, "two sign-ins must yield two distinct tokens")

        val resetToken = ResetToken("token-for-two-session-reset")
        assertTrue(components.passwordResets.issue(playerId, resetToken), "the reset token must be issued live")
        val resetResponse = client.post("/api/auth/reset-password") {
            header(HttpHeaders.ContentType, "application/json")
            setBody("""{"token":"${resetToken.value}","newPassword":"the brand new password after reset"}""")
        }
        assertEquals(HttpStatusCode.NoContent, resetResponse.status, "the reset itself must succeed")

        assertEquals(
            HttpStatusCode.Unauthorized,
            client.meWith(firstToken).status,
            "the first token must be refused after the reset",
        )
        assertEquals(
            HttpStatusCode.Unauthorized,
            client.meWith(secondToken).status,
            "the second token must be refused after the reset too — not only the one used most recently",
        )
    }

    @Test
    fun aResetEndsNobodyElsesSession(): Unit = testApplication {
        val components: ServerComponents = serverComponents(config, dataSource)
        application {
            module()
            authRoutes(
                components.reads,
                components.credentials,
                components.identities,
                components.sessions,
                components.signUpBudget,
                components.signInBudget,
            )
            profileRoutes(components.reads, components.writes, components.identities)
            recoveryRoutes(
                components.recoveryEmails,
                components.passwordResets,
                components.identities,
                components.credentials,
            )
        }

        val resettingPlayerId = insertPlayer()
        components.credentials.create(
            resettingPlayerId,
            CredentialKind.PASSWORD,
            "alice",
            PresentedSecret("the resetting player's original password"),
        )

        insertPlayer().also { bystanderId ->
            components.credentials.create(
                bystanderId,
                CredentialKind.PASSWORD,
                "bob",
                PresentedSecret("the bystander's own untouched password"),
            )
        }
        val bystanderToken = client.signIn("bob", "the bystander's own untouched password")

        val resetToken = ResetToken("token-for-isolation-reset")
        assertTrue(components.passwordResets.issue(resettingPlayerId, resetToken))
        val resetResponse = client.post("/api/auth/reset-password") {
            header(HttpHeaders.ContentType, "application/json")
            setBody("""{"token":"${resetToken.value}","newPassword":"the resetting player's brand new password"}""")
        }
        assertEquals(HttpStatusCode.NoContent, resetResponse.status)

        // Guards a `DELETE FROM auth_session` with no `WHERE`: the other player's row must survive
        // a reset that never named them.
        assertEquals(
            HttpStatusCode.OK,
            client.meWith(bystanderToken).status,
            "a second player's session must still work after somebody else's reset",
        )
    }

    @Test
    fun aResetHandsBackNoReplacement(): Unit = testApplication {
        val components: ServerComponents = serverComponents(config, dataSource)
        application {
            module()
            authRoutes(
                components.reads,
                components.credentials,
                components.identities,
                components.sessions,
                components.signUpBudget,
                components.signInBudget,
            )
            profileRoutes(components.reads, components.writes, components.identities)
            recoveryRoutes(
                components.recoveryEmails,
                components.passwordResets,
                components.identities,
                components.credentials,
            )
        }

        val playerId = insertPlayer()
        components.credentials.create(
            playerId,
            CredentialKind.PASSWORD,
            "carol",
            PresentedSecret("the password standing before this reset"),
        )
        val firstToken = client.signIn("carol", "the password standing before this reset")
        val secondToken = client.signIn("carol", "the password standing before this reset")

        val resetToken = ResetToken("token-for-no-replacement-reset")
        assertTrue(components.passwordResets.issue(playerId, resetToken))
        val resetResponse = client.post("/api/auth/reset-password") {
            header(HttpHeaders.ContentType, "application/json")
            setBody("""{"token":"${resetToken.value}","newPassword":"the password standing after this reset"}""")
        }

        assertEquals(HttpStatusCode.NoContent, resetResponse.status)
        val body = resetResponse.bodyAsText()
        assertTrue(body.isEmpty(), "the 204 body must be empty")
        assertNull(resetResponse.headers[HttpHeaders.SetCookie], "the reset must issue no Set-Cookie")
        assertFalse(body.contains(firstToken), "the response must not echo the first deleted token")
        assertFalse(body.contains(secondToken), "the response must not echo the second deleted token")
    }

    @Test
    fun theNewPasswordSignsInAfterwards(): Unit = testApplication {
        val components: ServerComponents = serverComponents(config, dataSource)
        application {
            module()
            authRoutes(
                components.reads,
                components.credentials,
                components.identities,
                components.sessions,
                components.signUpBudget,
                components.signInBudget,
            )
            profileRoutes(components.reads, components.writes, components.identities)
            recoveryRoutes(
                components.recoveryEmails,
                components.passwordResets,
                components.identities,
                components.credentials,
            )
        }

        val playerId = insertPlayer()
        components.credentials.create(
            playerId,
            CredentialKind.PASSWORD,
            "dave",
            PresentedSecret("the deleted password before this reset"),
        )
        val firstDeletedToken = client.signIn("dave", "the deleted password before this reset")
        val secondDeletedToken = client.signIn("dave", "the deleted password before this reset")

        val resetToken = ResetToken("token-for-sign-in-afterwards-reset")
        assertTrue(components.passwordResets.issue(playerId, resetToken))
        val resetResponse = client.post("/api/auth/reset-password") {
            header(HttpHeaders.ContentType, "application/json")
            setBody("""{"token":"${resetToken.value}","newPassword":"the winning password after this reset"}""")
        }
        assertEquals(HttpStatusCode.NoContent, resetResponse.status)

        // The positive control: without this test, a reset that deleted every session and also
        // broke the credential would still pass the three tests above.
        val freshToken = client.signIn("dave", "the winning password after this reset")
        assertNotEquals(firstDeletedToken, freshToken, "the fresh token must differ from the first deleted one")
        assertNotEquals(secondDeletedToken, freshToken, "the fresh token must differ from the second deleted one")
    }

    @Test
    fun everyRecoveryRouteAnswersOnTheRealServer(): Unit = testApplication {
        // Through duelServer(...) itself — Application.kt's real composition — unlike every test
        // above, which installs recoveryRoutes(...) by hand. This is the one test in this file
        // that reddens if the install line is ever removed from Application.kt.
        application { duelServer(serverComponents(config, dataSource)) }

        val verifyEmailResponse = client.post("/api/auth/verify-email") {
            header(HttpHeaders.ContentType, "application/json")
            setBody("""{"token":"a-token-nobody-ever-mailed"}""")
        }
        val resetPasswordResponse = client.post("/api/auth/reset-password") {
            header(HttpHeaders.ContentType, "application/json")
            setBody("""{"token":"a-token-nobody-ever-mailed","newPassword":"a password nobody will ever use"}""")
        }

        // assertAll, not two independent assertEquals blocks: both requests are already sent
        // above, and both routes install from the same recoveryRoutes(...) call site, so if that
        // line is ever removed this must report both endpoints having gone missing in the one run
        // rather than stopping at whichever request this method happens to check first.
        assertAll(
            {
                assertEquals(HttpStatusCode.BadRequest, verifyEmailResponse.status)
                assertNotEquals(
                    HttpStatusCode.NotFound,
                    verifyEmailResponse.status,
                    "a route that was never installed answers 404, which is what this test must rule out",
                )
            },
            {
                assertEquals(HttpStatusCode.BadRequest, resetPasswordResponse.status)
                assertNotEquals(
                    HttpStatusCode.NotFound,
                    resetPasswordResponse.status,
                    "a route that was never installed answers 404, which is what this test must rule out",
                )
            },
        )
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

    /** Signs in over HTTP and answers the freshly issued session token. */
    private suspend fun HttpClient.signIn(handle: String, password: String): String {
        val response = post("/api/auth/sign-in") {
            header(HttpHeaders.ContentType, "application/json")
            setBody("""{"handle":"$handle","password":"$password"}""")
        }
        check(response.status == HttpStatusCode.OK) { "sign-in fixture failed: ${response.status}" }
        return protocolJson.decodeFromString<SignInResponse>(response.bodyAsText()).sessionToken
    }

    /** Presents [token] as a bearer session token against `GET /api/me`. */
    private suspend fun HttpClient.meWith(token: String): HttpResponse =
        get("/api/me") { header(HttpHeaders.Authorization, "Bearer $token") }
}
