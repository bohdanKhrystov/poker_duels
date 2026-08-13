---
schema: 2
id: TASK-020720
title: ServerMessage.DuelFinished carries the duel's outcome
type: task
status: backlog
parent: STORY-0207
module: poker-server
estimate: XS
tier: haiku
review: standard
files_touched: 3
labels: [server, protocol, duel]
depends_on: [TASK-020719]
verify:
  - ./gradlew :poker-server:test --tests '*ProtocolDocumentationTest'
  - ./gradlew :poker-server:test --tests '*ProtocolPayloadTest'
  - ./gradlew :poker-server:test --tests '*ProtocolDiscriminatorTest'
  - ./gradlew :poker-server:check
---

## Goal

`ServerMessage` has a `DuelFinished` variant carrying `DuelOutcome`, so that the server can state
that a duel is over instead of leaving the client to infer it from two stacks (`ADR-0017`,
resolving `DEC-015`).

This ticket puts the message on the wire and nothing else: nobody builds one yet.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/protocol/ServerMessage.kt` | modify |
| `poker-server/src/main/kotlin/duels/poker/server/DuelSocket.kt` | modify |
| `docs/protocol.md` | modify |

`DuelSocket.kt` is in the budget because `serve()` closes an exhaustive `when` over `ServerMessage`
around `handshake()`'s result, listing every variant a handshake may not return. That `when` stops
compiling the moment a variant is added — which is exactly the compiler-enforced completeness
`ADR-0017` counted on. The change there is one line.

## Scope

- Add to `ServerMessage`:

  ```kotlin
  @Serializable
  @SerialName("DuelFinished")
  public data class DuelFinished(val outcome: DuelOutcome) : ServerMessage
  ```

  `DuelOutcome` is `duels.poker.engine.duel.DuelOutcome` and is already `@Serializable`.
- KDoc it with the two facts that make it a *projection* rather than a `MatchEvent` on the wire
  (`ADR-0017`): it carries the outcome the recipient is entitled to see, and it deliberately does
  **not** carry `MatchFinished` or its sequence number, because `ADR-0009` gave match events their
  own sequence space and putting it on the wire would blur that boundary.
- Add `is ServerMessage.DuelFinished,` to the list of variants in `DuelSocket.serve()`'s
  `error("handshake() returned $message; …")` branch. Nothing else in `DuelSocket.kt` changes.
- Add one row to `docs/protocol.md`'s message table:
  `` | `DuelFinished` | server → client | `outcome` (DuelOutcome) | The duel has ended | ``.

## Out of scope

- Building the frame — `TASK-020721` adds the projection helper in `duel/Addressed.kt`.
- Emitting it when a duel ends — `TASK-020722`.
- Sending it down a socket — `TASK-020715`.

## Tests

No new test file. Every check this addition must survive is descriptor-driven or document-driven
and already exists; each is named below and each covers the new variant automatically.

`ProtocolDocumentationTest`

| Test | Proves |
| --- | --- |
| `everyServerMessageHasARowSayingServerToClient` | the new variant has its documentation row |
| `theDocumentNamesNoMessageThatDoesNotExist` | the row names a message that really exists |

`ProtocolPayloadTest`

| Test | Proves |
| --- | --- |
| `theOnlyStateAServerMessageCarriesIsAPlayerView` | `Snapshot` is still the only state carrier — `DuelFinished` carries an outcome, not a view |
| `noServerMessageNamesADeckAnRngOrASeed` | `winner`, `handsPlayed` and `finalStacks` are none of those |
| `noServerMessageCarriesAStateDeckOrRngType` | `DuelOutcome` reaches no `GameState`, `Deck` or `Rng` |

`ProtocolDiscriminatorTest`

| Test | Proves |
| --- | --- |
| `noDiscriminatorIsAFullyQualifiedClassName` | `@SerialName("DuelFinished")` is explicit |
| `everyDiscriminatorIsShortAndUnique` | `DuelFinished` is 12 characters and collides with nothing |

## Acceptance criteria

- [ ] `ProtocolDocumentationTest.everyServerMessageHasARowSayingServerToClient` passes
- [ ] `ProtocolDocumentationTest.theDocumentNamesNoMessageThatDoesNotExist` passes
- [ ] `ProtocolPayloadTest.theOnlyStateAServerMessageCarriesIsAPlayerView` passes
- [ ] `ProtocolPayloadTest.noServerMessageCarriesAStateDeckOrRngType` passes
- [ ] `ProtocolDiscriminatorTest.everyDiscriminatorIsShortAndUnique` passes
- [ ] No test file appears in the diff
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
