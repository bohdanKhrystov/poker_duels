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
 * Every event the engine regenerates is compared against `log.events` at the same index as soon
 * as it is produced, so a log whose recorded events do not match what the engine actually did is
 * caught at the first place the two disagree, not silently accepted.
 *
 * @throws IllegalStateException if the engine refuses a recorded action; a log it cannot replay
 * is corrupt
 * @throws IllegalStateException if a regenerated event does not match the event recorded at the
 * same index, or if the replay and the log do not end up with the same number of events
 */
public fun replayHand(log: HandLog): HandReplay {
    val started = startHand(log.handNumber, log.buttonSeat, log.stacks, log.smallBlind, log.bigBlind, SplitMix64Rng(log.seed))

    val statesAfterAction = mutableListOf<GameState>()
    val events = mutableListOf<GameEvent>()
    var state = started.newState

    checkAgainstLog(log, events.size, started.events)
    events += started.events

    log.actions.forEachIndexed { index, action ->
        val result = DefaultPokerEngine.handle(state, action)
        if (result.isRejected) {
            throw IllegalStateException(
                "action at index $index, $action, was rejected on replay: ${result.rejection}",
            )
        }

        checkAgainstLog(log, events.size, result.events)
        events += result.events
        state = result.newState
        statesAfterAction += state
    }

    if (events.size != log.events.size) {
        throw IllegalStateException("log has ${log.events.size} events, the replay produced ${events.size}")
    }

    return HandReplay(started.newState, statesAfterAction, events)
}

/**
 * Compares [newEvents], produced starting right after [alreadyProduced] events, against the
 * events recorded in [log] at the same indices — one event overrunning the log's last index is
 * reported the same way as one that simply does not match.
 */
private fun checkAgainstLog(log: HandLog, alreadyProduced: Int, newEvents: List<GameEvent>) {
    newEvents.forEachIndexed { offset, event ->
        val index = alreadyProduced + offset
        if (index >= log.events.size) {
            throw IllegalStateException("log has ${log.events.size} events, the replay produced ${index + 1}")
        }

        val recorded = log.events[index]
        if (recorded != event) {
            throw IllegalStateException("log diverges at event index $index: recorded $recorded, replayed $event")
        }
    }
}
