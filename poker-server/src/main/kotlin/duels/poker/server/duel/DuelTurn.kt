package duels.poker.server.duel

import duels.poker.engine.game.ActionOn
import duels.poker.engine.game.GameEvent
import duels.poker.engine.game.GameState
import duels.poker.engine.game.legalActions
import duels.poker.server.protocol.ServerMessage

/**
 * Finds the decision point in a hand's events.
 *
 * The decision point is the event sequence number at which the engine prompted the player
 * on turn to act. It is found by looking for the last [ActionOn] in the hand, since the
 * engine emits one `ActionOn` per decision it makes, in order.
 *
 * @param handEvents The events of the live hand so far.
 * @return The last `ActionOn` in the hand, or `null` if no seat has yet been prompted.
 */
public fun decisionPointOf(handEvents: List<GameEvent>): ActionOn? {
    return handEvents.filterIsInstance<ActionOn>().lastOrNull()
}

/**
 * Builds a frame prompting the seat on turn to act.
 *
 * This is the frame that tells a client "it is your turn; here are the actions you may take
 * and the sequence number you must echo back when you respond".
 *
 * @param state The current game state.
 * @param handEvents Every event of the live hand so far.
 * @return An [Addressed] frame prompting the seat on turn, or `null` if the hand is over
 *         or no seat is currently to act.
 * @throws IllegalStateException if the last `ActionOn` names a seat other than the one
 *         on turn, which indicates a server bug.
 */
public fun turnFor(state: GameState, handEvents: List<GameEvent>): Addressed? {
    val seatToAct = state.seatToAct ?: return null
    val decisionPoint = decisionPointOf(handEvents) ?: return null

    check(decisionPoint.seat == seatToAct) {
        "The last ActionOn names seat ${decisionPoint.seat}, but state.seatToAct is $seatToAct"
    }

    return Addressed(
        seatToAct,
        ServerMessage.YourTurn(
            handNumber = state.handNumber,
            actionSequence = decisionPoint.sequence,
            legalActions = legalActions(state),
        ),
    )
}

/**
 * Builds all frames for one state transition.
 *
 * Combines the state snapshot broadcast to each seat with the turn prompt to the seat
 * on turn, in order: broadcast first (state and new events), then the turn prompt (if any).
 * This ensures the client can process the game facts before it is asked to act on them.
 *
 * @param state The current game state after the transition.
 * @param newEvents The events the transition just produced.
 * @param handEvents Every event of the live hand so far, `newEvents` included.
 * @return The frames each seat is entitled to see: broadcast frames, then the turn prompt (if any).
 */
public fun framesFor(
    state: GameState,
    newEvents: List<GameEvent>,
    handEvents: List<GameEvent>,
): List<Addressed> {
    return broadcast(state, newEvents, handEvents) + listOfNotNull(turnFor(state, handEvents))
}
