package duels.poker.server.http

/** Default limit for recent duels when no limit is specified. */
public const val DEFAULT_DUEL_LIMIT: Int = 10

/** Maximum limit for recent duels. Clamping prevents denial-of-service attacks from unbounded queries. */
public const val MAX_DUEL_LIMIT: Int = 50

/**
 * Parses the `limit` query parameter into the number of duels to read.
 *
 * - Returns [DEFAULT_DUEL_LIMIT] when the parameter is absent (`null` argument).
 * - Returns `null` (reject) when the value is not a positive number or cannot be parsed.
 * - Clamps values above [MAX_DUEL_LIMIT] to the cap to keep clients that ask for too much working.
 *
 * Parsed as [Long], not [Int]: a value like "99999999999" that is larger than [Int.MAX_VALUE]
 * is clamped to [MAX_DUEL_LIMIT], not rejected as a syntax error.
 *
 * @param raw the raw limit parameter, typically from a query string
 * @return the number of duels to read, or `null` if the parameter is invalid
 */
public fun duelLimitOrNull(raw: String?): Int? {
    if (raw == null) return DEFAULT_DUEL_LIMIT
    val requested = raw.toLongOrNull() ?: return null
    if (requested <= 0) return null
    return requested.coerceAtMost(MAX_DUEL_LIMIT.toLong()).toInt()
}
