package duels.poker.server.duel

import duels.poker.engine.game.GameEvent
import duels.poker.engine.game.GameState
import duels.poker.server.protocol.Act

/**
 * Why an inbound [Act] must not reach the engine.
 *
 * `NOT_YOUR_TURN` names an attempt to act out of turn or to act for the other seat.
 * `STALE_FRAME` names a frame that no longer names the current decision point — most often a
 * replay of a frame the server already answered.
 */
public enum class ActRefusal { NOT_YOUR_TURN, STALE_FRAME }

/**
 * Decides, before [message] reaches `DefaultPokerEngine.handle`, whether it is a replay to
 * drop, an attempt to act out of turn or for the opponent, or a frame the engine may see.
 *
 * [seat] is the seat of the connection [message] arrived on, as established by the server —
 * nothing in [message] itself is trusted to say who is speaking. Per ADR-0002, the server is
 * authoritative, so `message.action.seat` is only ever checked against [seat], never adopted.
 *
 * Staleness is checked before whose turn it is. A replayed frame usually arrives while the
 * *other* seat is on turn; checking turn first would answer it with `NOT_YOUR_TURN`, making a
 * duplicate frame visible to the very player who sent it. Checking staleness first keeps a
 * replay silent, as `ADR-0002` requires: `STALE_FRAME` is dropped, `NOT_YOUR_TURN` is answered.
 *
 * @param state The current game state.
 * @param handEvents Every event of the live hand so far.
 * @param seat The seat of the connection the frame arrived on: 0 or 1.
 * @param message The inbound action attempt.
 * @return The refusal that applies, or `null` if the engine may see the frame.
 */
public fun guard(
    state: GameState,
    handEvents: List<GameEvent>,
    seat: Int,
    message: Act,
): ActRefusal? {
    require(seat in 0..1) { "seat must be 0 or 1, was $seat" }

    return when {
        message.handNumber != state.handNumber -> ActRefusal.STALE_FRAME
        message.actionSequence != decisionPointOf(handEvents)?.sequence -> ActRefusal.STALE_FRAME
        state.seatToAct != seat -> ActRefusal.NOT_YOUR_TURN
        message.action.seat != seat -> ActRefusal.NOT_YOUR_TURN
        else -> null
    }
}
