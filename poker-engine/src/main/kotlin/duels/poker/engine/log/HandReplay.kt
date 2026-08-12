package duels.poker.engine.log

import duels.poker.engine.game.DefaultPokerEngine
import duels.poker.engine.game.GameEvent
import duels.poker.engine.game.GameState
import duels.poker.engine.game.startHand
import duels.poker.engine.random.SplitMix64Rng

/**
 * What replaying a [HandLog] recovers: the state the hand opened in, the state after every
 * recorded action — index-aligned with `log.actions` — and every event regenerated along the
 * way, opening events first.
 */
public data class HandReplay(
    val opening: GameState,
    val statesAfterAction: List<GameState>,
    val events: List<GameEvent>,
) {
    /** The state the hand ended in, or [opening] if no action was recorded. */
    public val finalState: GameState get() = statesAfterAction.lastOrNull() ?: opening
}

/**
 * Re-runs [log] through [DefaultPokerEngine], from the same seed it was dealt from, replaying
 * its recorded actions in order — the payoff `ADR-0001` was chosen for: a hand is fully
 * reconstructible from a seed and a list of decisions.
 *
 * @throws IllegalStateException if the engine refuses a recorded action; a log it cannot replay
 * is corrupt
 */
public fun replayHand(log: HandLog): HandReplay {
    val started = startHand(log.handNumber, log.buttonSeat, log.stacks, log.smallBlind, log.bigBlind, SplitMix64Rng(log.seed))

    val statesAfterAction = mutableListOf<GameState>()
    val events = started.events.toMutableList()
    var state = started.newState

    log.actions.forEachIndexed { index, action ->
        val result = DefaultPokerEngine.handle(state, action)
        if (result.isRejected) {
            throw IllegalStateException(
                "action at index $index, $action, was rejected on replay: ${result.rejection}",
            )
        }

        events += result.events
        state = result.newState
        statesAfterAction += state
    }

    return HandReplay(started.newState, statesAfterAction, events)
}
