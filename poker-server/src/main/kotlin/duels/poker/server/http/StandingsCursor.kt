package duels.poker.server.http

import duels.poker.server.season.Season
import java.time.Instant
import java.util.Base64
import java.util.UUID

/**
 * One standing's place in a standings walk: the exact `(asOf, coins, playerId)` tuple that
 * `STANDINGS_SQL` orders by (`ORDER BY coins DESC, player_id DESC`), pinned to the instant
 * the walk began per ADR-0066 §2.
 *
 * Opaque to clients — see [standingsCursorOrNull] for the decoding half of the contract.
 */
public data class StandingsCursor(val asOf: Instant, val coins: Int, val playerId: UUID) {
    /**
     * The URL-safe, unpadded base64 string a client hands back in the `after` query parameter to
     * ask for the page following this cursor.
     *
     * URL-safe because the value travels in a query string; unpadded because `=` in a query
     * string is the character most often mangled on the way back. The payload is three parts —
     * `asOf`, `coins`, and `playerId` — so a cursor is the row it names and nothing else. The
     * separator stays safe by construction — an `Instant`, an `Int`, and a `UUID` cannot contain
     * `|`.
     */
    public fun encoded(): String =
        Base64.getUrlEncoder().withoutPadding()
            .encodeToString("$asOf|$coins|$playerId".toByteArray(Charsets.UTF_8))
}

/**
 * Parses the `after` query parameter into the cursor it names, valid only within [season].
 *
 * Returns `null` for anything the server would not itself have produced within [season]: invalid
 * base64, a payload that does not split into exactly three parts, an instant, coins value, or id
 * that does not parse, or a payload that parses but is not the *canonical* encoding of the
 * [StandingsCursor] it decodes to — for example a differently-formatted but equal instant, an
 * upper-cased id, or padded base64. A cursor is a consistency check, never an authorisation
 * control per ADR-0066 §7 — the same page renders for every reader.
 *
 * The parameter is not nullable: a missing query parameter means the newest page, and that
 * default is the caller's business, not this function's.
 *
 * @param raw the raw `after` parameter, typically from a query string
 * @param season the season this cursor is being decoded within
 * @return the cursor it names, or `null` if the parameter is invalid or names a cutoff outside
 *   the season
 */
public fun standingsCursorOrNull(raw: String, season: Season): StandingsCursor? {
    val decoded =
        try {
            Base64.getUrlDecoder().decode(raw)
        } catch (malformed: IllegalArgumentException) {
            return null
        }
    val parts = decoded.toString(Charsets.UTF_8).split("|")
    if (parts.size != 3) return null
    val asOf =
        try {
            Instant.parse(parts[0])
        } catch (unparseable: Exception) {
            return null
        }
    val coins =
        try {
            parts[1].toInt()
        } catch (unparseable: Exception) {
            return null
        }
    val playerId =
        try {
            UUID.fromString(parts[2])
        } catch (unparseable: Exception) {
            return null
        }
    if (!season.contains(asOf)) return null
    return StandingsCursor(asOf, coins, playerId).takeIf { it.encoded() == raw }
}
