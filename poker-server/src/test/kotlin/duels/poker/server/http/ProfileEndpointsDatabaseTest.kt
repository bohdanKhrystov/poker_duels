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
import duels.poker.server.protocol.http.DuelOutcomeLabel
import duels.poker.server.protocol.http.ProfileResponse
import duels.poker.server.protocol.http.RecentDuelsResponse
import duels.poker.server.protocol.protocolJson
import duels.poker.server.session.DeviceId
import duels.poker.server.session.Player
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
import kotlin.test.assertTrue

class ProfileEndpointsDatabaseTest {
    private lateinit var dataSource: DataSource
    private lateinit var playerDirectory: PostgresPlayerDirectory
    private lateinit var profileReads: PostgresProfileReads
    private lateinit var duelResultStore: PostgresDuelResultStore
    private lateinit var alice: Player
    private lateinit var bob: Player

    @BeforeEach
    fun setupDatabase() {
        dataSource = PostgresTestSupport.freshDatabase()
        Migrations.migrate(dataSource)
        playerDirectory = PostgresPlayerDirectory(dataSource)
        profileReads = PostgresProfileReads(dataSource)
        duelResultStore = PostgresDuelResultStore(dataSource)

        runBlocking {
            alice = playerDirectory.resolve(DeviceId("alice"))
            bob = playerDirectory.resolve(DeviceId("bob"))

            // Record one duel won by seat 0 (alice)
            val finishedAt = Instant.now()
            val startedAt = finishedAt.minusSeconds(60)
            val duel = FinishedDuel(
                id = UUID.randomUUID(),
                format = formatLabel(DuelFormat.DEFAULT),
                startedAt = startedAt,
                finishedAt = finishedAt,
                seats = listOf(alice.id, bob.id),
                outcome = DuelOutcome(winner = 0, handsPlayed = 1, finalStacks = listOf(11_000, 9_000)),
            )
            duelResultStore.record(duel)
        }
    }

    @Test
    fun aDuelThatJustFinishedAppearsInTheList() = testApplication {
        application {
            module()
            profileRoutes(profileReads, PostgresProfileWrites(dataSource))
        }

        val response = client.get("/api/me/duels") {
            header(DEVICE_ID_HEADER, "alice")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        val recentDuels = protocolJson.decodeFromString<RecentDuelsResponse>(body)

        assertEquals(1, recentDuels.duels.size)
        val duelSummary = recentDuels.duels.single()

        assertEquals(bob.id.value, duelSummary.opponentPlayerId)
        assertEquals(DuelOutcomeLabel.WON, duelSummary.outcome)
        assertEquals(1, duelSummary.coinDelta)
        // Verify finishedAt is a valid instant
        val finishedInstant = Instant.parse(duelSummary.finishedAt)
        assertTrue(finishedInstant.isBefore(Instant.now().plusSeconds(1)))
    }

    @Test
    fun theLosersBalanceComesBackOverTheWireAsMinusOne() = testApplication {
        application {
            module()
            profileRoutes(profileReads, PostgresProfileWrites(dataSource))
        }

        val response = client.get("/api/me") {
            header(DEVICE_ID_HEADER, "bob")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        val profile = protocolJson.decodeFromString<ProfileResponse>(body)

        assertEquals(-1, profile.coinBalance)
    }

    @Test
    fun anUnknownDeviceIsRefusedAndCreatesNoProfile() = testApplication {
        application {
            module()
            profileRoutes(profileReads, PostgresProfileWrites(dataSource))
        }

        val countBefore = playerRowCount()

        val response = client.get("/api/me") {
            header(DEVICE_ID_HEADER, "ghost")
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
        val countAfter = playerRowCount()
        assertEquals(countBefore, countAfter)
    }

    private fun playerRowCount(): Int {
        return dataSource.connection.use { connection ->
            connection.prepareStatement("SELECT COUNT(*) FROM player").use { statement ->
                statement.executeQuery().use { rows ->
                    rows.next()
                    rows.getInt(1)
                }
            }
        }
    }
}
