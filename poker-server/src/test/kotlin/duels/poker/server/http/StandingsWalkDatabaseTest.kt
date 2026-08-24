package duels.poker.server.http

import duels.poker.engine.duel.DuelFormat
import duels.poker.engine.duel.DuelOutcome
import duels.poker.server.db.Migrations
import duels.poker.server.db.PostgresDuelResultStore
import duels.poker.server.db.PostgresPlayerDirectory
import duels.poker.server.db.PostgresProfileReads
import duels.poker.server.db.PostgresStandingsReads
import duels.poker.server.db.PostgresTestSupport
import duels.poker.server.duel.FinishedDuel
import duels.poker.server.duel.formatLabel
import duels.poker.server.module
import duels.poker.server.protocol.http.StandingsResponse
import duels.poker.server.protocol.protocolJson
import duels.poker.server.session.DeviceId
import duels.poker.server.session.Player
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID
import javax.sql.DataSource
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.fail

private const val PAGE_LIMIT = 2
private const val LADDER_SIZE = 7
private const val MAX_PAGE_WALK_REQUESTS = 10

// Fixed, not Clock.systemUTC(): the walk's asOf is minted once from this clock (ADR-0066 §2) and
// every duel's finishedAt below is chosen to sit inside the one season this instant names.
private val WALK_CLOCK: Clock = Clock.fixed(Instant.parse("2026-08-20T09:00:00Z"), ZoneOffset.UTC)

/**
 * Drives the paged `/api/standings` endpoint over real HTTP against a real PostgreSQL: every
 * player of the season comes back exactly once across the walk, and the rank on every row is the
 * *whole* ladder's competition rank (`ADR-0064` §2 — ties share, the next distinct standing skips)
 * rather than a count of the rows on the page in front of it.
 *
 * The fixture below never changes while it is read: nothing here finishes a duel mid-walk. That is
 * what makes exactly-once provable at all. `ADR-0066` §4 does not extend `STORY-0408`'s *total and
 * disjoint* guarantee to a ladder that moves between requests, and these first two tests don't
 * claim that. `TASK-050214`'s tests below do: their fixture finishes one duel between two requests
 * of the same walk, at the exact instant the walk's cutoff names, and prove the walk stays pinned
 * to the ladder as it stood when the walk began. The complementary case — a duel finishing
 * strictly *before* that cutoff — is `TASK-050215`'s fixture, not this one's.
 */
class StandingsWalkDatabaseTest {
    private lateinit var dataSource: DataSource
    private lateinit var playerA: Player
    private lateinit var playerB: Player
    private lateinit var playerC: Player
    private lateinit var playerD: Player
    private lateinit var playerE: Player
    private lateinit var playerT1: Player
    private lateinit var playerT2: Player

    @BeforeEach
    fun setupDatabase() {
        dataSource = PostgresTestSupport.freshDatabase()
        Migrations.migrate(dataSource)
        val playerDirectory = PostgresPlayerDirectory(dataSource)
        val duelResultStore = PostgresDuelResultStore(dataSource)

        runBlocking {
            // Creation order (d, b, a, e, t2, c, t1) is not the ladder's order, and the recording
            // order below is not either: a fixture that happened to arrive already sorted could
            // not fail a mutation to the ordering (ORDER BY coins DESC, player_id DESC).
            playerD = playerDirectory.resolve(DeviceId("d"))
            playerB = playerDirectory.resolve(DeviceId("b"))
            playerA = playerDirectory.resolve(DeviceId("a"))
            playerE = playerDirectory.resolve(DeviceId("e"))
            playerT2 = playerDirectory.resolve(DeviceId("t2"))
            playerC = playerDirectory.resolve(DeviceId("c"))
            playerT1 = playerDirectory.resolve(DeviceId("t1"))

            // a +2, t1 +1, t2 +1, b 0, e 0, c -2, d -2 -- sums to zero because every duel does,
            // and ranks (competition ranking) come out 1, 2, 2, 4, 4, 6, 6.
            val base = Instant.parse("2026-08-10T10:00:00Z")
            duelResultStore.record(drawnDuel(playerB, playerE, base.plusSeconds(60)))
            duelResultStore.record(wonDuel(playerA, playerC, base.plusSeconds(120)))
            duelResultStore.record(wonDuel(playerA, playerC, base.plusSeconds(180)))
            duelResultStore.record(wonDuel(playerT1, playerD, base.plusSeconds(240)))
            duelResultStore.record(wonDuel(playerT2, playerD, base.plusSeconds(300)))
        }
    }

    @Test
    fun everyPlayerOfTheLadderComesBackExactlyOnceOverTheWalk() {
        testApplication {
            application {
                module()
                standingsRoutes(PostgresProfileReads(dataSource), PostgresStandingsReads(dataSource), WALK_CLOCK, identitiesFor(dataSource))
            }

            val pages = walkAllPages(client, PAGE_LIMIT)
            val returnedIds = pages.flatMap { page -> page.rows.map { it.playerId } }
            val expectedIds =
                listOf(playerA, playerB, playerC, playerD, playerE, playerT1, playerT2)
                    .map { it.id.value }
            val occurrences = returnedIds.groupingBy { it }.eachCount()

            // Named individually, never folded into one toSet().size check: a player who repeats
            // and a player who disappears can cancel each other out in a bare size comparison.
            val missing = expectedIds.filter { (occurrences[it] ?: 0) == 0 }
            val repeated = expectedIds.filter { (occurrences[it] ?: 0) > 1 }

            assertEquals(listOf(2, 2, 2, 1), pages.map { it.rows.size })
            assertNull(pages.last().nextCursor)
            assertTrue(missing.isEmpty(), "players missing from the walk: $missing")
            assertTrue(repeated.isEmpty(), "players repeated across the walk: $repeated")
            assertEquals(LADDER_SIZE, returnedIds.size)
        }
    }

    @Test
    fun theRanksAWalkReturnsAreTheWholeLaddersAndNeverDecrease() {
        testApplication {
            application {
                module()
                standingsRoutes(PostgresProfileReads(dataSource), PostgresStandingsReads(dataSource), WALK_CLOCK, identitiesFor(dataSource))
            }

            val pages = walkAllPages(client, PAGE_LIMIT)
            val ranks = pages.flatMap { page -> page.rows.map { it.rank } }

            // The literal sequence, not something re-derived from row position -- a rank numbered
            // from where a row sits on its own page would satisfy that derivation just as well.
            assertEquals(listOf(1, 2, 2, 4, 4, 6, 6), ranks)
            assertEquals(pages[0].rows.last().rank, pages[1].rows.first().rank)

            var highestSoFar = Int.MIN_VALUE
            for ((index, rank) in ranks.withIndex()) {
                assertTrue(rank >= highestSoFar, "rank $rank at position $index is below an earlier rank in $ranks")
                highestSoFar = rank
            }
        }
    }

    /**
     * `ADR-0066` §4 and §9: a duel that finishes exactly at the walk's cutoff must be in no page of
     * it, and the fixture proves that only because its winner and loser sit on opposite sides of
     * the cursor page one leaves behind — a duel between two players already served, or two players
     * neither yet served, could not fail this even under the mutated window (`<=` instead of `<`).
     */
    @Test
    fun aDuelStampedAtTheCutoffIsInNoPageOfTheWalk() {
        val fixture = setupMidWalkFixture()

        testApplication {
            application {
                module()
                standingsRoutes(
                    PostgresProfileReads(fixture.dataSource),
                    PostgresStandingsReads(fixture.dataSource),
                    WALK_CLOCK,
                    identitiesFor(fixture.dataSource),
                )
            }

            val pageOne = fetchPage(client, PAGE_LIMIT, null)
            // Recorded only now, after page one's cursor already names x -- not folded into the
            // fixture above, and not recorded before this line either (TASK-050214's third
            // falsification checks exactly that timing). Stamped at WALK_CLOCK.instant() itself,
            // the same instant the cursorless request above just minted as asOf, so the half-open
            // window [season.start, asOf) excludes it from every remaining page of this walk.
            fixture.duelResultStore.record(wonDuel(fixture.w, fixture.x, WALK_CLOCK.instant()))
            val pages = listOf(pageOne) + walkAllPages(client, PAGE_LIMIT, pageOne.nextCursor)

            val rows = pages.flatMap { it.rows }
            val expectedIds = listOf(fixture.a, fixture.x, fixture.w, fixture.e, fixture.d).map { it.id.value }
            val occurrences = rows.map { it.playerId }.groupingBy { it }.eachCount()

            // Named individually, as the seven-player walk's test above does: a player who repeats
            // and a player who disappears can cancel each other out in a bare size comparison.
            val missing = expectedIds.filter { (occurrences[it] ?: 0) == 0 }
            val repeated = expectedIds.filter { (occurrences[it] ?: 0) > 1 }
            assertTrue(missing.isEmpty(), "players missing from the walk: $missing")
            assertTrue(repeated.isEmpty(), "players repeated across the walk: $repeated")

            val xRow = rows.single { it.playerId == fixture.x.id.value }
            val wRow = rows.single { it.playerId == fixture.w.id.value }
            assertEquals(1, xRow.coins, "x's row must read the standing it held at the cutoff, not after losing the mid-walk duel")
            assertEquals(0, wRow.coins, "w's row must read the standing it held at the cutoff, not after winning the mid-walk duel")

            // The direct check the two lookups above already imply, spelled out on its own because
            // it is the assertion the ticket's window mutation is aimed at: nowhere in the walk may
            // either player be seen holding the coin total the excluded duel would have given them.
            assertTrue(rows.none { it.playerId == fixture.w.id.value && it.coins == 1 }, "no row may carry w at the post-duel +1")
            assertTrue(rows.none { it.playerId == fixture.x.id.value && it.coins == 0 }, "no row may carry x at the post-duel 0")
        }
    }

    /**
     * The rank companion to [aDuelStampedAtTheCutoffIsInNoPageOfTheWalk]: `w`'s rank must stay the
     * one the whole ladder gave it before the mid-walk duel, never the one a win would have earned.
     */
    @Test
    fun theRanksALaterPageCarriesAreTheCutoffsRanks() {
        val fixture = setupMidWalkFixture()

        testApplication {
            application {
                module()
                standingsRoutes(
                    PostgresProfileReads(fixture.dataSource),
                    PostgresStandingsReads(fixture.dataSource),
                    WALK_CLOCK,
                    identitiesFor(fixture.dataSource),
                )
            }

            val pageOne = fetchPage(client, PAGE_LIMIT, null)
            fixture.duelResultStore.record(wonDuel(fixture.w, fixture.x, WALK_CLOCK.instant()))
            val pages = listOf(pageOne) + walkAllPages(client, PAGE_LIMIT, pageOne.nextCursor)

            val ranks = pages.flatMap { page -> page.rows.map { it.rank } }
            // The literal sequence, as the seven-player walk's rank test asserts -- a rank
            // re-derived from row position would pass just as well on a page the mid-walk duel
            // had actually reshuffled.
            assertEquals(listOf(1, 2, 3, 4, 5), ranks)

            val wRank = pages.flatMap { it.rows }.single { it.playerId == fixture.w.id.value }.rank
            assertEquals(3, wRank, "w must keep the rank it held before the mid-walk duel")

            var highestSoFar = Int.MIN_VALUE
            for ((index, rank) in ranks.withIndex()) {
                assertTrue(rank >= highestSoFar, "rank $rank at position $index is below an earlier rank in $ranks")
                highestSoFar = rank
            }
        }
    }

    /**
     * `ADR-0066` §4's second refusal, pinned rather than assumed away: `PostgresDuelResultSink`
     * stamps `finished_at` before its recording transaction commits, so a duel can commit after a
     * page was drawn while carrying a `finished_at` that lands inside the window every later page
     * still recomputes. `w`'s win lifts it above the cursor page one left behind, into territory a
     * forward-only walk never revisits, so `w` is returned zero times; `x`'s loss pushes it back
     * below that same cursor, into territory the walk still has to cross, so `x` is returned
     * twice. `ADR-0066` §9 asks for this test in as many words, so the anomaly is known rather
     * than discovered.
     */
    @Test
    fun aDuelCommittedAfterAPageWasDrawnReturnsItsLoserTwiceAndItsWinnerNever() {
        val fixture = setupMidWalkFixture()

        testApplication {
            application {
                module()
                standingsRoutes(
                    PostgresProfileReads(fixture.dataSource),
                    PostgresStandingsReads(fixture.dataSource),
                    WALK_CLOCK,
                    identitiesFor(fixture.dataSource),
                )
            }

            val pageOne = fetchPage(client, PAGE_LIMIT, null)
            // One minute inside [season.start, asOf), unlike the sibling fixture above which
            // stamps the same duel exactly at the cutoff and is excluded by it: this stamp lands
            // inside the window every later page recomputes, even though page one -- and its
            // cursor -- were already drawn from the ladder as it stood before this duel existed.
            fixture.duelResultStore.record(wonDuel(fixture.w, fixture.x, WALK_CLOCK.instant().minusSeconds(60)))
            val pages = listOf(pageOne) + walkAllPages(client, PAGE_LIMIT, pageOne.nextCursor)

            val rows = pages.flatMap { it.rows }
            val occurrences = rows.map { it.playerId }.groupingBy { it }.eachCount()

            // Named individually and by exact count, never folded into a set-size check, as the
            // seven-player walk's test above does: a repeat and a miss can cancel each other out.
            assertEquals(2, occurrences[fixture.x.id.value] ?: 0, "x (the mid-walk duel's loser) must be returned exactly twice")
            assertEquals(0, occurrences[fixture.w.id.value] ?: 0, "w (the mid-walk duel's winner) must be returned zero times")
            for (untouched in listOf(fixture.a, fixture.e, fixture.d)) {
                assertEquals(
                    1,
                    occurrences[untouched.id.value] ?: 0,
                    "${untouched.id.value} takes no part in the mid-walk duel and must appear exactly once",
                )
            }

            // The two appearances of x are not two identical rows: one is the standing it held
            // when page one was drawn, before the duel existed, and the other is what a later
            // page's recompute finds once the duel has committed.
            val xCoins = rows.filter { it.playerId == fixture.x.id.value }.map { it.coins }.sorted()
            assertEquals(listOf(0, 1), xCoins, "x's two rows must be the standings it held either side of the mid-walk duel")
        }
    }

    /**
     * `ADR-0066` §9's next criterion, immediately after the one above: with no refresh, sweep,
     * job or wait -- nothing but a plain request with no cursor -- the very next walk over the
     * same clock reads the ladder the duel actually left behind. This is the criterion a
     * materialised or periodically-refreshed ladder fails; `ADR-0066` §1's "no cache, no refresh
     * job" is what makes it true here.
     */
    @Test
    fun aNewWalkSeesTheDuelTheOldWalkCouldNot() {
        val fixture = setupMidWalkFixture()

        testApplication {
            application {
                module()
                standingsRoutes(
                    PostgresProfileReads(fixture.dataSource),
                    PostgresStandingsReads(fixture.dataSource),
                    WALK_CLOCK,
                    identitiesFor(fixture.dataSource),
                )
            }

            val pageOne = fetchPage(client, PAGE_LIMIT, null)
            fixture.duelResultStore.record(wonDuel(fixture.w, fixture.x, WALK_CLOCK.instant().minusSeconds(60)))
            // The old walk, finished exactly as the test above finishes it -- this is the walk
            // that could not see w, and the one this test's name compares against.
            walkAllPages(client, PAGE_LIMIT, pageOne.nextCursor)

            // The new walk: no cursor, no refresh, sweep, job or wait -- just the next request.
            val freshPages = walkAllPages(client, PAGE_LIMIT)
            val freshRows = freshPages.flatMap { it.rows }
            val expectedIds = listOf(fixture.a, fixture.w, fixture.x, fixture.e, fixture.d).map { it.id.value }
            val occurrences = freshRows.map { it.playerId }.groupingBy { it }.eachCount()

            val missing = expectedIds.filter { (occurrences[it] ?: 0) == 0 }
            val repeated = expectedIds.filter { (occurrences[it] ?: 0) > 1 }
            assertTrue(missing.isEmpty(), "players missing from the new walk: $missing")
            assertTrue(repeated.isEmpty(), "players repeated in the new walk: $repeated")

            val wRow = freshRows.single { it.playerId == fixture.w.id.value }
            val xRow = freshRows.single { it.playerId == fixture.x.id.value }
            assertEquals(1, wRow.coins, "w must read the standing the duel actually left it at")
            assertEquals(0, xRow.coins, "x must read the standing the duel actually left it at")
        }
    }

    private fun drawnDuel(firstPlayer: Player, secondPlayer: Player, finishedAt: Instant): FinishedDuel {
        return FinishedDuel(
            id = UUID.randomUUID(),
            format = formatLabel(DuelFormat.DEFAULT),
            startedAt = finishedAt.minusSeconds(60),
            finishedAt = finishedAt,
            seats = listOf(firstPlayer.id, secondPlayer.id),
            // ADR-0015: a draw pays neither seat, so both coin deltas land at zero rather than an
            // arbitrary seat being picked to break the tie.
            outcome = DuelOutcome(winner = null, handsPlayed = 1, finalStacks = listOf(10_000, 10_000)),
        )
    }

    private fun wonDuel(winner: Player, loser: Player, finishedAt: Instant): FinishedDuel {
        return FinishedDuel(
            id = UUID.randomUUID(),
            format = formatLabel(DuelFormat.DEFAULT),
            startedAt = finishedAt.minusSeconds(60),
            finishedAt = finishedAt,
            seats = listOf(winner.id, loser.id),
            outcome = DuelOutcome(winner = 0, handsPlayed = 1, finalStacks = listOf(11_000, 9_000)),
        )
    }

    /**
     * Fixture B for the two mid-walk tests above: five players with distinct standings, on a
     * database of their own rather than [dataSource] -- sharing [dataSource] would fold the
     * seven-player ladder's rows into this walk too, and the three pages `ADR-0066` §9's fixture
     * documents (`[a, x] [w, e] [d]`) would stop being the pages the walk actually returns.
     */
    private data class MidWalkFixture(
        val dataSource: DataSource,
        val duelResultStore: PostgresDuelResultStore,
        val w: Player,
        val x: Player,
        val a: Player,
        val e: Player,
        val d: Player,
    )

    private fun setupMidWalkFixture(): MidWalkFixture {
        val fixtureDataSource = PostgresTestSupport.freshDatabase()
        Migrations.migrate(fixtureDataSource)
        val playerDirectory = PostgresPlayerDirectory(fixtureDataSource)
        val duelResultStore = PostgresDuelResultStore(fixtureDataSource)

        return runBlocking {
            val minted =
                listOf("mw-1", "mw-2", "mw-3", "mw-4", "mw-5")
                    .map { playerDirectory.resolve(DeviceId(it)) }

            // player_id DESC is STANDINGS_ORDER's tiebreaker, and only that order decides which
            // side of the cursor a given id lands on -- never UUID.compareTo, which compares two
            // signed longs and disagrees with PostgreSQL's byte order over the canonical
            // lower-case text.
            val byId = minted.sortedByDescending { it.id.value }
            val w = byId[0]
            val x = byId[1]
            val a = byId[2]
            val e = byId[3]
            val d = byId[4]

            // a +2, x +1, w 0, e -1, d -2 -- sums to zero, and every finishedAt below sits on
            // 2026-08-10, inside the one season WALK_CLOCK's fixed instant names.
            val base = Instant.parse("2026-08-10T10:00:00Z")
            duelResultStore.record(wonDuel(a, e, base.plusSeconds(60)))
            duelResultStore.record(wonDuel(a, d, base.plusSeconds(120)))
            duelResultStore.record(wonDuel(x, e, base.plusSeconds(180)))
            duelResultStore.record(wonDuel(w, d, base.plusSeconds(240)))
            duelResultStore.record(wonDuel(e, w, base.plusSeconds(300)))

            MidWalkFixture(fixtureDataSource, duelResultStore, w, x, a, e, d)
        }
    }

    private suspend fun fetchPage(client: HttpClient, limit: Int, after: String?): StandingsResponse {
        val url = if (after == null) "/api/standings?limit=$limit" else "/api/standings?limit=$limit&after=$after"
        val response = client.get(url)

        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        return protocolJson.decodeFromString<StandingsResponse>(body)
    }

    // Bounded like DuelHistoryPagingDatabaseTest.walkAllPages: a cursor that never turns null is a
    // server defect, and this loop reports that defect with a message instead of spinning on it.
    // The cursor is passed straight through -- never decoded and re-encoded here, which would test
    // this test's round trip instead of the server's.
    //
    // startCursor defaults to null for the seven-player walk's two callers above, which start a
    // walk from its first page; the mid-walk tests pass page one's own cursor instead, so this
    // same loop can walk the rest of a walk whose first page they already fetched themselves.
    private suspend fun walkAllPages(client: HttpClient, limit: Int, startCursor: String? = null): List<StandingsResponse> {
        val pages = mutableListOf<StandingsResponse>()
        var cursor: String? = startCursor
        repeat(MAX_PAGE_WALK_REQUESTS) {
            val page = fetchPage(client, limit, cursor)
            pages.add(page)
            cursor = page.nextCursor
            if (cursor == null) {
                return pages
            }
        }
        fail("Standings page walk did not terminate within $MAX_PAGE_WALK_REQUESTS requests; last cursor was $cursor")
    }
}
