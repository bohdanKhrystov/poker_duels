package duels.poker.server.http

import duels.poker.server.auth.CredentialKind
import duels.poker.server.auth.EmailAddress
import duels.poker.server.auth.PresentedSecret
import duels.poker.server.auth.RecoveryMailer
import duels.poker.server.auth.RecoveryTokens
import duels.poker.server.auth.ResetToken
import duels.poker.server.auth.VerificationToken
import duels.poker.server.auth.VerifyEmailResult
import duels.poker.server.db.Migrations
import duels.poker.server.db.PostgresCredentials
import duels.poker.server.db.PostgresPasswordResets
import duels.poker.server.db.PostgresRecoveryEmails
import duels.poker.server.db.PostgresTestSupport
import duels.poker.server.mail.NoRecoveryMailer
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
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.UUID
import javax.sql.DataSource

/** The password every fixture credential in this file is created with; no test ever presents it. */
private const val FIXTURE_PASSWORD = "forgot-password-route-test-fixture-password"

/**
 * Tests for `POST /api/auth/forgot-password`, installed by [recoveryRoutes] and driven against a
 * real [PostgresRecoveryEmails], a real [PostgresPasswordResets] and a real [PostgresCredentials] —
 * this route has no double of its own to stand in for any of the three, exactly as
 * [AttachRecoveryEmailRouteTest] and [ResetPasswordRouteTest] drive their own handlers on this same
 * function. `identities` is never touched by this handler — the route is unauthenticated — so every
 * test binds it with `identitiesFor(emptyMap())`, [ResetPasswordRouteTest]'s own choice for the same
 * reason.
 *
 * Every verified fixture in this file is built by [verifiedPlayerFor], which — unlike every
 * `insertPlayer` helper elsewhere in this repository — also creates a real `password` credential,
 * per `TASK-041626`'s own named trap: `resetRecipientOf` now joins `credential`, so a verified
 * address whose owner holds none answers `null`, exactly like an address nobody has mentioned.
 */
class ForgotPasswordRouteTest {
    private lateinit var dataSource: DataSource
    private lateinit var recoveryEmails: PostgresRecoveryEmails
    private lateinit var credentials: PostgresCredentials
    private lateinit var passwordResets: PostgresPasswordResets

    @BeforeEach
    fun setupDatabase() {
        dataSource = PostgresTestSupport.freshDatabase()
        Migrations.migrate(dataSource)
        recoveryEmails = PostgresRecoveryEmails(dataSource, Clock.systemUTC())
        credentials = PostgresCredentials(dataSource)
        passwordResets = PostgresPasswordResets(dataSource, Clock.systemUTC(), RecoveryTokens())
    }

    @Test
    fun allFourCasesAnswerOneIdenticalTwoOhTwo() {
        lateinit var unmentioned: ForgotPasswordOutcome
        lateinit var pending: ForgotPasswordOutcome
        lateinit var verified: ForgotPasswordOutcome
        testApplication {
            application {
                module()
                recoveryRoutes(
                    recoveryEmails,
                    passwordResets,
                    identitiesFor(emptyMap()),
                    credentials,
                    mailer = RecordingRecoveryMailerForForgotPassword(),
                )
            }
            verifiedPlayerFor("verified-4case@x.test", "four-case-verified-handle")
            claimPendingAddressFor("pending-4case@x.test")

            unmentioned = client.forgotPasswordOutcome("nobody-mentioned-4case@x.test")
            pending = client.forgotPasswordOutcome("pending-4case@x.test")
            verified = client.forgotPasswordOutcome("verified-4case@x.test")
        }
        lateinit var noSenderConfigured: ForgotPasswordOutcome
        testApplication {
            application {
                module()
                recoveryRoutes(
                    recoveryEmails,
                    passwordResets,
                    identitiesFor(emptyMap()),
                    credentials,
                    mailer = NoRecoveryMailer,
                )
            }
            noSenderConfigured = client.forgotPasswordOutcome("no-sender-configured-4case@x.test")
        }

        assertEquals(HttpStatusCode.Accepted, unmentioned.status, "expected 202 for an address nobody has mentioned")
        assertEquals("", unmentioned.body, "expected an empty response body")
        assertEquals(
            unmentioned,
            pending,
            "expected an unmentioned and a pending-but-unverified address to answer identically",
        )
        assertEquals(unmentioned, verified, "expected an unmentioned and a verified address to answer identically")
        assertEquals(
            unmentioned,
            noSenderConfigured,
            "expected an unmentioned address and a caller with no sender configured to answer identically",
        )
    }

    @Test
    fun onlyTheVerifiedCaseMintsAToken() {
        var verifiedPlayer: PlayerId? = null
        testApplication {
            application {
                module()
                recoveryRoutes(
                    recoveryEmails,
                    passwordResets,
                    identitiesFor(emptyMap()),
                    credentials,
                    mailer = RecordingRecoveryMailerForForgotPassword(),
                )
            }
            verifiedPlayer = verifiedPlayerFor("verified-mint-check@x.test", "mint-check-verified-handle")
            claimPendingAddressFor("pending-mint-check@x.test")

            client.forgotPasswordOutcome("nobody-mentioned-mint-check@x.test")
            client.forgotPasswordOutcome("pending-mint-check@x.test")
            client.forgotPasswordOutcome("verified-mint-check@x.test")
        }
        testApplication {
            application {
                module()
                recoveryRoutes(
                    recoveryEmails,
                    passwordResets,
                    identitiesFor(emptyMap()),
                    credentials,
                    mailer = NoRecoveryMailer,
                )
            }
            client.forgotPasswordOutcome("no-sender-configured-mint-check@x.test")
        }

        assertEquals(1, dataSource.passwordResetRowCount(), "expected exactly one password_reset row across all four requests")
        assertEquals(
            checkNotNull(verifiedPlayer) { "setup: expected the verified case to have run" },
            dataSource.passwordResetOwner(),
            "expected the one minted row to belong to the verified address's own owner",
        )
    }

    @Test
    fun aSecondRequestInsideAQuarterHourSendsNothingAndKeepsTheLink() {
        val mailer = RecordingRecoveryMailerForForgotPassword()
        val clock = MutableClockForForgotPassword(WINDOW_TEST_START)
        val passwordResetsWithAMovableClock = PostgresPasswordResets(dataSource, clock, RecoveryTokens())
        var verifiedPlayer: PlayerId? = null
        lateinit var first: ForgotPasswordOutcome
        lateinit var second: ForgotPasswordOutcome
        lateinit var firstHash: ByteArray
        testApplication {
            application {
                module()
                recoveryRoutes(
                    recoveryEmails,
                    passwordResetsWithAMovableClock,
                    identitiesFor(emptyMap()),
                    credentials,
                    mailer = mailer,
                )
            }
            val player = verifiedPlayerFor("quarter-hour-inside@x.test", "quarter-hour-inside-handle")
            verifiedPlayer = player

            first = client.forgotPasswordOutcome("quarter-hour-inside@x.test")
            firstHash = checkNotNull(dataSource.passwordResetTokenHash(player)) {
                "setup: expected the first request to mint a token"
            }

            clock.advance(Duration.ofMinutes(INSIDE_THE_SUPPRESSION_WINDOW_MINUTES))
            second = client.forgotPasswordOutcome("quarter-hour-inside@x.test")
        }

        assertEquals(HttpStatusCode.Accepted, first.status, "setup: expected the first request to succeed")
        assertEquals(first, second, "expected byte-identical (status, body, header names) for both requests")
        assertArrayEquals(
            firstHash,
            dataSource.passwordResetTokenHash(checkNotNull(verifiedPlayer) { "setup: expected the fixture to have run" }),
            "expected the stored token_hash to still be the first request's, fourteen minutes later",
        )
        assertEquals(1, mailer.sent.size, "expected exactly one send — a second request inside the window sends nothing")
    }

    @Test
    fun aSecondRequestAfterAQuarterHourSendsAgain() {
        val mailer = RecordingRecoveryMailerForForgotPassword()
        val clock = MutableClockForForgotPassword(WINDOW_TEST_START)
        val passwordResetsWithAMovableClock = PostgresPasswordResets(dataSource, clock, RecoveryTokens())
        var verifiedPlayer: PlayerId? = null
        lateinit var first: ForgotPasswordOutcome
        lateinit var second: ForgotPasswordOutcome
        lateinit var firstHash: ByteArray
        testApplication {
            application {
                module()
                recoveryRoutes(
                    recoveryEmails,
                    passwordResetsWithAMovableClock,
                    identitiesFor(emptyMap()),
                    credentials,
                    mailer = mailer,
                )
            }
            val player = verifiedPlayerFor("quarter-hour-after@x.test", "quarter-hour-after-handle")
            verifiedPlayer = player

            first = client.forgotPasswordOutcome("quarter-hour-after@x.test")
            firstHash = checkNotNull(dataSource.passwordResetTokenHash(player)) {
                "setup: expected the first request to mint a token"
            }

            clock.advance(Duration.ofMinutes(PAST_THE_SUPPRESSION_WINDOW_MINUTES))
            second = client.forgotPasswordOutcome("quarter-hour-after@x.test")
        }

        assertEquals(HttpStatusCode.Accepted, first.status, "setup: expected the first request to succeed")
        assertEquals(
            HttpStatusCode.Accepted,
            second.status,
            "expected the request past the fifteen-minute window to succeed too",
        )
        assertFalse(
            firstHash.contentEquals(
                checkNotNull(
                    dataSource.passwordResetTokenHash(
                        checkNotNull(verifiedPlayer) { "setup: expected the fixture to have run" },
                    ),
                ),
            ),
            "expected the stored token_hash to change once the fifteen-minute window has elapsed",
        )
        assertEquals(2, mailer.sent.size, "expected two sends, one per request, once the fifteen-minute window has elapsed")
    }

    @Test
    fun aMalformedBodyAnswersTwoOhTwo() {
        lateinit var emptyBody: ForgotPasswordOutcome
        lateinit var emptyObject: ForgotPasswordOutcome
        testApplication {
            application {
                module()
                recoveryRoutes(
                    recoveryEmails,
                    passwordResets,
                    identitiesFor(emptyMap()),
                    credentials,
                    mailer = RecordingRecoveryMailerForForgotPassword(),
                )
            }
            emptyBody = client.forgotPasswordOutcomeWithBody("")
            emptyObject = client.forgotPasswordOutcomeWithBody("{}")
        }

        assertEquals(HttpStatusCode.Accepted, emptyBody.status, "expected 202 for an empty body")
        assertEquals(emptyBody, emptyObject, "expected an empty body and {} to answer identically")
        assertEquals(0, dataSource.passwordResetRowCount(), "expected a malformed body to mint nothing")
    }

    @Test
    fun theMailCarriesTheOwnersOwnHandle() {
        val mailer = RecordingRecoveryMailerForForgotPassword()
        testApplication {
            application {
                module()
                recoveryRoutes(recoveryEmails, passwordResets, identitiesFor(emptyMap()), credentials, mailer = mailer)
            }
            verifiedPlayerFor("first-owner@x.test", "first-owner-handle")
            verifiedPlayerFor("second-owner@x.test", "second-owner-handle")

            val first = client.forgotPasswordOutcome("first-owner@x.test")
            val second = client.forgotPasswordOutcome("second-owner@x.test")

            assertEquals(HttpStatusCode.Accepted, first.status, "setup: expected the first request to succeed")
            assertEquals(HttpStatusCode.Accepted, second.status, "setup: expected the second request to succeed")
        }

        assertEquals(2, mailer.sent.size, "expected exactly one send per verified address")
        val firstSent = mailer.sent.single { it.address == EmailAddress("first-owner@x.test") }
        val secondSent = mailer.sent.single { it.address == EmailAddress("second-owner@x.test") }
        assertEquals(
            "first-owner-handle",
            firstSent.handle,
            "expected the first address's mail to carry its own owner's handle",
        )
        assertEquals(
            "second-owner-handle",
            secondSent.handle,
            "expected the second address's mail to carry its own owner's handle, not the first's",
        )
    }

    @Test
    fun theResponseNeverCarriesTheAddress() {
        testApplication {
            application {
                module()
                recoveryRoutes(
                    recoveryEmails,
                    passwordResets,
                    identitiesFor(emptyMap()),
                    credentials,
                    mailer = RecordingRecoveryMailerForForgotPassword(),
                )
            }
            val firstAddress = "leakcheck-first@x.test"
            val firstHandle = "leakcheck-first-handle"
            val secondAddress = "leakcheck-second@x.test"
            val secondHandle = "leakcheck-second-handle"
            verifiedPlayerFor(firstAddress, firstHandle)
            verifiedPlayerFor(secondAddress, secondHandle)
            val sensitive = listOf(firstAddress, secondAddress, firstHandle, secondHandle)

            val first = client.post("/api/auth/forgot-password") {
                header(HttpHeaders.ContentType, "application/json")
                setBody("""{"address":"$firstAddress"}""")
            }
            assertEquals(HttpStatusCode.Accepted, first.status, "setup: expected the first request to succeed")
            assertResponseNeverCarries(first, sensitive)

            val second = client.post("/api/auth/forgot-password") {
                header(HttpHeaders.ContentType, "application/json")
                setBody("""{"address":"$secondAddress"}""")
            }
            assertEquals(HttpStatusCode.Accepted, second.status, "setup: expected the second request to succeed")
            assertResponseNeverCarries(second, sensitive)

            val unmentioned = client.post("/api/auth/forgot-password") {
                header(HttpHeaders.ContentType, "application/json")
                setBody("""{"address":"leakcheck-unmentioned@x.test"}""")
            }
            assertEquals(
                HttpStatusCode.Accepted,
                unmentioned.status,
                "setup: expected an unmentioned address to answer 202 too",
            )
            assertResponseNeverCarries(unmentioned, sensitive)

            val malformed = client.post("/api/auth/forgot-password") {
                header(HttpHeaders.ContentType, "application/json")
                setBody("{}")
            }
            assertEquals(HttpStatusCode.Accepted, malformed.status, "setup: expected a malformed body to answer 202 too")
            assertResponseNeverCarries(malformed, sensitive)
        }
    }

    /**
     * Creates a player holding a real `password` credential under [handle], then claims and
     * verifies [address] for them directly, bypassing the HTTP layer entirely — the same shape
     * [AttachRecoveryEmailRouteTest]'s own `attachVerifiedAddress` uses. Returns the new player's
     * id.
     */
    private suspend fun verifiedPlayerFor(address: String, handle: String): PlayerId {
        val playerId = insertPlayer()
        credentials.create(playerId, CredentialKind.PASSWORD, handle, PresentedSecret(FIXTURE_PASSWORD))
        val token = VerificationToken("forgot-password-route-test-verify-${UUID.randomUUID()}")
        recoveryEmails.claimPending(playerId, EmailAddress(address), token)
        val result = recoveryEmails.verifyPending(token)
        check(result is VerifyEmailResult.Verified) { "setup: expected the fixture claim to verify, got $result" }
        return playerId
    }

    /** Creates a player and claims [address] for them, deliberately never verifying it. */
    private suspend fun claimPendingAddressFor(address: String): PlayerId {
        val playerId = insertPlayer()
        val token = VerificationToken("forgot-password-route-test-pending-${UUID.randomUUID()}")
        recoveryEmails.claimPending(playerId, EmailAddress(address), token)
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

/** The instant [MutableClockForForgotPassword] starts at in every test that builds one. */
private val WINDOW_TEST_START: Instant = Instant.parse("2026-01-01T00:00:00Z")

/** How far the "inside the window" test advances the clock — inside `ADR-0031` §5's fifteen minutes. */
private const val INSIDE_THE_SUPPRESSION_WINDOW_MINUTES = 14L

/**
 * How far the "after the window" test advances the clock — past `ADR-0031` §5's fifteen minutes,
 * the same sixteen-minute-past-the-window fixture `TASK-041636` and `AttachRecoveryEmailRouteTest`
 * use, chosen once rather than invented again here.
 */
private const val PAST_THE_SUPPRESSION_WINDOW_MINUTES = 16L

/**
 * The number of consecutive characters that, shared between a secret and a response channel, count
 * as a leak — `theResponseNeverCarriesTheAddress` refuses any run **longer than three** characters,
 * so the window checked here is four.
 */
private const val LEAK_WINDOW = 4

/** Whether [text] contains [secret] itself, or any of its own substrings of [LEAK_WINDOW] characters. */
private fun containsFragmentOf(text: String, secret: String): Boolean {
    if (secret.length < LEAK_WINDOW) {
        return secret.isNotEmpty() && text.contains(secret)
    }
    return (0..secret.length - LEAK_WINDOW).any { start -> text.contains(secret.substring(start, start + LEAK_WINDOW)) }
}

/**
 * Asserts that neither [response]'s body nor any of its header values contains any of [secrets] —
 * an address or a handle — or any of their own substrings of [LEAK_WINDOW] characters.
 */
private suspend fun assertResponseNeverCarries(response: HttpResponse, secrets: List<String>) {
    val body = response.bodyAsText()
    secrets.forEach { secret ->
        assertFalse(
            containsFragmentOf(body, secret),
            "expected the response body to carry no fragment of $secret, was: $body",
        )
        response.headers.forEach { name, values ->
            values.forEach { value ->
                assertFalse(
                    containsFragmentOf(value, secret),
                    "expected header $name to carry no fragment of $secret, was: $value",
                )
            }
        }
    }
}

/**
 * What a caller who can see only the HTTP response could use to tell two `POST
 * /api/auth/forgot-password` answers apart: the status, the body text, and the set of header
 * names — lowercased, and never `date`, which varies on every response and is not a channel that
 * carries anything a caller could read. Two instances that are `equal` are byte-identical on every
 * one of those three channels — the same idiom [AttachRecoveryEmailRouteTest]'s private
 * `AttachOutcome` and [DetachRecoveryEmailRouteTest]'s private `DetachOutcome` use for their own
 * routes' answers.
 *
 * Named apart from both: a top-level `private` declaration is file-scoped in Kotlin source but
 * still compiles to a JVM class with the bare name, so two files declaring the same one collide at
 * the class file rather than the source level.
 */
private data class ForgotPasswordOutcome(
    val status: HttpStatusCode,
    val body: String,
    val headerNames: Set<String>,
)

private suspend fun HttpClient.forgotPasswordOutcomeWithBody(body: String): ForgotPasswordOutcome {
    val response = post("/api/auth/forgot-password") {
        header(HttpHeaders.ContentType, "application/json")
        setBody(body)
    }
    return ForgotPasswordOutcome(
        status = response.status,
        body = response.bodyAsText(),
        headerNames = response.headers.names().map { it.lowercase() }.toSet() - "date",
    )
}

private suspend fun HttpClient.forgotPasswordOutcome(address: String): ForgotPasswordOutcome =
    forgotPasswordOutcomeWithBody("""{"address":"$address"}""")

/** The number of rows in `password_reset`, for `onlyTheVerifiedCaseMintsAToken`'s positive control. */
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

/** The `player_id` of the single `password_reset` row, or `null` if none exists. */
private fun DataSource.passwordResetOwner(): PlayerId? {
    connection.use { connection ->
        connection.prepareStatement("SELECT player_id FROM password_reset").use { statement ->
            statement.executeQuery().use { rows ->
                return if (rows.next()) PlayerId(rows.getObject("player_id", UUID::class.java).toString()) else null
            }
        }
    }
}

/** The `token_hash` column of the `password_reset` row for [playerId], or `null` if none exists. */
private fun DataSource.passwordResetTokenHash(playerId: PlayerId): ByteArray? {
    connection.use { connection ->
        connection.prepareStatement("SELECT token_hash FROM password_reset WHERE player_id = ?").use { statement ->
            statement.setObject(1, UUID.fromString(playerId.value))
            statement.executeQuery().use { rows -> return if (rows.next()) rows.getBytes("token_hash") else null }
        }
    }
}

/**
 * A [sendPasswordReset][RecoveryMailer.sendPasswordReset] call recorded by
 * [RecordingRecoveryMailerForForgotPassword], carrying every argument the port passed so a test can
 * assert on the handle without assuming anything about the token's shape.
 */
private data class SentReset(val address: EmailAddress, val token: ResetToken, val handle: String)

/**
 * A [RecoveryMailer] recording every [sendPasswordReset][RecoveryMailer.sendPasswordReset] call it
 * receives, undecorated — no detachment, no coroutine launch — so a request that completes has
 * already recorded its send by the time the response arrives, and [sent] is decidable with no join,
 * no channel and no timeout (`ADR-0077`). Named apart from
 * [AttachRecoveryEmailRouteTest]'s own private `RecordingRecoveryMailer`, for the class-file-
 * collision reason [ForgotPasswordOutcome] is named apart from `AttachOutcome`.
 */
private class RecordingRecoveryMailerForForgotPassword : RecoveryMailer {
    private val recorded = mutableListOf<SentReset>()

    /** Every [sendPasswordReset][RecoveryMailer.sendPasswordReset] call this mailer received, in call order. */
    val sent: List<SentReset> get() = recorded

    override suspend fun sendVerification(address: EmailAddress, token: VerificationToken) {
        throw UnsupportedOperationException("forgot-password never sends a verification mail")
    }

    override suspend fun sendPasswordReset(address: EmailAddress, token: ResetToken, handle: String) {
        recorded.add(SentReset(address, token, handle))
    }
}

/**
 * A [Clock] whose instant advances only when [advance] is called, so a test can move past
 * `ADR-0031` §5's fifteen-minute suppression window without a real fifteen minutes elapsing.
 * [PostgresPasswordResets] reads this clock's [instant], never SQL `now()`, to decide the window —
 * the same kind of mutable holder [AttachRecoveryEmailRouteTest]'s private `MutableClock` builds
 * for [PostgresRecoveryEmails]'s own suppression window, named apart from it for the same
 * class-file-collision reason.
 */
private class MutableClockForForgotPassword(instant: Instant) : Clock() {
    private var delegate: Clock = Clock.fixed(instant, ZoneOffset.UTC)

    override fun instant(): Instant = delegate.instant()

    override fun getZone(): ZoneId = delegate.zone

    override fun withZone(zone: ZoneId): Clock = delegate.withZone(zone)

    /** Moves this clock's instant forward by [duration], visible to the next call to [instant]. */
    fun advance(duration: Duration) {
        delegate = Clock.fixed(delegate.instant().plus(duration), delegate.zone)
    }
}
