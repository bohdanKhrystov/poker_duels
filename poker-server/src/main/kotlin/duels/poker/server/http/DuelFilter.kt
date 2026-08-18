package duels.poker.server.http

import duels.poker.server.protocol.http.DuelOutcomeLabel

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
