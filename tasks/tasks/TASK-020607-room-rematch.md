---
schema: 2
id: TASK-020607
title: Both seats must offer before a rematch starts, and the button changes sides
type: task
status: backlog
parent: STORY-0206
module: poker-server
estimate: S
tier: sonnet
review: standard
files_touched: 3
labels: [server, rooms, rematch]
depends_on: [TASK-020606]
verify:
  - ./gradlew :poker-server:test --tests '*RoomRematchTest'
  - ./gradlew :poker-server:check
---

## Goal

A finished room takes one rematch offer per seat; when both have offered it returns to `PLAYING`
with a fresh `MatchState` whose button sits on the seat that did not open the last duel, and one
offer alone changes nothing.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/room/RematchResult.kt` | create |
| `poker-server/src/main/kotlin/duels/poker/server/room/Room.kt` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/room/RoomRematchTest.kt` | create |

`RoomTest`, `RoomJoinTest` and `RoomLifecycleTest` assert construction, joining and the end
states. This ticket only adds a method, so none of them is touched and every assertion stands.

## Scope

- `RematchResult.kt`, package `duels.poker.server.room`, KDoc on everything public:

  ```kotlin
  public sealed interface RematchResult {
      public data class Offered(val room: Room) : RematchResult   // recorded, still FINISHED
      public data class Agreed(val room: Room) : RematchResult    // both in, now PLAYING
      public data class Refused(val reason: RematchRefusal) : RematchResult
  }

  public enum class RematchRefusal { UNKNOWN_ROOM, NOT_FINISHED, NOT_A_PLAYER, ALREADY_OFFERED }
  ```

  `UNKNOWN_ROOM` is declared here but never returned by `Room` — only the registry
  (`TASK-020611`) can fail to find a room. Declaring it now keeps that ticket from editing this enum.
- `Room.offerRematch(player: PlayerId, now: Long): RematchResult`, in this order:
  1. `state != FINISHED` → `Refused(NOT_FINISHED)`;
  2. `seatOf(player) == null` → `Refused(NOT_A_PLAYER)`;
  3. `player in rematchOffers` → `Refused(ALREADY_OFFERED)`, offers unchanged;
  4. otherwise record the offer. If the offers now cover **both** seated players → `Agreed`;
     else → `Offered` with `state` still `FINISHED`.
- Agreement produces: `state = PLAYING`, `openingButtonSeat = 1 - openingButtonSeat`,
  `match = MatchState.start(format, openingButtonSeat)` — fresh stacks, `handsPlayed == 0` —
  `rematchOffers = emptySet()`, `lastActivityAt = now`.
- The button alternates against **`openingButtonSeat`**, the seat that had the button on hand one
  of the duel just played — not against `match.buttonSeat`, which has alternated hand by hand and
  says nothing about who opened. Put that reason in the KDoc; it is the one subtle line in the file.
- Pure and total: never throws, never mutates, no coroutine, no clock.

## Out of scope

- Actually dealing the rematch's first hand, or telling anyone it started — `STORY-0207`. This
  ticket owns the agreement, not the deal.
- An offer expiring on its own timer: an offer dies with the room when it is reaped
  (`TASK-020612`), and there is no separate offer timeout in v0.1.
- Carrying stacks or coins across a rematch — a rematch is a fresh duel at `format.startingStack`.

## Tests

`RoomRematchTest`, JUnit 5, package `duels.poker.server.room`. Reach `FINISHED` with
`Room.open(...).join(guest, 1_000L)` unwrapped, then `.finish(2_000L)`.

| Test | Proves |
| --- | --- |
| `oneOfferLeavesTheRoomFinished` | host offers → `Offered`, `state == FINISHED`, `rematchOffers == setOf(host)`, `match` unchanged |
| `bothOffersReturnTheRoomToPlaying` | guest then offers → `Agreed`, `state == PLAYING`, `rematchOffers.isEmpty()`, `lastActivityAt == now` |
| `theRematchButtonSitsOnTheOtherSeat` | after agreement `openingButtonSeat == 1`, `match!!.buttonSeat == 1`, `match!!.handsPlayed == 0`, both stacks `== format.startingStack` |
| `aSecondRematchReturnsTheButtonToTheHost` | finish and agree again → `openingButtonSeat == 0` and `match!!.buttonSeat == 0` |
| `offeringTwiceFromOneSeatIsRefused` | host offers twice → `Refused(ALREADY_OFFERED)`, and the room still holds exactly one offer |
| `aStrangerCannotOfferARematch` | `PlayerId("nobody")` → `Refused(NOT_A_PLAYER)` |
| `aRematchOfferBeforeTheDuelEndsIsRefused` | on a `PLAYING` room and on a `WAITING` room → `Refused(NOT_FINISHED)` |

## Acceptance criteria

- [ ] `RoomRematchTest.oneOfferLeavesTheRoomFinished` passes
- [ ] `RoomRematchTest.bothOffersReturnTheRoomToPlaying` passes
- [ ] `RoomRematchTest.theRematchButtonSitsOnTheOtherSeat` passes
- [ ] `RoomRematchTest.aSecondRematchReturnsTheButtonToTheHost` passes
- [ ] `RoomRematchTest.offeringTwiceFromOneSeatIsRefused` passes
- [ ] `RoomRematchTest.aStrangerCannotOfferARematch` passes
- [ ] `RoomRematchTest.aRematchOfferBeforeTheDuelEndsIsRefused` passes
- [ ] `Room.offerRematch` contains no `throw`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
