# ADR-0043 — A rejection closes no decision point; the client keeps the turn and counts the refusal

- **Status:** Accepted
- **Date:** 2026-08-15

## Context

`DuelAction.act` answers an action the engine refuses with the `Rejected` frame alone — no fresh
`YourTurn` follows one. `web-client/src/store/duel-state.ts` reduces that frame to
`{ ...state, pendingTurn: null, rejection: message.rejection }`, and nothing anywhere ever clears
`rejection`. Together those two facts mean one rejected action ends that player's ability to act for
the rest of the duel, with both players still connected, and leaves the refusal's sentence on screen
forty hands later. `STORY-0307`'s fifth acceptance criterion cannot be met while either holds.

What makes this a decision rather than a bug fix is that the two obvious repairs read `ADR-0002`
differently.

**The strict reading.** The client may hold only what the server is actively asserting. A turn the
server has not re-stated is not the client's to keep, so the server must re-prompt after a rejection
and the client's reducer is already right.

**The other reading.** A store *is* retained server statements — the client holds a `view` from the
last `Snapshot` across a dozen `Events` frames and nobody calls that an assertion. The only question
is which frames retract which statements, and a `Rejected` reports on an *attempt*, not on state.

The server's own code decides between them. `Rejection`'s KDoc says a rejection "never throws and
never changes state", and `act` returns the runner verbatim in that branch. So after an engine
rejection `state.handNumber` is unchanged, the hand's events are unchanged — hence the last
`ActionOn`, which is where `guard` reads `actionSequence` from — and `state.seatToAct` is unchanged.
A second `Act` bearing exactly the identity the client already holds passes `guard` and reaches the
engine. **The decision point is still open on the server.** A client that clears `pendingTurn` is
not being cautious; it is inferring a state change from a frame whose whole content is that no state
changed. That is derivation, and this epic forbids it.

Three more facts constrain the shape of the answer, none of them visible when the story was split:

- **The bar does not come back on its own.** `TASK-030707` makes the in-flight lock a `useState`
  inside `Live`, reset by remounting on `key={`${handNumber}:${actionSequence}`}`. Keeping
  `pendingTurn` *unchanged* therefore leaves the key unchanged, `Live` unmounted-never, and `sent`
  stuck at `true`. Whatever answer is chosen has to move something the bar can see.
- **`PendingTurn` cannot carry it.** `TASK-030701` pins `Object.keys(aTurn()).sort()` to exactly
  `actionSequence`, `handNumber`, `legalActions`, because that object means *what the server sent*.
- **Only the store sees frames as events.** A component sees state. "A rejection happened" is an
  event, so the store is the only layer that can turn it into a value that changes.

Finally, the frequency. A correct bar renders only `legalActions.allowed` and clamps its amount to
the server's own bounds, so it can barely produce a rejection at all: in practice they come from
races, from a server that has moved on, and from hand-written clients. That argues for the cheapest
answer that is correct, not the most elaborate one.

## Decision

**A `Rejected` closes nothing.** It reports on an attempt, so it changes only what the client knows
about that attempt.

1. The reducer's `Rejected` case leaves `pendingTurn` and `view` untouched. `TASK-030404`'s merged
   test `a rejected action clears the pending turn` states the behaviour this reverses and is
   rewritten, not deleted quietly.
2. `DuelState` gains `readonly rejectionCount: number`, `0` in `initialState()`, incremented by
   every `Rejected` and never reset. It is client bookkeeping, not a game fact, and carries a
   comment saying so.
3. A rejection is shown until the server next speaks about the game: `YourTurn`, `Snapshot` and
   `DuelFinished` each set `rejection` to `null`. `Events` does not — it is narration, and every
   `Events` frame the server sends is accompanied by a `Snapshot`.
4. The action bar's remount key becomes
   `` `${turn.handNumber}:${turn.actionSequence}:${rejectionCount}` ``, and `Lobby.tsx` passes
   `rejectionCount={state.rejectionCount}` beside the three props it already passes.
   `TASK-030707`'s mechanism is preserved exactly: the lock lifts because React unmounts the old
   `Live`, never because anything clears it, and `ActionBar.tsx` keeps its no-`useEffect`,
   no-`useRef` rule.
5. **The reducer never reads the `Rejection` variant.** All five reduce identically. `guard` answers
   `NOT_YOUR_TURN` both when the sender is not on turn *and* when the sender is on turn but named
   the opponent in `action.seat`; in that second case `Rejection.NotYourTurn` carries the
   recipient's **own** seat. A reducer that closed the turn on `NotYourTurn` would switch the bar
   off during the player's own turn.
6. **The server is not changed.** `DuelAction.act` keeps answering a rejection with the `Rejected`
   frame alone, and `YourTurn` keeps meaning *a new decision point has opened*.
7. `docs/protocol.md` states the invariant the client now leans on: a `Rejected` closes no decision
   point, and the `handNumber`/`actionSequence` pair the client holds stays valid until a frame that
   reports state names a different one. No frame shape changes; `PROTOCOL_VERSION` does not move.

## Consequences

**What it buys**

- A duel no longer ends for a player because one frame was refused, and the refusal's sentence stops
  outliving the attempt it describes.
- The client's model becomes *more* faithful, not less: it holds the turn for exactly as long as the
  server's own `guard` would accept it.
- The wire is untouched. No version move, no new frame, and `YourTurn` keeps its single meaning, so
  `EPIC-09`'s bot and anything that ever animates or sounds on a turn can keep treating it as an
  edge rather than a level.
- It is the cheapest answer to reverse — reducer rules in one file and one key string. If it is
  wrong, the next ADR deletes a field.

**What it costs**

- **`DuelState` stops being only what the server sent.** `STORY-0304` is titled *state is the last
  frame the server sent*, and `rejectionCount` is the first field that is not. That is a real
  erosion of a stated invariant and it will invite the next one. The field carries a comment; a
  second field of its kind should be argued in an ADR rather than added.
- **Every rejection remounts the bar, so the amount the player had dialled is lost** and the slider
  returns to `minRaiseTo` — including when the rejection had nothing to do with the amount
  (`ActionNotAllowed`, `HandComplete`). Preserving it would need the component to distinguish
  rejection variants, which is the derivation this epic forbids, so the reset is accepted.
- **The client can now hold a decision point the server has moved past.** Because the reducer
  ignores the variant, a `NotYourTurn` leaves the bar live; a click then sends a frame the server
  drops as `STALE_FRAME` — in complete silence — and the player sees nothing happen at all. The
  window is bounded, since the opponent's next action broadcasts a `Snapshot` that clears
  `pendingTurn`, but it is a window in which the bar lies, and it did not exist before.
- **Two identical refusals in a row look like one.** The second `Rejected` sets a deep-equal
  `rejection`, so the sentence does not change; the only feedback that the second attempt was
  refused too is the bar visibly resetting. Named here rather than discovered later.
- **The invariant is documented, not tested.** Nothing in `poker-server` proves that an `Act`
  identity survives a rejection — `DuelActionTest` proves a *replay* is dropped and that a rejection
  carries the engine's reason, and neither pins this. A future server change that made a rejection
  append an `ActionOn`, or advance the hand number, would silently turn every retry into a dropped
  frame: a player clicking into total silence. The test that closes it belongs to the server, not
  the client — `poker-server` asserts that after an engine rejection the same `Act` still passes
  `guard` — and is worth one ticket against the epic that owns the server.
- **The fix does not fit one schema-2 ticket.** Five files (`duel-state.ts`, `duel-state.test.ts`,
  `ActionBar.tsx`, `ActionBar.test.tsx`, `Lobby.tsx`) exceed `lint_tickets.py`'s
  `MAX_FILES_TOUCHED = 3`, so `TASK-030712` becomes the store half and the bar half is a sibling
  ticket. `STORY-0307`'s fifth acceptance criterion closes when both land.

**What it forecloses**

- **The server will not re-prompt.** A later client — a bot, a native app — that wants to be
  prompted again after a rejection must keep the turn as this one does, or supersede this ADR.
- **The reducer will not branch on `Rejection` variants.** If a variant is ever added that genuinely
  closes a decision point, the wire must report that as state, not as a rejection the client is
  expected to interpret.
- **`refusal` is untouched.** `Failure{DUEL_PAUSED}` says *do not re-send*, so leaving the bar
  locked there is correct; that its sentence also never clears is the same lifetime bug in a
  different field, and this ADR deliberately does not fix it. It is worth one ticket.

**Timing.** There is no deadline that makes this free today and impossible later, but the cost of
reversing grows: `STORY-0308`, `0310` and `0312` are all written against whichever reading of
`pendingTurn` stands. The one option that is genuinely hard to reverse is the server re-prompt,
because removing a frame that clients have come to depend on breaks them silently — which is a
reason to decide now, and a reason not to decide *that*.

## Alternatives considered

**1. The server re-prompts after a rejection.** Its strongest case is real and was nearly decisive:
it is the only answer needing *no client change at all* for the "act again" half. Today's reducer
clears `pendingTurn` on `Rejected`, which unmounts `Live`; the re-prompt's `YourTurn` then mounts a
fresh one with the lock already lifted, so the bar returns by construction. It is also the strictest
reading of `ADR-0002` — the client holds a turn only while the server is asserting it — and it is
four lines in `DuelAction.act` with no protocol version move, since the frame shape is unchanged.
Rejected because: it repairs a client inference with wire traffic, leaving the wrong reducer line in
place forever, propped up by a frame; it redefines `YourTurn` from *a new decision point opened* to
*a decision point is open*, a semantic every present and future consumer must then absorb; between
the two frames the client still believes, briefly and wrongly, that it has no turn, so on a slow
link the bar blinks out and back; and the four lines hide a trap — `turnFor` addresses the seat on
turn, so re-prompting from the `NOT_YOUR_TURN` guard branch would send a duplicate prompt to the
*opponent*, for a frame the opponent never sent. The asymmetry settles it: a correct client makes
the re-prompt unnecessary, while the re-prompt does not make an incorrect client correct.

**2. The bar keeps its own copy of the turn and restores it.** Its strongest case is that it is the
smallest diff in the repository — nothing in the store moves, `TASK-030404`'s merged test stays
green, and the bar already receives the `rejection` it would key off. Rejected because it puts *"it
is still your turn"* inside a component where no store test can see it, and because noticing that a
prop changed needs `useEffect` or `useRef`, both banned by `TASK-030707`'s acceptance criteria. It
also cannot count: with `rejection` as the only signal, a second refusal at the same decision point
changes nothing and the bar stays dead — the same bug, one attempt later.

**3. Delete the in-flight lock; the bar is live whenever `pendingTurn` is non-null.** Its strongest
case is that it is cheaper than every other option and safe on the wire: `guard` turns a duplicate
`Act` into a `STALE_FRAME` the server drops silently, so a double click cannot act twice, and it
*removes* state rather than adding it. Rejected because it contradicts `STORY-0307`'s own design
note — "after sending, controls are disabled until the next `YourTurn`, `Rejected` or `Snapshot`" —
reverses five merged tests from `TASK-030707`, and leaves a bar that is still live after the player
has folded, which is the client implying nothing has happened: the closest this screen can come to
an optimistic UI.

**4. Put the counter on `PendingTurn` instead of beside it.** Its strongest case is that it costs no
new prop and no `Lobby.tsx` change, keeping the whole fix inside three files and therefore inside
one ticket. Rejected because `PendingTurn` is the verbatim content of a `YourTurn` and
`TASK-030701` pins its key set to exactly the wire's three fields for that reason. Mixing client
bookkeeping into the one object that means *what the server sent* is precisely the confusion this
epic is most likely to be killed by; a sibling field says what it is.

**5. Clear the rejection when the player sends the next `Act`, rather than when the server next
speaks.** Its strongest case is that this is the exact moment the sentence stops describing the
latest attempt, and it removes the one genuinely confusing case this decision accepts — a second
identical refusal whose sentence does not change. Rejected because the store has no outbound path:
`ADR-0032` puts sending in `useSend()` from event handlers and keeps the store inbound-only, so this
means either teaching the store about sends — amending `ADR-0032` for one sentence's lifetime — or
hiding the notice from inside the component while `sent` is true, which is derivation in a component
again. A timer or a dismiss button was not considered seriously: nothing on the wire carries a
duration, and when a player is told something is a product question, not this one's.
