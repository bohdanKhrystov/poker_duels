package duels.poker.server.http

import duels.poker.server.auth.CredentialKind
import duels.poker.server.auth.EmailAddress
import duels.poker.server.auth.PasswordResets
import duels.poker.server.auth.PresentedSecret
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
import io.ktor.client.request.delete
import io.ktor.client.request.header
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Clock
import java.util.UUID
import javax.sql.DataSource

/**
 * Tests for `DELETE /api/auth/recovery-email`, installed by [recoveryRoutes] and driven against a
 * real [PostgresRecoveryEmails], a real [PostgresCredentials] and a real [PostgresAuthSessions] —
 * this route has no double of its own to stand in for any of the three, exactly as
 * [ResetPasswordRouteTest] drives its sibling route in this same file with a real
 * [PostgresPasswordResets][duels.poker.server.db.PostgresPasswordResets] and a real
 * [PostgresCredentials].
 *
 * Every fixture attaches an address by calling [PostgresRecoveryEmails.claimPending] then
 * [PostgresRecoveryEmails.verifyPending] directly, since `POST /api/auth/recovery-email` — the
 * route that would normally cause one — does not exist yet (`TASK-041625`).
 *
 * [sessions] is a second, independent [PostgresAuthSessions] instance from the one
 * [identitiesFor]'s database-backed overload builds internally, issuing against the identical
 * `auth_session` table on [dataSource] — so a token it mints is one the route's own
 * `IdentityResolver` genuinely resolves, never a fixture that only looks like a session.
 */
class DetachRecoveryEmailRouteTest {
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
    fun theRightPasswordErasesTheAddress() {
        testApplication {
            application {
                module()
                recoveryRoutes(recoveryEmails, PasswordResetsNeverCalled, identitiesFor(dataSource), credentials)
            }
            val playerId = insertPlayer()
            val password = "the one true original password"
            credentials.create(playerId, CredentialKind.PASSWORD, handleFor(playerId), PresentedSecret(password))
            val token = sessions.issue(playerId)
            attachVerifiedAddress(playerId, "erased@detach-route-test.example")

            val response = client.delete("/api/auth/recovery-email") {
                header(HttpHeaders.Authorization, "Bearer ${token.value}")
                header(HttpHeaders.ContentType, "application/json")
                setBody("""{"currentPassword":"$password"}""")
            }

            assertEquals(HttpStatusCode.NoContent, response.status)
            assertEquals("", response.bodyAsText())
            assertFalse(
                recoveryEmails.hasRecoveryEmail(playerId),
                "expected hasRecoveryEmail false after a right-password erase",
            )
            assertFalse(
                dataSource.recoveryEmailRowExists(playerId),
                "expected recovery_email to hold no row for the player after the erase",
            )
        }
    }

    @Test
    fun theAddressReturnsToTheFreeNamespace() {
        testApplication {
            application {
                module()
                recoveryRoutes(recoveryEmails, PasswordResetsNeverCalled, identitiesFor(dataSource), credentials)
            }
            val firstPlayer = insertPlayer()
            val password = "the first owner's own password"
            credentials.create(firstPlayer, CredentialKind.PASSWORD, handleFor(firstPlayer), PresentedSecret(password))
            val token = sessions.issue(firstPlayer)
            val address = "freed-namespace@detach-route-test.example"
            attachVerifiedAddress(firstPlayer, address)

            val erase = client.delete("/api/auth/recovery-email") {
                header(HttpHeaders.Authorization, "Bearer ${token.value}")
                header(HttpHeaders.ContentType, "application/json")
                setBody("""{"currentPassword":"$password"}""")
            }
            assertEquals(HttpStatusCode.NoContent, erase.status, "setup: the first owner must actually erase")

            // The point of the erase, and unobservable from theRightPasswordErasesTheAddress
            // alone: a second player must now be free to claim and verify the identical address.
            val secondPlayer = insertPlayer()
            val secondToken = VerificationToken("second-claimant-detach-route-token")
            recoveryEmails.claimPending(secondPlayer, EmailAddress(address), secondToken)
            val result = recoveryEmails.verifyPending(secondToken)

            assertEquals(VerifyEmailResult.Verified, result)
            assertTrue(recoveryEmails.hasRecoveryEmail(secondPlayer))
        }
    }

    @Test
    fun aWrongPasswordAnswersFourHundredAndThreeAndErasesNothing() {
        testApplication {
            application {
                module()
                recoveryRoutes(recoveryEmails, PasswordResetsNeverCalled, identitiesFor(dataSource), credentials)
            }
            val playerId = insertPlayer()
            credentials.create(
                playerId,
                CredentialKind.PASSWORD,
                handleFor(playerId),
                PresentedSecret("the actual current password"),
            )
            val token = sessions.issue(playerId)
            attachVerifiedAddress(playerId, "survives-a-wrong-password@detach-route-test.example")

            val response = client.delete("/api/auth/recovery-email") {
                header(HttpHeaders.Authorization, "Bearer ${token.value}")
                header(HttpHeaders.ContentType, "application/json")
                setBody("""{"currentPassword":"not the actual password"}""")
            }

            assertEquals(HttpStatusCode.Forbidden, response.status)
            assertTrue(recoveryEmails.hasRecoveryEmail(playerId), "expected the address to survive a wrong password")
        }
    }

    @Test
    fun noSessionAnswersFourHundredAndOne() {
        testApplication {
            application {
                module()
                recoveryRoutes(recoveryEmails, PasswordResetsNeverCalled, identitiesFor(dataSource), credentials)
            }
            val playerId = insertPlayer()
            credentials.create(
                playerId,
                CredentialKind.PASSWORD,
                handleFor(playerId),
                PresentedSecret("a password nobody presents in this test"),
            )
            attachVerifiedAddress(playerId, "survives-no-session@detach-route-test.example")

            // No Authorization header anywhere in this request, but a well-formed body: a route
            // that decoded before resolving identity would still decode this one and proceed,
            // rather than accidentally answering 400 for an unrelated reason.
            val response = client.delete("/api/auth/recovery-email") {
                header(HttpHeaders.ContentType, "application/json")
                setBody("""{"currentPassword":"whatever a stranger might guess"}""")
            }

            assertEquals(HttpStatusCode.Unauthorized, response.status)
            assertEquals("", response.bodyAsText())
            assertTrue(
                recoveryEmails.hasRecoveryEmail(playerId),
                "expected the address to survive an unauthenticated request",
            )
        }
    }

    @Test
    fun aDeviceIdentityAloneAnswersFourHundredAndOne() {
        testApplication {
            application {
                module()
                recoveryRoutes(recoveryEmails, PasswordResetsNeverCalled, identitiesFor(dataSource), credentials)
            }
            // The device genuinely resolves to a real player, minted the same way production
            // mints one on first contact (PostgresPlayerDirectory.resolve): a 401 below must be
            // about the guard refusing a resolvable device, never about a fixture that failed to
            // resolve at all.
            val deviceId = DeviceId("detach-route-test-device-${UUID.randomUUID()}")
            val playerId = PostgresPlayerDirectory(dataSource).resolve(deviceId).id
            credentials.create(
                playerId,
                CredentialKind.PASSWORD,
                handleFor(playerId),
                PresentedSecret("a password nobody presents in this test"),
            )
            attachVerifiedAddress(playerId, "survives-a-device-identity@detach-route-test.example")

            // X-Device-Id only, no Authorization header: a route resolving identity through the
            // shared resolvedPlayerOrNull helper (Identity.Session or Identity.Device) would
            // resolve this caller and reach 403 or 204 instead of refusing it outright.
            val response = client.delete("/api/auth/recovery-email") {
                header(DEVICE_ID_HEADER, deviceId.value)
                header(HttpHeaders.ContentType, "application/json")
                setBody("""{"currentPassword":"whatever a device identity might present"}""")
            }

            assertEquals(HttpStatusCode.Unauthorized, response.status)
            assertEquals("", response.bodyAsText())
            assertTrue(
                recoveryEmails.hasRecoveryEmail(playerId),
                "expected the address to survive a device-identity-only request",
            )
        }
    }

    @Test
    fun detachingNothingStillAnswersTwoHundredAndFour() {
        testApplication {
            application {
                module()
                recoveryRoutes(recoveryEmails, PasswordResetsNeverCalled, identitiesFor(dataSource), credentials)
            }
            val attachedPlayer = insertPlayer()
            val attachedPassword = "the attached player's own password"
            credentials.create(
                attachedPlayer,
                CredentialKind.PASSWORD,
                handleFor(attachedPlayer),
                PresentedSecret(attachedPassword),
            )
            val attachedToken = sessions.issue(attachedPlayer)
            attachVerifiedAddress(attachedPlayer, "attached@detach-route-test.example")

            val unattachedPlayer = insertPlayer()
            val unattachedPassword = "the unattached player's own password"
            credentials.create(
                unattachedPlayer,
                CredentialKind.PASSWORD,
                handleFor(unattachedPlayer),
                PresentedSecret(unattachedPassword),
            )
            val unattachedToken = sessions.issue(unattachedPlayer)
            // unattachedPlayer never calls claimPending or verifyPending: no row for them, ever.

            val attached = client.detachOutcome(attachedToken, attachedPassword)
            val unattached = client.detachOutcome(unattachedToken, unattachedPassword)

            assertEquals(HttpStatusCode.NoContent, attached.status, "setup: the attached player must actually erase")
            assertEquals(
                attached,
                unattached,
                "expected byte-identical (status, body, header names) for an address that existed " +
                    "and one that never did",
            )
        }
    }

    @Test
    fun oneErasureIsNotAnothers() {
        testApplication {
            application {
                module()
                recoveryRoutes(recoveryEmails, PasswordResetsNeverCalled, identitiesFor(dataSource), credentials)
            }
            val firstPlayer = insertPlayer()
            val firstPassword = "the first player's own password"
            credentials.create(
                firstPlayer,
                CredentialKind.PASSWORD,
                handleFor(firstPlayer),
                PresentedSecret(firstPassword),
            )
            val firstToken = sessions.issue(firstPlayer)
            attachVerifiedAddress(firstPlayer, "first-player@detach-route-test.example")

            val secondPlayer = insertPlayer()
            val secondAddress = EmailAddress("second-player@detach-route-test.example")
            attachVerifiedAddress(secondPlayer, secondAddress.value)

            val response = client.delete("/api/auth/recovery-email") {
                header(HttpHeaders.Authorization, "Bearer ${firstToken.value}")
                header(HttpHeaders.ContentType, "application/json")
                setBody("""{"currentPassword":"$firstPassword"}""")
            }
            assertEquals(HttpStatusCode.NoContent, response.status, "setup: the first player must actually erase")

            assertTrue(
                recoveryEmails.hasRecoveryEmail(secondPlayer),
                "expected the second player's row to survive the first player's erase",
            )
            assertEquals(
                secondPlayer,
                recoveryEmails.verifiedOwnerOf(secondAddress),
                "expected the second player's own address to remain intact and owned by them",
            )
        }
    }

    /** Claims and verifies [address] for [playerId] directly, bypassing the HTTP layer entirely. */
    private suspend fun attachVerifiedAddress(playerId: PlayerId, address: String) {
        val token = VerificationToken("attach-token-${playerId.value}")
        recoveryEmails.claimPending(playerId, EmailAddress(address), token)
        val result = recoveryEmails.verifyPending(token)
        check(result is VerifyEmailResult.Verified) { "setup: expected the fixture claim to verify, got $result" }
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
private fun handleFor(playerId: PlayerId): String = "detach-route-test-handle-${playerId.value}"

/**
 * What a caller who can see only the HTTP response could use to tell two `DELETE
 * /api/auth/recovery-email` answers apart: the status, the body text, and the set of header
 * names — lowercased, and never `date`, which varies on every response and is not a channel that
 * carries anything a caller could read. Two instances that are `equal` are byte-identical on
 * every one of those three channels — the same idiom [VerifyEmailRefusalsTest]'s private
 * `ObservedResponse` uses for `POST /api/auth/verify-email`'s own refusals.
 *
 * Named apart from that `ObservedResponse`: a top-level `private` declaration is file-scoped in
 * Kotlin source but still compiles to a JVM class with the bare name, so two files declaring the
 * same one collide at the class file rather than the source level.
 */
private data class DetachOutcome(
    val status: HttpStatusCode,
    val body: String,
    val headerNames: Set<String>,
)

private suspend fun HttpClient.detachOutcome(token: SessionToken, password: String): DetachOutcome {
    val response = delete("/api/auth/recovery-email") {
        header(HttpHeaders.Authorization, "Bearer ${token.value}")
        header(HttpHeaders.ContentType, "application/json")
        setBody("""{"currentPassword":"$password"}""")
    }
    return DetachOutcome(
        status = response.status,
        body = response.bodyAsText(),
        headerNames = response.headers.names().map { it.lowercase() }.toSet() - "date",
    )
}

/** Whether `recovery_email` holds a row for [playerId], read straight off the table. */
private fun DataSource.recoveryEmailRowExists(playerId: PlayerId): Boolean {
    connection.use { connection ->
        connection.prepareStatement("SELECT 1 FROM recovery_email WHERE player_id = ?").use { statement ->
            statement.setObject(1, UUID.fromString(playerId.value))
            statement.executeQuery().use { rows -> return rows.next() }
        }
    }
}

/**
 * `recoveryRoutes`' `recovery-email` handler never touches [PasswordResets]; a call reaching
 * either method here would mean the handler read the wrong parameter. Named apart from
 * [VerifyEmailRefusalsTest]'s private `StubPasswordResets` and [VerifyEmailRouteTest]'s private
 * `NoPasswordResets` for the same class-file-collision reason [DetachOutcome] is named apart from
 * `ObservedResponse`.
 */
private object PasswordResetsNeverCalled : PasswordResets {
    override suspend fun issue(playerId: PlayerId, token: ResetToken): Boolean {
        throw UnsupportedOperationException("recovery-email never issues a reset token")
    }

    override suspend fun consume(token: ResetToken, secret: PresentedSecret): Boolean {
        throw UnsupportedOperationException("recovery-email never consumes a reset token")
    }
}
