---
id: STORY-0213
title: The wire carries a rematch
type: story
status: ready
parent: EPIC-02
module: poker-server
labels: [server, protocol, rooms]
depends_on: [STORY-0206, STORY-0207]
---

## Goal

A seated player can offer a rematch over the socket, both seats learn that an offer stands, and the
second offer starts the next duel with the frames the server already builds — so `EPIC-03` has a
wire to render against and writes no Kotlin.

## Why

`EPIC-02`'s scope line reads *"Rooms: create, join by code, seat exactly two, **rematch**"*. It
shipped the rematch as far as `RoomRegistry.offerRematch` — one offer per seat, agreement, a flipped
button, a fresh runner, and (since `TASK-020733`) the opening frames handed back — and stopped one
wire message short of anyone being able to reach it. `docs/vision.md`'s success condition ends on
*"We hit Rematch."*

This story is the unfinished half returning to the epic that promised it.
[`ADR-0044`](../../docs/adr/ADR-0044-a-rematch-is-one-intent-and-one-room-fact.md) answers
`DEC-023` and reopens `EPIC-02` for exactly this, rather than letting `EPIC-03` edit `poker-server`
against its own out-of-scope rule.

## Design notes

`ADR-0044` is the specification; nothing here goes beyond it.

- **`ClientMessage.OfferRematch` is a `data object`**, like `CreateRoom`. It names no room, no seat
  and no duel: the socket's `RoomMembership.code` names the room and the session names the player.
- **`ServerMessage.RematchOffered(seat: Int)`**, `require(seat in 0..1)`, means *an offer from this
  seat stands*. It goes to **both** seats through `deliver`. It carries no card, no `PlayerView` and
  nothing derived from engine state, so it is built in transport from the `Room` the registry
  returned — where `RoomJoined` is built — and not in the projection layer.
- **A repeat offer is not an error.** `RematchRefusal.ALREADY_OFFERED` is answered with the same
  `RematchOffered(seat)`, rebuilt from `Room.rematchOffers` on a room read fresh from the registry.
  `replyToJoinRoom`'s `ALREADY_SEATED` branch is the precedent.
- **There is no started frame.** `RematchResult.Agreed.outbound` is delivered exactly as
  `JoinResult.Seated.outbound` is; the opening `Snapshot` — with the button already on the other
  seat — is the start.
- **A standing offer is restated on resume.** When `RoomRegistry.resume` answers for a `FINISHED`
  room, one `RematchOffered` per player in `Room.rematchOffers` follows `RoomJoined` **and the
  resumed frames**, in that order. `ADR-0044` §5 says why the order is load-bearing.
- **Refusals:** `UNKNOWN_ROOM`, `NOT_A_PLAYER` and a socket that entered no room all answer
  `Failure(UNKNOWN_ROOM)`, indistinguishably. `NOT_FINISHED` answers `Failure(REMATCH_UNAVAILABLE)`
  — a new `ProtocolError` value, documented as transient: nothing was recorded, the offer may be
  sent again. It covers `RoomRegistry`'s `recording` guard as well as a duel still running.
- **`PROTOCOL_VERSION` moves one step, taking the next number free when this lands** — `ADR-0028`
  §8. `ADR-0027`'s and `ADR-0028`'s bumps are unlanded and claim the same rule; this is not
  automatically 3. `TASK-020719` is the worked precedent for the bump itself.
- **`docs/protocol.md` moves in the same change as the Kotlin, never before it.**
  `ProtocolDocumentationTest` fails the build on a documented message that does not exist, and
  asserts the documented version equals `PROTOCOL_VERSION`. The document gains two rows, the
  `REMATCH_UNAVAILABLE` bullet, the new version line, and the rule that **after a `DuelFinished`, a
  `Snapshot` means the rematch has begun** — true because `resumeFrames` gives a finished duel
  `finishedFrames` alone.
- **The generated TypeScript is regenerated, never hand-edited**: `./gradlew
  :poker-server:generateProtocolTypes`, byte-checked by `verifyProtocolTypes` on every `check`
  (`ADR-0020`).
- **Nothing else changes.** `poker-engine`, `Room`, `RoomRegistry`, `RematchResult`,
  `RematchRefusal`, `RoomTimeouts` and `SeatDelivery` are untouched. No new room state, no timer, no
  persistence.

## Tasks

| ID | Title | Status |
| --- | --- | --- |
| — | *Not yet split. Run `/plan-story STORY-0213`.* | — |

## Acceptance criteria

- [ ] One seat's `OfferRematch` puts exactly one `RematchOffered` naming that seat on **both**
      sockets, and starts no duel.
- [ ] The other seat's `OfferRematch` starts a fresh duel: both sockets receive its opening frames,
      and the `Snapshot` puts the button on the other seat.
- [ ] A second `OfferRematch` from a seat that has already offered answers `RematchOffered` for that
      seat and records nothing new — proven by the opponent's single offer still being the one that
      starts the duel.
- [ ] An `OfferRematch` from a socket in no room, from a player holding no seat, and for a reaped
      room all answer `Failure(UNKNOWN_ROOM)` and cannot be told apart.
- [ ] An `OfferRematch` while the duel is still running answers `Failure(REMATCH_UNAVAILABLE)`,
      changes nothing, and the same offer succeeds once the duel has finished.
- [ ] A seat that offers while the opponent is disconnected, followed by that opponent rejoining
      with `JoinRoom`, puts `RematchOffered` on the returning socket **after** its `DuelFinished`.
- [ ] `./gradlew check` passes with `docs/protocol.md`, `PROTOCOL_VERSION` and
      `web-client/src/protocol/protocol.gen.ts` all moved in the same change.

## Out of scope

- **Any rendering.** What a player sees is `STORY-0309` in `EPIC-03`.
- Withdrawing an offer, declining one explicitly, and any deadline or countdown on the wire —
  `ADR-0044` rejects all three, each with its reason.
- A rematch that outlives the room, or an invitation to anyone who was not seated.
- `ADR-0028`'s server half (`OpponentPresence`, `ActedForAbsentSeat`). It is homeless in the same
  way and `ADR-0044` says so, but filing it here would be widening this story.
