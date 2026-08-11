---
schema: 2
id: TASK-010407
title: Seat state and its construction invariants
type: task
status: ready
parent: STORY-0104
module: poker-engine
estimate: S
tier: haiku
review: standard
files_touched: 2
labels: [engine, domain, contract]
depends_on: [TASK-010406]
verify:
  - ./gradlew :poker-engine:test --tests '*SeatTest'
  - ./gradlew :poker-engine:check
---

## Goal

One player's position in a hand — chips, cards and status — as an immutable value that cannot be
constructed in an impossible shape.

## Files

| File | Action |
| --- | --- |
| `poker-engine/src/main/kotlin/duels/poker/engine/game/Seat.kt` | create |
| `poker-engine/src/test/kotlin/duels/poker/engine/game/SeatTest.kt` | create |

Read `poker-engine/src/main/kotlin/duels/poker/engine/card/Card.kt` for the `Card` API.

## Scope

- `Seat.kt`, package `duels.poker.engine.game`:

  ```kotlin
  public data class Seat(
      val index: Int,
      val stack: Int,
      val holeCards: List<Card> = emptyList(),
      val committedThisStreet: Int = 0,
      val committedThisHand: Int = 0,
      val hasFolded: Boolean = false,
      val isAllIn: Boolean = false,
  ) {
      init {
          require(index in SEAT_INDICES) { "Seat index must be 0 or 1, was $index" }
          require(stack >= 0) { "Stack cannot be negative, was $stack" }
          require(holeCards.size in HOLE_CARD_SIZES) { "A seat holds 0 or 2 hole cards, not ${holeCards.size}" }
          require(holeCards.toSet().size == holeCards.size) { "Duplicate hole card in $holeCards" }
          require(committedThisStreet >= 0) { "..." }
          require(committedThisHand >= committedThisStreet) { "..." }
      }
  }
  ```

  `SEAT_INDICES` (`0..1`) and `HOLE_CARD_SIZES` are `private val` in the file; detekt flags bare
  literals.
- KDoc: `index` **is** the seat's identity — `0` and `1`, never "hero" and "villain", which are
  viewer-relative and belong to the projection layer. `committedThisStreet` is what this seat has
  put in on the current street; `committedThisHand` is the gross total for the hand and never
  decreases while the hand runs.

## Out of scope

- `commit`, `award` and street cleanup — `TASK-010408`.
- Who is on the button, and whose turn it is — `GameState`, `TASK-010409`.
- Any rule about when a seat may fold or is all-in — STORY-0105.

## Tests

`SeatTest`, JUnit 5. Build cards with `duels.poker.engine.card.cards("As Kd")`.

| Test | Proves |
| --- | --- |
| `buildsASeatWithDefaults` | `Seat(index = 0, stack = 10_000)` has no cards, zero commitments, not folded, not all-in |
| `holdsExactlyTwoHoleCards` | `holeCards = cards("As Kd")` round-trips through `copy` |
| `rejectsASeatIndexOutsideZeroAndOne` | index `-1` and `2` each throw `IllegalArgumentException` |
| `rejectsANegativeStack` | `stack = -1` throws |
| `rejectsAnyHoleCardCountButZeroOrTwo` | one card and three cards each throw |
| `rejectsDuplicateHoleCards` | `"As As"` throws |
| `rejectsStreetCommitmentAboveHandCommitment` | `committedThisStreet = 100, committedThisHand = 50` throws |
| `equalSeatsAreEqual` | two seats built from the same values are `==` and share a `hashCode` |

## Acceptance criteria

- [ ] `SeatTest.buildsASeatWithDefaults` passes
- [ ] `SeatTest.holdsExactlyTwoHoleCards` passes
- [ ] `SeatTest.rejectsASeatIndexOutsideZeroAndOne` passes
- [ ] `SeatTest.rejectsANegativeStack` passes
- [ ] `SeatTest.rejectsAnyHoleCardCountButZeroOrTwo` passes
- [ ] `SeatTest.rejectsDuplicateHoleCards` passes
- [ ] `SeatTest.rejectsStreetCommitmentAboveHandCommitment` passes
- [ ] `SeatTest.equalSeatsAreEqual` passes
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
