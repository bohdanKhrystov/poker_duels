package duels.poker.server.session

import duels.poker.server.room.RoomCode

/**
 * One instance per connection: a per-connection record of which room, if any, it has entered.
 *
 * This object is shared by reference between the socket loop that writes [code] and the
 * directory that reads it when resolving delivery targets. `code` is `@Volatile` because it
 * is written by the socket's single coroutine and read by others — the ticker's sweep and the
 * other seat's socket — and without it a delivery decision may read a stale room when a
 * connection has moved. See `ADR-0104` §2: *"getting it wrong fails invisibly and never on
 * one thread"*.
 */
public class RoomMembership {
    /** The code of the room this connection has entered, or `null` before it has entered one. */
    @Volatile
    public var code: RoomCode? = null
}
