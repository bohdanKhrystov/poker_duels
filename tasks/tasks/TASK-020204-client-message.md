---
schema: 2
id: TASK-020204
title: ClientMessage — a hierarchy that can only express an intent
type: task
status: done
parent: STORY-0202
module: poker-server
estimate: S
tier: haiku
review: standard
files_touched: 2
labels: [server, protocol, serialization, adr-0002]
depends_on: [TASK-020201]
verify:
  - ./gradlew :poker-server:test --tests '*ClientMessageTest'
  - ./gradlew :poker-server:check
---

## Goal

Everything a client is allowed to say exists as one sealed, versioned hierarchy — a handshake and
an action attempt, and nothing that states a game fact.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/protocol/ClientMessage.kt` | create |
| `poker-server/src/test/kotlin/duels/poker/server/protocol/ClientMessageTest.kt` | create |

Read, do not modify: `poker-server/src/main/kotlin/duels/poker/server/protocol/Protocol.kt`,
`poker-engine/src/main/kotlin/duels/poker/engine/game/PlayerAction.kt`,
`docs/adr/ADR-0002-server-authoritative.md`.

## Scope

- `ClientMessage.kt`, package `duels.poker.server.protocol`: a `@Serializable public sealed
  interface ClientMessage` with exactly two nested members, each `@Serializable` with an explicit
  `@SerialName`:

  ```kotlin
  @Serializable
  @SerialName("Hello")
  public data class Hello(
      val deviceId: String? = null,
      val protocolVersion: Int = PROTOCOL_VERSION,
  ) : ClientMessage

  @Serializable
  @SerialName("Act")
  public data class Act(
      val handNumber: Int,
      val actionSequence: Int,
      val action: PlayerAction,
  ) : ClientMessage {
      init {
          require(handNumber >= 1) { "handNumber must be at least 1, was $handNumber" }
          require(actionSequence >= 0) { "actionSequence must be non-negative, was $actionSequence" }
      }
  }
  ```

- `deviceId` is nullable because a first-time client has none and the server mints it
  (`ADR-0012`, `STORY-0205`). A client naming a device id is claiming an identity, not a game fact,
  which is why this is the one string a client may send.
- `handNumber` and `actionSequence` are there for exactly the reason `ADR-0002` gives: they say
  *which decision point this answers*, so a replayed or out-of-order frame is detected and dropped.
  They are questions, not assertions — the server compares them, never adopts them.
- KDoc on the interface states the rule this hierarchy exists to enforce: a client message carries
  an **intent** and never an outcome — no card, no stack, no pot, no seat-to-act, no view. Note that
  `PlayerAction.Call` and `PlayerAction.AllIn` already carry no amount, so even the action cannot
  smuggle one, and that `TASK-020211` asserts this structurally rather than by review.
- The `init` blocks are deliberate: they make a malformed-but-parseable frame throw during decoding,
  which is what `TASK-020208`'s codec has to absorb.

## Out of scope

- Any room, lobby or rematch message — see `DEC-010` in `docs/adr/README.md`. `ProtocolError`
  already reserves the room error codes, and a sealed hierarchy takes new members additively.
- `ServerMessage` and `ProtocolError` — `TASK-020205`.
- The codec, the handshake function, and the structural tests — `TASK-020207`, `TASK-020208`,
  `TASK-020211`.

## Tests

`ClientMessageTest`, JUnit 5, package `duels.poker.server.protocol`. Encode through the parent
serializer with `protocolJson`, never through the subtype's own serializer — the discriminator only
appears through the parent.

| Test | Proves |
| --- | --- |
| `helloRoundTrips` | `Hello("device-1", PROTOCOL_VERSION)` encodes and decodes back to an equal value through `ClientMessage.serializer()` |
| `helloWithNoDeviceIdRoundTrips` | `Hello()` round-trips with `deviceId == null` |
| `helloCarriesItsVersionEvenWhenDefaulted` | the encoded string of `Hello()` contains `"protocolVersion":1` — the default reaches the wire |
| `actRoundTrips` | `Act(3, 7, PlayerAction.Raise(0, 300))` encodes and decodes back to an equal value |
| `theDiscriminatorsAreExplicit` | the encoded strings contain `"type":"Hello"` and `"type":"Act"` |
| `actRejectsAHandNumberBelowOne` | `assertThrows<IllegalArgumentException> { Act(0, 0, PlayerAction.Fold(0)) }` |
| `actRejectsANegativeActionSequence` | `assertThrows<IllegalArgumentException> { Act(1, -1, PlayerAction.Fold(0)) }` |

## Acceptance criteria

- [ ] `ClientMessageTest.helloRoundTrips` passes
- [ ] `ClientMessageTest.helloWithNoDeviceIdRoundTrips` passes
- [ ] `ClientMessageTest.helloCarriesItsVersionEvenWhenDefaulted` passes
- [ ] `ClientMessageTest.actRoundTrips` passes
- [ ] `ClientMessageTest.theDiscriminatorsAreExplicit` passes
- [ ] `ClientMessageTest.actRejectsAHandNumberBelowOne` passes
- [ ] `ClientMessageTest.actRejectsANegativeActionSequence` passes
- [ ] `ClientMessage` declares exactly two members, and no property of either has a type from
      `duels.poker.engine` other than `PlayerAction`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
