package duels.poker.server.session

/**
 * A device identifier that uniquely identifies a client across sessions.
 *
 * @param value The device identifier string, must not be blank.
 */
@JvmInline
public value class DeviceId(public val value: String) {
    init {
        require(value.isNotBlank()) { "a device id must not be blank" }
    }
}

/**
 * A player identifier uniquely identifying a profile.
 *
 * @param value The player identifier string.
 */
@JvmInline
public value class PlayerId(public val value: String)

/**
 * A player profile, bound to a device id.
 *
 * @param id The unique player identifier.
 * @param deviceId The device identifier this profile is bound to.
 */
public data class Player(val id: PlayerId, val deviceId: DeviceId)

/**
 * A port that resolves a device id to a player profile.
 *
 * Implementations must be idempotent: the same `DeviceId` always returns a `Player`
 * with the same `PlayerId`.
 */
public fun interface PlayerDirectory {
    /**
     * Resolve a device id to a player profile.
     *
     * This operation is idempotent: calling with the same device id always returns
     * a player with the same id.
     *
     * @param deviceId The device identifier to resolve.
     * @return The player profile for this device.
     */
    public suspend fun resolve(deviceId: DeviceId): Player
}
