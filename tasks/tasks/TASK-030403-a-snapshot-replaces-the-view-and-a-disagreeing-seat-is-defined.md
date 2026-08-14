---
schema: 2
id: TASK-030403
title: A Snapshot replaces the view wholesale, and a disagreeing seat is a defined outcome
type: task
status: backlog
parent: STORY-0304
module: web-client
estimate: S
tier: sonnet
review: standard
files_touched: 2
labels: [client, store]
depends_on: [TASK-030402]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test 2>&1 | grep -qE 'Tests +72 passed \(72\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'replaces the view wholesale'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'clears a pending turn set by an earlier YourTurn'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF "keeps mySeat from RoomJoined when a snapshot's viewerSeat disagrees"
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF "keeps an opponent's hole cards empty until the snapshot reveals them"
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF "reflects a seat's hole cards exactly once the snapshot reveals them"
  - grep -qF 'from "../protocol"' web-client/src/store/duel-state.test.ts
  - cd web-client && npm run check
---

## Goal

A `Snapshot` replaces `state.view` wholesale and clears any pending turn; `mySeat` is untouched by
it, so a `PlayerView.viewerSeat` that disagrees with `RoomJoined.seat` has a defined outcome
instead of silently winning.

## Files

`files_touched` counts the create/modify rows only.

| File | Action |
| --- | --- |
| `web-client/src/store/duel-state.ts` | modify |
| `web-client/src/store/duel-state.test.ts` | modify |
| `web-client/src/protocol/protocol.gen.ts` | read — exact shape of `Snapshot`, `PlayerView`, `Board` and `SeatView` |

## Scope

- Add one case to the `switch` in `applyServerMessage`, after `case "YourTurn":`:

  ```ts
  case "Snapshot":
    return { ...state, view: message.view, pendingTurn: null };
  ```

- `view: message.view` is a **wholesale** replacement — the whole `PlayerView` object, not a
  field-by-field merge with the previous one. Nothing here walks `message.view.seats` or maps
  over a seat's `holeCards`; whatever the server sent is what state now holds, empty or revealed.
- `pendingTurn: null` unconditionally. The pending turn is cleared by the next `Snapshot`
  regardless of whether one was set — this is the mechanism `STORY-0304`'s design notes describe
  as *"a stale pending turn is how a client offers a button for a hand that is over."*
- **This case does not touch `mySeat`.** `mySeat` is written by `RoomJoined` only
  (`TASK-030401`) — `PlayerView.viewerSeat` is not read into it here, and it must not be. This is
  the story's third acceptance criterion made concrete: when a `Snapshot`'s `viewerSeat` disagrees
  with the seat `RoomJoined` set, the defined outcome is that `state.mySeat` keeps the
  `RoomJoined` value and `state.view.viewerSeat` reflects the `Snapshot` verbatim — both are held,
  neither corrects the other, and nothing throws. This should never happen given how the server
  addresses a `Snapshot` to the socket that owns a seat; the test below exists so a future
  "reconciliation" is a deliberate diff against a named test, not a silent behaviour change.

## Out of scope

- `Rejected` and `DuelFinished` also clearing a pending turn — `TASK-030404`, `TASK-030406`.
- `Events` leaving `view` untouched — `TASK-030405`. This ticket only proves what a `Snapshot`
  itself does; the guarantee that nothing *else* touches `view` is a separate, later test.
- Reconnect, or what happens across a socket replacing another — `STORY-0310`.

## Tests

`web-client/src/store/duel-state.test.ts`, describe block `"the duel state"`. Five `it` blocks,
appended after `TASK-030402`'s two. **Those six existing tests are not edited.**

Two local, non-exported helpers are added above the `describe` block, since `PlayerView` has
eleven top-level fields and a nested `SeatView` array — every later ticket in this story reuses
them unchanged:

```ts
function sampleSeat(overrides: Partial<SeatView> = {}): SeatView {
  return {
    index: 0,
    stack: 500,
    committedThisStreet: 0,
    committedThisHand: 0,
    hasFolded: false,
    isAllIn: false,
    holeCards: [],
    ...overrides,
  };
}

function samplePlayerView(overrides: Partial<PlayerView> = {}): PlayerView {
  return {
    viewerSeat: 0,
    handNumber: 1,
    buttonSeat: 0,
    street: "PREFLOP",
    board: { cards: [] },
    pot: 30,
    betToMatch: 20,
    minRaiseTo: 40,
    seatToAct: 0,
    smallBlind: 10,
    bigBlind: 20,
    seats: [sampleSeat({ index: 0 }), sampleSeat({ index: 1 })],
    ...overrides,
  };
}
```

`TASK-030402` already added `import type { LegalActions } from "../protocol";` to the test file.
Extend that one statement rather than adding a second — `import type { LegalActions, PlayerView,
SeatView } from "../protocol";` — the same barrel every wire type comes from.

| Test | Proves |
| --- | --- |
| `replaces the view wholesale` | `applyServerMessage(initialState(), {type:"Snapshot", view})` has `state.view` equal to `view` exactly |
| `clears a pending turn set by an earlier YourTurn` | `YourTurn` then `Snapshot` (two real `applyServerMessage` calls) leaves `pendingTurn` `null` |
| `keeps mySeat from RoomJoined when a snapshot's viewerSeat disagrees` | `RoomJoined` with `seat: 0` then `Snapshot` with `view.viewerSeat: 1` leaves `state.mySeat` at `0` while `state.view.viewerSeat` is `1` |
| `keeps an opponent's hole cards empty until the snapshot reveals them` | a `Snapshot` whose seat 1 has `holeCards: []` leaves `state.view.seats[1].holeCards` as `[]` |
| `reflects a seat's hole cards exactly once the snapshot reveals them` | a `Snapshot` whose seat 1 has `holeCards: ["2c", "7h"]` leaves `state.view.seats[1].holeCards` as exactly `["2c", "7h"]` |

Five tests. Sixty-seven exist, so the suite reports **72**.

## Proof

| Command | Proves |
| --- | --- |
| `Tests 72 passed (72)` | the five tests ran and the six before them still do |
| the five `--reporter=verbose` greps | each exists by name |
| `grep 'from "../protocol"' duel-state.test.ts` | the new fixtures import wire types from the one allowed surface |
| `npm run check` | typechecks (the fixtures must satisfy `PlayerView` and `SeatView` exactly), lints, formats |

**Name the edit that makes each assertion red:**

1. Add `mySeat: message.view.viewerSeat` to the case → `keeps mySeat from RoomJoined when a
   snapshot's viewerSeat disagrees` fails, `expected 1 to be +0`. Revert.
2. Drop `pendingTurn: null` from the case → `clears a pending turn set by an earlier YourTurn`
   fails, the object is non-null. Revert.
3. Map `message.view.seats` through a per-seat transform that defaults an empty `holeCards` to a
   two-card placeholder → `keeps an opponent's hole cards empty until the snapshot reveals them`
   fails, finding a non-empty array where `[]` was expected. Revert.

Quote all three in the PR. The third is `STORY-0304`'s design note made executable: *"there is no
'face-down card' placeholder in state, because a placeholder is a rendering decision."*

## Acceptance criteria

- [ ] `the duel state > replaces the view wholesale` passes
- [ ] `the duel state > clears a pending turn set by an earlier YourTurn` passes
- [ ] `the duel state > keeps mySeat from RoomJoined when a snapshot's viewerSeat disagrees` passes
- [ ] `the duel state > keeps an opponent's hole cards empty until the snapshot reveals them` passes
- [ ] `the duel state > reflects a seat's hole cards exactly once the snapshot reveals them` passes
- [ ] `npm run --silent test` reports `Tests  72 passed (72)`
- [ ] The six `it` blocks from `TASK-030401` and `TASK-030402` are unedited, and their assertions
      are byte-identical
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
