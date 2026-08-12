package duels.poker.ai

import duels.poker.engine.duel.DuelFormat
import duels.poker.engine.duel.MatchState
import duels.poker.engine.duel.outcomeOf
import duels.poker.engine.duel.recordHand
import duels.poker.engine.duel.startNextHand
import duels.poker.engine.game.DefaultPokerEngine
import duels.poker.engine.game.GameEvent
import duels.poker.engine.game.GameState
import duels.poker.engine.game.legalActions
import duels.poker.engine.random.Rng
import duels.poker.engine.random.SplitMix64Rng

/**
 * One point in an observed hand: the state at that point and every event the hand has produced up
 * to and including it. Keeping both, rather than just the state, is what lets a redaction property
 * check every event a recipient would ever have received, not only the current view.
 */
internal data class ObservedStep(val state: GameState, val eventsSoFar: List<GameEvent>)

/**
 * One whole hand of an [ObservedDuel]: its 1-based number, the seed its shuffle was dealt from,
 * and every [ObservedStep] from the hand's opening through its last accepted action.
 */
internal data class ObservedHand(
    val handNumber: Int,
    val handSeed: Long,
    val steps: List<ObservedStep>,
) {
    /** Every event this hand produced, in order — the last step's [ObservedStep.eventsSoFar]. */
    val events: List<GameEvent> get() = steps.last().eventsSoFar

    /** The hand's state once it is over — the last step's [ObservedStep.state]. */
    val finalState: GameState get() = steps.last().state
}

/**
 * A whole [RandomBot] duel played from [seed], with every hand's [ObservedStep]s kept — the raw
 * material redaction properties need, since a summary that keeps only outcomes could never prove
 * that no view and no event ever carried a card its recipient may not see.
 */
internal data class ObservedDuel(val seed: Long, val hands: List<ObservedHand>)

/**
 * Plays a whole [RandomBot] duel from [seed], mirroring [simulateDuel]'s loop exactly — the same
 * match-level [SplitMix64Rng] drawing each hand's seed, the same second, independent
 * [SplitMix64Rng] threading decisions across the whole duel — but keeps every [ObservedStep] of
 * every hand instead of only the accepted actions.
 *
 * Each hand opens with one [ObservedStep] holding [startNextHand]'s opened state and the events
 * that produced it, then appends one [ObservedStep] per accepted action, whose `eventsSoFar` is
 * everything before it plus that action's own events.
 *
 * @param seed the duel's own seed, from which every hand's seed is drawn.
 * @param maxHands the ceiling on hands played before the duel is judged to never terminate.
 * @param maxActionsPerHand the ceiling on actions accepted in one hand before it is judged stuck.
 * @return the finished duel: every hand played, each carrying every state and event along the way.
 * @throws AssertionError, naming the failing seeds, if an action is rejected, a hand exceeds
 *   [maxActionsPerHand], or the duel exceeds [maxHands] with no outcome.
 */
internal fun observeDuel(seed: Long, maxHands: Int = 500, maxActionsPerHand: Int = 200): ObservedDuel {
    val bots = listOf(RandomBot, RandomBot)

    var match = MatchState.start(DuelFormat.DEFAULT)
    var matchRng = SplitMix64Rng(seed)
    var decisionRng: Rng = SplitMix64Rng(seed)
    val hands = mutableListOf<ObservedHand>()

    while (true) {
        val outcome = outcomeOf(match)
        if (outcome != null) {
            return ObservedDuel(seed, hands.toList())
        }

        val handNumber = match.nextHandNumber
        if (handNumber > maxHands) {
            throw AssertionError("${seedReport(seed, 0L)}: duel exceeded $maxHands hands with no outcome")
        }

        val seedDraw = matchRng.nextLong()
        val handSeed = seedDraw.value
        matchRng = seedDraw.next

        val opened = startNextHand(match, SplitMix64Rng(handSeed))
        var state = opened.newState
        val steps = mutableListOf(ObservedStep(state, opened.events))

        while (!state.isHandOver) {
            if (steps.size - 1 >= maxActionsPerHand) {
                throw AssertionError(
                    "${seedReport(seed, handSeed)}: hand $handNumber exceeded $maxActionsPerHand actions",
                )
            }

            val seatToAct = state.seatToAct
                ?: throw AssertionError(
                    "${seedReport(seed, handSeed)}: no seat to act in hand $handNumber " +
                        "(street=${state.street}) but hand is not over",
                )

            val choice = bots[seatToAct].choose(state, legalActions(state), decisionRng)
            val result = DefaultPokerEngine.handle(state, choice.action)
            if (result.isRejected) {
                throw AssertionError(
                    "${seedReport(seed, handSeed)}: action ${choice.action} in hand $handNumber " +
                        "was rejected: ${result.rejection}",
                )
            }

            state = result.newState
            decisionRng = choice.rng
            steps += ObservedStep(state, steps.last().eventsSoFar + result.events)
        }

        hands += ObservedHand(handNumber, handSeed, steps.toList())
        match = recordHand(match, state)
    }
}

/**
 * A reproduction line for a failing assertion: names both [duelSeed] and [handSeed] so a property
 * built on [observeDuel] can be reproduced from its assertion message alone.
 */
internal fun seedReport(duelSeed: Long, handSeed: Long): String = "duel seed $duelSeed, hand seed $handSeed"
