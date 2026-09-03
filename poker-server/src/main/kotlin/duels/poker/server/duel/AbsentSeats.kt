package duels.poker.server.duel

import duels.poker.engine.game.ActionType
import duels.poker.engine.game.PlayerAction
import duels.poker.engine.game.legalActions
import duels.poker.server.protocol.Act
import duels.poker.server.protocol.ServerMessage

/**
 * Applies a fold or check for a single absent seat on turn. Returns the step unchanged and
 * identical (`===`) if there is no live hand, no seat on turn, the given seat is not on turn,
 * neither `FOLD` nor `CHECK` is legal, or the decision point cannot be read. Otherwise, applies
 * the action exactly as an inbound frame would be handled, marks it to both seats per `ADR-0028`,
 * and returns the new step.
 *
 * @param step The duel as it stands, together with the frames already queued before this call.
 * @param seat The seat to give up a turn for.
 * @param seeds The source a new hand's shuffle seed is drawn from, forwarded to [act] unchanged.
 * @return The step unchanged if the action cannot be applied, or the duel once the action is
 *   applied, with all frames produced appended after [step]'s own [DuelStep.outbound].
 */
public fun giveUpDecision(step: DuelStep, seat: Int, seeds: HandSeedSource): DuelStep {
    val hand = step.runner.hand ?: return step
    val onTurn = hand.state.seatToAct ?: return step
    if (onTurn != seat) return step

    val legal = legalActions(hand.state)
    val (giveUp, actionType) = when {
        legal.allows(ActionType.FOLD) -> PlayerAction.Fold(seat) to ActionType.FOLD
        legal.allows(ActionType.CHECK) -> PlayerAction.Check(seat) to ActionType.CHECK
        // The engine does not produce an on-turn seat with neither action legal; return
        // unchanged rather than send anything if it ever does.
        else -> return step
    }

    val frame = Act(
        handNumber = hand.state.handNumber,
        actionSequence = decisionPointOf(hand.log.events)?.sequence ?: return step,
        action = giveUp,
    )

    val before = step.runner
    val next = act(before, seat, frame, seeds)
    val moved = next.runner != before
    val produced = if (moved) {
        // Marked to both seats, per ADR-0028: this is a fact about the log they share, so
        // unlike a per-recipient frame it must name the seat it is about.
        val mark = ServerMessage.ActedForAbsent(
            seat = seat,
            handNumber = hand.state.handNumber,
            actionSequence = frame.actionSequence,
            action = actionType,
        )
        listOf(Addressed(0, mark), Addressed(1, mark)) + next.outbound
    } else {
        next.outbound
    }
    return DuelStep(next.runner, step.outbound + produced)
}

/**
 * Gives up the turn for every seat nobody is sitting in, each time the turn reaches it: folds
 * when a bet is faced, checks when nothing is owed. The action is never guessed from the spot's
 * shape — it is read from the engine's own [legalActions] at that decision point, per `ADR-0023`:
 * `Fold` when `FOLD` is in the legal set, `Check` otherwise, and never a call, bet, raise or
 * all-in.
 *
 * The give-up action is not a special case: for each absent seat on turn, this builds the same
 * [Act] frame that seat's own client would have had to send, and hands it to [act] exactly as an
 * inbound frame would be handled. That means the same [guard], the same engine call that decides
 * legality, and the same [advance] at the hand boundary — a turn given up for absence is, in the
 * log, indistinguishable from one a player acted on themselves, exactly as `ADR-0023` describes.
 * `ADR-0028` deliberately reverses the other half of that property: on the wire, it is not. Every
 * action this function actually applies is preceded, to **both** seats, by a
 * [ServerMessage.ActedForAbsent] frame naming the absent seat, the decision point, and the action
 * taken. That mark, and only that mark, is a frame this function constructs of its own; every
 * other frame it returns is exactly what [act] gave it.
 *
 * Termination is guaranteed two ways:
 *  - the action is always the cheapest one [legalActions] allows for the seat this function acts
 *    on behalf of — never a call, bet, raise or all-in — so a hand this loop drives by itself
 *    passes through a bounded number of decisions per street and always either folds or runs
 *    every street down to showdown; either way, [advance] then deals the next hand — moving
 *    `seatToAct` to the seat that is present, once the other absent seat is not also on turn — or
 *    ends the duel outright;
 *  - if a call to [act] ever returns a runner identical to the one it was given — a refusal, which
 *    would leave the seat on turn unchanged — the loop returns immediately instead of retrying an
 *    action that visibly made no progress, and marks nothing for it.
 *
 * @param step The duel as it stands, together with the frames already queued before this call.
 * @param absent The seats nobody is sitting in right now. May be empty, hold one seat, or both.
 * @param seeds The source a new hand's shuffle seed is drawn from, forwarded to [act] unchanged.
 * @return The duel once no seat on turn is both live and absent, with every frame produced along
 *   the way appended, in order, after [step]'s own [DuelStep.outbound].
 */
public fun foldAbsent(step: DuelStep, absent: Set<Int>, seeds: HandSeedSource): DuelStep {
    var current = step
    while (true) {
        val hand = current.runner.hand ?: return current
        val seat = hand.state.seatToAct ?: return current
        if (seat !in absent) return current

        val next = giveUpDecision(current, seat, seeds)
        val moved = next.runner != current.runner
        current = next
        if (!moved) return current
    }
}
