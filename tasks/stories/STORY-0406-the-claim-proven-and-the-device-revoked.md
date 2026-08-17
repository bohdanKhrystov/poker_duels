---
id: STORY-0406
title: The claim proven, and the device binding revoked
type: story
status: backlog
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

## Unblocked

**`DEC-041` is answered** by
[`ADR-0049`](../../docs/adr/ADR-0049-a-device-binding-is-a-row-and-revoking-is-final.md), which the
split must follow: the device→profile edge moves out of `player` into a `device_binding` table with
two partial unique indexes, revocation is one `UPDATE ... SET revoked_at` plus a finality trigger,
`player.device_id` is dropped in the same PR as the code that read it, and the endpoint is
`DELETE /api/me/device`. Its §4 names the orphan-profile defect the rewritten `resolve` will ship if
the mint is not rolled back whole, and §8 lists everything that moves with the migration.

`DEC-045` (does revoking also end every other session?) came out of the same ADR and is the product
owner's. **It does not block this story** — `ADR-0049` §6 ships revocation touching no session, and
the other answer is one additional `DELETE` on the same endpoint.

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
| — | *Not split yet. `DEC-041` is answered; the split follows `ADR-0049`.* | — |

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
