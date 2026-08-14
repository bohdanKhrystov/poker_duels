---
id: STORY-0311
title: The profile strip — my coins and my recent duels
type: story
status: backlog
parent: EPIC-03
module: web-client
labels: [client, http, profile]
depends_on: [STORY-0302, STORY-0303]
---

## Goal

The lobby shows who you are to the server: your duel-coin balance and your last few results, read
over plain HTTP with the device id the socket already owns.

## Why

`STORY-0211` shipped the read path on the argument that *a value nobody can ask for is a value that
does not exist*. It is asked for here or the coin remains invisible — and a coin nobody sees is not
the reward `docs/vision.md` says the game is played for.

## Design notes

- `GET /api/me` answers `{playerId, coinBalance}`. `GET /api/me/duels?limit=` answers
  `{duels: [...]}`, each `{duelId, opponentPlayerId, outcome, coinDelta, handsPlayed, finishedAt}`
  with `outcome` one of `"WON"`, `"LOST"`, `"DREW"`. `limit` defaults to 10 and is capped at 50;
  zero, negative and non-numeric are `400`, so the client sends a constant it knows is valid or no
  parameter at all.
- Both authenticate with the **`X-Device-Id` header** — the same id `STORY-0303` persists, read from
  the key that module exports. Two storage keys would be two identities for one player.
- **`coinBalance` is signed and may be negative** (`ADR-0014`): `−1` is a correct answer and is
  rendered as `−1`. Never clamped at zero, never shown as `0`, never hidden when negative.
- `401` means the stored id is absent or unknown to the server. That is the ordinary state of a
  first visit, so it renders as *no profile yet*, not as an error — the socket handshake will mint
  an id, after which the read succeeds. The endpoint answers `401` identically for absent, blank and
  unknown on purpose; the client must not try to tell them apart.
- An empty `duels` array is a legitimate answer for a new player (not `404`) and renders an empty
  state.
- **No opponent name is rendered.** The server returns none today; `ADR-0021` adds a display name
  and `DEC-017` decides its product rules. Printing a raw `opponentPlayerId` would pre-empt both and
  put a UUID in front of a player, so a result line shows the outcome, the coin delta, the hand
  count and when it finished — and nothing about who.
- `finishedAt` is an ISO-8601 UTC instant; format it in the browser's locale for display and keep
  the raw value out of the DOM's visible text.
- **These are not `ServerMessage`s.** They carry no `type` discriminator and are not in
  `protocol.gen.ts`; `docs/protocol.md` is their contract. Their shapes are declared **once**, in
  this HTTP module — the single hand-written wire shape the client is allowed, named here so a
  reviewer reads it as the stated exception rather than a violation.

## Tasks

| ID | Title | Status |
| --- | --- | --- |
| — | *Not yet split. Run `/plan-story STORY-0311`.* | — |

## Acceptance criteria

- [ ] A balance of `−1` renders as `−1`.
- [ ] `401` renders the no-profile state and no error alert.
- [ ] An empty `duels` array renders the empty state, and a populated one renders outcome, coin
      delta, hand count and time per row.
- [ ] Both requests carry `X-Device-Id` taken from the same storage key the socket module owns —
      asserted, not assumed.
- [ ] No result row contains an opponent identifier.

## Out of scope

- Paging, filtering, search, a full duel history — `EPIC-04`.
- Leaderboard, rating, season standing — `EPIC-05`.
- Display names — `ADR-0021` and `DEC-017`, and no name is on the wire yet.
- Replay of a listed duel — `EPIC-08`.
- Any socket traffic: this story speaks HTTP only.
