package duels.poker.server.e2e

import duels.poker.engine.duel.DuelOutcome
import duels.poker.server.db.PostgresTestSupport
import duels.poker.server.http.DEVICE_ID_HEADER
import duels.poker.server.protocol.http.DuelOutcomeLabel
import duels.poker.server.protocol.http.ProfileResponse
import duels.poker.server.protocol.http.RecentDuelsResponse
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
 * The duel the two sockets just played is the one duel in each player's `GET /api/me/duels`,
 * seen from opposite sides. Both players' lists must contain the duel with matching `duelId`,
 * opposite `coinDelta`s, opposite `outcome`s, correct `opponentPlayerId`s, and matching `handsPlayed`.
 */
@Timeout(120)
internal class SocketHistoryTest {
    private lateinit var dataSource: DataSource

    @BeforeEach
    fun setup() {
        PostgresTestSupport.requireDocker()
        dataSource = freshMigratedDatabase()
    }

    /**
     * Reads [deviceId]'s profile over `GET /api/me`, asserting the response is `200`, then decodes
     * the body with [protocolJson] directly.
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
     * Reads [deviceId]'s recent duels over `GET /api/me/duels`, asserting the response is `200`,
     * then decodes the body with [protocolJson] directly.
     */
    private suspend fun HttpClient.recentDuelsOf(deviceId: String): RecentDuelsResponse {
        val response = get("/api/me/duels") { header(DEVICE_ID_HEADER, deviceId) }
        assertEquals(
            HttpStatusCode.OK,
            response.status,
            "GET /api/me/duels for deviceId=$deviceId returned ${response.status}",
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
    fun oneDuelIsInBothPlayersLists(): Unit = runBlocking {
        testApplication {
            installDuelServer(dataSource)
            val client = createClient { install(WebSockets) }
            val duel = client.openSocketDuel()

            // Established before playToFinish() so the assertion below is on the duel that the duel
            // produced, not on a value that might already have been sitting there.
            val preDuelHostDuels = client.recentDuelsOf(duel.seat(0).deviceId).duels
            val preDuelGuestDuels = client.recentDuelsOf(duel.seat(1).deviceId).duels

            assertEquals(
                0,
                preDuelHostDuels.size,
                "handSeed=${duel.handSeed} policySeed=$POLICY_SEED: host's duel list should be empty before duel, got ${preDuelHostDuels.size}",
            )
            assertEquals(
                0,
                preDuelGuestDuels.size,
                "handSeed=${duel.handSeed} policySeed=$POLICY_SEED: guest's duel list should be empty before duel, got ${preDuelGuestDuels.size}",
            )

            val outcome = duel.playToFinish()

            val hostDuels = client.recentDuelsOf(duel.seat(0).deviceId).duels
            val guestDuels = client.recentDuelsOf(duel.seat(1).deviceId).duels

            assertEquals(
                1,
                hostDuels.size,
                "handSeed=${duel.handSeed} policySeed=$POLICY_SEED: host's duel list should contain exactly 1 duel, got ${hostDuels.size}",
            )
            assertEquals(
                1,
                guestDuels.size,
                "handSeed=${duel.handSeed} policySeed=$POLICY_SEED: guest's duel list should contain exactly 1 duel, got ${guestDuels.size}",
            )

            val hostDuelId = hostDuels.single().duelId
            val guestDuelId = guestDuels.single().duelId

            assertEquals(
                hostDuelId,
                guestDuelId,
                "handSeed=${duel.handSeed} policySeed=$POLICY_SEED: both players should see the same duelId, " +
                    "host got $hostDuelId, guest got $guestDuelId",
            )
        }
    }

    @Test
    fun theDeltasAreOppositeAndTheOutcomesAgree(): Unit = runBlocking {
        testApplication {
            installDuelServer(dataSource)
            val client = createClient { install(WebSockets) }
            val duel = client.openSocketDuel()

            // Established before playToFinish() so the assertion below is on the duels that the duel
            // produced, not on values that might already have been sitting there.
            val preDuelHostDuels = client.recentDuelsOf(duel.seat(0).deviceId).duels
            val preDuelGuestDuels = client.recentDuelsOf(duel.seat(1).deviceId).duels

            assertEquals(
                0,
                preDuelHostDuels.size,
                "handSeed=${duel.handSeed} policySeed=$POLICY_SEED: host's duel list should be empty before duel, got ${preDuelHostDuels.size}",
            )
            assertEquals(
                0,
                preDuelGuestDuels.size,
                "handSeed=${duel.handSeed} policySeed=$POLICY_SEED: guest's duel list should be empty before duel, got ${preDuelGuestDuels.size}",
            )

            val outcome = duel.playToFinish()
            val winnerSeat = duel.winnerSeat(outcome)
            val loserSeat = 1 - winnerSeat

            val hostProfile = client.profileOf(duel.seat(0).deviceId)
            val guestProfile = client.profileOf(duel.seat(1).deviceId)

            val hostDuels = client.recentDuelsOf(duel.seat(0).deviceId).duels
            val guestDuels = client.recentDuelsOf(duel.seat(1).deviceId).duels

            val hostDuel = hostDuels.single()
            val guestDuel = guestDuels.single()

            // Winner's seat has coinDelta +1, loser's seat has -1
            val seatCoinDeltas = intArrayOf(hostDuel.coinDelta, guestDuel.coinDelta)
            if (duel.seat(0).seat == winnerSeat) {
                assertEquals(
                    1,
                    hostDuel.coinDelta,
                    "handSeed=${duel.handSeed} policySeed=$POLICY_SEED: winner (seat=$winnerSeat) " +
                        "should have coinDelta +1, got ${hostDuel.coinDelta}",
                )
                assertEquals(
                    -1,
                    guestDuel.coinDelta,
                    "handSeed=${duel.handSeed} policySeed=$POLICY_SEED: loser (seat=$loserSeat) " +
                        "should have coinDelta -1, got ${guestDuel.coinDelta}",
                )
            } else {
                assertEquals(
                    -1,
                    hostDuel.coinDelta,
                    "handSeed=${duel.handSeed} policySeed=$POLICY_SEED: loser (seat=$loserSeat) " +
                        "should have coinDelta -1, got ${hostDuel.coinDelta}",
                )
                assertEquals(
                    1,
                    guestDuel.coinDelta,
                    "handSeed=${duel.handSeed} policySeed=$POLICY_SEED: winner (seat=$winnerSeat) " +
                        "should have coinDelta +1, got ${guestDuel.coinDelta}",
                )
            }

            // Verify deltas sum to zero
            assertEquals(
                0,
                hostDuel.coinDelta + guestDuel.coinDelta,
                "handSeed=${duel.handSeed} policySeed=$POLICY_SEED: coinDeltas should sum to 0, " +
                    "got ${hostDuel.coinDelta} + ${guestDuel.coinDelta}",
            )

            // Verify outcomes are correct
            if (duel.seat(0).seat == winnerSeat) {
                assertEquals(
                    DuelOutcomeLabel.WON,
                    hostDuel.outcome,
                    "handSeed=${duel.handSeed} policySeed=$POLICY_SEED: winner should have outcome WON, got ${hostDuel.outcome}",
                )
                assertEquals(
                    DuelOutcomeLabel.LOST,
                    guestDuel.outcome,
                    "handSeed=${duel.handSeed} policySeed=$POLICY_SEED: loser should have outcome LOST, got ${guestDuel.outcome}",
                )
            } else {
                assertEquals(
                    DuelOutcomeLabel.LOST,
                    hostDuel.outcome,
                    "handSeed=${duel.handSeed} policySeed=$POLICY_SEED: loser should have outcome LOST, got ${hostDuel.outcome}",
                )
                assertEquals(
                    DuelOutcomeLabel.WON,
                    guestDuel.outcome,
                    "handSeed=${duel.handSeed} policySeed=$POLICY_SEED: winner should have outcome WON, got ${guestDuel.outcome}",
                )
            }

            // Verify opponent playerIds are correct
            assertEquals(
                guestProfile.playerId,
                hostDuel.opponentPlayerId,
                "handSeed=${duel.handSeed} policySeed=$POLICY_SEED: host's opponentPlayerId should be guest's playerId, " +
                    "got ${hostDuel.opponentPlayerId}, expected ${guestProfile.playerId}",
            )
            assertEquals(
                hostProfile.playerId,
                guestDuel.opponentPlayerId,
                "handSeed=${duel.handSeed} policySeed=$POLICY_SEED: guest's opponentPlayerId should be host's playerId, " +
                    "got ${guestDuel.opponentPlayerId}, expected ${hostProfile.playerId}",
            )
        }
    }

    @Test
    fun bothListsAgreeWithTheFrameOnHandsPlayed(): Unit = runBlocking {
        testApplication {
            installDuelServer(dataSource)
            val client = createClient { install(WebSockets) }
            val duel = client.openSocketDuel()

            // Established before playToFinish() so the assertion below is on the duels that the duel
            // produced, not on values that might already have been sitting there.
            val preDuelHostDuels = client.recentDuelsOf(duel.seat(0).deviceId).duels
            val preDuelGuestDuels = client.recentDuelsOf(duel.seat(1).deviceId).duels

            assertEquals(
                0,
                preDuelHostDuels.size,
                "handSeed=${duel.handSeed} policySeed=$POLICY_SEED: host's duel list should be empty before duel, got ${preDuelHostDuels.size}",
            )
            assertEquals(
                0,
                preDuelGuestDuels.size,
                "handSeed=${duel.handSeed} policySeed=$POLICY_SEED: guest's duel list should be empty before duel, got ${preDuelGuestDuels.size}",
            )

            val outcome = duel.playToFinish()

            val hostDuels = client.recentDuelsOf(duel.seat(0).deviceId).duels
            val guestDuels = client.recentDuelsOf(duel.seat(1).deviceId).duels

            val hostDuel = hostDuels.single()
            val guestDuel = guestDuels.single()

            assertEquals(
                outcome.handsPlayed,
                hostDuel.handsPlayed,
                "handSeed=${duel.handSeed} policySeed=$POLICY_SEED: host's handsPlayed should match outcome, " +
                    "got ${hostDuel.handsPlayed}, expected ${outcome.handsPlayed}",
            )
            assertEquals(
                outcome.handsPlayed,
                guestDuel.handsPlayed,
                "handSeed=${duel.handSeed} policySeed=$POLICY_SEED: guest's handsPlayed should match outcome, " +
                    "got ${guestDuel.handsPlayed}, expected ${outcome.handsPlayed}",
            )
        }
    }
}
