package duels.poker.engine.duel

import duels.poker.engine.game.EngineResult
import duels.poker.engine.game.GameState
import duels.poker.engine.game.otherSeat
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

/**
 * Folds a settled hand's outcome back into [match]: stacks carry over, the hand is counted and
 * the button passes to the other seat. Blind levels are never stored on `MatchState` — they are
 * derived from [MatchState.handsPlayed] alone, so incrementing that count here is exactly what
 * lets the next hand's blind level follow, and guarantees no level can change mid-hand.
 *
 * This function owns advancing the button: [startNextHand] deliberately leaves it untouched, so
 * the button moves exactly once per hand, here.
 *
 * @param match the match [finished] was dealt from
 * @param finished the settled hand to fold back into [match]
 * @return [match] with [finished]'s stacks carried over, its hand counted and the button passed
 * @throws IllegalArgumentException if [finished] is not over, was not the hand [match] expected
 *   next, or its chip total does not match [match]'s
 */
public fun recordHand(match: MatchState, finished: GameState): MatchState {
    require(finished.isHandOver) {
        "hand ${finished.handNumber} is not over yet, still on street ${finished.street}"
    }
    require(finished.handNumber == match.nextHandNumber) {
        "match expected hand ${match.nextHandNumber}, but finished was hand ${finished.handNumber}"
    }
    require(finished.chipsInPlay == match.chips) {
        "chips must be conserved: match had ${match.chips}, finished hand had ${finished.chipsInPlay}"
    }

    return match.copy(
        handsPlayed = finished.handNumber,
        stacks = finished.seats.map { it.stack },
        buttonSeat = otherSeat(match.buttonSeat),
    )
}
