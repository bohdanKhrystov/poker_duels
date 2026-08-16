---
id: STORY-0404
title: Sign-up — one endpoint, and it attaches an account to the profile already here
type: story
status: backlog
parent: EPIC-04
module: poker-server
labels: [server, auth, http]
depends_on: [STORY-0403]
---

## Goal

`POST /api/auth/sign-up` takes a handle and a password and writes **one row**: a `credential`
pointing at the `player` row this request already resolves to. The coins, the history and the name
that profile holds are untouched, because they never move.

## Why

This is [`ADR-0012`](../../docs/adr/ADR-0012-device-bound-anonymous-profiles.md)'s stated debt — *a
lost device is a lost profile* — and the first half of paying it. It is also the story where the
single most expensive mistake in this epic is available: creating a second `player` row and copying
`duel_result` rows onto it, which satisfies a naive reading of "migrate my balance" and is wrong.

## Design notes

- **There is one endpoint and there is no `/api/auth/claim`.**
  [`ADR-0030`](../../docs/adr/ADR-0030-a-claim-adds-a-credential-and-moves-nothing.md) §1 collapses
  them: on a device that has a profile — which is every device that has ever connected — *creating
  an account* and *claiming this profile* are the same operation, and the only difference a second
  endpoint could express is the second `player` row the ADR exists to forbid.
- **The whole write is one `INSERT INTO credential`.** No `player` row is created. No `duel_result`
  row is written, moved, copied or deleted. `player.device_id`, `player.coin_balance`,
  `player.display_name` and `player.created_at` are untouched. The `INSERT` is the whole
  transaction, so a failed sign-up leaves nothing behind.
- **`player_id` comes from the resolved identity, never from the request body.** A body carrying a
  player id is a client asserting who it is, which
  [`ADR-0002`](../../docs/adr/ADR-0002-server-authoritative.md) forbids.
- **Sign-up creates no profile; it requires one.** No resolvable identity is `401` with an empty
  body and no rows written — `ADR-0012`'s rule that HTTP never mints a `player` row survives without
  an exception.
- **Identity here resolves by device id**, exactly as `GET /api/me` does. `ADR-0027`'s
  `IdentityResolver`, with its session precedence, arrives in `STORY-0405` and this endpoint moves
  onto it there; today a device id is the only credential a caller can hold.
- **A player who already holds a credential of the same kind gets `409` and nothing is written**
  (`ADR-0030` §1). Deliberately the conservative direction: loosening a refusal later is additive.
- **The handle rules are `STORY-0403`'s fold**, applied here: refused handles answer `400`, a taken
  handle answers `409`, and the two are distinguishable because `ADR-0031` §5 says so — a handle
  collision has to be reportable or the form is unusable.
- **No address field exists on this endpoint** (`ADR-0031` §5). Attaching a recovery email is its
  own endpoint in `STORY-0416`, and it costs the current password.
- The endpoint issues **no session**. A client signs in afterwards like anybody else; sign-in is
  `STORY-0405`.

## Tasks

| ID | Title | Status |
| --- | --- | --- |
| — | *Not yet split. Run `/plan-story STORY-0404` once `STORY-0403` has merged.* | — |

## Acceptance criteria

- [ ] A device with a profile signs up and gains exactly one `credential` row pointing at that
      profile's `player.id`.
- [ ] The `player` table is byte-identical before and after — the whole table, asserted as a
      multiset of rows, not one column of one row.
- [ ] `SUM(player.coin_balance)` and `SUM(duel_result.coin_delta)` are unchanged by a sign-up, and
      per-player `coin_balance` still equals the sum of that player's deltas.
- [ ] A player who won a duel before signing up still reads back that coin afterwards.
- [ ] An unauthenticated sign-up answers `401`, and the `player` row count is unchanged.
- [ ] A second sign-up for the same player answers `409` and writes nothing.
- [ ] A taken handle answers `409`; a handle that breaks the character rule answers `400`.
- [ ] No response body and no log line contains the password or its hash, asserted structurally.

## Out of scope

- Sign-in, the session token, and the socket handshake — `STORY-0405`.
- Revoking the device binding — `STORY-0406`.
- The recovery email — `STORY-0416`.
- Any screen — `STORY-0412`.
