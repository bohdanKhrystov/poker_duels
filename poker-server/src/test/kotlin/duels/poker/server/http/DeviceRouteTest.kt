package duels.poker.server.http

import duels.poker.server.auth.CredentialKind
import duels.poker.server.auth.RecordingDeviceBindings
import duels.poker.server.auth.SessionToken
import duels.poker.server.protocol.http.profileResponse
import duels.poker.server.session.PlayerId
import io.ktor.client.request.delete
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DeviceRouteTest {
    /**
     * `"device-1"` resolves to `"player-1"` — the same player session `"t-1"` names. Shared by
     * every test below so that [aDeviceIdAloneIsRefused]'s refusal is tested against a device
     * that genuinely resolves, not a fixture that would answer `401` for the wrong reason.
     */
    private val profiles = mapOf("device-1" to profileResponse("player-1", 0))
    private val identities = identitiesFor(profiles, FixedAuthSessions(mapOf("t-1" to "player-1")))

    @Test
    fun aSessionRevokesAndGetsTwoHundredAndFour() = testApplication {
        val bindings = RecordingDeviceBindings()
        application {
            deviceRoutes(identities, RecordingCredentials(holds = true), bindings)
        }
        val response = client.delete("/api/me/device") {
            header(HttpHeaders.Authorization, "Bearer t-1")
        }
        assertEquals(HttpStatusCode.NoContent, response.status)
        assertEquals("", response.bodyAsText())
        assertEquals(1, bindings.revokeCalls.size)
        assertEquals(PlayerId("player-1"), bindings.revokeCalls[0].playerId)
        assertEquals(SessionToken("t-1"), bindings.revokeCalls[0].keeping)
    }

    @Test
    fun aDeviceIdAloneIsRefused() = testApplication {
        // The positive control is the test above, against the same fixture: device-1 resolves to
        // player-1, so a route written with resolvedPlayerOrNull would answer 204 here. A
        // fixture whose device did not resolve would make this test pass for the wrong reason.
        val bindings = RecordingDeviceBindings()
        application {
            deviceRoutes(identities, RecordingCredentials(holds = true), bindings)
        }
        val response = client.delete("/api/me/device") {
            header(DEVICE_ID_HEADER, "device-1")
        }
        assertEquals(HttpStatusCode.Unauthorized, response.status)
        assertTrue(bindings.revokeCalls.isEmpty())
    }

    @Test
    fun anUnknownTokenIsRefusedEvenBesideAResolvableDevice() = testApplication {
        // ADR-0027 §4's no-fall-back rule, at this route: a session presented and unknown must
        // not let the resolvable device beside it stand in for it.
        val bindings = RecordingDeviceBindings()
        application {
            deviceRoutes(identities, RecordingCredentials(holds = true), bindings)
        }
        val response = client.delete("/api/me/device") {
            header(HttpHeaders.Authorization, "Bearer nope")
            header(DEVICE_ID_HEADER, "device-1")
        }
        assertEquals(HttpStatusCode.Unauthorized, response.status)
        assertTrue(bindings.revokeCalls.isEmpty())
    }

    @Test
    fun noCredentialAtAllIsRefused() = testApplication {
        val bindings = RecordingDeviceBindings()
        application {
            deviceRoutes(identities, RecordingCredentials(holds = true), bindings)
        }
        val response = client.delete("/api/me/device")
        assertEquals(HttpStatusCode.Unauthorized, response.status)
        assertTrue(bindings.revokeCalls.isEmpty())
    }

    @Test
    fun aValidTokenBesideAnUnknownDeviceStillRevokes() = testApplication {
        // The session decides and the device beside it is never consulted: a route that required
        // the device to resolve too would fail here, and no other test in this class would notice.
        val bindings = RecordingDeviceBindings()
        application {
            deviceRoutes(identities, RecordingCredentials(holds = true), bindings)
        }
        val response = client.delete("/api/me/device") {
            header(HttpHeaders.Authorization, "Bearer t-1")
            header(DEVICE_ID_HEADER, "no-such-device")
        }
        assertEquals(HttpStatusCode.NoContent, response.status)
        assertEquals(1, bindings.revokeCalls.size)
        assertEquals(PlayerId("player-1"), bindings.revokeCalls[0].playerId)
    }

    @Test
    fun everyRefusalHasAnEmptyBody() = testApplication {
        // One test, three requests: a 401 carrying a reason would say which of the three it was.
        // This is not a substitute for the status assertions above — a 204 also has an empty
        // body, so this test alone cannot tell a refusal from a success.
        val bindings = RecordingDeviceBindings()
        application {
            deviceRoutes(identities, RecordingCredentials(holds = true), bindings)
        }
        val deviceAlone = client.delete("/api/me/device") {
            header(DEVICE_ID_HEADER, "device-1")
        }
        val unknownToken = client.delete("/api/me/device") {
            header(HttpHeaders.Authorization, "Bearer nope")
            header(DEVICE_ID_HEADER, "device-1")
        }
        val nothing = client.delete("/api/me/device")
        assertEquals("", deviceAlone.bodyAsText())
        assertEquals("", unknownToken.bodyAsText())
        assertEquals("", nothing.bodyAsText())
    }

    @Test
    fun aPlayerWithNoCredentialGetsFourHundredAndNine() = testApplication {
        // The session is valid and device-1 resolves, exactly as the positive control above, so
        // the refusal is about the missing credential alone — never about identity. A count, not
        // a status: 409 alone cannot say whether the write happened first.
        val bindings = RecordingDeviceBindings()
        application {
            deviceRoutes(identities, RecordingCredentials(holds = false), bindings)
        }
        val response = client.delete("/api/me/device") {
            header(HttpHeaders.Authorization, "Bearer t-1")
        }
        assertEquals(HttpStatusCode.Conflict, response.status)
        assertEquals("", response.bodyAsText())
        assertTrue(bindings.revokeCalls.isEmpty())
    }

    @Test
    fun anUnknownDeviceWithNoSessionIsRefused() = testApplication {
        // The session is absent and the device does not resolve: both halves of the condition are
        // needed to reach the UnknownDevice branch. A valid token beside an unknown device
        // (aValidTokenBesideAnUnknownDeviceStillRevokes above) is not enough; the session answers
        // first, so the device is never looked up. This test must carry neither.
        val bindings = RecordingDeviceBindings()
        application {
            deviceRoutes(identities, RecordingCredentials(holds = true), bindings)
        }
        val response = client.delete("/api/me/device") {
            header(DEVICE_ID_HEADER, "no-such-device")
        }
        assertEquals(HttpStatusCode.Unauthorized, response.status)
        assertEquals("", response.bodyAsText())
        assertTrue(bindings.revokeCalls.isEmpty())
    }

    @Test
    fun theCredentialIsCheckedWithThisPlayerAndThePasswordKind() = testApplication {
        // Same request as above: the player the guard checks is the one the session named, never
        // one a header or a body could supply.
        val credentials = RecordingCredentials(holds = false)
        val bindings = RecordingDeviceBindings()
        application {
            deviceRoutes(identities, credentials, bindings)
        }
        client.delete("/api/me/device") {
            header(HttpHeaders.Authorization, "Bearer t-1")
        }
        assertEquals(1, credentials.holdsCalls.size)
        assertEquals(PlayerId("player-1") to CredentialKind.PASSWORD, credentials.holdsCalls[0])
    }

    @Test
    fun anUnauthenticatedCallerNeverReachesTheCredentialCheck() = testApplication {
        // The ordering assertion: a guard placed before the identity check would answer 409 here
        // — holds is false on this double — and holdsCalls would have size 1.
        val credentials = RecordingCredentials(holds = false)
        val bindings = RecordingDeviceBindings()
        application {
            deviceRoutes(identities, credentials, bindings)
        }
        val response = client.delete("/api/me/device")
        assertEquals(HttpStatusCode.Unauthorized, response.status)
        assertTrue(credentials.holdsCalls.isEmpty())
    }
}
