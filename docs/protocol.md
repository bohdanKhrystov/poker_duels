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

### Sign up

**Method and path:** `POST /api/auth/sign-up`

**Authentication:** The `X-Device-Id` header (same authentication as the socket handshake, see `ADR-0012`). An absent, blank, or unknown value answers `401 Unauthorized` with an empty body.

**Request body:** A JSON object with two required fields. The handle is stored folded (see `ADR-0031` §1: 3–32 of `[a-z0-9._-]`, first character `[a-z0-9]`), and the password is stored as-is in NFC form (`ADR-0048` §1: 8–128 code points, nothing trimmed, every code point permitted). No default values: a missing field is refused with `400 Bad Request`.

| Field | Type | Semantics |
| --- | --- | --- |
| handle | string | The player's chosen handle for sign-up. |
| password | string | The player's chosen password for sign-up. |

**Note:** No address field exists on this endpoint. The recovery email (if needed) is its own endpoint and costs the current password (see `ADR-0031` §5). The body carries no player id, because the server resolves identity from the device id and a client may not assert it.

**Responses:** Every response body is empty.

| Status | Meaning |
| --- | --- |
| `201 Created` | One `credential` row now points at the profile this request resolved to; **no session is issued** and the client signs in afterwards. |
| `400 Bad Request` | The body could not be decoded, or the handle fails the fold. |
| `401 Unauthorized` | No resolvable identity; nothing is written, and sign-up creates no profile. |
| `409 Conflict` | The handle is taken, **or** this player already holds a `password` credential. |
| `422 Unprocessable Entity` | The password is under 8 or over 128 code points. |

### Profile endpoint

**Method and path:** `GET /api/me`

**Authentication:** The `X-Device-Id` header (same authentication as the socket handshake, see `ADR-0012`). An absent, blank, or unknown value answers `401 Unauthorized` with an empty body. No distinction is made between absent, blank, and unknown — a bad device id cannot be used to probe for valid device ids.

**Response:** `200 OK` with a JSON object:

| Field | Type | Semantics |
| --- | --- | --- |
| playerId | string | The unique identifier of the player |
| coinBalance | number | The player's current coin balance, computed as wins minus losses. This is a signed integer and may be negative per `ADR-0014` — a balance of `−1` is a correct answer, not an error. |
| displayName | string or null | The player's chosen display name, or `null` if never set. Null means *never set*, and the server fabricates no placeholder per `ADR-0029` §6. |

### Set display name

**Method and path:** `PUT /api/me/name`

**Authentication:** The `X-Device-Id` header (same as `GET /api/me`). Identity is verified **before** the body is read — an absent, blank, or unknown device id answers `401 Unauthorized` with an empty body, and the write is never attempted. This order is critical: confirming name availability without confirming identity would turn the endpoint into an enumeration oracle.

**Request body:** A JSON object with a single required field:

| Field | Type | Semantics |
| --- | --- | --- |
| name | string | The display name the player has chosen. No default value: a missing field, an empty body, or an unrecognised field is refused with `400 Bad Request`. |

**Canonicalisation:** The server canonicalises the name before storing it. The write path applies the following transformations and validations and rejects a name with `400 Bad Request` if any fail:

1. **Trim:** Remove all leading and trailing Unicode whitespace characters.
2. **Normalise:** Apply NFC normalization (`java.text.Normalizer.Form.NFC`). A name that is the same string after a different normalisation form is accepted as-is and may render differently in different systems, so normalisation is the database's job too: `ADR-0029` §2 enforces it with a `CHECK`.
3. **Length:** The canonical name must be 1–32 code points (counted as Unicode code points, not UTF-16 units), otherwise `400`.
4. **Characters:** The server refuses:
   - Any character in Unicode category `Cc` (control) or `Cf` (format) — these are invisible and spoofable.
   - Any whitespace character other than `U+0020` (regular space).
   - Two or more consecutive `U+0020` characters — names that render identically but are stored differently are permanent once set, so this is prevented at write time.

A name that the player typed as, for example, `  Alice  ` becomes `Alice` (9 code points become 5). The database confirms all these rules via constraints; `ADR-0029` §2 lists them.

**Responses:**

| Status | Body | Meaning | Retryable? |
| --- | --- | --- | --- |
| `200 OK` | `ProfileResponse` with the canonical `displayName` | The name was set successfully. The response includes the exact string the player now owns (trimmed and normalised), because the server did both, and the client must be told what it received rather than assume it got what it sent. | No, the client already owns the name. Re-sending the identical name is idempotent and returns `200` again. |
| `400 Bad Request` | Empty | The body was absent, empty, malformed JSON, or named an unrecognised field; **or** the name failed canonicalisation (empty after trim, over 32 code points, or contains a refused character). The write is never attempted. | Yes, the client may fix the name and retry. |
| `401 Unauthorized` | Empty | No device id was provided, it was blank, or it is unknown. The write is never attempted. | No, the client must log in first. |
| `403 Forbidden` | Empty | This player already has a different name set. A display name is permanent once set (`ADR-0029` §4); a player who has chosen a name may never change it. Sending the identical name (exact bytes match) returns `200`, not `403`, so a retry is safe if the client is uncertain. | No, the name cannot be changed. |
| `409 Conflict` | Empty | The requested name (after canonicalisation) collides with another player's name. Names are unique case-insensitively (`ADR-0029` §1); `bob` and `Bob` cannot both exist. The write is never attempted. | Yes, the client may try a different name. |

### Recent duels endpoint

**Method and path:** `GET /api/me/duels`

**Query parameters:**

- `limit` (optional): The maximum number of recent duels to return, defaults to `10`, capped at `50`. Non-numeric, zero, or negative values are rejected with `400 Bad Request`.
- `after` (optional): An opaque cursor to retrieve the next page of results. The exact string a previous response returned in `nextCursor`, echoed back unchanged. Absent means the newest page. A value that does not decode is `400 Bad Request` and nothing is read. A client never constructs one — `ADR-0002`, the server is authoritative. This cursor names a position in the `finishedAt`/`duelId` order and is meaningful only alongside the filter that produced it; v0.1 does not yet refuse a cursor replayed under a different filter, so a client that changes a filter must start a new page walk rather than reuse `nextCursor`. `ADR-0057` settles how that refusal will work — the cursor will carry a fingerprint of the filter it was drawn under — and is decided but not yet built.
- `outcome` (optional): `WON`, `LOST` or `DREW` — the same three spellings a duel summary's own `outcome` field uses, so a client can hand one straight back. Any other value, **including a lower-case spelling**, is `400 Bad Request` and nothing is read. The outcome is read from the duel's stored coin delta, never asserted by a client (`ADR-0002`).
- `opponent` (optional): a **case-insensitive substring** of the opponent's display name. NFC normalised, then 1–32 code points — the display name's own bound — so blank or longer is `400 Bad Request` and nothing is read. `%` and `_` match **literally**: the term is a string, not a pattern. An opponent who has never set a name matches no search; the server fabricates no placeholder to match against (`ADR-0029` §6). Searching by name returns **duels**, never players: no path here turns a name into an identity (`ADR-0029` §7).

A parameter that is present but unusable refuses the whole request, so a request with a good `opponent` and a bad `outcome` is `400` rather than a page filtered by name alone.

**Authentication:** The `X-Device-Id` header (same as above). Identity is verified **before** the `limit` parameter is parsed — an unauthenticated request answers `401` even if the limit is invalid. This prevents a bad limit from revealing whether a device id is valid.

**Response:** `200 OK` with a JSON object containing:

| Field | Type | Semantics |
| --- | --- | --- |
| duels | array | List of recent duel summaries. Empty array if the player has no duels (not `404`). |
| nextCursor | string or null | The cursor to send as `after` for the next page, and `null` on the last page. Always present. |

**Paging:** Pages are total and disjoint — every duel appears exactly once, with no gap and no duplicate, even when a duel finishes between two requests. Results are ordered by `finishedAt` then `duelId`, both descending.

Each duel summary in the array contains:

| Field | Type | Semantics |
| --- | --- | --- |
| duelId | string | The unique identifier of the duel |
| opponentPlayerId | string | The stable identity a client correlates on. This is the opponent's `player.id`, never their device ID. A device ID is the sole authentication token in v0.1, so handing one to the other player would hand over their account. Per `ADR-0021`, the id is the stable identity and `opponentDisplayName` is the label; both travel. |
| opponentDisplayName | string or null | The opponent's chosen display name, or `null` if never set. Null means *never set*; the server fabricates no placeholder per `ADR-0029` §6. The name is read at request time per `ADR-0021`, so a name set later relabels an older duel line. |
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

- **Protocol evolution.** The client holds its protocol version in a single constant, typed against the generated `ProtocolVersion` alias ([ADR-0020](adr/ADR-0020-typescript-protocol-from-serial-descriptors.md)). When `PROTOCOL_VERSION` moves between messages, the client's build fails until it moves with the wire. Adding a new `ServerMessage` variant requires exactly one new entry in the `frames.ts` lookup table that the generated code proves against the union.

  **Three bumps are outstanding and each takes its own step**, in this order: [ADR-0044](adr/ADR-0044-a-rematch-is-one-intent-and-one-room-fact.md) (`STORY-0213`), [ADR-0028](adr/ADR-0028-the-wire-names-an-absent-opponent.md) (`STORY-0214`), [ADR-0027](adr/ADR-0027-the-session-outranks-the-device-id.md) (`STORY-0405`). One number names exactly one wire shape ([ADR-0028](adr/ADR-0028-the-wire-names-an-absent-opponent.md) §8), so no ADR, story or ticket names the integer: the branch making the bump is rebased on `develop` and takes what it finds **plus one**, and at most one bumping branch is open at a time ([ADR-0045](adr/ADR-0045-presence-belongs-to-the-table.md) §§3–4). Two branches that both move 2 → 3 merge without a conflict and every gate stays green, which is the failure the order exists to prevent.

  **The claim itself is recorded, not just ordered.** [`docs/protocol-versions.md`](protocol-versions.md) is a ledger of one row per version naming the wire shape that number means ([ADR-0047](adr/ADR-0047-a-protocol-version-is-claimed-in-a-ledger.md)). A bump claims its number by appending a row, and two branches appending a row for the same number now conflict textually rather than merging silently. A bump commit therefore carries **five** artifacts, not four: `PROTOCOL_VERSION`, this document's version line, the new message rows, a regenerated `protocol.gen.ts`, and one new ledger row.

## Notes

- Every message on the wire carries a `type` field whose value is the message discriminator (e.g., `"Hello"`, `"Act"`, `"Welcome"`).
- The server always writes default values, so fields like `protocolVersion` and zero amounts in `LegalActions` appear in every message that carries them.
- A frame the server cannot decode produces a `Failure` message; it never silently drops the connection, as the other players depend on continuous feedback to know their duel remains active.
