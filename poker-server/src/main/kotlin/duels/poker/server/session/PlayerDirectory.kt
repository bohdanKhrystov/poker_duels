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
public interface PlayerDirectory {
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

    /**
     * Find the player profile bound to a device id, without creating one.
     *
     * `resolve` mints a profile on first contact, which is correct for a socket's first
     * `Hello` but wrong for an HTTP route: a crawler hitting an endpoint that resolves
     * identity must not be able to mint a row for every device id it tries. This is the read
     * identity resolution outside the socket needs — one that can answer "no such device"
     * instead of creating an answer.
     *
     * @param deviceId The device identifier to look up.
     * @return The player profile for this device, or `null` if none exists.
     */
    public suspend fun findOrNull(deviceId: DeviceId): Player?
}
