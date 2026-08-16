---
id: STORY-0406
title: The claim proven, and the device binding revoked
type: story
status: blocked
parent: EPIC-04
module: poker-server
labels: [server, auth, coins, security]
depends_on: [STORY-0405]
---

## Goal

Two things that the sign-up endpoint alone does not give: a **property test** over the whole schema
proving that no identity operation ever mints, destroys or clamps a coin, and a **revoke** path that
lets a player who has attached a password stop their device id from signing in.

## Why

`STORY-0404` ships the claim as one `INSERT`. What it cannot ship is the guarantee that it stays one
`INSERT` after somebody adds an endpoint in a year without reading
[`ADR-0030`](../../docs/adr/ADR-0030-a-claim-adds-a-credential-and-moves-nothing.md). That is what
§5's two properties are for, and they are total over the schema rather than per endpoint.

The revoke half is [`ADR-0037`](../../docs/adr/ADR-0037-the-device-is-a-credential-until-revoked.md):
attaching a password *adds* a sign-in route and removes none, so an unattended browser still signs
in as you until you say otherwise. The human accepted that risk on the condition that the player can
end it and that the screens say which routes are live.

## Blocked on

**`DEC-041` — the architect's.** `ADR-0037` says outright that what revocation looks like in the
schema is left to this story: *"whether revocation nulls the column, moves the binding to its own
row, or marks it revoked beside the credential is a technical question with more than one defensible
answer and no reason to guess it here."* `ADR-0030` §2 says `player.device_id` is **never rewritten
by any identity operation** and makes that the structural reason the coin invariant holds, so nulling
the column is not obviously available. This story cannot be split until that is answered, and it must
be answered once, in an ADR, rather than in a ticket.

## Design notes

- **The two properties, from `ADR-0030` §5**, asserted by one test-fixture helper against the live
  schema:
  - **P1, per player**: `player.coin_balance = COALESCE(SUM(duel_result.coin_delta WHERE player_id =
    player.id), 0)` for every row.
  - **P2, globally**: `SUM(duel_result.coin_delta) = 0` and `SUM(player.coin_balance) = 0`.
- **Called after every step of one scenario**, not only at the end — a mint and a burn cancel:
  connect anonymously, play a duel and win, set a name, sign up, reconnect with the token, sign into
  a *second* account from the same device, play a duel as that account, sign out, reconnect
  anonymously, read the profile back.
- **The `player` multiset is snapshotted before and after each identity operation** and asserted
  byte-identical, with the single exception of the one row and column a rename may touch.
- **Revocation does not kill the revoking session** (`ADR-0037`): the player stays signed in on the
  device they are holding and presents the password next time.
- **Revocation is offered only when a credential exists**, since revoking the sole route to a
  profile would strand it. That is a server rule, not only a hidden button.
- The account screens' half of `ADR-0037` — stating which routes are live — is `STORY-0412`.

## Tasks

| ID | Title | Status |
| --- | --- | --- |
| — | *Blocked on `DEC-041`. Not split.* | — |

## Acceptance criteria

- [ ] P1 and P2 hold at every step of the scenario above, asserted after each one.
- [ ] The `player` table is byte-identical across sign-up, sign-in, sign-out and revocation.
- [ ] A revoked device id no longer resolves to that profile: a `Hello` presenting only it is not
      seated as that player.
- [ ] Revoking does not invalidate the session that revoked, asserted by using it immediately
      afterwards.
- [ ] Revocation is refused for a player holding no credential, and the refusal is tested.
- [ ] A player who revokes and then signs in with their password reaches the same profile, coins and
      name.

## Out of scope

- Recovery on a never-seen device as an end-to-end scenario — `STORY-0407`.
- The screens — `STORY-0412`.
- Deleting an account or a profile — `ADR-0039`: not in v0.1, and the schema must not foreclose it.
