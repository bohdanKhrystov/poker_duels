---
schema: 2
id: TASK-021303
title: The second offer starts a fresh duel, with the button on the other seat
type: task
status: backlog
parent: STORY-0213
module: poker-server
estimate: S
tier: sonnet
review: standard
labels: [server, protocol, rooms, tests]
files_touched: 1
depends_on: [TASK-021302]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.DuelSocketRematchTest'
  - ./gradlew :poker-server:ktlintCheck
---

## Goal

`ADR-0044` §4 over the wire: the second seat's `OfferRematch` starts a fresh duel, both sockets
receive its opening frames, the `Snapshot` puts the button on the **other** seat, and there is no
started frame of any kind — the `Snapshot` *is* the start.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/test/kotlin/duels/poker/server/DuelSocketRematchTest.kt` | modify — two tests added, fixture and existing test unchanged |

## Scope

- Both tests reuse `TASK-021302`'s `finishedDuel` fixture: host offers, guest offers, both sockets
  are drained after the second offer.
- The first duel's opening `Snapshot` is captured too, because the button claim is a *comparison*.
  A room opened by `RoomRegistry.create` has `openingButtonSeat = 0`, and `Room.offerRematch`
  flips it to `1 - openingButtonSeat` before the fresh runner is built.

## Out of scope

- Anything about the first offer — `TASK-021302` owns it, and its assertions do not move.
- A repeat offer, a refusal, a reconnect — `TASK-021304` onward.
- Playing the rematch out. That it *started* is this ticket's whole claim.

## Tests

`DuelSocketRematchTest`

| Test | Proves |
| --- | --- |
| `theSecondOfferStartsAFreshDuelOnBothSockets` | after the guest's `OfferRematch`, each socket has received at least one `Snapshot`, exactly one socket received a `YourTurn`, and **neither** received a `RematchOffered` — agreement produces the duel's own frames and no fact of its own |
| `theRematchPutsTheButtonOnTheOtherSeat` | the first duel's opening `Snapshot` carries `view.buttonSeat == 0` and the rematch's opening `Snapshot` carries `view.buttonSeat == 1`, on the same socket |

`theRematchPutsTheButtonOnTheOtherSeat` asserts both numbers, never only the second: a
`buttonSeat` that was `1` all along would satisfy half of it and prove nothing about the flip.

## Acceptance criteria

- [ ] `DuelSocketRematchTest.theSecondOfferStartsAFreshDuelOnBothSockets` passes
- [ ] `DuelSocketRematchTest.theRematchPutsTheButtonOnTheOtherSeat` passes
- [ ] `DuelSocketRematchTest.oneOfferPutsOneRematchOfferedNamingThatSeatOnBothSockets` passes with
      every assertion it already had
- [ ] No file outside `DuelSocketRematchTest.kt` changes
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
