---
id: EPIC-04
title: Identity and profiles
type: epic
status: ready
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

Written on 2026-08-16, when the epic opened. Three things changed from the plan above, and each is
recorded rather than quietly absorbed:

- **`STORY-0404` and `STORY-0406` are no longer the same shape as their titles.**
  [`ADR-0030`](../../docs/adr/ADR-0030-a-claim-adds-a-credential-and-moves-nothing.md) §1 collapses
  sign-up and the claim into **one** endpoint, `POST /api/auth/sign-up`, and says outright there is
  no `/api/auth/claim`. So `0404` is the whole write, and `0406` is what the endpoint cannot ship by
  itself: the coin properties asserted over the schema, and `ADR-0037`'s revoke path.
- **`STORY-0416` and `STORY-0417` are new.**
  [`ADR-0031`](../../docs/adr/ADR-0031-an-optional-verified-recovery-email.md) was accepted after
  this table was written and lands three tables, six endpoints, a mail port and four screens. That
  work has to live somewhere; swelling `0403`/`0404` past what one story can hold would have hidden
  it, so it is filed where a reader can see it.
- **`STORY-0403` depends on `STORY-0401`, and `STORY-0405` on `STORY-0213` and `STORY-0214`.** The
  first is the migration-number race `ADR-0027` §1, `ADR-0029` §8 and `ADR-0031` §7 each name — the
  display name takes `V3` and the rest number themselves after it. The second is `ADR-0047`'s rule
  that at most one protocol-bumping branch is open at a time.

| ID | Title | Depends on | Status |
| --- | --- | --- | --- |
| [STORY-0401](../stories/STORY-0401-display-name-and-the-write-path.md) | `player.display_name`, its canonical form, and the write path | — | **ready**, split into 18 tickets |
| [STORY-0402](../stories/STORY-0402-the-read-path-carries-the-display-name.md) | The read path carries the display name | 0401 | **ready**, split into 5 tickets |
| [STORY-0403](../stories/STORY-0403-credentials-storage-and-hashing.md) | Credentials — the schema, the hash, and a port that returns none | 0401 | **ready**, split into 14 tickets |
| [STORY-0404](../stories/STORY-0404-sign-up-an-account-for-the-profile-already-here.md) | Sign-up — one endpoint, and it attaches an account to the profile already here | 0403 | backlog |
| [STORY-0405](../stories/STORY-0405-sign-in-the-session-and-what-the-socket-presents.md) | Sign-in, the session, and what the socket presents | 0404, 0213, 0214 | backlog |
| [STORY-0406](../stories/STORY-0406-the-claim-proven-and-the-device-revoked.md) | The claim proven, and the device binding revoked | 0405 | backlog — `DEC-041` answered by `ADR-0049`, not yet split |
| [STORY-0407](../stories/STORY-0407-recovery-from-a-device-never-seen.md) | Recovery — signing in from a device that has never been seen | 0406 | backlog |
| [STORY-0408](../stories/STORY-0408-duel-history-paged-over-the-whole-record.md) | Duel history, paged over the whole record | 0402 | backlog |
| [STORY-0409](../stories/STORY-0409-history-filters-and-search.md) | History filters and search | 0408 | backlog |
| [STORY-0410](../stories/STORY-0410-the-display-name-product-rules.md) | The display-name product rules — screened when set, and takeable away | 0401 | backlog — `DEC-042` answered by `ADR-0051`, not yet split |
| [STORY-0411](../stories/STORY-0411-the-name-in-the-client.md) | The name in the client — shown, and settable | 0402 | backlog |
| [STORY-0412](../stories/STORY-0412-the-account-screens.md) | The account screens — sign up, sign in, sign out, and which routes are live | 0406, 0411 | backlog |
| [STORY-0413](../stories/STORY-0413-the-history-screen.md) | The history screen — pages, filters, search | 0409, 0411 | backlog |
| [STORY-0414](../stories/STORY-0414-claimed-here-recovered-there.md) | Claimed here, recovered there, end to end | 0407, 0412, 0413 | backlog |
| [STORY-0415](../stories/STORY-0415-the-offer-after-a-first-win.md) | The offer — an account after a first win, dismissed for good | 0412 | backlog |
| [STORY-0416](../stories/STORY-0416-the-recovery-email-and-the-password-reset.md) | The recovery email, verified, and the password reset | 0405 | backlog |
| [STORY-0417](../stories/STORY-0417-the-recovery-screens.md) | The recovery screens — attach an address, and reset a password | 0412, 0416 | backlog |

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
operator force-rename, and the retired-name set that uniqueness must also consult —
[`ADR-0051`](../../docs/adr/ADR-0051-a-name-is-registered-before-it-is-held.md) then makes all
three one table, which is why the story is one migration and one rewritten write path rather than
three mechanisms. It is no longer a leaf that touches only its own files: its migration joins the
chain, so it queues behind `STORY-0403`'s `V4` like everything else. `STORY-0415`
is a second leaf, hanging off `0412`, since an offer to make an account needs the screen it opens.
Each client story pairs with its server half and can be worked as soon as that half merges.

**Corrected when the stories were written.** The two branches do not in fact share no file: they
share the *migration chain*, and `ADR-0027` §1, `ADR-0029` §8 and `ADR-0031` §7 each say their
migration takes the next free `V<n>` **at merge time**. Two branches in flight would either collide
on a number or discover the collision at merge. `STORY-0403` therefore depends on `STORY-0401`:
the display name is `V3` and everything after it numbers itself with nothing to negotiate. That
makes the name-and-history chain go first, and it is the only place the two branches touch.

And the non-parallelism, recorded so nobody tries to break it:

- `0403 → 0404 → 0405` is a real chain. Three stories editing the same auth surface at once would
  conflict on every ticket, and a session model half-built is worse than none.
- `0401 → 0402` is sequential because a join cannot select a column that does not exist.
- `0412` and `0413` both extend `EPIC-03`'s store and screen shell, so they queue behind `0411`
  rather than beside it.

**Critical path:** `0401 → 0403 → 0404 → 0405 → 0406 → 0407 → 0412 → 0414`, with `0401` prepended
for the migration reason above. It begins with a ticket now, not a decision — which is the single
most useful thing that changed about scheduling this epic. `STORY-0401` merged in full on
2026-08-17; `STORY-0402` and `STORY-0403` both became startable when it did, and `STORY-0402` was
split first because `STORY-0408`, `0409`, `0411` and `0413` all queue behind it while `0403` has
only the credential chain behind it. `TASK-040201` is the one startable ticket.

Two answers add work inside stories already listed, rather than new stories:
[`ADR-0037`](../../docs/adr/ADR-0037-the-device-is-a-credential-until-revoked.md) puts a revoke
path and a session rule into `0406`/`0412`, and
[`ADR-0039`](../../docs/adr/ADR-0039-v01-offers-no-account-deletion.md) constrains `0403`'s schema
without adding a story.

## Open decisions

**Two, both the architect's, and none the human's.** `DEC-044` came out of splitting `STORY-0403` on
2026-08-17 and blocks nothing at all; `DEC-047` was raised on 2026-08-17 by `ADR-0052` and blocks
nothing either, since it is the shape of a fact that ADR already requires. Three were raised and
answered on 2026-08-17: `DEC-045` by
[`ADR-0050`](../../docs/adr/ADR-0050-revoking-the-device-signs-out-everywhere-but-here.md),
`DEC-042` — open since 2026-08-16 and the only thing blocking `STORY-0410` — by
[`ADR-0051`](../../docs/adr/ADR-0051-a-name-is-registered-before-it-is-held.md), and `DEC-046`,
raised by `ADR-0051` the same day, by
[`ADR-0052`](../../docs/adr/ADR-0052-a-takedown-is-told-to-the-player-it-happened-to.md). All three
have moved to the table below. **Nothing in this epic is blocked on a decision, and no story waits on
a human.**

| ID | Question | Blocks |
| --- | --- | --- |
| `DEC-047` | **The architect's** — by what shape does `GET /api/me` carry the fact that a name has been retired from the requesting player? `ADR-0052` §6 requires the profile read to answer it and **adds no column**: `ADR-0051` §1's `name_registry(reason = 'RETIRED', retired_from)` already records it, and one boolean about the caller — never a string — is what crosses the wire. Open: a `ProfileResponse` field (as `deviceRouteLive` is) or something else; join or subquery; and whether `ADR-0051` §1's *"nothing in production reads it"* needs a formal amendment | nothing — due before `STORY-0410` is split, since that is where the read lands |
| `DEC-044` | **The architect's** — the day the Argon2 cost is raised, what happens to rows written under the old parameters? `ADR-0027` §1's *"a constant change plus a rehash on next successful verify"* needs a parser that accepts other parameters; `STORY-0403` says the parser refuses everything but ours, because one that accepts `m=8` is a downgrade attack in a helper function. `TASK-040306` takes the conservative side because loosening a refusal is additive | nothing — due before anyone raises the cost |

The thirteen answered ones are kept here with their answers because the story table still cites them,
and because what each ADR *constrains* is this epic's work.

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
| `DEC-042` | [ADR-0051](../../docs/adr/ADR-0051-a-name-is-registered-before-it-is-held.md) | **One table is the whole namespace, and the operator's path is one function in it.** `name_registry(name PK, reason, retired_from, created_at)` holds names in use, blocked names and retired names as three values of `reason` behind one unique index on `ADR-0029` §1's ICU fold, and `player.display_name` gains a foreign key into it. **A string never leaves that table** — a takedown promotes `TAKEN → RETIRED` — so `ADR-0038`'s three sources of truth collapse into one `INSERT` and the `READ COMMITTED` race between a claim's snapshot and the unique index cannot arise. `STORY-0410` gains: one migration (next free `V<n>`, backfilling one `TAKEN` row per named player and writing no `player` row); a rewritten `PostgresProfileWrites` — two statements in one transaction, and **anything but one row from the second rolls back**, or a refused claim burns a string forever; `SetNameResult` unchanged, so blocked, retired and taken are all `409`; a replaced `player_display_name_is_permanent()` with **exactly one** exception, `name → NULL` and only when that name is already `RETIRED`; a `name_registry` monotonicity trigger; `retire_display_name(player_id, expected_name)` called from `psql`, with the second argument as the wrong-database interlock; `docs/operations.md` as the call site; a test that the function is named nowhere under `poker-server/src/main/kotlin`; and registry inserts in the seven test files that write a display name directly, one of which (`PostgresProfileWritesConcurrencyTest`) polls `pg_stat_activity` for a statement that is no longer the one that blocks. Screening stays a **set-time event** — no re-screening, and a blocklist entry cannot shadow a name in use. Raised **`DEC-046`** — answered below |
| `DEC-046` | [ADR-0052](../../docs/adr/ADR-0052-a-takedown-is-told-to-the-player-it-happened-to.md) | **Told, on the surface where a name is set, in four sentences, and nobody else is told anything.** The notice shows when the player holds no name **and** a name has been retired from them, and ends when they set a new one: *"Your display name was removed. A person running Poker Duels removed it — not a bug, and not another player. That name cannot be used again, by you or by anyone. Choose a new one whenever you like."* **No reason** — `ADR-0051` records no actor, no reason and no log — and no appeal, no contact route, no apology, no accusation, and the removed string is never echoed back. The telling is **derived from state, never delivered**: no notification, no *seen* flag, nothing to dismiss, and no column — `ADR-0051` §1's `retired_from` already records the fact, so `ADR-0052` amends §6's silence default and §1's *"nothing in production reads it"*, and one boolean about the caller crosses the wire (shape is **`DEC-047`**, above). A new name may be set **immediately**; nothing is withheld. `STORY-0410` gains **one** thing — `profileOf` answers the retired-from fact, with two criteria from two distinct fixtures and one negative criterion that a duel line for an opponent whose name was retired is byte-identical to one for an opponent who never set a name — and gains **nothing** on the write path: `SetNameResult` keeps three cases, the endpoint keeps its four codes, `retire_display_name` takes no third argument, the operator types no reason. `STORY-0411` gains a **fourth state** on the name surface (never a modal, never over the duel table) and two copy corrections: `409` becomes *"That name is not available. Try another."* — never *taken*, which is false for the one player most likely to trigger it — and the permanence line becomes *"A name is chosen once. You cannot change it later, and it can be taken away."* Costs recorded rather than discovered: the mistaken victim has the mistake confirmed to their face with no remedy; *why?* is raised and unanswerable, with no inbox anywhere in the product; a player who never opens the name surface is still never told; and an operator can no longer take a name away quietly |
| `DEC-041` | [ADR-0049](../../docs/adr/ADR-0049-a-device-binding-is-a-row-and-revoking-is-final.md) | The device→profile edge **leaves `player`** for its own `device_binding(device_id, player_id, bound_at, revoked_at)` table, keyed by the natural pair, with two partial unique indexes on `WHERE revoked_at IS NULL` — one live binding per device, one per player. Revocation is one `UPDATE ... SET revoked_at` against a table that is not the ledger, so `player` is byte-identical across it and `ADR-0030` §2 gains no fourth writer; the column it protected no longer exists. Revoking is **final** (an `ADR-0029`-shaped trigger refuses `revoked_at → NULL`; the composite key refuses the old pair), and a revoked device may still mint a **fresh** profile. `STORY-0406` gains the migration, `DELETE /api/me/device` (session required — `401`; `409` with no credential; `204` either way otherwise), a rewritten `PlayerDirectory.resolve`, and the orphan-profile assertion on its concurrency test. `STORY-0412` gets `ProfileResponse.deviceRouteLive`. Sessions and sockets are untouched, which raised **`DEC-045`** — answered below |
| `DEC-045` | [ADR-0050](../../docs/adr/ADR-0050-revoking-the-device-signs-out-everywhere-but-here.md) | **One button, and it means both.** `DELETE /api/me/device` keeps `ADR-0049` §5's route, verb, guards and `204`, and gains one statement in the same transaction: `DELETE FROM auth_session WHERE player_id = ? AND token_hash <> ?`, run **unconditionally**, whether or not a binding row was updated. The excluded row is the caller's own, found by hashing the token they presented, so `ADR-0037`'s *"revocation does not kill the revoking session"* holds by construction — and the screen therefore says **"everywhere except here"**, never "everywhere". `STORY-0406` **changes**, correcting `ADR-0049`'s *"ships under either answer"*: the `DELETE` lands with the endpoint, plus one criterion — a second session held by the same player stops working immediately while the revoking session still works, asserted with both tokens. `STORY-0412` gains one action labelled *Stop this device signing in*, offered only while `deviceRouteLive`, behind a confirmation stating three facts (it cannot be undone; you are signed out on every other device and stay signed in here; your password is then the only way back) — and **no session count and no list**, which would be the devices screen `ADR-0027` §2 declined. No schema change, no migration, no new route, no `ProfileResponse` field, no `PROTOCOL_VERSION` step. Costs recorded rather than discovered: a player who has already revoked can end no session at all; a cheap act now costs an irreversible one; a duel already running on a swept device plays to its end |
| `DEC-043` | [ADR-0048](../../docs/adr/ADR-0048-a-password-has-one-rule-and-it-is-length.md) | One rule — **8 to 128 code points**, counted after NFC normalisation — and nothing else: no composition rule, no character rule, no breach corpus, no strength meter, and nothing is trimmed. `STORY-0404` enforces the minimum at sign-up and answers **`422`** with an empty body, distinct from the handle's `400` and `409`; `STORY-0405` applies only the **maximum**, before Argon2 and before the identifier lookup, answering exactly as a wrong password does. `STORY-0416`'s reset uses the identical rule. **NFC is applied in the one place a secret becomes bytes**, so sign-up and sign-in cannot disagree — permanent from the first stored hash. `STORY-0403` is unaffected: `TASK-040307`'s *"no `init`, no `require`"* stands and ASCII is NFC-invariant, so the published-vector tests do not move. `STORY-0412` states the rule before the field is filled and shows no meter |

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
