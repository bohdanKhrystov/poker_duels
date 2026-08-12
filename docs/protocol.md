# Wire Protocol

This document is the contract for EPIC-03 (the game server's protocol). The Kotlin definitions in `duels.poker.server.protocol` are the source of truth for the wire protocol; the TypeScript client is generated from this schema (see `ADR-0003` and `STORY-0203`).

Protocol version: **1**

## Messages

| Message | Direction | Payload | Sent when |
| --- | --- | --- | --- |
| `Hello` | client → server | `deviceId`, `protocolVersion` | Client initiates connection |
| `Act` | client → server | `handNumber`, `actionSequence`, `action` | Client attempts an action |
| `Welcome` | server → client | `deviceId`, `protocolVersion` | Server accepts the connection |
| `Failure` | server → client | `error` | Server refuses the connection |
| `Snapshot` | server → client | `view` (PlayerView) | Server sends current game state |
| `Events` | server → client | `events` (List of GameEvent) | Server broadcasts game events |
| `YourTurn` | server → client | `handNumber`, `actionSequence`, `legalActions` | Server requests an action from client |
| `Rejected` | server → client | `rejection` | Server rejects an illegal action |

## Protocol Errors

- `UNKNOWN_MESSAGE`: A frame with no matching message type in the current schema.
- `MALFORMED_MESSAGE`: A frame that is not valid JSON, or names a field we do not have.
- `VERSION_MISMATCH`: The client's `protocolVersion` does not match the server's protocol version.
- `NOT_YOUR_TURN`: The client sent an action on a turn that is not theirs.
- `UNKNOWN_ROOM`: The client requested a room that does not exist.
- `ROOM_FULL`: The client requested to join a room that is at capacity.
- `NOT_IN_DUEL`: The client sent an action but is not participating in an active duel.
- `FRAME_LIMIT_EXCEEDED`: The frame was longer, or nested more deeply, than the server accepts, and was refused before parsing.

## Notes

- Every message on the wire carries a `type` field whose value is the message discriminator (e.g., `"Hello"`, `"Act"`, `"Welcome"`).
- The server always writes default values, so fields like `protocolVersion` and zero amounts in `LegalActions` appear in every message that carries them.
- A frame the server cannot decode produces a `Failure` message; it never silently drops the connection, as the other players depend on continuous feedback to know their duel remains active.
