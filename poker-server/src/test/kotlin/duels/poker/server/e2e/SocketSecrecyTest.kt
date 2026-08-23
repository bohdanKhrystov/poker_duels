package duels.poker.server.e2e

import duels.poker.engine.game.HandRevealed
import duels.poker.engine.game.HandStarted
import duels.poker.engine.game.HoleCardsDealt
import duels.poker.server.db.PostgresTestSupport
import duels.poker.server.protocol.ServerMessage
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.server.testing.testApplication
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import javax.sql.DataSource

/**
 * Across a whole duel played over two real WebSocket connections, no frame either client
 * actually received carried the opponent's hole cards before that hand's reveal.
 *
 * Built entirely from [SocketClient.received] — the frames a client was actually sent — never
 * from any server-internal state or its log. `RunnerLeakTest` proves the same three claims one
 * layer down, against the `Addressed` frames `playDuel` hands out directly; this class proves
 * them at the boundary a real opponent is limited to: two sockets and nothing else.
 */
@Timeout(120)
internal class SocketSecrecyTest {
    private lateinit var dataSource: DataSource

    @BeforeEach
    fun setup() {
        PostgresTestSupport.requireDocker()
        dataSource = freshMigratedDatabase()
    }

    /**
     * Walks one client's own [messages], in arrival order, tracking which seats a `HandRevealed`
     * has named **in the current hand only** — the set resets on every `HandStarted` — exactly as
     * `RunnerLeakTest.revealedAtEachSnapshot` does one layer down. Nothing here reads any
     * server-internal state or a log: only what [messages] itself contains decides the answer.
     *
     * @param seat the seat [messages] belongs to; a violation concerns seat `1 - seat`'s cards.
     * @param messages the frames [seat] actually received, in the order it received them.
     * @return one description per violation found. Empty is the only passing result — a checker
     *   that could only ever return empty would prove nothing, which is why
     *   [SocketSecrecyTest.theCheckerCatchesAPlantedCard] exists.
     */
    private fun leaks(seat: Int, messages: List<ServerMessage>): List<String> {
        val opponent = 1 - seat
        val violations = mutableListOf<String>()
        var revealed = mutableSetOf<Int>()

        messages.forEachIndexed { index, message ->
            when (message) {
                is ServerMessage.Events -> {
                    message.events.forEach { event ->
                        when (event) {
                            is HandStarted -> revealed = mutableSetOf()
                            is HandRevealed -> revealed.add(event.seat)
                            else -> Unit
                        }
                    }
                    val dealtToOpponent =
                        message.events.filterIsInstance<HoleCardsDealt>().filter { it.seat == opponent }
                    if (dealtToOpponent.isNotEmpty()) {
                        violations +=
                            "seat $seat, frame $index: Events carries HoleCardsDealt for opponent " +
                            "(seat $opponent): $dealtToOpponent"
                    }
                }

                is ServerMessage.Snapshot -> {
                    val opponentCards = message.view.seats[opponent].holeCards
                    if (opponent !in revealed && opponentCards.isNotEmpty()) {
                        violations +=
                            "seat $seat, frame $index: Snapshot shows opponent (seat $opponent) " +
                            "holeCards $opponentCards before seat $opponent was revealed in this hand"
                    }
                }

                is ServerMessage.Welcome,
                is ServerMessage.RoomJoined,
                is ServerMessage.RematchOffered,
                is ServerMessage.YourTurn,
                is ServerMessage.Rejected,
                is ServerMessage.Failure,
                is ServerMessage.DuelFinished,
                is ServerMessage.OpponentPresence,
                is ServerMessage.ActedForAbsent,
                -> Unit
            }
        }

        return violations
    }

    @Test
    fun neitherClientEverSawTheOthersCards(): Unit = runBlocking {
        testApplication {
            installDuelServer(dataSource)
            val client = createClient { install(WebSockets) }
            val duel = client.openSocketDuel()

            val outcome = duel.playToFinish()

            for (seat in 0..1) {
                val violations = leaks(seat, duel.seat(seat).received)
                assertTrue(
                    violations.isEmpty(),
                    "handSeed=${duel.handSeed} policySeed=$POLICY_SEED handsPlayed=${outcome.handsPlayed}: " +
                        "seat $seat's own frames leaked the opponent's hole cards: $violations",
                )
            }
        }
    }

    @Test
    fun eachClientSawItsOwnTwoCards(): Unit = runBlocking {
        testApplication {
            installDuelServer(dataSource)
            val client = createClient { install(WebSockets) }
            val duel = client.openSocketDuel()

            val outcome = duel.playToFinish()

            for (seat in 0..1) {
                val snapshots = duel.seat(seat).received.filterIsInstance<ServerMessage.Snapshot>()

                // The fixture that stops `neitherClientEverSawTheOthersCards` from passing because
                // nothing was ever sent: a duel that produced no frames would let that test's
                // `leaks()` call return an empty list vacuously.
                assertTrue(
                    snapshots.size >= outcome.handsPlayed,
                    "handSeed=${duel.handSeed}: seat $seat received ${snapshots.size} snapshots, expected " +
                        "at least outcome.handsPlayed=${outcome.handsPlayed}",
                )

                snapshots.forEachIndexed { index, snapshot ->
                    val ownCards = snapshot.view.seats[seat].holeCards
                    assertTrue(
                        ownCards.size == 2,
                        "handSeed=${duel.handSeed}: seat $seat's snapshot #$index shows ${ownCards.size} " +
                            "own hole cards, expected 2",
                    )
                }
            }
        }
    }

    @Test
    fun theCheckerCatchesAPlantedCard(): Unit = runBlocking {
        testApplication {
            installDuelServer(dataSource)
            val client = createClient { install(WebSockets) }
            val duel = client.openSocketDuel()

            duel.playToFinish()

            val host = duel.clients[0]
            val opponent = 1 - host.seat
            val original = host.received.filterIsInstance<ServerMessage.Snapshot>().first()

            val plantedSeats =
                original.view.seats.mapIndexed { index, seatView ->
                    if (index == opponent) {
                        seatView.copy(holeCards = original.view.viewer.holeCards)
                    } else {
                        seatView
                    }
                }
            val planted = original.copy(view = original.view.copy(seats = plantedSeats))
            val doctored = host.received.map { if (it === original) planted else it }

            assertTrue(
                leaks(host.seat, doctored).isNotEmpty(),
                "planting seat ${host.seat}'s own hole cards onto opponent seat $opponent's SeatView in " +
                    "the first snapshot should have been caught by leaks(), but wasn't",
            )
        }
    }
}
