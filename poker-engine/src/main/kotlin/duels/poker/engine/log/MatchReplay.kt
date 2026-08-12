package duels.poker.engine.log

import duels.poker.engine.duel.DuelOutcome
import duels.poker.engine.duel.MatchFinished
import duels.poker.engine.duel.MatchState
import duels.poker.engine.duel.outcomeOf
import duels.poker.engine.duel.recordHand

/**
 * What replaying a [MatchLog] recovers: every hand's [HandReplay] in order, the [MatchState] the
 * match ended in, and the [DuelOutcome] the engine reaches — `null` if the log records an
 * unfinished duel.
 */
public data class MatchReplay(
    val hands: List<HandReplay>,
    val finalMatch: MatchState,
    val outcome: DuelOutcome?,
)

/**
 * Re-runs [log] hand by hand through [MatchState.start] and [replayHand], the payoff of a match
 * being fully reconstructible from its log alone.
 *
 * Before each hand is replayed, its [HandLog] is checked against what [MatchState] says the next
 * hand must be — hand number, button seat, opening stacks and the blinds `MatchState.blinds`
 * derives for that hand — because match progression is the only place that can notice a hand log
 * claiming parameters its schedule never gave it; [replayHand] only knows the hand it is handed.
 * Each hand is then replayed with [replayHand], which re-runs it from its own seed and compares
 * every event, and folded back into the match with [recordHand].
 *
 * If the log carries a `MatchFinished` event, its recorded [DuelOutcome] must equal the one
 * [outcomeOf] reaches once every hand is replayed — a log whose recorded ending is not the ending
 * the engine reaches is corrupt.
 *
 * @throws IllegalArgumentException if a hand log's schedule (hand number, button seat, stacks or
 * blinds) does not match what [MatchState] expects next, or if the log's `MatchFinished` outcome
 * does not match the replayed outcome
 * @throws IllegalStateException if [replayHand] rejects an action or finds a diverging event
 *
 * Both exceptions above are rethrown, while replaying a hand, with a message that starts with
 * `hand <handNumber>: `, so the failure names the hand as well as whatever [replayHand] already
 * names within it.
 */
public fun replayMatch(log: MatchLog): MatchReplay {
    var match = MatchState.start(log.format, log.buttonSeat)
    val hands = mutableListOf<HandReplay>()

    for (handLog in log.hands) {
        checkHandMatchesSchedule(match, handLog)

        val replay =
            try {
                replayHand(handLog)
            } catch (e: IllegalStateException) {
                throw IllegalStateException("hand ${handLog.handNumber}: ${e.message}", e)
            } catch (e: IllegalArgumentException) {
                throw IllegalArgumentException("hand ${handLog.handNumber}: ${e.message}", e)
            }

        hands += replay
        match = recordHand(match, replay.finalState)
    }

    val outcome = outcomeOf(match)
    val recordedFinished = log.events.filterIsInstance<MatchFinished>().singleOrNull()
    if (recordedFinished != null) {
        require(recordedFinished.outcome == outcome) {
            "log records outcome ${recordedFinished.outcome}, replay reached $outcome"
        }
    }

    return MatchReplay(hands, match, outcome)
}

/**
 * Checks [handLog] against what [match] says its next hand must open with, throwing an
 * [IllegalArgumentException] naming [handLog]'s hand number if it does not: the schedule is
 * derived from [match] alone, so this is the only place a mismatch can be caught.
 */
private fun checkHandMatchesSchedule(match: MatchState, handLog: HandLog) {
    val handNumber = handLog.handNumber
    require(handNumber == match.nextHandNumber) {
        "hand $handNumber: expected hand ${match.nextHandNumber}, log has $handNumber"
    }
    require(handLog.buttonSeat == match.buttonSeat) {
        "hand $handNumber: expected button seat ${match.buttonSeat}, log has ${handLog.buttonSeat}"
    }
    require(handLog.stacks == match.stacks) {
        "hand $handNumber: expected stacks ${match.stacks}, log has ${handLog.stacks}"
    }

    val blinds = match.blinds
    require(handLog.smallBlind == blinds.smallBlind && handLog.bigBlind == blinds.bigBlind) {
        "hand $handNumber: expected blinds ${blinds.smallBlind}/${blinds.bigBlind}, " +
            "log has ${handLog.smallBlind}/${handLog.bigBlind}"
    }
}
