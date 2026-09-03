package duels.poker.server.room

import duels.poker.server.duel.Addressed

/**
 * The outcome of [RoomRegistry.expireTurnClocks] for one room whose turn clock ran out.
 *
 * Shaped like [JoinResult.Seated] and [Resumption]: a room, plus the frames this pass produced,
 * ready for the caller to address outward.
 *
 * @property room The room after its expiry was resolved: either the seat whose clock ran out
 *   was played as an ordinary decision, or — when both seats were gone — the room moved to
 *   [RoomState.ABANDONED].
 * @property outbound The frames the pass produced, if any. Empty when the room was abandoned
 *   instead: there is no duel left to hand frames about.
 */
public data class TurnClockExpiry(val room: Room, val outbound: List<Addressed>)
