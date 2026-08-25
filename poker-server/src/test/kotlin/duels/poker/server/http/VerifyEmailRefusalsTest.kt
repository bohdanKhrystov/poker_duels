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
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID
import javax.sql.DataSource

/**
 * Proves, at the wire, the two claims `ADR-0031` §5 makes about `POST /api/auth/verify-email`
 * that `TASK-041609`'s port-level tests cannot: that a caller who can see only the HTTP response
 * cannot tell an unknown, an expired and an already-consumed token apart, and that the `409` is
 * reachable only by someone who already holds a token minted for the contested address.
 *
 * The three refusals are compared as a triple of `(status, bodyText, headerNames)` — see
 * [ObservedResponse] — never as three separate "is this 400" assertions, which would pass even if
 * a `Content-Length` or some other header still distinguished them.
 *
 * Every fixture reuses [VerifyEmailRouteTest]'s pattern: a real [PostgresRecoveryEmails] against a
 * fresh Postgres container, [recoveryRoutes] installed directly since `Application.kt` does not
 * wire it in yet (`TASK-041622`), and a pending row created by calling
 * [PostgresRecoveryEmails.claimPending] directly, since `POST /api/auth/recovery-email` — the
 * route that would normally cause one — does not exist yet either (`TASK-041625`).
 */
class VerifyEmailRefusalsTest {
    private lateinit var dataSource: DataSource
    private lateinit var recoveryEmails: PostgresRecoveryEmails

    @BeforeEach
    fun setupDatabase() {
        dataSource = PostgresTestSupport.freshDatabase()
        Migrations.migrate(dataSource)
        recoveryEmails = PostgresRecoveryEmails(dataSource, Clock.systemUTC())
    }

    @Test
    fun anUnknownAnExpiredAndASpentTokenAreOneAnswer() {
        testApplication {
            application {
                module()
                recoveryRoutes(recoveryEmails, StubPasswordResets, identitiesFor(emptyMap()), RecordingCredentials())
            }

            // Unknown: no claim was ever made for this token, so no row anywhere names it.
            val unknown = client.attemptVerification("never-claimed-refusal-token")

            // Expired: the DELETE that consumes a token compares expires_at to SQL now() at read
            // time, never to the injected Clock a second time -- advancing this Clock after a
            // claim cannot retroactively expire the row it already wrote. So the fixture backdates
            // the Clock *before* the claim: expires_at is computed as 24 hours past an instant that
            // is itself already an hour further back than real now(), landing in the past the
            // moment the row is written.
            val expiredEmails = PostgresRecoveryEmails(
                dataSource,
                Clock.fixed(Instant.now().minus(Duration.ofHours(25)), ZoneOffset.UTC),
            )
            expiredEmails.claimPending(
                insertPlayer(),
                EmailAddress("expired@refusals.test"),
                VerificationToken("expired-refusal-token"),
            )
            val expired = client.attemptVerification("expired-refusal-token")

            // Already consumed: the first use frees the row entirely (TASK-041609's port deletes
            // rather than flags), so a second presentation of the same token finds no row at all --
            // indistinguishable, by construction, from a token nobody ever claimed.
            val spentPlayer = insertPlayer()
            recoveryEmails.claimPending(
                spentPlayer,
                EmailAddress("spent@refusals.test"),
                VerificationToken("spent-refusal-token"),
            )
            // The first use itself is not asserted here on purpose: theSpentCaseIsReallySpent is
            // the independent control for that. Guarding it here too would let this test catch a
            // construction bug that collapses every case into "unknown" all by itself, which is
            // exactly the vacuity the separate controls exist to expose instead.
            client.attemptVerification("spent-refusal-token")
            // That successful first use planted a recovery_email row -- an incidental side effect
            // of consuming the token, not what this leg is about. Detach it so what remains is
            // exactly "spent": a token whose email_verification row is gone, with no recovery_email
            // row alongside it that could make the second call answer AddressTaken instead of
            // Refused.
            recoveryEmails.detach(spentPlayer)
            val spent = client.attemptVerification("spent-refusal-token")

            assertEquals(HttpStatusCode.BadRequest, unknown.status)
            assertEquals(unknown, expired, "an unknown token and an expired one must be byte-identical at the wire")
            assertEquals(unknown, spent, "an unknown token and a spent one must be byte-identical at the wire")
        }
    }

    @Test
    fun theExpiredCaseIsReallyExpired() {
        testApplication {
            application {
                module()
                recoveryRoutes(recoveryEmails, StubPasswordResets, identitiesFor(emptyMap()), RecordingCredentials())
            }

            // The same construction the case above uses for "expired" -- a PostgresRecoveryEmails
            // backed by a fixed Clock -- except this Clock is never backdated. Without this check,
            // an expiry fixture that instead mints a token whose hash never matches any row would
            // pass the test above by being *unknown* rather than *expired*, and the expiry branch
            // would never be exercised at all. Proving this one still answers 204 is what rules
            // that out: the only variable that differs from the expired fixture is the Clock.
            val freshEmails = PostgresRecoveryEmails(dataSource, Clock.fixed(Instant.now(), ZoneOffset.UTC))
            val player = insertPlayer()
            freshEmails.claimPending(
                player,
                EmailAddress("not-yet-expired@refusals.test"),
                VerificationToken("not-yet-expired-token"),
            )

            val response = client.attemptVerification("not-yet-expired-token")

            assertEquals(HttpStatusCode.NoContent, response.status)
            assertTrue(recoveryEmails.hasRecoveryEmail(player), "a token claimed under a fresh clock must still verify")
        }
    }

    @Test
    fun theSpentCaseIsReallySpent() {
        testApplication {
            application {
                module()
                recoveryRoutes(recoveryEmails, StubPasswordResets, identitiesFor(emptyMap()), RecordingCredentials())
            }

            // The construction the "spent" case above reuses for its first use. Proving that use
            // alone succeeds rules out the same vacuity as above: a token that could never verify
            // at all would make the second use in the fixture above answer 400 for being unknown,
            // not for being spent.
            val player = insertPlayer()
            recoveryEmails.claimPending(player, EmailAddress("first-use@refusals.test"), VerificationToken("first-use-token"))

            val response = client.attemptVerification("first-use-token")

            assertEquals(HttpStatusCode.NoContent, response.status)
            assertTrue(recoveryEmails.hasRecoveryEmail(player), "the first use of the token must actually prove the address")
        }
    }

    @Test
    fun theSecondPlayerToProveOneAddressIsToldItIsTaken() {
        testApplication {
            application {
                module()
                recoveryRoutes(recoveryEmails, StubPasswordResets, identitiesFor(emptyMap()), RecordingCredentials())
            }

            // Two players race one address: email_verification carries no unique constraint on
            // address (ADR-0031 §3), so both pending claims coexist until one of them verifies.
            val address = EmailAddress("contested@refusals.test")
            recoveryEmails.claimPending(insertPlayer(), address, VerificationToken("first-claimant-token"))
            recoveryEmails.claimPending(insertPlayer(), address, VerificationToken("second-claimant-token"))

            val first = client.attemptVerification("first-claimant-token")
            assertEquals(HttpStatusCode.NoContent, first.status, "setup: the first claimant must win the address")

            val second = client.attemptVerification("second-claimant-token")

            assertEquals(HttpStatusCode.Conflict, second.status)
        }
    }

    @Test
    fun aStrangerWithNoTokenCannotReachTheNineOhFour() {
        testApplication {
            application {
                module()
                recoveryRoutes(recoveryEmails, StubPasswordResets, identitiesFor(emptyMap()), RecordingCredentials())
            }

            val address = EmailAddress("already-owned@refusals.test")
            recoveryEmails.claimPending(insertPlayer(), address, VerificationToken("owner-token"))
            val ownerVerify = client.attemptVerification("owner-token")
            assertEquals(HttpStatusCode.NoContent, ownerVerify.status, "setup: the address must already be verified to somebody")

            // A token nobody ever minted for this address: the caller has proven nothing, so §5's
            // not-an-oracle argument says this must land on the same refusal a stranger to any
            // address gets, never on the 409 that discloses the address is spoken for.
            val stranger = client.attemptVerification("a-token-nobody-ever-minted")

            assertEquals(HttpStatusCode.BadRequest, stranger.status)
            assertNotEquals(HttpStatusCode.Conflict, stranger.status)
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
 * What a caller who can see only the HTTP response could use to tell two refusals apart: the
 * status, the body text, and the set of header names — lowercased, and never `date`, which varies
 * on every response and is not a channel that carries anything a caller could read. Two instances
 * that are `equal` are byte-identical on every one of those three channels.
 */
private data class ObservedResponse(
    val status: HttpStatusCode,
    val body: String,
    val headerNames: Set<String>,
)

private suspend fun HttpClient.attemptVerification(token: String): ObservedResponse {
    val response = post("/api/auth/verify-email") {
        header(HttpHeaders.ContentType, "application/json")
        setBody("""{"token":"$token"}""")
    }
    return ObservedResponse(
        status = response.status,
        body = response.bodyAsText(),
        headerNames = response.headers.names().map { it.lowercase() }.toSet() - "date",
    )
}

/**
 * `recoveryRoutes`' `verify-email` handler never touches [PasswordResets]; a call reaching either
 * method here would mean the handler read the wrong parameter. Named apart from
 * [VerifyEmailRouteTest]'s private `NoPasswordResets`: a top-level `private` declaration is
 * file-scoped in Kotlin source but still compiles to a JVM class with the bare name, so two files
 * declaring the same one collide at the class file rather than the source level.
 */
private object StubPasswordResets : PasswordResets {
    override suspend fun issue(playerId: PlayerId, token: ResetToken): Boolean {
        throw UnsupportedOperationException("verify-email never issues a reset token")
    }

    override suspend fun consume(token: ResetToken, secret: PresentedSecret): Boolean {
        throw UnsupportedOperationException("verify-email never consumes a reset token")
    }
}
