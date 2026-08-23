---
schema: 2
id: TASK-021304
title: A repeat offer is answered, not refused, and records nothing new
type: task
status: backlog
parent: STORY-0213
module: poker-server
estimate: S
tier: sonnet
review: standard
labels: [server, protocol, rooms, tests]
files_touched: 1
depends_on: [TASK-021303]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.DuelSocketRematchTest'
  - ./gradlew :poker-server:ktlintCheck
---

## Goal

`ADR-0044` §3 over the wire: a second `OfferRematch` from a seat that has already offered is **not**
an error. It is answered with the same `RematchOffered(seat)`, to that socket alone, and it records
nothing — so a double click cannot produce an error state and cannot spend the opponent's turn.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/test/kotlin/duels/poker/server/DuelSocketRematchTest.kt` | modify — two tests added, fixture and existing tests unchanged |

## Scope

- Both tests reuse `TASK-021302`'s `finishedDuel` fixture. The host offers **twice** in a row.
- The second test then has the guest offer once, and asserts the duel starts on that offer — which
  is what proves the repeat recorded nothing. If the repeat had been recorded as a second offer, the
  room would have agreed on it and the fresh duel would already have started before the guest acted.

## Out of scope

- Two *different* seats offering — `TASK-021303` owns agreement.
- Any refusal — `TASK-021305` and `TASK-021306`.
- Concurrency. Two offers racing on one socket is not reachable: one connection's frames are
  processed one at a time, in a single coroutine.

## Tests

`DuelSocketRematchTest`

| Test | Proves |
| --- | --- |
| `aRepeatOfferIsAnsweredWithRematchOfferedAndNotAFailure` | the host's second `OfferRematch` puts exactly one `RematchOffered(seat = 0)` on the **host's** socket, no `Failure` of any kind, and **nothing at all** on the guest's socket — the opponent was told once, when the offer was recorded |
| `aRepeatOfferRecordsNothingSoTheOpponentsOfferIsStillTheOneThatStarts` | after host, host, guest, the fresh duel's `Snapshot` arrives on the guest's `OfferRematch` and on nothing earlier: the guest's socket receives no `Snapshot` between the two host offers |

## Acceptance criteria

- [ ] `DuelSocketRematchTest.aRepeatOfferIsAnsweredWithRematchOfferedAndNotAFailure` passes
- [ ] `DuelSocketRematchTest.aRepeatOfferRecordsNothingSoTheOpponentsOfferIsStillTheOneThatStarts` passes
- [ ] The repeat leaves the guest's drained frame list **empty**
- [ ] The two tests from `TASK-021302` and `TASK-021303` pass with every assertion they already had
- [ ] No file outside `DuelSocketRematchTest.kt` changes
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
