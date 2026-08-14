---
id: STORY-0305
title: The lobby — create a room, join by code, share the link
type: story
status: backlog
parent: EPIC-03
module: web-client
labels: [client, lobby, rooms]
depends_on: [STORY-0302, STORY-0304]
---

## Goal

The first screen. Create a room and show its code with a copyable link; join by pasting a code or by
opening a link; wait for the opponent; move to the table when the server says the duel has begun.

## Why

This is the half of `docs/vision.md`'s success condition that happens before any poker: *send a
link, she opens it in a browser*. The server has spoken room codes since `EPIC-02`; nobody can reach
one.

## Design notes

- `CreateRoom` answers `RoomJoined{code, seat: 0}` — the host always holds seat 0. `JoinRoom{code}`
  answers `RoomJoined{code, seat}`, or `Failure{UNKNOWN_ROOM}` or `Failure{ROOM_FULL}`.
- **There is no "opponent joined" message, and this story must not ask for one.** Seating the guest
  starts the duel, and `DuelSocket` delivers those frames to both seats — so the host learns the
  guest arrived by receiving its first `Events`/`Snapshot`. The waiting state therefore ends on the
  first `Snapshot`, not on a lobby message. Adding a frame for it is a protocol change and a version
  bump, and it is not this epic's to make.
- **The link carries the code as a query parameter** (`/?room=CODE`). A path segment (`/r/CODE`)
  would 404 on reload against any static server without a rewrite rule, and `EPIC-07` has not chosen
  one; a query parameter works on every host the bundle could ever sit on. The client reads it on
  boot and sends `JoinRoom` exactly once, after `Welcome` — never before the handshake, and never
  twice under a re-render.
- A code is eight Crockford base32 characters (`ADR-0022`). Input is trimmed and upper-cased before
  sending, and that is **all** the client does to it: the server answers an unparseable code and an
  unknown room identically on purpose, so validating the alphabet here would hand the shape oracle
  back that `DuelSocket.replyToJoinRoom` deliberately withholds.
- Failed joins are budgeted server-side at ten per player per minute (`ADR-0022`). The client shows
  the refusal and does not retry automatically — an auto-retry would spend a real player's budget on
  their behalf.
- Copy-to-clipboard falls back to a selectable, pre-focused text field where the clipboard API is
  unavailable or refused. The link must be obtainable without a working clipboard, because that is
  the one interaction the whole product depends on.
- Vocabulary per `docs/vision.md`: *duel, challenge, rival, room*. Never *table*, *lobby chips*,
  *buy-in*.

## Tasks

| ID | Title | Status |
| --- | --- | --- |
| — | *Not yet split. Run `/plan-story STORY-0305`.* | — |

## Acceptance criteria

- [ ] Creating a room renders the eight-character code and a link containing it.
- [ ] Booting with `?room=CODE` sends exactly one `JoinRoom`, after `Welcome` and not before.
- [ ] `UNKNOWN_ROOM` and `ROOM_FULL` each render their own message, leave the socket open, and send
      nothing further without a fresh click.
- [ ] A pasted code with surrounding whitespace and in lower case is sent trimmed and upper-cased.
- [ ] The waiting state ends when the first `Snapshot` arrives, and not on any other frame.

## Out of scope

- The table itself — `STORY-0306`.
- Rematch — `STORY-0309`.
- The coin balance and recent duels shown beside the lobby — `STORY-0311`.
- Any "your opponent is away" state — `DEC-018` is unanswered.
- Matchmaking, invitations, friends: v0.1 is one link, `docs/vision.md`.
