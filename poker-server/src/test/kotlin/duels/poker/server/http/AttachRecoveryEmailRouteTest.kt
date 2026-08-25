package duels.poker.server.http

import duels.poker.server.auth.CredentialKind
import duels.poker.server.auth.EmailAddress
import duels.poker.server.auth.PasswordResets
import duels.poker.server.auth.PresentedSecret
import duels.poker.server.auth.RecoveryMailer
import duels.poker.server.auth.ResetToken
import duels.poker.server.auth.SessionToken
import duels.poker.server.auth.VerificationToken
import duels.poker.server.auth.VerifyEmailResult
import duels.poker.server.db.Migrations
import duels.poker.server.db.PostgresAuthSessions
import duels.poker.server.db.PostgresCredentials
import duels.poker.server.db.PostgresPlayerDirectory
import duels.poker.server.db.PostgresRecoveryEmails
import duels.poker.server.db.PostgresTestSupport
import duels.poker.server.module
import duels.poker.server.session.DeviceId
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
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Clock
import java.util.UUID
import javax.sql.DataSource

/**
 * Tests for `POST /api/auth/recovery-email`, installed by [recoveryRoutes] and driven against a
 * real [PostgresRecoveryEmails], a real [PostgresCredentials] and a real [PostgresAuthSessions] —
 * this route has no double of its own to stand in for any of the three, exactly as
 * [DetachRecoveryEmailRouteTest] drives the `DELETE` on this same path with the same three real
 * ports. The identity-then-password guard order and the Postgres-backed fixture below mirror that
 * class; only the mailer is new, because `DELETE` never sends mail.
 *
 * [sessions] is a second, independent [PostgresAuthSessions] instance from the one
 * [identitiesFor]'s database-backed overload builds internally, issuing against the identical
 * `auth_session` table on [dataSource] — so a token it mints is one the route's own
 * `IdentityResolver` genuinely resolves, never a fixture that only looks like a session.
 */
class AttachRecoveryEmailRouteTest {
    private lateinit var dataSource: DataSource
    private lateinit var recoveryEmails: PostgresRecoveryEmails
    private lateinit var credentials: PostgresCredentials
    private lateinit var sessions: PostgresAuthSessions

    @BeforeEach
    fun setupDatabase() {
        dataSource = PostgresTestSupport.freshDatabase()
        Migrations.migrate(dataSource)
        recoveryEmails = PostgresRecoveryEmails(dataSource, Clock.systemUTC())
        credentials = PostgresCredentials(dataSource)
        sessions = PostgresAuthSessions(dataSource, Clock.systemUTC())
    }

    @Test
    fun theRightPasswordRecordsAPendingClaim() {
        testApplication {
            application {
                module()
                recoveryRoutes(recoveryEmails, PasswordResetsNeverCalledByAttach, identitiesFor(dataSource), credentials)
            }
            val playerId = insertPlayer()
            val password = "the one true original password"
            credentials.create(playerId, CredentialKind.PASSWORD, handleFor(playerId), PresentedSecret(password))
            val token = sessions.issue(playerId)
            // Case survives — ADR-0078 §6's own accepted fixture, and Proof step 6's target: a
            // .lowercase() on the way into storage reddens this test alone.
            val address = "Bob@Example.com"

            val response = client.post("/api/auth/recovery-email") {
                header(HttpHeaders.Authorization, "Bearer ${token.value}")
                header(HttpHeaders.ContentType, "application/json")
                setBody("""{"address":"$address","currentPassword":"$password"}""")
            }

            assertEquals(HttpStatusCode.Accepted, response.status)
            assertEquals("", response.bodyAsText())
            assertEquals(
                address,
                dataSource.pendingVerificationAddress(playerId),
                "expected email_verification to hold the address exactly as typed",
            )
            assertFalse(
                recoveryEmails.hasRecoveryEmail(playerId),
                "expected hasRecoveryEmail false until the address is proven",
            )
        }
    }

    @Test
    fun anAddressAlreadyProvenElsewhereStillAnswersTwoOhTwo() {
        var baseline: AttachOutcome? = null
        testApplication {
            application {
                module()
                recoveryRoutes(recoveryEmails, PasswordResetsNeverCalledByAttach, identitiesFor(dataSource), credentials)
            }
            val player = insertPlayer()
            val password = "the baseline player's own password"
            credentials.create(player, CredentialKind.PASSWORD, handleFor(player), PresentedSecret(password))
            val token = sessions.issue(player)
            baseline = client.attachOutcome(token, "fresh-for-comparison@attach-route-test.example", password)
        }
        val success = checkNotNull(baseline) { "setup: expected the baseline attach call to complete" }
        assertEquals(HttpStatusCode.Accepted, success.status, "setup: the baseline attach must actually succeed")

        val mailer = RecordingRecoveryMailer()
        testApplication {
            application {
                module()
                recoveryRoutes(
                    recoveryEmails,
                    PasswordResetsNeverCalledByAttach,
                    identitiesFor(dataSource),
                    credentials,
                    mailer = mailer,
                )
            }
            val firstPlayer = insertPlayer()
            val takenAddress = "already-claimed@attach-route-test.example"
            attachVerifiedAddress(firstPlayer, takenAddress)

            val secondPlayer = insertPlayer()
            val secondPassword = "the second player's own password"
            credentials.create(
                secondPlayer,
                CredentialKind.PASSWORD,
                handleFor(secondPlayer),
                PresentedSecret(secondPassword),
            )
            val secondToken = sessions.issue(secondPlayer)

            val contested = client.attachOutcome(secondToken, takenAddress, secondPassword)

            assertEquals(
                success,
                contested,
                "expected byte-identical (status, body, header names) for an address already " +
                    "verified elsewhere and a fresh one",
            )
        }
        assertEquals(
            emptyList<EmailAddress>(),
            mailer.sent,
            "expected no mail sent for an address already verified elsewhere",
        )
    }

    @Test
    fun aWrongPasswordAnswersFourHundredAndThreeAndRecordsNothing() {
        testApplication {
            application {
                module()
                recoveryRoutes(recoveryEmails, PasswordResetsNeverCalledByAttach, identitiesFor(dataSource), credentials)
            }
            val playerId = insertPlayer()
            credentials.create(
                playerId,
                CredentialKind.PASSWORD,
                handleFor(playerId),
                PresentedSecret("the actual current password"),
            )
            val token = sessions.issue(playerId)

            val response = client.post("/api/auth/recovery-email") {
                header(HttpHeaders.Authorization, "Bearer ${token.value}")
                header(HttpHeaders.ContentType, "application/json")
                setBody(
                    """{"address":"survives-a-wrong-password@attach-route-test.example",""" +
                        """"currentPassword":"not the actual password"}""",
                )
            }

            assertEquals(HttpStatusCode.Forbidden, response.status)
            assertNull(
                dataSource.pendingVerificationAddress(playerId),
                "expected no email_verification row for a wrong-password attempt",
            )
        }
    }

    @Test
    fun noSessionAnswersFourHundredAndOne() {
        testApplication {
            application {
                module()
                recoveryRoutes(recoveryEmails, PasswordResetsNeverCalledByAttach, identitiesFor(dataSource), credentials)
            }
            val playerId = insertPlayer()
            credentials.create(
                playerId,
                CredentialKind.PASSWORD,
                handleFor(playerId),
                PresentedSecret("a password nobody presents in this test"),
            )

            // No Authorization header anywhere in this request, but a well-formed *body* — the
            // JSON decodes fine, so a route that decoded before resolving identity would still
            // decode this one and proceed, rather than accidentally answering 400 for an
            // unrelated reason. The address itself is deliberately one emailAddressOrNull refuses
            // (no @): a route that checked the address before identity would answer 400 here too,
            // teaching a stranger the address rule without an account, which is exactly what a
            // well-formed address could never distinguish from the correct 401.
            val response = client.post("/api/auth/recovery-email") {
                header(HttpHeaders.ContentType, "application/json")
                setBody(
                    """{"address":"survives-no-session-and-is-not-an-address",""" +
                        """"currentPassword":"whatever a stranger might guess"}""",
                )
            }

            assertEquals(HttpStatusCode.Unauthorized, response.status)
            assertEquals("", response.bodyAsText())
            assertNull(
                dataSource.pendingVerificationAddress(playerId),
                "expected no email_verification row for an unauthenticated request",
            )
        }
    }

    @Test
    fun aDeviceIdentityAloneAnswersFourHundredAndOne() {
        testApplication {
            application {
                module()
                recoveryRoutes(recoveryEmails, PasswordResetsNeverCalledByAttach, identitiesFor(dataSource), credentials)
            }
            // The device genuinely resolves to a real player, minted the same way production
            // mints one on first contact (PostgresPlayerDirectory.resolve): a 401 below must be
            // about the guard refusing a resolvable device, never about a fixture that failed to
            // resolve at all. This is the failure TASK-041623 was rejected for on the DELETE
            // sibling of this route: accepting Identity.Device and reaching verifyCurrent instead
            // of refusing outright.
            val deviceId = DeviceId("attach-route-test-device-${UUID.randomUUID()}")
            val playerId = PostgresPlayerDirectory(dataSource).resolve(deviceId).id
            credentials.create(
                playerId,
                CredentialKind.PASSWORD,
                handleFor(playerId),
                PresentedSecret("a password nobody presents in this test"),
            )

            // X-Device-Id only, no Authorization header: a route resolving identity through a
            // more permissive helper (Identity.Session or Identity.Device) would resolve this
            // caller and reach 403 or 202 instead of refusing it outright.
            val response = client.post("/api/auth/recovery-email") {
                header(DEVICE_ID_HEADER, deviceId.value)
                header(HttpHeaders.ContentType, "application/json")
                setBody(
                    """{"address":"survives-a-device-identity@attach-route-test.example",""" +
                        """"currentPassword":"whatever a device identity might present"}""",
                )
            }

            assertEquals(HttpStatusCode.Unauthorized, response.status)
            assertEquals("", response.bodyAsText())
            assertNull(
                dataSource.pendingVerificationAddress(playerId),
                "expected no email_verification row for a device-identity-only request",
            )
        }
    }

    @Test
    fun aStringThatIsNotAnAddressAnswersFourHundred() {
        testApplication {
            application {
                module()
                recoveryRoutes(recoveryEmails, PasswordResetsNeverCalledByAttach, identitiesFor(dataSource), credentials)
            }
            val playerId = insertPlayer()
            val password = "the account holder's own password"
            credentials.create(playerId, CredentialKind.PASSWORD, handleFor(playerId), PresentedSecret(password))
            val token = sessions.issue(playerId)

            // "bob": no @, the one refusal a player can act on (ADR-0078 §6's refused table). The
            // password is right, so a 400 here is attributable to the address rule alone.
            val response = client.post("/api/auth/recovery-email") {
                header(HttpHeaders.Authorization, "Bearer ${token.value}")
                header(HttpHeaders.ContentType, "application/json")
                setBody("""{"address":"bob","currentPassword":"$password"}""")
            }

            assertEquals(HttpStatusCode.BadRequest, response.status)
            assertNull(
                dataSource.pendingVerificationAddress(playerId),
                "expected no email_verification row for a refused address",
            )
        }
    }

    @Test
    fun theResponseNeverCarriesTheAddress() {
        testApplication {
            application {
                module()
                recoveryRoutes(recoveryEmails, PasswordResetsNeverCalledByAttach, identitiesFor(dataSource), credentials)
            }
            val playerId = insertPlayer()
            val password = "the leak-check player's own password"
            credentials.create(playerId, CredentialKind.PASSWORD, handleFor(playerId), PresentedSecret(password))
            val token = sessions.issue(playerId)

            val acceptedAddress = "leakcheck-202-marker@attach-route-test.example"
            val accepted = client.post("/api/auth/recovery-email") {
                header(HttpHeaders.Authorization, "Bearer ${token.value}")
                header(HttpHeaders.ContentType, "application/json")
                setBody("""{"address":"$acceptedAddress","currentPassword":"$password"}""")
            }
            assertEquals(HttpStatusCode.Accepted, accepted.status, "setup: the leak-check attach must actually succeed")
            assertResponseNeverCarries(accepted, acceptedAddress)

            val badAddress = "leakcheck400marker"
            val badRequest = client.post("/api/auth/recovery-email") {
                header(HttpHeaders.Authorization, "Bearer ${token.value}")
                header(HttpHeaders.ContentType, "application/json")
                setBody("""{"address":"$badAddress","currentPassword":"$password"}""")
            }
            assertEquals(HttpStatusCode.BadRequest, badRequest.status, "setup: expected a refused address to answer 400")
            assertResponseNeverCarries(badRequest, badAddress)

            val noSessionAddress = "leakcheck-401-marker@attach-route-test.example"
            val unauthorized = client.post("/api/auth/recovery-email") {
                header(HttpHeaders.ContentType, "application/json")
                setBody("""{"address":"$noSessionAddress","currentPassword":"whatever"}""")
            }
            assertEquals(HttpStatusCode.Unauthorized, unauthorized.status, "setup: expected no session to answer 401")
            assertResponseNeverCarries(unauthorized, noSessionAddress)

            val wrongPasswordAddress = "leakcheck-403-marker@attach-route-test.example"
            val forbidden = client.post("/api/auth/recovery-email") {
                header(HttpHeaders.Authorization, "Bearer ${token.value}")
                header(HttpHeaders.ContentType, "application/json")
                setBody("""{"address":"$wrongPasswordAddress","currentPassword":"the wrong password"}""")
            }
            assertEquals(HttpStatusCode.Forbidden, forbidden.status, "setup: expected a wrong password to answer 403")
            assertResponseNeverCarries(forbidden, wrongPasswordAddress)
        }
    }

    /** Claims and verifies [address] for [playerId] directly, bypassing the HTTP layer entirely. */
    private suspend fun attachVerifiedAddress(playerId: PlayerId, address: String) {
        val token = VerificationToken("attach-route-test-token-${playerId.value}")
        recoveryEmails.claimPending(playerId, EmailAddress(address), token)
        val result = recoveryEmails.verifyPending(token)
        check(result is VerifyEmailResult.Verified) { "setup: expected the fixture claim to verify, got $result" }
    }

    /**
     * Asserts that neither [response]'s body nor any of its header values contains [address] or
     * any of its own substrings longer than three characters — the check
     * `theResponseNeverCarriesTheAddress` runs against every one of the endpoint's four answers.
     */
    private suspend fun assertResponseNeverCarries(response: HttpResponse, address: String) {
        val body = response.bodyAsText()
        assertFalse(
            containsAddressFragment(body, address),
            "expected the response body to carry no fragment of the address, was: $body",
        )
        response.headers.forEach { name, values ->
            values.forEach { value ->
                assertFalse(
                    containsAddressFragment(value, address),
                    "expected header $name to carry no fragment of the address, was: $value",
                )
            }
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
 * The login handle a fixture credential is created under, one per player so that a test creating
 * more than one credential in the same database never collides on `UNIQUE (kind, identifier)`.
 */
private fun handleFor(playerId: PlayerId): String = "attach-route-test-handle-${playerId.value}"

/**
 * The number of consecutive characters that, shared between the address and a response channel,
 * count as a leak — `theResponseNeverCarriesTheAddress` refuses any run **longer than three**
 * characters, so the window checked here is four.
 */
private const val ADDRESS_LEAK_WINDOW = 4

/** Whether [text] contains [address] itself, or any of its own substrings of four characters. */
private fun containsAddressFragment(text: String, address: String): Boolean {
    if (address.length < ADDRESS_LEAK_WINDOW) {
        return address.isNotEmpty() && text.contains(address)
    }
    return (0..address.length - ADDRESS_LEAK_WINDOW).any { start ->
        text.contains(address.substring(start, start + ADDRESS_LEAK_WINDOW))
    }
}

/**
 * What a caller who can see only the HTTP response could use to tell two `POST
 * /api/auth/recovery-email` answers apart: the status, the body text, and the set of header
 * names — lowercased, and never `date`, which varies on every response and is not a channel that
 * carries anything a caller could read. Two instances that are `equal` are byte-identical on
 * every one of those three channels — the same idiom `DetachRecoveryEmailRouteTest.kt`'s private
 * `DetachOutcome` uses for `DELETE /api/auth/recovery-email`'s own answers.
 *
 * Named apart from that class: a top-level `private` declaration is file-scoped in Kotlin source
 * but still compiles to a JVM class with the bare name, so two files declaring the same one
 * collide at the class file rather than the source level.
 */
private data class AttachOutcome(
    val status: HttpStatusCode,
    val body: String,
    val headerNames: Set<String>,
)

private suspend fun HttpClient.attachOutcome(token: SessionToken, address: String, password: String): AttachOutcome {
    val response = post("/api/auth/recovery-email") {
        header(HttpHeaders.Authorization, "Bearer ${token.value}")
        header(HttpHeaders.ContentType, "application/json")
        setBody("""{"address":"$address","currentPassword":"$password"}""")
    }
    return AttachOutcome(
        status = response.status,
        body = response.bodyAsText(),
        headerNames = response.headers.names().map { it.lowercase() }.toSet() - "date",
    )
}

/** The `address` column of the `email_verification` row for [playerId], or `null` if none exists. */
private fun DataSource.pendingVerificationAddress(playerId: PlayerId): String? {
    connection.use { connection ->
        connection.prepareStatement("SELECT address FROM email_verification WHERE player_id = ?").use { statement ->
            statement.setObject(1, UUID.fromString(playerId.value))
            statement.executeQuery().use { rows -> return if (rows.next()) rows.getString("address") else null }
        }
    }
}

/**
 * `recoveryRoutes`' `recovery-email` handler never touches [PasswordResets]; a call reaching
 * either method here would mean the handler read the wrong parameter. Named apart from
 * `DetachRecoveryEmailRouteTest`'s private `PasswordResetsNeverCalled`, `VerifyEmailRefusalsTest`'s
 * private `StubPasswordResets` and `VerifyEmailRouteTest`'s private `NoPasswordResets`, for the
 * same class-file-collision reason [AttachOutcome] is named apart from `DetachOutcome`.
 */
private object PasswordResetsNeverCalledByAttach : PasswordResets {
    override suspend fun issue(playerId: PlayerId, token: ResetToken): Boolean {
        throw UnsupportedOperationException("recovery-email never issues a reset token")
    }

    override suspend fun consume(token: ResetToken, secret: PresentedSecret): Boolean {
        throw UnsupportedOperationException("recovery-email never consumes a reset token")
    }
}

/**
 * A [RecoveryMailer] recording every [sendVerification][RecoveryMailer.sendVerification] call it
 * receives, undecorated — no detachment, no coroutine launch — so a request that completes has
 * already recorded (or not recorded) its send by the time the response arrives, and [sent] is
 * decidable with no join, no channel and no timeout (`ADR-0077`).
 */
private class RecordingRecoveryMailer : RecoveryMailer {
    private val recorded = mutableListOf<EmailAddress>()

    /** Every address [sendVerification] was called with, in call order. */
    val sent: List<EmailAddress> get() = recorded

    override suspend fun sendVerification(address: EmailAddress, token: VerificationToken) {
        recorded.add(address)
    }

    override suspend fun sendPasswordReset(address: EmailAddress, token: ResetToken, handle: String) {
        throw UnsupportedOperationException("recovery-email never sends a password reset")
    }
}
