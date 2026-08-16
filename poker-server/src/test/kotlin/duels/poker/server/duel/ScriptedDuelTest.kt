package duels.poker.server.duel

import duels.poker.engine.duel.DuelOutcome
import duels.poker.engine.game.HandRevealed
import duels.poker.server.protocol.Act
import duels.poker.server.protocol.ClientMessage
import duels.poker.server.protocol.ServerMessage
import duels.poker.server.protocol.protocolJson
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Proves that [scriptedDuel] gives each seat exactly its own session — every frame it received,
 * in order and complete — and that neither seat's session ever names a card it was not yet
 * entitled to see.
 */
@Timeout(120)
class ScriptedDuelTest {
    private val scripted = scriptedDuel()

    @Test
    fun bothSeatsGetTheirOwnWholeSession() {
        for (seat in scripted.seats) {
            assertTrue(seat.steps.size > 40, "seat ${seat.viewerSeat} had only ${seat.steps.size} steps")

            val welcome = seat.steps[0]
            assertEquals("server", welcome.from, "seat ${seat.viewerSeat}: first step should be from the server")
            assertTrue(decodeServer(welcome.frame) is ServerMessage.Welcome, "seat ${seat.viewerSeat}: first step should be a Welcome")

            val roomJoined = seat.steps[1]
            assertEquals("server", roomJoined.from, "seat ${seat.viewerSeat}: second step should be from the server")
            assertTrue(decodeServer(roomJoined.frame) is ServerMessage.RoomJoined, "seat ${seat.viewerSeat}: second step should be a RoomJoined")

            val last = seat.steps.last()
            assertEquals("server", last.from, "seat ${seat.viewerSeat}: last step should be from the server")
            assertTrue(decodeServer(last.frame) is ServerMessage.DuelFinished, "seat ${seat.viewerSeat}: last step should be a DuelFinished")
        }
    }

    @Test
    fun everyTurnIsFollowedByTheActThatAnsweredIt() {
        for (seat in scripted.seats) {
            val turnIndices = seat.steps.indices.filter { isYourTurnStep(seat, it) }
            val clientIndices = seat.steps.indices.filter { seat.steps[it].from == "client" }

            assertEquals(
                turnIndices.size,
                clientIndices.size,
                "seat ${seat.viewerSeat}: ${turnIndices.size} YourTurn frames but ${clientIndices.size} client steps",
            )

            for (turnIndex in turnIndices) {
                val turn = decodeServer(seat.steps[turnIndex].frame) as ServerMessage.YourTurn
                val actStep = seat.steps[turnIndex + 1]
                assertEquals(
                    "client",
                    actStep.from,
                    "seat ${seat.viewerSeat}: step right after the YourTurn at $turnIndex was not a client step",
                )
                val act = decodeClientAct(actStep.frame)
                assertEquals(turn.handNumber, act.handNumber, "seat ${seat.viewerSeat}: act at $turnIndex answered the wrong hand")
                assertEquals(
                    turn.actionSequence,
                    act.actionSequence,
                    "seat ${seat.viewerSeat}: act at $turnIndex answered the wrong decision point",
                )
            }
        }
    }

    @Test
    fun theRivalsCardsComeFromTheRivalsOwnFrames() {
        val seat0 = scripted.seats.single { it.viewerSeat == 0 }
        val seat1 = scripted.seats.single { it.viewerSeat == 1 }

        // Independently walk each seat's own Snapshot frames — not the production code's path —
        // so this test can catch the rival's hole cards being read from the wrong stream.
        val seat0OwnHands = ownHoleCardsByHand(seat0)
        val seat1OwnHands = ownHoleCardsByHand(seat1)

        assertEquals(seat1OwnHands, asMap(seat0.rivalHoleCards), "seat 0's rivalHoleCards should be seat 1's own cards, hand for hand")
        assertEquals(seat0OwnHands, asMap(seat1.rivalHoleCards), "seat 1's rivalHoleCards should be seat 0's own cards, hand for hand")

        for (hand in seat0.rivalHoleCards + seat1.rivalHoleCards) {
            assertEquals(2, hand.cards.size, "hand ${hand.handNumber} should show exactly two rival cards, was ${hand.cards}")
        }

        val handsPlayed = outcomeOf(seat0).handsPlayed
        val expected = (1..handsPlayed).toList()
        assertEquals(expected, seat0.rivalHoleCards.map { it.handNumber }, "seat 0's rivalHoleCards should run 1..$handsPlayed with no gap")
        assertEquals(expected, seat1.rivalHoleCards.map { it.handNumber }, "seat 1's rivalHoleCards should run 1..$handsPlayed with no gap")
    }

    @Test
    fun theScriptedDuelHasAShowdownAndAFoldWin() {
        for (seat in scripted.seats) {
            val rival = 1 - seat.viewerSeat
            val allHands = (1..outcomeOf(seat).handsPlayed).toSet()
            val reveals = handRevealsByHand(seat)
            val handsWithAnyReveal = reveals.keys
            val handsWithRivalRevealed = reveals.filterValues { rival in it }.keys
            val handsWithNoReveal = allHands - handsWithAnyReveal

            assertTrue(
                handsWithRivalRevealed.isNotEmpty(),
                "seat ${seat.viewerSeat}: no hand revealed the rival. " +
                    "Hands with a reveal: $handsWithAnyReveal. Hands with no reveal: $handsWithNoReveal",
            )
            assertTrue(
                handsWithNoReveal.isNotEmpty(),
                "seat ${seat.viewerSeat}: every hand had a reveal, so none ended by fold. " +
                    "Hands with a reveal: $handsWithAnyReveal. Hands with no reveal: $handsWithNoReveal",
            )
        }
    }

    @Test
    fun theDuelIsLongEnoughAndSmallEnough() {
        val outcome = outcomeOf(scripted.seats.single { it.viewerSeat == 0 })

        assertTrue(outcome.handsPlayed > 5, "duel played only ${outcome.handsPlayed} hands")
        assertTrue(outcome.winner != null, "duel ended in a draw")

        for (seat in scripted.seats) {
            assertTrue(seat.steps.size <= 1_200, "seat ${seat.viewerSeat} had ${seat.steps.size} steps, over the 1,200 ceiling")
        }
    }

    @Test
    fun theSameSeedWritesTheSameScript() {
        val again = scriptedDuel()

        assertEquals(scripted, again)
        for ((seatA, seatB) in scripted.seats.zip(again.seats)) {
            for ((stepA, stepB) in seatA.steps.zip(seatB.steps)) {
                assertEquals(stepA.frame, stepB.frame)
            }
        }
    }

    private fun isYourTurnStep(seat: ScriptedSeat, index: Int): Boolean {
        val step = seat.steps[index]
        return step.from == "server" && decodeServer(step.frame) is ServerMessage.YourTurn
    }

    private fun outcomeOf(seat: ScriptedSeat): DuelOutcome =
        (decodeServer(seat.steps.last().frame) as ServerMessage.DuelFinished).outcome

    /** For each hand, the first non-empty hole cards this seat's own `Snapshot`s show for itself. */
    private fun ownHoleCardsByHand(seat: ScriptedSeat): Map<Int, List<String>> {
        val hands = linkedMapOf<Int, List<String>>()
        for (step in seat.steps) {
            if (step.from != "server") continue
            val message = decodeServer(step.frame)
            if (message !is ServerMessage.Snapshot) continue
            val cards = message.view.seats[seat.viewerSeat].holeCards
            if (cards.isEmpty()) continue
            hands.putIfAbsent(message.view.handNumber, cards.map { it.toString() })
        }
        return hands
    }

    /**
     * For each hand, the seats named by a `HandRevealed` this seat's stream carries.
     *
     * A `HandRevealed`-bearing `Events` step is always immediately followed, in one seat's own
     * stream, by that same transition's `Snapshot` (`Addressed.kt`'s `broadcast`: events before
     * snapshot, one transition at a time), so the next step's `handNumber` names the hand.
     */
    private fun handRevealsByHand(seat: ScriptedSeat): Map<Int, List<Int>> {
        val hands = linkedMapOf<Int, MutableList<Int>>()
        seat.steps.forEachIndexed { index, step ->
            if (step.from != "server") return@forEachIndexed
            val message = decodeServer(step.frame)
            if (message !is ServerMessage.Events) return@forEachIndexed
            val revealedSeats = message.events.filterIsInstance<HandRevealed>().map { it.seat }
            if (revealedSeats.isEmpty()) return@forEachIndexed
            val snapshot = decodeServer(seat.steps[index + 1].frame) as ServerMessage.Snapshot
            hands.getOrPut(snapshot.view.handNumber) { mutableListOf() }.addAll(revealedSeats)
        }
        return hands
    }

    private fun asMap(hands: List<RivalHand>): Map<Int, List<String>> = hands.associate { it.handNumber to it.cards }

    private fun decodeServer(frame: String): ServerMessage = protocolJson.decodeFromString(ServerMessage.serializer(), frame)

    private fun decodeClientAct(frame: String): Act = protocolJson.decodeFromString(ClientMessage.serializer(), frame) as Act
}
