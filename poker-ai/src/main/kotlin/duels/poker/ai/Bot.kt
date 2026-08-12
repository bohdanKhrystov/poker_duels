package duels.poker.ai

import duels.poker.engine.game.GameState
import duels.poker.engine.game.LegalActions
import duels.poker.engine.game.PlayerAction
import duels.poker.engine.random.Rng

/**
 * A bot is a pure function from a decision point to an action. It never mutates state and never
 * reaches for a platform random source — all randomness is injected. The returned [Choice]
 * includes the [Rng] state after every draw the decision consumed, keeping simulated duels
 * reproducible: same seed and actions produce byte-identical games.
 */
public interface Bot {
    /**
     * Choose an action for the current decision point. [state] carries the board, stacks, and
     * bets; [legal] describes which actions are permissible and their amounts; [rng] is the
     * random source. The returned [Choice] packages the action and the advanced [Rng] state
     * together.
     *
     * @param state The current game state (unused by the base [RandomBot])
     * @param legal The legal actions available at this decision point
     * @param rng The random source to use for this decision
     * @return A [Choice] containing the selected action and the next [Rng] state
     */
    public fun choose(state: GameState, legal: LegalActions, rng: Rng): Choice

    /**
     * The action a bot chose at a decision point, paired with the [Rng] state after drawing it.
     *
     * @param action The selected [PlayerAction]
     * @param rng The [Rng] state after this choice was made
     */
    public data class Choice(val action: PlayerAction, val rng: Rng)
}
