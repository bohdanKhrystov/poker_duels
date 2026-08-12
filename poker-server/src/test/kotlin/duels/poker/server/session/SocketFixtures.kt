package duels.poker.server.session

import java.util.ArrayDeque

internal fun testDeps(
    directory: PlayerDirectory = InMemoryPlayerDirectory(),
    deviceIds: DeviceIdSource = RandomDeviceIdSource(),
    sessions: SessionRegistry = SessionRegistry(),
): SocketDependencies = SocketDependencies(directory, deviceIds, sessions)

internal fun fixedDeviceIds(vararg ids: String): DeviceIdSource {
    val queue = ArrayDeque(ids.toList())
    return DeviceIdSource {
        DeviceId(queue.pollFirst() ?: error("fixedDeviceIds ran out of ids"))
    }
}
