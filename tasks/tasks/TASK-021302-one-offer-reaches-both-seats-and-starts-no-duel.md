---
schema: 2
id: TASK-021302
title: One seat's offer puts RematchOffered on both sockets and starts no duel
type: task
status: ready
parent: STORY-0213
module: poker-server
estimate: S
tier: sonnet
review: standard
labels: [server, protocol, rooms, tests]
files_touched: 1
depends_on: [TASK-021301]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.DuelSocketRematchTest'
  - ./gradlew :poker-server:ktlintCheck
---

## Goal

`DuelSocketRematchTest` exists, drives two real sockets through a duel to its finish, and proves
`ADR-0044` §2's first half over the wire: one `OfferRematch` puts exactly one
`RematchOffered` naming the offering seat on **both** sockets, and starts nothing.

This ticket also lays the fixture `TASK-021303`–`TASK-021306` all reuse, which is most of its
diff — roughly 90 of about 140 lines. Those four tickets add a test each and no fixture.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/test/kotlin/duels/poker/server/DuelSocketRematchTest.kt` | create |

Read, not edited — the fixture below is mirrored from these two, and both show every helper it
needs already written:

- `poker-server/src/test/kotlin/duels/poker/server/DuelSocketDuelTest.kt` — `nextServerMessage`,
  `drainServerMessages`, `completeHandshake`, `enterRoom`, and `openADuel`'s trick of pre-creating a
  room on the registry so the test picks the format
- `poker-server/src/test/kotlin/duels/poker/server/DuelSocketReconnectTest.kt` —
  `aReconnectAfterTheDuelFinishedGetsTheFinishedState`, which is the recipe for finishing a duel over
  two sockets in four lines

## Scope

- Create `DuelSocketRematchTest` in package `duels.poker.server`, with its own **private** file-level
  helpers. Every socket test file in this package already keeps its own copies rather than sharing
  them; follow that, and do not refactor the existing files to extract a common fixture — that would
  put three files outside this ticket's budget for no test gained.
- The fixture is one function, `finishedDuel`, that returns two live sockets sitting on a room in
  `RoomState.FINISHED`:
  1. `DuelFormat.DEFAULT.copy(endCondition = EndCondition.FixedHands(1))`, because `CreateRoom`
     always opens `DuelFormat.DEFAULT` and a test cannot ask for a one-hand duel over the wire.
  2. `rooms.create(host.id, format)`, then `"host"` and `"guest"` join by code over `/ws`.
  3. The host folds hand 1's only decision — `FixedHands(1)` ends the duel there.
  4. Both sockets are drained, so a later read on either sees only what happens after the fixture.
- Seeds are fixed (`HandSeedSource { 7L }`), so every run deals the same hand.
- The offer is sent as `ProtocolCodec.encode(OfferRematch)` — a `data object`, so there is nothing
  to construct and no room code to pass.

## Out of scope

- The agreeing second offer and the flipped button — `TASK-021303`.
- A repeat offer from the same seat — `TASK-021304`.
- Any refusal — `TASK-021305` and `TASK-021306`.
- A reconnect — `TASK-021307`.
- Touching any existing test file. If a helper here duplicates one there, that is deliberate.

## Tests

`DuelSocketRematchTest`

| Test | Proves |
| --- | --- |
| `oneOfferPutsOneRematchOfferedNamingThatSeatOnBothSockets` | after the host's single `OfferRematch`, each socket has received **exactly one** `RematchOffered`, both carry `seat == 0` — the offering seat, not the recipient's — and neither socket received a `Snapshot`, an `Events` or a `YourTurn`, so no duel started |

Assert the count on each socket separately, not across both drains combined: a single frame
delivered twice to one socket and never to the other would satisfy a total of two.

## Acceptance criteria

- [ ] `DuelSocketRematchTest.oneOfferPutsOneRematchOfferedNamingThatSeatOnBothSockets` passes
- [ ] Both drained frame lists contain exactly one `ServerMessage.RematchOffered`, and both name
      `seat = 0`
- [ ] Neither drained list contains a `Snapshot`, an `Events` or a `YourTurn`
- [ ] No file outside `DuelSocketRematchTest.kt` changes
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
