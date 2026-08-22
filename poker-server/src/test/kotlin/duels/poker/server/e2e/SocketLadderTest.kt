package duels.poker.server.e2e

import duels.poker.engine.duel.DuelFormat
import duels.poker.engine.duel.DuelOutcome
import duels.poker.server.db.PostgresDuelResultStore
import duels.poker.server.db.PostgresPlayerDirectory
import duels.poker.server.db.PostgresProfileReads
import duels.poker.server.db.PostgresTestSupport
import duels.poker.server.duel.FinishedDuel
import duels.poker.server.duel.formatLabel
import duels.poker.server.http.DEVICE_ID_HEADER
import duels.poker.server.protocol.http.StandingRow
import duels.poker.server.protocol.http.StandingsResponse
import duels.poker.server.protocol.protocolJson
import duels.poker.server.season.Season
import duels.poker.server.season.currentSeason
import duels.poker.server.session.DeviceId
import duels.poker.server.session.Player
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.decodeFromString
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import java.time.Clock
import java.time.Instant
import java.util.UUID
import javax.sql.DataSource
import kotlin.test.fail

private const val LADDER_LIMIT = 10
private const val FILLER_ONE_DEVICE = "e2e-f1"
private const val FILLER_TWO_DEVICE = "e2e-f2"

/**
 * The ladder is readable over HTTP from inside the same running application that hosts the
 * two WebSocket clients. Two seated players who have finished nothing are on no ladder and hold
 * no place — the *before* half of every later assertion in this story.
 *
 * Every criterion in this story is a difference, and a difference needs a before: [ADR-0061] §4
 * *"the ladder is results, not players"* so two profiles that exist and have duelled nobody
 * appear on no row. [ADR-0065] §4's three answers distinguish "known, and placed nowhere" from
 * "who are you", and both are asserted here because a `self` that collapsed them would still look
 * right on every later test in this class.
 */
@Timeout(120)
internal class SocketLadderTest {
    private lateinit var dataSource: DataSource

    // Read once per test instance (JUnit5's default per-method lifecycle makes a fresh instance,
    // and this property initializer, run for every @Test), so every fixture instant below agrees
    // on the same season however close to a month boundary the suite runs.
    private val ladderSeason: Season = currentSeason(Clock.systemUTC())

    /** An instant inside [ladderSeason], anchored to its inclusive lower edge, never to the wall clock. */
    private fun thisSeasonAt(offsetMillis: Long): Instant = ladderSeason.start.plusMillis(offsetMillis)

    /** One second before [ladderSeason] begins — the prior calendar month, regardless of when this runs. */
    private fun lastSeasonAt(): Instant = ladderSeason.start.minusSeconds(1)

    @BeforeEach
    fun setup() {
        PostgresTestSupport.requireDocker()
        dataSource = freshMigratedDatabase()
    }

    /**
     * Reads the standings from `/api/standings` with the given parameters, asserting the response
     * is `200`, then decodes the body with [protocolJson] directly — the same manual decode every
     * other e2e test in this package uses for frames off a socket, never client-side content
     * negotiation.
     *
     * @param deviceId The device ID to send in the `X-Device-Id` header. If `null`, no header is sent.
     * @param limit The maximum number of rows to return. Defaults to [LADDER_LIMIT].
     * @param after The cursor for pagination. If `null`, not included in the query string.
     */
    private suspend fun HttpClient.ladder(
        deviceId: String?,
        limit: Int = LADDER_LIMIT,
        after: String? = null,
    ): StandingsResponse {
        val queryParams = mutableListOf("limit=$limit")
        if (after != null) {
            queryParams.add("after=$after")
        }
        val query = queryParams.joinToString("&")
        val response = get("/api/standings?$query") {
            if (deviceId != null) {
                header(DEVICE_ID_HEADER, deviceId)
            }
        }
        assertEquals(
            HttpStatusCode.OK,
            response.status,
            "GET /api/standings for deviceId=$deviceId returned ${response.status}",
        )
        return protocolJson.decodeFromString(response.bodyAsText())
    }

    @Test
    fun theLadderKnowsBothDuellistsAndPlacesNeitherBeforeTheyPlay(): Unit = runBlocking {
        testApplication {
            installDuelServer(dataSource)
            val client = createClient { install(WebSockets) }
            val duel = client.openSocketDuel()

            // Get the profiles of both players for comparison
            val hostPlayerId = PostgresProfileReads(dataSource).profileOf(DeviceId(HOST_DEVICE))
                ?.playerId
                ?: error("Host profile not found")
            val guestPlayerId = PostgresProfileReads(dataSource).profileOf(DeviceId(GUEST_DEVICE))
                ?.playerId
                ?: error("Guest profile not found")

            // Request 1: Host device
            val hostStandings = client.ladder(HOST_DEVICE)
            assertEquals(true, hostStandings.rows.isEmpty(), "rows should be empty for host device")
            assertEquals(hostPlayerId, hostStandings.self?.playerId, "self.playerId should match host")
            assertEquals(null, hostStandings.self?.rank, "self.rank should be null for host")
            assertEquals(null, hostStandings.self?.coins, "self.coins should be null for host")
            assertEquals(
                true,
                hostStandings.season.isNotBlank(),
                "season should be non-blank",
            )

            // Request 2: Guest device
            val guestStandings = client.ladder(GUEST_DEVICE)
            assertEquals(true, guestStandings.rows.isEmpty(), "rows should be empty for guest device")
            assertEquals(guestPlayerId, guestStandings.self?.playerId, "self.playerId should match guest")
            assertEquals(null, guestStandings.self?.rank, "self.rank should be null for guest")
            assertEquals(null, guestStandings.self?.coins, "self.coins should be null for guest")

            // Request 3: Unknown device
            val strangerStandings = client.ladder("e2e-stranger")
            assertEquals(true, strangerStandings.rows.isEmpty(), "rows should be empty for unknown device")
            assertNull(strangerStandings.self, "self should be null for unknown device")

            // Request 4: No device ID header
            val noHeaderStandings = client.ladder(null)
            assertEquals(true, noHeaderStandings.rows.isEmpty(), "rows should be empty for no header")
            assertNull(noHeaderStandings.self, "self should be null for no header")
        }
    }

    private suspend fun resolvePlayer(deviceId: String): Player =
        PostgresPlayerDirectory(dataSource).resolve(DeviceId(deviceId))

    private suspend fun recordWin(winner: Player, loser: Player, at: Instant) {
        PostgresDuelResultStore(dataSource).record(
            FinishedDuel(
                id = UUID.randomUUID(),
                format = formatLabel(DuelFormat.DEFAULT),
                startedAt = at,
                finishedAt = at,
                seats = listOf(winner.id, loser.id),
                outcome = DuelOutcome(winner = 0, handsPlayed = 1, finalStacks = listOf(20_000, 0)),
            ),
        )
    }

    private suspend fun recordDraw(first: Player, second: Player, at: Instant) {
        PostgresDuelResultStore(dataSource).record(
            FinishedDuel(
                id = UUID.randomUUID(),
                format = formatLabel(DuelFormat.DEFAULT),
                startedAt = at,
                finishedAt = at,
                seats = listOf(first.id, second.id),
                // ADR-0015: a draw pays neither seat, so both coin deltas land at zero.
                outcome = DuelOutcome(winner = null, handsPlayed = 1, finalStacks = listOf(10_000, 10_000)),
            ),
        )
    }

    private data class LadderFixture(val fillerOne: Player, val fillerTwo: Player)

    /**
     * The three duels the story's standing fixture is built from — see the table in
     * `TASK-050602`. `host` and `guest` are the two devices [openSocketDuel] already seated;
     * `fillerOne` and `fillerTwo` exist only to give the ladder a shared rank, a skipped one, and
     * a duel that must not count.
     */
    private suspend fun seedTheLadderTheDuelArrivesInto(host: Player, guest: Player): LadderFixture {
        val fillerOne = resolvePlayer(FILLER_ONE_DEVICE)
        val fillerTwo = resolvePlayer(FILLER_TWO_DEVICE)

        recordWin(winner = host, loser = fillerOne, at = thisSeasonAt(1))
        recordDraw(first = guest, second = fillerTwo, at = thisSeasonAt(2))
        // Last season: must move nobody's standing this season, only appear in duel history.
        recordWin(winner = fillerOne, loser = fillerTwo, at = lastSeasonAt())

        return LadderFixture(fillerOne, fillerTwo)
    }

    private fun StandingsResponse.rowFor(player: Player): StandingRow =
        rows.singleOrNull { it.playerId == player.id.value }
            ?: fail("no row for player ${player.id.value} in rows=$rows")

    @Test
    fun theLadderTheDuelArrivesIntoSharesARankAndSkipsTheNext(): Unit = runBlocking {
        testApplication {
            installDuelServer(dataSource)
            val client = createClient { install(WebSockets) }
            client.openSocketDuel()

            val host = resolvePlayer(HOST_DEVICE)
            val guest = resolvePlayer(GUEST_DEVICE)
            val fixture = seedTheLadderTheDuelArrivesInto(host, guest)

            val standings = client.ladder(HOST_DEVICE, limit = LADDER_LIMIT)

            // The literal sequence, never re-derived from row position: a rank numbered from where
            // a row sits on the page would read 1, 2, 3, 4 and satisfy that derivation just as well.
            assertEquals(
                listOf(1, 2, 2, 4),
                standings.rows.map { it.rank },
                "ranks in page order should be 1, 2, 2, 4 -- a shared rank and a skipped one",
            )

            val hostRow = standings.rowFor(host)
            assertEquals(1, hostRow.rank, "host's rank")
            assertEquals(1, hostRow.coins, "host's coins")

            val guestRow = standings.rowFor(guest)
            assertEquals(2, guestRow.rank, "guest's rank")
            assertEquals(0, guestRow.coins, "guest's coins")

            val fillerTwoRow = standings.rowFor(fixture.fillerTwo)
            assertEquals(2, fillerTwoRow.rank, "fillerTwo's rank")
            assertEquals(0, fillerTwoRow.coins, "fillerTwo's coins")

            val fillerOneRow = standings.rowFor(fixture.fillerOne)
            assertEquals(4, fillerOneRow.rank, "fillerOne's rank")
            assertEquals(-1, fillerOneRow.coins, "fillerOne's coins")

            assertNull(standings.nextCursor, "nextCursor should be null: all four rows fit on one page of $LADDER_LIMIT")
        }
    }
}
