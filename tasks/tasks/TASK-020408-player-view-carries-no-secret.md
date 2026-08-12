---
schema: 2
id: TASK-020408
title: Assert structurally that a view carries no deck, rng or seed
type: task
status: done
parent: STORY-0204
module: poker-engine
estimate: S
tier: sonnet
review: deep
files_touched: 1
labels: [engine, projection, security, serialization]
depends_on: [TASK-020404]
verify:
  - ./gradlew :poker-engine:test --tests '*PlayerViewSerializationTest'
  - ./gradlew :poker-engine:check
---

## Goal

A test, not a reading of the class, proves that the wire form of a `PlayerView` names no deck, no
rng and no seed, and carries no card the viewer is not entitled to.

## Files

| File | Action |
| --- | --- |
| `poker-engine/src/test/kotlin/duels/poker/engine/game/PlayerViewSerializationTest.kt` | create |

Read, do not modify: `poker-engine/src/main/kotlin/duels/poker/engine/game/PlayerView.kt`,
`poker-engine/src/main/kotlin/duels/poker/engine/game/SeatView.kt`,
`poker-engine/src/test/kotlin/duels/poker/engine/game/GameStates.kt`,
`poker-engine/src/test/kotlin/duels/poker/engine/card/Cards.kt`,
`poker-engine/src/test/kotlin/duels/poker/engine/game/GameEventSerializationTest.kt` (the `Json`
setup to copy).

## Scope

- One new test class. No main-source file changes: if a test here fails, the fix is a new ticket
  against `PlayerView`, not an edit from this branch.
- Walk the wire shape through the generated descriptors rather than through reflection: for
  `PlayerView.serializer().descriptor` and each `getElementDescriptor(i)`, iterate
  `0 until elementsCount` reading `getElementName(i)`. `elementsCount`, `getElementName` and
  `getElementDescriptor` are stable API, so no `@OptIn` is needed and no new dependency appears.
- Build the sample view with `PlayerView.of(state, 0)` from a `handState()` where seat 0 holds
  `cards("As Kh")`, seat 1 holds `cards("Qd Jc")` and the board is `cards("2c 7d 9s")`.
- Assert on the encoded JSON **string** for the card checks: `Card` serializes to its standard
  notation, so `"Qd"` appearing anywhere in the payload is a leak.

## Out of scope

- Changing `PlayerView` or `SeatView` in any way.
- Reveals — this ticket only ever calls `of(state, seat)`, whose opponent cards are always
  hidden; the revealed case is `TASK-020405`'s and the property runs are `TASK-020410`'s.
- `kotlin-reflect`: it is not on the engine's classpath and adding it would fail
  `checkNoDependencies`.

## Tests

`PlayerViewSerializationTest`, JUnit 5, package `duels.poker.engine.game`, with
`private val json = Json { prettyPrint = false }`.

| Test | Proves |
| --- | --- |
| `noFieldOfAViewIsNamedDeckRngOrSeed` | no element name of `PlayerView`'s descriptor, nor of any of its element descriptors, equals `deck`, `rng` or `seed` ignoring case |
| `noFieldOfAViewHasADeckOrRngType` | no element descriptor's `serialName` contains `Deck` or `Rng` |
| `theViewsFieldNamesAreExactlyTheTwelveDeclared` | `PlayerView`'s descriptor names exactly `viewerSeat, handNumber, buttonSeat, street, board, pot, betToMatch, minRaiseTo, seatToAct, smallBlind, bigBlind, seats`, so a field added later fails this test and gets a decision |
| `aViewRoundTripsThroughJson` | encoding and decoding the sample view returns an equal value |
| `theJsonCarriesTheViewersOwnCards` | the encoded string contains `As` and `Kh` |
| `theJsonCarriesNoOpponentCard` | the encoded string contains neither `Qd` nor `Jc` |
| `theJsonCarriesTheBoard` | the encoded string contains `2c`, `7d` and `9s` |

## Acceptance criteria

- [ ] `PlayerViewSerializationTest.noFieldOfAViewIsNamedDeckRngOrSeed` passes
- [ ] `PlayerViewSerializationTest.noFieldOfAViewHasADeckOrRngType` passes
- [ ] `PlayerViewSerializationTest.theViewsFieldNamesAreExactlyTheTwelveDeclared` passes
- [ ] `PlayerViewSerializationTest.aViewRoundTripsThroughJson` passes
- [ ] `PlayerViewSerializationTest.theJsonCarriesTheViewersOwnCards` passes
- [ ] `PlayerViewSerializationTest.theJsonCarriesNoOpponentCard` passes
- [ ] `PlayerViewSerializationTest.theJsonCarriesTheBoard` passes
- [ ] No file outside the one in the Files table is modified
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
