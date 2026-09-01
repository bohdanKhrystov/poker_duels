package duels.poker.server

import duels.poker.server.duel.Addressed
import duels.poker.server.protocol.ProtocolCodec
import duels.poker.server.room.Room
import duels.poker.server.session.ConnectionDirectory

/**
 * Delivers each [Addressed] frame to the [duels.poker.server.session.ConnectionWriter] of the
 * seat it names, and to no other.
 *
 * Every [Addressed] arrives already built for exactly one seat by the engine's projection layer
 * (`duels.poker.server.duel.Addressed`). This function does not inspect, rewrite, merge or
 * re-derive that content — it reads [Addressed.seat] and nothing else, and only decides *where*
 * the frame goes. Seat `0` names [Room.host], seat `1` names [Room.guest].
 *
 * A seat with no seated player — a [duels.poker.server.room.RoomState.WAITING] room has no
 * guest — a seated player with no writer currently registered in [connections] — a player
 * mid-reconnect has none — or a seated player whose connection is currently in a different room,
 * or in none at all, is **skipped silently**: [duels.poker.server.session.ConnectionDirectory.writerFor]
 * answers only for the room this frame is about (`ADR-0104` §1). All three are ordinary
 * conditions, never a reason to fail the caller, and never a reason to send that frame to the
 * *other* seat instead: silently dropping an undeliverable frame is safe, silently broadcasting it
 * to whoever is present is the hole-card leak this function exists to prevent.
 *
 * Each frame is encoded with [ProtocolCodec.encode] once, inside the loop, immediately before it
 * is sent to the writer that frame named — never hoisted out and reused across frames. Two seats'
 * frames are different values on purpose; encoding one and sending it to both would undo the
 * engine's projection layer.
 *
 * @param frames The frames to deliver, written in the order given.
 * @param room The room whose seating decides which player each seat names.
 * @param connections The directory used to look up each named player's live writer, if any.
 */
internal suspend fun deliver(frames: List<Addressed>, room: Room, connections: ConnectionDirectory) {
    for (frame in frames) {
        val player = when (frame.seat) {
            0 -> room.host
            1 -> room.guest
            else -> null
        } ?: continue
        val writer = connections.writerFor(player, room.code) ?: continue
        writer.send(ProtocolCodec.encode(frame.message))
    }
}
