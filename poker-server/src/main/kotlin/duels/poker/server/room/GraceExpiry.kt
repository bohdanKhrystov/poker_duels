package duels.poker.server.room

import duels.poker.server.duel.Addressed

/**
 * The outcome of [RoomRegistry.expireGracePeriods] for one room whose disconnect grace window
 * (`ADR-0013`) ran out.
 *
 * Shaped like [JoinResult.Seated] and [Resumption]: a room, plus the frames this pass produced,
 * ready for the caller to address outward.
 *
 * @property room The room after its expiry was resolved: either the seat whose window ran out
 *   folded as an ordinary action, or — when both seats were gone — the room moved to
 *   [RoomState.ABANDONED].
 * @property outbound The frames the fold produced, if any. Empty when the room was abandoned
 *   instead: there is no duel left to hand frames about.
 */
public data class GraceExpiry(val room: Room, val outbound: List<Addressed>)
