package duels.poker.ai

import duels.poker.engine.duel.DuelFormat
import duels.poker.engine.duel.DuelOutcome
import duels.poker.engine.duel.MatchState
import duels.poker.engine.duel.outcomeOf
import duels.poker.engine.duel.recordHand
import duels.poker.engine.duel.startNextHand
import duels.poker.engine.game.DefaultPokerEngine
import duels.poker.engine.game.PlayerAction
import duels.poker.engine.game.legalActions
import duels.poker.engine.random.Rng
import duels.poker.engine.random.SplitMix64Rng

/**
 * One hand played inside a [SimulatedDuel]: its 1-based number in the duel, the seed its shuffle
 * was dealt from, and every action accepted during it, in order.
 *
 * `(handSeed, actions)` is exactly the pair the engine's `replayHand` needs to reproduce this
 * hand on its own, independent of the rest of the duel.
 */
public data class SimulatedHand(val handNumber: Int, val handSeed: Long, val actions: List<PlayerAction>)

/**
 * A whole duel between two bots, played from [seed] to a decided [outcome].
 *
 * @property seed the duel's own seed, from which every hand's seed is drawn.
 * @property outcome the finished duel's result: winner, hands played and final stacks.
 * @property hands every hand played, in order, each carrying its own seed and actions.
 */
public data class SimulatedDuel(val seed: Long, val outcome: DuelOutcome, val hands: List<SimulatedHand>)

/**
 * The one failure a simulated duel can produce: an illegal bot action, a broken invariant, a hand
 * that runs past [SimulatedHand]'s action ceiling, or a duel that runs past its hand ceiling.
 *
 * Every case carries the same two lines of data a reproduction needs — the duel [seed] and the
 * failing hand's [handSeed] and [actions] so far — because one shape for every failure is what
 * makes a report actionable (`ADR-0001`).
 *
 * @property seed the duel seed [simulateDuel] was called with.
 * @property handNumber the 1-based number of the hand in progress when the failure happened.
 * @property handSeed the seed the failing hand was dealt from.
 * @property actions the actions accepted in the failing hand before the failure, in order.
 */
public class SimulationFailure(
    public val seed: Long,
    public val handNumber: Int,
    public val handSeed: Long,
    public val actions: List<PlayerAction>,
    message: String,
) : RuntimeException(message)

/**
 * Plays one whole duel between [bots] from [seed], checking [firstViolation] after every accepted
 * action, and returns the finished [SimulatedDuel].
 *
 * Every hand's seed is drawn from a match-level [SplitMix64Rng] seeded with [seed]: the drawn
 * value becomes the hand's seed and the generator that follows it carries forward to draw the
 * next hand's seed, so the whole duel is a pure function of [seed]. Decisions are drawn from a
 * second, independent [Rng] stream that also starts at `SplitMix64Rng(seed)` and threads forward
 * across every decision of every hand — same [seed] and same bots, byte-identical duel, always.
 *
 * @param seed the duel's own seed.
 * @param bots the two bots, seat-indexed; must hold exactly two entries.
 * @param format the duel's configuration; defaults to [DuelFormat.DEFAULT].
 * @param maxHands the ceiling on hands played before a duel is judged to never terminate.
 * @param maxActionsPerHand the ceiling on actions accepted in one hand before it is judged stuck.
 * @return the finished duel: its outcome and every hand played to reach it.
 * @throws SimulationFailure if a bot's action is rejected, an invariant breaks, a hand exceeds
 *   [maxActionsPerHand], or the duel exceeds [maxHands] with no outcome.
 */
public fun simulateDuel(
    seed: Long,
    bots: List<Bot>,
    format: DuelFormat = DuelFormat.DEFAULT,
    maxHands: Int = 500,
    maxActionsPerHand: Int = 200,
): SimulatedDuel {
    require(bots.size == 2) { "bots must hold exactly two entries, seat-indexed, had ${bots.size}" }

    var match = MatchState.start(format)
    var matchRng = SplitMix64Rng(seed)
    var decisionRng: Rng = SplitMix64Rng(seed)
    val hands = mutableListOf<SimulatedHand>()

    while (true) {
        val outcome = outcomeOf(match)
        if (outcome != null) {
            return SimulatedDuel(seed, outcome, hands.toList())
        }

        val handNumber = match.nextHandNumber
        if (handNumber > maxHands) {
            fail(seed, handNumber, 0L, emptyList(), "duel exceeded $maxHands hands with no outcome")
        }

        val seedDraw = matchRng.nextLong()
        val handSeed = seedDraw.value
        matchRng = seedDraw.next

        val actions = mutableListOf<PlayerAction>()
        var state = startNextHand(match, SplitMix64Rng(handSeed)).newState
        val chipsAtOpen = state.chipsInPlay

        firstViolation(state, chipsAtOpen)?.let { fail(seed, handNumber, handSeed, actions.toList(), it) }

        while (!state.isHandOver) {
            if (actions.size >= maxActionsPerHand) {
                fail(seed, handNumber, handSeed, actions.toList(), "hand exceeded $maxActionsPerHand actions")
            }

            val seatToAct = state.seatToAct
                ?: fail(
                    seed,
                    handNumber,
                    handSeed,
                    actions.toList(),
                    "no seat to act: hand is not over (street=${state.street}) but seatToAct is null",
                )

            val choice = bots[seatToAct].choose(state, legalActions(state), decisionRng)
            val result = DefaultPokerEngine.handle(state, choice.action)
            if (result.isRejected) {
                fail(
                    seed,
                    handNumber,
                    handSeed,
                    actions.toList(),
                    "action ${choice.action} was rejected: ${result.rejection}",
                )
            }

            state = result.newState
            actions += choice.action
            decisionRng = choice.rng

            firstViolation(state, chipsAtOpen)?.let { fail(seed, handNumber, handSeed, actions.toList(), it) }
        }

        hands += SimulatedHand(handNumber, handSeed, actions.toList())
        match = recordHand(match, state)
    }
}

/** Throws [SimulationFailure] carrying the two lines of data that reproduce the failure. */
private fun fail(
    seed: Long,
    handNumber: Int,
    handSeed: Long,
    actions: List<PlayerAction>,
    message: String,
): Nothing = throw SimulationFailure(seed, handNumber, handSeed, actions, message)
