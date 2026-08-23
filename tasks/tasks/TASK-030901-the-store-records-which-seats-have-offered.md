---
schema: 2
id: TASK-030901
title: The store records which seats have offered a rematch
type: task
status: done
parent: STORY-0309
module: web-client
estimate: S
tier: haiku
review: standard
files_touched: 2
labels: [client, store, rooms]
depends_on: []
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test 2>&1 | grep -qE 'Tests +536 passed \(536\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'records the seat a rematch offer named'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'records an offer from each seat'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'records a repeated offer once, and returns the state it was given'
  - cd web-client && npm run check
---

## Goal

`RematchOffered` stops falling through the reducer's `default`: the store accumulates the seats
whose offers stand, and a repeat of one it already holds changes nothing.

## Files

| File | Action |
| --- | --- |
| `web-client/src/store/duel-state.ts` | modify |
| `web-client/src/store/duel-state.test.ts` | modify |
| `web-client/src/protocol/protocol.gen.ts` | read — `RematchOffered` is `{ type, seat }`, already generated |

## Scope

- `DuelState` gains one field, after `refusal`:

  ```ts
  /**
   * The seats whose rematch offers stand, in the order the server stated them.
   * Client bookkeeping the store accumulates across frames, in the same class as
   * `rejectionCount` — no single frame carries it (`ADR-0044`, Consequences).
   */
  readonly rematchOffers: readonly number[];
  ```

- `initialState()` gains `rematchOffers: []`.
- One new case, beside the others:

  ```ts
  case "RematchOffered":
    // ADR-0044 §3: a repeat offer is answered with the same frame, not an error. Returning
    // the state unchanged is what keeps the store from notifying anybody about nothing.
    if (state.rematchOffers.includes(message.seat)) return state;
    return { ...state, rematchOffers: [...state.rematchOffers, message.seat] };
  ```

- The seat comes off `message.seat` and nowhere else. Never `state.mySeat`, never a literal.

## This ticket owns the assertion its change unsettles

`duel-state.test.ts`'s `starts with nothing the server has not sent` asserts the whole initial
state with `toEqual` against an object literal, so a new field fails it. **Measured**: with the
field added and nothing else changed, that is the *only* failing test in the whole client suite
(75 files, 533 tests → 1 failed, 532 passed).

The edit is exactly one line — `rematchOffers: []` added to that literal, after `refusal: null`.
Every other key keeps its value, the test keeps its name, and no assertion is weakened: the object
is still compared whole, so a reducer that seeded the field with anything at all still fails it.

## Out of scope

- Clearing the offers. `DuelFinished` is `TASK-030902` and `Snapshot` is `TASK-030903`; this ticket
  only ever adds.
- Anything about *whose* offer it is. `mySeat` is not read here — `TASK-030905` owns the comparison.
- Rendering. No component changes.
- `Failure(REMATCH_UNAVAILABLE)` — `TASK-030904`.

## Tests

`web-client/src/store/duel-state.test.ts`, describe block `"the duel state"`. Three added, one
modified.

| Test | Proves |
| --- | --- |
| `records the seat a rematch offer named` | `RematchOffered` with `seat: 1` on a fresh state leaves `rematchOffers` equal to `[1]`. **Seat 1 on purpose**: a reducer that pushed a literal `0`, or pushed `mySeat`, reads the same frame wrongly and fails here |
| `records an offer from each seat` | `RematchOffered(0)` then `RematchOffered(1)` leaves `[0, 1]` — arrival order, both seats, neither dropped |
| `records a repeated offer once, and returns the state it was given` | `RematchOffered(0)` twice leaves `[0]`, **and** the second call's return value is the *same object* (`toBe`) as the first call's — so `duel-store.ts`'s `next === state` guard notifies nobody |

## Proof

| Command | Proves |
| --- | --- |
| `Tests 536 passed (536)` | three added to 533, the modified one still runs, nothing else moved |
| the three `--reporter=verbose` greps | all three names exist |
| `npm run check` | `rematchOffers` typechecks as `readonly number[]` at every read |

**Name the edit that makes each assertion red:**

1. Push a literal `0` instead of `message.seat` → `records the seat a rematch offer named` fails
   with `[0]` against `[1]`. Revert.
2. Delete the `includes` guard → `records a repeated offer once, and returns the state it was
   given` fails on both the `toEqual` and the `toBe`. Revert.

Quote both in the PR.

## Acceptance criteria

- [ ] `the duel state > records the seat a rematch offer named` passes
- [ ] `the duel state > records an offer from each seat` passes
- [ ] `the duel state > records a repeated offer once, and returns the state it was given` passes
- [ ] `the duel state > starts with nothing the server has not sent` passes, and the only line of it
      that differs is the added `rematchOffers: []`
- [ ] `duel-state.ts` contains no literal seat number and no read of `state.mySeat` in the new case
- [ ] No other test in `duel-state.test.ts` differs from `develop`
- [ ] `npm run --silent test` reports `Tests  536 passed (536)`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
