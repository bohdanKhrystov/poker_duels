package duels.poker.engine.game

/**
 * The real [PokerEngine]: refuses an illegal action with a reason, applies a legal one.
 *
 * `handle` composes four pieces that already exist, holding no rules of its own:
 * [rejectionFor] to check legality, [eventFor] to turn a legal action into its event,
 * [StateProjection.apply] to fold that event into the state, and [EngineResult.accepted] to
 * carry both back. [continueHand] then names the next actor, if the round still has one.
 */
public object DefaultPokerEngine : PokerEngine {
    override fun handle(state: GameState, action: PlayerAction): EngineResult {
        val rejection = rejectionFor(state, action)
        if (rejection != null) {
            return EngineResult.rejected(state, rejection)
        }

        val event = eventFor(state, action)
        val newState = StateProjection.apply(state, event)
        val continued = continueHand(newState, action)
        return EngineResult.accepted(continued.newState, listOf(event) + continued.events)
    }
}
