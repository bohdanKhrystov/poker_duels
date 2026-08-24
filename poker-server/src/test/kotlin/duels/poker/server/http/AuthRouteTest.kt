package duels.poker.server.http

import duels.poker.server.auth.CreateCredentialResult
import duels.poker.server.auth.CredentialKind
import duels.poker.server.module
import duels.poker.server.protocol.http.profileResponse
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
import org.junit.jupiter.api.Test

class AuthRouteTest {
    @Test
    fun anAbsentDeviceIdIsRefused() {
        testApplication {
            val reads = FixedProfileReads(emptyMap())
            val credentials = RecordingCredentials()
            application {
                module()
                authRoutes(reads, credentials, identitiesFor(reads.profiles))
            }
            // A well-formed, entirely valid body: a wrong implementation that reached the guard
            // or the write would answer 201/409, not 401 — this 401 can only have come from the
            // identity step.
            val response = client.post("/api/auth/sign-up") {
                header(HttpHeaders.ContentType, "application/json")
                setBody("""{"handle":"bob","password":"hunter2222"}""")
            }
            assertEquals(HttpStatusCode.Unauthorized, response.status)
            assertEquals("", response.bodyAsText())
            assertTrue(credentials.createCalls.isEmpty())
            assertTrue(credentials.holdsCalls.isEmpty())
        }
    }

    @Test
    fun aBlankDeviceIdIsRefused() {
        testApplication {
            val reads = FixedProfileReads(emptyMap())
            val credentials = RecordingCredentials()
            application {
                module()
                authRoutes(reads, credentials, identitiesFor(reads.profiles))
            }
            // A header of only whitespace: DeviceId's init rejects a blank value, so without the
            // isNotBlank guard this would throw and answer 500, not 401.
            val response = client.post("/api/auth/sign-up") {
                header(DEVICE_ID_HEADER, "   ")
            }
            assertEquals(HttpStatusCode.Unauthorized, response.status)
            assertEquals("", response.bodyAsText())
            assertTrue(credentials.createCalls.isEmpty())
            assertTrue(credentials.holdsCalls.isEmpty())
        }
    }

    @Test
    fun anUnknownDeviceIdIsRefused() {
        testApplication {
            val reads = FixedProfileReads(emptyMap())
            val credentials = RecordingCredentials()
            application {
                module()
                authRoutes(reads, credentials, identitiesFor(reads.profiles))
            }
            // A malformed body: a wrong implementation that only checked the header's presence
            // before decoding, deferring the profile lookup until after, would answer 400 here —
            // the same wrong answer a stranger's own malformed body would produce — instead of
            // 401.
            val response = client.post("/api/auth/sign-up") {
                header(DEVICE_ID_HEADER, "ghost")
                header(HttpHeaders.ContentType, "application/json")
                setBody("not json")
            }
            assertEquals(HttpStatusCode.Unauthorized, response.status)
            assertEquals("", response.bodyAsText())
            assertTrue(credentials.createCalls.isEmpty())
            assertTrue(credentials.holdsCalls.isEmpty())
        }
    }

    @Test
    fun anUndecodableBodyIsFourHundred() {
        testApplication {
            val reads = FixedProfileReads(mapOf("alice" to profileResponse("p-alice", 0)))
            val credentials = RecordingCredentials()
            application {
                module()
                authRoutes(reads, credentials, identitiesFor(reads.profiles))
            }
            val response = client.post("/api/auth/sign-up") {
                header(DEVICE_ID_HEADER, "alice")
                header(HttpHeaders.ContentType, "application/json")
                setBody("not json")
            }
            assertEquals(HttpStatusCode.BadRequest, response.status)
            assertEquals("", response.bodyAsText())
            assertTrue(credentials.createCalls.isEmpty())
            assertTrue(credentials.holdsCalls.isEmpty())
        }
    }

    @Test
    fun aBodyMissingThePasswordIsFourHundred() {
        testApplication {
            val reads = FixedProfileReads(mapOf("alice" to profileResponse("p-alice", 0)))
            val credentials = RecordingCredentials()
            application {
                module()
                authRoutes(reads, credentials, identitiesFor(reads.profiles))
            }
            val response = client.post("/api/auth/sign-up") {
                header(DEVICE_ID_HEADER, "alice")
                header(HttpHeaders.ContentType, "application/json")
                setBody("""{"handle":"bob"}""")
            }
            assertEquals(HttpStatusCode.BadRequest, response.status)
            assertEquals("", response.bodyAsText())
            assertTrue(credentials.createCalls.isEmpty())
            assertTrue(credentials.holdsCalls.isEmpty())
        }
    }

    @Test
    fun aBodyCarryingAPlayerIdIsFourHundred() {
        testApplication {
            val reads = FixedProfileReads(mapOf("alice" to profileResponse("p-alice", 0)))
            val credentials = RecordingCredentials()
            application {
                module()
                authRoutes(reads, credentials, identitiesFor(reads.profiles))
            }
            // An unrecognised field: a client cannot assert an identity even by trying, because
            // SignUpRequest has no playerId field to decode one into.
            val response = client.post("/api/auth/sign-up") {
                header(DEVICE_ID_HEADER, "alice")
                header(HttpHeaders.ContentType, "application/json")
                setBody("""{"handle":"bob","password":"hunter2222","playerId":"p-mallory"}""")
            }
            assertEquals(HttpStatusCode.BadRequest, response.status)
            assertEquals("", response.bodyAsText())
            assertTrue(credentials.createCalls.isEmpty())
            assertTrue(credentials.holdsCalls.isEmpty())
        }
    }

    @Test
    fun theBodyIsNeverDecodedBeforeIdentity() {
        testApplication {
            val reads = FixedProfileReads(emptyMap())
            val credentials = RecordingCredentials()
            application {
                module()
                authRoutes(reads, credentials, identitiesFor(reads.profiles))
            }
            // The single most important test in the ticket: no device id, and a body that cannot
            // even decode. The wrong implementation this must fail against is one that decodes
            // first — that implementation would answer 400 here, telling a stranger their body,
            // not their missing identity, was the problem.
            val response = client.post("/api/auth/sign-up") {
                header(HttpHeaders.ContentType, "application/json")
                setBody("not json")
            }
            assertEquals(HttpStatusCode.Unauthorized, response.status)
            assertEquals("", response.bodyAsText())
            assertTrue(credentials.createCalls.isEmpty())
            assertTrue(credentials.holdsCalls.isEmpty())
        }
    }

    @Test
    fun theFieldsAreNeverJudgedBeforeIdentity() {
        testApplication {
            val reads = FixedProfileReads(emptyMap())
            val credentials = RecordingCredentials()
            application {
                module()
                authRoutes(reads, credentials, identitiesFor(reads.profiles))
            }
            // The body decodes fine, but its handle fails signUpFieldsOf's own rule, which would
            // answer 400 once identity is known. A wrong implementation that judged fields before
            // confirming identity would answer 400 here instead of 401, telling a stranger their
            // handle, not their missing identity, was the problem.
            val response = client.post("/api/auth/sign-up") {
                header(HttpHeaders.ContentType, "application/json")
                setBody("""{"handle":"","password":"hunter2222"}""")
            }
            assertEquals(HttpStatusCode.Unauthorized, response.status)
            assertEquals("", response.bodyAsText())
            assertTrue(credentials.createCalls.isEmpty())
            assertTrue(credentials.holdsCalls.isEmpty())
        }
    }

    @Test
    fun aSignUpAnswersCreated() {
        testApplication {
            val reads = FixedProfileReads(mapOf("alice" to profileResponse("p-alice", 0)))
            val credentials = RecordingCredentials()
            application {
                module()
                authRoutes(reads, credentials, identitiesFor(reads.profiles))
            }
            val response = client.post("/api/auth/sign-up") {
                header(DEVICE_ID_HEADER, "alice")
                header(HttpHeaders.ContentType, "application/json")
                setBody("""{"handle":"bob","password":"hunter2222"}""")
            }
            assertEquals(HttpStatusCode.Created, response.status)
            assertEquals("", response.bodyAsText())
        }
    }

    @Test
    fun theCreateCallCarriesTheResolvedPlayerAndTheFoldedHandle() {
        testApplication {
            val reads = FixedProfileReads(mapOf("alice" to profileResponse("p-alice", 0)))
            val credentials = RecordingCredentials()
            application {
                module()
                authRoutes(reads, credentials, identitiesFor(reads.profiles))
            }
            // Bob_1 changes under the fold, so a handler that skipped folding would fail here even
            // though it would pass with an already-lowercase handle. The player id must be the
            // server's own p-alice — never the device id alice, and never a body field, because
            // SignUpRequest has none to carry one.
            val response = client.post("/api/auth/sign-up") {
                header(DEVICE_ID_HEADER, "alice")
                header(HttpHeaders.ContentType, "application/json")
                setBody("""{"handle":"Bob_1","password":"hunter2222"}""")
            }
            assertEquals(HttpStatusCode.Created, response.status)
            assertEquals(1, credentials.createCalls.size)
            val call = credentials.createCalls[0]
            assertEquals(PlayerId("p-alice"), call.playerId)
            assertEquals(CredentialKind.PASSWORD, call.kind)
            assertEquals("bob_1", call.identifier)
            // PresentedSecret redacts toString, so the secret is compared by its value directly.
            assertEquals("hunter2222", call.secret.value)
        }
    }

    @Test
    fun aPlayerWhoAlreadyHoldsAPasswordIsRefused() {
        testApplication {
            val reads = FixedProfileReads(mapOf("alice" to profileResponse("p-alice", 0)))
            val credentials = RecordingCredentials(holds = true)
            application {
                module()
                authRoutes(reads, credentials, identitiesFor(reads.profiles))
            }
            // holds = true: the guard alone must stop the write. createCalls staying empty proves
            // no Argon2 work was spent (ADR-0030 §1); holdsCalls being non-empty proves the guard
            // was actually the thing that answered, not skipped in favour of some other check.
            val response = client.post("/api/auth/sign-up") {
                header(DEVICE_ID_HEADER, "alice")
                header(HttpHeaders.ContentType, "application/json")
                setBody("""{"handle":"bob","password":"hunter2222"}""")
            }
            assertEquals(HttpStatusCode.Conflict, response.status)
            assertTrue(credentials.holdsCalls.isNotEmpty())
            assertTrue(credentials.createCalls.isEmpty())
        }
    }

    @Test
    fun aTakenHandleIsRefused() {
        testApplication {
            val reads = FixedProfileReads(mapOf("alice" to profileResponse("p-alice", 0)))
            val credentials =
                RecordingCredentials(createResult = CreateCredentialResult.IdentifierTaken)
            application {
                module()
                authRoutes(reads, credentials, identitiesFor(reads.profiles))
            }
            // The guard passes (holds = false, the default) but the write itself reports the
            // identifier taken — the same 409 as the case above, reached by a different branch.
            val response = client.post("/api/auth/sign-up") {
                header(DEVICE_ID_HEADER, "alice")
                header(HttpHeaders.ContentType, "application/json")
                setBody("""{"handle":"bob","password":"hunter2222"}""")
            }
            assertEquals(HttpStatusCode.Conflict, response.status)
        }
    }

    @Test
    fun aRefusedHandleReachesNeitherPortFunction() {
        testApplication {
            val reads = FixedProfileReads(mapOf("alice" to profileResponse("p-alice", 0)))
            val credentials = RecordingCredentials()
            application {
                module()
                authRoutes(reads, credentials, identitiesFor(reads.profiles))
            }
            // "ab" fails signUpFieldsOf's own rule, after identity and decoding both already
            // succeeded. A refusal that still costs a round trip to either port is a refusal that
            // leaks work.
            val response = client.post("/api/auth/sign-up") {
                header(DEVICE_ID_HEADER, "alice")
                header(HttpHeaders.ContentType, "application/json")
                setBody("""{"handle":"ab","password":"hunter2222"}""")
            }
            assertEquals(HttpStatusCode.BadRequest, response.status)
            assertTrue(credentials.createCalls.isEmpty())
            assertTrue(credentials.holdsCalls.isEmpty())
        }
    }

    @Test
    fun aPasswordOutsideTheBoundsReachesNeitherPortFunction() {
        // Two inputs, one per bound: a password one code point under the floor, and one code
        // point over the ceiling.
        val tooShort = "a".repeat(7)
        testApplication {
            val reads = FixedProfileReads(mapOf("alice" to profileResponse("p-alice", 0)))
            val credentials = RecordingCredentials()
            application {
                module()
                authRoutes(reads, credentials, identitiesFor(reads.profiles))
            }
            val response = client.post("/api/auth/sign-up") {
                header(DEVICE_ID_HEADER, "alice")
                header(HttpHeaders.ContentType, "application/json")
                setBody("""{"handle":"bob","password":"$tooShort"}""")
            }
            assertEquals(HttpStatusCode.UnprocessableEntity, response.status)
            assertTrue(credentials.createCalls.isEmpty())
            assertTrue(credentials.holdsCalls.isEmpty())
        }

        val tooLong = "a".repeat(129)
        testApplication {
            val reads = FixedProfileReads(mapOf("alice" to profileResponse("p-alice", 0)))
            val credentials = RecordingCredentials()
            application {
                module()
                authRoutes(reads, credentials, identitiesFor(reads.profiles))
            }
            val response = client.post("/api/auth/sign-up") {
                header(DEVICE_ID_HEADER, "alice")
                header(HttpHeaders.ContentType, "application/json")
                setBody("""{"handle":"bob","password":"$tooLong"}""")
            }
            assertEquals(HttpStatusCode.UnprocessableEntity, response.status)
            assertTrue(credentials.createCalls.isEmpty())
            assertTrue(credentials.holdsCalls.isEmpty())
        }
    }

    @Test
    fun theGuardIsAskedBeforeCreate() {
        testApplication {
            val reads = FixedProfileReads(mapOf("alice" to profileResponse("p-alice", 0)))
            val credentials = RecordingCredentials()
            application {
                module()
                authRoutes(reads, credentials, identitiesFor(reads.profiles))
            }
            val response = client.post("/api/auth/sign-up") {
                header(DEVICE_ID_HEADER, "alice")
                header(HttpHeaders.ContentType, "application/json")
                setBody("""{"handle":"bob","password":"hunter2222"}""")
            }
            assertEquals(HttpStatusCode.Created, response.status)
            assertEquals(1, credentials.holdsCalls.size)
            assertEquals(PlayerId("p-alice") to CredentialKind.PASSWORD, credentials.holdsCalls[0])
        }
    }
}
