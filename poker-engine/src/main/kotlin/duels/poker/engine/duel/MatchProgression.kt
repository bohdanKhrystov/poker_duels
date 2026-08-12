package duels.poker.engine.duel

import duels.poker.engine.game.EngineResult
import duels.poker.engine.game.startHand
import duels.poker.engine.random.Rng

/**
 * Opens [match]'s next hand: the right hand number, button, blind level and stacks, all read off
 * [match] and handed to `startHand`, which already knows how to deal one. This function holds no
 * hand rules of its own — a seat too short for its blind is dealt in and posts all-in, exactly as
 * `startHand` already does.
 *
 * No `MatchState` comes back, because the hand this opens is not over yet — `recordHand`
 * (`TASK-010710`) folds its outcome back into the match once it is.
 *
 * @param match the match whose next hand is about to be dealt
 * @param rng the source of randomness that hand draws its shuffle from
 * @return the opened hand's state and the events that produced it
 */
public fun startNextHand(match: MatchState, rng: Rng): EngineResult {
    require(match.stacks.all { it >= 1 }) {
        "every seat needs at least one chip to open a hand, had ${match.stacks}"
    }

    return startHand(
        handNumber = match.nextHandNumber,
        buttonSeat = match.buttonSeat,
        stacks = match.stacks,
        smallBlind = match.blinds.smallBlind,
        bigBlind = match.blinds.bigBlind,
        rng = rng,
    )
}
