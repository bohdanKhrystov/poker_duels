---
schema: 2
id: TASK-130809
title: The sweep expires turn clocks in one pass through act
type: task
status: done
parent: STORY-1308
module: poker-server
estimate: S
tier: sonnet
review: deep
files_touched: 10
atomic:
  - the Kotlin compiler — a renamed public suspend method and a renamed public type redden every call site and every import in one step, and there is no intermediate tree in which both names exist
  - ktlint standard:filename — a file holding one top-level class must be named for it, so renaming GraceExpiry and GraceExpiryTest forces both file renames in the same commit
labels: [server, clock, sweep, atomic]
depends_on: [TASK-130808]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.room.TurnClockExpiryTest'
  - python3 -c "import xml.etree.ElementTree as E,sys;n=int(E.parse('poker-server/build/test-results/test/TEST-duels.poker.server.room.TurnClockExpiryTest.xml').getroot().get('tests'));print(n);sys.exit(0 if n==21 else 1)"
  - sh -c '! grep -rq "expireGracePeriods\|GraceExpiry" poker-server/src'
  - sh -c 'test -f poker-server/src/main/kotlin/duels/poker/server/room/TurnClockExpiry.kt'
  - sh -c 'test ! -e poker-server/src/main/kotlin/duels/poker/server/room/GraceExpiry.kt'
  - sh -c 'test "$(grep -c "expireTurnClocks" poker-server/src/main/kotlin/duels/poker/server/Application.kt)" -eq 1'
  - ./gradlew check -PrequireDocker=true
  - python3 .github/scripts/lint_tickets.py
---

## Goal

`RoomRegistry.expireGracePeriods()` is **replaced** by `expireTurnClocks()` — one pass instead of
two, judged against a `now` read once, going through `act` so a duel that ends on a timeout reaches
the coin by the same single path a played one does (`ADR-0113` §5, `ADR-0108` §3).

## Why this is `atomic:`, and how the count was reached

The count is a **measured reference set**, not a green probe: `grep -rl` over `poker-server/src`
for `expireGracePeriods` and `GraceExpiry` on `develop` at `360bcacf`, minus the one occurrence
that lives in a `DuelSocketDisconnectTest` KDoc that `TASK-130805` deletes, plus the two file
renames ktlint's filename rule forces. **Say so in the PR body.** **The re-probe ran, and the count moved 9 → 10.** Re-derived against `develop` at `82d1f750`,
after `TASK-130801`–`TASK-130808` landed, the reference set for `expireGracePeriods` / `GraceExpiry`
contains one file the planner's `grep` could not have seen:
`poker-server/src/test/kotlin/duels/poker/server/room/TurnClockFramesTest.kt`, whose line 194 calls
`registry.expireGracePeriods()`. **`TASK-130806` created that file two tickets after the split was
written.** The rename breaks it at compile time, so it is in the blast radius by `ADR-0070`'s
definition.

The two entries below that are absent from today's tree — `TurnClockExpiry.kt` and
`TurnClockExpiryTest.kt` — are the files this rename *creates*, and are correctly not in the
reference set.

This is what the promise below is for: a table written against one baseline and dispatched against
another is short by whatever landed in between.

**This count is provisional, and `ADR-0070` §4 is not the way to correct it.** §4 lets a coder add
a path its own green run names, but its closing sentence excludes *"a rename, a refactor"* by name
— and this ticket is a rename. So do **not** add a row under §4 here.

The reason the count is a reading rather than a green probe is structural, not an omission: this
ticket's baseline is the tree left by `TASK-130808`, which does not exist while the story is being
planned, so no probe could have reached green. The **scheduler** re-runs `ADR-0070` §1's loop
against that baseline and replaces this table before the ticket is dispatched, exactly as
`ADR-0070` §5 re-sized `TASK-021301` after it was written. If the loop still names a path this
table lacks, **stop and say so** — that is a finding about the sizing, not a licence to widen it.

## What `TASK-130808` hands you: two both-gone criteria, not one

`TASK-130808` gave `Room.giveUpTurn` its own both-gone branch, returning an abandoned room inside
`TurnGiveUp.room`. `RoomRegistry.expireGracePeriods` still has one too (`RoomRegistry.kt:629-633`,
`bothGone` -> `expired.abandon(now)`). **Today they cannot both fire**, and the review established
why rather than assuming it: the registry reads `it.giveUpTurn(now, handSeeds)?.step` and references
`.room` nowhere in main source, `giveUpTurn` returns `null` for any room pass 1 already abandoned,
and `Room.abandon` is idempotent on an already-`ABANDONED` room. So there is no defect on that diff.

**This ticket is where it stops being inert**, because this ticket consumes `TurnGiveUp.room`. The
hazard is not two calls to `abandon` — it is that the two branches decide *different questions*:

- pass 1 checks `expired.absentSeats == setOf(0, 1)` after a pure `gracePeriods` expiry
- `giveUpTurn` checks the wider absence-plus-latch state it has just computed

Wire the room through and both criteria are live at once, on states neither was written to see
together. Reconcile them deliberately — say which one decides, delete the other, and give the choice
a test that fails if both run. A comment at `RoomRegistry.kt:642-645` names this; do not merely
delete the comment.

## The count is 21, and why it moved

It was **19**, and that exact-count gate actively prevented a test for a real hazard. The first
implementation guarded the sweep's fallback branch on `expired.runner != null` alone; the coder
verified that guard with a temporary test and then **deleted the test** to keep the suite at 19,
saying so plainly in its commit message. Review then reverted the guard to `runner!!` and found
**all 1787 server tests still green** — the guard was genuinely unpinned.

Worse, review found the guard is also too narrow, and built a repro. A **`FINISHED`** room carries a
stale, outcome-bearing runner, so it passes a null-check that was written for the `WAITING` case:

1. a duel finishes; the remaining seat closes its tab
2. `disconnectGraceMillis` (60s) elapses on a later sweep, well inside `finishedMillis` (300s)
3. `expireTurnClocks()` forwards the stale runner into `actOn`
4. `restarted.finish(now)` runs on an already-`FINISHED` room and `Room.finish`'s
   `check(state == RoomState.PLAYING)` throws

The per-room `catch` swallows the throw, but `recording[code] = duelId` is set on the line **before**
it and the `try/finally` that clears it is never reached. The claim leaks permanently: the room
becomes un-reapable, refuses every rematch offer, and logs an error on every subsequent sweep tick.
With the shipped `RoomTimeouts.DEFAULT` this fires whenever a player closes the tab within about four
minutes of their duel ending — ordinary, not an edge case.

So the fallback branch must require the room to be **`PLAYING`**, not merely to have a runner, and
two tests are added rather than one:

| Test | Proves |
| --- | --- |
| `aFinishedRoomsLateDisconnectDoesNotLeakTheRecordingClaim` | a `FINISHED` room whose seat's grace window elapses on a later sweep is left alone: no throw, and the room still reaps and still accepts a rematch offer |
| `aWaitingRoomsDisconnectedHostIsLeftForTheIdleTimeout` | the original `WAITING`/no-runner case, restored — the test the count had forced out |

The first is the review's finding and is not optional. The second is the one the gate deleted; it
costs almost nothing once the first ticket's fixture exists, and leaving it out would repeat exactly
the mistake that hid the defect.

## Files

| File | Action | Why it cannot be fewer |
| --- | --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/room/RoomRegistry.kt` | modify | The rename and the one-pass rewrite are here |
| `poker-server/src/main/kotlin/duels/poker/server/room/GraceExpiry.kt` | delete | ktlint `standard:filename` — the file holds one class and cannot keep this name once the class is renamed |
| `poker-server/src/main/kotlin/duels/poker/server/room/TurnClockExpiry.kt` | create | The same rule from the other side: the renamed class needs the file named for it |
| `poker-server/src/main/kotlin/duels/poker/server/Application.kt` | modify | Kotlin compiler: `sweepPass` calls the renamed method and imports the renamed type |
| `poker-server/src/main/kotlin/duels/poker/server/room/Disconnection.kt` | modify | Its KDoc names `[GraceExpiry]`, a type this commit deletes. No gate fails on a KDoc link — this row is here because `ADR-0069` §2 stops a ticket at a file its table does not name, and a doc reference to a deleted type is exactly the staleness `ADR-0113` §9 warns about |
| `poker-server/src/test/kotlin/duels/poker/server/room/GraceExpiryTest.kt` | delete | ktlint `standard:filename`, and every test in it names the renamed method or type |
| `poker-server/src/test/kotlin/duels/poker/server/room/TurnClockExpiryTest.kt` | create | The renamed suite, carrying every merged assertion forward plus this ticket's own |
| `poker-server/src/test/kotlin/duels/poker/server/room/TurnClockFramesTest.kt` | modify | the Kotlin compiler: line 194 calls `registry.expireGracePeriods()` |
| `poker-server/src/test/kotlin/duels/poker/server/room/GraceWindowConfigTest.kt` | modify | Kotlin compiler: it calls `expireGracePeriods()` seven times and compares against `emptyList<GraceExpiry>()` |
| `poker-server/src/test/kotlin/duels/poker/server/room/RoomResumeTest.kt` | modify | Kotlin compiler: one `registry.expireGracePeriods()` call |

## Scope

- `expireGracePeriods()` → `public suspend fun expireTurnClocks(): List<TurnClockExpiry>`, and
  `GraceExpiry(room, outbound)` → `TurnClockExpiry(room, outbound)` — same shape, same two
  properties.
- **One pass, where there were two.** The unlocked pre-check is
  `room.turnDeadline != null && (now >= room.turnDeadline.expiresAt || room.turnDeadline.seat in room.absentSeats)`,
  re-decided under the room's own lock; a candidate then goes through
  `act(code) { it.giveUpTurn(now, handSeeds) }` and nothing writes a room back directly. The
  first pass's `expireGrace` call, its `expiredSeatByCode` map and the paragraph of KDoc explaining
  why two passes had to isolate every room all go.
- `now` is read **once** at the top and every room is judged against it, so an enforced expiry can
  only ever **trail** the stated deadline and never precede it (`ADR-0108` §6).
- The per-room `try`/`catch` around `act` stays exactly as it is: one room's failure must not cost
  another room its own give-up, and `CancellationException` is still rethrown.
- The presence frame naming a seat that latched `ABSENT` in this pass is still prepended to that
  room's outbound, addressed to the other seat, built from the same `now`.
- `Application.sweepPass` calls `rooms.expireTurnClocks()`; the ticker's shape — delay first,
  expiry then reap, each guarded independently — is untouched, and the log line's wording moves
  with the method.

## Out of scope

- **`gracePeriods`, `awaySeats`, `isPaused`, `Room.expireGrace`, `presenceOf`'s arity and
  `disconnectGraceMillis`** — `TASK-130810`. `Room.expireGrace` stops being *called* here and is
  deleted there.
- `Room.giveUpTurn`'s own behaviour — `TASK-130808`, merged.
- The `TurnClock` frame the give-up's write-back emits — that comes free from `TASK-130806`,
  because the sweep goes through `act`.

## Tests

`TurnClockExpiryTest` — the renamed `GraceExpiryTest`, **15 tests carried forward verbatim** except
for the method and type names, plus **4 added**, for **19**. No carried assertion is weakened, and
none is deleted.

| Test | Proves |
| --- | --- |
| `aSeatWhoseClockRanOutIsPlayed` | With a connected seat on turn past `expiresAt`, one pass gives up its decision and returns one `TurnClockExpiry` |
| `nowIsReadOnceSoTheExpiryNeverPrecedesTheDeadline` | Two rooms whose deadlines straddle the pass's instant: the one at `expiresAt - 1` is untouched and the one at `expiresAt` is played, in the same pass |
| `anActAtTheDeadlineMinusOneMovesTheDuelAndTheNextSweepExpiresNothing` | The race, the player's way: exactly **one** action is recorded at that decision point |
| `aSweepThatWinsMakesTheStaleActComeBackRejected` | The race, the sweep's way: the player's `Act` carrying the offered decision point is `Rejected`, and again exactly **one** action is recorded there |

## Acceptance criteria

- [ ] Each of the four tests above passes, by name
- [ ] `TurnClockExpiryTest` reports exactly **19** tests
- [ ] `expireGracePeriods` and `GraceExpiry` appear nowhere under `poker-server/src`
- [ ] `TurnClockExpiry.kt` exists and `GraceExpiry.kt` does not
- [ ] `Application.kt` names `expireTurnClocks` exactly once
- [ ] `./gradlew check -PrequireDocker=true` is green
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket: `verify` green,
review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into `develop`.
