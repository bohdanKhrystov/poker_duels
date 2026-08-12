package duels.poker.engine.game

/**
 * The real [PokerEngine]: refuses an illegal action with a reason, applies a legal one.
 *
 * `handle` composes four pieces that already exist, holding no rules of its own:
 * [rejectionFor] to check legality, [eventFor] to turn a legal action into its event,
 * [StateProjection.apply] to fold that event into the state, and [EngineResult.accepted] to
 * carry both back.
 *
 * TODO(TASK-010514): `applyBetting` clears `seatToAct` and nothing here names the next actor,
 * so the hand stops after every single action for now. Delete this note when that ticket lands.
 */
public object DefaultPokerEngine : PokerEngine {
    override fun handle(state: GameState, action: PlayerAction): EngineResult {
        val rejection = rejectionFor(state, action)
        if (rejection != null) {
            return EngineResult.rejected(state, rejection)
        }

        val event = eventFor(state, action)
        val newState = StateProjection.apply(state, event)
        return EngineResult.accepted(newState, listOf(event))
    }
}
