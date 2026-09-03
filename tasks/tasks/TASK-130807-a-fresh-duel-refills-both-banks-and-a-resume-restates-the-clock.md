---
schema: 2
id: TASK-130807
title: A fresh duel refills both banks, and a resuming seat is told the live deadline again
type: task
status: ready
parent: STORY-1308
module: poker-server
estimate: S
tier: sonnet
review: standard
files_touched: 5
atomic:
  - TurnClockFramesTest.anOverrunReachesTheNextFramesBank — asserts a bank of 0 because nothing seeded it; withFreshClocks makes the real figure 178000 and the test reddens the moment this ticket lands
  - RoomDuelTest.theOpeningFramesAreTheOnesTheRunnerProduced — a byte-for-byte assertEquals against startDuel's raw output on the join path, which now carries two TurnClock frames
  - './gradlew check -PrequireDocker=true' — holds all five files green together

labels: [server, clock, protocol]
depends_on: [TASK-130806]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.room.RoomResumeTest' --tests 'duels.poker.server.room.RoomRematchTest'
  - python3 -c "import xml.etree.ElementTree as E,sys;n=int(E.parse('poker-server/build/test-results/test/TEST-duels.poker.server.room.RoomResumeTest.xml').getroot().get('tests'));print(n);sys.exit(0 if n==20 else 1)"
  - python3 -c "import xml.etree.ElementTree as E,sys;n=int(E.parse('poker-server/build/test-results/test/TEST-duels.poker.server.room.RoomRematchTest.xml').getroot().get('tests'));print(n);sys.exit(0 if n==12 else 1)"
  - sh -c 'test "$(grep -c "withFreshClocks(" poker-server/src/main/kotlin/duels/poker/server/room/RoomRegistry.kt)" -eq 1'
  - ./gradlew check -PrequireDocker=true
  - python3 .github/scripts/lint_tickets.py
---

## Goal

A join and an agreed rematch start both seats on a full timebank with no deadline, and a client
that comes back mid-duel is handed the live deadline again in the same critical section that
already builds its presence frames (`ADR-0113` §§1, 3; `ADR-0108` §§1, 6).

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/room/RoomRegistry.kt` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/room/RoomResumeTest.kt` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/room/RoomRematchTest.kt` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/room/TurnClockFramesTest.kt` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/room/RoomDuelTest.kt` | modify |

## Two files this ticket's own change forces, and one of them gets *stronger*

`TASK-130807`'s first dispatch stopped here, correctly: `./gradlew check` reddens two tests in files
the table did not name. Both are caused by this ticket's mandated change — seeding the timebank, and
`join`/rematch outbound now carrying `TurnClock` frames — so both are in the blast radius by
`ADR-0070`'s definition.

**`TurnClockFramesTest.anOverrunReachesTheNextFramesBank` — this is a repair, not damage.**
`TASK-130806` shipped it asserting `assertEquals(0L, …bankRemainingMillis[openedSeat])` and its coder
recorded plainly *why* that was weak: nothing seeded `Room.timebankRemainingMillis`, so the value
clamped to zero rather than scaling with the overrun, and it declined to strengthen the test past
what its own scope could reach. `withFreshClocks` is what seeds it. The failure is
`expected: <0> but was: <178000>` — the real, scaled figure. Update the expectation to **178000**.
The neighbouring assertion already pins the rival's bank at the full `timebankMillis`, so after this
change the two seats carry genuinely different figures and the test finally discriminates the thing
its name promises. **Do not weaken it back toward a clamp.**

**`RoomDuelTest.theOpeningFramesAreTheOnesTheRunnerProduced`** is a byte-for-byte `assertEquals`
against `startDuel`'s raw output on the **join** path, which now carries the two `TurnClock` frames.
Update the expectation the same way `TASK-130806` updated its sibling: build the expected frame from
facts independent of the mechanism under test — the hand's own `seatToAct`, `handNumber` and
`decisionPointOf(...).sequence`, plus the named `RoomTimeouts` constants — never by calling
`Room.clocked` or `Room.turnClock`. An expectation derived from the call it checks is `x == x`.

The driver flagged this same assertion during `TASK-130806` and was wrong then: `join` emitted no
clock at that point, and the coder proved it by running the class before and after. It is forced
now, one ticket later, because this ticket is what makes `join` emit one.

**A third correction, to this ticket's own text.** The *Tests* section says
`aFinishedRoomResumesAsItsOutcome`'s `assertEquals(2, resumption.outbound.size)` moves to **3**. It
does not, and the first dispatch was right to leave it at 2: a duel that ends via
`EndCondition.FixedHands(1)` clears `turnDeadline` to null inside the same `clocked()` call that ends
it, because no seat is left to act — so a genuinely `FINISHED` room's resume adds no clock frame.
That matches this ticket's own *Scope* text (*"adds nothing … a `FINISHED` room"*) and contradicts
its *Tests* row. **Scope wins**; the count stays 2, and the separate new test
`aFinishedRoomsResumeStatesNoClock` covers the case directly. Do not change the 2 to a 3 to satisfy
a row that was wrong.

## Scope

- `withFreshRunner` applies `withFreshClocks(timeouts.timebankMillis)` to the room it returns, then
  `clocked(started.runner, now, timeouts.turnMillis)` — so the opening decision of a joined or
  rematched duel is clocked exactly like every later one, and its outbound gains the same two
  `TurnClock` frames `TASK-130806` appends in `act`. *A rematch is a new duel and a fresh bank.*
- `resume` adds **one** `TurnClock` addressed to the returning seat, built from the room's
  `turnDeadline` and the **same `now`** the presence frames beside it already read — and adds
  nothing when the room carries no live decision point (a `FINISHED` room, or a duel between
  hands). The frame goes after `resumeFrames(runner, seat)` and the presence frames.
- The resuming seat is told the clock even when it was never away: a reload has no memory, and
  `ADR-0113` §1's *"every state a client can be in should be reachable from one frame"* is why.

## Out of scope

- `act`'s write-back — `TASK-130806`, merged.
- The sweep — `TASK-130808`, `TASK-130809`.
- Telling the **rival** anything on a resume beyond the presence frames it already sends.

## Tests

`RoomResumeTest` — 17 today, **20** after. `aResumeSendsTheSeatItsOwnPresenceAndTheRoom`'s
`assertEquals(2, resumption!!.outbound.size)` at line 128 **moves to 3**, because the resume now
carries a clock; that is the only existing assertion in the file that changes, nothing else in it
moves, and no assertion is weakened.

| Test | Proves |
| --- | --- |
| `aResumingSeatIsToldTheLiveDeadline` | The resumption's outbound carries exactly one `TurnClock`, addressed to the resuming seat, naming the room's live decision point |
| `aResumingSeatIsToldTheClockEvenWhenItNeverLeft` | A resume with no prior disconnect still carries it |
| `aFinishedRoomsResumeStatesNoClock` | A `FINISHED` room's resume carries no `TurnClock` at all |

`RoomRematchTest` — 10 today, **12** after. `anAgreedRematchCarriesTheOpeningHandsFrames`'s
`assertEquals(expected.outbound, agreed.outbound)` at line 174 is **rewritten** to compare the
frames before the two clock frames and to assert the clock frames separately; it is not deleted and
its opening-hand expectation is not weakened.

| Test | Proves |
| --- | --- |
| `anAgreedRematchRefillsBothBanks` | After a duel in which seat 0 spent 20 s of bank, the rematch's room carries the configured full bank for **both** seats |
| `anAgreedRematchClocksItsOpeningDecision` | The agreed rematch's outbound ends with a `TurnClock` to each seat, `turnRemainingMillis` equal to the configured allowance |

## Acceptance criteria

- [ ] `RoomResumeTest.aResumingSeatIsToldTheLiveDeadline` passes
- [ ] `RoomResumeTest.aResumingSeatIsToldTheClockEvenWhenItNeverLeft` passes
- [ ] `RoomResumeTest.aFinishedRoomsResumeStatesNoClock` passes
- [ ] `RoomRematchTest.anAgreedRematchRefillsBothBanks` passes
- [ ] `RoomRematchTest.anAgreedRematchClocksItsOpeningDecision` passes
- [ ] `RoomResumeTest` reports exactly **20** tests and `RoomRematchTest` exactly **12**
- [ ] `RoomRegistry.kt` contains exactly one `withFreshClocks(` call site
- [ ] `./gradlew check -PrequireDocker=true` is green
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket: `verify` green,
review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into `develop`.
