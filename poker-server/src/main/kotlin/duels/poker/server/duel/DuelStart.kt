package duels.poker.server.duel

import duels.poker.engine.duel.DuelFormat
import duels.poker.engine.duel.MatchState
import duels.poker.engine.duel.startNextHand
import duels.poker.engine.log.HandLog
import duels.poker.engine.log.MatchLog
import duels.poker.engine.random.SplitMix64Rng

/**
 * Opens one hand from a match and a seed: creates a `LiveHand` with its `GameState` and `HandLog`.
 *
 * All hand parameters — hand number, blinds, stacks, button — are read from the match, never
 * computed or assumed. The seed is recorded in the log exactly as provided, enabling perfect
 * replay.
 *
 * @param match the match whose next hand is to be dealt
 * @param seed the random seed this hand will draw its shuffle from
 * @return a live hand ready to play or ready for the next decision
 */
internal fun openHand(match: MatchState, seed: Long): LiveHand {
    val result = startNextHand(match, SplitMix64Rng(seed))
    return LiveHand(
        result.newState,
        HandLog(
            seed = seed,
            handNumber = match.nextHandNumber,
            buttonSeat = match.buttonSeat,
            stacks = match.stacks,
            smallBlind = match.blinds.smallBlind,
            bigBlind = match.blinds.bigBlind,
            actions = emptyList(),
            events = result.events,
        ),
    )
}

/**
 * Starts a new duel: creates a fresh match and deals its first hand.
 *
 * Returns a `DuelRunner` with the live hand and opening frames for both seats. The opening
 * frames include a `Snapshot` and `Events` for each seat, plus a `YourTurn` for the seat
 * whose turn it is.
 *
 * The seed is provided as a parameter and recorded in the hand log exactly as given, enabling
 * perfect replay and making this function deterministic and testable without external randomness.
 *
 * @param format the duel's configuration: starting stack, blind schedule, end condition
 * @param buttonSeat the seat holding the button for the first hand; defaults to 0
 * @param seed the random seed the first hand will draw its shuffle from
 * @return a `DuelStep` containing the duel's initial state and the frames each seat is entitled
 *   to see
 * @throws IllegalStateException if the opening hand has no seat to act (never happens for a legal
 *   `DuelFormat`, since the button always posts the small blind and has chips left)
 */
public fun startDuel(format: DuelFormat, buttonSeat: Int = 0, seed: Long): DuelStep {
    val match = MatchState.start(format, buttonSeat)
    val hand = openHand(match, seed)
    check(hand.state.seatToAct != null) {
        "opening hand must have a seat to act"
    }
    val runner = DuelRunner(
        match = match,
        hand = hand,
        log = MatchLog(format, buttonSeat, emptyList(), emptyList()),
        outcome = null,
    )
    val outbound = framesFor(hand.state, hand.log.events, hand.log.events)
    return DuelStep(runner, outbound)
}
