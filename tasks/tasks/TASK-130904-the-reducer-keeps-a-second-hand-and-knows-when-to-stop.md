---
schema: 2
id: TASK-130904
title: The reducer keeps a second hand, and stops it once the clock can no longer move
type: task
status: backlog
parent: STORY-1309
module: web-client
estimate: S
tier: sonnet
review: standard
files_touched: 2
labels: [client, store, clock]
depends_on: [TASK-130903]
verify:
  - cd web-client && npm ci && FORCE_COLOR=0 NO_COLOR=1 npm run --silent check
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/store/duel-state.test.ts 2>&1 | awk '/^ *Tests +[0-9]+ passed \([0-9]+\)$/ { n = $2 } END { exit !(n >= 88) }'
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/store/duel-state.test.ts -t "lets one tick land at zero before it stops" 2>&1 | awk '/^ *Tests +[0-9]+ passed/ { n = $2 } END { exit !(n >= 1) }'
  - sh -c '! grep -qF "Date.now" web-client/src/store/duel-state.ts'
  - sh -c 'grep -qF "export function tickClock" web-client/src/store/duel-state.ts'
  - python3 .github/scripts/lint_tickets.py
---

## Goal

`DuelState` carries the reading the countdown is drawn against, and one exported function moves it
— returning the state it was handed, unchanged and by reference, whenever moving it could change
nothing a player reads.

## Why the reading lives in the state, and what stops it

**`useSyncExternalStore` only redraws when the snapshot's reference moves** (`ADR-0032` §1: *"the
reference moves only when a message changed something"*). A countdown that changes once a second
therefore needs the store's own state to move once a second — which is exactly what `ADR-0102` §1
already established for the runout, where `advanceReveal` moves the reference on a timer and no
message at all. This is that mechanism, second instance: a reducer function the store calls, never
a timer inside a component.

**Nothing is decremented** (`ADR-0113` §6): the field is a *reading*, and every figure is recomputed
from the anchored deadlines against it, so *"a throttled background tab, a slow frame or a missed
interval costs an update, never accuracy"*.

**The stop condition is the whole subtlety, and it is one clause.** A store that ticks forever
re-renders every screen once a second for the life of the tab — including the result screen, since
nothing clears `turnClock` when a duel finishes. So `tickClock` returns the **identical** state when
there is no clock, and when *the reading it already holds* has passed `expiresAt`. Comparing the
**held** reading rather than the incoming one is what lets the last tick land: the update that first
crosses `expiresAt` still happens — that is the one that puts `0` on screen — and the next one does
not.

## Files

| File | Action |
| --- | --- |
| `web-client/src/store/duel-state.ts` | modify |
| `web-client/src/store/duel-state.test.ts` | modify |

## Scope

- **`DuelState` gains `readonly nowMillis: number`**, `0` in `initialState()`, KDoc'd as the reading
  the clock is drawn against — on the same monotonic clock `turnClock`'s two instants were anchored
  from, and meaningless beside any other.
- **`applyServerMessage`'s `TurnClock` case also sets `nowMillis: arrivedAt`**, the reading it
  already takes. **No other case touches it.** A frame the reducer has no opinion about must keep
  returning the identical state, or `duel-store.ts`'s `next === state` check stops working and every
  screen re-renders on every frame.
- **One new export:**

  ```ts
  export function tickClock(state: DuelState, nowMillis: number): DuelState
  ```

  Returns `state` itself — same reference — when `state.turnClock === null`, or when
  `state.nowMillis >= state.turnClock.expiresAt`. Otherwise returns `{ ...state, nowMillis }`.
  Nothing else in the state moves.
- **`tickClock` reads no clock.** The reading is its argument, as `arrivedAt` already is: *"timer
  tests state time instead of sleeping"* (`ADR-0113` §6, inheriting `ADR-0013`'s rule).
- **A `RoomJoined` naming a different room already clears `turnClock`** (`TASK-130811`); reset
  `nowMillis` to `0` in that same branch so the two never disagree about which room they belong to.
- Comment *why* the stop compares the held reading, not the incoming one. That is the line a later
  reader will otherwise simplify into a bug.

## Out of scope

- **Arming anything.** No `setTimeout`, no `setInterval`, no `schedule` — `TASK-130905` arms this
  from the store, at `ADR-0102` §4's existing seam.
- **Drawing.** No component reads `nowMillis` until `TASK-130907`.
- **Clearing `turnClock` when a duel finishes.** Nothing merged says a `DuelFinished` clears room
  facts (`view` and `roomCode` both outlive the duel), and this ticket does not start. The stop
  clause above is what keeps a finished duel from ticking, and it needs no new clearing rule.
- **The anchor.** `turnEndsAt` and `expiresAt` are merged and are not recomputed here.

## Tests

`duel-state.test.ts` — **6** added to the 82 it has, so the file reports **88**. No merged test is
edited or removed: `nowMillis` is a new field and `tickClock` a new export, and no existing
assertion observes either.

| Test | Proves |
| --- | --- |
| `starts with no reading of its own` | `initialState().nowMillis` is `0` |
| `stamps the reading a TurnClock arrived at` | the same frame at `arrivedAt` 1 000 and at 5 500 leaves `nowMillis` 1 000 and 5 500 — two inputs, because one cannot tell a reading from a constant |
| `moves the reading forward while a clock is live` | `tickClock(state, 7 000)` then `tickClock(…, 9 000)` leaves `nowMillis` 7 000 then 9 000 |
| `hands back the same state when no clock is live` | `tickClock(initialState(), 7 000)` is `toBe` the state it was given — reference identity, not equality |
| `hands back the same state once the reading is already past the expiry` | after a tick that lands beyond `expiresAt`, a further `tickClock` is `toBe` the state it was given |
| `lets one tick land at zero before it stops` | from a reading inside the allowance, a tick to `expiresAt + 2 000` **does** move the state; the next tick, to `expiresAt + 3 000`, is `toBe` the first's result. Both halves asserted, because a stop that fires one tick early never shows the player a zero |

## Acceptance criteria

- [ ] `duel-state.test.ts` reports at least **88** passing tests and none failing
- [ ] `lets one tick land at zero before it stops` passes when run alone by name
- [ ] Each of the other five tests above passes, by name
- [ ] `duel-state.ts` exports `tickClock` and contains no `Date.now`
- [ ] `cd web-client && npm run check` exits 0
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
