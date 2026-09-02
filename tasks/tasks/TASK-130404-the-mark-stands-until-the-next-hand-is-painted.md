---
schema: 2
id: TASK-130404
title: The mark stands until the next hand is painted, the duel's end retires it, and nothing else touches it
type: task
status: ready
parent: STORY-1304
module: web-client
estimate: S
tier: sonnet
review: standard
files_touched: 2
labels: [client, store, guard]
depends_on: [TASK-130403]
verify:
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npm run --silent check
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/store/duel-state.test.ts 2>&1 | grep -qE '^ *Tests +75 passed \(75\)$'
  - awk 'index($0, "lastAct: null") { n++ } END { exit (n < 2) }' web-client/src/store/duel-state.ts
  - awk 'index($0, "duelState.advanceReveal(") { n++ } END { exit (n < 1) }' web-client/src/store/duel-state.test.ts
  - awk 'index($0, "BettingRoundEnded") { n++ } END { exit (n < 1) }' web-client/src/store/duel-state.test.ts
  - awk 'index($0, "leaves the mark standing") { n++ } END { exit (n < 5) }' web-client/src/store/duel-state.test.ts
  - sh -c 'grep -q "lastAct" web-client/src/store/duel-state.test.ts && ! grep -q "setTimeout" web-client/src/store/duel-state.ts'
  - python3 .github/scripts/lint_tickets.py
---

## Goal

`lastAct`'s lifetime is `ADR-0109` §3 exactly: within a hand it is **only ever replaced**; it stands
through `ADR-0095` §4's award window and goes when the **next hand is painted**, not when the next
hand's frame arrives; `DuelFinished` retires it so no rematch inherits one; and a street's end, a
street's deal, a `Snapshot`, a presence frame and a rejection each leave it exactly where it was.

## Why the painting boundary is the whole ticket

`ADR-0102`, confirmed fact 4: the next hand is dealt in the same server call and its frames are
delivered in the same batch. The recorded script proves the shape — a fold's `Events`, then the
hand-completing `Snapshot`, then **hand 2's own `Events` carrying `HandStarted`**, then hand 2's
`Snapshot`. So a client that cleared on the arriving frame would erase a fold's mark in the same
tick it was made, and `ADR-0095` §4's award banner would stand with no *why* beside it.

Nothing in this ticket schedules anything. `ADR-0102` §1's queue already holds every frame that
arrives while steps remain and folds them back through this same reducer once the last step has
stood — so `TASK-130403`'s `HandStarted` walk fires at the painted moment **by construction**, and
this ticket's job is to assert that rather than to build it. That is why the production change here
is one line and the tests are the ticket.

## What is already true, measured on `develop` 2026-09-02

- `duel-state.test.ts` reports **71** after `TASK-130403` (67 merged + 3).
- A hand-completing `Snapshot` with no `StreetDealt` in front of it lays out **one** step
  (`layOutReveal`), so one `advanceReveal` releases the queue — the fold case, which is the case
  `ADR-0109` §3 works through.
- `DuelFinished` already clears `serverAction` with the comment *"`ADR-0075` §2: a boundary guard"*.
  The same guard, applied to this field, is the one production line this ticket adds.
- No `setTimeout` exists in `duel-state.ts` and none may appear: `ADR-0109` §4 refuses a timer, and
  a gate refuses one here.
- **No merged test in `duel-state.test.ts` drives `advanceReveal` at all** — the name appears once,
  in the exports assertion, and nowhere else. The queue's behaviour is asserted from
  `DuelTable.test.tsx` today. So the first test below is this file's first, and its gate
  (`duelState.advanceReveal(` at least once) is a real gate rather than a coincidence.

## Files

| File | Action |
| --- | --- |
| `web-client/src/store/duel-state.ts` | modify |
| `web-client/src/store/duel-state.test.ts` | modify |
| `docs/adr/ADR-0102-a-hand-ends-in-steps-and-the-client-owns-the-clock.md` | read |
| `docs/adr/ADR-0109-the-table-marks-the-last-act-and-the-next-deal-clears-it.md` | read |

## Scope

- **One production line:** the `DuelFinished` case gains `lastAct: null`, with a comment in the
  neighbouring `serverAction` line's register — a boundary guard, so nothing survives into a
  rematch, since a `Snapshot` clears `outcome` and brings the table back (`ADR-0044` §4).
- **Four tests**, below. No new helper and no new fixture: `samplePlayerView` and
  `duelState.advanceReveal` are already in this file.
- **Nothing else in the reducer moves.** In particular the `Snapshot` case does **not** gain a
  `lastAct` line: leaving a `Snapshot` alone is the behaviour, and a test asserts it.
- **No timer, no schedule, no decay.** `ADR-0109` §4. A gate refuses `setTimeout` in
  `duel-state.ts`.

## Out of scope

- **Rendering.** No component reads `lastAct` until `TASK-130406`–`TASK-130408`.
- **`duel-store.ts`'s tick.** `ADR-0102` §4's clock schedules when facts *appear* and never when
  they are removed; this ticket adds nothing to it and opens the file not at all.
- **A `PlayerView` field for a resuming client.** The fourth test writes the accepted cost down as
  behaviour; it does not repair it (`ADR-0109` §Consequences, and this story's *Out of scope*).
- **Clearing at a street's end.** `ADR-0109` §Alternative 3 refuses it on the delivery; the second
  test is what would fail if someone added it.

## Tests

`duel-state.test.ts` — four added to the 71 it will have, so the file reports **75**.

| Test | Proves |
| --- | --- |
| `stands through the award window and goes only when the next hand is painted` | the whole of `ADR-0109` §3's hardest clause, in one flow. A `PlayerFolded` at `seat: 1` in an `Events` frame; a hand-completing `Snapshot` (`street: "COMPLETE"`) → `reveal` is non-null and **`lastAct` still equals the fold** (the award window); then hand 2's `Events` carrying a `HandStarted` **and** hand 2's `Snapshot` are applied — both are queued, so **`lastAct` still equals the fold** even though the frames have arrived; then one `advanceReveal` → `reveal` is `null` **and `lastAct` is `null`**. The middle assertion is the one a clear-on-arrival implementation fails |
| `a street's end, a deal, a snapshot, a presence and a rejection leave the mark standing` | five labelled assertions, one per frame `ADR-0109` §3 names, applied in sequence over one standing `PlayerBet`: `Events[BettingRoundEnded]`, then `Events[StreetDealt]`, then a mid-hand `Snapshot`, then `OpponentPresence` (`AWAY`), then `Rejected`. Each is `expect(state.lastAct, "<label>").toEqual(bet)` with the label written **verbatim** — `a street's end leaves the mark standing`, `a street's deal leaves the mark standing`, `a snapshot leaves the mark standing`, `a presence frame leaves the mark standing`, `a rejection leaves the mark standing` — so a gate counts five of them and a thinned version cannot ship. Clearing at a street's end (`ADR-0109` §Alternative 3) fails the first two |
| `the duel ending takes the mark off` | `Events[PlayerAllIn]` then `DuelFinished` leaves `lastAct` `null`, so a `Snapshot` that brings the table back for a rematch inherits none. The one production line this ticket adds |
| `a resume rebuilds no mark` | `RoomJoined`, then a `Snapshot` with no `Events` in front of it, then a `YourTurn`, leaves `lastAct` `null` — `ADR-0102` §5's resume shape. This is `ADR-0109` §Consequences' accepted cost stated as behaviour rather than left for a player to discover: **a refresh loses the mark until the next act**, because `PlayerView` carries no last-act field |

Nothing in the 71 is edited: this ticket adds four tests and touches no merged assertion. The
`DuelFinished` cases already in the file read `outcome`, `pendingTurn`, `rejection`, `refusal`,
`rematchOffers` and `serverAction` — measured, none of them reads a whole state object, so the new
field moves none of them.

## Acceptance criteria

- [ ] `duel-state.test.ts` reports `Tests  75 passed (75)`
- [ ] `duel-state.stands through the award window and goes only when the next hand is painted`
      passes
- [ ] `duel-state.a street's end, a deal, a snapshot, a presence and a rejection leave the mark standing`
      passes
- [ ] `duel-state.the duel ending takes the mark off` passes
- [ ] `duel-state.a resume rebuilds no mark` passes
- [ ] `duel-state.ts` carries `lastAct: null` on at least two lines — `initialState` and
      `DuelFinished` — and contains no `setTimeout`
- [ ] `duel-state.test.ts` calls `duelState.advanceReveal(` at least once (it calls it **zero**
      times today, measured), mentions `BettingRoundEnded`, and carries the string
      `leaves the mark standing` at least five times
- [ ] `cd web-client && npm run check` exits 0
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
