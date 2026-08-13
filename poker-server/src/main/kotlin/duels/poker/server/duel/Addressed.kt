package duels.poker.server.duel

import duels.poker.engine.duel.DuelOutcome
import duels.poker.engine.game.GameEvent
import duels.poker.engine.game.GameState
import duels.poker.engine.game.PlayerView
import duels.poker.engine.game.revealedSeats
import duels.poker.engine.game.visibleTo
import duels.poker.server.protocol.ServerMessage

/**
 * An outbound frame addressed to one seat, built by the engine's projection layer.
 *
 * Every card decision is made by [PlayerView.of] and [visibleTo] — this type contains no card
 * logic, no reference to hole cards, and no filtering other than calling the engine's own redaction
 * layer.
 *
 * @property seat The recipient: 0 or 1.
 * @property message The frame the recipient is entitled to see.
 */
public data class Addressed(
    val seat: Int,
    val message: ServerMessage,
) {
    init {
        require(seat in 0..1) { "seat must be 0 or 1, was $seat" }
    }
}

/**
 * Turns one engine transition into the [Addressed] frames each seat is entitled to.
 *
 * Every card decision is made by [PlayerView.of], [visibleTo], and [revealedSeats].
 * No events are constructed, filtered, or redacted here — all card logic lives in the engine's
 * projection layer.
 *
 * @param state The current game state after the transition.
 * @param newEvents The events the transition just produced.
 * @param handEvents Every event of the live hand so far, `newEvents` included. Both are needed
 *                   because the `Events` frame carries only what is new, while `revealed` must
 *                   be computed over the whole hand.
 * @return The frames each seat is entitled to see, in order: seat 0's frames, then seat 1's,
 *         and within each seat, events (if any) before snapshot. An empty [visibleTo] result
 *         emits no `Events` frame — a seat is never sent an empty `Events`.
 */
public fun broadcast(
    state: GameState,
    newEvents: List<GameEvent>,
    handEvents: List<GameEvent>,
): List<Addressed> {
    val result = mutableListOf<Addressed>()
    val revealed = revealedSeats(handEvents)

    for (seat in 0..1) {
        // Emit Events frame if there are visible new events for this seat.
        val visibleEvents = visibleTo(newEvents, seat)
        if (visibleEvents.isNotEmpty()) {
            result.add(Addressed(seat, ServerMessage.Events(visibleEvents)))
        }

        // Always emit Snapshot frame, which is the authoritative last word on state.
        val view = PlayerView.of(state, seat, revealed)
        result.add(Addressed(seat, ServerMessage.Snapshot(view)))
    }

    return result
}

/**
 * Builds the finished-duel frames for each seat.
 *
 * Both seats receive the same [outcome]. A `DuelOutcome` is a projection of facts both seats
 * are entitled to know: the winning seat (or null for a draw), the hand count, and the final stacks.
 * The final stacks are already in the last `Snapshot` each seat received, so there is nothing here
 * to redact. This file is where that judgement is allowed to be made (ADR-0017): the finished-duel
 * frame is a projection, filtered like everything else through `Addressed.kt`.
 *
 * @param outcome The duel's outcome.
 * @return Two frames, addressed to seats 0 and 1, each carrying [ServerMessage.DuelFinished] with
 *         the same outcome.
 */
public fun finishedFrames(outcome: DuelOutcome): List<Addressed> =
    listOf(
        Addressed(0, ServerMessage.DuelFinished(outcome)),
        Addressed(1, ServerMessage.DuelFinished(outcome)),
    )
