package duels.poker.server.http

import duels.poker.server.module
import duels.poker.server.protocol.http.DuelOutcomeLabel
import duels.poker.server.protocol.http.DuelSummaryResponse
import duels.poker.server.protocol.http.ProfileResponse
import duels.poker.server.protocol.http.duelSummaryResponse
import duels.poker.server.protocol.http.profileResponse
import duels.poker.server.session.DeviceId
import duels.poker.server.session.PlayerId
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
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
                "alice" to profileResponse("p-alice", 4),
            ),
        )
        application {
            module()
            profileRoutes(reads, FakeProfileWrites())
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
                "bob" to profileResponse("p-bob", -3),
            ),
        )
        application {
            module()
            profileRoutes(reads, FakeProfileWrites())
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
            profileRoutes(reads, FakeProfileWrites())
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
            profileRoutes(reads, FakeProfileWrites())
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
            profileRoutes(reads, FakeProfileWrites())
        }
        val response = client.get("/api/me") {
            header(DEVICE_ID_HEADER, "ghost")
        }
        assertEquals(HttpStatusCode.Unauthorized, response.status)
        assertEquals("", response.bodyAsText())
    }

    @Test
    fun aKnownDeviceGetsItsDuelsInTheOrderTheReaderReturnedThem() = testApplication {
        val duel1 = duelSummaryResponse(
            duelId = "duel-1",
            opponentPlayerId = "p-bob",
            outcome = DuelOutcomeLabel.WON,
            coinDelta = 1,
            handsPlayed = 10,
            finishedAt = "2026-08-12T10:00:00Z",
        )
        val duel2 = duelSummaryResponse(
            duelId = "duel-2",
            opponentPlayerId = "p-charlie",
            outcome = DuelOutcomeLabel.LOST,
            coinDelta = -1,
            handsPlayed = 5,
            finishedAt = "2026-08-12T09:00:00Z",
        )
        val reads = FakeProfileReads(
            mapOf("alice" to profileResponse("p-alice", 0)),
            mapOf("p-alice" to listOf(duel1, duel2)),
        )
        application {
            module()
            profileRoutes(reads, FakeProfileWrites())
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
            mapOf("alice" to profileResponse("p-alice", 0)),
            mapOf("p-alice" to emptyList()),
        )
        application {
            module()
            profileRoutes(reads, FakeProfileWrites())
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
            mapOf("alice" to profileResponse("p-alice", 0)),
            mapOf("p-alice" to emptyList()),
        )
        application {
            module()
            profileRoutes(reads, FakeProfileWrites())
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
            mapOf("alice" to profileResponse("p-alice", 0)),
            mapOf("p-alice" to emptyList()),
        )
        application {
            module()
            profileRoutes(reads, FakeProfileWrites())
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
            mapOf("alice" to profileResponse("p-alice", 0)),
            mapOf("p-alice" to emptyList()),
        )
        application {
            module()
            profileRoutes(reads, FakeProfileWrites())
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
            profileRoutes(reads, FakeProfileWrites())
        }
        val response = client.get("/api/me/duels?limit=abc") {
            header(DEVICE_ID_HEADER, "ghost")
        }
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun aKnownDeviceSetsItsName() = testApplication {
        val reads = FakeProfileReads(mapOf("alice" to profileResponse("p-alice", 4)))
        val writes = FakeProfileWrites(SetNameResult.NameSet(profileResponse("p-alice", 4, "Alice")))
        application {
            module()
            profileRoutes(reads, writes)
        }
        val response = client.put("/api/me/name") {
            header(DEVICE_ID_HEADER, "alice")
            header(HttpHeaders.ContentType, "application/json")
            setBody("""{"name":"Alice"}""")
        }
        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        assertTrue(body.contains("\"playerId\":\"p-alice\""))
        assertTrue(body.contains("\"coinBalance\":4"))
        assertTrue(body.contains("\"displayName\":\"Alice\""))
    }

    @Test
    fun theCanonicalNameIsWhatReachesThePort() = testApplication {
        val reads = FakeProfileReads(mapOf("alice" to profileResponse("p-alice", 0)))
        val writes = FakeProfileWrites(SetNameResult.NameSet(profileResponse("p-alice", 0, "Bob")))
        application {
            module()
            profileRoutes(reads, writes)
        }
        val response = client.put("/api/me/name") {
            header(DEVICE_ID_HEADER, "alice")
            header(HttpHeaders.ContentType, "application/json")
            setBody("""{"name":"  Bob  "}""")
        }
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("Bob", writes.received.single())
    }

    @Test
    fun anAbsentDeviceIdIsRefusedBeforeTheBodyIsRead() = testApplication {
        val reads = FakeProfileReads(emptyMap())
        val writes = FakeProfileWrites()
        application {
            module()
            profileRoutes(reads, writes)
        }
        // A valid body, so a wrong implementation that answered on the body would answer 200,
        // not 401 — the identity refusal below, not the body one.
        val response = client.put("/api/me/name") {
            header(HttpHeaders.ContentType, "application/json")
            setBody("""{"name":"Bob"}""")
        }
        assertEquals(HttpStatusCode.Unauthorized, response.status)
        assertTrue(writes.received.isEmpty())
    }

    @Test
    fun anAbsentDeviceIdIsRefusedBeforeAMalformedBodyIsRead() = testApplication {
        val reads = FakeProfileReads(emptyMap())
        val writes = FakeProfileWrites()
        application {
            module()
            profileRoutes(reads, writes)
        }
        // A body that cannot even decode: if identity were checked after the body were read,
        // this would answer 400 — the same wrong answer the body's own defect would produce —
        // instead of 401. This is what tells "identity first" apart from "identity checked
        // before the port is called, but only after a harmless body is silently discarded".
        val response = client.put("/api/me/name") {
            header(HttpHeaders.ContentType, "application/json")
            setBody("""{"nickname":"bob"}""")
        }
        assertEquals(HttpStatusCode.Unauthorized, response.status)
        assertTrue(writes.received.isEmpty())
    }

    @Test
    fun anUnknownDeviceIdIsRefusedBeforeTheNameIsSet() = testApplication {
        val reads = FakeProfileReads(emptyMap())
        val writes = FakeProfileWrites()
        application {
            module()
            profileRoutes(reads, writes)
        }
        val response = client.put("/api/me/name") {
            header(DEVICE_ID_HEADER, "ghost")
            header(HttpHeaders.ContentType, "application/json")
            setBody("""{"name":"Bob"}""")
        }
        assertEquals(HttpStatusCode.Unauthorized, response.status)
        assertTrue(writes.received.isEmpty())
    }

    @Test
    fun aNameTheRulesRefuseIsABadRequest() = testApplication {
        val reads = FakeProfileReads(mapOf("alice" to profileResponse("p-alice", 0)))
        val writes = FakeProfileWrites()
        application {
            module()
            profileRoutes(reads, writes)
        }
        val response = client.put("/api/me/name") {
            header(DEVICE_ID_HEADER, "alice")
            header(HttpHeaders.ContentType, "application/json")
            setBody("""{"name":"  "}""")
        }
        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertTrue(writes.received.isEmpty())
    }

    @Test
    fun aBodyThatIsNotTheRequestShapeIsABadRequest() = testApplication {
        val reads = FakeProfileReads(mapOf("alice" to profileResponse("p-alice", 0)))
        val writes = FakeProfileWrites()
        application {
            module()
            profileRoutes(reads, writes)
        }
        val response = client.put("/api/me/name") {
            header(DEVICE_ID_HEADER, "alice")
            header(HttpHeaders.ContentType, "application/json")
            setBody("""{"nickname":"bob"}""")
        }
        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertTrue(writes.received.isEmpty())
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

    private class FakeProfileWrites(
        private val result: SetNameResult = SetNameResult.NameTaken,
    ) : ProfileWrites {
        val received: MutableList<String> = mutableListOf()

        override suspend fun setDisplayName(playerId: PlayerId, canonicalName: String): SetNameResult {
            received.add(canonicalName)
            return result
        }
    }
}
