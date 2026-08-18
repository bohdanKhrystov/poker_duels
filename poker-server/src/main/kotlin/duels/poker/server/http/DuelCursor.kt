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
     * ask for the page following this cursor, minted under [filter].
     *
     * URL-safe because the value travels in a query string; unpadded because `=` in a query
     * string is the character most often mangled on the way back. The payload is three parts —
     * `finishedAt`, `duelId`, and `filter`'s fingerprint — so a cursor minted under one filter and
     * replayed under another is refused by [duelCursorOrNull] instead of silently answering the
     * wrong set of rows. The fingerprint is not a field of [DuelCursor] itself: a cursor is a
     * position, and the filter is context supplied at both ends of the encoding (`ADR-0057` §1).
     * The separator stays safe by construction — an `Instant`, a `UUID` and unpadded base64url
     * all cannot contain `|`.
     *
     * @param filter the filter this cursor is being handed out under
     */
    public fun encoded(filter: DuelFilter): String =
        Base64.getUrlEncoder().withoutPadding()
            .encodeToString("$finishedAt|$duelId|${filter.fingerprint()}".toByteArray(Charsets.UTF_8))
}

/**
 * Parses the `after` query parameter into the cursor it names, valid only under [filter].
 *
 * Returns `null` for anything the server would not itself have produced under [filter]: invalid
 * base64, a payload that does not split into exactly three parts, an instant or id that does not
 * parse, or a payload that parses but is not the *canonical* encoding of the [DuelCursor] it
 * decodes to under [filter] — for example a differently-formatted but equal instant, an
 * upper-cased id, padded base64, or a fingerprint drawn from a different filter. Two different
 * strings must never name the same row under the same filter, or a client could mint cursors the
 * server never issued; and the same string must never name a row under two different filters, or
 * a cursor minted for one read could be replayed against another.
 *
 * The parameter is not nullable: a missing query parameter means the newest page, and that
 * default is the caller's business, not this function's.
 *
 * @param raw the raw `after` parameter, typically from a query string
 * @param filter the filter this cursor is being decoded under
 * @return the cursor it names, or `null` if the parameter is invalid or was minted under a
 *   different filter
 */
public fun duelCursorOrNull(raw: String, filter: DuelFilter): DuelCursor? {
    val decoded =
        try {
            Base64.getUrlDecoder().decode(raw)
        } catch (malformed: IllegalArgumentException) {
            return null
        }
    val parts = decoded.toString(Charsets.UTF_8).split("|")
    if (parts.size != 3) return null
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
    return DuelCursor(finishedAt, duelId).takeIf { it.encoded(filter) == raw }
}
