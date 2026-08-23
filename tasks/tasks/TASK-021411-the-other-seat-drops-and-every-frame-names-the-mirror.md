---
schema: 2
id: TASK-021411
title: The host is the seat that goes, and every presence frame names the mirror image
type: task
status: ready
parent: STORY-0214
module: poker-server
estimate: S
tier: haiku
review: standard
files_touched: 3
labels: [server, presence, tests]
depends_on: [TASK-021410]
verify:
  - ./gradlew :poker-server:test --tests '*DuelSocketDisconnectTest'
  - ./gradlew :poker-server:test --tests '*GraceExpiryTest'
  - ./gradlew :poker-server:test --tests '*RoomResumeTest'
---

## Goal

Every per-seat value this story puts on the wire is asserted at **both** of its values, so a
hard-coded seat cannot pass the story. A **test-only** ticket: no production file is opened.

## Why this exists

`STORY-0213` shipped a whole story in which every ticket drove the same seat into the action, and a
hard-coded constant would have passed all seven; `TASK-021308` had to be filed afterwards to flip
it. Three paths here have the same shape and are flipped **before** they can hide anything:

- the socket path always drops the **guest**, so `OpponentPresence` is only ever delivered to the
  host — a `deliver` to a fixed seat 0 would pass `TASK-021405` and `TASK-021406`;
- `GraceExpiryTest` always expires the seat that is **on turn** for one fixed seed, so `ABSENT` is
  only ever addressed to one seat and the mark only ever names the other;
- `RoomResumeTest` always brings the **guest** back, so a returning seat is only ever 1.

The mark's own `seat` field is already covered: `TASK-021408` asserts it at the button's seat and
`TASK-021409` at the big blind's, which are different by construction. Nothing here re-proves that.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/test/kotlin/duels/poker/server/DuelSocketDisconnectTest.kt` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/room/GraceExpiryTest.kt` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/room/RoomResumeTest.kt` | modify |

Read nothing new: every fixture these tests need is already in the three files.

## Scope

- One test per file, each the mirror image of a test the story already has, reusing that file's
  existing fixtures and helpers rather than adding new ones.
- Assert the **seat the frame is addressed to**, not merely that a frame arrived. "A presence frame
  exists" is true of the bug this ticket exists to catch.
- No production change, no new fixture constant, no new helper, and no edit to any existing test.

## Out of scope

- **Any production file.** A failure here is a defect and a new ticket, not an edit from this
  branch.
- The mark's `seat` field, covered by `TASK-021408` and `TASK-021409` at two values already.
- `presenceOf`'s own seat argument, covered at both values by `TASK-021403`.

## Tests

| Test | File | Proves |
| --- | --- | --- |
| `aClosingHostTellsTheGuestItIsAway` | `DuelSocketDisconnectTest` | after `setup.host.close()`, the **guest**'s drained frames carry exactly one `OpponentPresence(AWAY, TEST_DISCONNECT_GRACE_MILLIS)`, and the host's socket carried none — the mirror of `aClosingSocketTellsTheOpponentItIsAway` |
| `theSeatOffTurnExpiringTellsTheSeatOnTurn` | `GraceExpiryTest` | dropping and expiring `seatedPlayer(offTurn, host, guest)` puts `OpponentPresence(ABSENT, null)` addressed to `onTurn`, and none addressed to `offTurn` — the mirror of `theSeatThatStayedIsToldTheOtherIsAbsent`, whose expired seat is `onTurn` |
| `aReturningHostIsToldAndTellsTheGuest` | `RoomResumeTest` | with the **host** dropped and resuming, `outbound` carries `OpponentPresence` addressed to seat 0 (the returning host, about the guest) and one addressed to seat 1 (the guest, about the host) — the mirror of `theReturningSeatIsToldTheOpponentIsPresent` |

## Acceptance criteria

- [ ] `DuelSocketDisconnectTest.aClosingHostTellsTheGuestItIsAway` passes
- [ ] `GraceExpiryTest.theSeatOffTurnExpiringTellsTheSeatOnTurn` passes
- [ ] `RoomResumeTest.aReturningHostIsToldAndTellsTheGuest` passes
- [ ] Each of the three asserts the recipient seat number, and each asserts that **no** presence
      frame went to the seat that left
- [ ] No production file is in the diff, and no existing test in any of the three files is edited
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.
