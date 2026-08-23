---
schema: 2
id: TASK-021306
title: REMATCH_UNAVAILABLE is transient, and the same offer succeeds afterwards
type: task
status: backlog
parent: STORY-0213
module: poker-server
estimate: S
tier: sonnet
review: standard
labels: [server, protocol, rooms, tests]
files_touched: 1
depends_on: [TASK-021305]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.DuelSocketRematchTest'
  - ./gradlew :poker-server:ktlintCheck
---

## Goal

`ADR-0044` §6's second bullet, made executable: an `OfferRematch` while the duel is still running is
refused as `Failure(REMATCH_UNAVAILABLE)` and changes nothing — and the **same** offer is accepted
once that duel has finished, which is what makes the word *transient* a fact rather than a KDoc.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/test/kotlin/duels/poker/server/DuelSocketRematchTest.kt` | modify — two tests added, fixture and existing tests unchanged |

## Scope

- This is the one place a test needs a duel that is **running**, so it cannot start from
  `finishedDuel`. Split the fixture's steps: seat both sockets on the `FixedHands(1)` room, drain,
  and offer *before* the fold rather than after it. Take the shared part out into a helper the
  existing `finishedDuel` also calls, so the fixture keeps one definition — that is a change inside
  this file and inside this ticket's budget.
- *Changes nothing* is asserted by what happens next, not by an absence: after the refusal, the host
  plays the fold as normal, the duel finishes for both seats, and the second test's offer is
  accepted. A duel the refusal had disturbed could not do that.

## Out of scope

- `RoomRegistry`'s `recording` window — the other condition behind `NOT_FINISHED`, which is the
  moment between a duel finishing and its result reaching the sink. It is the registry's own guard,
  reached through the same refusal, and forcing it open at the socket needs a slow `DuelResultSink`
  double that no other test in this file needs. Not ticketed.
- `UNKNOWN_ROOM` — `TASK-021305`.
- Any assertion about *how long* the window lasts. There is no deadline on the wire, deliberately
  (`ADR-0044`, *Alternatives considered*).

## Tests

`DuelSocketRematchTest`

| Test | Proves |
| --- | --- |
| `anOfferWhileTheDuelIsRunningIsRefusedAsRematchUnavailable` | with the duel still in `RoomState.PLAYING`, the host's `OfferRematch` puts exactly one frame on the host's socket, `Failure(REMATCH_UNAVAILABLE)`, and **nothing** on the guest's |
| `theSameOfferIsAcceptedOnceTheDuelHasFinished` | the same socket, having been refused, folds the duel to its end and offers again — and that offer is answered with `RematchOffered(seat = 0)` on both sockets, with no `Failure` anywhere |

`theSameOfferIsAcceptedOnceTheDuelHasFinished` must send the refused offer first and reuse the same
socket, never open a fresh one: the claim is that the *same* client may re-send, and a new socket
would prove only that a rematch works, which `TASK-021302` already proves.

## Acceptance criteria

- [ ] `DuelSocketRematchTest.anOfferWhileTheDuelIsRunningIsRefusedAsRematchUnavailable` passes
- [ ] `DuelSocketRematchTest.theSameOfferIsAcceptedOnceTheDuelHasFinished` passes
- [ ] The refused offer leaves the guest's drained frame list **empty**
- [ ] Every test from `TASK-021302`–`TASK-021305` passes with every assertion it already had
- [ ] No file outside `DuelSocketRematchTest.kt` changes
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
