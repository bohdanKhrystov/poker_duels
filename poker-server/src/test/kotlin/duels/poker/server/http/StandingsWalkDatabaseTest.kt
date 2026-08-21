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
 * disjoint* guarantee to a ladder that moves between requests, and no test in this file claims
 * that — a moving ladder is `TASK-050214` and `TASK-050215`'s concern, not this one's.
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
                standingsRoutes(PostgresProfileReads(dataSource), PostgresStandingsReads(dataSource), WALK_CLOCK)
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
                standingsRoutes(PostgresProfileReads(dataSource), PostgresStandingsReads(dataSource), WALK_CLOCK)
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
    private suspend fun walkAllPages(client: HttpClient, limit: Int): List<StandingsResponse> {
        val pages = mutableListOf<StandingsResponse>()
        var cursor: String? = null
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
