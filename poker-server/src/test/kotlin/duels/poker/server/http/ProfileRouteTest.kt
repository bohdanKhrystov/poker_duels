package duels.poker.server.http

import duels.poker.server.module
import duels.poker.server.protocol.http.DuelOutcomeLabel
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

    @Test
    fun aKnownDeviceGetsItsDuelsInTheOrderTheReaderReturnedThem() = testApplication {
        val duel1 = DuelSummaryResponse(
            duelId = "duel-1",
            opponentPlayerId = "p-bob",
            outcome = DuelOutcomeLabel.WON,
            coinDelta = 1,
            handsPlayed = null,
            finishedAt = "2026-08-12T10:00:00Z",
        )
        val duel2 = DuelSummaryResponse(
            duelId = "duel-2",
            opponentPlayerId = "p-charlie",
            outcome = DuelOutcomeLabel.LOST,
            coinDelta = -1,
            handsPlayed = null,
            finishedAt = "2026-08-12T09:00:00Z",
        )
        val reads = FakeProfileReads(
            mapOf("alice" to ProfileResponse("p-alice", 0)),
            mapOf("p-alice" to listOf(duel1, duel2)),
        )
        application {
            module()
            profileRoutes(reads)
        }
        val response = client.get("/api/me/duels") {
            header(DEVICE_ID_HEADER, "alice")
        }
        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        assertTrue(body.contains("\"duelId\":\"duel-1\""))
        assertTrue(body.contains("\"duelId\":\"duel-2\""))
        assertTrue(body.indexOf("duel-1") < body.indexOf("duel-2"))
    }

    @Test
    fun anAbsentLimitAsksForTheDefault() = testApplication {
        val reads = FakeProfileReads(
            mapOf("alice" to ProfileResponse("p-alice", 0)),
            mapOf("p-alice" to emptyList()),
        )
        application {
            module()
            profileRoutes(reads)
        }
        val response = client.get("/api/me/duels") {
            header(DEVICE_ID_HEADER, "alice")
        }
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(DEFAULT_DUEL_LIMIT, reads.lastLimitRequested)
    }

    @Test
    fun aLimitAboveTheCapIsClamped() = testApplication {
        val reads = FakeProfileReads(
            mapOf("alice" to ProfileResponse("p-alice", 0)),
            mapOf("p-alice" to emptyList()),
        )
        application {
            module()
            profileRoutes(reads)
        }
        val response = client.get("/api/me/duels?limit=999") {
            header(DEVICE_ID_HEADER, "alice")
        }
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(MAX_DUEL_LIMIT, reads.lastLimitRequested)
    }

    @Test
    fun aNonNumericLimitIsABadRequest() = testApplication {
        val reads = FakeProfileReads(
            mapOf("alice" to ProfileResponse("p-alice", 0)),
            mapOf("p-alice" to emptyList()),
        )
        application {
            module()
            profileRoutes(reads)
        }
        val response = client.get("/api/me/duels?limit=abc") {
            header(DEVICE_ID_HEADER, "alice")
        }
        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertEquals(0, reads.lastLimitRequested)
    }

    @Test
    fun aPlayerWithNoDuelsGetsAnEmptyListAndTwoHundred() = testApplication {
        val reads = FakeProfileReads(
            mapOf("alice" to ProfileResponse("p-alice", 0)),
            mapOf("p-alice" to emptyList()),
        )
        application {
            module()
            profileRoutes(reads)
        }
        val response = client.get("/api/me/duels") {
            header(DEVICE_ID_HEADER, "alice")
        }
        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        assertTrue(body.contains("\"duels\":[]"))
    }

    @Test
    fun anUnknownDeviceIsRefusedBeforeTheLimitIsParsed() = testApplication {
        val reads = FakeProfileReads(emptyMap())
        application {
            module()
            profileRoutes(reads)
        }
        val response = client.get("/api/me/duels?limit=abc") {
            header(DEVICE_ID_HEADER, "ghost")
        }
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    private class FakeProfileReads(
        private val profiles: Map<String, ProfileResponse>,
        private val duels: Map<String, List<DuelSummaryResponse>> = emptyMap(),
    ) : ProfileReads {
        val queried: MutableList<String> = mutableListOf()
        var lastLimitRequested: Int = 0

        override suspend fun profileOf(deviceId: DeviceId): ProfileResponse? {
            queried.add(deviceId.value)
            return profiles[deviceId.value]
        }

        override suspend fun recentDuelsOf(playerId: PlayerId, limit: Int): List<DuelSummaryResponse> {
            lastLimitRequested = limit
            return duels[playerId.value] ?: emptyList()
        }
    }
}
