package duels.poker.server.e2e

import duels.poker.engine.duel.DuelFormat
import duels.poker.server.db.PostgresTestSupport
import duels.poker.server.protocol.ServerMessage
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.server.testing.testApplication
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import javax.sql.DataSource

/**
 * Chips are conserved in the frames the two players actually received over the wire.
 *
 * Every assertion here is built from the [ServerMessage.Snapshot] frames in each
 * [SocketClient.received] — never from server-internal state or the engine's log.
 * The engine already has its own conservation property; this one is worth having only
 * because it is computed from the wire what real clients actually saw.
 */
@Timeout(120)
internal class SocketChipsTest {
    private lateinit var dataSource: DataSource

    @BeforeEach
    fun setup() {
        PostgresTestSupport.requireDocker()
        dataSource = freshMigratedDatabase()
    }

    /**
     * The chip total a single snapshot claims, per the formula in TASK-021208's `## Scope`.
     * Stacks plus pot plus committed chips at a given instant must equal the total chips
     * in play for the duel.
     */
    private fun accountedFor(snapshot: ServerMessage.Snapshot): Int =
        snapshot.view.pot + snapshot.view.seats.sumOf { it.stack } + snapshot.view.seats.sumOf { it.committedThisStreet }

    @Test
    fun everySnapshotAccountsForEveryChip(): Unit = runBlocking {
        testApplication {
            installDuelServer(dataSource)
            val client = createClient { install(WebSockets) }
            val duel = client.openSocketDuel()

            val outcome = duel.playToFinish()
            val expectedTotal = 2 * DuelFormat.DEFAULT.startingStack

            for (seat in 0..1) {
                val snapshots = duel.seat(seat).received.filterIsInstance<ServerMessage.Snapshot>()
                snapshots.forEachIndexed { index, snapshot ->
                    val accounted = accountedFor(snapshot)
                    assertEquals(
                        expectedTotal,
                        accounted,
                        "handSeed=${duel.handSeed} policySeed=$POLICY_SEED seat=$seat frame=$index: " +
                            "snapshot accounted for $accounted chips, expected $expectedTotal",
                    )
                }
            }
        }
    }

    @Test
    fun bothClientsReceivedASnapshotPerHand(): Unit = runBlocking {
        testApplication {
            installDuelServer(dataSource)
            val client = createClient { install(WebSockets) }
            val duel = client.openSocketDuel()

            val outcome = duel.playToFinish()

            for (seat in 0..1) {
                val snapshots = duel.seat(seat).received.filterIsInstance<ServerMessage.Snapshot>()

                // The fixture that stops `everySnapshotAccountsForEveryChip` from passing
                // vacuously because a duel that produced no frames would conserve chips perfectly.
                assertTrue(
                    snapshots.size >= outcome.handsPlayed,
                    "handSeed=${duel.handSeed} policySeed=$POLICY_SEED seat=$seat: " +
                        "received ${snapshots.size} snapshots, expected at least outcome.handsPlayed=${outcome.handsPlayed}",
                )
            }
        }
    }

    @Test
    fun noSnapshotShowedANegativeStackOrPot(): Unit = runBlocking {
        testApplication {
            installDuelServer(dataSource)
            val client = createClient { install(WebSockets) }
            val duel = client.openSocketDuel()

            duel.playToFinish()

            for (seat in 0..1) {
                duel.seat(seat).received.filterIsInstance<ServerMessage.Snapshot>().forEachIndexed { index, snapshot ->
                    val view = snapshot.view
                    assertTrue(
                        view.pot >= 0,
                        "handSeed=${duel.handSeed} policySeed=$POLICY_SEED seat=$seat frame=$index: " +
                            "snapshot has negative pot ${view.pot}",
                    )
                    view.seats.forEach { seatView ->
                        assertTrue(
                            seatView.stack >= 0,
                            "handSeed=${duel.handSeed} policySeed=$POLICY_SEED seat=$seat frame=$index: " +
                                "seat ${seatView.index} has negative stack ${seatView.stack}",
                        )
                        assertTrue(
                            seatView.committedThisStreet >= 0,
                            "handSeed=${duel.handSeed} policySeed=$POLICY_SEED seat=$seat frame=$index: " +
                                "seat ${seatView.index} has negative committedThisStreet ${seatView.committedThisStreet}",
                        )
                    }
                }
            }
        }
    }
}
