package duels.poker.engine.game

/**
 * Folds one [BettingEvent] into the [GameState] it happened in, producing the state after it.
 *
 * A `BettingEvent`'s `to` amount is the seat's **street total after the action**, not an
 * increment — the chips that actually moved are `to - committedThisStreet`, which only the
 * state knows, so this function folds rather than sums (see the KDoc on `BettingEvent`).
 *
 * Every branch ends with `seatToAct = null`: the actor for the *next* action is named by the
 * next `ActionOn` event, never inferred here.
 *
 * `PlayerAllIn`'s effect on `minRaiseTo` follows `docs/duel-rules.md`: a full all-in raises the
 * bar exactly as an ordinary raise would, but a short all-in call does not raise it. The same
 * split governs `lastAggressor`: a full all-in is aggression and is recorded, but a short all-in
 * call for less than the bar is a forced call, not a challenge, and leaves `lastAggressor`
 * untouched. `lastAggressor` is what a showdown reads to decide who shows first — see
 * [`ADR-0008`](../../../../../../../../docs/adr/ADR-0008-loser-mucks-at-showdown.md). Whether a
 * seat that faced a short all-in is then allowed to raise at all is `TASK-010502`'s decision,
 * not this function's — this only replays what the log says happened.
 *
 * Callers should not reach for this directly; it exists so `StateProjection.apply`
 * (`TASK-010425`) can dispatch to it for the betting branch of the full event hierarchy.
 */
public fun applyBetting(state: GameState, event: BettingEvent): GameState {
    val folded = when (event) {
        is PlayerFolded -> state.withSeat(event.seat) { it.copy(hasFolded = true) }
        is PlayerChecked -> state
        is PlayerCalled -> commitTo(state, event.seat, event.to)
        is PlayerBet -> raiseTo(state, event.seat, event.to).copy(lastAggressor = event.seat)
        is PlayerRaised -> raiseTo(state, event.seat, event.to).copy(lastAggressor = event.seat)
        is PlayerAllIn -> allInTo(state, event.seat, event.to)
    }
    return folded.copy(seatToAct = null, eventCount = event.sequence + 1)
}

/** Moves [seat]'s commitment up to the street total [to]; overdrawing the stack throws. */
private fun commitTo(state: GameState, seat: Int, to: Int): GameState {
    return state.withSeat(seat) { it.commit(to - it.committedThisStreet) }
}

/** A bet or a raise: commit up to [to], then set the bar and double the raise increment. */
private fun raiseTo(state: GameState, seat: Int, to: Int): GameState {
    val increment = to - state.betToMatch
    return commitTo(state, seat, to).copy(betToMatch = to, minRaiseTo = to + increment)
}

/**
 * An all-in: commit up to [to] and force `isAllIn`, then, only if it moves the bar past where
 * it stood, raise `minRaiseTo` by the same increment a normal raise would and record [seat] as
 * the last aggressor. A short all-in — one that does not clear the existing bar — is a forced
 * call, not aggression: it does not challenge the opponent to act, so it leaves `lastAggressor`
 * exactly as it found it.
 */
private fun allInTo(state: GameState, seat: Int, to: Int): GameState {
    val committed = commitTo(state, seat, to).withSeat(seat) { it.copy(isAllIn = true) }
    if (to <= state.betToMatch) return committed

    val increment = to - state.betToMatch
    val minRaiseTo = if (to >= state.minRaiseTo) to + increment else state.minRaiseTo
    return committed.copy(betToMatch = to, minRaiseTo = minRaiseTo, lastAggressor = seat)
}
