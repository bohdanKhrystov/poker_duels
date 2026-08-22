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
import duels.poker.server.protocol.http.RecentDuelsResponse
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
import org.junit.jupiter.api.Assertions.assertNotNull
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

// Bounded like StandingsWalkDatabaseTest.walkAllPages: a cursor that never turns null is a server
// defect, and the walk reports that defect with a message instead of spinning on it forever.
private const val MAX_LADDER_WALK_REQUESTS = 10

/**
 * Measured, not derived: under the fixed `HAND_SEED` and `POLICY_SEED` that [openSocketDuel]
 * plays with -- the determinism [SocketDuelTest.theSameSeedsPlayTheSameDuel] proves holds on a
 * fresh schema, twice, frame count included -- the duel is won by seat 1, the `GUEST_DEVICE`
 * client, in 9 hands with final stacks `[0, 20000]`.
 *
 * The head start in [seedTheLadderTheDuelArrivesInto] has to belong to whoever turns out to lose,
 * chosen before the duel is played, which is why this seat is pinned rather than read off the
 * outcome. If the assertion pinning it below ever fails, these two seeds now play a different
 * duel: move the head start in [seedTheLadderTheDuelArrivesInto] to the other seat and update this
 * constant to match.
 */
private const val EXPECTED_WINNING_SEAT: Int = 1

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

    /**
     * Walks every page of `/api/standings` for [deviceId], following `nextCursor` from the first
     * page to the last and concatenating each page's `rows` into one list -- a real walk, not a
     * page wearing a loop, whenever [limit] is smaller than the number of rows on the ladder.
     *
     * Bounded by [MAX_LADDER_WALK_REQUESTS] and failing with a message naming the last cursor if
     * the walk does not terminate, exactly as [StandingsWalkDatabaseTest.walkAllPages] does. The
     * cursor is passed straight through from one page's `nextCursor` into the next request's
     * `after` -- never decoded and re-encoded here, which would test this walk's round trip
     * instead of the server's.
     */
    private suspend fun HttpClient.walkLadder(deviceId: String?, limit: Int): List<StandingRow> {
        val rows = mutableListOf<StandingRow>()
        var cursor: String? = null
        repeat(MAX_LADDER_WALK_REQUESTS) {
            val page = ladder(deviceId, limit = limit, after = cursor)
            rows.addAll(page.rows)
            cursor = page.nextCursor
            if (cursor == null) {
                return rows
            }
        }
        fail("Ladder walk did not terminate within $MAX_LADDER_WALK_REQUESTS requests; last cursor was $cursor")
    }

    /**
     * Reads [deviceId]'s recent duels from `GET /api/me/duels`, asserting the response is `200`,
     * then decodes the body with [protocolJson] directly -- copied from
     * [SocketHistoryTest.recentDuelsOf], the per-player history this story's per-player check reads.
     */
    private suspend fun HttpClient.recentDuelsOf(deviceId: String): RecentDuelsResponse {
        val response = get("/api/me/duels") { header(DEVICE_ID_HEADER, deviceId) }
        assertEquals(
            HttpStatusCode.OK,
            response.status,
            "GET /api/me/duels for deviceId=$deviceId returned ${response.status}",
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

    /**
     * The winning seat named by [outcome], failing loudly and reproducibly if there is none.
     *
     * Copied from `SocketCoinsTest`: a `checkNotNull` that names both seeds when the outcome has
     * no winner, so a fixture that somehow drew reports that rather than silently treating seat 0
     * as the winner.
     */
    private fun SocketDuel.winnerSeat(outcome: DuelOutcome): Int =
        checkNotNull(outcome.winner) {
            "handSeed=$handSeed policySeed=$POLICY_SEED: outcome has no winner, outcome=$outcome"
        }

    /**
     * Six wins inside [ladderSeason] that put both [host] and [guest] at `±3` before the story's
     * central duel is played: three for [host] over `fillerOne`, three for `fillerTwo` over
     * [guest]. Deliberately a different fixture from [seedTheLadderTheDuelArrivesInto]'s: that
     * one's post-duel numbers happen to include a `+1`, so a ladder that never sums history and
     * instead answers a fixed `+1`/`−1` per outcome could pass it by accident. Starting both
     * duellists away from zero turns every later assertion into an `after − before` difference,
     * wrong for both duellists whichever seat wins, and that is what this fixture is for.
     */
    private suspend fun seedADeepLadder(host: Player, guest: Player): LadderFixture {
        val fillerOne = resolvePlayer(FILLER_ONE_DEVICE)
        val fillerTwo = resolvePlayer(FILLER_TWO_DEVICE)

        recordWin(winner = host, loser = fillerOne, at = thisSeasonAt(1))
        recordWin(winner = host, loser = fillerOne, at = thisSeasonAt(2))
        recordWin(winner = host, loser = fillerOne, at = thisSeasonAt(3))
        recordWin(winner = fillerTwo, loser = guest, at = thisSeasonAt(4))
        recordWin(winner = fillerTwo, loser = guest, at = thisSeasonAt(5))
        recordWin(winner = fillerTwo, loser = guest, at = thisSeasonAt(6))

        return LadderFixture(fillerOne, fillerTwo)
    }

    @Test
    fun theLadderMovesTheWinnerUpOneAndTheLoserDownOne(): Unit = runBlocking {
        testApplication {
            installDuelServer(dataSource)
            val client = createClient { install(WebSockets) }
            val duel = client.openSocketDuel()
            val hostSeat = duel.clients.single { it.deviceId == HOST_DEVICE }.seat
            val guestSeat = duel.clients.single { it.deviceId == GUEST_DEVICE }.seat

            val host = resolvePlayer(HOST_DEVICE)
            val guest = resolvePlayer(GUEST_DEVICE)
            val fixture = seedADeepLadder(host, guest)
            val playerByDevice = mapOf(HOST_DEVICE to host, GUEST_DEVICE to guest)

            // Established before playToFinish() so the deltas asserted below are the duel's own,
            // not a number that was already sitting there -- the same bracketing SocketCoinsTest
            // uses, applied here to a ladder that starts away from zero instead of at it.
            val before = client.ladder(HOST_DEVICE, limit = LADDER_LIMIT)
            val beforeHost = before.rowFor(host).coins
            val beforeGuest = before.rowFor(guest).coins
            val beforeFillerOne = before.rowFor(fixture.fillerOne).coins
            val beforeFillerTwo = before.rowFor(fixture.fillerTwo).coins
            val beforeByDevice = mapOf(HOST_DEVICE to beforeHost, GUEST_DEVICE to beforeGuest)

            assertEquals(
                3,
                beforeHost,
                "handSeed=${duel.handSeed} policySeed=$POLICY_SEED: host seat=$hostSeat " +
                    "expected pre-duel coins 3, got $beforeHost",
            )
            assertEquals(
                -3,
                beforeGuest,
                "handSeed=${duel.handSeed} policySeed=$POLICY_SEED: guest seat=$guestSeat " +
                    "expected pre-duel coins -3, got $beforeGuest",
            )
            assertEquals(
                -3,
                beforeFillerOne,
                "handSeed=${duel.handSeed} policySeed=$POLICY_SEED: fillerOne expected pre-duel coins -3, " +
                    "got $beforeFillerOne",
            )
            assertEquals(
                3,
                beforeFillerTwo,
                "handSeed=${duel.handSeed} policySeed=$POLICY_SEED: fillerTwo expected pre-duel coins 3, " +
                    "got $beforeFillerTwo",
            )

            val outcome = duel.playToFinish()
            val winnerSeat = duel.winnerSeat(outcome)
            val loserSeat = 1 - winnerSeat
            val winner = duel.seat(winnerSeat)
            val loser = duel.seat(loserSeat)
            val winnerPlayer = playerByDevice.getValue(winner.deviceId)
            val loserPlayer = playerByDevice.getValue(loser.deviceId)
            val beforeWinner = beforeByDevice.getValue(winner.deviceId)
            val beforeLoser = beforeByDevice.getValue(loser.deviceId)

            val after = client.ladder(HOST_DEVICE, limit = LADDER_LIMIT)
            val afterWinner = after.rowFor(winnerPlayer).coins
            val afterLoser = after.rowFor(loserPlayer).coins
            val afterFillerOne = after.rowFor(fixture.fillerOne).coins
            val afterFillerTwo = after.rowFor(fixture.fillerTwo).coins

            assertEquals(
                1,
                afterWinner - beforeWinner,
                "handSeed=${duel.handSeed} policySeed=$POLICY_SEED: winner seat=$winnerSeat " +
                    "deviceId=${winner.deviceId} expected coins delta +1, before=$beforeWinner " +
                    "after=$afterWinner",
            )
            assertEquals(
                -1,
                afterLoser - beforeLoser,
                "handSeed=${duel.handSeed} policySeed=$POLICY_SEED: loser seat=$loserSeat " +
                    "deviceId=${loser.deviceId} expected coins delta -1, before=$beforeLoser " +
                    "after=$afterLoser",
            )
            assertEquals(
                beforeFillerOne,
                afterFillerOne,
                "handSeed=${duel.handSeed} policySeed=$POLICY_SEED: fillerOne's coins must be unchanged by a " +
                    "duel it did not play, before=$beforeFillerOne after=$afterFillerOne",
            )
            assertEquals(
                beforeFillerTwo,
                afterFillerTwo,
                "handSeed=${duel.handSeed} policySeed=$POLICY_SEED: fillerTwo's coins must be unchanged by a " +
                    "duel it did not play, before=$beforeFillerTwo after=$afterFillerTwo",
            )
        }
    }

    /**
     * The head start belongs to whoever turns out to lose, chosen before the duel is played --
     * see [EXPECTED_WINNING_SEAT] for why the seat has to be named rather than read off
     * [DuelOutcome.winner] after the fact.
     */
    @Test
    fun theDuelPutsTheWinnerAboveTheLoserOnALadderThatHadThemTheOtherWayRound(): Unit = runBlocking {
        testApplication {
            installDuelServer(dataSource)
            val client = createClient { install(WebSockets) }
            val duel = client.openSocketDuel()

            val host = resolvePlayer(HOST_DEVICE)
            val guest = resolvePlayer(GUEST_DEVICE)
            seedTheLadderTheDuelArrivesInto(host, guest)

            val before = client.ladder(HOST_DEVICE, limit = LADDER_LIMIT)
            val beforeHostRow = before.rowFor(host)
            val beforeGuestRow = before.rowFor(guest)
            val beforeHostIndex = before.rows.indexOf(beforeHostRow)
            val beforeGuestIndex = before.rows.indexOf(beforeGuestRow)

            assertEquals(1, beforeHostRow.rank, "before: host's rank")
            assertEquals(1, beforeHostRow.coins, "before: host's coins")
            assertEquals(2, beforeGuestRow.rank, "before: guest's rank")
            assertEquals(0, beforeGuestRow.coins, "before: guest's coins")
            assertEquals(
                true,
                beforeHostIndex < beforeGuestIndex,
                "before: host's row index ($beforeHostIndex) should be lower than guest's " +
                    "($beforeGuestIndex) -- the loser-to-be is ahead, rows=${before.rows}",
            )

            val outcome = duel.playToFinish()
            assertEquals(
                EXPECTED_WINNING_SEAT,
                outcome.winner,
                "handSeed=${duel.handSeed} policySeed=$POLICY_SEED: expected seat " +
                    "$EXPECTED_WINNING_SEAT to win, got outcome=$outcome -- these seeds now play a " +
                    "different duel than the one this ticket measured; move the head start in " +
                    "seedTheLadderTheDuelArrivesInto to the other seat and update " +
                    "EXPECTED_WINNING_SEAT to match",
            )

            val after = client.ladder(HOST_DEVICE, limit = LADDER_LIMIT)
            val afterGuestRow = after.rowFor(guest)
            val afterHostRow = after.rowFor(host)
            val afterGuestIndex = after.rows.indexOf(afterGuestRow)
            val afterHostIndex = after.rows.indexOf(afterHostRow)

            assertEquals(1, afterGuestRow.rank, "after: guest's rank")
            assertEquals(1, afterGuestRow.coins, "after: guest's coins")
            assertEquals(2, afterHostRow.rank, "after: host's rank")
            assertEquals(0, afterHostRow.coins, "after: host's coins")
            assertEquals(
                true,
                afterGuestIndex < afterHostIndex,
                "after: guest's row index ($afterGuestIndex) should be lower than host's " +
                    "($afterHostIndex) -- the winner overtook the loser, rows=${after.rows}",
            )
        }
    }

    /**
     * The story's conservation criterion end to end (`ADR-0063` §4): every row the ladder prints
     * is that player's own season results, and the season as a whole sums to zero. The order below
     * matters: the per-player check has teeth because a wrong window moves it, while the total
     * asserted last does not, because every duel's two `duel_result` rows already sum to zero
     * (`ADR-0015`) whatever window a query happens to read them through. Neither assertion
     * subsumes the other -- this one catches a wrong window, the total catches a row lost or
     * invented outright.
     */
    @Test
    fun everyStandingIsThatPlayersOwnSeasonResultsAndTheLadderTotalsZero(): Unit = runBlocking {
        testApplication {
            installDuelServer(dataSource)
            val client = createClient { install(WebSockets) }
            val duel = client.openSocketDuel()

            val host = resolvePlayer(HOST_DEVICE)
            val guest = resolvePlayer(GUEST_DEVICE)
            val fixture = seedTheLadderTheDuelArrivesInto(host, guest)
            val playersByDevice =
                listOf(
                    HOST_DEVICE to host,
                    GUEST_DEVICE to guest,
                    FILLER_ONE_DEVICE to fixture.fillerOne,
                    FILLER_TWO_DEVICE to fixture.fillerTwo,
                )

            duel.playToFinish()

            // limit = 2 makes this a real walk -- two pages and a cursor between them -- rather
            // than one page wearing a loop: the four fixture rows do not fit on a page of 2.
            val rows = client.walkLadder(HOST_DEVICE, limit = 2)

            // (1) Row count and identity, asserted before either total below: a page returned
            // instead of the full walk, or a row served twice for one player with its coins
            // otherwise unchanged, would slip past every per-player comparison that follows --
            // rows.size catches the duplicate that a set of ids alone would silently collapse away.
            assertEquals(
                4,
                rows.size,
                "handSeed=${duel.handSeed} policySeed=$POLICY_SEED: walk should return exactly " +
                    "four rows, got $rows",
            )
            assertEquals(
                playersByDevice.map { (_, player) -> player.id.value }.toSet(),
                rows.map { it.playerId }.toSet(),
                "handSeed=${duel.handSeed} policySeed=$POLICY_SEED: the four rows should be host, " +
                    "guest, fillerOne and fillerTwo, got $rows",
            )
            val rowsByPlayerId = rows.associateBy { it.playerId }

            // (2) Every standing is that player's own season results: the ladder's coins compared
            // against a sum independently computed from that player's GET /api/me/duels, filtered
            // to the season -- never the same value read twice.
            for ((deviceId, player) in playersByDevice) {
                val history = client.recentDuelsOf(deviceId)
                assertNull(
                    history.nextCursor,
                    "$deviceId's duel history must fit on one page or this sum would miss " +
                        "entries left unread, nextCursor=${history.nextCursor}",
                )
                val seasonSum =
                    history.duels
                        .filter { ladderSeason.contains(Instant.parse(it.finishedAt)) }
                        .sumOf { it.coinDelta }
                val row = rowsByPlayerId.getValue(player.id.value)
                assertEquals(
                    seasonSum,
                    row.coins,
                    "handSeed=${duel.handSeed} policySeed=$POLICY_SEED: $deviceId's ladder coins " +
                        "(${row.coins}) should equal the season sum of its own duel history ($seasonSum)",
                )
            }

            // (3) fillerOne is the player who makes the window observable: a win at lastSeasonAt()
            // leaves their all-time record at 0 while their season standing is -1 -- these two
            // numbers differing is exactly what a query reading the wrong window would get caught
            // by, and it is why the fixture carries this player at all.
            val fillerOneDuels = client.recentDuelsOf(FILLER_ONE_DEVICE).duels
            val fillerOneSeasonSum =
                fillerOneDuels
                    .filter { ladderSeason.contains(Instant.parse(it.finishedAt)) }
                    .sumOf { it.coinDelta }
            val fillerOneUnfilteredSum = fillerOneDuels.sumOf { it.coinDelta }
            assertEquals(-1, fillerOneSeasonSum, "fillerOne's season sum should be -1")
            assertEquals(0, fillerOneUnfilteredSum, "fillerOne's unfiltered, all-time sum should be 0")

            // (4) The ladder as a whole still sums to zero -- necessary, but unlike (2), not
            // sufficient: every duel writes two duel_result rows that sum to zero (ADR-0015 makes a
            // draw two zeroes rather than nothing), so this total would read 0 under this season,
            // last season, or no window at all. It would survive the exact wrong-window mutation
            // (2) exists to catch; what it catches instead is a row lost or invented outright.
            assertEquals(
                0,
                rows.sumOf { it.coins },
                "handSeed=${duel.handSeed} policySeed=$POLICY_SEED: the four coins should sum to " +
                    "exactly 0, got $rows",
            )
        }
    }

    /**
     * `limit = 1` with `FILLER_ONE_DEVICE`: the player at rank 4 who is not on a page showing
     * rank 1 still receives their own standing. `rows` holds exactly one row (rank 1),
     * `fillerOne`'s player id is not on it, `nextCursor` is non-null, and `self` reads that
     * player's own id with rank `4` and coins `−1` — a rank naming three players ahead on a
     * page showing one. This proves the self standing is computed from the whole ladder, not
     * just the page returned.
     *
     * `ADR-0065` §4, §8: the self standing is a separate query, and there is no `playerId`
     * parameter — the requester is named by the session alone.
     */
    @Test
    fun theSelfStandingIsTheWholeLaddersPlaceForAPlayerOnNoPageDrawn(): Unit = runBlocking {
        testApplication {
            installDuelServer(dataSource)
            val client = createClient { install(WebSockets) }
            val duel = client.openSocketDuel()

            val host = resolvePlayer(HOST_DEVICE)
            val guest = resolvePlayer(GUEST_DEVICE)
            val fixture = seedTheLadderTheDuelArrivesInto(host, guest)

            duel.playToFinish()

            // After the duel: guest is rank 1 (+1), host and fillerTwo are rank 2 (0), fillerOne is rank 4 (-1).
            // Request with limit=1 as fillerOne (rank 4, off the page).
            val standings = client.ladder(FILLER_ONE_DEVICE, limit = 1)

            // The page shows only one row: the rank 1 player (guest).
            assertEquals(
                1,
                standings.rows.size,
                "rows should have exactly one row",
            )
            assertEquals(
                guest.id.value,
                standings.rows[0].playerId,
                "the one row should be guest (rank 1)",
            )

            // There are more rows beyond this page.
            assertNotNull(
                standings.nextCursor,
                "nextCursor should not be null since fillerOne (rank 4) is not on this page",
            )

            // The self standing shows fillerOne's own rank and coins, not derived from the page.
            assertEquals(
                fixture.fillerOne.id.value,
                standings.self?.playerId,
                "self should be fillerOne",
            )
            assertEquals(
                4,
                standings.self?.rank,
                "self rank should be 4 (the literal rank of fillerOne on the whole ladder)",
            )
            assertEquals(
                -1,
                standings.self?.coins,
                "self coins should be -1 (the literal coin delta for fillerOne)",
            )
        }
    }

    /**
     * `limit = 1` with `GUEST_DEVICE`, the duel's winner now at rank 1: `rows` holds exactly one
     * row, it **is** `guest`'s row with rank `1` and coins `1`, and `self` reads the same id,
     * rank and coins. The row is present, not filtered out for being the requester's.
     *
     * This test shares a `RANKED_CTE` with the database's `standingOf` query in
     * `PostgresStandingsReads`, so "self equals my own row in the same response" is two paths
     * through **one piece of SQL**. The shared CTE could compute the rank wrongly and both sides
     * would fail equally — this test would not catch it alone. But paired with the first test's
     * off-page player, the two paths together assure the CTE is correct: a rank computed as
     * "rows on this page plus one" would answer 2 for rank-4 fillerOne and redden the first test.
     *
     * `ADR-0065` §4, §8: the self standing is a separate query, and there is no `playerId`
     * parameter — the requester is named by the session alone.
     */
    @Test
    fun theSelfStandingRepeatsThePlayersOwnRowWhenTheyAreOnThePage(): Unit = runBlocking {
        testApplication {
            installDuelServer(dataSource)
            val client = createClient { install(WebSockets) }
            val duel = client.openSocketDuel()

            val host = resolvePlayer(HOST_DEVICE)
            val guest = resolvePlayer(GUEST_DEVICE)
            seedTheLadderTheDuelArrivesInto(host, guest)

            duel.playToFinish()

            // After the duel: guest is rank 1 (+1), the winner.
            // Request with limit=1 as guest (rank 1, on the page).
            val standings = client.ladder(GUEST_DEVICE, limit = 1)

            // The page shows exactly one row: guest's row.
            assertEquals(
                1,
                standings.rows.size,
                "rows should have exactly one row",
            )
            val guestRow = standings.rows[0]
            assertEquals(
                guest.id.value,
                guestRow.playerId,
                "the one row should be guest",
            )

            // Guest's row on the page matches the literal values.
            assertEquals(
                1,
                guestRow.rank,
                "guest's rank should be 1 (the literal rank as the duel's winner)",
            )
            assertEquals(
                1,
                guestRow.coins,
                "guest's coins should be 1 (the literal delta: 0 before the duel, +1 for winning)",
            )

            // The self standing matches guest's row exactly.
            assertEquals(
                guest.id.value,
                standings.self?.playerId,
                "self should be guest",
            )
            assertEquals(
                1,
                standings.self?.rank,
                "self rank should be 1 (matching the row's rank)",
            )
            assertEquals(
                1,
                standings.self?.coins,
                "self coins should be 1 (matching the row's coins)",
            )

            // Verify the self standing is consistent with the row, and both are anchored to literals.
            assertEquals(
                guestRow.rank,
                standings.self?.rank,
                "self rank should equal the row's rank",
            )
            assertEquals(
                guestRow.coins,
                standings.self?.coins,
                "self coins should equal the row's coins",
            )
        }
    }
}
