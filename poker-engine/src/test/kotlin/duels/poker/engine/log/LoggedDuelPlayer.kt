package duels.poker.engine.log

import duels.poker.engine.duel.DuelFormat
import duels.poker.engine.duel.MatchState
import duels.poker.engine.duel.matchFinishedEvent
import duels.poker.engine.duel.outcomeOf
import duels.poker.engine.duel.recordHand
import duels.poker.engine.duel.startNextHand
import duels.poker.engine.game.DefaultPokerEngine
import duels.poker.engine.game.PlayerAction
import duels.poker.engine.game.legalActions
import duels.poker.engine.game.pickAction
import duels.poker.engine.random.Rng
import duels.poker.engine.random.SplitMix64Rng

/**
 * Plays one whole duel for [seed] under [format], hand after hand, until [outcomeOf] reports it
 * over, and returns the [MatchLog] that records it.
 *
 * Unlike `playRandomDuel`, every hand gets its own seed: a match-level [SplitMix64Rng] seeded by
 * [seed] yields one `nextLong()` draw per hand — that draw's value is the hand's seed, and its
 * `next` carries the chain forward to the following hand — and each hand is opened with
 * [startNextHand] fed `SplitMix64Rng(handSeed)`. That is what makes every recorded [HandLog]
 * replayable on its own by [replayHand], which reconstructs the hand from `SplitMix64Rng(log.seed)`
 * alone, independent of every other hand in the match.
 *
 * Decisions come from a second, separate [SplitMix64Rng] chain seeded by [seed] and advanced by
 * `pickAction` across the whole duel — the same function `playRandomHand` and `playRandomDuel`
 * use, so this harness makes decisions exactly as they do.
 *
 * The first hand's button is `(seed % 2).toInt()`, exactly as `playRandomDuel` picks it, via
 * `MatchState.start(format, buttonSeat = ...)`.
 *
 * Each hand is driven to `GameState.isHandOver`, folded back into the match with [recordHand],
 * and the loop stops as soon as [outcomeOf] reports the match over, at which point the match log
 * is closed with `MatchLog(format, firstButtonSeat, hands, listOfNotNull(matchFinishedEvent(match)))`.
 *
 * Throws [AssertionError] naming [seed] if [maxHands] hands pass without an outcome — checked
 * before a hand is opened, so `maxHands = 0` fails immediately — if a single hand takes more than
 * [maxActionsPerHand] actions to reach `isHandOver`, if an action is rejected, or if a hand that
 * has not ended has no seat to act: the same failure modes `playRandomDuel` already reports.
 *
 * @param seed the single seed the whole duel, its hands and its decisions are derived from
 * @param format the duel's configuration; defaults to [DuelFormat.DEFAULT]
 * @param maxHands the number of hands the duel may play before it is considered stuck
 * @param maxActionsPerHand the number of actions a single hand may take before it is considered
 *   stuck
 */
internal fun playLoggedDuel(
    seed: Long,
    format: DuelFormat = DuelFormat.DEFAULT,
    maxHands: Int = 500,
    maxActionsPerHand: Int = 200,
): MatchLog {
    val firstButtonSeat = (seed % 2).toInt()
    var match = MatchState.start(format, buttonSeat = firstButtonSeat)
    var handSeedRng = SplitMix64Rng(seed)
    var decisionRng: Rng = SplitMix64Rng(seed)
    val hands = mutableListOf<HandLog>()

    while (true) {
        val outcome = outcomeOf(match)
        if (outcome != null) {
            return MatchLog(format, firstButtonSeat, hands, listOfNotNull(matchFinishedEvent(match)))
        }

        if (hands.size >= maxHands) {
            throw AssertionError(
                "seed $seed: duel did not reach an outcome within $maxHands hands; " +
                    "hands so far: ${hands.size}",
            )
        }

        val handSeedDraw = handSeedRng.nextLong()
        val handSeed = handSeedDraw.value
        handSeedRng = handSeedDraw.next

        val buttonSeat = match.buttonSeat
        val stacks = match.stacks
        val smallBlind = match.blinds.smallBlind
        val bigBlind = match.blinds.bigBlind

        val started = startNextHand(match, SplitMix64Rng(handSeed))
        var state = started.newState
        val actions = mutableListOf<PlayerAction>()
        val events = started.events.toMutableList()
        var actionsInHand = 0

        while (!state.isHandOver) {
            if (actionsInHand >= maxActionsPerHand) {
                throw AssertionError(
                    "seed $seed: hand ${state.handNumber} did not reach a terminal state within " +
                        "$maxActionsPerHand actions",
                )
            }

            val seatToAct = state.seatToAct
            if (seatToAct == null) {
                throw AssertionError(
                    "seed $seed: no seat to act but hand ${state.handNumber} has not ended " +
                        "(street=${state.street})",
                )
            }

            val (action, nextDecisionRng) = pickAction(seatToAct, legalActions(state), decisionRng)
            decisionRng = nextDecisionRng

            val result = DefaultPokerEngine.handle(state, action)
            if (result.isRejected) {
                throw AssertionError(
                    "seed $seed: $action was rejected (${result.rejection}) in hand " +
                        "${state.handNumber} though legalActions allowed it",
                )
            }

            actions += action
            events += result.events
            state = result.newState
            actionsInHand += 1
        }

        hands += HandLog(
            seed = handSeed,
            handNumber = state.handNumber,
            buttonSeat = buttonSeat,
            stacks = stacks,
            smallBlind = smallBlind,
            bigBlind = bigBlind,
            actions = actions,
            events = events,
        )
        match = recordHand(match, state)
    }
}
