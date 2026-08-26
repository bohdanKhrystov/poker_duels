package duels.poker.server.mail

import duels.poker.server.config.ServerConfig
import duels.poker.server.db.Migrations
import duels.poker.server.db.PostgresTestSupport
import duels.poker.server.duelServer
import duels.poker.server.serverComponents
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlinx.coroutines.CompletableJob
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.postgresql.ds.PGSimpleDataSource
import java.io.File

/**
 * `ADR-0031` §7 makes a build with no sender configured a valid state in development and every
 * CI run — every developer machine and every CI run, per `ADR-0077` §1. This file proves the
 * shipped server actually holds up in that state: it boots, every recovery endpoint answers, and
 * the detached delivery [duelServer] composes is a supervised child of the application's own job,
 * never the reverse.
 *
 * `ADR-0077` §7 is categorical: no test here asserts about a mail. [duelServer] composes the
 * `DetachedRecoveryMailer` decorator, and a test booted this way would have to join a scope it
 * does not hold — presence and absence of a mail belong to the undecorated-double tests
 * elsewhere. This file asserts only that the server runs, and that the concurrency shape
 * `Application.kt` builds around it is the one `ADR-0077` §3 specifies.
 */
class NoSenderConfiguredTest {
    private lateinit var dataSource: PGSimpleDataSource
    private lateinit var config: ServerConfig

    @BeforeEach
    fun setUp() {
        PostgresTestSupport.requireDocker()
        val coordinates = PostgresTestSupport.containerCoordinates()
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
    fun theServerStartsWithNoSenderConfigured(): Unit = testApplication {
        application { duelServer(serverComponents(config, dataSource)) }
        val response = client.get("/health")
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("OK", response.bodyAsText())
    }

    @Test
    fun everyRecoveryEndpointAnswers(): Unit = testApplication {
        application { duelServer(serverComponents(config, dataSource)) }

        val endpoints = listOf(
            RecoveryEndpoint("POST", "/api/auth/verify-email", HttpStatusCode.BadRequest),
            RecoveryEndpoint("POST", "/api/auth/forgot-password", HttpStatusCode.Accepted),
            RecoveryEndpoint("POST", "/api/auth/reset-password", HttpStatusCode.BadRequest),
            RecoveryEndpoint("POST", "/api/auth/recovery-email", HttpStatusCode.Unauthorized),
            RecoveryEndpoint("DELETE", "/api/auth/recovery-email", HttpStatusCode.Unauthorized),
        )
        // A list that quietly lost one endpoint would otherwise pass having checked four; assert
        // the count before calling any of them.
        assertEquals(5, endpoints.size)

        for (endpoint in endpoints) {
            val response = when (endpoint.method) {
                "POST" -> client.post(endpoint.path) {
                    header(HttpHeaders.ContentType, "application/json")
                    setBody("{}")
                }
                "DELETE" -> client.delete(endpoint.path)
                else -> error("unsupported method ${endpoint.method}")
            }
            // Not a 500 and not a 404, and the exact status its own handler specifies — a route
            // silently changing behaviour with no sender configured reddens this too.
            assertEquals(
                endpoint.expected,
                response.status,
                "${endpoint.method} ${endpoint.path} answered ${response.status}, expected ${endpoint.expected}",
            )
        }
    }

    @Test
    fun noRouteFileBranchesOnWhetherASenderIsConfigured() {
        val source = File("src/main/kotlin/duels/poker/server/http/RecoveryRoutes.kt").readText()

        // Positive control: the search mechanism must find something known to be present, or an
        // absent match below proves nothing — a search that finds nothing cannot fail either.
        assertTrue(
            source.contains("fun Application.recoveryRoutes"),
            "did not find recoveryRoutes — the search itself is broken",
        )

        val forbiddenAnywhere = listOf(
            "isConfigured",
            "mailer == null",
            "mailer != null",
            "null == mailer",
            "null != mailer",
            "mailer?.",
        )
        for (token in forbiddenAnywhere) {
            assertFalse(source.contains(token), "RecoveryRoutes.kt contains forbidden token: $token")
        }

        // The one wiring-level default — `mailer: RecoveryMailer = NoRecoveryMailer` — is not a
        // branch: ADR-0077 §1 makes NoRecoveryMailer "an object, never a null", the identical
        // idiom this file already uses for `tokens: RecoveryTokens = RecoveryTokens()`. What
        // ADR-0077 §1 actually forbids is a *handler* reintroducing a check, so this sweeps the
        // routing block those handlers live in, not the function signature above it.
        assertTrue(source.contains("routing {"), "did not find a routing block to sweep")
        val routingBlock = source.substringAfter("routing {")
        assertFalse(
            routingBlock.contains("NoRecoveryMailer"),
            "a route handler references NoRecoveryMailer directly",
        )
    }

    @Test
    fun nothingShutsTheDeliveryScopeDownExplicitly() {
        val source = File("src/main/kotlin/duels/poker/server/Application.kt").readText()

        // Positive control, as above.
        assertTrue(
            source.contains("fun Application.duelServer"),
            "did not find duelServer — the search itself is broken",
        )

        val forbiddenTokens = listOf("withTimeoutOrNull", "joinAll", "GlobalScope")
        for (token in forbiddenTokens) {
            assertFalse(source.contains(token), "Application.kt contains forbidden token: $token")
        }
        assertFalse(
            Regex(""":\s*Job\b""").containsMatchIn(source),
            "Application.kt stores a Job field",
        )

        // Application.kt contains exactly one CoroutineScope( call, and it is the delivery scope
        // ADR-0077 §3 specifies, verbatim.
        val scopeCalls = Regex("""CoroutineScope\(""").findAll(source).count()
        assertEquals(1, scopeCalls, "expected exactly one CoroutineScope( call, found $scopeCalls")
        assertTrue(source.contains("SupervisorJob("), "the delivery scope does not carry a SupervisorJob(")
        assertTrue(
            source.contains("""CoroutineName("recovery-mail")"""),
            """the delivery scope does not carry CoroutineName("recovery-mail")""",
        )
    }

    @Test
    fun theDeliveryScopeIsASupervisorChildOfTheApplication() {
        lateinit var appJob: Job
        lateinit var deliveryJob: CompletableJob

        testApplication {
            application {
                val app = coroutineContext.job
                val before = app.children.toSet()
                duelServer(serverComponents(config, dataSource))
                val added = (app.children.toSet() - before).filterIsInstance<CompletableJob>()
                check(added.size == 1) {
                    "expected exactly one new CompletableJob under the application's job, found " +
                        "${added.size}"
                }
                appJob = app
                deliveryJob = added.single()
            }
            // `application { }` blocks install lazily; force the engine to actually run it.
            client.get("/health")

            // Direction 2: a failure launched directly into the delivery scope — bypassing
            // DetachedRecoveryMailer's own catch-all, which is TASK-041631's territory, not this
            // one's — must not reach the application's job. Asserted on the jobs, not a mail.
            // A CoroutineExceptionHandler catches the deliberate throw here so it is this
            // assertion that observes it, not the JVM's default uncaught-exception handler.
            var observed = false
            val handler = CoroutineExceptionHandler { _, _ -> observed = true }
            val failing = CoroutineScope(deliveryJob).launch(handler) {
                throw RuntimeException("simulated delivery failure")
            }
            failing.join()
            assertTrue(observed, "the simulated failure never ran")
            assertTrue(appJob.isActive, "a failed delivery cancelled the application's job")
            assertTrue(deliveryJob.isActive, "a failed delivery completed the supervisor itself")
        }

        // Direction 1: the block above has now stopped the test application. The delivery scope
        // is a structural child of the application's job, so cancellation — which cascades
        // downward regardless of the supervisor barrier that only blocks a child's failure from
        // cascading upward — must have completed it too.
        assertTrue(deliveryJob.isCompleted, "delivery scope's job did not complete when the application stopped")
        assertTrue(appJob.isCompleted, "application job did not complete when the application stopped")
    }

    private data class RecoveryEndpoint(val method: String, val path: String, val expected: HttpStatusCode)
}
