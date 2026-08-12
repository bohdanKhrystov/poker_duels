package duels.poker.server.room

/**
 * A room code is a type with a shape, not a `String`: eight characters from one unambiguous
 * alphabet, parsed leniently from what a human types and stored strictly.
 *
 * The alphabet is Crockford base32: the digits plus the uppercase letters **excluding `I`, `L`,
 * `O` and `U`**, so a code read aloud or typed from a link cannot be misheard as another code.
 * Length 8 over 32 symbols is 40 bits — that is the entropy budget for guessing a code.
 *
 * @param value The room code string, must be exactly [LENGTH] characters all drawn from [ALPHABET].
 * @throws IllegalArgumentException if the code is not well-formed.
 */
@JvmInline
public value class RoomCode(public val value: String) {
    init {
        require(value.length == LENGTH && value.all { it in ALPHABET }) {
            "room code must be exactly $LENGTH characters from the Crockford base32 alphabet (0-9, A-Z excluding I, L, O, U)"
        }
    }

    public companion object {
        /** The length of a room code in characters. */
        public const val LENGTH: Int = 8

        /** The Crockford base32 alphabet: unambiguous characters that survive being read aloud. */
        public const val ALPHABET: String = "0123456789ABCDEFGHJKMNPQRSTVWXYZ"

        /**
         * Parse a room code leniently from human input.
         *
         * Trims the input, uppercases it, and strips hyphens and spaces. Returns `null` if
         * the result is still not a valid code. Never throws.
         *
         * @param raw The raw input string.
         * @return A valid [RoomCode] if parsing succeeds, or `null` if it does not.
         */
        public fun parse(raw: String): RoomCode? {
            val cleaned = raw.trim().uppercase().replace("-", "").replace(" ", "")
            return try {
                RoomCode(cleaned)
            } catch (e: IllegalArgumentException) {
                null
            }
        }
    }
}
