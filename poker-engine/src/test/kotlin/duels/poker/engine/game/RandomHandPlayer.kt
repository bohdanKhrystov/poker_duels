// The ticket names this file after its entry point, playRandomHand, not after PlayedHand, the
// one data class it also declares — ktlint's file-naming heuristic disagrees.
@file:Suppress("ktlint:standard:filename")

package duels.poker.engine.game

import duels.poker.engine.card.Deck
import duels.poker.engine.random.Rng
import duels.poker.engine.random.SplitMix64Rng

/**
 * The state a random hand for [seed] played out to: [opening] is the pre-[HandStarted] state
 * from which [events] can be folded to recover [finalState] via [StateProjection.fold] — every
 * field but `deck` and `rng`, which no event ever carries. [actions] is the sequence of player
 * decisions the harness fed to the engine, in order.
 */
internal data class PlayedHand(
    val opening: GameState,
    val actions: List<PlayerAction>,
    val events: List<GameEvent>,
    val finalState: GameState,
)

private const val MIN_STACK = 60
private const val MAX_STACK = 10_000
private const val STACK_SPAN = (MAX_STACK - MIN_STACK + 1).toLong()

// SMALL_BLIND and BIG_BLIND (50/100) come from GameStates.kt, the shared test fixture.

// Two different multiplicative hash constants so the two stacks vary independently across
// consecutive seeds; the arithmetic itself carries no meaning beyond spreading seeds 1..1000
// over 60..10_000, including the low end where a blind forces an all-in.
private const val STACK_MULTIPLIER_A = 2_654_435_761L
private const val STACK_MULTIPLIER_B = 40_503L

/**
 * Plays one heads-up hand for [seed] against [engine], start to finish, checking the betting
 * invariants after every action. Every decision — which legal action, which amount when several
 * are legal — is drawn from a single [SplitMix64Rng] seeded by [seed], so the same seed always
 * replays the same hand: no platform random source, anywhere.
 *
 * Throws [AssertionError] the instant an invariant breaks, or once [maxActions] actions have
 * been taken without reaching a terminal state, naming [seed] and the actions taken so far —
 * a hand that cannot progress is a bug this function should surface, not hide behind a timeout.
 */
internal fun playRandomHand(
    seed: Long,
    engine: PokerEngine = DefaultPokerEngine,
    maxActions: Int = 200,
): PlayedHand {
    val buttonSeat = (seed % 2).toInt()
    val stacks = stacksFor(seed)

    // The state HandStarted resets everything but deck, rng and eventCount, so any state that
    // passes GameState's own invariants works here — it exists only so the log can be replayed
    // from scratch by StateProjection.fold.
    val opening = GameState(
        handNumber = 1,
        buttonSeat = buttonSeat,
        street = Street.PREFLOP,
        seats = stacks.mapIndexed { index, stack -> Seat(index = index, stack = stack) },
        board = Board.EMPTY,
        pot = 0,
        betToMatch = 0,
        minRaiseTo = BIG_BLIND,
        seatToAct = null,
        smallBlind = SMALL_BLIND,
        bigBlind = BIG_BLIND,
        eventCount = 0,
        deck = Deck.full(),
        rng = SplitMix64Rng(seed),
    )

    val started = startHand(1, buttonSeat, stacks, SMALL_BLIND, BIG_BLIND, SplitMix64Rng(seed))
    val chipsAtOpen = started.newState.chipsInPlay

    var state = started.newState
    val events = started.events.toMutableList()
    val actions = mutableListOf<PlayerAction>()
    var decisionRng: Rng = SplitMix64Rng(seed)

    while (!isHandEnded(state)) {
        if (actions.size >= maxActions) {
            throw AssertionError(
                "seed $seed: hand did not reach a terminal state within $maxActions actions; " +
                    "actions so far: $actions",
            )
        }

        val seatToAct = state.seatToAct
        val legal = legalActions(state)
        if (seatToAct == null || legal.allowed.isEmpty()) {
            throw AssertionError(
                "seed $seed: no seat to act but the hand has not ended (street=${state.street}, " +
                    "folded=${state.seats.map { it.hasFolded }}); actions so far: $actions",
            )
        }

        val actingSeat = state.seat(seatToAct)
        if (actingSeat.hasFolded || actingSeat.isAllIn || actingSeat.stack == 0) {
            throw AssertionError(
                "seed $seed: seat $seatToAct is to act but hasFolded=${actingSeat.hasFolded}, " +
                    "isAllIn=${actingSeat.isAllIn}, stack=${actingSeat.stack}; actions so far: $actions",
            )
        }

        val (action, nextRng) = pickAction(seatToAct, legal, decisionRng)
        decisionRng = nextRng

        val result = engine.handle(state, action)
        if (result.isRejected) {
            throw AssertionError(
                "seed $seed: $action was rejected (${result.rejection}) though legalActions allowed it; " +
                    "actions so far: $actions",
            )
        }

        assertDenseEvents(seed, actions + action, state.eventCount, result.events)

        actions += action
        events += result.events
        state = result.newState

        assertChipsConserved(seed, actions, chipsAtOpen, state)
        assertNoNegativeStacksOrOvercommit(seed, actions, state)
        assertBetToMatchCoversCommitments(seed, actions, state)
    }

    return PlayedHand(opening, actions, events, state)
}

/**
 * Every path out of a hand now settles — a fold, a closed river, or a run-out all end in
 * [Street.COMPLETE] with `HandFinished` on the log (`STORY-0106`). A hand that stops anywhere
 * else is a bug the harness should surface, not a shape it should accept, so this narrows to
 * `state.isHandOver` alone: a hand that never settles runs to `maxActions` and throws naming the
 * seed instead of quietly declaring itself over.
 */
private fun isHandEnded(state: GameState): Boolean = state.isHandOver

/** Both seats' starting stacks, spread over `60..10_000` so short-stack all-ins occur. */
private fun stacksFor(seed: Long): List<Int> {
    val stack0 = MIN_STACK + (seed * STACK_MULTIPLIER_A).mod(STACK_SPAN).toInt()
    val stack1 = MIN_STACK + ((seed + 1) * STACK_MULTIPLIER_B).mod(STACK_SPAN).toInt()
    return listOf(stack0, stack1)
}

/**
 * One uniformly chosen legal action for [seat], drawn from [legal] via [rng]: which action type
 * comes from a uniform draw over [LegalActions.allowed] sorted for determinism, and a `BET` or
 * `RAISE` amount is a further uniform draw between the minimum legal total and going all-in.
 */
private fun pickAction(seat: Int, legal: LegalActions, rng: Rng): Pair<PlayerAction, Rng> {
    val sortedTypes = legal.allowed.sorted()
    val typeDraw = rng.nextInt(sortedTypes.size)
    var nextRng = typeDraw.next

    val action: PlayerAction = when (sortedTypes[typeDraw.value]) {
        ActionType.FOLD -> PlayerAction.Fold(seat)
        ActionType.CHECK -> PlayerAction.Check(seat)
        ActionType.CALL -> PlayerAction.Call(seat)
        ActionType.BET -> {
            val amountDraw = nextRng.nextInt(2)
            nextRng = amountDraw.next
            PlayerAction.Bet(seat, if (amountDraw.value == 0) legal.minBetTo else legal.allInTo)
        }
        ActionType.RAISE -> {
            val amountDraw = nextRng.nextInt(2)
            nextRng = amountDraw.next
            PlayerAction.Raise(seat, if (amountDraw.value == 0) legal.minRaiseTo else legal.allInTo)
        }
        ActionType.ALL_IN -> PlayerAction.AllIn(seat)
    }

    return action to nextRng
}

private fun assertDenseEvents(seed: Long, actionsSoFar: List<PlayerAction>, startingSequence: Int, newEvents: List<GameEvent>) {
    var expected = startingSequence
    for (event in newEvents) {
        if (event.sequence != expected) {
            throw AssertionError(
                "seed $seed: event sequence ${event.sequence} is not dense, expected $expected in $newEvents; " +
                    "actions so far: $actionsSoFar",
            )
        }
        expected += 1
    }
}

private fun assertChipsConserved(seed: Long, actionsSoFar: List<PlayerAction>, chipsAtOpen: Int, state: GameState) {
    if (state.chipsInPlay != chipsAtOpen) {
        throw AssertionError(
            "seed $seed: chipsInPlay drifted from $chipsAtOpen to ${state.chipsInPlay}; " +
                "actions so far: $actionsSoFar",
        )
    }
}

private fun assertNoNegativeStacksOrOvercommit(seed: Long, actionsSoFar: List<PlayerAction>, state: GameState) {
    for (seat in state.seats) {
        if (seat.stack < 0) {
            throw AssertionError(
                "seed $seed: seat ${seat.index} has a negative stack ${seat.stack}; actions so far: $actionsSoFar",
            )
        }
        if (seat.committedThisStreet > seat.committedThisHand) {
            throw AssertionError(
                "seed $seed: seat ${seat.index} committedThisStreet ${seat.committedThisStreet} exceeds " +
                    "committedThisHand ${seat.committedThisHand}; actions so far: $actionsSoFar",
            )
        }
    }
}

private fun assertBetToMatchCoversCommitments(seed: Long, actionsSoFar: List<PlayerAction>, state: GameState) {
    val maxCommitted = state.seats.maxOf { it.committedThisStreet }
    if (state.betToMatch < maxCommitted) {
        throw AssertionError(
            "seed $seed: betToMatch ${state.betToMatch} is less than the largest committedThisStreet " +
                "$maxCommitted; actions so far: $actionsSoFar",
        )
    }
}
