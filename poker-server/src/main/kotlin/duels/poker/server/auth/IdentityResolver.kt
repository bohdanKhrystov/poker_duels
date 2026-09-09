package duels.poker.server.auth

import duels.poker.server.session.DeviceId
import duels.poker.server.session.PlayerDirectory
import duels.poker.server.session.PlayerId

/**
 * Who a request is, as decided by [IdentityResolver].
 *
 * Five answers, not three, because the socket and HTTP act on two of them differently:
 * [UnknownDevice] and [Anonymous] are a `401` over HTTP and a mint-and-resolve on the socket, per
 * `ADR-0027` §4. `IdentityResolver` itself never creates a profile — which of the two behaviours
 * applies is entirely a caller's decision.
 */
public sealed interface Identity {
    /**
     * A valid session names this player. Carries no device id: a session that wins is not merely
     * unaffected by the device beside it, it never reads one.
     */
    public data class Session(val playerId: PlayerId) : Identity

    /** No session was presented; this device id resolved to an existing player. */
    public data class Device(val playerId: PlayerId, val deviceId: DeviceId) : Identity

    /** No session was presented; this device id resolves to no existing player. */
    public data class UnknownDevice(val deviceId: DeviceId) : Identity

    /** A session token was presented and is unknown or expired — indistinguishably. */
    public data object Refused : Identity

    /** Neither a session token nor a device id was presented. */
    public data object Anonymous : Identity
}

/**
 * Decides who a request is, per `ADR-0027` §4: the one place the session-versus-device
 * precedence rule is written, so the rule has exactly one behaviour instead of one per entry
 * point.
 *
 * A session token, if presented, is verified and wins outright — a device id presented beside it
 * is not read, not validated and not compared. An invalid token answers [Identity.Refused], never
 * a fall back to the device: a silent downgrade from signed in as A to anonymous B would let a
 * player win a coin into an account they believe they are not using, and it is the one failure a
 * player can neither detect nor undo. Only when no token is presented at all does the device id
 * apply, exactly as it does today.
 *
 * This resolver never creates a profile. Minting one on an unknown device id is a caller's
 * decision — the socket mints, HTTP refuses — so this class hands back a value each entry point
 * acts on rather than making that choice itself.
 */
public class IdentityResolver(private val sessions: AuthSessions, private val players: PlayerDirectory) {
    /**
     * Resolves the identity behind [token] and [deviceId], per `ADR-0027` §4's precedence.
     *
     * @param token The session token presented, or `null` if none was.
     * @param deviceId The device id presented, or `null` if none was.
     * @return The resolved [Identity].
     */
    public suspend fun resolve(token: SessionToken?, deviceId: DeviceId?): Identity {
        if (token != null) {
            val sessionPlayerId = sessions.playerOf(token) ?: return Identity.Refused
            return Identity.Session(sessionPlayerId)
        }
        if (deviceId == null) {
            return Identity.Anonymous
        }
        val player = players.findOrNull(deviceId)
        return if (player != null) Identity.Device(player.id, deviceId) else Identity.UnknownDevice(deviceId)
    }
}
