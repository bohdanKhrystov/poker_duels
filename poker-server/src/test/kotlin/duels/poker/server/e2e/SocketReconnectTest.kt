package duels.poker.server.e2e

import duels.poker.engine.duel.DuelOutcome
import duels.poker.server.db.PostgresTestSupport
import duels.poker.server.protocol.ServerMessage
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.server.testing.testApplication
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout

/**
 * The count of `YourTurn` prompts [SocketReconnectTest.theOutcomeIsTheSameAsWithoutTheDisconnect]
 * lets through untouched before it drops one: nine real `Act`s have already been sent by then, so
 * the tenth prompt is the one interrupted. [POLICY_SEED] against [HAND_SEED] needs 21 to finish
 * (see `SocketDuel.kt`), comfortably more, so the drop lands mid-duel, not on its last decision.
 */
private const val TENTH_ACT: Int = 10

/**
 * Drops and reconnects whichever seat answers this duel's very first `YourTurn`, then plays it
 * through to completion — so a caller can inspect exactly what that one seat's `received` gained
 * around the reconnect without also having to drive the rest of the duel by hand.
 *
 * @return the seat that was dropped and reconnected.
 */
private suspend fun SocketDuel.playDroppingTheSeatOnTurn(http: HttpClient): Int {
    var droppedSeat = -1
    playToFinish(
        beforeAct = { client, _ ->
            if (droppedSeat == -1) {
                droppedSeat = client.seat
                reconnect(http, client.seat)
                true
            } else {
                false
            }
        },
    )
    check(droppedSeat != -1) { "handSeed=$handSeed: the duel finished without ever prompting a seat" }
    return droppedSeat
}

/**
 * `TASK-021211`: a socket that drops mid-duel with a genuine WebSocket close, and rejoins on the
 * same device inside the disconnect grace window (`STORY-0208`), resumes the very seat it held,
 * and the duel ends with the outcome it would have reached without the interruption.
 *
 * Every drop and reconnect in this file is immediate and in-process: no clock is injected and no
 * time is advanced, which puts every reconnect comfortably inside the default grace window by any
 * wall-clock measure, so nothing here exercises expiry — that has no production driver yet
 * (`DEC-019`) and is `TASK-020806`/`TASK-020812`'s subject, not this file's.
 */
@Timeout(120)
internal class SocketReconnectTest {
    private lateinit var dataSource: javax.sql.DataSource

    @BeforeEach
    fun setup() {
        PostgresTestSupport.requireDocker()
        dataSource = freshMigratedDatabase()
    }

    @Test
    fun theOutcomeIsTheSameAsWithoutTheDisconnect(): Unit = runBlocking {
        suspend fun playUndisturbed(): DuelOutcome {
            lateinit var outcome: DuelOutcome
            testApplication {
                installDuelServer(freshMigratedDatabase())
                val http = createClient { install(WebSockets) }
                outcome = http.openSocketDuel().playToFinish()
            }
            return outcome
        }

        suspend fun playInterrupted(): DuelOutcome {
            lateinit var outcome: DuelOutcome
            testApplication {
                installDuelServer(freshMigratedDatabase())
                val http = createClient { install(WebSockets) }
                val duel = http.openSocketDuel()

                var seen = 0
                outcome = duel.playToFinish(
                    beforeAct = { client, _ ->
                        seen++
                        if (seen == TENTH_ACT) {
                            duel.reconnect(http, client.seat)
                            true
                        } else {
                            false
                        }
                    },
                )
            }
            return outcome
        }

        val undisturbed = playUndisturbed()
        val interrupted = playInterrupted()

        assert(undisturbed == interrupted) {
            "handSeed=$HAND_SEED policySeed=$POLICY_SEED: expected the interrupted duel to reach the same " +
                "outcome as the undisturbed one, got undisturbed=$undisturbed interrupted=$interrupted"
        }
    }

    @Test
    fun theReturningSocketKeepsItsSeat(): Unit = runBlocking {
        testApplication {
            installDuelServer(dataSource)
            val http = createClient { install(WebSockets) }
            val duel = http.openSocketDuel()

            val droppedSeat = duel.playDroppingTheSeatOnTurn(http)

            val roomJoinedFrames = duel.seat(droppedSeat).received.filterIsInstance<ServerMessage.RoomJoined>()
            assert(roomJoinedFrames.size == 2) {
                "handSeed=$HAND_SEED seat=$droppedSeat: expected the original RoomJoined and one from the " +
                    "reconnect, got $roomJoinedFrames"
            }
            assert(roomJoinedFrames[1].seat == droppedSeat) {
                "handSeed=$HAND_SEED seat=$droppedSeat: expected the resumed RoomJoined to name the same " +
                    "seat, got ${roomJoinedFrames[1]}"
            }
        }
    }

    @Test
    fun theReturningSocketIsPromptedAgain(): Unit = runBlocking {
        testApplication {
            installDuelServer(dataSource)
            val http = createClient { install(WebSockets) }
            val duel = http.openSocketDuel()

            val droppedSeat = duel.playDroppingTheSeatOnTurn(http)

            val received = duel.seat(droppedSeat).received
            val resumeIndex = received.indexOfLast { it is ServerMessage.RoomJoined }
            assert(resumeIndex >= 0) {
                "handSeed=$HAND_SEED seat=$droppedSeat: expected a RoomJoined from the reconnect, got $received"
            }
            assert(resumeIndex + 2 < received.size) {
                "handSeed=$HAND_SEED seat=$droppedSeat: expected at least two frames after the resumed " +
                    "RoomJoined, got ${received.drop(resumeIndex + 1)}"
            }
            assert(received[resumeIndex + 1] is ServerMessage.Snapshot) {
                "handSeed=$HAND_SEED seat=$droppedSeat: expected a Snapshot right after the resumed " +
                    "RoomJoined, got ${received[resumeIndex + 1]}"
            }
            assert(received[resumeIndex + 2] is ServerMessage.YourTurn) {
                "handSeed=$HAND_SEED seat=$droppedSeat: expected a YourTurn after the resumed Snapshot, " +
                    "got ${received[resumeIndex + 2]}"
            }
        }
    }
}
