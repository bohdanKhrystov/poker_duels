---
schema: 2
id: TASK-010208
title: Lock two seeds to their recorded deck orderings
type: task
status: done
parent: STORY-0102
module: poker-engine
estimate: XS
tier: haiku
review: standard
files_touched: 1
labels: [engine, test, determinism]
depends_on: [TASK-010207]
verify:
  - ./gradlew :poker-engine:test --tests '*DeckDeterminismTest'
  - ./gradlew :poker-engine:check
---

## Goal

Two seeds are nailed to the exact deck they produce, so any future change to the generator or
the shuffle breaks a test loudly instead of silently invalidating every stored replay.

## Files

| File | Action |
| --- | --- |
| `poker-engine/src/test/kotlin/duels/poker/engine/card/DeckDeterminismTest.kt` | create |

Read `Deck.kt` and `SplitMix64Rng.kt` for the API only.

## Scope

- One test file holding the two recorded orderings below as string constants, plus a file-level
  comment stating that changing either string is a breaking change to every stored replay and
  must be an ADR, not a test edit.
- The ordering under test is produced exactly like this:

  ```kotlin
  Deck.full().shuffled(SplitMix64Rng(seed)).deck.deal(52).cards.joinToString(" ")
  ```

- Recorded orderings, to be pasted verbatim:

  - seed `42`:

    ```
    9h Ad 4h 8s Js 9s Qs Kh Ts 5d 4d Tc Kc 2h Th Ks 8h Td As 3s 9c 8d 6d 4c 6s 3h Ah Qd 7c 5h 5c Ac Jh Qh 5s Jc 9d 7h 8c 3c 2c 2d Jd 2s 6h 6c Qc Kd 3d 7d 7s 4s
    ```

  - seed `7`:

    ```
    9d Qs 7c 3d 3c 9h Th 2s 4h 8d 2c 9c 5d 2d 9s 5h Jh 7h 4s Qd 7s Jc 8s Ks 4c 6c Kh 6s Kc 4d 3h 5c Qc Td As Ad 8h Ah 2h 3s 5s 6d Ac 7d Jd Qh Kd Ts Js Tc 8c 6h
    ```

- These strings were computed from the algorithm pinned in `TASK-010205` and `TASK-010207`.
  **If a test fails, the implementation deviates from that algorithm — fix the implementation.
  Do not edit the recorded strings, and do not regenerate them from the current output.** If you
  believe a string is wrong, say so in the PR and stop.

## Out of scope

- Uniformity statistics — `TASK-010209`.
- Any change to `Deck.kt`, `SplitMix64Rng.kt` or `Rng.kt`. This ticket touches one test file.
- Serialising a seed or an event log — `STORY-0108`.

## Tests

`DeckDeterminismTest`

| Test | Proves |
| --- | --- |
| `seedFortyTwoProducesTheRecordedOrdering` | the seed-`42` shuffle deals exactly the recorded 52-card string |
| `seedSevenProducesTheRecordedOrdering` | the seed-`7` shuffle deals exactly the recorded 52-card string, and it differs from the seed-`42` one |

## Acceptance criteria

- [ ] `DeckDeterminismTest.seedFortyTwoProducesTheRecordedOrdering` passes
- [ ] `DeckDeterminismTest.seedSevenProducesTheRecordedOrdering` passes
- [ ] The two recorded strings in the merged file are character-for-character those in this
      ticket
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
