package duels.poker.server.http

import duels.poker.server.auth.AttemptBudget
import duels.poker.server.auth.AttemptLimits
import duels.poker.server.auth.CredentialKind
import duels.poker.server.auth.EmailAddress
import duels.poker.server.auth.PresentedSecret
import duels.poker.server.auth.RecoveryMailer
import duels.poker.server.auth.RecoveryTokens
import duels.poker.server.auth.ResetToken
import duels.poker.server.auth.SessionToken
import duels.poker.server.auth.VerificationToken
import duels.poker.server.auth.VerifyEmailResult
import duels.poker.server.db.Migrations
import duels.poker.server.db.PostgresAuthSessions
import duels.poker.server.db.PostgresCredentials
import duels.poker.server.db.PostgresPasswordResets
import duels.poker.server.db.PostgresRecoveryEmails
import duels.poker.server.db.PostgresTestSupport
import duels.poker.server.module
import duels.poker.server.session.PlayerId
import duels.poker.server.time.MutableClock
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.application.call
import io.ktor.server.plugins.mutableOriginConnectionPoint
import io.ktor.server.testing.testApplication
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Clock
import java.util.UUID
import javax.sql.DataSource

/** The password every fixture credential in this file is created with; no test ever presents it. */
private const val FIXTURE_PASSWORD = "recovery-budgets-test-fixture-password"

/**
 * Tests for the two per-address rate limiters `ADR-0079` adds to `POST /api/auth/forgot-password`
 * and `POST /api/auth/recovery-email` (`TASK-041628`): ten forgets and five attaches, both per
 * rolling sixty seconds, both answering exactly as a within-budget success does once exhausted, so
 * neither limiter is itself an oracle.
 *
 * Every budget here is built over a [MutableClock] the test itself advances — [AttemptBudget]'s own
 * window, never [PostgresRecoveryEmails]' or [PostgresPasswordResets]' own fifteen-minute
 * suppression window, which stays on a real [Clock] throughout this file and is never the thing
 * under test. Remote addresses are set with the same `intercept` idiom
 * `AuthRouteTest.twoAddressesHaveTwoSignUpBudgets` uses, because Ktor's test client always reports
 * `origin.remoteAddress` as `"localhost"`.
 *
 * Both routes drive real, Postgres-backed ports — `recoveryEmails`, `credentials`, `passwordResets`
 * and `sessions` — exactly as `ForgotPasswordRouteTest` and `AttachRecoveryEmailRouteTest` do for
 * their own handlers, because forgot-password's mint and recovery-email's claim are exactly the
 * database facts an over-budget request must never produce, and the wire cannot show their absence:
 * both endpoints answer `202` whether or not the budget admitted the request.
 */
class RecoveryBudgetsTest {
    private lateinit var dataSource: DataSource
    private lateinit var recoveryEmails: PostgresRecoveryEmails
    private lateinit var credentials: PostgresCredentials
    private lateinit var passwordResets: PostgresPasswordResets
    private lateinit var sessions: PostgresAuthSessions

    @BeforeEach
    fun setupDatabase() {
        dataSource = PostgresTestSupport.freshDatabase()
        Migrations.migrate(dataSource)
        recoveryEmails = PostgresRecoveryEmails(dataSource, Clock.systemUTC())
        credentials = PostgresCredentials(dataSource)
        passwordResets = PostgresPasswordResets(dataSource, Clock.systemUTC(), RecoveryTokens())
        sessions = PostgresAuthSessions(dataSource, Clock.systemUTC())
    }

    @Test
    fun anOverBudgetForgotPasswordAnswersLikeASuccess() {
        testApplication {
            application {
                module()
                recoveryRoutes(
                    recoveryEmails,
                    passwordResets,
                    identitiesFor(emptyMap()),
                    credentials,
                    forgotPasswordBudget = AttemptBudget(AttemptLimits(1, 60_000L), MutableClock()),
                )
            }
            val success = client.forgotPasswordOutcomeFor("budgets-forgot-answers-success@x.test")
            assertEquals(HttpStatusCode.Accepted, success.status, "setup: expected the admitted request to succeed")

            val overBudget = client.forgotPasswordOutcomeFor("budgets-forgot-answers-over@x.test")

            assertEquals(
                success,
                overBudget,
                "expected an over-budget forgot-password request's (status, body, header names) " +
                    "triple to equal a within-budget success's triple",
            )
            assertEquals(HttpStatusCode.Accepted, overBudget.status, "expected the over-budget answer to be 202")
        }
    }

    @Test
    fun anOverBudgetForgotPasswordMintsNothing() {
        val mailer = RecordingRecoveryMailerForBudgets()
        testApplication {
            application {
                module()
                recoveryRoutes(
                    recoveryEmails,
                    passwordResets,
                    identitiesFor(emptyMap()),
                    credentials,
                    mailer = mailer,
                    forgotPasswordBudget = AttemptBudget(AttemptLimits(1, 60_000L), MutableClock()),
                )
            }
            val admitted = verifiedPlayerFor("budgets-forgot-mints-admitted@x.test", "budgets-forgot-mints-admitted-handle")
            val refusedFirst =
                verifiedPlayerFor("budgets-forgot-mints-refused-1@x.test", "budgets-forgot-mints-refused-1-handle")
            val refusedSecond =
                verifiedPlayerFor("budgets-forgot-mints-refused-2@x.test", "budgets-forgot-mints-refused-2-handle")

            // The positive control the triple comparison above cannot give: the very first request
            // from this address, to a verified address never mailed before, must actually mint —
            // proving the budget is not simply refusing everything by coincidence.
            val first = client.forgotPasswordOutcomeFor("budgets-forgot-mints-admitted@x.test")
            assertEquals(HttpStatusCode.Accepted, first.status, "setup: expected the admitted request to succeed")

            // Over budget, and each for a DIFFERENT, never-mailed verified address: ADR-0031 §5's
            // own fifteen-minute per-account suppression would otherwise explain a missing mint on
            // its own, so repeating the admitted address here would leave a bypassed budget
            // invisible — reusing one address is the trap this test exists to avoid.
            client.forgotPasswordOutcomeFor("budgets-forgot-mints-refused-1@x.test")
            client.forgotPasswordOutcomeFor("budgets-forgot-mints-refused-2@x.test")

            assertEquals(
                1,
                dataSource.passwordResetRowCount(),
                "expected exactly one password_reset row across all three requests",
            )
            assertTrue(dataSource.hasPasswordReset(admitted), "expected the admitted address's own row")
            assertFalse(dataSource.hasPasswordReset(refusedFirst), "expected no row for the first over-budget address")
            assertFalse(dataSource.hasPasswordReset(refusedSecond), "expected no row for the second over-budget address")
            assertEquals(1, mailer.resetsSent.size, "expected exactly one send across all three requests")
        }
    }

    @Test
    fun anOverBudgetAttachAnswersLikeASuccess() {
        val mailer = RecordingRecoveryMailerForBudgets()
        testApplication {
            application {
                module()
                recoveryRoutes(
                    recoveryEmails,
                    passwordResets,
                    identitiesFor(dataSource),
                    credentials,
                    mailer = mailer,
                    recoveryEmailBudget = AttemptBudget(AttemptLimits(1, 60_000L), MutableClock()),
                )
            }
            val admittedPlayer = insertPlayer()
            val admittedPassword = "budgets-attach-answers-admitted-password"
            credentials.create(
                admittedPlayer,
                CredentialKind.PASSWORD,
                handleForBudgets(admittedPlayer),
                PresentedSecret(admittedPassword),
            )
            val admittedToken = sessions.issue(admittedPlayer)

            val overBudgetPlayer = insertPlayer()
            val overBudgetPassword = "budgets-attach-answers-over-budget-password"
            credentials.create(
                overBudgetPlayer,
                CredentialKind.PASSWORD,
                handleForBudgets(overBudgetPlayer),
                PresentedSecret(overBudgetPassword),
            )
            val overBudgetToken = sessions.issue(overBudgetPlayer)

            val success = client.attachOutcomeFor(admittedToken, "budgets-attach-answers-a@x.test", admittedPassword)
            assertEquals(HttpStatusCode.Accepted, success.status, "setup: expected the admitted claim to succeed")

            // A RIGHT password on the over-budget request: within budget this would claim, so the
            // 202 below can only have come from the budget refusing before verifyCurrent is ever
            // reached, not from a password happening to be wrong.
            val overBudget = client.attachOutcomeFor(overBudgetToken, "budgets-attach-answers-b@x.test", overBudgetPassword)

            assertEquals(
                success,
                overBudget,
                "expected an over-budget recovery-email request's (status, body, header names) " +
                    "triple to equal a within-budget success's triple, even with its own password right",
            )
            assertFalse(
                dataSource.hasPendingVerification(overBudgetPlayer),
                "expected no email_verification row for the over-budget player",
            )
            assertEquals(
                1,
                mailer.verificationsSent.size,
                "expected exactly one verification send, for the admitted claim alone",
            )
        }
    }

    @Test
    fun oneAddressBudgetIsNotAnothers() {
        val forgotPasswordBudget = AttemptBudget(AttemptLimits(1, 60_000L), MutableClock())

        testApplication {
            application {
                module()
                intercept(ApplicationCallPipeline.Plugins) {
                    call.mutableOriginConnectionPoint.remoteAddress = "203.0.113.41"
                }
                recoveryRoutes(
                    recoveryEmails,
                    passwordResets,
                    identitiesFor(emptyMap()),
                    credentials,
                    forgotPasswordBudget = forgotPasswordBudget,
                )
            }
            // Address A spends the shared budget's only slot.
            val first = client.forgotPasswordOutcomeFor("budgets-one-address-a@x.test")
            assertEquals(HttpStatusCode.Accepted, first.status, "setup: expected address A's request to succeed")
        }

        testApplication {
            application {
                module()
                intercept(ApplicationCallPipeline.Plugins) {
                    call.mutableOriginConnectionPoint.remoteAddress = "203.0.113.42"
                }
                recoveryRoutes(
                    recoveryEmails,
                    passwordResets,
                    identitiesFor(emptyMap()),
                    credentials,
                    forgotPasswordBudget = forgotPasswordBudget,
                )
            }
            val forB = verifiedPlayerFor("budgets-one-address-b@x.test", "budgets-one-address-b-handle")

            // A different address's first request, against the very same budget instance, still
            // reaches the mint — proving the key that separates them was actually read from the
            // request rather than being a single counter shared by everyone (a constant key would
            // refuse this, since address A already spent the whole shared budget above).
            val second = client.forgotPasswordOutcomeFor("budgets-one-address-b@x.test")
            assertEquals(HttpStatusCode.Accepted, second.status, "setup: expected address B's request to answer 202 too")
            assertTrue(
                dataSource.hasPasswordReset(forB),
                "expected address B's own request to mint, proving address A's exhausted budget did not carry over",
            )
        }
    }

    @Test
    fun theTwoEndpointsDoNotShareABudget() {
        val mailer = RecordingRecoveryMailerForBudgets()

        // Direction one: forgot-password's budget exhausted, recovery-email unaffected from the
        // same remote address.
        testApplication {
            application {
                module()
                intercept(ApplicationCallPipeline.Plugins) {
                    call.mutableOriginConnectionPoint.remoteAddress = "203.0.113.51"
                }
                recoveryRoutes(
                    recoveryEmails,
                    passwordResets,
                    identitiesFor(dataSource),
                    credentials,
                    mailer = mailer,
                    forgotPasswordBudget = AttemptBudget(AttemptLimits(1, 60_000L), MutableClock()),
                    recoveryEmailBudget = AttemptBudget(AttemptLimits(5, 60_000L), MutableClock()),
                )
            }
            val forgotFirst = client.forgotPasswordOutcomeFor("budgets-direction-one-forgot@x.test")
            assertEquals(
                HttpStatusCode.Accepted,
                forgotFirst.status,
                "setup: expected the first forgot-password request to succeed",
            )

            val player = insertPlayer()
            val password = "budgets-direction-one-password"
            credentials.create(player, CredentialKind.PASSWORD, handleForBudgets(player), PresentedSecret(password))
            val token = sessions.issue(player)

            val attach = client.attachOutcomeFor(token, "budgets-direction-one-attach@x.test", password)
            assertEquals(
                HttpStatusCode.Accepted,
                attach.status,
                "expected recovery-email to still succeed from the same address once forgot-password's own budget is spent",
            )
            assertTrue(
                dataSource.hasPendingVerification(player),
                "expected the claim to actually be recorded, proving recovery-email's budget was untouched",
            )
        }

        // Direction two: recovery-email's budget exhausted, forgot-password unaffected from the
        // same remote address.
        testApplication {
            application {
                module()
                intercept(ApplicationCallPipeline.Plugins) {
                    call.mutableOriginConnectionPoint.remoteAddress = "203.0.113.52"
                }
                recoveryRoutes(
                    recoveryEmails,
                    passwordResets,
                    identitiesFor(dataSource),
                    credentials,
                    mailer = mailer,
                    forgotPasswordBudget = AttemptBudget(AttemptLimits(5, 60_000L), MutableClock()),
                    recoveryEmailBudget = AttemptBudget(AttemptLimits(1, 60_000L), MutableClock()),
                )
            }
            val player = insertPlayer()
            val password = "budgets-direction-two-password"
            credentials.create(player, CredentialKind.PASSWORD, handleForBudgets(player), PresentedSecret(password))
            val token = sessions.issue(player)

            val attachFirst = client.attachOutcomeFor(token, "budgets-direction-two-attach@x.test", password)
            assertEquals(HttpStatusCode.Accepted, attachFirst.status, "setup: expected the first attach to succeed")

            val forB = verifiedPlayerFor("budgets-direction-two-forgot@x.test", "budgets-direction-two-forgot-handle")

            val forgot = client.forgotPasswordOutcomeFor("budgets-direction-two-forgot@x.test")
            assertEquals(
                HttpStatusCode.Accepted,
                forgot.status,
                "expected forgot-password to still answer 202 from the same address once recovery-email's own budget is spent",
            )
            assertTrue(
                dataSource.hasPasswordReset(forB),
                "expected the mint to actually happen, proving forgot-password's budget was untouched",
            )
        }
    }

    @Test
    fun theBudgetWindowRollsOnTheInjectedClock() {
        val clock = MutableClock()
        val forgotPasswordBudget = AttemptBudget(AttemptLimits(1, 60_000L), clock)
        testApplication {
            application {
                module()
                recoveryRoutes(
                    recoveryEmails,
                    passwordResets,
                    identitiesFor(emptyMap()),
                    credentials,
                    forgotPasswordBudget = forgotPasswordBudget,
                )
            }
            val admitted = verifiedPlayerFor("budgets-window-admitted@x.test", "budgets-window-admitted-handle")
            val refused = verifiedPlayerFor("budgets-window-refused@x.test", "budgets-window-refused-handle")
            val readmitted = verifiedPlayerFor("budgets-window-readmitted@x.test", "budgets-window-readmitted-handle")

            val first = client.forgotPasswordOutcomeFor("budgets-window-admitted@x.test")
            assertEquals(HttpStatusCode.Accepted, first.status, "setup: expected the first request to succeed")

            // Immediately after, same window, a different address: over budget, mints nothing.
            client.forgotPasswordOutcomeFor("budgets-window-refused@x.test")

            // Past the window's own 60,000 ms: only the clock moved, no test sleeps.
            clock.advance(60_000L)
            client.forgotPasswordOutcomeFor("budgets-window-readmitted@x.test")

            assertTrue(dataSource.hasPasswordReset(admitted), "expected the first, within-budget request to mint")
            assertFalse(dataSource.hasPasswordReset(refused), "expected the second, same-window request to mint nothing")
            assertTrue(
                dataSource.hasPasswordReset(readmitted),
                "expected the third request, sent after the clock advanced past the window, to mint again",
            )
        }
    }

    @Test
    fun aForwardedHeaderDoesNotChooseTheKey() {
        val forgotPasswordBudget = AttemptBudget(AttemptLimits(1, 60_000L), MutableClock())
        testApplication {
            application {
                module()
                intercept(ApplicationCallPipeline.Plugins) {
                    call.mutableOriginConnectionPoint.remoteAddress = "203.0.113.61"
                }
                recoveryRoutes(
                    recoveryEmails,
                    passwordResets,
                    identitiesFor(emptyMap()),
                    credentials,
                    forgotPasswordBudget = forgotPasswordBudget,
                )
            }
            val admitted = verifiedPlayerFor("budgets-forwarded-admitted@x.test", "budgets-forwarded-admitted-handle")
            val spoofed = verifiedPlayerFor("budgets-forwarded-spoofed@x.test", "budgets-forwarded-spoofed-handle")

            // Spends the real address's only slot.
            val first = client.forgotPasswordOutcomeFor("budgets-forwarded-admitted@x.test")
            assertEquals(HttpStatusCode.Accepted, first.status, "setup: expected the first request to succeed")

            // The intercept above still reports the same real remote address; only a client-
            // supplied X-Forwarded-For claims otherwise. If the route ever read it, this would be
            // treated as a fresh key with its own fresh slot and admitted — the status could never
            // show that, which is why this is asserted through the same mechanism as
            // anOverBudgetForgotPasswordMintsNothing.
            val response = client.post("/api/auth/forgot-password") {
                header("X-Forwarded-For", "10.0.0.1")
                header(HttpHeaders.ContentType, "application/json")
                setBody("""{"address":"budgets-forwarded-spoofed@x.test"}""")
            }
            assertEquals(HttpStatusCode.Accepted, response.status, "setup: expected the spoofed request to answer 202 too")

            assertTrue(dataSource.hasPasswordReset(admitted), "expected the first request to mint")
            assertFalse(
                dataSource.hasPasswordReset(spoofed),
                "expected the X-Forwarded-For request to still be refused by the real address's budget",
            )
        }
    }

    /**
     * Creates a player holding a real `password` credential under [handle], then claims and
     * verifies [address] for them directly, bypassing the HTTP layer entirely — the same shape
     * `ForgotPasswordRouteTest`'s own `verifiedPlayerFor` and `AttachRecoveryEmailRouteTest`'s own
     * `attachVerifiedAddress` use. Returns the new player's id.
     */
    private suspend fun verifiedPlayerFor(address: String, handle: String): PlayerId {
        val playerId = insertPlayer()
        credentials.create(playerId, CredentialKind.PASSWORD, handle, PresentedSecret(FIXTURE_PASSWORD))
        val token = VerificationToken("recovery-budgets-test-verify-${UUID.randomUUID()}")
        recoveryEmails.claimPending(playerId, EmailAddress(address), token)
        val result = recoveryEmails.verifyPending(token)
        check(result is VerifyEmailResult.Verified) { "setup: expected the fixture claim to verify, got $result" }
        return playerId
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
 * The login handle a fixture credential is created under in this file, one per player so that a
 * test creating more than one credential in the same database never collides on
 * `UNIQUE (kind, identifier)`.
 */
private fun handleForBudgets(playerId: PlayerId): String = "recovery-budgets-test-handle-${playerId.value}"

/**
 * What a caller who can see only the HTTP response could use to tell two answers from either
 * budgeted route apart: the status, the body text, and the set of header names — lowercased, and
 * never `date`, which varies on every response and carries nothing a caller could read. Two
 * instances that are `equal` are byte-identical on every one of those three channels — the same
 * idiom `ForgotPasswordRouteTest`'s own private `ForgotPasswordOutcome` and
 * `AttachRecoveryEmailRouteTest`'s own private `AttachOutcome` use for their own routes' answers.
 *
 * Named apart from both: a top-level `private` declaration is file-scoped in Kotlin source but
 * still compiles to a JVM class with the bare name, so two files declaring the same one collide at
 * the class file rather than the source level.
 */
private data class RecoveryBudgetOutcome(
    val status: HttpStatusCode,
    val body: String,
    val headerNames: Set<String>,
)

private suspend fun HttpClient.forgotPasswordOutcomeFor(address: String): RecoveryBudgetOutcome {
    val response = post("/api/auth/forgot-password") {
        header(HttpHeaders.ContentType, "application/json")
        setBody("""{"address":"$address"}""")
    }
    return RecoveryBudgetOutcome(
        status = response.status,
        body = response.bodyAsText(),
        headerNames = response.headers.names().map { it.lowercase() }.toSet() - "date",
    )
}

private suspend fun HttpClient.attachOutcomeFor(token: SessionToken, address: String, password: String): RecoveryBudgetOutcome {
    val response = post("/api/auth/recovery-email") {
        header(HttpHeaders.Authorization, "Bearer ${token.value}")
        header(HttpHeaders.ContentType, "application/json")
        setBody("""{"address":"$address","currentPassword":"$password"}""")
    }
    return RecoveryBudgetOutcome(
        status = response.status,
        body = response.bodyAsText(),
        headerNames = response.headers.names().map { it.lowercase() }.toSet() - "date",
    )
}

/** The number of rows in `password_reset`, across every player — the positive control this file's tests rely on. */
private fun DataSource.passwordResetRowCount(): Int {
    connection.use { connection ->
        connection.prepareStatement("SELECT COUNT(*) FROM password_reset").use { statement ->
            statement.executeQuery().use { rows ->
                rows.next()
                return rows.getInt(1)
            }
        }
    }
}

/** Whether a `password_reset` row exists for [playerId]. */
private fun DataSource.hasPasswordReset(playerId: PlayerId): Boolean {
    connection.use { connection ->
        connection.prepareStatement("SELECT COUNT(*) FROM password_reset WHERE player_id = ?").use { statement ->
            statement.setObject(1, UUID.fromString(playerId.value))
            statement.executeQuery().use { rows ->
                rows.next()
                return rows.getInt(1) > 0
            }
        }
    }
}

/** Whether an `email_verification` row exists for [playerId]. */
private fun DataSource.hasPendingVerification(playerId: PlayerId): Boolean {
    connection.use { connection ->
        connection.prepareStatement("SELECT COUNT(*) FROM email_verification WHERE player_id = ?").use { statement ->
            statement.setObject(1, UUID.fromString(playerId.value))
            statement.executeQuery().use { rows ->
                rows.next()
                return rows.getInt(1) > 0
            }
        }
    }
}

/**
 * A [RecoveryMailer] recording every call it receives, both [sendPasswordReset] and
 * [sendVerification] — unlike `ForgotPasswordRouteTest`'s and `AttachRecoveryEmailRouteTest`'s own
 * recording mailers, which each cover one endpoint and throw on the other, this one covers both,
 * because `theTwoEndpointsDoNotShareABudget` drives both endpoints against the same application.
 * Undecorated — no detachment, no coroutine launch — so a request that completes has already
 * recorded its send by the time the response arrives (`ADR-0077`). Named apart from both of the
 * others for the class-file-collision reason [RecoveryBudgetOutcome] is named apart from them.
 */
private class RecordingRecoveryMailerForBudgets : RecoveryMailer {
    private val resets = mutableListOf<EmailAddress>()
    private val verifications = mutableListOf<EmailAddress>()

    /** Every address [sendPasswordReset] was called with, in call order. */
    val resetsSent: List<EmailAddress> get() = resets

    /** Every address [sendVerification] was called with, in call order. */
    val verificationsSent: List<EmailAddress> get() = verifications

    override suspend fun sendVerification(address: EmailAddress, token: VerificationToken) {
        verifications.add(address)
    }

    override suspend fun sendPasswordReset(address: EmailAddress, token: ResetToken, handle: String) {
        resets.add(address)
    }
}
