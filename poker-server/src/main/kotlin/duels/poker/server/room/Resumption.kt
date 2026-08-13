package duels.poker.server.room

import duels.poker.server.duel.Addressed

/**
 * The outcome of [RoomRegistry.resume]: the seat a returning player holds, and the frames it is
 * owed to pick up where it left off.
 *
 * Shaped exactly like [JoinResult.Seated]: a room, plus the frames this call produced, ready for
 * the caller to address outward.
 *
 * @property room The room after the resume: the returning seat's disconnect grace window
 *   (`ADR-0013`) cleared, and its activity clock touched.
 * @property seat The seat the resuming player holds: `0` for the host, `1` for the guest.
 * @property outbound The frames [seat] is entitled to see right now — never another seat's.
 */
public data class Resumption(val room: Room, val seat: Int, val outbound: List<Addressed>)
