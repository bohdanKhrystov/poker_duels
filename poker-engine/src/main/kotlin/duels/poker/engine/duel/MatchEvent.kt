package duels.poker.engine.duel

/**
 * The schema version of [MatchEvent]. Incremented when the structure of any [MatchEvent]
 * subtype changes in a way that invalidates prior serializations. Events with version
 * mismatches must not be trusted.
 */
public const val MATCH_EVENT_SCHEMA_VERSION: Int = 1

/**
 * A durable fact about a match: events that occurred at the match level, numbered from the start.
 *
 * `MatchEvent` has its own sequence space: [sequence] is a position within the match, starting
 * at 0 and dense and gap-free, never a position within a hand. This hierarchy remains distinct
 * from [GameEvent], which stays hand-scoped, per `ADR-0009`.
 */
public sealed interface MatchEvent {
    /**
     * Position within the match, starting at 0, dense and gap-free — never a position in a hand.
     */
    public val sequence: Int

    /**
     * The schema version of this event. Defaults to [MATCH_EVENT_SCHEMA_VERSION], so it never
     * participates in equality: this way, old and new serializations can round-trip without
     * changing what equality means.
     */
    public val version: Int get() = MATCH_EVENT_SCHEMA_VERSION
}

/**
 * The duel is over. Carries [DuelOutcome] whole, rather than re-listing winner, hand count, and
 * final stacks, because [DuelOutcome] already validates all three and centralizes the source of
 * truth for "who won".
 *
 * @param sequence the position of this event within the match (starting at 0).
 * @param outcome the result of the finished duel.
 */
public data class MatchFinished(
    override val sequence: Int,
    val outcome: DuelOutcome,
) : MatchEvent {
    init {
        require(sequence >= 0) { "sequence must be non-negative, was $sequence" }
    }
}

/**
 * Factory for [MatchFinished], or `null` if the match is still running.
 *
 * Delegates to [outcomeOf] to decide whether the match has ended. If it has, wraps the outcome
 * with the given [sequence]. Contains no end-condition logic of its own; [outcomeOf] owns that.
 *
 * @param match the state of the match to evaluate.
 * @param sequence the sequence number to assign to the event if the match has finished; defaults to 0.
 * @return a [MatchFinished] event if the match is over, or `null` if it is still running.
 */
public fun matchFinishedEvent(match: MatchState, sequence: Int = 0): MatchFinished? {
    val outcome = outcomeOf(match)
    return if (outcome != null) MatchFinished(sequence, outcome) else null
}
