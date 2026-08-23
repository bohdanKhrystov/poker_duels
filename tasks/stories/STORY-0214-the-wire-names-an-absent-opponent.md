---
id: STORY-0214
title: The wire names an absent opponent
type: story
status: ready
parent: EPIC-02
module: poker-server
labels: [server, protocol, rooms, presence]
depends_on: [STORY-0208]
---

## Goal

The player who is still there learns that their opponent has dropped, how long the server will wait,
when the waiting ended and when they came back — and every action the server took for an absent seat
is labelled as the server's. So `EPIC-03` has a wire to render the pause state against, and writes no
Kotlin.

## Why

`EPIC-02`'s scope line reads *"Sessions, the socket lifecycle, and the disconnect grace period of
`ADR-0013`"*. `STORY-0208` shipped that grace period as far as a room that knows exactly who is gone
— `Room.gracePeriods`, `Room.absentSeats`, `Room.isPaused`, `foldAbsent`, `DUEL_PAUSED` — and stopped
before telling anyone. A present player cannot tell a paused duel from a slow opponent, and a fold
the server submitted is byte-identical on the wire to a fold somebody chose.

[`ADR-0028`](../../docs/adr/ADR-0028-the-wire-names-an-absent-opponent.md) answered `DEC-018` — the
human's product call, made verbatim — and specified the whole server half two days after
`STORY-0208` had closed, so it never became a ticket.
[`ADR-0045`](../../docs/adr/ADR-0045-presence-belongs-to-the-table.md) answers `DEC-038` and files it
here: the unfinished half returns to the epic that promised it and to the module that owns the code,
rather than to `EPIC-03`, whose own rule is that a client needing a new frame raises a decision
instead of editing Kotlin.

`STORY-0208` is **not** reopened. Its fifteen tickets and its acceptance criteria are what
`EPIC-02`'s first close was measured against, and this is a sibling rather than an edit to a settled
record.

## Design notes

[`ADR-0028`](../../docs/adr/ADR-0028-the-wire-names-an-absent-opponent.md) §§1–5 are the
specification; nothing here goes beyond it, and `ADR-0045` adds no type, field or emission point.

- **Two `ServerMessage` subtypes and one enum**, generated like every other wire type, in
  `duels.poker.server.protocol`. `SeatPresence { PRESENT, AWAY, ABSENT }`;
  `OpponentPresence(presence, graceRemainingMillis: Long?)` with an `init` pinning the nullable field
  present **exactly when** `AWAY` and never negative; `ActedForAbsent(seat, handNumber,
  actionSequence, action)` with an `init` that refuses any action but `FOLD` or `CHECK`, pinning
  `ADR-0023` at the wire boundary.
- **`OpponentPresence` carries no seat.** It is addressed to one recipient and its whole content is
  relative to them, like `YourTurn`. `ActedForAbsent` is the opposite: a fact about the shared
  log, delivered identically to both seats, so it names the seat it is about.
- **The countdown is a remaining duration, sent once** — not a deadline and not a tick. The two sides
  share no epoch: `ServerClock` is monotonic from an arbitrary origin on purpose, and wall clock is
  what it exists to keep out of timeout code. Computed in `RoomRegistry`, inside the same `mutate`
  critical section that reads the deadline, and nowhere else. Clamped at zero; `AWAY` with zero
  remaining is legal and means the window ran out before the sweep landed.
- **`Room` gains one pure function**, `presenceOf(seat: Int, now: Long): ServerMessage.OpponentPresence`,
  derived from `gracePeriods`, `absentSeats` and the `now` it is handed. **No new field is stored
  anywhere** — presence is a projection of state the room already keeps. `Room` still reads no clock.
- **Five emission points**, `ADR-0028` §5's table: `disconnect` → `AWAY` with the configured window;
  `expireGracePeriods` → `ABSENT`; `resume` → `PRESENT` to the seat that stayed, and
  `presenceOf(otherSeat, now)` to the returning seat always; `foldAbsent` → the mark, to both seats.
  A frame is produced **only when there is another seated player to receive it**.
- **Two plumbing changes, both easy to get wrong.** `RoomRegistry.disconnect` must return frames — it
  returns `Room?` today — shaped like `Resumption` and `GraceExpiry`, and its one call site,
  `DuelSocket`'s `finally` block, delivers them **inside the existing `withContext(NonCancellable)`**.
  And `Resumption.outbound` stops being single-seat: its KDoc says *"never another seat's"* and that
  contract becomes the one `JoinResult.Seated` already has. `deliver` already routes by seat, so no
  call site changes.
- **The mark is emitted in `foldAbsent` and nowhere else**, for every action it submits — the check
  as well as the fold — and **only if the runner actually moved**. It precedes the frames the action
  produced.
- **`PROTOCOL_VERSION` moves one step, taking the number `develop` says plus one at the moment the
  bump is made** — `ADR-0028` §8, with `ADR-0045` §4's mechanics. No number is named in this story:
  a number written in advance is stale the moment another bump lands. **`STORY-0213` is in front of
  this story in the version queue** (`ADR-0045` §3), and at most one protocol-bumping branch may be
  open at a time — two branches both moving 2 → 3 merge without a conflict and every gate stays
  green.
- **`docs/protocol.md` moves in the same change as the Kotlin, never before it.**
  `ProtocolDocumentationTest` fails the build on a documented message that does not exist and asserts
  the documented version equals `PROTOCOL_VERSION`.
- **The generated TypeScript is regenerated, never hand-edited**: `./gradlew
  :poker-server:generateProtocolTypes`, byte-checked by `verifyProtocolTypes` on every `check`
  (`ADR-0020`).
- **A debt this story pays.** `ADR-0028` retracted the wire half of `ADR-0023`'s indistinguishability
  property. `TASK-020806` shipped a test asserting it and `foldAbsent`'s KDoc says the function
  *"constructs no game state, event, or frame of its own"*. Both are green and correct as written,
  and both must become false in this change. That is a deliberate reversal, not a regression — say so
  in the commit.
- **Nothing else changes.** `poker-engine` gains nothing: no clock, no absence, no networking, no
  event, no `EVENT_SCHEMA_VERSION` bump. `DUEL_PAUSED` keeps its meaning and wording. `deliver` keeps
  its one job. No new room state and no new timer — `expireGracePeriods` rides `ADR-0025`'s existing
  ticker.

## Tasks

| ID | Title | Status |
| --- | --- | --- |
| [TASK-021401](../tasks/TASK-021401-disconnect-answers-with-a-room-and-its-frames.md) | `RoomRegistry.disconnect` answers with a room and the frames it produced | **ready** |
| [TASK-021402](../tasks/TASK-021402-the-wire-names-presence-and-the-version-takes-its-step.md) | `OpponentPresence` and `ActedForAbsent` reach the wire, and `PROTOCOL_VERSION` takes its step | **ready** |
| [TASK-021403](../tasks/TASK-021403-room-presence-of-projects-the-three-states.md) | `Room.presenceOf` projects a seat's presence from state the room already keeps | backlog |
| [TASK-021404](../tasks/TASK-021404-a-drop-builds-the-away-frame-for-the-other-seat.md) | A drop builds `AWAY` and the configured window, for the other seat only | backlog |
| [TASK-021405](../tasks/TASK-021405-the-away-frame-reaches-the-opponents-socket.md) | The `AWAY` frame reaches the opponent's socket, from inside the `NonCancellable` block | backlog |
| [TASK-021406](../tasks/TASK-021406-an-act-after-the-countdown-would-have-hit-zero.md) | An `Act` sent after the client's countdown would have reached zero is still refused | backlog |
| [TASK-021407](../tasks/TASK-021407-expiry-says-absent-before-the-fold-it-explains.md) | Expiry says `ABSENT` before the fold it explains, and an abandoned room says nothing | backlog |
| [TASK-021408](../tasks/TASK-021408-fold-absent-marks-every-action-it-takes.md) | `foldAbsent` marks every action it takes for an absent seat, to both seats | backlog |
| [TASK-021409](../tasks/TASK-021409-a-checked-down-absent-seat-is-marked-as-a-check.md) | A checked-down absent seat is marked as a check, where a fold is not legal | backlog |
| [TASK-021410](../tasks/TASK-021410-a-resume-tells-both-sides-where-they-stand.md) | A resume tells the returning seat where its opponent stands, and the seat that stayed only if it changed | backlog |
| [TASK-021411](../tasks/TASK-021411-the-other-seat-drops-and-every-frame-names-the-mirror.md) | The host is the seat that goes, and every presence frame names the mirror image | backlog |

The chain is linear: every ticket depends on the one before it, and several share a test file, so no
two could overlap anyway.

**`TASK-021401` is `ready` and starts the story.** It is the only ticket that needs none of
`ADR-0028`'s new types: `Disconnection(room, outbound)` is built from `Room` and `Addressed`, which
both exist. Putting it in front meant the story moved while `DEC-066` was answered.

**`TASK-021402` is unblocked and `ready`, `TASK-021401` having landed.** The wire step
was sized by the [`ADR-0070`](../../docs/adr/ADR-0070-a-blast-radius-is-complete-only-when-the-gates-are-green.md)
§2 probe — a throwaway stub of every declaration this story adds, run through the commands
`.github/workflows/build.yml` runs, iterated until the whole gate set exited **0**, then reverted.
It is **thirteen** files, not `TASK-021301`'s seventeen: this story adds no `ProtocolError` value and
no `ClientMessage` variant, so `ServerMessageHandshakeTest`'s golden error list,
`TypeScriptDeclarationsTest`'s golden `ClientMessage` union and `connection.test.ts` are all
untouched. Seventeen was a fact about that ticket, never about protocol bumps.

The probe also found what no reading had: `ProtocolDiscriminatorTest.everyDiscriminatorIsShortAndUnique`
has failed the build on any discriminator over 16 characters since `TASK-020210`, and `ADR-0028` §1
specified `@SerialName("ActedForAbsentSeat")` — **eighteen**. `OpponentPresence` is exactly sixteen
and passes. Three edits satisfied that gate and they were not equivalent, so it was `DEC-066` and
the architect's, not a propagation.
[`ADR-0071`](../../docs/adr/ADR-0071-a-discriminator-is-its-kotlin-type-name.md) answers it: **the
type is renamed `ActedForAbsent`**, Kotlin name and `@SerialName` together and equal to each other,
and the 16-character gate is left unedited. The count stays **thirteen** — the new name is fourteen
characters, so `ProtocolDiscriminatorTest.kt` is not a fourteenth row. Every ticket below writes
`ActedForAbsent`.

## Acceptance criteria

- [ ] A disconnect puts exactly one `OpponentPresence(AWAY, …)` on the opponent's socket carrying the
      configured window, and none on any other socket; changing `disconnectGraceMillis` changes the
      number with no code change.
- [ ] Expiry puts `ABSENT` on the present seat's socket **before** any frame the resulting fold or
      check produced.
- [ ] **An `Act` sent after a client's countdown would have reached zero, but before the sweep has
      expired the window, is still refused with `DUEL_PAUSED` and moves nothing.** The
      server-authoritative rule made executable.
- [ ] A checked-down absent seat produces `ActedForAbsent` with `action = CHECK`, on virtual
      time, in a spot where `FOLD` is not legal.
- [ ] A submission that makes no progress produces no mark.
- [ ] A returning player receives the opponent's current presence and no replayed `Events`; the seat
      that stayed receives `PRESENT` only when the returning seat had actually been away.
- [ ] Both seats expiring abandons the room and sends nothing.
- [ ] No frame introduced here carries a card — the existing `ProtocolPayloadTest` descriptor walk
      covers both new types the day they are added.
- [ ] No test asserts a frame delivered to a seat with no writer, and no test sleeps on a real clock.
- [ ] `./gradlew check` passes with `PROTOCOL_VERSION`, `docs/protocol.md` and
      `web-client/src/protocol/protocol.gen.ts` all moved in the same change.

## Out of scope

- **Any rendering.** What a player sees is `STORY-0313` in `EPIC-03`; the words they read are settled
  by [`ADR-0046`](../../docs/adr/ADR-0046-the-table-says-away-timed-out-and-back.md) (`DEC-039`), and
  no string of it belongs in a Kotlin file. Nothing here changes because of that ADR — a frame's job
  is to carry the fact, not the sentence.
- **A per-seat journal of what the server did while a player was away.** `ADR-0028` §6 declines it
  explicitly: a returning player is told the state they come back to and no more. Strictly addable
  later.
- **Provenance in the event log.** Absence is a fact about the server; `poker-engine` learns nothing
  about it, and a replay reconstructed from the log still cannot say which actions were the
  server's.
- **A per-second countdown frame**, an absolute wall-clock deadline, and any `ClientMessage` field
  carrying a timestamp or a remaining duration. All three are rejected in `ADR-0028`, each with its
  reason.
- Spectators. `ADR-0040` settles what one may see; no epic owns building one, and
  `OpponentPresence` is recipient-relative so it has no meaning for a party with no opponent.
