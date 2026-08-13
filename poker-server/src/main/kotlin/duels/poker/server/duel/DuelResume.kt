package duels.poker.server.duel

/**
 * Rebuilds, from the duel's live state, the frames [seat] is entitled to see right now.
 *
 * A returning player may have missed events while away, so this never replays what it missed.
 * For a live hand it hands [framesFor] the current state with `newEvents` empty: [broadcast]
 * only emits an `Events` frame when a seat's visible new events are non-empty, so passing none
 * produces none — just a fresh `Snapshot`, plus a `YourTurn` if [seat] is the one holding play
 * up. The `Snapshot` is already the authoritative last word on state; resending `handEvents` as
 * `newEvents` would restate the same facts a second time — and would still need the very
 * per-seat filtering the snapshot already carries — so it would be redundant, not more complete.
 *
 * A finished duel has no game state left to project: the hand is `null` and the duel's one
 * remaining fact is its outcome, which is exactly what [finishedFrames] carries. Nothing else is
 * built for that case; that already *is* "a reconnect after the duel has finished gets the
 * finished state, not a resumed duel" — there is no final snapshot to invent.
 *
 * This function constructs no frame of its own. It only calls [framesFor] and [finishedFrames] —
 * the two functions the projection layer already exposes — and filters their result to [seat].
 *
 * @param runner The duel to resume.
 * @param seat The returning seat: 0 or 1.
 * @return The frames [seat] is entitled to see: a live hand's snapshot (and turn prompt, if any),
 *   or a finished duel's single `DuelFinished` frame.
 * @throws IllegalArgumentException if [seat] is not 0 or 1.
 */
public fun resumeFrames(runner: DuelRunner, seat: Int): List<Addressed> {
    require(seat in 0..1) { "seat must be 0 or 1, was $seat" }

    val hand = runner.hand
    val frames =
        if (hand != null) {
            framesFor(hand.state, newEvents = emptyList(), handEvents = hand.log.events)
        } else {
            val outcome =
                checkNotNull(runner.outcome) {
                    "a duel with no live hand must have an outcome, per DuelRunner's own init"
                }
            finishedFrames(outcome)
        }

    return frames.filter { it.seat == seat }
}
