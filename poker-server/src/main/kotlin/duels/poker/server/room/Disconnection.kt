package duels.poker.server.room

import duels.poker.server.duel.Addressed

/**
 * The outcome of [RoomRegistry.disconnect]: the room after the disconnect grace window was
 * started, and the frames it produced.
 *
 * Shaped exactly like [Resumption] and [GraceExpiry]: a room, plus the frames this call produced,
 * ready for the caller to address outward.
 *
 * @property room The room after the transition: the disconnected seat's grace window
 *   (`ADR-0013`) started at a deadline computed from the configured timeout, and nothing else.
 * @property outbound The frames the disconnect produced. Currently always empty — a later ticket
 *   will emit [duels.poker.server.protocol.ServerMessage.OpponentPresence] here when a player's
 *   connection is gone, addressed to the seat that is still connected.
 */
public data class Disconnection(val room: Room, val outbound: List<Addressed>)
