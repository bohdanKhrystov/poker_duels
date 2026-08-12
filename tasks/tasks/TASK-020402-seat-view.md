---
schema: 2
id: TASK-020402
title: A seat as a recipient may see it
type: task
status: ready
parent: STORY-0204
module: poker-engine
estimate: S
tier: haiku
review: standard
files_touched: 2
labels: [engine, projection, security]
depends_on: []
verify:
  - ./gradlew :poker-engine:test --tests '*SeatViewTest'
  - ./gradlew :poker-engine:check
---

## Goal

`SeatView` exists: everything about a seat a recipient may be told, with hole cards **hidden by
default** rather than copied across.

## Files

| File | Action |
| --- | --- |
| `poker-engine/src/main/kotlin/duels/poker/engine/game/SeatView.kt` | create |
| `poker-engine/src/test/kotlin/duels/poker/engine/game/SeatViewTest.kt` | create |

Read, do not modify: `poker-engine/src/main/kotlin/duels/poker/engine/game/Seat.kt` (the fields
and their `require` blocks), `poker-engine/src/test/kotlin/duels/poker/engine/card/Cards.kt`
(the `cards("As Kh")` test helper).

## Scope

- One public type with KDoc, package `duels.poker.engine.game`:

  ```kotlin
  @Serializable
  public data class SeatView(
      val index: Int,
      val stack: Int,
      val committedThisStreet: Int,
      val committedThisHand: Int,
      val hasFolded: Boolean,
      val isAllIn: Boolean,
      val holeCards: List<Card> = emptyList(),
  )
  ```

- `holeCards` comes **last and defaults to empty**: hiding is the default, showing is a
  deliberate act by the caller. Say so in the KDoc — this is the whole point of the type.
- `require` blocks mirroring `Seat`'s, in this order: `index in 0..1`; `stack >= 0`;
  `holeCards.size` is 0 or 2; hole cards distinct; `committedThisStreet >= 0`;
  `committedThisHand >= committedThisStreet`. Each message names the offending value.
- It is a separate type from `Seat`, not a copy of it. That is what makes a future field on
  `Seat` invisible until someone adds it here on purpose.

## Out of scope

- Building one from a `Seat` — that mapping is `TASK-020404`'s, and it is where the entitlement
  rule lives.
- The `PlayerView` that holds two of these: `TASK-020403`.
- Any "hero"/"villain" naming. A seat is 0 or 1 everywhere in the engine.

## Tests

`SeatViewTest`, JUnit 5, package `duels.poker.engine.game`. Build with named arguments; use
`cards("As Kh")` for a two-card hand.

| Test | Proves |
| --- | --- |
| `hidesHoleCardsByDefault` | a `SeatView` built without `holeCards` has `holeCards == emptyList<Card>()` |
| `carriesTheHoleCardsItIsGiven` | a `SeatView` built with `cards("As Kh")` returns exactly those two, in order |
| `rejectsASeatIndexOutsideZeroOrOne` | `index = 2` throws `IllegalArgumentException` |
| `rejectsANegativeStack` | `stack = -1` throws `IllegalArgumentException` |
| `rejectsOneHoleCard` | `holeCards = cards("As")` throws `IllegalArgumentException` |
| `rejectsDuplicateHoleCards` | two identical cards throw `IllegalArgumentException` |
| `rejectsAStreetCommitmentAboveTheHandCommitment` | `committedThisStreet = 200, committedThisHand = 100` throws `IllegalArgumentException` |
| `roundTripsThroughJson` | encoding and decoding a `SeatView` holding two cards returns an equal value |

## Acceptance criteria

- [ ] `SeatViewTest.hidesHoleCardsByDefault` passes
- [ ] `SeatViewTest.carriesTheHoleCardsItIsGiven` passes
- [ ] `SeatViewTest.rejectsASeatIndexOutsideZeroOrOne` passes
- [ ] `SeatViewTest.rejectsANegativeStack` passes
- [ ] `SeatViewTest.rejectsOneHoleCard` passes
- [ ] `SeatViewTest.rejectsDuplicateHoleCards` passes
- [ ] `SeatViewTest.rejectsAStreetCommitmentAboveTheHandCommitment` passes
- [ ] `SeatViewTest.roundTripsThroughJson` passes
- [ ] No file outside the two in the Files table is modified
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
