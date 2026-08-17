package duels.poker.server.http

import duels.poker.server.module
import duels.poker.server.protocol.http.profileResponse
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
                authRoutes(reads, credentials)
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
                authRoutes(reads, credentials)
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
                authRoutes(reads, credentials)
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
                authRoutes(reads, credentials)
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
                authRoutes(reads, credentials)
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
                authRoutes(reads, credentials)
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
                authRoutes(reads, credentials)
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
                authRoutes(reads, credentials)
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
}
