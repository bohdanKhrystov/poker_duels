---
schema: 2
id: TASK-021413
title: A finished room's resume names the other seat, and the test says so
type: task
status: backlog
parent: STORY-0214
module: poker-server
estimate: XS
tier: haiku
review: standard
labels: [server, rooms, presence, tests]
files_touched: 1
depends_on: [TASK-021412]
verify:
  - ./gradlew :poker-server:test --tests '*RoomResumeTest'
  - ./gradlew :poker-server:ktlintCheck
---

## Goal

`aFinishedRoomResumesAsItsOutcome` checks the `DuelFinished` frame and nothing else, so the
`OpponentPresence` frame a FINISHED resume also carries is unverified. A `seat`/`otherSeat` mixup
confined to that path — `Addressed(seat, presenceOf(seat, now))`, a seat told about itself — passes
every merged test in the repository.

## Why it was not fixed where it was found

`TASK-021410` added presence to the resume and had to repair this test, whose old
`assertEquals(1, outbound.size)` its own change made false. Its coder found this gap, reported it,
and did not close it: the ticket prescribed that assertion's replacement **verbatim**, and widening
it would have been widening scope. Review then confirmed the gap is real and that nothing in
`RoomResumeTest`, `DuelSocketReconnectTest` or `SocketReconnectTest` covers it.

That is the correct sequence — a coder deferring to its ticket, and the finding surviving as a
ticket rather than as a silent extra assertion — and this is its consequence.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/test/kotlin/duels/poker/server/room/RoomResumeTest.kt` | modify — one test extended |

## Scope

- Extend `aFinishedRoomResumesAsItsOutcome` so the `OpponentPresence` frame in a FINISHED room's
  resume is asserted: exactly one, and addressed to the seat that **stayed**, not the returning one.
- That fixture resumes as the **host**, so the expected address is the guest's seat. Write it as a
  literal, so a `presenceOf(seat, …)` mixup fails rather than agreeing with a derived value.
- Assert the total `outbound` size too, so a stray third frame on this path is caught. `TASK-021410`
  dropped the old size assertion because the count moved from one to two; restore it at the count
  that is now correct.

## Out of scope

- Any production change. This ticket asserts behaviour that already exists; if it turns out not to,
  stop and say so rather than fixing it here — that would be a different ticket.
- Every other test in the file, and every other resume path. `TASK-021410`'s new tests already cover
  PRESENT, AWAY and ABSENT on a running room.

## Tests

`RoomResumeTest`

| Test | Proves |
| --- | --- |
| `aFinishedRoomResumesAsItsOutcome` (extended) | a FINISHED room's resume carries exactly one `OpponentPresence`, addressed to the seat that stayed, alongside its one `DuelFinished` — so a seat told about itself on this path fails |

## Acceptance criteria

- [ ] `aFinishedRoomResumesAsItsOutcome` asserts the `OpponentPresence` frame's address as a literal
- [ ] It asserts the total `outbound` size
- [ ] Its existing `DuelFinished` and no-`Snapshot` assertions are unchanged
- [ ] No production file changes
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
