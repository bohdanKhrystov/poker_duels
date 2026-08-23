---
schema: 2
id: TASK-021413
title: A finished room's resume names the other seat, and the test says so
type: task
status: ready
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
`OpponentPresence` frame a FINISHED resume also carries is unverified — its existence, its address
and the total frame count are all unasserted on this path.

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

- Extend `aFinishedRoomResumesAsItsOutcome` so the `OpponentPresence` frame a FINISHED resume
  carries is asserted at all: exactly one, addressed to the **returning** seat, carrying `PRESENT`.
- That fixture resumes as the host, so the expected address is the literal `0`. Write it as a
  literal.
- Assert the total `outbound` size, so a stray third frame on this path is caught. `TASK-021410`
  dropped the old size assertion because the count moved from one to two; restore it at two.

**Corrected before dispatch, and the correction is the interesting part.** This ticket first asked
for the frame to be addressed to the seat that *stayed*. That is backwards: `RoomRegistry.resume`
always sends the returning seat a frame **about its opponent** — `Addressed(seat,
presenceOf(otherSeat, now))` — and only tells the seat that stayed when the returner `wasAway`. The
review that found this gap hypothesised `Addressed(seat, presenceOf(seat, now))`, a wrong *value*
rather than a wrong address; `OpponentPresence` carries no seat field, so the two differ only in the
presence they report.

**And that mixup is undetectable in this fixture.** The host resumes without having been away, so
`presenceOf(guest)` and `presenceOf(host)` are both `PRESENT`. Asserting the address and the count
is all this fixture can prove. The value mixup is already caught on the running-room path by
`TASK-021410`'s `theReturningSeatIsToldTheOpponentIsAway`, where the two presences differ — say so
in the test's comment rather than implying this assertion closes it.

## Out of scope

- Any production change. This ticket asserts behaviour that already exists; if it turns out not to,
  stop and say so rather than fixing it here — that would be a different ticket.
- Every other test in the file, and every other resume path. `TASK-021410`'s new tests already cover
  PRESENT, AWAY and ABSENT on a running room.

## Tests

`RoomResumeTest`

| Test | Proves |
| --- | --- |
| `aFinishedRoomResumesAsItsOutcome` (extended) | a FINISHED room's resume carries exactly one `OpponentPresence`, addressed to the returning seat and reading `PRESENT`, alongside its one `DuelFinished`, and nothing else |

## Acceptance criteria

- [ ] `aFinishedRoomResumesAsItsOutcome` asserts the `OpponentPresence` frame's address as the literal `0`
- [ ] It asserts the total `outbound` size
- [ ] Its existing `DuelFinished` and no-`Snapshot` assertions are unchanged
- [ ] No production file changes
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
