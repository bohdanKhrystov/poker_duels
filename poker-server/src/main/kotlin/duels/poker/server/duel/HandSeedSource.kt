package duels.poker.server.duel

import java.security.SecureRandom
import java.util.Random

/**
 * A port for drawing the seed of each hand from a secure random source.
 *
 * The seed is recorded in the hand's `HandLog` but never leaves the server while the duel is
 * live (`ADR-0002`).
 *
 * Randomness from the engine's `Rng` is not suitable here: the engine's randomness is
 * deterministic by design so that a duel replays identically from one seed, and a value that
 * replays is exactly what a hand seed must not be.
 */
public fun interface HandSeedSource {
    /**
     * Draw a new hand seed.
     *
     * @return A random long value to seed the shuffle of the next hand.
     */
    public fun newHandSeed(): Long
}

/**
 * An implementation of [HandSeedSource] that draws from a provided or default secure random source.
 *
 * @param random The random number generator. Defaults to [SecureRandom] for production.
 */
public class SecureHandSeedSource(private val random: Random = SecureRandom()) : HandSeedSource {
    /**
     * Whether this source is drawing from [SecureRandom].
     *
     * This property exists so that a test can state, rather than assume, that the default
     * instance draws from a cryptographically secure source. Tests may inject a deterministic
     * [java.util.Random] to verify reproducibility without sacrificing security in production.
     */
    public val isSecure: Boolean
        get() = random is SecureRandom

    override fun newHandSeed(): Long = random.nextLong()
}
