package duels.poker.server.e2e

import duels.poker.engine.duel.DuelOutcome
import duels.poker.server.db.PostgresProfileReads
import duels.poker.server.db.PostgresTestSupport
import duels.poker.server.protocol.ServerMessage
import duels.poker.server.session.DeviceId
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.server.testing.testApplication
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout

@Timeout(120)
internal class SocketDuelTest {
    private lateinit var dataSource: javax.sql.DataSource

    @BeforeEach
    fun setup() {
        PostgresTestSupport.requireDocker()
        dataSource = freshMigratedDatabase()
    }

    @Test
    fun bothClientsAreSeatedInOneRoom(): Unit = runBlocking {
        testApplication {
            installDuelServer(dataSource)
            val client = createClient { install(WebSockets) }

            val duel = client.openSocketDuel()

            // Both clients' RoomJoined frames name the same code
            assert(duel.clients[0].received.filterIsInstance<duels.poker.server.protocol.ServerMessage.RoomJoined>().single().code == duel.code)
            assert(duel.clients[1].received.filterIsInstance<duels.poker.server.protocol.ServerMessage.RoomJoined>().single().code == duel.code)

            // Their seats are exactly {0, 1}
            val seats = setOf(duel.clients[0].seat, duel.clients[1].seat)
            assert(seats == setOf(0, 1)) { "Expected seats {0, 1}, got $seats" }
        }
    }

    @Test
    fun eachDeviceGotItsOwnProfile(): Unit = runBlocking {
        testApplication {
            installDuelServer(dataSource)
            val client = createClient { install(WebSockets) }

            val duel = client.openSocketDuel()

            // Both devices have profiles
            val profileReads = PostgresProfileReads(dataSource)
            val hostProfile = profileReads.profileOf(DeviceId(HOST_DEVICE))
            val guestProfile = profileReads.profileOf(DeviceId(GUEST_DEVICE))

            assert(hostProfile != null) { "Host device profile should exist" }
            assert(guestProfile != null) { "Guest device profile should exist" }

            // The two playerId values differ
            val hostPlayerId = hostProfile!!.playerId
            val guestPlayerId = guestProfile!!.playerId
            assert(hostPlayerId != guestPlayerId) { "Player IDs should differ: host=$hostPlayerId, guest=$guestPlayerId" }
        }
    }

    @Test
    fun theDuelReachesADeclaredWinner(): Unit = runBlocking {
        testApplication {
            installDuelServer(dataSource)
            val client = createClient { install(WebSockets) }
            val duel = client.openSocketDuel()

            val outcome = duel.playToFinish()

            assert(outcome.winner == 0 || outcome.winner == 1) { "Expected a declared winner, got ${outcome.winner}" }

            val hostFinished = duel.seat(0).received.filterIsInstance<ServerMessage.DuelFinished>()
            val guestFinished = duel.seat(1).received.filterIsInstance<ServerMessage.DuelFinished>()
            assert(hostFinished.size == 1) { "Expected exactly one DuelFinished for seat 0, got ${hostFinished.size}" }
            assert(guestFinished.size == 1) { "Expected exactly one DuelFinished for seat 1, got ${guestFinished.size}" }
            assert(hostFinished.single().outcome == guestFinished.single().outcome) {
                "Expected equal outcomes, got ${hostFinished.single().outcome} and ${guestFinished.single().outcome}"
            }
        }
    }

    @Test
    fun noClientWasEverRejected(): Unit = runBlocking {
        testApplication {
            installDuelServer(dataSource)
            val client = createClient { install(WebSockets) }
            val duel = client.openSocketDuel()

            duel.playToFinish()

            for (socketClient in duel.clients) {
                val refusals = socketClient.received.filter { it is ServerMessage.Rejected || it is ServerMessage.Failure }
                assert(refusals.isEmpty()) { "Seat ${socketClient.seat} was refused: $refusals" }
            }
        }
    }

    @Test
    fun theSameSeedsPlayTheSameDuel(): Unit = runBlocking {
        suspend fun playOnFreshSchema(): Pair<DuelOutcome, List<Int>> {
            lateinit var outcome: DuelOutcome
            lateinit var frameCounts: List<Int>
            testApplication {
                installDuelServer(freshMigratedDatabase())
                val client = createClient { install(WebSockets) }
                val duel = client.openSocketDuel()

                outcome = duel.playToFinish()
                frameCounts = listOf(duel.seat(0).received.size, duel.seat(1).received.size)
            }
            return outcome to frameCounts
        }

        val (firstOutcome, firstCounts) = playOnFreshSchema()
        val (secondOutcome, secondCounts) = playOnFreshSchema()

        assert(firstOutcome == secondOutcome) { "Expected equal outcomes, got $firstOutcome and $secondOutcome" }
        assert(firstCounts == secondCounts) { "Expected equal frame counts per seat, got $firstCounts and $secondCounts" }
    }
}
