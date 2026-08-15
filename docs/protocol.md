# Wire Protocol

This document is the contract for EPIC-03 (the game server's protocol). The Kotlin definitions in `duels.poker.server.protocol` are the source of truth for the wire protocol; the TypeScript client is generated from this schema (see `ADR-0003` and `STORY-0203`).

Protocol version: **2**

## Messages

| Message | Direction | Payload | Sent when |
| --- | --- | --- | --- |
| `Hello` | client → server | `deviceId`, `protocolVersion` | Client initiates connection |
| `Act` | client → server | `handNumber`, `actionSequence`, `action` | Client attempts an action |
| `CreateRoom` | client → server | (none) | Client attempts to open a room |
| `JoinRoom` | client → server | `code` | Client attempts to join a room |
| `Welcome` | server → client | `deviceId`, `protocolVersion` | Server accepts the connection |
| `Failure` | server → client | `error` | Server refuses the connection |
| `RoomJoined` | server → client | `code`, `seat` | The server seated you in a room |
| `Snapshot` | server → client | `view` (PlayerView) | Server sends current game state |
| `Events` | server → client | `events` (List of GameEvent) | Server broadcasts game events |
| `YourTurn` | server → client | `handNumber`, `actionSequence`, `legalActions` | Server requests an action from client |
| `Rejected` | server → client | `rejection` | Server rejects an illegal action |
| `DuelFinished` | server → client | `outcome` (DuelOutcome) | The duel has ended |

### What a `Rejected` does not change

A `Rejected` reports on an *attempt*, not on state. The engine returns a rejection with an empty
event list and no state change, and `DuelAction.act` returns the duel verbatim in that branch, so
after one:

- `handNumber` is unchanged,
- the hand's last `ActionOn` — where the `actionSequence` a client must echo comes from — is
  unchanged,
- the seat on turn is unchanged.

**The decision point therefore stays open**, and the `handNumber`/`actionSequence` pair the client
already holds stays valid: a second `Act` bearing it passes the server's guard and reaches the
engine. The server does **not** re-prompt after a rejection, and a client must not treat a
`Rejected` as closing its turn. A decision point closes only when a frame that reports state says so
— a `Snapshot`, a `YourTurn` naming a different sequence, or a `DuelFinished`. See
[ADR-0043](adr/ADR-0043-a-rejection-closes-no-decision-point.md).

## Generated TypeScript

The TypeScript type definitions for this protocol are generated from the Kotlin schema. The committed output file is `web-client/src/protocol/protocol.gen.ts`.

To regenerate after protocol changes, run:

```
./gradlew :poker-server:generateProtocolTypes
```

The `./gradlew :poker-server:verifyProtocolTypes` task runs as part of `./gradlew check`, so forgetting to regenerate the file will fail the build.

The generated file contains types only. If you encounter a merge conflict in `protocol.gen.ts`, resolve it by regenerating the file rather than hand-merging — see `ADR-0020` for the rationale.

## HTTP endpoints

These endpoints are **plain HTTP** — they carry no `type` discriminator and are not `ServerMessage`s. The lobby reads them before any socket exists.

### Profile endpoint

**Method and path:** `GET /api/me`

**Authentication:** The `X-Device-Id` header (same authentication as the socket handshake, see `ADR-0012`). An absent, blank, or unknown value answers `401 Unauthorized` with an empty body. No distinction is made between absent, blank, and unknown — a bad device id cannot be used to probe for valid device ids.

**Response:** `200 OK` with a JSON object:

| Field | Type | Semantics |
| --- | --- | --- |
| playerId | string | The unique identifier of the player |
| coinBalance | number | The player's current coin balance, computed as wins minus losses. This is a signed integer and may be negative per `ADR-0014` — a balance of `−1` is a correct answer, not an error. |

### Recent duels endpoint

**Method and path:** `GET /api/me/duels`

**Query parameters:**

- `limit` (optional): The maximum number of recent duels to return, defaults to `10`, capped at `50`. Non-numeric, zero, or negative values are rejected with `400 Bad Request`.

**Authentication:** The `X-Device-Id` header (same as above). Identity is verified **before** the `limit` parameter is parsed — an unauthenticated request answers `401` even if the limit is invalid. This prevents a bad limit from revealing whether a device id is valid.

**Response:** `200 OK` with a JSON object containing:

| Field | Type | Semantics |
| --- | --- | --- |
| duels | array | List of recent duel summaries. Empty array if the player has no duels (not `404`). |

Each duel summary in the array contains:

| Field | Type | Semantics |
| --- | --- | --- |
| duelId | string | The unique identifier of the duel |
| opponentPlayerId | string | The player ID of the opponent (not the device ID — see `DEC-016` for the question of displaying opponent names) |
| outcome | string | Outcome from the requesting player's perspective: `"WON"`, `"LOST"`, or `"DREW"`. A drawn duel appears in the list with `coinDelta` 0 and outcome `DREW` per `ADR-0015`. |
| coinDelta | number | The change in coins from this duel. A signed integer: the winner gains one, the loser loses one, a draw changes nothing. |
| handsPlayed | number | The number of hands played in the duel. |
| finishedAt | string | ISO-8601 instant when the duel finished, as text in UTC (produced by `Instant.toString()`) |

## Protocol Errors

- `UNKNOWN_MESSAGE`: A frame with no matching message type in the current schema.
- `MALFORMED_MESSAGE`: A frame that is not valid JSON, or names a field we do not have.
- `VERSION_MISMATCH`: The client's `protocolVersion` does not match the server's protocol version.
- `NOT_YOUR_TURN`: The client sent an action on a turn that is not theirs.
- `UNKNOWN_ROOM`: The client requested a room that does not exist.
- `ROOM_FULL`: The client requested to join a room that is at capacity.
- `NOT_IN_DUEL`: The client sent an action but is not participating in an active duel.
- `DUEL_PAUSED`: The duel is paused; your action was not applied. The duel resumes when the opponent returns or when their grace period expires. Do not re-send your action.
- `FRAME_LIMIT_EXCEEDED`: The frame was longer, or nested more deeply, than the server accepts, and was refused before parsing.

## What a client does with a frame it cannot read

The web client receives frames in exactly one place: `web-client/src/protocol/`. No other file in the client declares a wire type or touches the WebSocket.

When the client encounters a frame or version it cannot process, it behaves as follows:

- **Unrecognizable frames.** A frame that is not JSON, is not an object, carries no `type` field, or carries a `type` the client's generated union does not name is logged with `console.warn` and dropped. Nothing is rendered; instead, the next `Snapshot` from the server re-establishes the truth. This is why the server sends a `Snapshot` after every duel transition.

- **Failures.** A `Failure` message never closes the socket. The client surfaces the `ProtocolError` verbatim to the user — including any error code its generated union does not yet recognize — and keeps the connection open.

- **Terminal conditions.** `VERSION_MISMATCH`, and a `Welcome` whose `protocolVersion` does not match the version the client sent, are terminal for the connection. The client sends nothing further, does not close the socket, and does not reconnect. A mismatched `Welcome` is not persisted; reloading the page is the only remedy.

- **Protocol evolution.** The client holds its protocol version in a single constant, typed against the generated `ProtocolVersion` alias ([ADR-0020](adr/ADR-0020-typescript-protocol-from-serial-descriptors.md)). When `PROTOCOL_VERSION` moves between messages — [ADR-0027](adr/ADR-0027-the-session-outranks-the-device-id.md), then [ADR-0028](adr/ADR-0028-the-wire-names-an-absent-opponent.md), or the other way round — the client's build fails until it moves with the wire. Adding a new `ServerMessage` variant requires exactly one new entry in the `frames.ts` lookup table that the generated code proves against the union.

## Notes

- Every message on the wire carries a `type` field whose value is the message discriminator (e.g., `"Hello"`, `"Act"`, `"Welcome"`).
- The server always writes default values, so fields like `protocolVersion` and zero amounts in `LegalActions` appear in every message that carries them.
- A frame the server cannot decode produces a `Failure` message; it never silently drops the connection, as the other players depend on continuous feedback to know their duel remains active.
