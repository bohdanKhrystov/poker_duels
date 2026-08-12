---
schema: 2
id: TASK-020401
title: Make `Board` serializable
type: task
status: done
parent: STORY-0204
module: poker-engine
estimate: XS
tier: haiku
review: light
files_touched: 2
labels: [engine, serialization, projection]
depends_on: []
verify:
  - ./gradlew :poker-engine:test --tests '*BoardSerializationTest'
  - ./gradlew :poker-engine:check
---

## Goal

`Board` carries `@Serializable`, so the `PlayerView` that will hold one can be serialized whole.

## Files

| File | Action |
| --- | --- |
| `poker-engine/src/main/kotlin/duels/poker/engine/game/Board.kt` | modify |
| `poker-engine/src/test/kotlin/duels/poker/engine/game/BoardSerializationTest.kt` | create |

Read, do not modify: `poker-engine/src/test/kotlin/duels/poker/engine/card/CardSerializerTest.kt`
(how a card round-trips), `poker-engine/src/test/kotlin/duels/poker/engine/card/Cards.kt` (the
`cards("As Kh 2d")` test helper).

## Scope

- Add `@Serializable` to `Board` and the `kotlinx.serialization.Serializable` import. Nothing
  else in `Board.kt` changes: same fields, same `require` blocks, same `EMPTY`, same `dealt`.
  `ADR-0010` already permits this, so `checkNoDependencies` is unaffected.
- `Card` already has its own serializer, so a board serializes as an object with one `cards`
  array of card strings and needs no custom serializer of its own.

## Out of scope

- Annotating `GameState` or `Seat`. Neither ever crosses the wire — `GameState` carries the deck
  and the rng, which `ADR-0002` says never leave the server, and the redacted seat is a new type
  in `TASK-020402`.
- The `PlayerView` that will hold this board: `TASK-020403`.

## Tests

`BoardSerializationTest`, JUnit 5, package `duels.poker.engine.game`. Use
`private val json = Json { prettyPrint = false }` as `GameEventSerializationTest` does.

| Test | Proves |
| --- | --- |
| `anEmptyBoardRoundTrips` | `Board.EMPTY` encodes and decodes back equal |
| `aCompleteBoardRoundTrips` | `Board(cards("As Kh 2d 7c 9s"))` encodes and decodes back equal |
| `theJsonNamesEachCardInStandardNotation` | the encoded string of `Board(cards("As Kh 2d"))` contains `As`, `Kh` and `2d` |

## Acceptance criteria

- [ ] `BoardSerializationTest.anEmptyBoardRoundTrips` passes
- [ ] `BoardSerializationTest.aCompleteBoardRoundTrips` passes
- [ ] `BoardSerializationTest.theJsonNamesEachCardInStandardNotation` passes
- [ ] `BoardTest` is not modified and still passes as part of `./gradlew :poker-engine:check`
- [ ] `./gradlew :poker-engine:checkNoDependencies` passes, as part of `:poker-engine:check`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
