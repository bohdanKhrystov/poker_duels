package duels.poker.server.http

import duels.poker.server.protocol.http.ProfileResponse
import duels.poker.server.session.DeviceId

/**
 * A port for reading player profiles and their coin balances.
 *
 * This port exists so the routes can be tested without a database, and so no route ever holds
 * a `DataSource` (`ADR-0011`). It returns the response type rather than a parallel domain type
 * because the answer's shape *is* the wire's shape; a second identical type would be a copy nobody
 * reads. **Nothing on this port creates anything** — an unknown device is `null`, and profile
 * creation happens on the socket handshake only (`ADR-0012`), so a crawler cannot mint rows.
 */
public fun interface ProfileReads {
    /**
     * Read a device's profile and balance.
     *
     * @param deviceId The device identifier to look up.
     * @return A profile response if the device is known, `null` otherwise. Does not create
     *   anything on an unknown device.
     */
    public suspend fun profileOf(deviceId: DeviceId): ProfileResponse?
}
