---
schema: 2
id: TASK-130806
title: Every act write-back restarts the clock and states it to both seats
type: task
status: done
parent: STORY-1308
module: poker-server
estimate: S
tier: sonnet
review: standard
files_touched: 4
atomic:
  - RoomRegistryTest.importsNoTransportOrProtocolTypes — asserts RoomRegistry.kt contains no 'import duels.poker.server.protocol', so the frame is built in Room.kt and the two files move together
  - RoomDuelTest.anActRoutedThroughTheRoomMovesTheDuelAndReturnsTheRunnersFrames — its exact-outbound assertEquals fails the moment the act batch gains frames (the driver first claimed a second one at line 410; the coder ran the class before and after, 14/14 green both times, and that assertion tests registry.join which this ticket never touches)
  - the Kotlin compiler — a new Room projection and its RoomRegistry call site are one step
  - './gradlew check -PrequireDocker=true' — holds all four files green together
labels: [server, clock, protocol]
depends_on: [TASK-130805]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.room.TurnClockFramesTest'
  - python3 -c "import xml.etree.ElementTree as E,sys;n=int(E.parse('poker-server/build/test-results/test/TEST-duels.poker.server.room.TurnClockFramesTest.xml').getroot().get('tests'));print(n);sys.exit(0 if n==8 else 1)"
  - sh -c 'test "$(grep -c "clocked(" poker-server/src/main/kotlin/duels/poker/server/room/RoomRegistry.kt)" -eq 1'
  - ./gradlew check -PrequireDocker=true
  - python3 .github/scripts/lint_tickets.py
---

## Goal

Every `RoomRegistry.act` write-back passes the room through `Room.clocked` and, when the duel came
to rest on a live decision point, appends **one** `ServerMessage.TurnClock` to each seat, last in
the batch — so both players are told the deadline and both banks for the decision that is now open
(`ADR-0113` §§1, 3).

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/room/RoomRegistry.kt` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/room/TurnClockFramesTest.kt` | create |
| `poker-server/src/main/kotlin/duels/poker/server/room/Room.kt` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/room/RoomDuelTest.kt` | modify |

## Where the frame is built, and why that is not a `DEC`

`TASK-130806`'s first dispatch stopped here, correctly. `RoomRegistry.act` cannot construct
`ServerMessage.TurnClock`, because `RoomRegistryTest.importsNoTransportOrProtocolTypes` asserts
`RoomRegistry.kt` contains no `import duels.poker.server.protocol`, and the class's own KDoc says the
registry knows no protocol. The ticket as written required exactly that import.

The coder judged this a decision because three resolutions looked plausible. It is not, because the
repository has already answered it twice, in this very file:

- `Room.kt` imports `ServerMessage` and `Room.presenceOf(seat, now)` returns a
  `ServerMessage.OpponentPresence` — the room projects its own state into a frame.
- `RoomRegistry.disconnect` and `RoomRegistry.resume` call `disconnected.presenceOf(seat, now)` and
  wrap the result in `Addressed(...)`, importing no protocol type to do it.

That is the same problem and the same shape. So `Room` gains a projection for the turn clock,
mirroring `presenceOf`, and `RoomRegistry` calls it — no new pattern, no relaxed invariant, and
nothing `ADR-0113` has to decide. Relaxing `importsNoTransportOrProtocolTypes` would be the decision;
following the merged precedent is not.

What was actually wrong was the **Files table**: it named two files for work that four gates hold
together. `Room.kt` was missing, and so was `RoomDuelTest.kt`, whose exact-outbound `assertEquals`
at line 84 fails the moment the batch grows. Both are gate-forced, so both are in the blast radius
by `ADR-0070`'s definition, and the count is four.

**One correction, recorded rather than quietly fixed.** The driver's first re-scope claimed a
*second* gate-forced assertion in `RoomDuelTest` at line 410. That was wrong: it came from a
`grep` that found an exact-outbound `assertEquals` there and assumed it was affected. The coder
ran the class before and after its change — 14/14 green both times — and established that line
410 tests `registry.join`, which this ticket never touches. The file is in the table on one
gate, not two.

## Scope

- In `act`'s `mutate` block, read `now` **once** from `clock`, and build the written-back room as
  `room.clocked(duelStep.runner, now, timeouts.turnMillis)` before the existing
  `finish` / `copy(runner = …)` branch is applied. The clock is restarted in the same critical
  section that writes the runner back, so no reader can see one without the other.
- When the resulting room carries a `turnDeadline`, append two frames — `Addressed(0, clock)` and
  `Addressed(1, clock)`, the **same** `TurnClock` value — to the end of `duelStep.outbound`, after
  the `Events`, `Snapshot` and `YourTurn` the batch already carries.
- The frame's fields come from the room and the same `now`:
  `turnRemainingMillis = (bankBeginsAt - now).coerceAtLeast(0)`,
  `bankRemainingMillis = listOf(bank[0], bank[1])` with a missing seat reading as the configured
  full bank, `seat`/`handNumber`/`actionSequence` from `turnDeadline`.
- **One clock frame per write-back, never one per intermediate decision.** A batch that plays
  several absent seats through carries a clock for the decision the duel came to rest on and no
  other.
- No clock frame when the write-back leaves no live decision point — a finished duel, a rejected
  frame that closed nothing, a room that answered `null`.

## Out of scope

- **`join`, `offerRematch` and `resume`** — `TASK-130807`.
- **The sweep** — `TASK-130808` and `TASK-130809`. This ticket enforces nothing; the deadline is
  stated and not yet acted on, which is the intermediate `ADR-0113` §9 licenses.
- The client reading the frame — `TASK-130811`.
- `gracePeriods`, `isPaused`, `presenceOf` — `TASK-130810`.

## Tests

`TurnClockFramesTest` — a new file, 8 tests, on `MutableClock`; nothing sleeps.

| Test | Proves |
| --- | --- |
| `bothSeatsAreToldTheClock` | After one act, the step's outbound carries a `TurnClock` addressed to seat 0 and an equal one addressed to seat 1 |
| `theClockIsLastInTheBatch` | The two clock frames are the final two entries, after every `Events`, `Snapshot` and `YourTurn` in the same step |
| `theClockNamesTheDecisionItIsFor` | Its `handNumber` and `actionSequence` equal the `YourTurn` in the same batch, and its `seat` is the seat that `YourTurn` reached |
| `aFreshDecisionIsOwedTheWholeAllowance` | With `turnMillis = 30_000`, `turnRemainingMillis == 30_000` at the instant the frame is built |
| `bothBanksAreStatedEveryFrame` | `bankRemainingMillis.size == 2`, and both entries are the configured bank at the duel's first decision |
| `anOverrunReachesTheNextFramesBank` | Advancing the clock past `bankBeginsAt` before the seat acts leaves that seat's entry reduced by exactly the overrun and the rival's untouched — two seats, so a constant cannot pass |
| `aBatchThatPlaysSeveralDecisionsCarriesOneClock` | With an absent seat played through more than one decision in a single write-back, exactly **two** `TurnClock` frames leave — one per seat, not one per decision |
| `aFinishedDuelIsOwedNoClock` | The act that ends the duel produces a step with **no** `TurnClock` and leaves `turnDeadline` null |

## Acceptance criteria

- [ ] Each of the eight tests above passes, by name
- [ ] `TurnClockFramesTest` reports exactly **8** tests
- [ ] `RoomRegistry.kt` contains exactly **one** `clocked(` call site — the deadline is restarted in
      one place, as `ADR-0113` §3 requires, and never re-derived beside it
- [ ] `./gradlew check -PrequireDocker=true` is green: no merged server test, socket test or e2e
      run is reddened by the extra frame
- [ ] Every command in `verify:` exits 0

> If a merged test outside this table does redden, `ADR-0070` §4 applies only when the fix is
> propagation — an expectation updated, never derived away, and no assertion weakened. Anything
> else is a `DEC`, not a wider ticket.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket: `verify` green,
review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into `develop`.
