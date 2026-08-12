---
schema: 2
id: TASK-020614
title: Two concurrent creators never receive the same room code
type: task
status: done
parent: STORY-0206
module: poker-server
estimate: S
tier: sonnet
review: standard
files_touched: 1
labels: [server, room, concurrency, test-coverage]
depends_on: [TASK-020609]
verify:
  - ./gradlew :poker-server:test --tests '*RoomRegistryConcurrencyTest'
  - ./gradlew :poker-server:check
---

## Goal

`RoomRegistry.create` inserts with `putIfAbsent` and retries on collision, which is the correct
mechanism — but **every test in `RoomRegistryTest` creates rooms sequentially.** The collision path
is exercised by scripting the code source to repeat itself, which proves the retry logic and proves
nothing about atomicity.

So the registry's central claim — two players creating a room at the same instant never receive the
same code — is currently true by construction and untested. Found during the `TASK-020609` review.

It is worth pinning because the failure is silent and severe: two rooms under one code means one
player joins the wrong duel, and it would appear only under load, which is exactly when nobody is
reading logs.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/test/kotlin/duels/poker/server/room/RoomRegistryConcurrencyTest.kt` | create |

A new file, so `RoomRegistryTest` stays the sequential-behaviour suite and this stays the
concurrency one. Read `RoomRegistry.kt` and `RoomCodeSource.kt`. Modify neither.

## Scope

- Launch many concurrent creators against **one** registry — at least 100, on a multi-threaded
  dispatcher, not `runBlocking`'s single thread. `Dispatchers.Default` with `async` and an
  `awaitAll` is enough. A test that runs coroutines on one thread does not test this.
- Assert every returned code is distinct, and that the registry holds exactly as many rooms as
  creators that succeeded.
- Then the sharp version: **force the race**. Hand the registry a `RoomCodeSource` drawing from a
  deliberately tiny alphabet — say 8 possible codes — and run more creators than there are codes.
  With a small space, collisions are near-certain rather than astronomically unlikely, so the
  atomicity is actually exercised instead of merely being given the opportunity to fail.
  Creators that exhaust the retry limit must fail with the defined exception; the ones that
  succeed must still hold distinct codes. Assert both.

Do not add `@Repeat` or loop the whole test to chase flakiness — the tiny-alphabet trick is what
makes it deterministic enough to be worth running.

## Tests

| Name | Asserts |
| --- | --- |
| `concurrentCreatorsAllReceiveDistinctCodes` | 100+ concurrent creates on a real thread pool, all codes distinct |
| `aTinyCodeSpaceForcesCollisionsAndStillNeverDuplicates` | more creators than possible codes: successes are distinct, the rest fail with the defined exception, and the registry never holds two rooms under one code |

The second test must be shown to fail if `putIfAbsent` is replaced with an unconditional `put`.
State in the PR that this was checked — a concurrency test that cannot fail is worse than none,
because it is trusted.

## Done

Both `verify:` commands exit 0, and replacing `putIfAbsent` with `put` makes
`aTinyCodeSpaceForcesCollisionsAndStillNeverDuplicates` fail.
