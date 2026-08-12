---
schema: 2
id: TASK-010815
title: PlayerAction is serializable under a short type name
type: task
status: backlog
parent: STORY-0108
module: poker-engine
estimate: S
tier: haiku
review: light
files_touched: 2
labels: [engine, serialization, log]
depends_on: [TASK-010813]
verify:
  - ./gradlew :poker-engine:test --tests '*PlayerActionSerializationTest'
  - ./gradlew :poker-engine:check
---

## Goal

The six things a player can attempt survive a round trip through JSON, each tagged with a short,
stable name rather than a fully qualified class name.

## Files

| File | Action |
| --- | --- |
| `poker-engine/src/main/kotlin/duels/poker/engine/game/PlayerAction.kt` | modify |
| `poker-engine/src/test/kotlin/duels/poker/engine/game/PlayerActionSerializationTest.kt` | create |

## Scope

- `@Serializable` on the sealed interface `PlayerAction` and on each of its six nested data
  classes, each subtype also carrying `@SerialName` with its own simple name:
  `"Fold"`, `"Check"`, `"Call"`, `"Bet"`, `"Raise"`, `"AllIn"`.
- The `@SerialName` values are the wire contract. Without them the discriminator would be the
  fully qualified class name, and moving a class between packages would silently change the format
  of every stored log.
- Nothing else in the file changes: `type` stays a computed `val` with no backing field, so it is
  not written to the wire and the `ActionType` enum needs no annotation. The `require` blocks in
  `Bet` and `Raise` stay exactly as they are — they run on decode too, which is the point.

## Out of scope

- Any event type — `TASK-010816`, `TASK-010817`.
- `HandLog` — `TASK-010818`.
- Introducing a shared `Json` instance; each test configures its own until `TASK-010819` gives the
  log one.

## Tests

`PlayerActionSerializationTest`, JUnit 5, package `duels.poker.engine.game`. Encode through the
parent serializer — `Json.encodeToString(PlayerAction.serializer(), action)` — because that is how
a `List<PlayerAction>` inside a log will be written.

| Test | Proves |
| --- | --- |
| `everyActionRoundTripsThroughTheParentSerializer` | all six actions, encoded and decoded through `PlayerAction.serializer()`, come back equal |
| `betEncodesItsSeatAndAmount` | `PlayerAction.Bet(0, 300)` encodes to exactly `{"type":"Bet","seat":0,"to":300}` |
| `theDiscriminatorIsTheShortName` | the encoding of `PlayerAction.AllIn(1)` contains `"type":"AllIn"` and does not contain `duels.poker.engine` |
| `aListOfActionsRoundTrips` | a list of four different actions round-trips through `ListSerializer(PlayerAction.serializer())` |
| `decodingAnUnknownActionTypeFails` | `{"type":"Shove","seat":0}` throws `SerializationException` |
| `decodingANonPositiveBetFails` | `{"type":"Bet","seat":0,"to":0}` throws `IllegalArgumentException` — the `init` guard runs on decode |

## Acceptance criteria

- [ ] `PlayerActionSerializationTest.everyActionRoundTripsThroughTheParentSerializer` passes
- [ ] `PlayerActionSerializationTest.betEncodesItsSeatAndAmount` passes
- [ ] `PlayerActionSerializationTest.theDiscriminatorIsTheShortName` passes
- [ ] `PlayerActionSerializationTest.aListOfActionsRoundTrips` passes
- [ ] `PlayerActionSerializationTest.decodingAnUnknownActionTypeFails` passes
- [ ] `PlayerActionSerializationTest.decodingANonPositiveBetFails` passes
- [ ] `PlayerActionTest` still passes unchanged — this ticket adds annotations and changes no
      behaviour it observes
- [ ] No file outside the two in the Files table is modified
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
