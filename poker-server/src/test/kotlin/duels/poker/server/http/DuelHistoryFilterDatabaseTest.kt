package duels.poker.server.http

import duels.poker.engine.duel.DuelFormat
import duels.poker.engine.duel.DuelOutcome
import duels.poker.engine.duel.EndCondition
import duels.poker.server.db.Migrations
import duels.poker.server.db.PostgresDuelResultStore
import duels.poker.server.db.PostgresPlayerDirectory
import duels.poker.server.db.PostgresProfileReads
import duels.poker.server.db.PostgresProfileWrites
import duels.poker.server.db.PostgresTestSupport
import duels.poker.server.duel.FinishedDuel
import duels.poker.server.duel.formatLabel
import duels.poker.server.module
import duels.poker.server.protocol.http.DuelOutcomeLabel
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

private const val ALL_DUELS_LIMIT = 50

/**
 * Drives `?outcome=` and `?opponent=` over real HTTP against a real PostgreSQL: proves the query
 * string reaches the SQL and the page it answers is exactly the rows the filter names, in order —
 * not a count, and not a subset, because a count is satisfied by the wrong rows.
 *
 * The fixture's four duels alternate outcome and opponent along the finished-at timeline (WON,
 * LOST, DREW, WON; bob, carol, dave, carol) so that every filter below has a non-matching row
 * sitting between two matching ones rather than every matching row clustered at one end. A route
 * that only applied the filter to the newest page, or ignored it past the first row, would show up
 * here — it would not against a fixture where every match is newer than every non-match.
 */
class DuelHistoryFilterDatabaseTest {
    private lateinit var dataSource: DataSource
    private lateinit var duelResultStore: PostgresDuelResultStore
    private lateinit var alice: Player
    private lateinit var wonAgainstBobDuelId: String
    private lateinit var lostToCarolDuelId: String
    private lateinit var drewWithDaveDuelId: String
    private lateinit var wonAgainstCarolDuelId: String

    @BeforeEach
    fun setupDatabase() {
        dataSource = PostgresTestSupport.freshDatabase()
        Migrations.migrate(dataSource)
        val playerDirectory = PostgresPlayerDirectory(dataSource)
        duelResultStore = PostgresDuelResultStore(dataSource)

        runBlocking {
            alice = playerDirectory.resolve(DeviceId("alice"))
            val bob = playerDirectory.resolve(DeviceId("bob"))
            val carol = playerDirectory.resolve(DeviceId("carol"))
            val dave = playerDirectory.resolve(DeviceId("dave"))
            setPlayerDisplayName(bob.id.value, "Halvard")
            setPlayerDisplayName(carol.id.value, "Halvardsen")

            val base = Instant.parse("2024-01-01T10:00:00Z")
            wonAgainstBobDuelId = recordDuel(bob, winner = 0, finishedAt = base.plusSeconds(60))
            lostToCarolDuelId = recordDuel(carol, winner = 1, finishedAt = base.plusSeconds(120))
            drewWithDaveDuelId = recordDuel(dave, winner = null, finishedAt = base.plusSeconds(180))
            wonAgainstCarolDuelId = recordDuel(carol, winner = 0, finishedAt = base.plusSeconds(240))
        }
    }

    @Test
    fun filteringByOutcomeOverHttpReturnsOnlyThatOutcome() {
        testApplication {
            application {
                module()
                profileRoutes(PostgresProfileReads(dataSource), PostgresProfileWrites(dataSource), identitiesFor(dataSource))
            }

            val wonPage = fetchDuels(client, outcome = "WON")
            assertEquals(listOf(wonAgainstCarolDuelId, wonAgainstBobDuelId), wonPage.duels.map { it.duelId })
            assertTrue(wonPage.duels.all { it.outcome == DuelOutcomeLabel.WON })

            val drewPage = fetchDuels(client, outcome = "DREW")
            assertEquals(listOf(drewWithDaveDuelId), drewPage.duels.map { it.duelId })
        }
    }

    @Test
    fun searchingAnOpponentOverHttpReturnsOnlyThatOpponentsDuels() {
        testApplication {
            application {
                module()
                profileRoutes(PostgresProfileReads(dataSource), PostgresProfileWrites(dataSource), identitiesFor(dataSource))
            }

            // Lower-cased term against the stored "Halvardsen": proves the case fold, the
            // direction of the match (bob's "Halvard" is a prefix of the term and must not
            // match) and the exclusion of dave, whose opponent name is unset entirely.
            val page = fetchDuels(client, opponent = "halvardsen")

            assertEquals(listOf(wonAgainstCarolDuelId, lostToCarolDuelId), page.duels.map { it.duelId })
        }
    }

    @Test
    fun bothFiltersComposeOverHttp() {
        testApplication {
            application {
                module()
                profileRoutes(PostgresProfileReads(dataSource), PostgresProfileWrites(dataSource), identitiesFor(dataSource))
            }

            val both = fetchDuels(client, outcome = "WON", opponent = "Halvardsen")
            val outcomeOnly = fetchDuels(client, outcome = "WON")
            val opponentOnly = fetchDuels(client, opponent = "Halvardsen")

            assertEquals(listOf(wonAgainstCarolDuelId), both.duels.map { it.duelId })
            assertEquals(2, outcomeOnly.duels.size)
            assertEquals(2, opponentOnly.duels.size)
            assertTrue(
                both.duels.size < outcomeOnly.duels.size,
                "intersection (${both.duels.size}) must be smaller than outcome=WON alone (${outcomeOnly.duels.size})",
            )
            assertTrue(
                both.duels.size < opponentOnly.duels.size,
                "intersection (${both.duels.size}) must be smaller than opponent=Halvardsen alone " +
                    "(${opponentOnly.duels.size})",
            )
        }
    }

    @Test
    fun anEmptyResultIsTwoHundredWithAnEmptyPage() {
        testApplication {
            application {
                module()
                profileRoutes(PostgresProfileReads(dataSource), PostgresProfileWrites(dataSource), identitiesFor(dataSource))
            }

            val page = fetchDuels(client, opponent = "Sigrid")

            assertTrue(page.duels.isEmpty())
            assertNull(page.nextCursor)
        }
    }

    @Test
    fun anUnusableOutcomeIsFourHundredOverHttp() {
        testApplication {
            application {
                module()
                profileRoutes(PostgresProfileReads(dataSource), PostgresProfileWrites(dataSource), identitiesFor(dataSource))
            }

            val response =
                client.get("/api/me/duels?limit=$ALL_DUELS_LIMIT&outcome=won") {
                    header(DEVICE_ID_HEADER, "alice")
                }

            assertEquals(HttpStatusCode.BadRequest, response.status)
            assertTrue(response.bodyAsText().isEmpty())
        }
    }

    /**
     * Records a duel of alice (always seat 0) against [opponent], returning its id. A draw needs
     * an end condition the engine can actually finish level on — the default format cannot — so
     * only the `winner == null` case swaps in [EndCondition.FixedHands].
     */
    private suspend fun recordDuel(opponent: Player, winner: Int?, finishedAt: Instant): String {
        val outcome = when (winner) {
            0 -> DuelOutcome(winner = 0, handsPlayed = 1, finalStacks = listOf(11_000, 9_000))
            1 -> DuelOutcome(winner = 1, handsPlayed = 1, finalStacks = listOf(9_000, 11_000))
            null -> DuelOutcome(winner = null, handsPlayed = 1, finalStacks = listOf(10_000, 10_000))
            else -> throw IllegalArgumentException("winner must be 0, 1, or null, got $winner")
        }
        val format =
            if (winner == null) {
                formatLabel(DuelFormat.DEFAULT.copy(endCondition = EndCondition.FixedHands(20)))
            } else {
                formatLabel(DuelFormat.DEFAULT)
            }
        val duel =
            FinishedDuel(
                id = UUID.randomUUID(),
                format = format,
                startedAt = finishedAt.minusSeconds(60),
                finishedAt = finishedAt,
                seats = listOf(alice.id, opponent.id),
                outcome = outcome,
            )
        duelResultStore.record(duel)
        return duel.id.toString()
    }

    private fun setPlayerDisplayName(playerId: String, displayName: String) {
        dataSource.connection.use { connection ->
            val registerName = "INSERT INTO name_registry (name, reason) VALUES (?, 'TAKEN')"
            connection.prepareStatement(registerName).use { statement ->
                statement.setString(1, displayName)
                statement.executeUpdate()
            }
            connection.prepareStatement("UPDATE player SET display_name = ? WHERE id = ?::uuid").use { statement ->
                statement.setString(1, displayName)
                statement.setString(2, playerId)
                statement.executeUpdate()
            }
        }
    }

    private suspend fun fetchDuels(
        client: HttpClient,
        outcome: String? = null,
        opponent: String? = null,
    ): RecentDuelsResponse {
        val query =
            buildString {
                append("limit=$ALL_DUELS_LIMIT")
                if (outcome != null) append("&outcome=$outcome")
                if (opponent != null) append("&opponent=$opponent")
            }
        val response = client.get("/api/me/duels?$query") { header(DEVICE_ID_HEADER, "alice") }

        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        return protocolJson.decodeFromString<RecentDuelsResponse>(body)
    }
}
