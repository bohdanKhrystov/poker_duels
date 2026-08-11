package duels.poker.engine.game

/**
 * The engine interface: the whole project is built on this shape.
 *
 * See ADR-0001 for the design rationale.
 */
public interface PokerEngine {
    /**
     * The next state and the events that produced it. Pure: the same [state] and [action]
     * always give the same result, on any machine. An illegal action neither throws nor
     * changes anything — it comes back as [EngineResult.rejection] with no events.
     */
    public fun handle(state: GameState, action: PlayerAction): EngineResult
}
