---
id: STORY-0202
title: The wire protocol, defined once in Kotlin
type: story
status: backlog
parent: EPIC-02
module: poker-server
labels: [server, protocol, serialization]
depends_on: [STORY-0201, STORY-0204]
---

## Goal

One Kotlin definition of every message that crosses the socket, in both directions, versioned and
round-trippable — plus a codec that refuses unknown or malformed input without killing the
connection or the process. `docs/protocol.md` describes it in one table.

## Why

The protocol is the contract between this epic and `EPIC-03`, and per
[`ADR-0003`](../../docs/adr/ADR-0003-technology-stack.md) the client's TypeScript types are
*generated* from these definitions rather than hand-written. Everything downstream — sessions,
rooms, the duel runner, the client — is written against these types, so getting the shape right
once is cheaper than correcting it in five places later.

It is also where `ADR-0002` is either enforced or lost. A protocol whose client messages can carry
state is a protocol where the server is not authoritative, regardless of what the server code does
with it.

## Design notes

- The types live in `poker-server`, package `duels.poker.server.protocol`.
  [`architecture.md`](../../docs/architecture.md)'s module list has no protocol module, and adding
  one amends the architecture — that is an ADR, not something a ticket does on the way past.
- **Two sealed hierarchies.** `ClientMessage` is what a client may say; `ServerMessage` is what the
  server says. Sealed, `@Serializable`, short `@SerialName` discriminators, exhaustive `when` — the
  same discipline as `GameEvent`.
- A `ClientMessage` expresses exactly one thing: an intent (`ADR-0002`). `ClientMessage.Act` carries
  a `PlayerAction` **and** the hand number and action sequence it answers, so a replayed or
  out-of-order frame is detected and dropped rather than applied twice — `ADR-0002` names this
  requirement explicitly. It carries no state, no cards, no amounts the server can derive: note
  that `PlayerAction.Call` and `PlayerAction.AllIn` deliberately have no amount already.
- A `ServerMessage` carrying game state carries a **`PlayerView`** (`STORY-0204`) and never a
  `GameState`. This is why the story waits on `STORY-0204`.
- The seat on turn is also sent its `LegalActions` — `legalActions(state)` in
  `BettingRules.kt` already computes it — so the client can draw the right buttons without
  containing a single poker rule.
- Rejections come from the engine's own `Rejection` type, mapped into a message; the protocol does
  not invent a second vocabulary for "that action was illegal".
- A closed, small error set for everything that is not a rejection: unknown message, protocol
  version mismatch, not your turn, unknown room, room full, not in a duel. Closed because an open
  `error: String` is untestable on the client side.
- `PROTOCOL_VERSION` is a constant carried in the handshake. A mismatch is refused with a typed
  message and a closed socket — never a silent misread, the same rule `HandLogJson` and
  `MatchLogJson` already follow for logs.
- The codec is one object with `encode`/`decode` returning a typed failure rather than throwing
  across the connection boundary. Malformed JSON from a client is an expected event, not an
  exception path.
- `docs/protocol.md`: one table of messages, direction, payload and when it is sent. It is what
  `EPIC-03` reads instead of the Kotlin.

## Tasks

| ID | Title | Status |
| --- | --- | --- |
| — | *Tickets are produced by `/plan-story STORY-0202`.* | — |

## Acceptance criteria

- [ ] Every `ClientMessage` and every `ServerMessage` subtype round-trips through JSON unchanged,
      asserted subtype by subtype.
- [ ] Every subtype's discriminator is an explicit `@SerialName`, so a Kotlin rename cannot change
      the wire format; asserted by a test over the serial descriptors.
- [ ] Decoding an unknown discriminator, a truncated frame or a non-JSON frame yields a typed
      failure — no exception escapes the codec, asserted per case.
- [ ] A handshake declaring a different `PROTOCOL_VERSION` is refused with the version-mismatch
      message.
- [ ] No `ServerMessage` can carry a `GameState`, a `Deck`, an `Rng` or a seed, and no
      `ClientMessage` can carry a `Card`, a stack, a pot or a `GameState` — asserted structurally
      over the serial descriptors, not by review.
- [ ] `docs/protocol.md` lists every message with its direction, and a test fails if a message
      type exists that the document does not name.

## Out of scope

- Sending or receiving anything — this story defines types and a codec, `STORY-0205` opens the
  socket.
- The generated TypeScript — `STORY-0203`.
- Room and lobby semantics behind the messages — `STORY-0206`.
- The HTTP read path's DTOs — `STORY-0211` adds them in the same package family.
