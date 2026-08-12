package duels.poker.engine.game

import kotlinx.serialization.Serializable

/**
 * Everything a client needs to draw the right buttons and sliders for the current decision point.
 *
 * All amounts are street totals, not increments. If a seat has already contributed 100 to the
 * street and the next legal raise is to a total of 300, then `minRaiseTo = 300`.
 *
 * This is a descriptor only: computing it from a [GameState] during a real betting round is
 * STORY-0105. This type enforces the invariants of a legal action set but does not compute one.
 *
 * ## Street totals
 *
 * - `callTo`: the total this seat will have contributed after calling, capped at its stack. For
 *   example, if the seat has 400 chips and faces a bet of 500, `callTo = 400` (calling all-in
 *   for less). The seat commits everything it has.
 *
 * - `minBetTo`: the smallest legal total for a bet, when no bet is outstanding. In a duel's
 *   default 50/100 blinds, this would be 100.
 *
 * - `minRaiseTo`: the smallest legal total for a raise, when facing a bet. Facing a bet of 100,
 *   a raise might be to a minimum of 200 (raising by the same amount).
 *
 * - `allInTo`: the total if the seat commits its entire remaining stack.
 *
 * ## The uncoverable all-in case
 *
 * When a seat faces an all-in it cannot cover:
 *
 * ```
 * allowed = setOf(ActionType.FOLD, ActionType.CALL)
 * callTo = 400
 * allInTo = 400
 * ```
 *
 * There is no raise because no amount of additional chips would make a raise legal — the
 * opponent has nothing left. Calling is the only active option besides folding.
 *
 * ## Serialization: default amounts on the wire
 *
 * All four amount fields (`callTo`, `minBetTo`, `minRaiseTo`, `allInTo`) default to 0.
 * A `Json` instance without `encodeDefaults = true` will omit these fields during encoding.
 * A client decoding such a message would then receive the default 0 instead of the real
 * (possibly nonzero) value — silently offering a player an action the server never allowed.
 * The protocol's `Json` instance sets `encodeDefaults = true` (TASK-020201) to prevent this;
 * this note is why that setting must remain in place.
 */
@Serializable
public data class LegalActions(
    val seat: Int,
    val allowed: Set<ActionType>,
    val callTo: Int = 0,
    val minBetTo: Int = 0,
    val minRaiseTo: Int = 0,
    val allInTo: Int = 0,
) {
    init {
        require(callTo >= 0) { "callTo must be non-negative, was $callTo" }
        require(minBetTo >= 0) { "minBetTo must be non-negative, was $minBetTo" }
        require(minRaiseTo >= 0) { "minRaiseTo must be non-negative, was $minRaiseTo" }
        require(allInTo >= 0) { "allInTo must be non-negative, was $allInTo" }

        require(!(ActionType.CHECK in allowed && ActionType.CALL in allowed)) {
            "CHECK and CALL are never both allowed"
        }

        require(!(ActionType.BET in allowed && ActionType.RAISE in allowed)) {
            "BET and RAISE are never both allowed"
        }

        if (ActionType.BET in allowed) {
            require(minBetTo in 1..allInTo) {
                "If BET is allowed, minBetTo must be in 1..allInTo, was $minBetTo with allInTo = $allInTo"
            }
        }

        if (ActionType.RAISE in allowed) {
            require(minRaiseTo in 1..allInTo) {
                "If RAISE is allowed, minRaiseTo must be in 1..allInTo, was $minRaiseTo with allInTo = $allInTo"
            }
        }
    }

    /**
     * Whether [type] is in the set of legal actions for this decision point.
     */
    public fun allows(type: ActionType): Boolean = type in allowed

    public companion object {
        /**
         * No action is legal — it is not this seat's turn, or the hand is over.
         */
        public fun none(seat: Int): LegalActions = LegalActions(seat, emptySet())
    }
}
