package duels.poker.server.session

import java.security.SecureRandom
import java.util.Base64
import java.util.Random

private const val ID_BYTES = 16

/**
 * A port for issuing opaque device identifiers drawn from a random source.
 *
 * The client never chooses a device id; if it presents none, the server issues one. Once issued,
 * a device id is the sole authentication token — there is no login flow in v0.1 — so it must
 * come from a cryptographically secure source and be long enough that guessing is not practical.
 *
 * Randomness from the engine's `Rng` is not suitable here: the engine's randomness is
 * deterministic by design so that a duel replays identically from one seed, and a value that
 * replays is exactly what an identifier must not be.
 */
public fun interface DeviceIdSource {
    /**
     * Issue a new opaque device identifier.
     *
     * @return A device identifier, 22 URL-safe characters encoding 128 bits of entropy.
     */
    public fun newDeviceId(): DeviceId
}

/**
 * An implementation of [DeviceIdSource] that draws from a provided or default secure random source.
 *
 * @param random The random number generator. Defaults to [SecureRandom] for production.
 */
public class RandomDeviceIdSource(private val random: Random = SecureRandom()) : DeviceIdSource {
    /**
     * Whether this source is drawing from [SecureRandom].
     *
     * This property exists so that a test can state, rather than assume, that the default
     * instance draws from a cryptographically secure source. Tests may inject a deterministic
     * [java.util.Random] to verify reproducibility without sacrificing security in production.
     */
    public val isSecure: Boolean
        get() = random is SecureRandom

    override fun newDeviceId(): DeviceId {
        val bytes = ByteArray(ID_BYTES)
        random.nextBytes(bytes)
        return DeviceId(Base64.getUrlEncoder().withoutPadding().encodeToString(bytes))
    }
}
