package duels.poker.engine.game

/** Refuses every action. It exists so the contract suite has something to run against. */
internal object NoOpEngine : PokerEngine {
    override fun handle(state: GameState, action: PlayerAction): EngineResult =
        EngineResult.rejected(state, Rejection.ActionNotAllowed(action.type, emptySet()))
}
