package duels.poker.server.e2e

import duels.poker.server.db.PostgresProfileReads
import duels.poker.server.db.PostgresTestSupport
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
}
