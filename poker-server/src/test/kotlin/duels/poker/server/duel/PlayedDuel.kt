package duels.poker.server.duel

import duels.poker.engine.duel.DuelFormat
import duels.poker.engine.game.ActionType
import duels.poker.engine.game.PlayerAction
import duels.poker.engine.random.Rng
import duels.poker.engine.random.SplitMix64Rng
import duels.poker.server.protocol.Act
import duels.poker.server.protocol.ServerMessage

/**
 * The whole record of one duel played to completion through the runner, as a real client would
 * play it: by answering every `YourTurn` frame and nothing else.
 *
 * @property seed the seed [playDuel] was called with.
 * @property runner the duel's final state: `hand == null`, `outcome` set.
 * @property outbound every [Addressed] frame the runner produced, in order, starting with
 *   `startDuel`'s.
 * @property actions the number of `Act` frames sent to reach that outcome.
 */
internal data class PlayedDuel(
    val seed: Long,
    val runner: DuelRunner,
    val outbound: List<Addressed>,
    val actions: Int,
)

/**
 * Plays a whole duel from the first deal to `MatchFinished`, seeing only what a client would see.
 *
 * Each turn, the loop takes the single [Addressed] frame whose message is a
 * `ServerMessage.YourTurn`, and answers it with an action drawn uniformly from
 * `turn.legalActions` — never by reading a `GameState`, a `LegalActions` computed from one, or the
 * runner's logs. This keeps every statement built from a [PlayedDuel] a statement about the
 * boundary a client sees, not about the engine behind it.
 *
 * Determinism is total: one [SplitMix64Rng] seeded with [seed] drives the action policy, and a
 * second, independent [SplitMix64Rng] seeded with [seed] drives the [HandSeedSource] behind every
 * hand after the first. No `kotlin.random.Random`, no clock, no I/O — calling `playDuel(seed)`
 * twice produces equal [PlayedDuel]s.
 *
 * @param seed the opening hand's seed, and the seed of both generators derived from it.
 * @param format the duel's configuration: starting stack, blind schedule, end condition.
 * @param buttonSeat the seat holding the button for the first hand.
 * @param maxActions the ceiling on `Act` frames sent, guarding against a duel that never ends.
 * @return the finished duel and every frame produced while playing it.
 * @throws AssertionError naming [seed], if [maxActions] is exceeded or a step ever offers more
 *   than one `YourTurn` frame.
 */
internal fun playDuel(
    seed: Long,
    format: DuelFormat = DuelFormat.DEFAULT,
    buttonSeat: Int = 0,
    maxActions: Int = 20_000,
): PlayedDuel {
    var policyRng: Rng = SplitMix64Rng(seed)
    var seedRng: SplitMix64Rng = SplitMix64Rng(seed)
    val seeds = HandSeedSource {
        val draw = seedRng.nextLong()
        seedRng = draw.next
        draw.value
    }

    var step = startDuel(format, buttonSeat, seed)
    val outbound = mutableListOf<Addressed>()
    outbound += step.outbound
    var actions = 0

    while (true) {
        val turns = step.outbound.filter { it.message is ServerMessage.YourTurn }
        if (turns.size > 1) {
            throw AssertionError("seed $seed: step offered ${turns.size} YourTurn frames, expected at most one")
        }
        val turnFrame = turns.singleOrNull() ?: break

        if (actions >= maxActions) {
            throw AssertionError("seed $seed: exceeded maxActions ($maxActions)")
        }

        val turn = turnFrame.message as ServerMessage.YourTurn
        val legal = turn.legalActions
        val types = legal.allowed.sorted()
        val typeDraw = policyRng.nextInt(types.size)
        policyRng = typeDraw.next
        val type = types[typeDraw.value]

        val action: PlayerAction =
            when (type) {
                ActionType.FOLD -> PlayerAction.Fold(legal.seat)
                ActionType.CHECK -> PlayerAction.Check(legal.seat)
                ActionType.CALL -> PlayerAction.Call(legal.seat)
                ActionType.ALL_IN -> PlayerAction.AllIn(legal.seat)
                ActionType.BET -> {
                    val amountDraw = policyRng.nextInt(2)
                    policyRng = amountDraw.next
                    val to = if (amountDraw.value == 0) legal.minBetTo else legal.allInTo
                    PlayerAction.Bet(legal.seat, to)
                }
                ActionType.RAISE -> {
                    val amountDraw = policyRng.nextInt(2)
                    policyRng = amountDraw.next
                    val to = if (amountDraw.value == 0) legal.minRaiseTo else legal.allInTo
                    PlayerAction.Raise(legal.seat, to)
                }
            }

        val message = Act(turn.handNumber, turn.actionSequence, action)
        step = act(step.runner, turnFrame.seat, message, seeds)
        outbound += step.outbound
        actions++
    }

    check(step.runner.hand == null)

    return PlayedDuel(seed, step.runner, outbound, actions)
}
