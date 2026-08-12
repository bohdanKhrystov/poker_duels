package duels.poker.server.room

import java.security.SecureRandom
import java.util.Random

/**
 * A port for minting new [RoomCode]s from a random source.
 *
 * A room code is an invite: whoever holds it can take the open seat in that room. Whether that
 * alone is the right authorisation model, and whether join attempts need rate limiting, is
 * `DEC-012` — this port neither answers nor depends on that decision.
 *
 * Randomness from the engine's `Rng` is not suitable here: the engine's randomness is
 * deterministic by design so that a duel replays identically from one seed, and a value that
 * replays is exactly what an invite code must not be.
 */
public fun interface RoomCodeSource {
    /**
     * Mint a new room code.
     *
     * @return A [RoomCode] of [RoomCode.LENGTH] characters, each drawn independently from
     *   [RoomCode.ALPHABET].
     */
    public fun newRoomCode(): RoomCode
}

/**
 * An implementation of [RoomCodeSource] that draws from a provided or default secure random source.
 *
 * [RoomCode.ALPHABET] has exactly 32 symbols, a power of two, so drawing each character with
 * `random.nextInt(32)` is unbiased: every symbol has exactly a 1/32 chance regardless of the
 * generator's internal range. A modulo over a non-power-of-two alphabet would quietly skew the
 * codes toward the low end of the alphabet; that failure mode does not exist here.
 *
 * A code has [RoomCode.LENGTH] characters over a 32-symbol alphabet, so the entropy is
 * 32^8 = 2^40 ≈ 1.1 × 10^12 possible codes.
 *
 * @param random The random number generator. Defaults to [SecureRandom] for production.
 */
public class RandomRoomCodeSource(private val random: Random = SecureRandom()) : RoomCodeSource {
    /**
     * Whether this source is drawing from [SecureRandom].
     *
     * This property exists so that a test can state, rather than assume, that the default
     * instance draws from a cryptographically secure source. Tests may inject a deterministic
     * [java.util.Random] to verify reproducibility without sacrificing security in production.
     */
    public val isSecure: Boolean
        get() = random is SecureRandom

    override fun newRoomCode(): RoomCode {
        val code = CharArray(RoomCode.LENGTH) { RoomCode.ALPHABET[random.nextInt(32)] }
        return RoomCode(String(code))
    }
}
