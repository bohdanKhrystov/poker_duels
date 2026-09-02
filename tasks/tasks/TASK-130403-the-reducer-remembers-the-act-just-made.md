---
schema: 2
id: TASK-130403
title: The reducer remembers the act just made, and the deal that opens a hand takes it off
type: task
status: ready
parent: STORY-1304
module: web-client
estimate: S
tier: sonnet
review: standard
files_touched: 2
labels: [client, store]
depends_on: [TASK-130402]
verify:
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npm run --silent check
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/store/duel-state.test.ts 2>&1 | grep -qE '^ *Tests +71 passed \(71\)$'
  - awk 'index($0, "lastAct") { n++ } END { exit (n < 6) }' web-client/src/store/duel-state.ts
  - awk 'index($0, "ActEvent") { n++ } END { exit (n < 3) }' web-client/src/store/duel-state.ts
  - awk 'index($0, "HandStarted") { n++ } END { exit (n < 1) }' web-client/src/store/duel-state.ts
  - sh -c 'grep -q "lastAct" web-client/src/store/duel-state.ts && ! grep -q "export function isAct" web-client/src/store/duel-state.ts'
  - sh -c 'grep -q "lastAct: null" web-client/src/store/duel-state.test.ts && grep -q "PlayerAllIn" web-client/src/store/duel-state.test.ts'
  - awk 'index($0, "seat: 1") { n++ } END { exit (n < 1) }' web-client/src/store/duel-state.test.ts
  - python3 .github/scripts/lint_tickets.py
---

## Goal

`DuelState` carries `lastAct` — the most recent of the six act events of the hand on screen, the
whole event, or `null`. Any of the six sets it, a later one replaces it, and a `HandStarted`
anywhere in the frame takes it off. That is `ADR-0109` §1 and the first clause of §3, in the one
layer that already remembers things across frames.

## Why the store and not the render layer

`ADR-0109` §Consequences calls the whole mechanism *"two reducer keys and a line of markup"*. The
reducer is where this client already keeps what no single frame carries — `serverAction` holds a
whole `ActedForAbsent` for exactly this reason, and its KDoc says why: keep the server's own event,
so a screen reads the seat and the total off the wire rather than off something a client worked out.
The mark is the same shape of thing, and holding it as state rather than re-deriving it per render
is what makes its **lifetime** testable as reducer behaviour instead of as a scan over a log.

`ADR-0102` §1's step queue then gives *the deal as painted* for free: frames arriving while a
hand's ending is being painted are queued and only ever reach this reducer once the last step has
stood. So a `HandStarted` clears the mark at the moment a player sees the new hand, never at the
moment its frame lands — `TASK-130404` is where that is asserted.

## Two traps in these two files, read them before writing

- **`duel-state.test.ts:43` asserts the whole initial state with `toEqual`.** Adding a field to
  `initialState()` reddens `starts with nothing the server has not sent` unless that literal gains
  `lastAct: null`. It is in this ticket's budget and this ticket owns the change: exactly one line
  added, no assertion removed, no assertion weakened.
- **`duel-state.test.ts:92` asserts `Object.keys(duelState).sort()` is exactly
  `["advanceReveal", "applyServerMessage", "initialState"]`.** A new **exported function** reddens
  it. So `isAct` stays module-private, and `ActEvent` is exported as a **type** only — a type export
  is erased and never reaches `Object.keys`. A gate refuses `export function isAct`.

## What is already true, measured on `develop` 2026-09-02

- `duel-state.test.ts` reports **67** tests.
- `duel-state.ts` imports only from `../protocol` — no production module in `store/` reaches into a
  feature folder, and this ticket keeps it that way: `ActEvent` is built from the six generated
  interfaces, and `table/action-text.ts` imports the type from here in `TASK-130405`, the way
  `table/act-frame.ts` already imports `PendingTurn` from here.
- `seat: 1` appears **0** times in `duel-state.test.ts`, so a gate requiring it is a real gate.
- The `Events` case today is a two-field return; the six act events are `PlayerFolded`,
  `PlayerChecked`, `PlayerCalled`, `PlayerBet`, `PlayerRaised` and `PlayerAllIn`, and the last four
  carry the server's own `to`.

## Files

| File | Action |
| --- | --- |
| `web-client/src/store/duel-state.ts` | modify |
| `web-client/src/store/duel-state.test.ts` | modify |
| `web-client/src/protocol/protocol.gen.ts` | read |
| `docs/adr/ADR-0109-the-table-marks-the-last-act-and-the-next-deal-clears-it.md` | read |

## Scope

- **`export type ActEvent`** in `duel-state.ts`: the union of the six act interfaces, imported as
  types from `../protocol`. KDoc cites `ADR-0109` §1 and says these six and no other event are acts
  — a blind post, a deal, a presence change and a rejection are not.
- **`readonly lastAct: ActEvent | null`** on `DuelState`, with KDoc in `serverAction`'s register:
  the whole event kept rather than picked apart, so the seat and the `to` are the server's own; and
  the accepted cost written down — **a resume rebuilds nothing here, because `PlayerView` carries no
  last-act field (`ADR-0109` §Consequences), and repairing it is a `PROTOCOL_VERSION` bump this
  epic does not spend.**
- **`initialState()` returns `lastAct: null`.**
- **The `Events` case walks its own events in order**, in one pass, keeping the existing `narration`
  and `pendingStreetDealt` lines untouched:
  - a `HandStarted` sets the running value to `null`;
  - any of the six acts sets it to that event;
  - the frame's result is whatever the walk ended on, and the frame is the only thing that decides.
    Not `events.at(-1)`, not a reverse scan: the order inside a frame is the server's and the walk
    must respect it, which is what the third test below exists to pin.
- **A module-private `isAct` type guard**, in `isStreetDealt`'s idiom. Not exported.
- **Nothing else in the reducer moves.** No other case gains or loses a line in this ticket.

## Out of scope

- **`DuelFinished`, the step queue, and the frames that leave the mark standing.**
  `TASK-130404` — this ticket's `Events` walk is the only clearing it adds.
- **Rendering anything.** `action-text.ts`, `SeatPlate.tsx`, `DuelTable.tsx` and `Lobby.tsx` are
  `TASK-130405`–`TASK-130408`. No component reads `lastAct` yet, and that is fine: an unread field
  breaks nothing.
- **Putting the last act on `PlayerView`.** `ADR-0109` §Consequences accepts the refresh cost by
  name; a ticket that wants it repaired is asking for a wire bump and is out of scope.
- **A log, or a second mark.** One field, one act. `ADR-0109` §1 and §Alternative 2.

## Tests

`duel-state.test.ts` — three added to the 67 it has (measured 2026-09-02), so the file reports
**71**, plus the one-line repair to the initial-state literal.

| Test | Proves |
| --- | --- |
| `records the act exactly as the server sent it` | each of the six acts, applied in its own `Events` frame from a fresh `initialState()`, leaves `lastAct` `toEqual` that same event object — seat, sequence and `to` included. Six inputs, so a field the reducer dropped or rewrote fails |
| `a later act replaces an earlier one, at either seat` | `PlayerBet` at `seat: 1` then `PlayerCalled` at `seat: 0` in two frames leaves `lastAct` equal to the call and **not** the bet; then a `PlayerRaised` at `seat: 1` in a third frame leaves the raise. **Two seats and three acts**: a per-seat field would keep both, and a field that only ever recorded one seat would fail the third step |
| `the deal that opens a hand takes the mark off, in the order the frame sent it` | **three inputs, and the third is the point.** `Events[bet]` then `Events[HandStarted, BlindPosted]` → `null`. `Events[bet, HandStarted]` in **one** frame → `null`. `Events[HandStarted, bet]` in one frame → the **bet**. An implementation that clears whenever a frame contains a `HandStarted` passes the first two and fails the third |

The 67 merged tests keep every assertion they have. Exactly one line moves: `lastAct: null` joins
the `toEqual` literal in `starts with nothing the server has not sent`, which is the only place in
the repository that enumerates every field of `DuelState` (measured). No other merged assertion in
this file reads a whole state object.

## Acceptance criteria

- [ ] `duel-state.test.ts` reports `Tests  71 passed (71)`
- [ ] `duel-state.records the act exactly as the server sent it` passes
- [ ] `duel-state.a later act replaces an earlier one, at either seat` passes, and the file contains
      the literal `seat: 1`
- [ ] `duel-state.the deal that opens a hand takes the mark off, in the order the frame sent it`
      passes
- [ ] `duel-state.ts` mentions `lastAct` on at least six lines, `ActEvent` on at least three and
      `HandStarted` on at least two, and exports **no** function named `isAct`
- [ ] `duel-state.test.ts` contains `lastAct: null` and `PlayerAllIn`
- [ ] `cd web-client && npm run check` exits 0 — the whole suite, including the four end-to-end
      files `ADR-0100` §3 forbids editing
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
