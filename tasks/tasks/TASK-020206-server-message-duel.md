---
schema: 2
id: TASK-020206
title: The four duel ServerMessages — a view, events, the turn, a rejection
type: task
status: backlog
parent: STORY-0202
module: poker-server
estimate: S
tier: haiku
review: standard
files_touched: 3
labels: [server, protocol, serialization, adr-0002]
depends_on: [TASK-020205, TASK-020202, TASK-020203]
verify:
  - ./gradlew :poker-server:test --tests '*ServerMessageDuelTest'
  - ./gradlew :poker-server:check
---

## Goal

The server can describe a live duel to one player: the state it is entitled to see, the facts that
just happened, its turn with its legal actions, and why an attempt was refused — all without a
`GameState` existing anywhere in the hierarchy.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/protocol/ServerMessage.kt` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/protocol/ProtocolSamples.kt` | create |
| `poker-server/src/test/kotlin/duels/poker/server/protocol/ServerMessageDuelTest.kt` | create |

Read, do not modify: `poker-engine/src/main/kotlin/duels/poker/engine/game/PlayerView.kt`,
`poker-engine/src/main/kotlin/duels/poker/engine/game/SeatView.kt`,
`poker-engine/src/main/kotlin/duels/poker/engine/game/LegalActions.kt`,
`poker-engine/src/main/kotlin/duels/poker/engine/game/Rejection.kt`.

## Scope

- Four more members on the existing `ServerMessage` interface, each `@Serializable` with an explicit
  `@SerialName`, appended below `Failure`:

  ```kotlin
  @Serializable
  @SerialName("Snapshot")
  public data class Snapshot(val view: PlayerView) : ServerMessage

  @Serializable
  @SerialName("Events")
  public data class Events(val events: List<GameEvent>) : ServerMessage

  @Serializable
  @SerialName("YourTurn")
  public data class YourTurn(
      val handNumber: Int,
      val actionSequence: Int,
      val legalActions: LegalActions,
  ) : ServerMessage

  @Serializable
  @SerialName("Rejected")
  public data class Rejected(val rejection: Rejection) : ServerMessage
  ```

- `Snapshot` is the **only** state-carrying message and its only field is a `PlayerView`. One
  sentence of KDoc saying why: the redaction lives in the engine's projection layer
  (`STORY-0204`), so transport has nothing left to filter and no way to widen what is sent.
- `Events` carries whatever `visibleTo(events, seat)` returned for that recipient — this type does
  no filtering of its own and its KDoc says so.
- `YourTurn` repeats `handNumber` and `actionSequence` because they are exactly what a
  `ClientMessage.Act` must echo back (`ADR-0002`). It does not repeat the view.
- `Rejected` wraps the engine's own `Rejection`. The protocol invents no second vocabulary for
  "that action was illegal".
- `ProtocolSamples.kt` (test sources, package `duels.poker.server.protocol`) holds two `internal`
  builders reused by later tickets, and nothing else:
  - `sampleView()` — a `PlayerView` with `viewerSeat = 0`, `handNumber = 3`, `buttonSeat = 0`,
    `street = Street.FLOP`, `board = Board(cards "2c 7d 9s")`, `pot = 300`, `betToMatch = 0`,
    `minRaiseTo = 100`, `seatToAct = 0`, `smallBlind = 50`, `bigBlind = 100`, seat 0 holding
    `As Kh` and seat 1 holding no cards, both with `stack = 900`, `committedThisStreet = 0`,
    `committedThisHand = 100`. Build the cards with `Card.parse`.
  - `sampleLegalActions()` — `LegalActions(0, setOf(ActionType.CHECK, ActionType.BET),
    minBetTo = 100, allInTo = 900)`.
- `ServerMessageHandshakeTest` is **not** in this ticket's budget and must not be edited: it asserts
  only that `Welcome` and `Failure` round-trip, which adding members cannot change, and
  `TASK-020205` forbade it from pinning the member count for exactly this reason.

## Out of scope

- Producing any of these messages from a real duel — `STORY-0207`.
- The descriptor-level assertions that the hierarchy is complete and carries no seed —
  `TASK-020210` and `TASK-020211`.
- Adding a room or lobby message; see `DEC-010`.

## Tests

`ServerMessageDuelTest`, JUnit 5, package `duels.poker.server.protocol`, encoding through
`ServerMessage.serializer()` with `protocolJson`.

| Test | Proves |
| --- | --- |
| `snapshotRoundTrips` | `Snapshot(sampleView())` encodes and decodes back to an equal value |
| `eventsRoundTrip` | `Events(listOf(ActionOn(3, 0), PlayerBet(4, 0, 300)))` encodes and decodes back to an equal value |
| `yourTurnRoundTrips` | `YourTurn(3, 7, sampleLegalActions())` encodes and decodes back to an equal value |
| `rejectedRoundTrips` | `Rejected(Rejection.NotYourTurn(1))` encodes and decodes back to an equal value |
| `theDiscriminatorsAreExplicit` | the four encoded strings contain `"type":"Snapshot"`, `"type":"Events"`, `"type":"YourTurn"` and `"type":"Rejected"` |
| `aSnapshotShowsOnlyTheViewersCards` | the encoded `Snapshot(sampleView())` contains `As` and `Kh` and does not contain `Qd` — build the sample's seat 1 with no hole cards, so a leak could only come from a later change to `PlayerView` |
| `yourTurnCarriesItsZeroAmounts` | the encoded `YourTurn(3, 7, sampleLegalActions())` contains `"minRaiseTo":0` — `protocolJson` writes defaults |

## Acceptance criteria

- [ ] `ServerMessageDuelTest.snapshotRoundTrips` passes
- [ ] `ServerMessageDuelTest.eventsRoundTrip` passes
- [ ] `ServerMessageDuelTest.yourTurnRoundTrips` passes
- [ ] `ServerMessageDuelTest.rejectedRoundTrips` passes
- [ ] `ServerMessageDuelTest.theDiscriminatorsAreExplicit` passes
- [ ] `ServerMessageDuelTest.aSnapshotShowsOnlyTheViewersCards` passes
- [ ] `ServerMessageDuelTest.yourTurnCarriesItsZeroAmounts` passes
- [ ] `ServerMessageHandshakeTest` is not modified and still passes
- [ ] `ServerMessage` has exactly six members after this ticket
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
