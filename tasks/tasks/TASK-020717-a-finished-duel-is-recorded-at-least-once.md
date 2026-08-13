---
schema: 2
id: TASK-020717
title: A finished duel is recorded at least once, not at most once
type: task
status: backlog
parent: STORY-0207
module: poker-server
estimate: S
tier: sonnet
review: deep
files_touched: 3
labels: [server, durability, correctness]
depends_on: [TASK-020714]
verify:
  - ./gradlew :poker-server:test --tests '*RoomDuelTest'
  - ./gradlew :poker-server:test --tests '*RoomRegistry*'
  - ./gradlew :poker-server:check
---

## Goal

`RoomRegistry.act` moves the room to `FINISHED` **inside** the lock, then calls
`DuelResultSink.record` **outside** it. Both halves are individually right — `ADR-0016` forbids I/O
under the lock, and the state flip is what makes recording exactly-once.

Together they lose data. If `record` throws — a brief database outage, an exhausted pool, a dropped
connection — the room is already `FINISHED`, so `Room.act` returns `null` for any later frame, the
sink is never reached again, and **that duel's result and both coin awards are gone permanently**.
The caller sees an exception and has nowhere to put it.

So the guarantee today is *at most once*. It needs to be *at least once*.

This is the loss `ADR-0011` brought PostgreSQL into v0.1 to prevent. `ADR-0011` does say in-flight
duel state need not survive a restart — a *finished* duel's result is not that.

Found in the `TASK-020714` deep review.

## Why the existing idempotency does not already cover this

`TASK-021009` made `PostgresDuelResultStore` idempotent on duel id precisely so a retry is safe, and
its KDoc says callers may retry without side effects. That safety net is never reached, for two
independent reasons:

1. The `FINISHED` guard forecloses any retry path at all.
2. `RandomDuelIdSource` mints "a fresh random `UUID` per call", so even if a retry did happen it
   would carry a **new** duel id and insert a second row rather than being absorbed.

Both have to change for a retry to mean anything.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/room/Room.kt` | modify |
| `poker-server/src/main/kotlin/duels/poker/server/room/RoomRegistry.kt` | modify |
| `poker-server/src/main/kotlin/duels/poker/server/duel/DuelResultSink.kt` | modify |
| `poker-server/src/main/kotlin/duels/poker/server/db/PostgresDuelResultSink.kt` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/room/RoomDuelTest.kt` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/duel/DuelResultSinkTest.kt` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/db/PostgresDuelResultSinkTest.kt` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/room/RoomReapTest.kt` | create |

> **Files table corrected during implementation.** The original table required "a retry carries
> the same duel id" while forbidding `DuelResultSink.kt` and `PostgresDuelResultSink.kt` — the two
> files that mint and carry that id. Its own Done criterion was unreachable at `files_touched: 3`.
> The prohibition on those two files (plus their test counterparts) was lifted after the deep
> review that found this; the prohibition on `PostgresDuelResultStore.kt` — the transaction,
> rollback and `ON CONFLICT` idempotency — stands unchanged and untouched. Separately, the deep
> review overruled this ticket's "record before you finish" ordering instruction: the
> implementation claims the room as `FINISHED` before recording and gives the claim back
> (`unclaim`) only if recording fails, because once a runner carries an outcome every later frame
> yields the same terminal `DuelStep`, so the room's state gate is the only reliable claim, and
> recording strictly before the flip would let two concurrent finishing frames both call `record`.
> The reviewer judged that argument correct on its merits and kept the code as implemented. A
> second deep review then found that `reap`/`isReapable` never consulted `recording`: a `record`
> call that outlasts `timeouts.finishedMillis` let the reaper collect the still-`FINISHED` room out
> from under it, so the later `unclaim` landed on a room `rooms` no longer held and silently did
> nothing — the same "at most once" loss this ticket exists to close, reintroduced through the
> reaper. `RoomReapTest.kt` was added as an 8th file to cover it.

## Scope

**Give the duel a stable id, minted once when it starts.** Put it on the `Room` when the duel
begins, and pass it to the sink rather than letting the sink invent one per call. A retry then
carries the same id, and the store's `ON CONFLICT (id) DO NOTHING` absorbs the duplicate — which is
what turns "retry" from "double award" into "no-op".

**Record before you finish.** Reorder so the result is recorded *before* the room is written back as
`FINISHED`:

- the sink call still happens outside the lock — that constraint does not move;
- if `record` throws, the room stays in a state from which the finishing frame can be replayed, so
  the duel is recorded on the next attempt;
- if `record` succeeds and the write-back then fails, the retry re-records under the same id and is
  absorbed. That is the trade this ticket makes deliberately: **a duplicate attempt is harmless, a
  lost result is not.**

Say in a comment why the order is what it is. "Record then finish" reads as arbitrary; "a duplicate
is absorbed by the duel id, a loss is unrecoverable" reads as a reason.

Keep `RoomRegistry` to its existing two `withLock` call sites.

## Tests

| Name | Asserts |
| --- | --- |
| `afailingSinkLeavesTheDuelRecordableAgain` | a sink that throws once, then succeeds: the duel is ultimately recorded, with the same duel id both times |
| `aretriedFinishRecordsUnderTheSameDuelId` | the id the sink receives on the retry equals the id it received first — the property that makes the store's idempotency engage |
| `concurrentFinishingFramesRecordExactlyOnce` | two callers racing the *finishing* frame on real threads produce exactly one `record` — the existing tests race a mid-hand action and retry the finish only sequentially, so this case is currently unverified |

The third closes a gap the review found independently: exactly-once under a genuine concurrent
finish is structurally guaranteed by the mutex but untested.

## Done

All three `verify:` commands exit 0; a sink failure no longer loses a duel; a retry carries the same
duel id; and concurrent finishing frames record once.
