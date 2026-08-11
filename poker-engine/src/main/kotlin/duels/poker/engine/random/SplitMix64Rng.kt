package duels.poker.engine.random

// The published SplitMix64 constants, written as Kotlin signed hex literals. Comments give the
// unsigned form for anyone checking this against the reference algorithm.
private const val GOLDEN_GAMMA = -0x61c8864680b583ebL // 0x9E3779B97F4A7C15
private const val MIX_A = -0x40a7b892e31b1a47L // 0xBF58476D1CE4E5B9
private const val MIX_B = -0x6b2fb644ecceee15L // 0x94D049BB133111EB
private const val VALUE_SHIFT = 33

/**
 * SplitMix64, transliterated from the published reference algorithm.
 *
 * This is a **durable contract**: the algorithm, the constants above and the rejection rule in
 * [nextInt] must never change. A match is only reproducible from its seed for as long as they
 * stay exactly as written here — altering any of them invalidates every stored replay in the
 * project, forever. Do not "simplify" or "optimize" this file.
 *
 * The generator is immutable and stateless from the caller's point of view: [nextLong] and
 * [nextInt] never mutate the receiver, they return the generator that follows alongside the
 * drawn value, so the same instance can be drawn from repeatedly with the same result. This
 * class deliberately does not depend on the platform random source.
 */
public data class SplitMix64Rng(private val state: Long) : Rng {

    /** The next raw 64-bit value in the sequence, and the generator that follows it. */
    public fun nextLong(): LongDraw {
        val next = state + GOLDEN_GAMMA
        var z = next
        z = (z xor (z ushr 30)) * MIX_A
        z = (z xor (z ushr 27)) * MIX_B
        return LongDraw(z xor (z ushr 31), SplitMix64Rng(next))
    }

    override fun nextInt(bound: Int): Rng.Draw {
        require(bound > 0) { "bound must be positive, was $bound" }
        var source = this
        while (true) {
            val draw = source.nextLong()
            val bits = (draw.value ushr VALUE_SHIFT).toInt() // 31 bits, never negative
            val value = bits % bound
            source = draw.next
            // Rejection sampling: retry when the last, short block of the range was hit,
            // because keeping it would make small values fractionally more likely.
            if (bits - value + (bound - 1) >= 0) return Rng.Draw(value, source)
        }
    }

    /** The result of a raw draw: the [value] produced and the [next] generator to draw from. */
    public data class LongDraw(val value: Long, val next: SplitMix64Rng)
}
