---
schema: 2
id: TASK-141108
title: The read beat is spent once, and a repeat of the same hand is a step
type: task
status: blocked
parent: STORY-1411
module: web-client
estimate: S
tier: sonnet
review: standard
files_touched: 2
labels: [client, table, store, bug]
depends_on: [TASK-141106]
verify:
  - cd web-client && npm ci && FORCE_COLOR=0 NO_COLOR=1 npm run --silent check
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/store/duel-state.test.ts > "${TMPDIR:-/tmp}/state-after.txt" 2>&1; grep -qF 'Tests  95 passed (95)' "${TMPDIR:-/tmp}/state-after.txt"
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/e2e > "${TMPDIR:-/tmp}/e2e-after.txt" 2>&1; grep -qF 'Test Files  7 passed (7)' "${TMPDIR:-/tmp}/e2e-after.txt" && grep -qF 'Tests  55 passed (55)' "${TMPDIR:-/tmp}/e2e-after.txt"
  - cd web-client && rm -f "${TMPDIR:-/tmp}/state-no-guard.txt" && cp src/store/duel-state.ts "${TMPDIR:-/tmp}/duel-state.fixed.ts" && perl -0pi -e 's/rivalCardsShown\(view\) && !alreadyShown/rivalCardsShown(view)/' src/store/duel-state.ts && ! cmp -s "${TMPDIR:-/tmp}/duel-state.fixed.ts" src/store/duel-state.ts && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/store/duel-state.test.ts > "${TMPDIR:-/tmp}/state-no-guard.txt" 2>&1; cp "${TMPDIR:-/tmp}/duel-state.fixed.ts" src/store/duel-state.ts; cmp -s "${TMPDIR:-/tmp}/duel-state.fixed.ts" src/store/duel-state.ts && grep -qF 'FAIL  src/store/duel-state.test.ts > the duel state > a hand-ending view delivered a second time is a step, not a second read' "${TMPDIR:-/tmp}/state-no-guard.txt" && grep -qF 'FAIL  src/store/duel-state.test.ts > the duel state > a repeat queued behind the beat it repeats is a step when the queue drains' "${TMPDIR:-/tmp}/state-no-guard.txt" && grep -qF 'Tests  2 failed | 93 passed (95)' "${TMPDIR:-/tmp}/state-no-guard.txt"
  - cd web-client && rm -f "${TMPDIR:-/tmp}/state-any-hand.txt" && cp src/store/duel-state.ts "${TMPDIR:-/tmp}/duel-state.fixed2.ts" && perl -0pi -e 's/    held\.handNumber === view\.handNumber &&\n//' src/store/duel-state.ts && ! cmp -s "${TMPDIR:-/tmp}/duel-state.fixed2.ts" src/store/duel-state.ts && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/store/duel-state.test.ts > "${TMPDIR:-/tmp}/state-any-hand.txt" 2>&1; cp "${TMPDIR:-/tmp}/duel-state.fixed2.ts" src/store/duel-state.ts; cmp -s "${TMPDIR:-/tmp}/duel-state.fixed2.ts" src/store/duel-state.ts && grep -qF "FAIL  src/store/duel-state.test.ts > the duel state > the next hand's showdown is a read, however recently the last one was" "${TMPDIR:-/tmp}/state-any-hand.txt" && grep -qF 'Tests  1 failed | 94 passed (95)' "${TMPDIR:-/tmp}/state-any-hand.txt"
  - sh -c 'test -z "$(git status --porcelain web-client/src/e2e web-client/src/store/duel-store.ts web-client/src/store/boot.ts)"'
  - python3 .github/scripts/lint_tickets.py
---

## Goal

A hand's reveal is classified `hold: "read"` on the delivery that **first** turns the rival's hand
face up, and `"step"` on every later delivery of the same hand — so the two-second beat
`TASK-141105` schedules is spent once per hand, not once per frame carrying cards the viewer has
already seen.

## Blocked on `DEC-153`, and it must not be started before that merges

`ADR-0136` §1 says, of the predicate this ticket changes, that **`layOutReveal`'s signature does not
change** and that *"nothing is computed, no event is consulted, and **no previous view is
remembered**"*. This ticket contradicts all three. In this repository an ADR is amended by another
ADR — `docs/adr/README.md`: *"An ADR is never edited to change its decision"* — and no ticket may
carry that amendment, so `DEC-153` is registered for **the architect** and this ticket waits for the
answering ADR.

The question is real rather than ceremonial, because **the wrong answer is not reachable today**
(measured below), which leaves a second honest answer available: leave the predicate alone and
*write the invariant down* instead. If the ADR takes that route, this ticket is `dropped` and
whatever the ADR asks for replaces it. If it takes the guard below, everything in this ticket —
including every measured number in `verify:` — stands as written.

## Why the predicate is wrong as merged

`ADR-0120` §3's rule is *"A hand that ends with a hand face up **the viewer has not been shown
before** holds its last step for 2,000 ms."* The next paragraph restates it *"as the client can
evaluate it"* — *"the hand-completing view carries hole cards for the rival's seat"* — and **that
restatement drops the word `before`**. `TASK-141104` implemented the restatement, so today the
answer is `"read"` on **every** delivery carrying rival hole cards, first or fiftieth.

Three merged facts say the frame cannot answer the question by itself:

- `poker-engine/.../game/PlayerView.kt:99` builds each `SeatView` with
  `showCards = it.index == seat || it.index in revealed`, and `revealed` is
  `revealedSeats(handEvents)` — every seat a `HandRevealed` has fired for **anywhere in the hand's
  log**. Its own KDoc says the log is the only record of what has been shown. Nothing marks a
  projection as the first one.
- `duel-state.ts`'s `Snapshot` case lays out a reveal on **every** `view.street === "COMPLETE"`.
- `duel-store.ts` builds its state once (`let state = initialState()`) and keeps it across sockets;
  `applyServerMessage`'s `RoomJoined` case clears nothing when the code is the one it already holds.

## What was measured, and why this is latent rather than live

**Measured on `develop` at `5cce33c4`** with a throwaway `poker-server` probe (a full `playDuel`
loop over 60 seeds, since reverted): **604** hand-completing snapshots were delivered, **115** of
them carrying the rival's hole cards, and **2,220** `resumeFrames` frames were taken at every step
of every duel. `Snapshot`s at `COMPLETE` in a resume: **0**. Seats receiving one hand's completing
view twice: **0**.

The reason is `DuelAction.kt:59` — `act` calls `advance` **in the same call** when a hand ends, and
`DuelProgress.advance` loops until a hand is not over, so a live `DuelRunner.hand` is never over;
`GameState.isHandOver` *is* `street == Street.COMPLETE`; and `resumeFrames` projects only
`runner.hand.state` or, for a finished duel, `finishedFrames`, which carries no `Snapshot` at all.
`broadcast` (`Addressed.kt:63`) is the only place a `Snapshot` is constructed.

**The probe was falsified rather than trusted**: the same run, asked the same question of the
runner as it would stand if `act` did *not* call `advance`, found **602** hand-completing snapshots
in a resume. The detector works; the path is simply closed.

So the defect is a **latent** one: correct output resting on a sequencing property of a module this
epic forbids opening, stated in no ADR and pinned by no test. `ADR-0102` §5 already writes as though
the path is open — *"A resume `Snapshot` that happens to arrive at `COMPLETE`…"* — and
`duel-state.test.ts`'s merged *a snapshot at COMPLETE with no events before it takes one step, not
four* is a test of exactly that frame.

## Files

| File | Action |
| --- | --- |
| `web-client/src/store/duel-state.ts` | modify |
| `web-client/src/store/duel-state.test.ts` | modify |

Read the ADR that answers `DEC-153` first, then
[`ADR-0120`](../../docs/adr/ADR-0120-a-showdown-shows-the-hands-the-rules-showed-and-the-beat-that-shows-them-stands.md)
§3 and [`ADR-0136`](../../docs/adr/ADR-0136-a-beat-declares-its-own-length-and-zero-silences-every-beat.md)
§§1 and 5. **No other file is opened.** Two files is the whole diff and it was measured: with the
change below in place, `duel-store.test.ts` stays at **14** and `src/routing/room-standing.test.ts`
at **13**, both untouched, and `npm run check` is green across **124** files.

## Scope

- **One helper, above `layOutReveal`'s own KDoc block** — the frame-level half of `ADR-0120` §3,
  lifted out of `TASK-141104`'s inline expression **unchanged** so it can be asked of either view:

  ```ts
  /**
   * Whether [view] carries hole cards for a seat other than the one it was addressed to — the
   * frame-level half of `ADR-0120` §3, asked of whichever view it is given.
   */
  function rivalCardsShown(view: PlayerView): boolean {
    return view.seats.some(
      (seat) => seat.index !== view.viewerSeat && seat.holeCards.length > 0,
    );
  }
  ```

  `.some` and `view.viewerSeat` are kept for `ADR-0136` §1's own two reasons: nothing depends on the
  array's order or on there being two seats, and each view answers for itself.

- **`layOutReveal` takes the view the store already holds**, and nothing else changes about it:

  ```ts
  function layOutReveal(
    view: PlayerView,
    streetDealt: readonly StreetDealt[],
    held: PlayerView | null,
  ): Reveal {
  ```

- **The final step asks whether the cards are *newly* shown.** Reproduce this exactly — both
  `verify:` mutations match it by literal, and `prettier` accepts this wrapping as written:

  ```ts
  const alreadyShown =
    held !== null &&
    held.handNumber === view.handNumber &&
    rivalCardsShown(held);
  steps[streetDealt.length] = {
    board: view.board.cards,
    street: view.street,
    hold: rivalCardsShown(view) && !alreadyShown ? "read" : "step",
  };
  ```

  `held.handNumber === view.handNumber` is what keeps the **next** hand's showdown a read; without
  it one showdown would silence every showdown after it, and the third test below is the one that
  says so. No duel identifier is compared because none exists on the wire, and none is needed: a new
  duel's opening hand cannot be at `COMPLETE` (`DuelStart.kt`'s `check(hand.state.seatToAct != null)`),
  so its own opening `Snapshot` always overwrites `state.view` first.

- **The call site passes `state.view`**, which in the `Snapshot` case is still the **previous** view,
  because the reveal is computed from `state` while `view:` is being set from `message`:

  ```ts
  ? layOutReveal(message.view, state.pendingStreetDealt, state.view)
  ```

- **`TASK-141104`'s comment above the predicate is replaced**, because its first clause — *"evaluated
  over this frame alone: it reads view.viewerSeat rather than any state carried across frames"* — is
  no longer true. The replacement says the `why` in `ADR-0120` §3's own words: the rule is *has not
  been shown before*, and `held` is the whole of what *before* means, because `PlayerView.of`
  populates a revealed seat's `holeCards` on **every** projection of the hand. Keep the second half
  verbatim — the client reads a hole card here to choose a schedule and never a face
  (`ADR-0136` §5).

- **`layOutReveal`'s KDoc gains a clause and loses an ambiguity**: *"which is why no previous view is
  needed"* is true of the **boards** and false of the hold, so it reads *"no previous view is needed
  **for the boards**"*, and one sentence says what `held` is for. Nothing else in the KDoc moves.

- **`advanceReveal` is byte-unchanged** and `applyServerMessage`'s signature does not change. The
  queued replay path is what the second test exercises: `advanceReveal` folds a queued `Snapshot`
  back through `applyServerMessage` with the state whose `view` the first delivery already set, so
  the guard is reached there too — which is exactly why it lives in the reducer and not in
  `duel-store.ts`.

- **Three tests are added** to the existing `describe("the duel state")`, with these names exactly —
  the `verify:` block greps all three:

  1. `a hand-ending view delivered a second time is a step, not a second read`
  2. `a repeat queued behind the beat it repeats is a step when the queue drains`
  3. `the next hand's showdown is a read, however recently the last one was`

## Out of scope

- **Every other file.** `duel-store.ts`, `boot.ts`, every component, `no-derivation.test.tsx` and
  everything under `web-client/src/e2e/` — a `verify:` gate requires `git status --porcelain` to
  report nothing for `src/e2e`, `duel-store.ts` and `boot.ts`. **No merged assertion moves**: this
  change adds a case the existing frames never exercise — every merged frame is delivered once — so
  the three tests are additions, the count goes **92 → 95**, and the reviewer should read the diff
  of `duel-state.test.ts` as three new `it(` blocks and nothing else.
- **The engine and the server.** `EPIC-14`'s *Out of scope* forbids opening `poker-engine`, and
  `ADR-0120` took `poker-server` out of item 2a. The probe that measured the reachability above was
  reverted in full and this ticket adds no Kotlin file — **including** the server test that would
  pin *"a resume never re-projects a hand-completing view"*. If the ADR answering `DEC-153` wants
  that test, it belongs to a server story, not here.
- **A wire field marking a first send.** That is the engine change `ADR-0008` and this epic both
  refuse; `PlayerView` carries no such field and `ADR-0120` moved no wire. Named, deliberately not
  registered.
- **The all-in-and-called reveal, a voluntary show, and any post-hand disclosure of a mucked hand.**
  `STORY-1411`'s *Out of scope*, unchanged by this ticket.
- **Any duration.** No millisecond figure is read, written or asserted here; `TASK-141105` owns the
  schedule and `TASK-141106` the constant. This ticket only decides which **kind** a beat carries.

## Tests

`duel-state.test.ts`. Baseline measured on `develop` at `5cce33c4`: **92**. After: **95**.

| Test | Proves |
| --- | --- |
| `a hand-ending view delivered a second time is a step, not a second read` | The resume shape: one `COMPLETE` view at `handNumber: 4` with both seats' cards, applied → `"read"`; `advanceReveal` drains it to `reveal === null`; **the same view applied again** → `"step"`. Two deliveries, or the test cannot see a repeat at all |
| `a repeat queued behind the beat it repeats is a step when the queue drains` | The same view delivered while the beat still stands is **queued** (`ADR-0102` §1, asserted as `queued` having length 1), and the reveal it lays out when `advanceReveal` replays it is `"step"`. This is the delivery path a guard placed in `duel-store.ts` rather than in the reducer would miss |
| `the next hand's showdown is a read, however recently the last one was` | `handNumber: 4` then `handNumber: 5`, both `COMPLETE`, both carrying the rival's cards, drained between: **both** are `"read"`. Without this, "spend the read once" could be read as "once per duel", and the second mutation below is what makes it bite |

**Both mutations were run against a prototype in this worktree and reverted; the numbers are
measurements, and each gate additionally requires `! cmp -s` to prove the literal it mutates was
actually there** — otherwise a mutation that matches nothing passes vacuously on unfixed code.

| Mutation | Result required |
| --- | --- |
| `rivalCardsShown(view) && !alreadyShown` → `rivalCardsShown(view)` (the merged behaviour restored) | `Tests  2 failed \| 93 passed (95)` — the two repeat tests, and only those. The next-hand test stays green, which is what tells you it is not measuring the guard |
| the `held.handNumber === view.handNumber &&` line deleted | `Tests  1 failed \| 94 passed (95)` — the next-hand test alone. The two repeat tests stay green, which is what tells you the hand comparison is load-bearing on its own |

Falsified rather than assumed: with the source reverted and these three tests kept, the count gate
and **both** mutation gates fail independently (`1`, `2`, `2`).

## Acceptance criteria

- [ ] The ADR answering `DEC-153` is **merged**, and this ticket's shape is what it asks for
- [ ] `layOutReveal` takes a third parameter `held: PlayerView | null`, and `state.view` is what the
      `Snapshot` case passes it
- [ ] `duel-state.test.ts` reports `Tests  95 passed (95)`
- [ ] `npx vitest run src/e2e` reports `Test Files  7 passed (7)` and `Tests  55 passed (55)`
- [ ] With `&& !alreadyShown` removed, exactly `Tests  2 failed | 93 passed (95)`, the failures being
      `a hand-ending view delivered a second time is a step, not a second read` and
      `a repeat queued behind the beat it repeats is a step when the queue drains`
- [ ] With the `held.handNumber === view.handNumber &&` line deleted, exactly
      `Tests  1 failed | 94 passed (95)`, the failure being
      `the next hand's showdown is a read, however recently the last one was`
- [ ] Each mutation gate proves the literal was present (`! cmp -s` after the `perl`), and each
      restore is checked with `cmp -s`
- [ ] `advanceReveal` is byte-unchanged and `applyServerMessage`'s signature does not change
- [ ] `git status --porcelain` is empty for `web-client/src/e2e`, `duel-store.ts` and `boot.ts`
- [ ] `cd web-client && npm run check` exits 0
- [ ] The diff touches exactly the two files in the table above
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
