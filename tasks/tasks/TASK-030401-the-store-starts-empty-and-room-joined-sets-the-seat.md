---
schema: 2
id: TASK-030401
title: The store starts empty, and RoomJoined sets the seat and room code
type: task
status: ready
parent: STORY-0304
module: web-client
estimate: S
tier: haiku
review: light
files_touched: 2
labels: [client, store]
depends_on: [TASK-030313]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test 2>&1 | grep -qE 'Tests +65 passed \(65\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'starts with nothing the server has not sent'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'RoomJoined sets the seat and the room code'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'leaves state unchanged for a message it has no opinion about'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'exports only the reducer and the initial state'
  - grep -qF 'from "../protocol"' web-client/src/store/duel-state.ts
  - cd web-client && npm run check
---

## Goal

A new `web-client/src/store/` module exports `DuelState`, `initialState()` and
`applyServerMessage()`; `RoomJoined` is the first message folded in, and nothing else is
exported.

## Files

`files_touched` counts the create/modify rows only.

| File | Action |
| --- | --- |
| `web-client/src/store/duel-state.ts` | create |
| `web-client/src/store/duel-state.test.ts` | create |
| `web-client/src/protocol/protocol.gen.ts` | read — exact shape of `RoomJoined`, `Welcome` and `ServerMessage` |

## Scope

- `duel-state.ts` imports every wire type it needs from `"../protocol"` — never from
  `"../protocol/protocol.gen"` or any other file inside that module:

  ```ts
  import type {
    DuelOutcome,
    GameEvent,
    LegalActions,
    PlayerView,
    Rejection,
    ServerMessage,
  } from "../protocol";
  ```

- The full state shape is declared now, even though most of it is still unused — later tickets in
  this story add the case that fills each field, not a new field:

  ```ts
  export interface DuelState {
    readonly mySeat: number | null;
    readonly roomCode: string | null;
    readonly view: PlayerView | null;
    readonly pendingTurn: PendingTurn | null;
    readonly narration: readonly GameEvent[];
    readonly rejection: Rejection | null;
    readonly outcome: DuelOutcome | null;
  }

  export interface PendingTurn {
    readonly handNumber: number;
    readonly actionSequence: number;
    readonly legalActions: LegalActions;
  }

  export function initialState(): DuelState {
    return {
      mySeat: null,
      roomCode: null,
      view: null,
      pendingTurn: null,
      narration: [],
      rejection: null,
      outcome: null,
    };
  }
  ```

- `applyServerMessage` is a pure `switch` over `message.type`, with exactly one case and a
  fallback that changes nothing:

  ```ts
  export function applyServerMessage(
    state: DuelState,
    message: ServerMessage,
  ): DuelState {
    switch (message.type) {
      case "RoomJoined":
        return { ...state, mySeat: message.seat, roomCode: message.code };
      default:
        return state;
    }
  }
  ```

- The `default` branch returns `state` itself, not a copy (`{ ...state }`). A later ticket's test
  relies on that reference being stable for every message this reducer has no case for yet.
- No class, no subscription, no hook. `DEC-022` decides how a screen reads this later; this ticket
  is the pure function only.

## Out of scope

- Every other `ServerMessage` variant — `TASK-030402` through `TASK-030406`.
- Anything that renders, subscribes, or is a React hook or context — `STORY-0305` onward.
- `Failure`. The goal paragraph of `STORY-0304` mentions it, but no acceptance criterion or design
  note gives it a shape, and `UNKNOWN_ROOM` / `ROOM_FULL` are naturally the lobby's concern once
  `STORY-0305` exists to read them. Left unticketed rather than guessed.

## Tests

`web-client/src/store/duel-state.test.ts`, describe block `"the duel state"`. Four `it` blocks, in
this order. The whole file imports the module once, as a namespace, so the fourth test can
reflect on it: `import * as duelState from "./duel-state";` — every test after this one calls
`duelState.initialState()` / `duelState.applyServerMessage(...)`, not a separate named import.

| Test | Proves |
| --- | --- |
| `starts with nothing the server has not sent` | `initialState()` equals an object of all seven fields at their empty value |
| `RoomJoined sets the seat and the room code` | `applyServerMessage(initialState(), {type:"RoomJoined", code:"ABCD", seat:1})` has `mySeat: 1` and `roomCode: "ABCD"` |
| `leaves state unchanged for a message it has no opinion about` | applying a `Welcome` (a message this reducer never special-cases) to a state already carrying a seat returns the exact same object, `toBe` not `toEqual` |
| `exports only the reducer and the initial state` | `Object.keys(duelState).sort()` equals `["applyServerMessage", "initialState"]` |

Four tests. Sixty-one exist, so the suite reports **65**.

## Proof

| Command | Proves |
| --- | --- |
| `Tests 65 passed (65)` | the four tests ran and nothing earlier was displaced |
| the four `--reporter=verbose` greps | each test exists by name |
| `grep 'from "../protocol"' duel-state.ts` | the module respects its one import surface, never reaching into `protocol.gen.ts` directly |
| `npm run check` | typechecks, lints, formats, and re-runs `src/protocol/boundary.test.ts`, which scans every file under `src/` outside `protocol/` — including this new one — for a redeclared wire type name; `DuelState` and `PendingTurn` collide with nothing it reserves |

**Name the edit that makes each assertion red:**

1. Drop `roomCode: message.code` from the `RoomJoined` case → `RoomJoined sets the seat and the
   room code` fails, `expected null to be 'ABCD'` (the field falls back to `initialState()`'s
   `null`, not `undefined`). Revert.
2. Change `default: return state;` to `default: return { ...state };` → `leaves state unchanged
   for a message it has no opinion about` fails: the two objects are `toEqual` but no longer
   `toBe`, so the assertion — written as `toBe` on purpose — catches the needless copy. Revert.
3. Add `export function anything() {}` anywhere in `duel-state.ts` → `exports only the reducer and
   the initial state` fails, naming `anything` in the actual array. Revert.

Quote all three in the PR.

## Acceptance criteria

- [ ] `the duel state > starts with nothing the server has not sent` passes
- [ ] `the duel state > RoomJoined sets the seat and the room code` passes
- [ ] `the duel state > leaves state unchanged for a message it has no opinion about` passes
- [ ] `the duel state > exports only the reducer and the initial state` passes
- [ ] `npm run --silent test` reports `Tests  65 passed (65)`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
