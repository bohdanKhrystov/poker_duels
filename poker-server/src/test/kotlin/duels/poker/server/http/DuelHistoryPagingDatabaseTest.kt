package duels.poker.server.http

import duels.poker.engine.duel.DuelFormat
import duels.poker.engine.duel.DuelOutcome
import duels.poker.server.db.Migrations
import duels.poker.server.db.PostgresDuelResultStore
import duels.poker.server.db.PostgresPlayerDirectory
import duels.poker.server.db.PostgresProfileReads
import duels.poker.server.db.PostgresProfileWrites
import duels.poker.server.db.PostgresTestSupport
import duels.poker.server.duel.FinishedDuel
import duels.poker.server.duel.formatLabel
import duels.poker.server.module
import duels.poker.server.protocol.http.RecentDuelsResponse
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
import java.time.Instant
import java.util.UUID
import javax.sql.DataSource
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.fail

private const val PAGE_LIMIT = 3
private const val ALL_DUELS_LIMIT = 50
private const val MAX_PAGE_WALK_REQUESTS = 10
private const val ALICE_DUEL_COUNT = 7

/**
 * Drives the paged `/api/me/duels` endpoint over real HTTP against a real PostgreSQL: every duel
 * comes back exactly once across pages, the cursor the server issues is the cursor it accepts
 * back, and a cursor replayed by a second player never reads the first player's rows.
 */
class DuelHistoryPagingDatabaseTest {
    private lateinit var dataSource: DataSource
    private lateinit var duelResultStore: PostgresDuelResultStore
    private lateinit var alice: Player
    private lateinit var bob: Player
    private lateinit var carol: Player
    private lateinit var dave: Player
    private lateinit var aliceDuelIds: List<String>
    private lateinit var carolDuelId: String

    @BeforeEach
    fun setupDatabase() {
        dataSource = PostgresTestSupport.freshDatabase()
        Migrations.migrate(dataSource)
        val playerDirectory = PostgresPlayerDirectory(dataSource)
        duelResultStore = PostgresDuelResultStore(dataSource)

        runBlocking {
            alice = playerDirectory.resolve(DeviceId("alice"))
            bob = playerDirectory.resolve(DeviceId("bob"))
            carol = playerDirectory.resolve(DeviceId("carol"))
            dave = playerDirectory.resolve(DeviceId("dave"))

            // Alice's seven duels land at 10:01 through 10:07. Carol's single duel lands at
            // 10:00:30 -- older than every cursor alice's page walk can issue -- so replaying
            // alice's cursor as carol reads a row of carol's own rather than trivially nothing.
            val base = Instant.parse("2024-01-01T10:00:00Z")
            aliceDuelIds =
                (1..ALICE_DUEL_COUNT).map { minute ->
                    val duel = finishedDuel(alice, bob, base.plusSeconds(minute * 60L))
                    duelResultStore.record(duel)
                    duel.id.toString()
                }

            val carolDuel = finishedDuel(carol, dave, base.plusSeconds(30))
            duelResultStore.record(carolDuel)
            carolDuelId = carolDuel.id.toString()
        }
    }

    @Test
    fun everyDuelComesBackExactlyOnceOverThePagedEndpoint() {
        testApplication {
            application {
                module()
                profileRoutes(PostgresProfileReads(dataSource), PostgresProfileWrites(dataSource))
            }

            val pages = walkAllPages(client, "alice", PAGE_LIMIT)
            val singleRequest = fetchPage(client, "alice", ALL_DUELS_LIMIT, after = null)

            val flattenedIds = pages.flatMap { page -> page.duels.map { it.duelId } }
            val expectedIds = singleRequest.duels.map { it.duelId }

            assertEquals(listOf(3, 3, 1), pages.map { it.duels.size })
            assertEquals(expectedIds, flattenedIds)
            assertEquals(ALICE_DUEL_COUNT, flattenedIds.toSet().size)
            assertNull(pages.last().nextCursor)
        }
    }

    @Test
    fun theCursorTheServerIssuedIsTheCursorItAccepts() {
        testApplication {
            application {
                module()
                profileRoutes(PostgresProfileReads(dataSource), PostgresProfileWrites(dataSource))
            }

            val firstPage = fetchPage(client, "alice", PAGE_LIMIT, after = null)
            val issuedCursor =
                requireNotNull(firstPage.nextCursor) { "Expected alice's first page to carry a next cursor" }

            // Handed back exactly as received -- no decode-and-re-encode of our own, which would
            // test our round trip rather than the server's.
            val secondPage = fetchPage(client, "alice", PAGE_LIMIT, after = issuedCursor)

            assertEquals(3, secondPage.duels.size)
        }
    }

    @Test
    fun aCursorIssuedToOnePlayerReadsNoneOfThatPlayersDuels() {
        testApplication {
            application {
                module()
                profileRoutes(PostgresProfileReads(dataSource), PostgresProfileWrites(dataSource))
            }

            val alicesFirstPage = fetchPage(client, "alice", PAGE_LIMIT, after = null)
            val alicesCursor =
                requireNotNull(alicesFirstPage.nextCursor) { "Expected alice's first page to carry a next cursor" }

            // Carol has an older duel of her own in this cursor's range, so a non-empty page
            // actually exercises the isolation rather than passing by returning nothing.
            val carolsPage = fetchPage(client, "carol", ALL_DUELS_LIMIT, after = alicesCursor)
            val carolsDuelIds = carolsPage.duels.map { it.duelId }

            assertEquals(listOf(carolDuelId), carolsDuelIds)
            assertTrue(aliceDuelIds.none { it in carolsDuelIds })
        }
    }

    private fun finishedDuel(firstPlayer: Player, secondPlayer: Player, finishedAt: Instant): FinishedDuel {
        return FinishedDuel(
            id = UUID.randomUUID(),
            format = formatLabel(DuelFormat.DEFAULT),
            startedAt = finishedAt.minusSeconds(60),
            finishedAt = finishedAt,
            seats = listOf(firstPlayer.id, secondPlayer.id),
            outcome = DuelOutcome(winner = 0, handsPlayed = 1, finalStacks = listOf(11_000, 9_000)),
        )
    }

    private suspend fun fetchPage(
        client: HttpClient,
        device: String,
        limit: Int,
        after: String?,
    ): RecentDuelsResponse {
        val url = if (after == null) "/api/me/duels?limit=$limit" else "/api/me/duels?limit=$limit&after=$after"
        val response = client.get(url) { header(DEVICE_ID_HEADER, device) }

        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        return protocolJson.decodeFromString<RecentDuelsResponse>(body)
    }

    private suspend fun walkAllPages(client: HttpClient, device: String, limit: Int): List<RecentDuelsResponse> {
        val pages = mutableListOf<RecentDuelsResponse>()
        var cursor: String? = null
        repeat(MAX_PAGE_WALK_REQUESTS) {
            val page = fetchPage(client, device, limit, cursor)
            pages.add(page)
            cursor = page.nextCursor
            if (cursor == null) {
                return pages
            }
        }
        fail(
            "Page walk for device '$device' did not terminate within $MAX_PAGE_WALK_REQUESTS requests; " +
                "last cursor was $cursor",
        )
    }
}
