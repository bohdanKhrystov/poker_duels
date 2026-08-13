---
id: STORY-0211
title: The read path — my coins and my recent duels
type: story
status: backlog
parent: EPIC-02
module: poker-server
labels: [server, http, profiles, coins]
depends_on: [STORY-0210]
---

## Goal

A player can ask the server two questions and get correct answers: *how many duel coins do I have?*
and *how did my last few duels go?* Both scoped to the device-bound profile, both answered without
the client doing any arithmetic.

## Why

Stored is not the same as visible. `STORY-0210` makes the coin durable; this story makes it
*askable*, and that gap — data that exists and can never be requested — is exactly how a feature
falls between this epic and `EPIC-03`. `docs/vision.md` says the reward in this game is the record,
and a record nobody can read is not a reward.

The rendering is `EPIC-03`'s. What `EPIC-02` owes it is an endpoint that already knows the answer.

## Design notes

- **Transport: plain HTTP, not the WebSocket.** Stated because it is a real choice with a real
  reason: this data is not a live game fact, needs no push and no ordering against duel frames; the
  lobby wants it *before* any socket exists, so putting it on the socket would force a connection
  just to read a number; and keeping it off the socket stops the duel protocol growing query
  messages that `EPIC-03` would then have to correlate with a connection. If a screen later wants
  the balance to update live, the duel-finished `ServerMessage` can carry the new balance — an
  addition, not a rewrite.
- Two endpoints: the profile with its balance, and the recent duels. Something like `GET /api/me`
  and `GET /api/me/duels?limit=N`.
- Identity is the same device id as the socket handshake (`ADR-0012`), presented as a request
  header. **An absent or unknown device id is refused and creates nothing** — profile creation
  happens on the socket handshake only, so a crawler hitting the endpoint cannot mint rows. This is
  a small decision with a large blast radius if it goes the other way.
- **The balance is returned exactly as stored, including negative**
  ([`ADR-0014`](../../docs/adr/ADR-0014-duel-coin-economy.md)). No clamping, no `max(0, …)`, no
  treating `< 0` as missing data or an error, no absolute value in a formatter. A player at `−3`
  gets `−3`.
- "Recent" is a bounded, ordered list: newest first, a default of about ten and a hard cap of about
  fifty. No cursors, no paging, no filters — v0.1 has no history screen, and `EPIC-04`/`EPIC-05`
  own the one it eventually gets. A cap exists because an unbounded `limit` is a denial-of-service
  parameter.
- Each entry carries what a result line needs and nothing else: the opponent's label, won/lost/drew,
  the signed coin delta for that duel, hands played, and when it finished. The outcome and the delta
  both come from stored rows — the client never derives who won.
- One query, no N+1: opponent and result come back together.
- Reads go through the same repository boundary as the writes (`ADR-0011`); no route touches SQL
  directly.
- The response types are kotlinx.serialization DTOs in the protocol package family, so `DEC-007`'s
  generator covers them once it is answered. This story does not wait on that.
- Both endpoints are documented in `docs/protocol.md` beside the socket messages, so `EPIC-03` has
  exactly one document to read.

- **`DEC-014` — open, and the read path is planned around it.** The `duel` table has no
  `hands_played` column, so `handsPlayed` is a nullable field that every ticket here leaves `null`,
  with the reason written into its KDoc. Whichever way the decision goes the change is additive: a
  column and the two lines that read it, or the removal of one field. No ticket in this story is
  blocked on it.
- **`DEC-016` — raised while splitting.** A result line names the opponent, and the only thing
  `player` holds is an id: `device_id` is the sole authentication token and must never be shown to
  the other player. The tickets therefore return `opponentPlayerId`, and `DEC-016` asks whether a
  profile gains a display name. A name would be a new field, not a change to that one.

## Tasks

| ID | Title | Status |
| --- | --- | --- |
| [TASK-021101](../tasks/TASK-021101-read-path-response-types.md) | Declare the profile and duel-summary response types | ready |
| [TASK-021102](../tasks/TASK-021102-outcome-from-a-stored-delta.md) | Read won, lost or drew off a stored coin delta | backlog |
| [TASK-021103](../tasks/TASK-021103-recent-duels-limit.md) | Parse, default and cap the recent-duels limit | backlog |
| [TASK-021104](../tasks/TASK-021104-profile-reads-port-and-balance.md) | Read a device's profile and balance behind a `ProfileReads` port | backlog |
| [TASK-021105](../tasks/TASK-021105-the-balance-read-back-is-the-stored-one.md) | Prove the balance read back is the one the duels wrote, minus one included | backlog |
| [TASK-021106](../tasks/TASK-021106-recent-duels-query.md) | Read a player's recent duels with their opponent in one query | backlog |
| [TASK-021107](../tasks/TASK-021107-newest-first-capped-and-mine-only.md) | Prove the duel list is newest first, capped, and nobody else's | backlog |
| [TASK-021108](../tasks/TASK-021108-a-draw-is-visible-in-the-list.md) | Prove a drawn duel is visible in both players' lists | backlog |
| [TASK-021109](../tasks/TASK-021109-the-profile-endpoint.md) | Answer `GET /api/me` for a known device, refuse anything else | backlog |
| [TASK-021110](../tasks/TASK-021110-the-recent-duels-endpoint.md) | Answer `GET /api/me/duels` with a bounded, ordered list | backlog |
| [TASK-021111](../tasks/TASK-021111-endpoints-against-the-database.md) | Read a just-finished duel and its coin back over HTTP, against the database | backlog |
| [TASK-021112](../tasks/TASK-021112-document-both-endpoints.md) | Document both read endpoints in `docs/protocol.md` | backlog |

## Acceptance criteria

- [ ] `GET /api/me` with a known device id returns the profile and its coin balance, and that
      balance equals the sum of the player's stored coin deltas.
- [ ] A player whose only duel was a loss reads back `-1` over the wire — negative, unclamped, and
      not an error response.
- [ ] `GET /api/me/duels` returns that player's finished duels, newest first, each carrying
      opponent, outcome, signed coin delta, hands played and finish time.
- [ ] A duel that finished moments earlier appears in the list, asserted end to end against the
      database container.
- [ ] Only that player's duels are returned: a second profile's duels never appear, asserted with
      two profiles and two duels.
- [ ] An absent or unknown device id is refused, and the profile table row count is unchanged
      afterwards.
- [ ] A `limit` above the cap is clamped rather than honoured; a non-numeric `limit` is a 400; a
      player with no duels gets an empty list and a 200, not a 404.
- [ ] Both endpoints appear in `docs/protocol.md`.

## Out of scope

- Rendering any of it — `EPIC-03` owns the lobby, the profile panel and the results list.
- Leaderboards, rankings, seasons, comparison against other players — `EPIC-05`.
- Full duel history with paging, search or filters — `EPIC-04`.
- Replaying a listed duel — the log is not even persisted yet (`DEC-008`), and the viewer is
  `EPIC-03`.
- Live balance updates over the socket — noted above as a later addition.
