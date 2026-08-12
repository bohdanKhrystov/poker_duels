package duels.poker.ai

import duels.poker.engine.game.ActionType
import duels.poker.engine.game.GameState
import duels.poker.engine.game.LegalActions
import duels.poker.engine.game.PlayerAction
import duels.poker.engine.random.Rng

/**
 * A deliberately simple bot that chooses uniformly at random among all legal actions. Each draw
 * is deterministic: the same [Rng] seed always produces the same sequence of choices.
 *
 * The uniform action selection is the baseline against which smarter bots measure themselves —
 * it reaches game states a thinking player never would, which is exactly what a testing bot needs.
 */
public object RandomBot : Bot {
    /**
     * Pick a uniformly random legal action.
     *
     * For [ActionType.BET] and [ActionType.RAISE], a second uniform draw chooses between the
     * minimum legal total and going all-in, each with 50% probability. All other action types
     * carry no amount.
     *
     * @param state Ignored; a smarter bot (EPIC-09) will need it for equity calculation
     * @param legal The set of legal actions, their amounts, and the seat that acts
     * @param rng The random source
     * @return The chosen action paired with the [Rng] state after all draws
     */
    override fun choose(state: GameState, legal: LegalActions, rng: Rng): Bot.Choice {
        // Sort the allowed action types for determinism: Set iteration order is not a contract.
        val sortedTypes = legal.allowed.sorted()

        // First draw: which action type?
        val typeDraw = rng.nextInt(sortedTypes.size)
        var nextRng = typeDraw.next

        // Construct the action based on the type drawn.
        val action: PlayerAction = when (sortedTypes[typeDraw.value]) {
            ActionType.FOLD -> PlayerAction.Fold(legal.seat)
            ActionType.CHECK -> PlayerAction.Check(legal.seat)
            ActionType.CALL -> PlayerAction.Call(legal.seat)
            ActionType.BET -> {
                // Second draw: minimum bet or all-in?
                val amountDraw = nextRng.nextInt(2)
                nextRng = amountDraw.next
                PlayerAction.Bet(legal.seat, if (amountDraw.value == 0) legal.minBetTo else legal.allInTo)
            }
            ActionType.RAISE -> {
                // Second draw: minimum raise or all-in?
                val amountDraw = nextRng.nextInt(2)
                nextRng = amountDraw.next
                PlayerAction.Raise(legal.seat, if (amountDraw.value == 0) legal.minRaiseTo else legal.allInTo)
            }
            ActionType.ALL_IN -> PlayerAction.AllIn(legal.seat)
        }

        return Bot.Choice(action, nextRng)
    }
}
