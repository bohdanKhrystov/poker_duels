package duels.poker.engine.game

/**
 * Heads-up blind and action order. This is the single most-often-inverted rule in heads-up
 * hold'em: the button is the small blind and acts first only before the flop.
 *
 * These functions live here to avoid re-deriving them in every caller.
 */

/**
 * Returns the other seat at the heads-up table.
 *
 * @param seat must be 0 or 1
 * @return 1 if seat is 0, 0 if seat is 1
 */
public fun otherSeat(seat: Int): Int {
    require(seat in 0..1) { "Seat must be 0 or 1, got $seat" }
    return 1 - seat
}

/**
 * Returns the seat that posts the small blind.
 *
 * Per docs/duel-rules.md "Positions and blinds": "The button is the small blind."
 * This function exists because the name looks like a typo otherwise.
 *
 * @param buttonSeat must be 0 or 1
 * @return the button seat (which posts the small blind)
 */
public fun smallBlindSeat(buttonSeat: Int): Int {
    require(buttonSeat in 0..1) { "Button seat must be 0 or 1, got $buttonSeat" }
    return buttonSeat
}

/**
 * Returns the seat that posts the big blind.
 *
 * @param buttonSeat must be 0 or 1
 * @return the non-button seat
 */
public fun bigBlindSeat(buttonSeat: Int): Int {
    require(buttonSeat in 0..1) { "Button seat must be 0 or 1, got $buttonSeat" }
    return otherSeat(buttonSeat)
}

/**
 * Returns the seat that acts first on a given street.
 *
 * The button acts first before the flop and last on every street after it.
 * SHOWDOWN and COMPLETE have no betting.
 *
 * @param street the current street
 * @param buttonSeat must be 0 or 1
 * @return the seat that acts first
 * @throws IllegalArgumentException if street is SHOWDOWN or COMPLETE, or if buttonSeat is invalid
 */
public fun firstToActOn(street: Street, buttonSeat: Int): Int {
    require(buttonSeat in 0..1) { "Button seat must be 0 or 1, got $buttonSeat" }
    return when (street) {
        Street.PREFLOP -> buttonSeat
        Street.FLOP, Street.TURN, Street.RIVER -> otherSeat(buttonSeat)
        Street.SHOWDOWN, Street.COMPLETE -> throw IllegalArgumentException("No betting on $street")
    }
}
