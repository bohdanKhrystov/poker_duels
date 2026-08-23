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
| [TASK-021301](../tasks/TASK-021301-the-wire-gains-a-rematch-and-the-version-takes-its-step.md) | `OfferRematch` and `RematchOffered` reach the wire, and `PROTOCOL_VERSION` takes its step | **ready** |
| [TASK-021302](../tasks/TASK-021302-one-offer-reaches-both-seats-and-starts-no-duel.md) | One seat's offer puts `RematchOffered` on both sockets and starts no duel | backlog |
| [TASK-021303](../tasks/TASK-021303-the-second-offer-starts-the-duel-with-the-button-moved.md) | The second offer starts a fresh duel, with the button on the other seat | backlog |
| [TASK-021304](../tasks/TASK-021304-a-repeat-offer-is-answered-and-records-nothing.md) | A repeat offer is answered, not refused, and records nothing new | backlog |
| [TASK-021305](../tasks/TASK-021305-three-ways-to-hold-no-seat-answer-one-frame.md) | Three ways to hold no seat answer one indistinguishable `UNKNOWN_ROOM` | backlog |
| [TASK-021306](../tasks/TASK-021306-rematch-unavailable-is-transient-and-provably-so.md) | `REMATCH_UNAVAILABLE` is transient, and the same offer succeeds afterwards | backlog |
| [TASK-021307](../tasks/TASK-021307-a-standing-offer-survives-a-reconnect.md) | A standing offer is restated to a returning socket, after its `DuelFinished` | backlog |

The chain is linear: every ticket after `TASK-021301` depends on the one before it, and all but
`TASK-021302`–`TASK-021306` — which share one test file — could not overlap anyway.

**`TASK-021301` is `ready`, and everything in this story queues behind it.** The
split found that the wire step is irreducibly **twelve** files: the three type files, the constant,
`DuelSocket`'s two exhaustive `when`s, `ProtocolJsonTest`'s literal, `docs/protocol.md`,
`docs/protocol-versions.md`, both generated client artifacts, `version.ts` and `frames.ts`. Every one
is forced by a gate that is already merged, and no two of them can land in different commits: a wire
shape with no ledger row fails `ProtocolVersionLedgerTest`, a documented message with no type fails
`ProtocolDocumentationTest`, and a type with no row fails it the other way. A schema-2 ticket was
capped at three files, which is what `DEC-063` asked about.
[`ADR-0068`](../../docs/adr/ADR-0068-an-atomic-ticket-names-the-gate-that-forbids-splitting-it.md)
answers it: **the gates do not move** — a budgeting rule never rewrites a correctness gate — and
instead `files_touched` becomes a true count, with a ticket held together by a merged gate naming
those gates in a new `atomic:` key.

**It was fifteen, and that was `DEC-064`.** An implementation attempt got all twelve right and then
found three more the change forces — `SocketDuel.kt` and `SocketSecrecyTest.kt`, exhaustive `when`s
over `ServerMessage` in **test** sources, and `connection.test.ts`, whose fixtures run through the
client's own version comparison. It stopped and raised a decision rather than growing the ticket.
[`ADR-0069`](../../docs/adr/ADR-0069-the-blast-radius-is-probed-not-remembered.md) answers it: the
twelve-file ceiling is **deleted** rather than raised, `files_touched` must equal the ticket's own
*Files* table, and **a file the table does not name stops the ticket at any count**. The ticket
carries `files_touched: 15`.

`STORY-0214` and `STORY-0405` are behind this story in the same queue and are written the same way,
each **probing** its own set per `ADR-0069` §3 — a throwaway `PROTOCOL_VERSION + 1` and a throwaway
sealed variant, run through `:poker-server:check` and `npm run check`, then reverted — rather than
reusing this story's number, which is a fact about one ticket and not about protocol bumps.

Two of the twelve are files `ADR-0044` §9 did not foresee, and both are mechanical:
`web-client/src/protocol/frames.ts` holds a `satisfies Record<ServerMessage["type"], true>` table
that stops compiling until the new discriminator is added — `docs/protocol.md` already documents
that edit — and `web-client/src/e2e/scripted-duel.gen.json` embeds a `Welcome`'s `protocolVersion`
and is byte-checked by `:poker-server:verifyDuelScript` on every `check`.

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
