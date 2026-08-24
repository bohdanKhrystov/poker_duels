---
schema: 2
id: TASK-031303
title: The store holds the presence the server stated, and counts the frames
type: task
status: ready
parent: STORY-0313
module: web-client
estimate: S
tier: haiku
review: standard
files_touched: 2
labels: [client, store, presence]
depends_on: [TASK-031302]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test 2>&1 | grep -qE 'Tests +591 passed \(591\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'records the presence and the window the server sent'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'records a window that ran out, with nothing left of it'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'counts two windows that carry the same remaining as two'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'a presence changes nothing a snapshot or a turn established'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'presence persists through snapshot and turn'
  - cd web-client && npm run check
---

## Goal

`OpponentPresence` stops falling through the reducer's `default`: the store holds what the server
said about the rival, how much of the window was left when it said it, and how many times it has
said anything.

## Files

| File | Action |
| --- | --- |
| `web-client/src/store/duel-state.ts` | modify — three fields, one reducer case |
| `web-client/src/store/duel-state.test.ts` | modify — four tests added, one line changed |
| `web-client/src/protocol/protocol.gen.ts` | read — `OpponentPresence` is `{ type, presence, graceRemainingMillis }`, already generated |

## Scope

- `DuelState` gains three fields, after `rematchOffers`:

  ```ts
  /**
   * The rival's presence, as the last `OpponentPresence` stated it, or `null` before the
   * server has stated one. Presence is state, not an event (`ADR-0028` §2): nothing but
   * another `OpponentPresence` moves it, and a `Snapshot` in particular does not — the duel
   * goes on being played while a seat is absent.
   */
  readonly rivalPresence: SeatPresence | null;
  /**
   * How much of the grace window was left at the instant the server built the frame, or
   * `null` whenever the presence is not `AWAY`. A duration, never a deadline: the two sides
   * share no epoch (`ADR-0028` §2). The reducer never reads it back and never counts it down.
   */
  readonly graceRemainingMillis: number | null;
  /**
   * How many `OpponentPresence` frames the server has sent. Client bookkeeping in the class of
   * `rejectionCount`, and the same job: something that always changes. Two grace windows in one
   * duel carry the same `graceRemainingMillis`, so the value cannot tell a second window from
   * a re-render — only a count can.
   */
  readonly presenceCount: number;
  ```

- `initialState()` gains `rivalPresence: null`, `graceRemainingMillis: null`, `presenceCount: 0`.
- One new case, beside the others:

  ```ts
  case "OpponentPresence":
    return {
      ...state,
      rivalPresence: message.presence,
      graceRemainingMillis: message.graceRemainingMillis,
      presenceCount: state.presenceCount + 1,
    };
  ```

- Both values come off the message and nowhere else. The reducer computes no presence, no deadline
  and no expiry, and reads no clock — `duel-state.ts` stays pure and framework-free.
- `SeatPresence` is imported as a type from `../protocol`, beside the others.

## This ticket owns the assertion its change unsettles

`duel-state.test.ts`'s `starts with nothing the server has not sent` asserts the whole initial state
with `toEqual` against an object literal, so a new field fails it. This ticket adds **exactly three
lines** to that literal, after `rematchOffers: []`:

```ts
      rivalPresence: null,
      graceRemainingMillis: null,
      presenceCount: 0,
```

Every other key keeps its value, the test keeps its name, and no assertion is weakened: the object
is still compared whole with `toEqual`, so a reducer that seeded any of the three with anything at
all still fails it. `TASK-031304` adds a fourth line to the same literal for the same reason.

## Out of scope

- Telling a return from a status quo. `rivalReturned` is `TASK-031304`, and until it lands nothing
  distinguishes the two kinds of `PRESENT`.
- Anything about `ActedForAbsent`. That frame still falls through `default`; `TASK-031314` folds it.
- Rendering. No component changes.
- Clearing `rivalPresence`. Presence is state and only a presence frame moves it — a `Snapshot` in
  particular must not, because hands go on being dealt while a seat is `ABSENT`.

## Tests

`web-client/src/store/duel-state.test.ts`, describe block `"the duel state"`. Four added, one
modified.

| Test | Proves |
| --- | --- |
| `records the presence and the window the server sent` | `OpponentPresence(AWAY, 47000)` on a fresh state leaves `rivalPresence` `"AWAY"` and `graceRemainingMillis` `47000`. **47000 on purpose**: it is not the server's 60 000 default, so a reducer that stored a constant window fails here |
| `records a window that ran out, with nothing left of it` | `OpponentPresence(ABSENT, null)` leaves `rivalPresence` `"ABSENT"` and `graceRemainingMillis` `null` — and applying it **after** the `AWAY` frame above replaces the `47000` rather than keeping it, so a reducer that only ever wrote a non-null remaining fails |
| `counts two windows that carry the same remaining as two` | two `OpponentPresence(AWAY, 47000)` frames, byte-identical, leave `presenceCount` `2`. This is the whole reason the field exists: the two frames differ in nothing else, so a counter derived from the payload cannot see the second one |
| `a presence changes nothing a snapshot or a turn established` | after a `Snapshot` and a `YourTurn`, an `OpponentPresence(AWAY, 47000)` leaves `view`, `pendingTurn`, `narration`, `rejection`, `rejectionCount`, `outcome`, `refusal`, `mySeat`, `roomCode` and `rematchOffers` all identical to what they were |

Five tests. Five hundred and eighty-six exist after `TASK-031302`, so the suite reports **591**.

## Proof

| Command | Proves |
| --- | --- |
| `Tests 591 passed (591)` | five ran, the modified one still runs, and nothing else moved |
| the four `--reporter=verbose` greps | each exists by name |
| `npm run check` | `graceRemainingMillis` typechecks as `number \| null` and `rivalPresence` as `SeatPresence \| null` |

**Name the edit that makes each assertion red:**

1. Write `graceRemainingMillis: 60000` instead of `message.graceRemainingMillis` → `records the
   presence and the window the server sent` fails with `60000` against `47000`. Revert.
2. Write `presenceCount: state.presenceCount` — drop the `+ 1` → `counts two windows that carry the
   same remaining as two` fails with `0` against `2`. Revert.

Quote both in the PR.

## Acceptance criteria

- [ ] `the duel state > records the presence and the window the server sent` passes
- [ ] `the duel state > records a window that ran out, with nothing left of it` passes
- [ ] `the duel state > counts two windows that carry the same remaining as two` passes
- [ ] `the duel state > a presence changes nothing a snapshot or a turn established` passes
- [ ] `the duel state > starts with nothing the server has not sent` passes, and the only lines of it
      that differ from `develop` are the three added above
- [ ] `duel-state.ts` imports nothing from `react` and reads no clock — no `Date`, no `performance`
- [ ] No other test in `duel-state.test.ts` differs from `develop`
- [ ] `npm run --silent test` reports `Tests  591 passed (591)`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
