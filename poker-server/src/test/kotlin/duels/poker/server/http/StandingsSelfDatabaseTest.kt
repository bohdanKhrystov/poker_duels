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
import kotlin.test.assertNotNull
import kotlin.test.assertNull
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
                standingsRoutes(PostgresProfileReads(dataSource), PostgresStandingsReads(dataSource), CLOCK, identitiesFor(dataSource))
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
                standingsRoutes(PostgresProfileReads(dataSource), PostgresStandingsReads(dataSource), CLOCK, identitiesFor(dataSource))
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
                standingsRoutes(PostgresProfileReads(dataSource), PostgresStandingsReads(dataSource), CLOCK, identitiesFor(dataSource))
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

    @Test
    fun aProfileWithNoDuelThisSeasonIsToldItHasNoPlaceAndIsNotGivenAZero() {
        val fixtureBDataSource = PostgresTestSupport.freshDatabase()
        Migrations.migrate(fixtureBDataSource)
        val fixture = setupFixtureB(fixtureBDataSource)

        testApplication {
            application {
                module()
                standingsRoutes(
                    PostgresProfileReads(fixtureBDataSource),
                    PostgresStandingsReads(fixtureBDataSource),
                    CLOCK,
                    identitiesFor(fixtureBDataSource),
                )
            }

            val absentPage = fetchPage(client, "absent", PAGE_LIMIT, after = null)
            val absentSelf =
                requireNotNull(absentPage.self) { "absent has a profile and must be told so, even with no place" }
            assertEquals(fixture.absent.id.value, absentSelf.playerId)
            assertNull(absentSelf.rank, "no duel this season must read as no rank, not a page-local miss")
            assertNull(absentSelf.coins, "no duel this season must not be reported as standing at zero")

            val drewPage = fetchPage(client, "drew", PAGE_LIMIT, after = null)
            val drewSelf = requireNotNull(drewPage.self) { "drew drew a duel this season and must have a place" }
            assertEquals(fixture.drew.id.value, drewSelf.playerId)
            assertNotNull(drewSelf.rank, "drew played this season and must be ranked, unlike absent")
            assertEquals(0, drewSelf.coins, "a draw stands at zero coins, ADR-0015 -- not absent from the ladder")
        }
    }

    @Test
    fun anUnknownDeviceGetsThePageAndNoSelfLine() {
        testApplication {
            application {
                module()
                standingsRoutes(PostgresProfileReads(dataSource), PostgresStandingsReads(dataSource), CLOCK, identitiesFor(dataSource))
            }

            val knownPage = fetchPage(client, "a", PAGE_LIMIT, after = null)

            val unknownDevicePage = fetchPage(client, "never-seen-before", PAGE_LIMIT, after = null)
            assertNull(unknownDevicePage.self, "an unknown device reads the ladder like anybody else")
            assertEquals(knownPage.rows, unknownDevicePage.rows, "the page must not depend on who is asking")

            val noHeaderPage = fetchPage(client, null, PAGE_LIMIT, after = null)
            assertNull(noHeaderPage.self, "no header at all must draw no self line either")
            assertEquals(knownPage.rows, noHeaderPage.rows, "the page must not depend on a header being present")
        }
    }

    @Test
    fun readingTheLadderCreatesNothing() {
        testApplication {
            application {
                module()
                standingsRoutes(PostgresProfileReads(dataSource), PostgresStandingsReads(dataSource), CLOCK, identitiesFor(dataSource))
            }

            val before = playerRowCount(dataSource)
            fetchPage(client, "never-seen-before", PAGE_LIMIT, after = null)
            val after = playerRowCount(dataSource)

            assertEquals(before, after, "reading the ladder must mint no row -- ADR-0012, profiles are not")
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

    private fun drawnDuel(playerX: Player, playerY: Player, finishedAt: Instant): FinishedDuel {
        return FinishedDuel(
            id = UUID.randomUUID(),
            format = formatLabel(DuelFormat.DEFAULT),
            startedAt = finishedAt.minusSeconds(60),
            finishedAt = finishedAt,
            seats = listOf(playerX.id, playerY.id),
            outcome = DuelOutcome(winner = null, handsPlayed = 1, finalStacks = listOf(10_000, 10_000)),
        )
    }

    /**
     * Fixture B: `drew` and `rival` draw a duel in August; `absent`'s only duel finished in July,
     * against `july` -- a season this fixture's [CLOCK] has already left. Built on its own fresh
     * database rather than the class fixture's, so its August ladder never gains the class
     * fixture's five players: "the August ladder holds drew and rival and nobody else" would
     * otherwise stop being true.
     */
    private fun setupFixtureB(dataSource: DataSource): FixtureB {
        val playerDirectory = PostgresPlayerDirectory(dataSource)
        val duelResultStore = PostgresDuelResultStore(dataSource)

        return runBlocking {
            val drew = playerDirectory.resolve(DeviceId("drew"))
            val rival = playerDirectory.resolve(DeviceId("rival"))
            val absent = playerDirectory.resolve(DeviceId("absent"))
            val july = playerDirectory.resolve(DeviceId("july"))

            duelResultStore.record(drawnDuel(drew, rival, Instant.parse("2026-08-05T10:00:00Z")))
            duelResultStore.record(wonDuel(absent, july, Instant.parse("2026-07-05T10:00:00Z")))

            FixtureB(drew, rival, absent)
        }
    }

    private data class FixtureB(val drew: Player, val rival: Player, val absent: Player)

    // A direct count is a fact about the player table itself, not about whatever a port chooses
    // to expose through it -- the same way ServerComponentsTest counts rows.
    private fun playerRowCount(dataSource: DataSource): Long {
        dataSource.connection.use { connection ->
            connection.createStatement().use { statement ->
                statement.executeQuery("SELECT count(*) FROM player").use { resultSet ->
                    resultSet.next()
                    return resultSet.getLong(1)
                }
            }
        }
    }

    private suspend fun fetchPage(client: HttpClient, device: String?, limit: Int, after: String?): StandingsResponse {
        val url = if (after == null) "/api/standings?limit=$limit" else "/api/standings?limit=$limit&after=$after"
        val response = client.get(url) { if (device != null) header(DEVICE_ID_HEADER, device) }

        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        return protocolJson.decodeFromString<StandingsResponse>(body)
    }
}
