---
schema: 2
id: TASK-010418
title: Settlement events — uncalled bet, pot award, hand finished
type: task
status: done
parent: STORY-0104
module: poker-engine
estimate: XS
tier: haiku
review: standard
files_touched: 2
labels: [engine, domain, chips]
depends_on: [TASK-010417]
verify:
  - ./gradlew :poker-engine:test --tests '*SettlementEventsTest'
  - ./gradlew :poker-engine:check
---

## Goal

The three events that move chips back out of the pot and close the hand — the last words in every
hand's log.

## Files

| File | Action |
| --- | --- |
| `poker-engine/src/main/kotlin/duels/poker/engine/game/DealerEvents.kt` | modify |
| `poker-engine/src/test/kotlin/duels/poker/engine/game/SettlementEventsTest.kt` | create |

Read `TASK-010417` for the `DealerEvent` interface these join. Do not change the events already
in the file.

## Scope

- Appended to `DealerEvents.kt`:

  ```kotlin
  /** Chips nobody could cover, going back to the seat that bet them. Always before [PotAwarded]. */
  public data class UncalledBetReturned(
      override val sequence: Int,
      val seat: Int,
      val amount: Int,
  ) : DealerEvent

  /** Part or all of the pot going to a seat. A split pot emits one of these per seat. */
  public data class PotAwarded(
      override val sequence: Int,
      val seat: Int,
      val amount: Int,
  ) : DealerEvent

  /** The hand is over. Always the last event of a hand. */
  public data class HandFinished(override val sequence: Int) : DealerEvent
  ```

- `require`s: `sequence >= 0`, `seat in 0..1`, `amount > 0`.
- `amount` here is chips moving out of the pot, **not** a street total — these two events are the
  only place in the vocabulary where an amount is a delta, and the KDoc must say so, because
  everything else in the module is a total.

## Out of scope

- Deciding who wins or how much — STORY-0106.
- The ordering rule between these events — it is asserted where the events are emitted
  (`TASK-010601`), not in a constructor.

## Tests

`SettlementEventsTest`, JUnit 5.

| Test | Proves |
| --- | --- |
| `settlementEventsAreVersionedDealerEvents` | all three are `DealerEvent` and report `version == EVENT_SCHEMA_VERSION` |
| `amountsAreChipsMoving` | `UncalledBetReturned(11, 0, 400).amount == 400`, `PotAwarded(12, 1, 1_200).amount == 1_200` |
| `rejectsANonPositiveAmount` | amount `0` and `-1` each throw `IllegalArgumentException` for both events |
| `rejectsAnInvalidSeatOrSequence` | `seat = 2` and `sequence = -1` each throw |
| `handFinishedCarriesOnlyItsSequence` | `HandFinished(13).sequence == 13` |

## Acceptance criteria

- [ ] `SettlementEventsTest.settlementEventsAreVersionedDealerEvents` passes
- [ ] `SettlementEventsTest.amountsAreChipsMoving` passes
- [ ] `SettlementEventsTest.rejectsANonPositiveAmount` passes
- [ ] `SettlementEventsTest.rejectsAnInvalidSeatOrSequence` passes
- [ ] `SettlementEventsTest.handFinishedCarriesOnlyItsSequence` passes
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
