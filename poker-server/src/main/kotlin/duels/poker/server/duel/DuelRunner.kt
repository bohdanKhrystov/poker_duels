package duels.poker.server.duel

import duels.poker.engine.duel.DuelOutcome
import duels.poker.engine.duel.MatchState
import duels.poker.engine.duel.outcomeOf
import duels.poker.engine.game.GameState
import duels.poker.engine.log.HandLog
import duels.poker.engine.log.MatchLog

/**
 * The live state of one hand: the engine's GameState and the log it produces.
 *
 * @property state The current game state.
 * @property log The complete log of this hand so far.
 */
public data class LiveHand(val state: GameState, val log: HandLog) {
    init {
        require(state.handNumber == log.handNumber) {
            "state.handNumber (${state.handNumber}) must equal log.handNumber (${log.handNumber})"
        }
        require(log.events.size == state.eventCount) {
            "log.events.size (${log.events.size}) must equal state.eventCount (${state.eventCount})"
        }
    }
}

/**
 * An immutable value holding one duel: the match state, the live hand (if any), the match log
 * and the outcome (if any). The three ways these can disagree are rejected in init.
 *
 * The engine adds no rules here — blinds, button, hand numbering and the end condition all come
 * from the engine. Arithmetic on a blind or a button in this package is a review finding.
 *
 * @property match The match state: format, hands played, stacks and button.
 * @property hand The live hand currently in progress, or null if the duel is over.
 * @property log The match log so far, carrying all completed hands.
 * @property outcome The outcome of the duel if it is over, or null if still running.
 */
public data class DuelRunner(
    val match: MatchState,
    val hand: LiveHand?,
    val log: MatchLog,
    val outcome: DuelOutcome?,
) {
    init {
        require((hand == null) == (outcome != null)) {
            "a duel has a live hand until it is over, and never after: hand=${hand?.state?.handNumber}, outcome=$outcome"
        }
        if (hand != null) {
            require(hand.state.handNumber == match.nextHandNumber) {
                "the live hand (${hand.state.handNumber}) must be the hand the match expects next (${match.nextHandNumber})"
            }
        }
        if (outcome != null) {
            val engineOutcome = outcomeOf(match)
            require(outcome == engineOutcome) {
                "a recorded outcome must be the engine's own: recorded=$outcome, engine=$engineOutcome"
            }
        }
    }
}

/**
 * The next state of a duel and the outbound frames resulting from a transition.
 *
 * @property runner The duel after the transition.
 * @property outbound The frames each seat is entitled to see.
 */
public data class DuelStep(val runner: DuelRunner, val outbound: List<Addressed>)
