package duels.poker.server.http

import duels.poker.server.module
import duels.poker.server.protocol.http.DuelSummaryResponse
import duels.poker.server.protocol.http.ProfileResponse
import duels.poker.server.session.DeviceId
import duels.poker.server.session.PlayerId
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ProfileRouteTest {
    @Test
    fun aKnownDeviceGetsItsProfile() = testApplication {
        val reads = FakeProfileReads(
            mapOf(
                "alice" to ProfileResponse("p-alice", 4),
            ),
        )
        application {
            module()
            profileRoutes(reads)
        }
        val response = client.get("/api/me") {
            header(DEVICE_ID_HEADER, "alice")
        }
        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        assertTrue(body.contains("\"playerId\":\"p-alice\""))
        assertTrue(body.contains("\"coinBalance\":4"))
        assertTrue(reads.queried.contains("alice"))
    }

    @Test
    fun aNegativeBalanceIsReturnedUnclamped() = testApplication {
        val reads = FakeProfileReads(
            mapOf(
                "bob" to ProfileResponse("p-bob", -3),
            ),
        )
        application {
            module()
            profileRoutes(reads)
        }
        val response = client.get("/api/me") {
            header(DEVICE_ID_HEADER, "bob")
        }
        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        assertTrue(body.contains("\"coinBalance\":-3"))
    }

    @Test
    fun anAbsentDeviceIdHeaderIsRefused() = testApplication {
        val reads = FakeProfileReads(emptyMap())
        application {
            module()
            profileRoutes(reads)
        }
        val response = client.get("/api/me")
        assertEquals(HttpStatusCode.Unauthorized, response.status)
        assertTrue(reads.queried.isEmpty())
    }

    @Test
    fun aBlankDeviceIdHeaderIsRefused() = testApplication {
        val reads = FakeProfileReads(emptyMap())
        application {
            module()
            profileRoutes(reads)
        }
        val response = client.get("/api/me") {
            header(DEVICE_ID_HEADER, "  ")
        }
        assertEquals(HttpStatusCode.Unauthorized, response.status)
        assertTrue(reads.queried.isEmpty())
    }

    @Test
    fun anUnknownDeviceIdIsRefused() = testApplication {
        val reads = FakeProfileReads(emptyMap())
        application {
            module()
            profileRoutes(reads)
        }
        val response = client.get("/api/me") {
            header(DEVICE_ID_HEADER, "ghost")
        }
        assertEquals(HttpStatusCode.Unauthorized, response.status)
        assertEquals("", response.bodyAsText())
    }

    private class FakeProfileReads(
        private val profiles: Map<String, ProfileResponse>,
    ) : ProfileReads {
        val queried: MutableList<String> = mutableListOf()

        override suspend fun profileOf(deviceId: DeviceId): ProfileResponse? {
            queried.add(deviceId.value)
            return profiles[deviceId.value]
        }

        override suspend fun recentDuelsOf(playerId: PlayerId, limit: Int): List<DuelSummaryResponse> =
            emptyList()
    }
}
