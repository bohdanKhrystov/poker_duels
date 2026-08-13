package duels.poker.server.e2e

import duels.poker.engine.duel.DuelOutcome
import duels.poker.server.db.PostgresTestSupport
import duels.poker.server.http.DEVICE_ID_HEADER
import duels.poker.server.protocol.http.ProfileResponse
import duels.poker.server.protocol.protocolJson
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
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import javax.sql.DataSource

/**
 * The coin a duel awards, read back the only way a real client can: `GET /api/me` on an HTTP
 * client, in the same running application that just played the duel over two WebSocket
 * connections.
 *
 * Every balance here is a [ProfileResponse] decoded off an HTTP response body — never a row read
 * straight out of the database, and never a query run against it directly. The duel's own frames
 * prove the write already landed (`RoomRegistry.act` awaits `DuelResultSink.record` before
 * `DuelFinished` is delivered), so this class needs no sleep, poll, or retry between the socket
 * half and the HTTP half.
 */
@Timeout(120)
internal class SocketCoinsTest {
    private lateinit var dataSource: DataSource

    @BeforeEach
    fun setup() {
        PostgresTestSupport.requireDocker()
        dataSource = freshMigratedDatabase()
    }

    /**
     * Reads [deviceId]'s profile over `GET /api/me`, asserting the response is `200`, then decodes
     * the body with [protocolJson] directly — the same manual decode every other e2e test in this
     * package uses for frames off a socket, never client-side content negotiation.
     */
    private suspend fun HttpClient.profileOf(deviceId: String): ProfileResponse {
        val response = get("/api/me") { header(DEVICE_ID_HEADER, deviceId) }
        assertEquals(
            HttpStatusCode.OK,
            response.status,
            "GET /api/me for deviceId=$deviceId returned ${response.status}",
        )
        return protocolJson.decodeFromString(response.bodyAsText())
    }

    /**
     * The winning seat named by [outcome], failing loudly and reproducibly if there is none.
     *
     * `CreateRoom` always opens a freezeout, which always names a winner (a draw is out of scope
     * for this suite — `TASK-021008`, `TASK-021108`), so a `null` here means the fixture itself
     * produced a draw, not that the test should silently treat seat 0 as the winner.
     */
    private fun SocketDuel.winnerSeat(outcome: DuelOutcome): Int =
        checkNotNull(outcome.winner) {
            "handSeed=$handSeed policySeed=$POLICY_SEED: outcome has no winner, outcome=$outcome"
        }

    @Test
    fun bothBalancesStartAtZero(): Unit = runBlocking {
        testApplication {
            installDuelServer(dataSource)
            val client = createClient { install(WebSockets) }
            val duel = client.openSocketDuel()

            for (player in duel.clients) {
                val profile = client.profileOf(player.deviceId)
                assertEquals(
                    0,
                    profile.coinBalance,
                    "deviceId=${player.deviceId} seat=${player.seat}: expected coinBalance 0 before any " +
                        "duel, got ${profile.coinBalance}",
                )
            }
        }
    }

    @Test
    fun theWinnerGainsACoinAndTheLoserLosesOne(): Unit = runBlocking {
        testApplication {
            installDuelServer(dataSource)
            val client = createClient { install(WebSockets) }
            val duel = client.openSocketDuel()

            // Established before playToFinish() so the assertion below is on the delta the duel
            // produced, not on a value that might already have been sitting there.
            val preDuelBalance =
                intArrayOf(
                    client.profileOf(duel.seat(0).deviceId).coinBalance,
                    client.profileOf(duel.seat(1).deviceId).coinBalance,
                )

            val outcome = duel.playToFinish()
            val winnerSeat = duel.winnerSeat(outcome)
            val loserSeat = 1 - winnerSeat
            val winner = duel.seat(winnerSeat)
            val loser = duel.seat(loserSeat)

            val postWinnerBalance = client.profileOf(winner.deviceId).coinBalance
            val postLoserBalance = client.profileOf(loser.deviceId).coinBalance

            assertEquals(
                preDuelBalance[winnerSeat] + 1,
                postWinnerBalance,
                "handSeed=${duel.handSeed} policySeed=$POLICY_SEED: winner seat=$winnerSeat " +
                    "deviceId=${winner.deviceId} expected coinBalance ${preDuelBalance[winnerSeat] + 1}, " +
                    "got $postWinnerBalance",
            )
            assertEquals(
                preDuelBalance[loserSeat] - 1,
                postLoserBalance,
                "handSeed=${duel.handSeed} policySeed=$POLICY_SEED: loser seat=$loserSeat " +
                    "deviceId=${loser.deviceId} expected coinBalance ${preDuelBalance[loserSeat] - 1}, " +
                    "got $postLoserBalance",
            )
        }
    }

    @Test
    fun theLosersBalanceIsNegativeAndNotClamped(): Unit = runBlocking {
        testApplication {
            installDuelServer(dataSource)
            val client = createClient { install(WebSockets) }
            val duel = client.openSocketDuel()

            val outcome = duel.playToFinish()
            val loser = duel.seat(1 - duel.winnerSeat(outcome))

            val postLoserBalance = client.profileOf(loser.deviceId).coinBalance

            // Exactly -1, not coerceAtLeast(0) or an absolute value: a negative balance is the
            // point (TASK-021010) and nothing here may launder it back to non-negative.
            assertEquals(
                -1,
                postLoserBalance,
                "handSeed=${duel.handSeed} policySeed=$POLICY_SEED: loser seat=${loser.seat} " +
                    "deviceId=${loser.deviceId} expected coinBalance -1, got $postLoserBalance",
            )
        }
    }
}
