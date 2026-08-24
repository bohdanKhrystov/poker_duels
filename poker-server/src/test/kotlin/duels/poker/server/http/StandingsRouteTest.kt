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
private class RecordingProfileReads(val profiles: Map<String, duels.poker.server.protocol.http.ProfileResponse> = emptyMap()) : ProfileReads {
    val profileOfCalls: MutableList<String> = mutableListOf()

    override suspend fun profileOf(playerId: PlayerId): duels.poker.server.protocol.http.ProfileResponse? {
        profileOfCalls.add(playerId.value)
        return profiles.values.find { it.playerId == playerId.value }
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
            standingsRoutes(FixedProfileReads(emptyMap()), standings, CLOCK, identitiesFor(emptyMap()))
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
            standingsRoutes(FixedProfileReads(emptyMap()), standings, CLOCK, identitiesFor(emptyMap()))
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
            standingsRoutes(FixedProfileReads(emptyMap()), standings, CLOCK, identitiesFor(emptyMap()))
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
            standingsRoutes(FixedProfileReads(emptyMap()), standings, CLOCK, identitiesFor(emptyMap()))
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
            standingsRoutes(FixedProfileReads(emptyMap()), standings, CLOCK, identitiesFor(emptyMap()))
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
            standingsRoutes(FixedProfileReads(emptyMap()), standings, CLOCK, identitiesFor(emptyMap()))
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
                    deviceRouteLive = true,
                ),
                "device-2" to duels.poker.server.protocol.http.ProfileResponse(
                    "player-2",
                    coinBalance = 500,
                    displayName = "Bob",
                    displayNameRemoved = false,
                    deviceRouteLive = true,
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
            standingsRoutes(profileReads, standings, CLOCK, identitiesFor(profileReads.profiles))
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

    @Test
    fun aLimitTheServerWillNotParseIsFourHundredAndReadsNothing(): Unit = testApplication {
        val standings = RecordingStandingsReads()
        application {
            module()
            standingsRoutes(FixedProfileReads(emptyMap()), standings, CLOCK, identitiesFor(emptyMap()))
        }

        // Test limit=0
        val zeroResponse = client.get("/api/standings?limit=0")
        assertEquals(HttpStatusCode.BadRequest, zeroResponse.status)
        assertEquals("", zeroResponse.bodyAsText())
        assertEquals(0, standings.calls.size, "standings.standingsPage should not be called for limit=0")

        // Test limit=-1
        val negativeResponse = client.get("/api/standings?limit=-1")
        assertEquals(HttpStatusCode.BadRequest, negativeResponse.status)
        assertEquals("", negativeResponse.bodyAsText())
        assertEquals(0, standings.calls.size, "standings.standingsPage should not be called for limit=-1")

        // Test limit=abc
        val abcResponse = client.get("/api/standings?limit=abc")
        assertEquals(HttpStatusCode.BadRequest, abcResponse.status)
        assertEquals("", abcResponse.bodyAsText())
        assertEquals(0, standings.calls.size, "standings.standingsPage should not be called for limit=abc")
    }

    @Test
    fun aLimitAboveTheCapIsClampedRatherThanRefused(): Unit = testApplication {
        val standings = RecordingStandingsReads()
        application {
            module()
            standingsRoutes(FixedProfileReads(emptyMap()), standings, CLOCK, identitiesFor(emptyMap()))
        }

        val response = client.get("/api/standings?limit=999")
        assertEquals(HttpStatusCode.OK, response.status)
        val call = standings.calls.single()
        // The route asks for limit + 1, and 999 is clamped to 50, so it asks for 51
        assertEquals(51, call.limit)
    }

    @Test
    fun aCursorThatDoesNotDecodeIsFourHundredAndReadsNothing(): Unit = testApplication {
        val standings = RecordingStandingsReads()
        application {
            module()
            standingsRoutes(FixedProfileReads(emptyMap()), standings, CLOCK, identitiesFor(emptyMap()))
        }

        // Test after=not-a-cursor
        val invalidCursorResponse = client.get("/api/standings?after=not-a-cursor")
        assertEquals(HttpStatusCode.BadRequest, invalidCursorResponse.status)
        assertEquals("", invalidCursorResponse.bodyAsText())
        assertEquals(0, standings.calls.size, "standings.standingsPage should not be called for invalid cursor")

        // Test after= (present and empty)
        val emptyCursorResponse = client.get("/api/standings?after=")
        assertEquals(HttpStatusCode.BadRequest, emptyCursorResponse.status)
        assertEquals("", emptyCursorResponse.bodyAsText())
        assertEquals(0, standings.calls.size, "standings.standingsPage should not be called for empty cursor")
    }

    @Test
    fun aCursorFromAnotherSeasonIsTheSameFourHundred(): Unit = testApplication {
        // Create a cursor with asOf in August 2026
        val augustCursorAsOf = Instant.parse("2026-08-20T09:00:00Z")
        val augustCursor = StandingsCursor(augustCursorAsOf, 40, UUID.fromString("00000000-0000-0000-0000-00000000000a"))
        val augustCursorString = augustCursor.encoded()

        // Create a cursor with asOf in September 2026
        val septemberCursorAsOf = Instant.parse("2026-09-01T00:00:00Z")
        val septemberCursor = StandingsCursor(septemberCursorAsOf, 40, UUID.fromString("00000000-0000-0000-0000-00000000000b"))
        val septemberCursorString = septemberCursor.encoded()

        // Test 1: August cursor under August clock (2026-08-20T09:00:00Z) should return 200
        testApplication {
            val augustStandings = RecordingStandingsReads()
            val augustClock = Clock.fixed(Instant.parse("2026-08-20T09:00:00Z"), ZoneOffset.UTC)
            application {
                module()
                standingsRoutes(FixedProfileReads(emptyMap()), augustStandings, augustClock, identitiesFor(emptyMap()))
            }
            val augustUnderAugustResponse = client.get("/api/standings?after=$augustCursorString")
            assertEquals(HttpStatusCode.OK, augustUnderAugustResponse.status)
            assertEquals(1, augustStandings.calls.size, "standings.standingsPage should be called for valid cursor in same season")
        }

        // Test 2: August cursor under September clock (2026-09-01T00:00:01Z) should return 400
        testApplication {
            val septemberStandings = RecordingStandingsReads()
            val septemberClock = Clock.fixed(Instant.parse("2026-09-01T00:00:01Z"), ZoneOffset.UTC)
            application {
                module()
                standingsRoutes(FixedProfileReads(emptyMap()), septemberStandings, septemberClock, identitiesFor(emptyMap()))
            }
            val augustUnderSeptemberResponse = client.get("/api/standings?after=$augustCursorString")
            assertEquals(HttpStatusCode.BadRequest, augustUnderSeptemberResponse.status)
            assertEquals("", augustUnderSeptemberResponse.bodyAsText())
            assertEquals(0, septemberStandings.calls.size, "standings.standingsPage should not be called for cursor from different season")
        }

        // Test 3: September cursor under August clock (2026-08-20T09:00:00Z) should return 400
        testApplication {
            val augustStandings2 = RecordingStandingsReads()
            val augustClock2 = Clock.fixed(Instant.parse("2026-08-20T09:00:00Z"), ZoneOffset.UTC)
            application {
                module()
                standingsRoutes(FixedProfileReads(emptyMap()), augustStandings2, augustClock2, identitiesFor(emptyMap()))
            }
            val septemberUnderAugustResponse = client.get("/api/standings?after=$septemberCursorString")
            assertEquals(HttpStatusCode.BadRequest, septemberUnderAugustResponse.status)
            assertEquals("", septemberUnderAugustResponse.bodyAsText())
            assertEquals(0, augustStandings2.calls.size, "standings.standingsPage should not be called for cursor from different season")
        }

        // Test 4: September cursor under September clock (2026-09-01T00:00:01Z) should return 200
        testApplication {
            val septemberStandings2 = RecordingStandingsReads()
            val septemberClock2 = Clock.fixed(Instant.parse("2026-09-01T00:00:01Z"), ZoneOffset.UTC)
            application {
                module()
                standingsRoutes(FixedProfileReads(emptyMap()), septemberStandings2, septemberClock2, identitiesFor(emptyMap()))
            }
            val septemberUnderSeptemberResponse = client.get("/api/standings?after=$septemberCursorString")
            assertEquals(HttpStatusCode.OK, septemberUnderSeptemberResponse.status)
            assertEquals(1, septemberStandings2.calls.size, "standings.standingsPage should be called for valid cursor in same season")
        }
    }
}
