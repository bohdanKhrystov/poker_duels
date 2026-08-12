// The ticket names this file after its entry point, playRandomDuel, not after PlayedDuel, the
// one data class it also declares — ktlint's file-naming heuristic disagrees.
@file:Suppress("ktlint:standard:filename")

package duels.poker.engine.duel

import duels.poker.engine.game.DefaultPokerEngine
import duels.poker.engine.game.legalActions
import duels.poker.engine.game.pickAction
import duels.poker.engine.random.Rng
import duels.poker.engine.random.SplitMix64Rng

/**
 * One hand as it settled inside a duel: the hand it was, the button and blinds it was played at,
 * and both stacks once it ended.
 */
internal data class HandSummary(
    val handNumber: Int,
    val buttonSeat: Int,
    val blinds: BlindLevel,
    val stacksAfter: List<Int>,
)

/**
 * The state a random duel for [seed] played out to: [outcome] is the settled [DuelOutcome], and
 * [hands] is every hand played to reach it, in order.
 */
internal data class PlayedDuel(
    val seed: Long,
    val format: DuelFormat,
    val outcome: DuelOutcome,
    val hands: List<HandSummary>,
)

/**
 * Plays one whole duel for [seed] under [format], hand after hand, until [outcomeOf] reports it
 * over. Each hand is opened with [startNextHand] and driven to `GameState.isHandOver` by feeding
 * decisions through `pickAction` — the same function `playRandomHand` uses, so both harnesses make
 * decisions the same way — to [DefaultPokerEngine], then folded back into the match with
 * [recordHand].
 *
 * Two random sources, exactly as `playRandomHand` splits them: the shuffle [Rng] is carried
 * forward from the finished hand's `state.rng` after every hand, so one seed deals a different
 * board each time; the decision `Rng` is a separate [SplitMix64Rng] seeded by [seed], advanced by
 * `pickAction` across the whole duel.
 *
 * Throws [AssertionError] naming [seed] if [maxHands] hands pass without an outcome — checked
 * before a hand is opened, so `maxHands = 0` fails immediately — or if a single hand takes more
 * than [maxActionsPerHand] actions to reach `isHandOver`. A rejected action or a `null` seat to
 * act in a hand that has not ended each throw the same way: this harness does not re-assert the
 * per-action betting or settlement invariants, which `BettingInvariantTest` and
 * `SettlementInvariantTest` already own.
 *
 * @param seed the single seed the whole duel is replayed from
 * @param format the duel's configuration; defaults to [DuelFormat.DEFAULT]
 * @param maxHands the number of hands the duel may play before it is considered stuck
 * @param maxActionsPerHand the number of actions a single hand may take before it is considered
 *   stuck
 */
internal fun playRandomDuel(
    seed: Long,
    format: DuelFormat = DuelFormat.DEFAULT,
    maxHands: Int = 500,
    maxActionsPerHand: Int = 200,
): PlayedDuel {
    var match = MatchState.start(format, buttonSeat = (seed % 2).toInt())
    var shuffleRng: Rng = SplitMix64Rng(seed)
    var decisionRng: Rng = SplitMix64Rng(seed)
    val hands = mutableListOf<HandSummary>()

    while (true) {
        val outcome = outcomeOf(match)
        if (outcome != null) {
            return PlayedDuel(seed, format, outcome, hands)
        }

        if (hands.size >= maxHands) {
            throw AssertionError(
                "seed $seed: duel did not reach an outcome within $maxHands hands; " +
                    "hands so far: $hands",
            )
        }

        val started = startNextHand(match, shuffleRng)
        var state = started.newState
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

            state = result.newState
            actionsInHand += 1
        }

        shuffleRng = state.rng
        hands += HandSummary(
            handNumber = state.handNumber,
            buttonSeat = state.buttonSeat,
            blinds = BlindLevel(state.smallBlind, state.bigBlind),
            stacksAfter = state.seats.map { it.stack },
        )
        match = recordHand(match, state)
    }
}
