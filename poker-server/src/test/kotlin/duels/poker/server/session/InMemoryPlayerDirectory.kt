package duels.poker.server.session

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * An in-memory implementation of PlayerDirectory for testing.
 *
 * Profiles are stored in a thread-safe map and minted on first access.
 * This implementation is not shipped and is intended for tests only.
 */
internal class InMemoryPlayerDirectory : PlayerDirectory {
    private val players = ConcurrentHashMap<DeviceId, Player>()
    private val counter = AtomicInteger(0)

    /**
     * The current number of profiles stored in this directory.
     */
    internal val profileCount: Int
        get() = players.size

    override suspend fun resolve(deviceId: DeviceId): Player {
        return players.computeIfAbsent(deviceId) { id ->
            Player(
                id = PlayerId("player-${counter.incrementAndGet()}"),
                deviceId = id,
            )
        }
    }

    /**
     * Find the player profile bound to a device id, without creating one.
     *
     * A plain map read: an unknown device id answers `null` and leaves [profileCount] unchanged.
     */
    override suspend fun findOrNull(deviceId: DeviceId): Player? = players[deviceId]
}
