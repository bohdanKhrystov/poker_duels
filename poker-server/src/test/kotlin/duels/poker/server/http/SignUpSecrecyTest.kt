package duels.poker.server.http

import duels.poker.server.auth.AttemptBudget
import duels.poker.server.auth.AttemptLimits
import duels.poker.server.auth.CreateCredentialResult
import duels.poker.server.module
import duels.poker.server.protocol.http.profileResponse
import duels.poker.server.time.MutableClock
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

// The same two secrets every scenario below tries to carry, so a leak of either is caught no
// matter which sign-up outcome produced it — including the two scenarios that cannot carry both,
// because the whole point of that request is that one of the two is invalid.
private const val SIGN_UP_HANDLE = "Bob_1"
private const val SIGN_UP_PASSWORD = "hunter2222"

private data class SignUpOutcome(val name: String, val status: HttpStatusCode, val body: String)

/**
 * Sweeps sign-up's outcomes and the files on its path, proving two properties by driving the real
 * route rather than by inspection: every outcome answers an empty body, and no file that handles
 * the password on its way in can print it.
 */
class SignUpSecrecyTest {
    @Test
    fun everyOutcomeAnswersAnEmptyBody() {
        val outcomes = driveEverySignUpOutcome()

        // A wrong device id and a sweep that silently narrowed to three cases look identical once
        // only bodies are checked: there is nothing left to fail. The count is asserted first,
        // then the exact sequence, so neither failure can hide behind the other.
        assertEquals(7, outcomes.size, "expected 7 outcomes, got ${outcomes.map { it.name }}")
        assertEquals(
            listOf(401, 400, 400, 422, 409, 409, 201),
            outcomes.map { it.status.value },
            "outcome statuses drifted: ${outcomes.map { "${it.name}=${it.status.value}" }}",
        )
        for (outcome in outcomes) {
            assertEquals("", outcome.body, "${outcome.name} answered a non-empty body: '${outcome.body}'")
        }
    }

    @Test
    fun noOutcomeEchoesTheSecretsItWasSent() {
        val outcomes = driveEverySignUpOutcome()

        assertTrue(outcomes.isNotEmpty(), "expected at least one outcome to check")
        for (outcome in outcomes) {
            assertTrue(
                !outcome.body.contains(SIGN_UP_PASSWORD),
                "${outcome.name} echoed the password in its body: '${outcome.body}'",
            )
            assertTrue(
                !outcome.body.contains("bob_1"),
                "${outcome.name} echoed the folded handle in its body: '${outcome.body}'",
            )
            assertTrue(
                !outcome.body.contains(SIGN_UP_HANDLE),
                "${outcome.name} echoed the handle in its body: '${outcome.body}'",
            )
        }
    }

    @Test
    fun noFileOnTheSignUpPathNamesALogger() {
        // Same idiom as HttpEndpointDocumentationTest: Gradle's working directory is per module,
        // not per repository, so this walks up from it until it finds the marker that exists only
        // at the repository root.
        val repositoryRoot = generateSequence(File("").absoluteFile) { it.parentFile }
            .firstOrNull { File(it, "docs/protocol.md").isFile }
            ?: error("repository root not found above ${File("").absolutePath}")

        val mainKotlin = "poker-server/src/main/kotlin/duels/poker/server"
        val signUpFiles = listOf(
            "AuthRoutes.kt" to File(repositoryRoot, "$mainKotlin/http/AuthRoutes.kt"),
            "SignUpFields.kt" to File(repositoryRoot, "$mainKotlin/http/SignUpFields.kt"),
            "AuthDtos.kt" to File(repositoryRoot, "$mainKotlin/protocol/http/AuthDtos.kt"),
            "PasswordPolicy.kt" to File(repositoryRoot, "$mainKotlin/auth/PasswordPolicy.kt"),
        )

        // The list above is itself the sweep's coverage: a name quietly dropped from it would
        // otherwise still pass, having checked one file fewer and told no one. Naming all four
        // here first, before any of them is opened, is what turns that into a failure.
        assertEquals(
            listOf("AuthRoutes.kt", "SignUpFields.kt", "AuthDtos.kt", "PasswordPolicy.kt"),
            signUpFiles.map { it.first },
            "expected to sweep exactly 4 files, got ${signUpFiles.map { it.first }}",
        )

        // A wrong repository root resolves all four of these to files that simply are not there,
        // and an empty set of forbidden tokens would then pass vacuously. Naming each file here,
        // before a single byte of its content is judged, is what makes that failure loud instead
        // of quiet.
        for ((name, file) in signUpFiles) {
            assertTrue(file.isFile, "$name not found at ${file.absolutePath}")
        }
        val contents = signUpFiles.map { (name, file) -> name to file.readText() }
        for ((name, text) in contents) {
            assertTrue(text.isNotEmpty(), "$name was found but its contents are empty")
        }

        val loggingTokens = listOf(
            "println",
            "LoggerFactory",
            "Logger",
            "log.info",
            "log.debug",
            "log.warn",
            "log.error",
        )
        for ((name, text) in contents) {
            for (token in loggingTokens) {
                assertTrue(!text.contains(token), "$name names a logger via '$token'")
            }
        }
    }

    // Drives sign-up's real route once per outcome, through the exact wiring AuthRouteTest uses,
    // and returns what came back rather than what was expected — both tests above assert against
    // this result instead of each hand-rolling their own seven requests.
    private fun driveEverySignUpOutcome(): List<SignUpOutcome> {
        val outcomes = mutableListOf<SignUpOutcome>()

        testApplication {
            val reads = FixedProfileReads(emptyMap())
            val credentials = RecordingCredentials()
            application {
                module()
                authRoutes(
                    reads,
                    credentials,
                    identitiesFor(reads.profiles),
                    NoAuthSessions,
                    AttemptBudget(AttemptLimits(5, 900_000L), MutableClock()),
                )
            }
            val response = client.post("/api/auth/sign-up") {
                header(HttpHeaders.ContentType, "application/json")
                setBody("""{"handle":"$SIGN_UP_HANDLE","password":"$SIGN_UP_PASSWORD"}""")
            }
            outcomes += SignUpOutcome("no device id", response.status, response.bodyAsText())
        }

        testApplication {
            val reads = FixedProfileReads(mapOf("alice" to profileResponse("p-alice", 0)))
            val credentials = RecordingCredentials()
            application {
                module()
                authRoutes(
                    reads,
                    credentials,
                    identitiesFor(reads.profiles),
                    NoAuthSessions,
                    AttemptBudget(AttemptLimits(5, 900_000L), MutableClock()),
                )
            }
            // SignUpRequest has no playerId field, so this fails to decode — the same mechanism
            // AuthRouteTest.aBodyCarryingAPlayerIdIsFourHundred proves — while still carrying both
            // secrets literally, so a leak here would still be caught.
            val response = client.post("/api/auth/sign-up") {
                header(DEVICE_ID_HEADER, "alice")
                header(HttpHeaders.ContentType, "application/json")
                setBody(
                    """{"handle":"$SIGN_UP_HANDLE","password":"$SIGN_UP_PASSWORD","playerId":"p-mallory"}""",
                )
            }
            outcomes += SignUpOutcome("undecodable body", response.status, response.bodyAsText())
        }

        testApplication {
            val reads = FixedProfileReads(mapOf("alice" to profileResponse("p-alice", 0)))
            val credentials = RecordingCredentials()
            application {
                module()
                authRoutes(
                    reads,
                    credentials,
                    identitiesFor(reads.profiles),
                    NoAuthSessions,
                    AttemptBudget(AttemptLimits(5, 900_000L), MutableClock()),
                )
            }
            // "ab" fails signUpFieldsOf's own rule (proven by
            // AuthRouteTest.aRefusedHandleReachesNeitherPortFunction). Bob_1 is proven valid
            // elsewhere in this suite, so it cannot also be what trips this branch — the password
            // stays the shared secret, so a leak of it would still be caught here.
            val response = client.post("/api/auth/sign-up") {
                header(DEVICE_ID_HEADER, "alice")
                header(HttpHeaders.ContentType, "application/json")
                setBody("""{"handle":"ab","password":"$SIGN_UP_PASSWORD"}""")
            }
            outcomes += SignUpOutcome("refused handle", response.status, response.bodyAsText())
        }

        testApplication {
            val reads = FixedProfileReads(mapOf("alice" to profileResponse("p-alice", 0)))
            val credentials = RecordingCredentials()
            application {
                module()
                authRoutes(
                    reads,
                    credentials,
                    identitiesFor(reads.profiles),
                    NoAuthSessions,
                    AttemptBudget(AttemptLimits(5, 900_000L), MutableClock()),
                )
            }
            // One code point under the floor (proven by
            // AuthRouteTest.aPasswordOutsideTheBoundsReachesNeitherPortFunction). hunter2222 is
            // proven in-bounds elsewhere in this suite, so it cannot also be what trips this
            // branch — the handle stays the shared secret, so a leak of it would still be caught
            // here.
            val response = client.post("/api/auth/sign-up") {
                header(DEVICE_ID_HEADER, "alice")
                header(HttpHeaders.ContentType, "application/json")
                setBody("""{"handle":"$SIGN_UP_HANDLE","password":"${"a".repeat(7)}"}""")
            }
            outcomes += SignUpOutcome("short password", response.status, response.bodyAsText())
        }

        testApplication {
            val reads = FixedProfileReads(mapOf("alice" to profileResponse("p-alice", 0)))
            val credentials = RecordingCredentials(holds = true)
            application {
                module()
                authRoutes(
                    reads,
                    credentials,
                    identitiesFor(reads.profiles),
                    NoAuthSessions,
                    AttemptBudget(AttemptLimits(5, 900_000L), MutableClock()),
                )
            }
            val response = client.post("/api/auth/sign-up") {
                header(DEVICE_ID_HEADER, "alice")
                header(HttpHeaders.ContentType, "application/json")
                setBody("""{"handle":"$SIGN_UP_HANDLE","password":"$SIGN_UP_PASSWORD"}""")
            }
            outcomes += SignUpOutcome("already holds", response.status, response.bodyAsText())
        }

        testApplication {
            val reads = FixedProfileReads(mapOf("alice" to profileResponse("p-alice", 0)))
            val credentials = RecordingCredentials(createResult = CreateCredentialResult.IdentifierTaken)
            application {
                module()
                authRoutes(
                    reads,
                    credentials,
                    identitiesFor(reads.profiles),
                    NoAuthSessions,
                    AttemptBudget(AttemptLimits(5, 900_000L), MutableClock()),
                )
            }
            val response = client.post("/api/auth/sign-up") {
                header(DEVICE_ID_HEADER, "alice")
                header(HttpHeaders.ContentType, "application/json")
                setBody("""{"handle":"$SIGN_UP_HANDLE","password":"$SIGN_UP_PASSWORD"}""")
            }
            outcomes += SignUpOutcome("handle taken", response.status, response.bodyAsText())
        }

        testApplication {
            val reads = FixedProfileReads(mapOf("alice" to profileResponse("p-alice", 0)))
            val credentials = RecordingCredentials()
            application {
                module()
                authRoutes(
                    reads,
                    credentials,
                    identitiesFor(reads.profiles),
                    NoAuthSessions,
                    AttemptBudget(AttemptLimits(5, 900_000L), MutableClock()),
                )
            }
            val response = client.post("/api/auth/sign-up") {
                header(DEVICE_ID_HEADER, "alice")
                header(HttpHeaders.ContentType, "application/json")
                setBody("""{"handle":"$SIGN_UP_HANDLE","password":"$SIGN_UP_PASSWORD"}""")
            }
            outcomes += SignUpOutcome("success", response.status, response.bodyAsText())
        }

        return outcomes
    }
}
