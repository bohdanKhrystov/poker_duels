---
schema: 2
id: TASK-030501
title: The store is subscribable, and notifies only when the state moved
type: task
status: done
parent: STORY-0305
module: web-client
estimate: S
tier: haiku
review: standard
files_touched: 2
labels: [client, store]
depends_on: []
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test 2>&1 | grep -qE 'Tests +87 passed \(87\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF "starts at the reducer's initial state"
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'folds an applied message into the state'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'notifies a subscriber when the state changed'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'notifies nobody when the reducer had no opinion'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'hands out the same state reference until a message changes it'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'stops notifying once the unsubscriber has run'
  - cd web-client && npm run check
---

## Goal

`STORY-0304`'s pure reducer gains the framework-free subscribable shell `ADR-0032` §1 specifies:
`createDuelStore()` returning `getState` / `subscribe` / `apply`, with no React in the file.

## Files

`files_touched` counts the create/modify rows only.

| File | Action |
| --- | --- |
| `web-client/src/store/duel-store.ts` | create |
| `web-client/src/store/duel-store.test.ts` | create |
| `web-client/src/store/duel-state.ts` | read — `DuelState`, `initialState`, `applyServerMessage` |
| `docs/adr/ADR-0032-react-subscribes-to-a-store-it-does-not-own.md` | read — §1 only |

## Scope

- Create `web-client/src/store/duel-store.ts` with exactly this content:

  ```ts
  import type { ServerMessage } from "../protocol";
  import { applyServerMessage, initialState, type DuelState } from "./duel-state";

  /** The tab's one duel state, and the subscription a renderer reads it through. */
  export interface DuelStore {
    getState(): DuelState;
    subscribe(listener: () => void): () => void;
    apply(message: ServerMessage): void;
  }

  /** A fresh store at the reducer's initial state, with nobody listening yet. */
  export function createDuelStore(): DuelStore {
    let state = initialState();
    const listeners = new Set<() => void>();

    return {
      getState: () => state,
      subscribe: (listener) => {
        listeners.add(listener);
        return () => {
          listeners.delete(listener);
        };
      },
      apply: (message) => {
        const next = applyServerMessage(state, message);
        // The reducer returns the state it was given for a frame it has no
        // opinion about. Notifying then would re-render every screen for nothing,
        // and would hand useSyncExternalStore a snapshot that never settles.
        if (next === state) return;
        state = next;
        for (const listener of listeners) listener();
      },
    };
  }
  ```

- **`getState` returns the held reference, never a copy.** No spread, no `structuredClone`, no
  `{ ...state }`. `useSyncExternalStore` compares snapshots by identity; a fresh object per call is
  the one classic way that hook is misused, and `ADR-0032` names it as the trap our immutable
  reducer removes by construction.
- **`duel-state.ts` is not edited by this ticket.** Its reducer already returns the identical
  reference for a message it has no opinion about (`Welcome` today), which is what makes the
  `next === state` check meaningful rather than defensive.
- Every member is an arrow property or an arrow-returning property, so `store.subscribe` and
  `store.getState` work when passed unbound — which is exactly how `TASK-030505` will pass them to
  `useSyncExternalStore`. Nothing in this file may read `this`.

## Out of scope

- Anything React: no `useSyncExternalStore`, no hooks, no `.tsx` — `TASK-030505`.
- The connection, `bootDuelClient`, and any send — `TASK-030503`.
- Selectors or per-slice subscriptions. `ADR-0032` accepts whole-state subscription and names the
  selector-taking hook as additive if profiling ever asks.
- A `Failure` case in the reducer — `TASK-030502`. This ticket must pass with the reducer exactly
  as `STORY-0304` left it, which is why the first test compares against `initialState()` rather
  than a field-by-field literal.

## Tests

`web-client/src/store/duel-store.test.ts`, one `describe("the duel store")`. Two fixtures above it:

```ts
const WELCOME = { type: "Welcome", deviceId: "d-1", protocolVersion: 2 } as const;
const ROOM_JOINED = { type: "RoomJoined", code: "ABCDEFGH", seat: 0 } as const;
```

| Test | Proves |
| --- | --- |
| `starts at the reducer's initial state` | `createDuelStore().getState()` deep-equals `initialState()` — compared against the function, never a literal, so a later ticket adding a field does not touch this file |
| `folds an applied message into the state` | after `apply(ROOM_JOINED)`, `getState().roomCode` is `"ABCDEFGH"` and `getState().mySeat` is `0` |
| `notifies a subscriber when the state changed` | a `vi.fn()` listener subscribed before `apply(ROOM_JOINED)` is called exactly once |
| `notifies nobody when the reducer had no opinion` | the same listener is not called at all for `apply(WELCOME)` |
| `hands out the same state reference until a message changes it` | `getState()` is `toBe`-identical across `apply(WELCOME)` and not identical after `apply(ROOM_JOINED)` |
| `stops notifying once the unsubscriber has run` | the function `subscribe` returned, once called, stops the listener being called by a later `apply(ROOM_JOINED)` |

Six tests. Eighty-one exist, so the suite reports **87**.

## Proof

| Command | Proves |
| --- | --- |
| `Tests 87 passed (87)` | the six tests ran and the eighty-one before them still do |
| the six `--reporter=verbose` greps | each exists by name |
| `npm run check` | typechecks, lints, formats |

**Name the edit that makes each assertion red** — all three were run against this exact test file:

1. Delete the `if (next === state) return;` line → `notifies nobody when the reducer had no
   opinion` fails with `expected "spy" to not be called at all, but actually been called 1 times`.
   Revert.
2. Delete the `state = next;` line → `folds an applied message into the state` fails with
   `expected null to be 'ABCDEFGH' // Object.is equality`. Revert.
3. Change `getState: () => state` to `getState: () => ({ ...state })` → `hands out the same state
   reference until a message changes it` fails with `expected { mySeat: null, roomCode: null,
   …(5) } to be { … } // Object.is equality` and `Received: serializes to the same string`.
   Revert.

Quote all three in the PR. The third is the `useSyncExternalStore` misuse `ADR-0032` §1 exists to
prevent, made executable one ticket before React arrives.

## Acceptance criteria

- [ ] `the duel store > starts at the reducer's initial state` passes
- [ ] `the duel store > folds an applied message into the state` passes
- [ ] `the duel store > notifies a subscriber when the state changed` passes
- [ ] `the duel store > notifies nobody when the reducer had no opinion` passes
- [ ] `the duel store > hands out the same state reference until a message changes it` passes
- [ ] `the duel store > stops notifying once the unsubscriber has run` passes
- [ ] `npm run --silent test` reports `Tests  87 passed (87)`
- [ ] `web-client/src/store/duel-state.ts` and `duel-state.test.ts` are byte-identical to `develop`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
