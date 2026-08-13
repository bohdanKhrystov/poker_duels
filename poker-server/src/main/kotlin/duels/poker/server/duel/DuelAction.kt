package duels.poker.server.duel

import duels.poker.engine.game.DefaultPokerEngine
import duels.poker.engine.game.Rejection
import duels.poker.server.protocol.Act
import duels.poker.server.protocol.ServerMessage

/**
 * Applies one inbound [Act] frame to a running duel: the whole path from a frame arriving on a
 * seat's connection to the frames every seat is entitled to see in response.
 *
 * The order is fixed: [guard] first, so a stale or out-of-turn frame never reaches the engine;
 * then `DefaultPokerEngine.handle`, so legality is decided once, by the engine, and nowhere else
 * in this function; then, if the action ended the hand, [advance], so the duel never stalls on a
 * hand that has already dealt itself past the end.
 *
 * @param runner The duel as it stands before this frame.
 * @param seat The seat of the connection [message] arrived on, as established by the server —
 *   never a claim [message] itself makes.
 * @param message The inbound attempt to act.
 * @param seeds The source a new hand's shuffle seed is drawn from, used only if this action ends
 *   the hand.
 * @return The duel after this frame, and the frames each seat is entitled to see because of it.
 */
public fun act(
    runner: DuelRunner,
    seat: Int,
    message: Act,
    seeds: HandSeedSource,
): DuelStep {
    val hand = runner.hand ?: return DuelStep(runner, emptyList())

    val refusal = guard(hand.state, hand.log.events, seat, message)
    if (refusal != null) {
        return when (refusal) {
            ActRefusal.STALE_FRAME -> DuelStep(runner, emptyList())
            ActRefusal.NOT_YOUR_TURN ->
                DuelStep(
                    runner,
                    listOf(Addressed(seat, ServerMessage.Rejected(Rejection.NotYourTurn(hand.state.seatToAct)))),
                )
        }
    }

    val result = DefaultPokerEngine.handle(hand.state, message.action)
    val rejection = result.rejection
    if (rejection != null) {
        return DuelStep(runner, listOf(Addressed(seat, ServerMessage.Rejected(rejection))))
    }

    val newLog = hand.log.copy(actions = hand.log.actions + message.action, events = hand.log.events + result.events)
    val outbound = framesFor(result.newState, result.events, newLog.events)
    val played = runner.copy(hand = LiveHand(result.newState, newLog))

    if (!result.newState.isHandOver) {
        return DuelStep(played, outbound)
    }

    val advanced = advance(played, seeds)
    return DuelStep(advanced.runner, outbound + advanced.outbound)
}
