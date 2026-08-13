package duels.poker.server.duel

import duels.poker.engine.duel.matchFinishedEvent
import duels.poker.engine.duel.outcomeOf
import duels.poker.engine.duel.recordHand
import duels.poker.engine.log.MatchLog

/**
 * Folds a just-finished hand back into its duel: records it in the match, and either deals the
 * next hand or ends the duel with the engine's own `MatchFinished`.
 *
 * Opening a hand can also finish it: a seat too short for its blind posts all-in, and `openHand`
 * runs the board straight out to showdown, leaving no seat to act. Returning that hand as "live"
 * would stall the duel forever — no inbound frame could ever move it, because no seat has a
 * decision to make. So this function loops, dealing hand after hand, until either the duel is
 * over or a hand is genuinely awaiting an action.
 *
 * @param runner a duel whose live hand has just ended (`runner.hand!!.state.isHandOver`)
 * @param seeds the source this duel draws each new hand's shuffle seed from
 * @return the duel advanced past every hand that dealt itself out, with the outbound frames each
 *   seat is entitled to see along the way
 * @throws IllegalArgumentException if `runner` has no live hand, or its live hand is not over
 */
public fun advance(runner: DuelRunner, seeds: HandSeedSource): DuelStep {
    require(runner.hand != null) { "advance requires a live hand, but runner has none" }
    require(runner.hand.state.isHandOver) {
        "advance requires a finished hand, but hand ${runner.hand.state.handNumber} is still on " +
            "street ${runner.hand.state.street}"
    }

    var current = runner
    val outbound = mutableListOf<Addressed>()

    while (true) {
        val hand = current.hand!!
        val match = recordHand(current.match, hand.state)
        val log: MatchLog = current.log.copy(hands = current.log.hands + hand.log)

        val finished = matchFinishedEvent(match, log.events.size)
        if (finished != null) {
            current = DuelRunner(
                match = match,
                hand = null,
                log = log.copy(events = log.events + finished),
                outcome = outcomeOf(match),
            )
            val outcome = checkNotNull(current.outcome) {
                "a match that produced MatchFinished must have an outcome, but runner.outcome was null"
            }
            outbound += finishedFrames(outcome)
            return DuelStep(current, outbound)
        }

        val next = openHand(match, seeds.newHandSeed())
        outbound += framesFor(next.state, next.log.events, next.log.events)
        current = DuelRunner(match = match, hand = next, log = log, outcome = null)

        if (!next.state.isHandOver) {
            check(current.hand!!.state.seatToAct != null) {
                "a live hand must always have a seat to act, but hand ${next.state.handNumber} has none"
            }
            return DuelStep(current, outbound)
        }
    }
}
