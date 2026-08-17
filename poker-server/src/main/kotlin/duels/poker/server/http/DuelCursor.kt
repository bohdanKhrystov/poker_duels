package duels.poker.server.http

import java.time.Instant
import java.util.Base64
import java.util.UUID

/**
 * One duel's place in the recent-duels list: the exact `(finishedAt, duelId)` tuple that
 * `RECENT_DUELS_SQL` orders by (`ORDER BY d.finished_at DESC, d.id DESC`).
 *
 * Opaque to clients — see [duelCursorOrNull] for the decoding half of the contract.
 */
public data class DuelCursor(val finishedAt: Instant, val duelId: UUID) {
    /**
     * The URL-safe, unpadded base64 string a client hands back in the `after` query parameter to
     * ask for the page following this cursor.
     *
     * URL-safe because the value travels in a query string; unpadded because `=` in a query
     * string is the character most often mangled on the way back.
     */
    public fun encoded(): String =
        Base64.getUrlEncoder().withoutPadding().encodeToString("$finishedAt|$duelId".toByteArray(Charsets.UTF_8))
}

/**
 * Parses the `after` query parameter into the cursor it names.
 *
 * Returns `null` for anything the server would not itself have produced: invalid base64, a
 * payload that does not split into exactly two parts, an instant or id that does not parse, or a
 * payload that parses but is not the *canonical* encoding of the [DuelCursor] it decodes to —
 * for example a differently-formatted but equal instant, an upper-cased id, or padded base64.
 * Two different strings must never name the same row, or a client could mint cursors the server
 * never issued.
 *
 * The parameter is not nullable: a missing query parameter means the newest page, and that
 * default is the caller's business, not this function's.
 *
 * @param raw the raw `after` parameter, typically from a query string
 * @return the cursor it names, or `null` if the parameter is invalid
 */
public fun duelCursorOrNull(raw: String): DuelCursor? {
    val decoded =
        try {
            Base64.getUrlDecoder().decode(raw)
        } catch (malformed: IllegalArgumentException) {
            return null
        }
    val parts = decoded.toString(Charsets.UTF_8).split("|")
    if (parts.size != 2) return null
    val finishedAt =
        try {
            Instant.parse(parts[0])
        } catch (unparseable: Exception) {
            return null
        }
    val duelId =
        try {
            UUID.fromString(parts[1])
        } catch (unparseable: Exception) {
            return null
        }
    return DuelCursor(finishedAt, duelId).takeIf { it.encoded() == raw }
}
