---
schema: 2
id: TASK-020815
title: The configured window decides the instant, on a clock that never sleeps
type: task
status: ready
parent: STORY-0208
module: poker-server
estimate: S
tier: haiku
review: standard
files_touched: 1
labels: [server, rooms, config, test]
depends_on: [TASK-020814]
verify:
  - ./gradlew :poker-server:test --tests '*GraceWindowConfigTest'
  - ./gradlew :poker-server:test --tests '*GraceExpiryTest'
  - grep -rl 'Thread.sleep' poker-server/src/test/kotlin | wc -l | tr -d ' ' | grep -qx 0
---

## Goal

Changing the configured grace window changes when a hand is folded, with no code change — and the
whole story's suite proves it without waiting a single real millisecond.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/test/kotlin/duels/poker/server/room/GraceWindowConfigTest.kt` | create |

No production file changes. Everything this ticket asserts already exists: `TASK-020802` put the
value in `ServerConfig`, `TASK-020801` carried it on `RoomTimeouts`, and `TASK-020812` expires
against it. This ticket is the story's acceptance criteria written as commands that exit 0.

Read `test/room/GraceExpiryTest.kt` for the fixture shape, `config/ServerConfig.kt` for the key and
environment names, and `test/duel/RunnerLeakTest.kt` for how a source-scanning test is kept honest.

## Scope

- One helper the whole file shares: given a `ServerConfig`, build a `RoomRegistry` over a
  `MutableClock` with `config.roomTimeouts()` and `HandSeedSource { 7L }`, seat a host and a guest,
  and disconnect **the player whose seat is on turn** — so that expiry has a hand to fold at the
  instant it fires.
- Every configuration comes through `ServerConfig.from(MapApplicationConfig(...)) { env }`. No test
  in this file constructs a `RoomTimeouts` directly: the point is that the value travelled from
  configuration, and a hand-built `RoomTimeouts` would prove only that the registry can read a
  field.
- No `Thread.sleep`, no `delay`, no real waiting anywhere. Time moves only by `MutableClock.advance`.

## Out of scope

- Any production change. If one turns out to be needed, that is a new ticket, not a widening of
  this one.
- What drives the sweep in production — `DEC-019`.

## Tests

`GraceWindowConfigTest` — a new file.

| Test | Proves |
| --- | --- |
| `theShippedDefaultIsTheDeclaredOne` | `ServerConfig.from(MapApplicationConfig()) { null }.roomTimeouts().disconnectGraceMillis == RoomTimeouts.DEFAULT_DISCONNECT_GRACE_MILLIS` |
| `aFiveSecondWindowFoldsAtFiveSeconds` | with `"duel.disconnectGraceMillis" to "5000"`: a sweep at `4_999` returns an empty list and the hand log holds no fold; a sweep at `5_000` folds the absent seat |
| `aFortyFiveSecondWindowFoldsAtFortyFiveSeconds` | the identical script with `"45000"` does nothing at `5_000` and folds at `45_000`. Together with the case above this is the story's "changing the configured window changes the behaviour with no code change" — either test alone could pass by agreeing with a hard-coded value |
| `theEnvironmentAloneMovesTheWindow` | an empty `MapApplicationConfig` and an `env` returning `"7000"` for `ServerConfig.DISCONNECT_GRACE_MILLIS_ENV` folds at `7_000` — an operator changes this without touching the file, let alone the code |
| `aLongWindowCostsNoRealTime` | the 45-second case again, annotated `@Timeout(5)`. A window measured on wall-clock time instead of `ServerClock` would take 45 real seconds here and fail; on `MutableClock` it is instant. This is the story's "no real-time wait" as an executable assertion rather than a review note |
| `noServerTestWaitsOnRealTime` | walks `File("src/test/kotlin/duels/poker/server")`, asserts the file list is **non-empty** first — the guard `RunnerLeakTest.serverSourceFiles` uses, so a wrong path fails loudly instead of passing vacuously — then asserts no file contains `Thread.sleep` |

## Acceptance criteria

- [ ] All six `GraceWindowConfigTest` cases named above pass
- [ ] `GraceExpiryTest` passes with the file unchanged
- [ ] No file under `poker-server/src/test/kotlin` contains `Thread.sleep`
- [ ] No file outside `poker-server/src/test/kotlin/duels/poker/server/room/GraceWindowConfigTest.kt`
      is modified by this ticket
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
