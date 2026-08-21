package duels.poker.server.http

import duels.poker.server.module
import duels.poker.server.protocol.http.SelfStandingResponse
import duels.poker.server.protocol.http.StandingRow
import duels.poker.server.season.Season
import duels.poker.server.session.PlayerId
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
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
 * them. `standingOf` is unused by this file's tests, none of which send an `X-Device-Id` header,
 * so it always answers `null`.
 */
private class RecordingStandingsReads(private val rows: List<StandingRow> = emptyList()) : StandingsReads {
    val calls: MutableList<RecordedCall> = mutableListOf()

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

    override suspend fun standingOf(playerId: PlayerId, season: Season, asOf: Instant): SelfStandingResponse? = null
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
}
