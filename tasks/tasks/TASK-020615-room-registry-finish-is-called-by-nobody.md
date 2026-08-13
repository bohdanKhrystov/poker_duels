---
schema: 2
id: TASK-020615
title: RoomRegistry.finish is called by no production code — remove it or say why it stays
type: task
status: done
parent: STORY-0206
module: poker-server
estimate: XS
tier: haiku
review: standard
files_touched: 3
labels: [server, rooms, cleanup]
depends_on: [TASK-020717]
verify:
  - ./gradlew :poker-server:test --tests '*RoomRegistry*'
  - ./gradlew :poker-server:test --tests '*RoomReapTest'
  - ./gradlew :poker-server:check
---

## Goal

`RoomRegistry.finish(code)` is either gone, or it carries a KDoc paragraph saying in so many words
that it is a test-only affordance and why the production path does not use it. Either outcome is
acceptable; leaving it as it is, is not.

## What was found

`TASK-020717` made `RoomRegistry.act` finish a room itself — it applies `Room.finish` inside its own
`mutate` critical section, alongside the `recording` claim, because the claim and the write-back must
not be observably out of step. From that point on, `RoomRegistry.finish` has had **no production
caller at all**. Every call site is a test:

- `RoomRegistryLifecycleTest.kt` — five calls
- `RoomReapTest.kt` — two calls

A public suspending method on a registry that nothing in production calls is a method a later story
will find, call, and thereby take a claim that `act` believes only it can take.

## What this ticket must not do

**Do not assume the answer.** Establish it, then act:

1. Search the whole repository for callers outside `src/test`. If there are none, the method is
   dead.
2. Decide whether the two test files *need* it, or whether each can reach `FINISHED` the way
   production does — by playing a duel to its end through `act`. `RoomReapTest` already builds real
   duels; `RoomRegistryLifecycleTest` uses `finish` as a shortcut to set up a state.

If it is dead **and** the tests can reach `FINISHED` through `act`, delete the method and rewrite the
call sites. If a test genuinely cannot, keep the method and document it as test-only, naming the
test that needs it and the reason production does not.

## Files

Whichever three of these the answer requires — the method's file plus its two test files:

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/room/RoomRegistry.kt` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/room/RoomRegistryLifecycleTest.kt` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/room/RoomReapTest.kt` | modify |

`Room.finish(now)` — the pure method on the value — is **not** in scope and must not be touched:
`RoomRegistry.act` calls it, and it is the thing that actually finishes a room.

## Scope

- Establish, from the code, whether any production caller exists, and say so in the PR description.
- Then either remove `RoomRegistry.finish` and every test call to it, or keep it with a KDoc
  paragraph that names it test-only, names the test that needs it, and says why `act` is the
  production path.
- Whatever the outcome: `RoomRegistry.kt` still has exactly one `withLock` call site, and every
  assertion that survives in the two test files is the assertion it makes today. A test that used
  `finish` to reach `FINISHED` must still assert the same thing about a `FINISHED` room; only how it
  got there may change.

## Out of scope

- `unclaim`, `abandon`, `reap` and `offerRematch`. All four have production callers.
- Any change to `RoomRegistry.act`'s finishing behaviour, the `recording` map, or the sink.

## Tests

No new test file. The evidence is that the existing suites still pass with the same assertions.

| Test | Proves |
| --- | --- |
| every case in `RoomRegistryLifecycleTest` | a room still reaches and behaves as `FINISHED`, however this ticket gets it there |
| every case in `RoomReapTest` | a `FINISHED` room is still reaped on the same rules, and a room mid-recording is still not |

## Acceptance criteria

- [ ] The PR description states whether a production caller exists, with the search that established
      it
- [ ] Either `RoomRegistry.finish` no longer exists, or its KDoc names it test-only and names the
      test that requires it
- [ ] `RoomRegistryLifecycleTest` and `RoomReapTest` keep every assertion they have today; no test
      method is deleted and none is weakened
- [ ] `RoomRegistry.kt` still contains exactly one `withLock`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
