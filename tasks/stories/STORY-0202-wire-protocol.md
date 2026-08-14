---
id: STORY-0202
title: The wire protocol, defined once in Kotlin
type: story
status: done
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

## ⚠ Open — `DEC-010`, and what the tickets do meanwhile

> **Do the room and lobby messages — create a room, join by code, offer a rematch, leave — belong
> to this protocol, or does `STORY-0207` add them to these two sealed hierarchies once
> `RoomRegistry` exists?**
>
> Their payloads are the shape of a component nobody has built: `STORY-0206` owns the room code, the
> lifecycle and the rematch agreement, and `STORY-0207` owns the wiring. Declaring `JoinRoom(code)`
> here would be guessing at that shape, and `STORY-0203` would then generate TypeScript for a
> message the server cannot answer.
>
> **Nothing is blocked.** The tickets ship the handshake and the duel messages, which are the ones
> `STORY-0205` needs; `ProtocolError` already reserves `UNKNOWN_ROOM`, `ROOM_FULL` and
> `NOT_IN_DUEL`; and a sealed hierarchy takes new members additively, so answering this later costs
> new subtypes and no change to existing ones.

## Tasks

| ID | Title | Status |
| --- | --- | --- |
| [TASK-020201](../tasks/TASK-020201-protocol-json-and-version.md) | Give `poker-server` the serialization plugin, `PROTOCOL_VERSION` and one shared `Json` | ready |
| [TASK-020202](../tasks/TASK-020202-rejection-serializable.md) | Make `Rejection` serializable with explicit discriminators | ready |
| [TASK-020203](../tasks/TASK-020203-legal-actions-serializable.md) | Make `LegalActions` serializable, with its defaults on the wire | ready |
| [TASK-020204](../tasks/TASK-020204-client-message.md) | `ClientMessage` — a hierarchy that can only express an intent | backlog |
| [TASK-020205](../tasks/TASK-020205-server-message-handshake.md) | `ProtocolError` and the two handshake `ServerMessage`s | backlog |
| [TASK-020206](../tasks/TASK-020206-server-message-duel.md) | The four duel `ServerMessage`s — a view, events, the turn, a rejection | backlog |
| [TASK-020207](../tasks/TASK-020207-handshake.md) | The handshake refuses a mismatched protocol version | backlog |
| [TASK-020208](../tasks/TASK-020208-protocol-codec.md) | `ProtocolCodec` — encode, and a decode that returns a typed failure | backlog |
| [TASK-020209](../tasks/TASK-020209-codec-refuses-junk.md) | One bad frame is a value, not an exception | backlog |
| [TASK-020210](../tasks/TASK-020210-explicit-discriminators.md) | Every message's discriminator is an explicit `@SerialName` | backlog |
| [TASK-020211](../tasks/TASK-020211-no-forbidden-payload.md) | Structurally, no seed goes out and no card comes in | backlog |
| [TASK-020212](../tasks/TASK-020212-protocol-doc.md) | `docs/protocol.md`, and the test that keeps it honest | backlog |

Three are startable at once and touch disjoint files in two modules: `TASK-020201`
(`poker-server`), `TASK-020202` and `TASK-020203` (`poker-engine`). The widest the story ever gets
is four, once the codec and the descriptor helper have merged: `TASK-020207`, `TASK-020209`,
`TASK-020211` and `TASK-020212` are then independent and share no file.

`ServerMessage` is built in two passes over one file — the handshake pair first
(`TASK-020205`), the four duel messages second (`TASK-020206`) — because the second needs
`Rejection` and `LegalActions` to be serializable and the first does not. `TASK-020205` is
forbidden from pinning the member count for exactly that reason; `TASK-020210` owns that
assertion, once the hierarchy is complete.

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
