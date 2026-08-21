package duels.poker.server.http

import duels.poker.server.module
import duels.poker.server.protocol.http.SelfStandingResponse
import duels.poker.server.protocol.http.StandingRow
import duels.poker.server.protocol.http.StandingsResponse
import duels.poker.server.protocol.protocolJson
import duels.poker.server.season.Season
import duels.poker.server.session.PlayerId
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID

/**
 * A [StandingsReads] double that records every `standingsPage` call — the exact
 * `(season, asOf, limit, after)` tuple the route computed — and answers [rows] to every one of
 * them. Optionally tracks `standingOf` calls and returns [standingOfResponse] for them.
 */
private class RecordingStandingsReads(
    private val rows: List<StandingRow> = emptyList(),
    private val standingOfResponse: SelfStandingResponse? = null,
) : StandingsReads {
    val calls: MutableList<RecordedCall> = mutableListOf()
    val standingOfCalls: MutableList<Pair<PlayerId, Season>> = mutableListOf()

    data class RecordedCall(val season: Season, val asOf: Instant, val limit: Int, val after: StandingsCursor?)

    override suspend fun standingsPage(
        season: Season,
        asOf: Instant,
        limit: Int,
        after: StandingsCursor?,
    ): List<StandingRow> {
        calls.add(RecordedCall(season, asOf, limit, after))
        return rows
    }

    override suspend fun standingOf(playerId: PlayerId, season: Season, asOf: Instant): SelfStandingResponse? {
        standingOfCalls.add(playerId to season)
        return standingOfResponse
    }
}

/**
 * A [ProfileReads] double that records every `profileOf` call and answers from a fixed map.
 */
private class RecordingProfileReads(private val profiles: Map<String, duels.poker.server.protocol.http.ProfileResponse> = emptyMap()) : ProfileReads {
    val profileOfCalls: MutableList<String> = mutableListOf()

    override suspend fun profileOf(deviceId: duels.poker.server.session.DeviceId): duels.poker.server.protocol.http.ProfileResponse? {
        profileOfCalls.add(deviceId.value)
        return profiles[deviceId.value]
    }

    override suspend fun recentDuelsOf(
        playerId: PlayerId,
        limit: Int,
        after: DuelCursor?,
        filter: DuelFilter,
    ): List<duels.poker.server.protocol.http.DuelSummaryResponse> = emptyList()
}

/** The server's clock for every test here — fixed, so a pinned cutoff is the only clock reading. */
private val CLOCK: Clock = Clock.fixed(Instant.parse("2026-08-20T09:00:00Z"), ZoneOffset.UTC)

class StandingsRouteTest {
    @Test
    fun theResponseNamesTheSeasonTheServersClockIsIn() = testApplication {
        val standings = RecordingStandingsReads()
        application {
            module()
            standingsRoutes(FixedProfileReads(emptyMap()), standings, CLOCK)
        }
        val response = client.get("/api/standings")
        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("\"season\":\"2026-08\""))
        assertEquals(Season(2026, 8), standings.calls.single().season)
    }

    @Test
    fun aCursorlessRequestPinsTheWalkToTheInstantTheClockReports() = testApplication {
        val standings = RecordingStandingsReads()
        application {
            module()
            standingsRoutes(FixedProfileReads(emptyMap()), standings, CLOCK)
        }
        val response = client.get("/api/standings?limit=5")
        assertEquals(HttpStatusCode.OK, response.status)
        val call = standings.calls.single()
        assertEquals(Instant.parse("2026-08-20T09:00:00Z"), call.asOf)
        assertNull(call.after)
        assertEquals(6, call.limit)
    }

    @Test
    fun aCursoredRequestReusesTheCursorsCutoffAndNotTheClock() = testApplication {
        val standings = RecordingStandingsReads()
        application {
            module()
            standingsRoutes(FixedProfileReads(emptyMap()), standings, CLOCK)
        }
        // cursorAsOf sits inside August 2026 — CLOCK's own season — but earlier than CLOCK's own
        // instant, so a route that re-read the clock instead of the cursor is caught right here.
        val cursorAsOf = Instant.parse("2026-08-15T12:00:00Z")
        val cursor = StandingsCursor(cursorAsOf, 40, UUID.fromString("00000000-0000-0000-0000-00000000000a"))
        val response = client.get("/api/standings?after=${cursor.encoded()}")
        assertEquals(HttpStatusCode.OK, response.status)
        val call = standings.calls.single()
        assertEquals(cursorAsOf, call.asOf)
        assertNotEquals(Instant.parse("2026-08-20T09:00:00Z"), call.asOf)
    }

    @Test
    fun theProbeRowIsNeverServedAndTheCursorNamesTheRowThatWas() = testApplication {
        // Four rows: the route asks for limit + 1 = 4 rows, expecting to get 3 served and 1 probe
        val row1 = StandingRow(1, "00000000-0000-0000-0000-000000000001", "Alice", 100)
        val row2 = StandingRow(2, "00000000-0000-0000-0000-000000000002", "Bob", 90)
        val row3 = StandingRow(3, "00000000-0000-0000-0000-000000000003", "Charlie", 80)
        val row4 = StandingRow(4, "00000000-0000-0000-0000-000000000004", "Probe", 70)
        val standings = RecordingStandingsReads(listOf(row1, row2, row3, row4))
        application {
            module()
            standingsRoutes(FixedProfileReads(emptyMap()), standings, CLOCK)
        }
        val response = client.get("/api/standings?limit=3")
        assertEquals(HttpStatusCode.OK, response.status)
        val body = protocolJson.decodeFromString<StandingsResponse>(response.bodyAsText())
        // Only 3 rows should be served, not 4
        assertEquals(3, body.rows.size)
        // Row 4 (the probe) should not appear in the response
        val servedIds = body.rows.map { it.playerId }
        assertEquals(listOf("00000000-0000-0000-0000-000000000001", "00000000-0000-0000-0000-000000000002", "00000000-0000-0000-0000-000000000003"), servedIds)
        assertTrue(servedIds.none { it == "00000000-0000-0000-0000-000000000004" })
        // nextCursor should decode to row 3, not row 4
        val nextCursor = body.nextCursor
        assertNotNull(nextCursor)
        val decodedCursor = standingsCursorOrNull(nextCursor!!, Season(2026, 8))
        assertNotNull(decodedCursor)
        assertEquals(row3.coins, decodedCursor!!.coins)
        assertEquals(row3.playerId, decodedCursor.playerId.toString())
    }

    @Test
    fun theLastPageSaysThereIsNoNextPage() = testApplication {
        // Exactly 3 rows — no probe row needed
        val row1 = StandingRow(1, "00000000-0000-0000-0000-000000000001", "Alice", 100)
        val row2 = StandingRow(2, "00000000-0000-0000-0000-000000000002", "Bob", 90)
        val row3 = StandingRow(3, "00000000-0000-0000-0000-000000000003", "Charlie", 80)
        val standings = RecordingStandingsReads(listOf(row1, row2, row3))
        application {
            module()
            standingsRoutes(FixedProfileReads(emptyMap()), standings, CLOCK)
        }
        val response = client.get("/api/standings?limit=3")
        assertEquals(HttpStatusCode.OK, response.status)
        val body = protocolJson.decodeFromString<StandingsResponse>(response.bodyAsText())
        // All 3 rows should be served
        assertEquals(3, body.rows.size)
        // nextCursor should be null on the last page
        assertNull(body.nextCursor)
    }

    @Test
    fun anEmptyLadderIsTwoHundredWithAnEmptyPage() = testApplication {
        // No rows at all
        val standings = RecordingStandingsReads(emptyList())
        application {
            module()
            standingsRoutes(FixedProfileReads(emptyMap()), standings, CLOCK)
        }
        val response = client.get("/api/standings?limit=3")
        // Should be 200, not 404
        assertEquals(HttpStatusCode.OK, response.status)
        val body = protocolJson.decodeFromString<StandingsResponse>(response.bodyAsText())
        // rows should be empty
        assertEquals(emptyList<StandingRow>(), body.rows)
        // nextCursor should be null
        assertNull(body.nextCursor)
    }

    @Test
    fun theSelfObjectTakesOneOfItsThreeShapes() = testApplication {
        // Three rows for all requests
        val row1 = StandingRow(1, "00000000-0000-0000-0000-000000000001", "Alice", 100)
        val row2 = StandingRow(2, "00000000-0000-0000-0000-000000000002", "Bob", 90)
        val row3 = StandingRow(3, "00000000-0000-0000-0000-000000000003", "Charlie", 80)
        val rows = listOf(row1, row2, row3)

        // Create profile and standings readers that can handle all three scenarios
        val profileReads = RecordingProfileReads(
            mapOf(
                "device-1" to duels.poker.server.protocol.http.ProfileResponse(
                    "player-1",
                    coinBalance = 1000,
                    displayName = "Alice",
                    displayNameRemoved = false,
                ),
                "device-2" to duels.poker.server.protocol.http.ProfileResponse(
                    "player-2",
                    coinBalance = 500,
                    displayName = "Bob",
                    displayNameRemoved = false,
                ),
            ),
        )

        // Use a standings reader that returns null for device-2's standingOf call (no standing)
        val standings = object : StandingsReads {
            override suspend fun standingsPage(
                season: Season,
                asOf: Instant,
                limit: Int,
                after: StandingsCursor?,
            ): List<StandingRow> = rows

            override suspend fun standingOf(playerId: PlayerId, season: Season, asOf: Instant): SelfStandingResponse? =
                when (playerId.value) {
                    "player-1" -> SelfStandingResponse("player-1", 1, 100)
                    else -> null // device-2's player has no standing
                }
        }

        application {
            module()
            standingsRoutes(profileReads, standings, CLOCK)
        }

        // Test 1: No X-Device-Id header
        val noDeviceResponse = client.get("/api/standings?limit=3")
        assertEquals(HttpStatusCode.OK, noDeviceResponse.status)
        val noDeviceBody = protocolJson.decodeFromString<StandingsResponse>(noDeviceResponse.bodyAsText())
        // self should be null
        assertNull(noDeviceBody.self)
        // Rows should match
        assertEquals(rows, noDeviceBody.rows)
        // ProfileReads.profileOf should not have been called
        assertTrue(profileReads.profileOfCalls.isEmpty(), "profileOf should not be called when no device header is present")

        // Test 2: Known device with standing
        profileReads.profileOfCalls.clear() // Clear previous calls
        val withDeviceResponse = client.get("/api/standings?limit=3") {
            headers["X-Device-Id"] = "device-1"
        }
        assertEquals(HttpStatusCode.OK, withDeviceResponse.status)
        val withDeviceBody = protocolJson.decodeFromString<StandingsResponse>(withDeviceResponse.bodyAsText())
        // self should carry rank and coins
        assertEquals("player-1", withDeviceBody.self?.playerId)
        assertEquals(1, withDeviceBody.self?.rank)
        assertEquals(100, withDeviceBody.self?.coins)
        // Rows should match
        assertEquals(rows, withDeviceBody.rows)

        // Test 3: Known device with no standing (null response from standingOf)
        profileReads.profileOfCalls.clear() // Clear previous calls
        val noStandingResponse = client.get("/api/standings?limit=3") {
            headers["X-Device-Id"] = "device-2"
        }
        assertEquals(HttpStatusCode.OK, noStandingResponse.status)
        val noStandingBody = protocolJson.decodeFromString<StandingsResponse>(noStandingResponse.bodyAsText())
        // self should be present with both rank and coins as null
        assertEquals("player-2", noStandingBody.self?.playerId)
        assertNull(noStandingBody.self?.rank)
        assertNull(noStandingBody.self?.coins)
        // Rows should match
        assertEquals(rows, noStandingBody.rows)
    }
}
