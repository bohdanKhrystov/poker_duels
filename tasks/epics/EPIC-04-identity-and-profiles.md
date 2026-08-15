---
id: EPIC-04
title: Identity and profiles
type: epic
status: backlog
module: poker-server, web-client
labels: [server, client, identity, auth, persistence]
---

## Goal

A player stops being a browser. They pick a name, attach credentials to the profile they already
have, and find that profile again from a phone that has never seen this site — with the coin, the
name and every duel they have played still there. Then they can read that history properly: paged,
filtered and searched, rather than the handful of recent rows v0.1 shows.

When this epic closes, one test plays a duel anonymously, wins the coin, names the profile, claims
it with credentials, and signs in from a **second client with a different device id** — which reads
back the same balance, the same name and the same duel. That test is the epic; everything else is
how it is made to pass.

## Why now

[`ADR-0012`](../../docs/adr/ADR-0012-device-bound-anonymous-profiles.md) bought v0.1 its speed by
naming a debt precisely, and the debt is now due. Its own words: *"a lost device is a lost
profile"*, and `EPIC-04` **must** include a claim flow *"or the first player to change phones loses
their ladder position with no recourse"*. Nothing in `EPIC-02` or `EPIC-03` paid that down.

[`ADR-0021`](../../docs/adr/ADR-0021-a-profile-gains-a-display-name.md) is **Accepted and
unbuilt**. The migration chain on disk stops at `V2`, there is no `display_name` column, no
`ProfileWrites` port and no join in the read path — so `EPIC-03` renders a UUID where a name
belongs, and says so in its own out-of-scope table. The ADR settled the technical shape a year
before anyone has to argue it again; this epic is where the shape becomes code.

And `EPIC-05` cannot open on top of device ids. `ADR-0012` records that they are trivially minted,
that this is a farming and smurfing vector, and that *"it must not still be true when the
leaderboard goes public"*. Real identity is the countermeasure, and it has to exist before the
ladder means anything.

## Scope

- `player.display_name`, the `V3` migration, and the `PUT /api/me/name` write path exactly as
  `ADR-0021` specifies — a new port rather than an erosion of `ProfileReads`' read-only contract.
- The name in the read path: one more join in `RECENT_DUELS_SQL`, `ProfileResponse.displayName`
  and `DuelSummaryResponse.opponentDisplayName`, both nullable, no fabricated placeholder.
- Credentials: where they are stored, how they are hashed, and the fact that nothing ever reads
  one back.
- Sign-up, sign-in, and the session — including what the WebSocket handshake presents once a
  device id is no longer the only credential a player has.
- **The claim.** Credentials attach to the profile this device already owns, with its coins and its
  history intact. This is `ADR-0012`'s stated obligation, not a feature.
- **Recovery.** Signing in from a device that has never been seen, and finding the same profile.
  The claim is only worth building because of this story.
- Duel history: paged over the whole record, then filtered and searched — `EPIC-02` shipped *"a
  handful of recent results"* and called the rest ours.
- The client half of all of the above: the name shown and settable, the account screens, the
  history screen. `EPIC-03` renders no name and has no account UI, and pointed here for both.
- One end-to-end test that claims a profile on one client and recovers it on another.

## Out of scope

| Not here | Where |
| --- | --- |
| Ratings, seasons, leaderboard, any coin economy beyond the one counter | EPIC-05 |
| Viewing *another* player's profile or history | EPIC-05 — it needs a name per leaderboard row and owns what a row links to. Here, `/api/me` means me |
| Smurf and multi-account defence | EPIC-05. `ADR-0012` records it as a gate on the public leaderboard; this epic makes the countermeasure *available* and enforces nothing |
| Docker images, hosting, deployment, TLS termination | EPIC-07 — and a password is only as safe as its transport, so shipping this publicly is gated on that epic. Recorded here so the gate is not rediscovered late |
| The visual language of the account and history screens, any art | EPIC-06. This epic composes `design/tokens/tokens.css`; it authors no colour |
| Any change to the rules of poker, or to `poker-engine` | Nowhere. The engine gains nothing from this epic — not a name, not an account, not a clock |
| OAuth, social sign-in, passkeys | Not assumed either way — it is part of `DEC-027`, and no story presumes a password |
| Account deletion and data export | `DEC-029`, unanswered. Nothing is built; the schema must simply not foreclose an answer |
| Making an account *required* to play | `DEC-025`, unanswered. Anonymous play stays until the human says otherwise |
| Persisting the full `MatchLog` so history can show the hands themselves | `DEC-008`, unanswered, and EPIC-08. History lists results, not cards |
| Friends, rivals, statistics, the replay viewer | v0.4 |

## Non-negotiables this epic is most likely to break

Identity is where a careful codebase usually springs its first leak, and four of these are one
convenience helper away from being violated:

- **The engine never learns what a player is called.** No name, account, credential or session type
  crosses into `poker-engine`, and its dependency allowlist
  ([`ADR-0010`](../../docs/adr/ADR-0010-engine-takes-a-serialization-dependency.md)) does not move.
  A duel is played by two seats; who they are is a server fact.
- **A client may never assert who it is.** Per
  [`ADR-0002`](../../docs/adr/ADR-0002-server-authoritative.md), a client presents a credential and
  is *told* its identity. A `ClientMessage` or request body carrying a `playerId` the server did not
  itself resolve is a defect, and a login that trusts a client-supplied id is the same defect
  wearing a hat.
- **A name is never an authentication factor.** `ADR-0021` states it outright and this epic must not
  weaken it: no code path resolves a device, session or player *from* a display name. Sign-in
  resolves a credential; the name is data reached by joining on `player.id`.
- **A credential is never readable.** Hashed with a memory-hard function, never logged, never in a
  response body, never in a `ServerMessage`. Sign-in failures do not distinguish *no such account*
  from *wrong password* — an enumeration oracle is worse than a missing feature.
- **A claim moves no coins.** [`ADR-0014`](../../docs/adr/ADR-0014-duel-coin-economy.md) says the
  balance is `wins − losses`, signed and unclamped. Claiming, recovering or renaming a profile
  mints nothing, destroys nothing and clamps nothing. This is chip conservation wearing its
  identity clothes, and it is the property most likely to break the day `DEC-026` is answered.
- **Migrations are immutable.** `ADR-0021` already says it: a change to the schema is a new file,
  never an edit to `V1` or `V2`. This epic adds several and the temptation compounds.
- **No wire type is hand-written.** Anything crossing the socket comes from the generated
  `protocol.gen.ts` ([`ADR-0020`](../../docs/adr/ADR-0020-typescript-protocol-from-serial-descriptors.md));
  the plain-HTTP endpoints stay contracted in `docs/protocol.md` and declared once.

## Stories

Not written yet — this is a scoping document. Titles and order are the plan, and the story files
are authored when the epic opens.

| ID | Title | Depends on | Status |
| --- | --- | --- | --- |
| STORY-0401 | `player.display_name` and the write path | — | *not written* |
| STORY-0402 | The read path carries the display name | 0401 | *not written* |
| STORY-0403 | Credentials: storage and hashing | — | *not written* |
| STORY-0404 | Sign-up: an account for a profile | 0403 | *not written* |
| STORY-0405 | Sign-in, the session, and what the socket presents | 0404 | *not written* |
| STORY-0406 | The claim: credentials attach to the profile already here | 0405 | *not written* |
| STORY-0407 | Recovery: signing in from a device that has never been seen | 0406 | *not written* |
| STORY-0408 | Duel history, paged over the whole record | 0402 | *not written* |
| STORY-0409 | History filters and search | 0408 | *not written* |
| STORY-0410 | The display-name product rules | 0401 | *not written* |
| STORY-0411 | The name in the client: shown, and settable | 0402 | *not written* |
| STORY-0412 | The account screens: sign up, sign in, claim | 0406, 0411 | *not written* |
| STORY-0413 | The history screen: pages, filters, search | 0409, 0411 | *not written* |
| STORY-0414 | Claimed here, recovered there, end to end | 0407, 0412, 0413 | *not written* |
| STORY-0415 | The offer: an account after a first win, dismissed for good | 0412 | *not written* |

## What can run in parallel

The honest headline has changed: **this epic is no longer decision-starved.** It was written
when six of its seven decisions were open, and the scheduling advice below was shaped by that.
All seven are now answered (see *Open decisions*), so every story here is startable in dependency
order and nothing waits on a human.

There are two long branches that share no file:

- **The credential chain** — `0403 → 0404 → 0405 → 0406 → 0407` — lives in new auth files plus the
  handshake `STORY-0405` owns.
- **The name-and-history chain** — `0401 → 0402 → 0408 → 0409` — touches the migration chain,
  `RECENT_DUELS_SQL` and the profile DTOs, and nothing the credential chain opens.

They meet only at `STORY-0414`. `STORY-0410` is a leaf hanging off `0401` and is now unblocked
by [`ADR-0038`](../../docs/adr/ADR-0038-a-name-is-screened-when-set-and-can-be-taken-away.md),
which gives it three pieces of work rather than one: the blocklist on the write path, the
operator force-rename, and the retired-name set that uniqueness must also consult. `STORY-0415`
is a second leaf, hanging off `0412`, since an offer to make an account needs the screen it opens.
Each client story pairs with its server half and can be worked as soon as that half merges.

And the non-parallelism, recorded so nobody tries to break it:

- `0403 → 0404 → 0405` is a real chain. Three stories editing the same auth surface at once would
  conflict on every ticket, and a session model half-built is worse than none.
- `0401 → 0402` is sequential because a join cannot select a column that does not exist.
- `0412` and `0413` both extend `EPIC-03`'s store and screen shell, so they queue behind `0411`
  rather than beside it.

**Critical path:** `0403 → 0404 → 0405 → 0406 → 0407 → 0412 → 0414`. It begins with a ticket now,
not a decision — which is the single most useful thing that changed about scheduling this epic.

Two answers add work inside stories already listed, rather than new stories:
[`ADR-0037`](../../docs/adr/ADR-0037-the-device-is-a-credential-until-revoked.md) puts a revoke
path and a session rule into `0406`/`0412`, and
[`ADR-0039`](../../docs/adr/ADR-0039-v01-offers-no-account-deletion.md) constrains `0403`'s schema
without adding a story.

## Open decisions

**None.** Every decision this epic was blocked on has been answered. They are kept here with
their answers because the epic's story table still cites them, and because what each ADR
*constrains* is this epic's work.

| ID | Answered by | What it means here |
| --- | --- | --- |
| `DEC-025` | [ADR-0036](../../docs/adr/ADR-0036-an-account-is-offered-never-required.md) | Never required; anonymous play stays fully ranked. The fifteenth story **does** exist: the client offers an account after a first win, dismissible permanently. Nothing else in the epic gates on identity |
| `DEC-026` | [ADR-0030](../../docs/adr/ADR-0030-a-claim-adds-a-credential-and-moves-nothing.md) | A claim attaches a credential to the `player` row already resolved — no second profile, no copied `duel_result`, no `UPDATE player`. Unblocks `STORY-0407` |
| `DEC-027` | [ADR-0031](../../docs/adr/ADR-0031-an-optional-verified-recovery-email.md) | An optional, **verified-only** recovery email in its own table, feeding a single-use one-hour reset token; sign-in is by a lowercase handle, never the display name. Unblocks `STORY-0403`, `STORY-0404` |
| `DEC-028` | [ADR-0027](../../docs/adr/ADR-0027-the-session-outranks-the-device-id.md) | A session token outranks a device id and never falls back to it; stored as SHA-256, 30-day absolute expiry. `PROTOCOL_VERSION` moves to 3 once, in `STORY-0405`. Unblocks the whole credential chain |
| `DEC-029` | [ADR-0039](../../docs/adr/ADR-0039-v01-offers-no-account-deletion.md) | No deletion in v0.1 — but `STORY-0403`'s schema **may not foreclose one**, which forbids denormalising a display name into `duel_result`. The history read path keeps joining for it |
| `DEC-030` | [ADR-0037](../../docs/adr/ADR-0037-the-device-is-a-credential-until-revoked.md) | The device signs in until the player revokes it. Adds a revoke path, a settings affordance and a session rule to `STORY-0406`/`STORY-0412`; the account screens must state which routes are live |
| `DEC-031` | [ADR-0041](../../docs/adr/ADR-0041-a-handle-and-a-password-are-the-only-credential.md) | Handle and password only. `STORY-0412`'s screens are designed for one credential — no provider row |
| `DEC-017` | [ADR-0038](../../docs/adr/ADR-0038-a-name-is-screened-when-set-and-can-be-taken-away.md) | A blocklist screens at set time, an operator may force-rename, and a taken name is **retired forever**. Uniqueness gains two more sources of truth. Unblocks `STORY-0410` |

`ADR-0012` is **not** open: anonymous device-bound profiles stay, and this epic adds identity on
top of them rather than replacing them. `ADR-0021` is **not** open either — the display name's
schema, write path, read path and wire shape are settled, and `STORY-0401` implements them as
written.

## Definition of done

- [ ] Every story is `done` or `dropped`.
- [ ] One test plays a duel anonymously, wins the coin, sets a name, claims the profile with
      credentials, and signs in from a second client bearing a different device id — reading back
      the same balance, the same name and the same duel.
- [ ] A claim leaves the balance byte-identical, still `wins − losses`, still signed and unclamped.
- [ ] No response body, log line or `ServerMessage` anywhere in the codebase contains a password or
      a password hash, asserted structurally rather than by inspection.
- [ ] Sign-in against a wrong password and sign-in against an account that does not exist are
      indistinguishable to the caller.
- [ ] No code path resolves a player, device or session from a display name.
- [ ] `GET /api/me` and `GET /api/me/duels` still answer for a device that never created an
      account — anonymous play is not collateral damage.
- [ ] History paging is total and disjoint: `N` duels read in pages of `k` return each duel exactly
      once, in one order, with no gap and no duplicate across a concurrent insert.
- [ ] `V1` and `V2` are byte-unchanged, and every schema change this epic makes is a new file.
- [ ] `poker-engine` declares no dependency outside the `ADR-0010` allowlist, and no engine type
      names an account, a credential or a player's name.
- [ ] `./gradlew :poker-server:verifyProtocolTypes` still passes and `protocol.gen.ts` is
      byte-identical to what the emitter writes.
- [ ] Checked by hand, once, and recorded: a profile is claimed on one device and recovered on
      another with the coin intact. `ADR-0012` named this cost in advance; this is the receipt.

## Metrics

Filled in when the epic closes; feeds the Product B case study.

| | |
| --- | --- |
| Tasks completed | |
| Accepted on first review | |
| Average review iterations | |
| Test lines / production lines | |
| Tasks re-scoped mid-flight | |
| Manual human edits | |
