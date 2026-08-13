package duels.poker.server.duel

import duels.poker.engine.game.PlayerAction
import duels.poker.server.protocol.Act

/**
 * Folds for every seat nobody is sitting in, each time the turn reaches it.
 *
 * The fold is not a special case: for each absent seat on turn, this builds the same [Act] frame
 * that seat's own client would have had to send, and hands it to [act] exactly as an inbound
 * frame would be handled. That means the same [guard], the same engine call that decides legality,
 * the same [advance] at the hand boundary, and the same frames to both seats — a hand folded for
 * absence is, in the log and on the wire, indistinguishable from one a player folded themselves.
 * Nothing here constructs a game state, an event, or a frame of its own; it only returns what
 * [act] gave it.
 *
 * Termination is guaranteed two ways:
 *  - in heads-up, a fold always ends the hand, so [advance] either deals the next hand — moving
 *    `seatToAct` to the seat that is present, once the other absent seat is not also on turn — or
 *    ends the duel outright; either way, the folding seat's stack strictly decreases by the blind
 *    it posted that hand, so even a freezeout with an absent seat runs down and ends rather than
 *    looping forever;
 *  - if a call to [act] ever returns a runner identical to the one it was given — a refusal, which
 *    would leave the seat on turn unchanged — the loop returns immediately instead of retrying an
 *    action that visibly made no progress.
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

        val frame = Act(
            handNumber = hand.state.handNumber,
            actionSequence = decisionPointOf(hand.log.events)?.sequence ?: return current,
            action = PlayerAction.Fold(seat),
        )

        val before = current.runner
        val next = act(before, seat, frame, seeds)
        current = DuelStep(next.runner, current.outbound + next.outbound)
        if (next.runner == before) return current
    }
}
