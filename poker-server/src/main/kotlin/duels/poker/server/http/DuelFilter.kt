package duels.poker.server.http

import duels.poker.server.protocol.http.DuelOutcomeLabel
import java.security.MessageDigest
import java.text.Normalizer
import java.util.Base64

/**
 * A filter for history reads specifying which duels a read wants.
 *
 * A `null` outcome means the read will not be narrowed by outcome; a `null` opponent means the
 * read will not be narrowed by opponent. [NONE] is the filter every unfiltered read passes.
 *
 * The `opponent` field exists from the start and is declared so the type is not re-shaped twice;
 * it is not constructed non-null yet. See `TASK-040902` for the rules a search term obeys and
 * `TASK-040908` for what first fills the field.
 *
 * @property outcome The outcome to filter by, or `null` to not narrow by outcome.
 * @property opponent The opponent to filter by, or `null` to not narrow by opponent.
 */
public data class DuelFilter(val outcome: DuelOutcomeLabel?, val opponent: String?) {
    public companion object {
        /**
         * The filter that narrows neither axis — the read the endpoint already performs when no
         * filter parameters are sent.
         */
        public val NONE: DuelFilter = DuelFilter(outcome = null, opponent = null)
    }
}

/**
 * Parses the raw `outcome` and `opponent` query parameters into a `DuelFilter`, or into a single
 * refusal — so the route has one thing to check instead of two.
 *
 * A `null` argument means the parameter was not in the query string and that axis does not narrow
 * the read; a `null` return means a parameter was present and unparseable. A present-but-unusable
 * parameter on either axis causes the entire filter to be refused rather than being quietly
 * dropped. Both arguments come straight from `request.queryParameters[...]` with nothing done to
 * them first.
 *
 * @param outcome the raw `outcome` parameter, or `null` if absent from the query string
 * @param opponent the raw `opponent` parameter, or `null` if absent from the query string
 * @return a `DuelFilter` when both parameters parse (or are absent), or `null` if either is
 *   present and unparseable
 */
public fun duelFilterOrNull(outcome: String?, opponent: String?): DuelFilter? {
    val parsedOutcome = outcome?.let { duelOutcomeOrNull(it) ?: return null }
    val parsedOpponent = opponent?.let { opponentSearchOrNull(it) ?: return null }
    return DuelFilter(parsedOutcome, parsedOpponent)
}

/**
 * Renders this filter to its canonical text representation.
 *
 * The output is one line per axis that actually narrows the read, in the fixed order `outcome`
 * then `opponent`. An absent axis contributes nothing at all — not an empty segment, not a
 * placeholder. This is what keeps existing cursors byte-identical when a third filter axis is
 * added later. Each line is length-delimited so no value can fake a line boundary:
 *
 * ```
 * "<axis>:<utf8ByteLength>:<value>\n"
 * ```
 *
 * The length is the UTF-8 byte count of the value, never `String.length` and never
 * `codePointCount`, because the byte count consumes the value exactly and no value can fake a
 * line boundary. An axis whose value is `null` contributes nothing at all, so
 * `DuelFilter.NONE` renders to the empty string, and `DuelFilter(WON, "Halvard")` renders to
 * `"outcome:3:WON\nopponent:7:Halvard\n"`.
 *
 * @return the canonical text of this filter
 */
internal fun DuelFilter.canonicalText(): String {
    val lines = mutableListOf<String>()
    outcome?.let { outcomeLabel ->
        val value = outcomeLabel.name
        val byteCount = value.toByteArray(Charsets.UTF_8).size
        lines.add("outcome:$byteCount:$value\n")
    }
    opponent?.let { opponentValue ->
        val byteCount = opponentValue.toByteArray(Charsets.UTF_8).size
        lines.add("opponent:$byteCount:$opponentValue\n")
    }
    return lines.joinToString("")
}

/**
 * Renders this filter to its fingerprint — a consistency check that binds a cursor to the filter
 * it was drawn under.
 *
 * The fingerprint is the first 8 bytes of the SHA-256 of the canonical text, as unpadded
 * URL-safe base64, which is always eleven characters. The fingerprint is a consistency check and
 * never an authorisation check: it is unkeyed, anybody who knows the scheme can mint one, and
 * the read is safe only because it is keyed off the player the server resolved.
 *
 * Three rules bind every future change to `DuelFilter`, and they are enforced by nothing except
 * the golden test that pins this method's output for one specific filter. A new axis is appended
 * to the canonical text order and never inserted; an existing axis is never renamed; and an
 * absent axis contributes nothing — which together mean every cursor already in flight stays
 * byte-identical when a third axis lands.
 *
 * @return the eleven-character fingerprint of this filter
 */
internal fun DuelFilter.fingerprint(): String {
    val canonicalBytes = canonicalText().toByteArray(Charsets.UTF_8)
    val digestBytes = MessageDigest.getInstance("SHA-256").digest(canonicalBytes)
    val truncated = digestBytes.copyOf(8)
    return Base64.getUrlEncoder().withoutPadding().encodeToString(truncated)
}

/**
 * Parses the `outcome` query parameter into the outcome label it names.
 *
 * Returns `null` for anything the server would not accept: a string that is not one of the
 * enum's own names (`WON`, `LOST`, `DREW`), the empty string, a name in the wrong case, or
 * whitespace-padded spellings. The parameter is not nullable: a missing query parameter is the
 * caller's business, not this function's.
 *
 * Matching is case-sensitive; `won` is refused and `WON ` (with trailing space) is refused. The
 * accepted spellings are the exact strings `DuelSummaryResponse.outcome` already puts on the
 * wire, so a client can feed a duel line's own outcome straight back as a filter.
 *
 * @param raw the raw `outcome` parameter, typically from a query string
 * @return the outcome label it names, or `null` if the parameter is invalid
 */
public fun duelOutcomeOrNull(raw: String): DuelOutcomeLabel? =
    DuelOutcomeLabel.entries.firstOrNull { it.name == raw }

private const val MAX_OPPONENT_SEARCH_CODE_POINTS = 32

/**
 * Parses the `opponent` query parameter into a usable search term, or returns `null` when no
 * search term can be extracted from [raw].
 *
 * NFC is applied because the column is NFC (V3__player_display_name.sql enforces
 * `CHECK (display_name IS NFC NORMALIZED)` and `canonicalDisplayNameOrNull` normalises before
 * storing), so a term typed on a keyboard that produces NFD would match a name it is character-
 * for-character equal to — and silently return nothing without normalisation. This is why `ADR-0048`
 * §1 normalises before hashing: compare like with like.
 *
 * Blank input — the empty string or a run of whitespace — is refused, not treated as an absent
 * parameter. The caller sent `opponent=` or `opponent=   `, which means the caller meant to search
 * for something. Accepting blank would turn that intent into a silent first page, the one behaviour
 * this endpoint exists to refuse.
 *
 * The returned string is the NFC form and is otherwise unchanged — never trimmed. A stored name may
 * hold an interior `U+0020` (ADR-0029 §3 permits one, refusing only doubled spaces), so ` Hal` is a
 * real substring of `Big Hal` and trimming would make it unfindable. This is the one place this
 * parser deliberately does less than `canonicalDisplayNameOrNull`.
 *
 * The limit is 32 code points because a name is at most 32 code points (the `player_display_name_length`
 * column has `CHECK (char_length(display_name) BETWEEN 1 AND 32)`, and PostgreSQL's `char_length`
 * counts characters), so a longer term can match nothing and is refused rather than sent to the database.
 * Counted with `codePointCount`, never `String.length` — PasswordPolicy.kt states the reason: astral
 * characters are 2 UTF-16 units but 1 code point, and a 32-character limit must count code points.
 *
 * @param raw the raw `opponent` parameter, typically from a query string
 * @return the NFC-normalised search term, when its code-point length is between 1 and 32 (inclusive);
 *   `null` otherwise
 */
public fun opponentSearchOrNull(raw: String): String? {
    val normalised = Normalizer.normalize(raw, Normalizer.Form.NFC)
    if (normalised.isBlank()) return null
    if (normalised.codePointCount(0, normalised.length) > MAX_OPPONENT_SEARCH_CODE_POINTS) return null
    return normalised
}
