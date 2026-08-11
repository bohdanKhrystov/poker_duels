package duels.poker.engine.random

/**
 * The engine's single source of randomness.
 *
 * Every random decision in a duel — shuffling, dealing, anything that needs an unpredictable but
 * reproducible choice — flows through an [Rng] rather than through the platform random source.
 * An [Rng] is immutable: drawing a value never mutates the receiver, it returns the generator
 * that follows it alongside the drawn value. This is what makes "same seed, same actions ⇒
 * byte-identical game" possible — the whole sequence is a pure function of the seed.
 */
public interface Rng {
    /** A uniform value in `0 until bound`, together with the generator that follows it. */
    public fun nextInt(bound: Int): Draw

    /** The result of a draw: the [value] produced and the [next] generator to draw from. */
    public data class Draw(val value: Int, val next: Rng)
}
