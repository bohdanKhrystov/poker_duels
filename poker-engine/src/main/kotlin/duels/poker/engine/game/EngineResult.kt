package duels.poker.engine.game

/**
 * The single value the engine returns: the new state and the events that produced it are two
 * descriptions of one transition. The contract suite asserts they agree.
 *
 * Anyone adding a field here should read ADR-0001 first.
 */
public data class EngineResult(
    val newState: GameState,
    val events: List<GameEvent> = emptyList(),
    val rejection: Rejection? = null,
) {
    init {
        require(rejection == null || events.isEmpty()) {
            "A rejected action emits no events, got ${events.size}"
        }
    }

    public val isRejected: Boolean get() = rejection != null

    public companion object {
        /** The action was refused: the state comes back untouched and nothing happened. */
        public fun rejected(state: GameState, reason: Rejection): EngineResult =
            EngineResult(state, emptyList(), reason)

        public fun accepted(state: GameState, events: List<GameEvent>): EngineResult =
            EngineResult(state, events, null)
    }
}
