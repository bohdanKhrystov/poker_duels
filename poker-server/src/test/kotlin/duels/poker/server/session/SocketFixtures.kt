package duels.poker.server.session

import duels.poker.server.config.ServerConfig
import duels.poker.server.room.RandomRoomCodeSource
import duels.poker.server.room.RoomRegistry
import duels.poker.server.time.SystemClock
import java.util.ArrayDeque

/**
 * Defaults [maxFrameLength] and [maxFrameNestingDepth] to the codec's own defaults, so every
 * existing socket test keeps its current behaviour without naming these two fields.
 */
internal fun testDeps(
    directory: PlayerDirectory = InMemoryPlayerDirectory(),
    deviceIds: DeviceIdSource = RandomDeviceIdSource(),
    sessions: SessionRegistry = SessionRegistry(),
    rooms: RoomRegistry = RoomRegistry(RandomRoomCodeSource(), SystemClock),
    connections: ConnectionDirectory = ConnectionDirectory(),
    maxFrameLength: Int = ServerConfig.DEFAULT_MAX_FRAME_LENGTH,
    maxFrameNestingDepth: Int = ServerConfig.DEFAULT_MAX_FRAME_NESTING_DEPTH,
): SocketDependencies =
    SocketDependencies(directory, deviceIds, sessions, rooms, connections, maxFrameLength, maxFrameNestingDepth)

internal fun fixedDeviceIds(vararg ids: String): DeviceIdSource {
    val queue = ArrayDeque(ids.toList())
    return DeviceIdSource {
        DeviceId(queue.pollFirst() ?: error("fixedDeviceIds ran out of ids"))
    }
}
