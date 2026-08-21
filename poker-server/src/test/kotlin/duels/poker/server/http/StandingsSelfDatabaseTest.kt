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
import io.ktor.client.request.header
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
import kotlin.test.assertTrue

private const val PAGE_LIMIT = 2

// Fixed, not Clock.systemUTC(): every duel's finishedAt below sits inside the one season this
// instant names, the same clock StandingsWalkDatabaseTest reads its own walk from.
private val CLOCK: Clock = Clock.fixed(Instant.parse("2026-08-20T09:00:00Z"), ZoneOffset.UTC)

/**
 * Drives `GET /api/standings`'s `self` field over real HTTP against a real PostgreSQL: the
 * caller's own standing is a whole-season aggregate, not an echo of the page just drawn
 * (`ADR-0065` §8).
 *
 * A single fixture whose requester happens to sit on the page returned cannot tell a real
 * whole-ladder lookup from a page-local one -- both answer the same number. Every test here names
 * its requester by `X-Device-Id`, and the fixture is walked with both a requester on the page
 * drawn and one three pages away from it, so an implementation that quietly searches the page
 * instead of the ladder is caught here rather than hidden by a fixture that never leaves the page.
 */
class StandingsSelfDatabaseTest {
    private lateinit var dataSource: DataSource
    private lateinit var playerA: Player
    private lateinit var playerB: Player
    private lateinit var playerC: Player
    private lateinit var playerD: Player
    private lateinit var playerE: Player

    @BeforeEach
    fun setupDatabase() {
        dataSource = PostgresTestSupport.freshDatabase()
        Migrations.migrate(dataSource)
        val playerDirectory = PostgresPlayerDirectory(dataSource)
        val duelResultStore = PostgresDuelResultStore(dataSource)

        runBlocking {
            // Creation order (e, c, a, d, b) is neither the ladder's order nor the recording
            // order below: a fixture that happened to arrive already sorted could not tell a
            // whole-ladder rank from a page-local one.
            playerE = playerDirectory.resolve(DeviceId("e"))
            playerC = playerDirectory.resolve(DeviceId("c"))
            playerA = playerDirectory.resolve(DeviceId("a"))
            playerD = playerDirectory.resolve(DeviceId("d"))
            playerB = playerDirectory.resolve(DeviceId("b"))

            // a +2, b +1, c 0, d -1, e -2 -- sums to zero, ranks 1, 2, 3, 4, 5, pages [a, b]
            // [c, d] [e] under PAGE_LIMIT.
            val base = Instant.parse("2026-08-10T10:00:00Z")
            duelResultStore.record(wonDuel(playerA, playerD, base.plusSeconds(60)))
            duelResultStore.record(wonDuel(playerA, playerE, base.plusSeconds(120)))
            duelResultStore.record(wonDuel(playerB, playerE, base.plusSeconds(180)))
            duelResultStore.record(wonDuel(playerC, playerD, base.plusSeconds(240)))
            duelResultStore.record(wonDuel(playerD, playerC, base.plusSeconds(300)))
        }
    }

    @Test
    fun theSelfStandingIsTheWholeLaddersForAPlayerOnALaterPage() {
        testApplication {
            application {
                module()
                standingsRoutes(PostgresProfileReads(dataSource), PostgresStandingsReads(dataSource), CLOCK)
            }

            val page = fetchPage(client, "e", PAGE_LIMIT, after = null)

            assertEquals(listOf(playerA.id.value, playerB.id.value), page.rows.map { it.playerId })
            assertTrue(page.rows.none { it.playerId == playerE.id.value }, "e must not be on page one")

            val self = requireNotNull(page.self) { "Expected a self standing: e has finished duels" }
            assertEquals(playerE.id.value, self.playerId)
            assertEquals(5, self.rank, "e's rank must be the whole ladder's, not a page that never holds e")
            assertEquals(-2, self.coins)
        }
    }

    @Test
    fun theSelfStandingEqualsTheRowWhenThePlayerIsOnThePageDrawn() {
        testApplication {
            application {
                module()
                standingsRoutes(PostgresProfileReads(dataSource), PostgresStandingsReads(dataSource), CLOCK)
            }

            val page = fetchPage(client, "a", PAGE_LIMIT, after = null)

            val self = requireNotNull(page.self) { "Expected a self standing: a has finished duels" }
            assertEquals(playerA.id.value, self.playerId)
            assertEquals(1, self.rank)
            assertEquals(2, self.coins)

            val ownRows = page.rows.filter { it.playerId == playerA.id.value }
            assertEquals(1, ownRows.size, "a's row must appear once -- present in self too is not a duplicate")
            assertEquals(self.rank, ownRows.single().rank)
            assertEquals(self.coins, ownRows.single().coins)
        }
    }

    @Test
    fun theSelfStandingIsIdenticalOnEveryPageOfOneWalk() {
        testApplication {
            application {
                module()
                standingsRoutes(PostgresProfileReads(dataSource), PostgresStandingsReads(dataSource), CLOCK)
            }

            val pageOne = fetchPage(client, "e", PAGE_LIMIT, after = null)
            val cursor =
                requireNotNull(pageOne.nextCursor) { "Expected page one of a five-player ladder to carry a cursor" }
            val pageTwo = fetchPage(client, "e", PAGE_LIMIT, after = cursor)

            assertEquals(
                pageOne.self,
                pageTwo.self,
                "self must come from the server's cutoff, not remembered by the client across the walk",
            )
            assertTrue(pageOne.rows.none { it.playerId == playerE.id.value })
            assertTrue(pageTwo.rows.none { it.playerId == playerE.id.value })
        }
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

    private suspend fun fetchPage(client: HttpClient, device: String, limit: Int, after: String?): StandingsResponse {
        val url = if (after == null) "/api/standings?limit=$limit" else "/api/standings?limit=$limit&after=$after"
        val response = client.get(url) { header(DEVICE_ID_HEADER, device) }

        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        return protocolJson.decodeFromString<StandingsResponse>(body)
    }
}
