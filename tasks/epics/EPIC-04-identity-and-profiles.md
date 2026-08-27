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
| [STORY-0404](../stories/STORY-0404-sign-up-an-account-for-the-profile-already-here.md) | Sign-up — one endpoint, and it attaches an account to the profile already here | 0403 | **ready**, split into 14 tickets — raised `DEC-048`, answered by `ADR-0055`, which leaves all 14 unchanged |
| [STORY-0405](../stories/STORY-0405-sign-in-the-session-and-what-the-socket-presents.md) | Sign-in, the session, and what the socket presents | 0404, 0213, 0214 | **ready**, split into 24 tickets on 2026-08-23 — raised `DEC-069` (the sign-in budget's numbers), answered on 2026-08-24 by [`ADR-0074`](../../docs/adr/ADR-0074-sign-in-is-ten-wrong-passwords-a-minute-reserved-before-the-hash.md); nothing in the chain is blocked |
| [STORY-0406](../stories/STORY-0406-the-claim-proven-and-the-device-revoked.md) | The claim proven, and the device binding revoked | 0405 | backlog — `DEC-041` answered by `ADR-0049`, not yet split |
| [STORY-0407](../stories/STORY-0407-recovery-from-a-device-never-seen.md) | Recovery — signing in from a device that has never been seen | 0406 | backlog |
| [STORY-0408](../stories/STORY-0408-duel-history-paged-over-the-whole-record.md) | Duel history, paged over the whole record | 0402 | **ready**, split into 11 tickets — raised no decision |
| [STORY-0409](../stories/STORY-0409-history-filters-and-search.md) | History filters and search | 0408 | backlog |
| [STORY-0410](../stories/STORY-0410-the-display-name-product-rules.md) | The display-name product rules — screened when set, and takeable away | 0401 | backlog — `DEC-042` answered by `ADR-0051`, not yet split |
| [STORY-0411](../stories/STORY-0411-the-name-in-the-client.md) | The name in the client — shown, and settable | 0402 | backlog |
| [STORY-0412](../stories/STORY-0412-the-account-screens.md) | The account screens — sign up, sign in, sign out, and which routes are live | 0406, 0411 | **ready**, split into 27 tickets on 2026-08-26 — `TASK-041201` is startable; the last two in the chain read `blocked` on `DEC-077` (the product owner's), **answered the same day** by [`ADR-0083`](../../docs/adr/ADR-0083-the-second-account-screen-is-sign-in-and-its-address-is-never-refused.md) and waiting only on the planner folding it in. **Two** account screens, which `ADR-0076` §1 left to this story: `#/account`, and a sign-in screen the product now calls ***Sign in*** at `#/sign-in` |
| [STORY-0413](../stories/STORY-0413-the-history-screen.md) | The history screen — pages, filters, search | 0409, 0411 | backlog |
| [STORY-0414](../stories/STORY-0414-claimed-here-recovered-there.md) | Claimed here, recovered there, end to end | 0407, 0412, 0413 | backlog |
| [STORY-0415](../stories/STORY-0415-the-offer-after-a-first-win.md) | The offer — an account after a first win, dismissed for good | 0412 | in progress — partially split into four on 2026-08-27; raised `DEC-079` and `DEC-080` |
| [STORY-0416](../stories/STORY-0416-the-recovery-email-and-the-password-reset.md) | The recovery email, verified, and the password reset | 0405 | **ready**, split into 29 tickets on 2026-08-25 — `TASK-041601` is startable; six are `blocked` on `DEC-071` (the product owner's), `DEC-072`, `DEC-073` and `DEC-074` (the architect's). `DEC-072` was answered on 2026-08-25 by [`ADR-0077`](../../docs/adr/ADR-0077-no-sender-is-an-implementation-and-detachment-is-a-decorator.md), `DEC-073` the same day by [`ADR-0079`](../../docs/adr/ADR-0079-five-to-attach-ten-to-forget-and-the-attach-budget-is-the-only-mail-cap.md), and `DEC-074` the same day by [`ADR-0080`](../../docs/adr/ADR-0080-the-password-is-judged-before-the-token-is-touched.md), so **none of the four is open**. None was the human's: `ADR-0031` §7 already defers the transport, and therefore any bill, to `EPIC-07` |
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

**`STORY-0408` was split out of order on 2026-08-18, and the reason is recorded rather than
inferred.** `STORY-0405` — the next story on the critical path — depends on `STORY-0213` and
`STORY-0214` in `EPIC-02`, which are not this epic's to work, and `0406`/`0407` sit behind it.
`STORY-0408` depends only on `STORY-0402`, which is done, so the name-and-history chain is where
work exists that nothing outside this epic gates. It needs no session, no sign-in and no
`auth_session`, and its eleven tickets assume none.

Two answers add work inside stories already listed, rather than new stories:
[`ADR-0037`](../../docs/adr/ADR-0037-the-device-is-a-credential-until-revoked.md) puts a revoke
path and a session rule into `0406`/`0412`, and
[`ADR-0039`](../../docs/adr/ADR-0039-v01-offers-no-account-deletion.md) constrains `0403`'s schema
without adding a story.

## Open decisions

**Four, raised on 2026-08-25 when `STORY-0416` was split: one the product owner's and three the
architect's.** `DEC-054`, raised on 2026-08-19 by the ADR that answered `DEC-053` and the last
decision `STORY-0412` was waiting on, was answered the same day and is recorded below. `DEC-069`,
raised on 2026-08-23 when `STORY-0405` was split, was answered on 2026-08-24 and is recorded below
too. `STORY-0412` was gated by no decision at all when it was split, and the four new ones blocked
six tickets inside `STORY-0416` and nothing else in this epic. **That sentence has since stopped
being true in one respect and is corrected rather than deleted:** splitting `STORY-0412` on
2026-08-26 raised `DEC-077`, which gated the last two of its twenty-seven tickets and was answered
the same day.

**Two, raised on 2026-08-27 when `STORY-0415` was split, and they are a pair: one the product
owner's and one the architect's downstream of it.**

| ID | Question | Whose | Blocks |
| --- | --- | --- | --- |
| `DEC-079` | Is `ADR-0036`'s *"not again"* a fact about the **player** or about **this browser**, and what **spends** the offer? `ADR-0036` §Consequences says the flag *"belongs on the profile"*; `STORY-0415` §Design notes says a key the client module owns, asserted *"through the injected storage"*. And `DEC-049` says a `429` spends nothing *"including `ADR-0036`'s offer, **which only 'Not now' dismisses**"*, while `STORY-0415` says the offer never appears after a second win — so does an accepted-then-abandoned offer come back? | **The product owner's.** Both halves are what a player experiences, on a second browser and after an abandoned sign-up, and both derive from `docs/vision.md`. Neither adds to nor subtracts from *What it is / What it is not*, so **not the human's** | `STORY-0415`'s fifth, sixth and seventh tickets — the persistence, the `Lobby` wiring and the whole-client arc. **None of the four already written**, which hold under either answer |
| `DEC-080` | What carries the offer's state on the wire, and what tells the client this win is the **first**? `GET /api/me` carries neither field, and `ADR-0036` calls the first-win fact *"a read-path question"* while naming no field, endpoint or column. Under *the player*: the column, the field, the dismissal endpoint, and whether this `module: web-client` story grows a server half. Under *this browser*: whether it is one client-owned key — `one-module-owns-each-storage-key.test.ts` gaining a third entry — and that `ADR-0036` is **amended rather than ignored** | **The architect's**, and strictly downstream: its answer is different under each half of `DEC-079` | Exactly what `DEC-079` blocks, and nothing else |

`DEC-077`, the product owner's, was raised on 2026-08-26 when
`STORY-0412` was split and answered on 2026-08-26 — the table that carried it is gone, and what it
settled is recorded below. `DEC-075` — a fifth from the `STORY-0416` split, raised on 2026-08-25 by
[`ADR-0077`](../../docs/adr/ADR-0077-no-sender-is-an-implementation-and-detachment-is-a-decorator.md)
rather than by the split, and blocking nothing — was answered the same day and is recorded below.
`DEC-076` — a sixth, raised on 2026-08-26 by an implementation attempt on `TASK-041626` rather than
by a planner or an ADR — was **registered and answered in the same PR** and is recorded below too.

`DEC-077` asked **what the product calls the screen a player opens to reach an account from a browser
that does not hold it, and therefore what that screen's permanent slug is.** It was the product
owner's because `ADR-0076` §1 makes a slug the lowercase form of a word the product already says and
states outright that the ADR **coins no player-facing vocabulary**: a screen needing a word the
product does not yet say is a product question. `STORY-0412` settled the count `ADR-0076` §1 left to
it — **two** account screens — and one of the two names was found already merged rather than coined,
since `ADR-0050` §3, `ADR-0036` and `ADR-0056` §2 each say *account* to a player, so `#/account`
ships in `TASK-041222` with nothing invented. The second screen had only a **verb** in the merged
record (`ADR-0050` §3's *"You stay signed in here"*). It was **not** the human's: it adds nothing to
and takes nothing from the vision's *What it is* / *What it is not*, it costs no money and moves no
roadmap row, and the vision's *Positioning* sentence — *"Lichess and Chess.com… Dark, quiet, fast,
minimal"*, the same sentence `ADR-0056` and `ADR-0078` derived from — was the input the product owner
already had. It blocked `TASK-041226` and `TASK-041227`, the last two tickets in the story, and
nothing else in this epic.

It was answered on 2026-08-26 by
[`ADR-0083`](../../docs/adr/ADR-0083-the-second-account-screen-is-sign-in-and-its-address-is-never-refused.md):
**the second account screen is *Sign in*, and its address is never refused.** Nothing is coined —
`ADR-0050` §3's merged text already says *sign in* to a player twice, and the *Positioning* sentence
above is what licensed the plain word over a themed one. The heading is `SIGN_IN_HEADING = "Sign in"`
and **the slug is `sign-in`**, a literal in `screen.ts`, with **the hyphen taken from the product's
own spelling** in `POST /api/auth/sign-in` rather than from a slugifier — so the client's address and
the server's path read the same character for character. That widens `ADR-0076` §1's *"a word"* to a
hyphenated compound for this one word, and writes down the rule that survives it. **The word is said
twice from one constant**: the screen's heading and the single door on the account screen, offered
only when `signedIn` is false (`ADR-0060` §2), with no fourth door on the lobby. **The address is
refused to nobody** — a browser holding a session token that opens `#/sign-in` gets the screen, and
holding a token is **not** a fourth branch, because `signedIn` means *this browser holds a string*,
nothing here reacts to a `401`, and `ADR-0050` §3 leaves every other device holding a dead one. Those
are the browsers that need the screen most, and `ADR-0076` §2 forbids an address that makes a second
claim about entitlement. `ADR-0076` §3's three store-owned branches still outrank it. **A successful
sign-in lands on `#/account`**, because the account screen's routes statement is the only
confirmation this product has. `TASK-041226` and `TASK-041227` are unblocked by it, and the planner
folds in three corrections: the `^[a-z]+$` criterion widens, `TASK-041227` gains the landing rule and
a fourth file, and the heading is queried by role.

`DEC-054` — *does the web client grow URL-addressable routes and a working browser Back, and what
carries them?* — was answered on 2026-08-25 by
[`ADR-0076`](../../docs/adr/ADR-0076-a-screen-the-player-chose-has-an-address.md): **a screen the
player chose has an address; a screen the server gave has none.** The client gets addresses, and an
address is a URL **fragment** — `/`, `/#/duels`, `/#/leaderboard`, and **one slug per screen for
`STORY-0412`, however many screens that story turns out to have**, which is the story's call and not
the ADR's. A slug is the lowercase form of a word the product already says to a player, written as a
literal so a restyled heading cannot break a link. The waiting screen, the table and the result
screen get **no address, ever** — all three are chosen by frames, and an address claiming a seat is a
client asserting a game fact — and the **store outranks the address** wherever they disagree.
`Lobby.tsx` keeps its branch order and loses its two `useState` flags; `HistoryScreen` and
`LadderScreen` do not change at all, which is `ADR-0060` §4 paying off. What carries it is **two
owned files and no new dependency**: a pure `screen.ts` and a `use-screen.ts` over
`useSyncExternalStore`, the primitive `ADR-0032` §3 already chose. The fragment beat a path segment
on the sentence already merged in `room-link.ts` — a path segment 404s on a static host with no
rewrite rule, and `EPIC-07` has no file — and beat a query parameter because a query reaches every
host's access log and would ride along in the invite link. Browser *Back* now **returns to the first
screen in the same document**; `ADR-0060` §4's in-page control replaces rather than pushes; and
`DuelResult`'s `<a href="/">` and the waiting screen's *Back to the lobby* **stay real page loads**,
because routing them would ship `ADR-0075`'s four-field presence leak. No address is gated, and an
unknown fragment renders the first screen with no error. **A separate word, worth naming here because
this epic uses both:** `STORY-0412`'s *"which routes are live"* means the **sign-in** routes of
`ADR-0037` — the device binding and the credential — and nothing about that acceptance criterion
changes because a screen gained an address.

**None of the four is the human's, and this was checked rather than assumed.** `STORY-0416`'s
subject is email delivery, which is the shape of question that usually reaches the human — a
provider is a bill, and sending to real addresses is deliverability risk with consequences outside
the software. Neither arises, because
[`ADR-0031`](../../docs/adr/ADR-0031-an-optional-verified-recovery-email.md) §7 already settled it:
*"The mail transport is not decided here... belongs to `EPIC-07`; the port is the boundary that lets
this ship without it"*, and *"a build with no sender configured is a valid state."* Every ticket in
`STORY-0416` sends nothing under every answer to `DEC-072`. Nor does anything there touch the
vision's *What it is* / *What it is not*: the human chose *optional email, recovery only* when they
answered `DEC-027`, and these four apply that choice rather than revisiting it.

**No table follows, because there is nothing to put in it: every decision this epic has raised is
answered.** The row that stood here was `DEC-077`, struck by the PR that answered it.

`DEC-076` — raised on 2026-08-26 by a coder on `TASK-041626` who blocked rather than guessing, and
blocking that one ticket — was **registered and answered in the same PR** by
[`ADR-0082`](../../docs/adr/ADR-0082-a-handle-is-read-from-a-proven-address-never-from-a-player-id.md):
**a handle is read from a proven address, never from a player id.**
`RecoveryMailer.sendPasswordReset(address, token, handle)` requires a login handle and **nothing in
this codebase could obtain one from a `PlayerId`** — `PasswordResets.issue` answers a `Boolean`,
`RecoveryEmails.verifiedOwnerOf` a `PlayerId?`, `PostgresPlayerDirectory` resolves device ids only,
and `Credentials` declares exactly four members, with `verifyCurrent`'s own KDoc recording that a
reverse lookup was deliberately refused *"for no other reason than this one check."* **`RecoveryEmails`
gains one member, keyed by an address**: `resetRecipientOf(address: EmailAddress): ResetRecipient?`,
answering `ResetRecipient(playerId, handle)` in one statement joining `recovery_email` to
`credential`, with `SELECT_VERIFIED_OWNER_SQL`'s `WHERE` clause **character for character** and
`REWRITE_CREDENTIAL_SQL`'s `kind = 'password'` verbatim. **There is no `PlayerId` overload and there
must never be one** — the fence is the argument type, not the member name: obtaining a handle
requires already holding a **verified** recovery address, the exact secret this endpoint refuses to
disclose, which is `ADR-0031` §5's own test for `verify-email`'s `409` applied to a lookup rather
than to a status code. A **`JOIN`, never a `LEFT JOIN`**, so an unknown address, a pending-only one
and a verified address whose owner holds no `password` credential all answer `null` and the third —
unreachable under §3 — is answered rather than handed to the route as a null handle.
**`verifyCurrent`'s refusal is upheld rather than overturned**, and becomes a build failure for the
first time: a test asserts `Credentials` declares no member returning `String` or `String?`, green
today and reddening on exactly `handleOf(playerId): String?`. **`PasswordResets.issue` keeps its
`Boolean`**, with §5's two outcomes, seven merged assertions and four test doubles untouched — the
closest call, lost by `Issued(handle)`/`Suppressed` because `issue(playerId, newResetToken())` **is**
a `PlayerId → handle` function with a side effect and because a credential-less owner leaves `Issued`
no honest `String`. **Costs**: a read whose product is a login handle now exists, fenced by an
argument type and a KDoc rather than by an impossibility proof; `RecoveryEmails` reads a third table,
so two of its own KDoc sentences are amended with it; `ResetRecipient` is a plain `data class`, so
`"$recipient"` prints the handle — deliberately unredacted, with the trigger named as the first log
line anywhere on the reset path; two reads of `recovery_email` now carry one `WHERE` clause in two
constants; the join adds a third caller to an unindexed `credential (player_id)` scan; and the
`'password'` literal goes ambiguous the day `DEC-027` admits a second kind carrying an identifier.
The gate is a **tripwire, not a proof** — a value-class-wrapped handle passes it, since Kotlin
reflection reports the wrapper rather than `String`. **No migration, no index, no protocol version,
no `recoveryRoutes` parameter, and `RecoveryMailer` byte-unchanged.** **Unblocks `TASK-041626`**,
which needs a fixture whose verified owner actually holds a `password` credential and a
`theMailCarriesTheOwnersOwnHandle` test over two players with different handles; **`TASK-041630`
needs one acceptance criterion widened** from two distinct argument strings to three. Raises no
`DEC`, and nothing is the product owner's or the human's.

`DEC-075` — raised on 2026-08-25 by `ADR-0077`, blocking nothing — was answered on the same day by
[`ADR-0081`](../../docs/adr/ADR-0081-a-mailed-link-is-a-fragment-route-and-the-token-is-the-segment-behind-the-slug.md):
**a mailed link is a fragment route, and the token is the segment behind the slug.** `RecoveryLinks`
returns `"$baseUrl/#/reset/$token"` and `"$baseUrl/#/verify/$token"`, so recovery works on an object
store serving one file at `/` and **`EPIC-07` inherits no rewrite rule**. **The token has not left
the fragment**: it is the second segment of one, which is *not* a URL path segment and is still never
transmitted, never logged and never in a `Referer`, so `ADR-0031` §4's entropy, hash at rest, one
hour, single use and refusal of a query string stand **byte-unchanged** and `TASK-041620` is
untouched — the server still decodes the token from a body and contains no `queryParameters`. **A
recovery link contains no `?` at all**, absolute rather than conditional, which is what beat a query
section inside the fragment (`#/reset?token=…`) in the closest call here: *a `?` is fine, but only
after the first `#`* is broken by deleting two characters from a string that still looks ordinary.
**The slugs are `reset` and `verify`, fixed by the ADR rather than left to `STORY-0417`** — the
register's own premise cut the other way, because these two addresses are **minted by the server into
a mail**, so a slug one module writes and another parses cannot be chosen later without reaching back
into `RecoveryLinks`; neither word is coined, `ADR-0031` §4 and `ADR-0077` §6 wrote them, and
`STORY-0417` keeps every other address `ADR-0076` §1 gives it, the account screen's and the *forgot
password* screen's included. **`verify` and `reset` are answered the same way**: the `404` is
deterministic, so the immediate second attempt mails the same dead link, and a failed verification is
what *creates* the total-loss state, since `ADR-0031` §3 makes an unverified address recover nothing
while the player believes they have opted in. **A stale or spent link is a screen that renders and a
`400` on submission, never a routing outcome** — the client never inspects the token and must not
learn how, `ADR-0080` having deliberately left no liveness oracle; a missing token is an empty input
rather than an unknown address; and the sentence a player reads is already `STORY-0417`'s, so nothing
here is the product owner's. `screen.ts` gains `tokenFromHash`, matches on the **first fragment
segment**, and the token is read once at mount before the address is replaced with
`hashForScreen(screen)`. Costs recorded rather than discovered: **the two ends agree by two literals
in two modules and nothing mechanical**, since a fragment crosses no wire and `protocol.gen.ts`
cannot carry it, so a divergence lands every mailed link on the lobby silently; `#/duels/anything`
now renders the record, a real widening of `ADR-0076` §7; **a reset link opened in a tab already
seated is destroyed** by `ADR-0076` §3, at fifteen minutes' cost; `/reset` and `/verify` become dead
addresses a host with a rewrite rule will serve; and two player-facing words are fixed in a URL before
`STORY-0412` names the screens. **Erred toward the `404` being unacceptable rather than toward the
most recent ADR**: `ADR-0076`'s six costs are legible and survivable, while a `404` here is silent,
deterministic, invisible to every test in this repository and permanent. **`TASK-041633` changes in
two string literals and its `DEC-075` note** — its other four tests, its no-`?` criterion, its
no-encoder refusal and its `Host`-header sweep survive verbatim — and `TASK-041632` and `TASK-041620`
are unchanged.

`DEC-074` — raised on 2026-08-25 when `STORY-0416` was split, blocking `TASK-041629` — was answered
on the same day by
[`ADR-0080`](../../docs/adr/ADR-0080-the-password-is-judged-before-the-token-is-touched.md):
**the password is judged before the token is touched, so a refusal costs no link.** `ADR-0031` §5's
precondition is what gives way; §4's `DELETE … RETURNING` and its *"no read-then-write window"* are
byte-unchanged, and keeping them is the point. `POST /api/auth/reset-password` runs three steps and
no others: decode ⇒ `400`; `passwordIsLongEnough` **and** `passwordIsWithinTheWorkBound` ⇒ `422`,
with **no connection taken and no statement executed**; then `consume` ⇒ `204` / `400`. A `422`
therefore leaves the row exactly as it was, the same link works on the next submission while it
lives, and §5's fifteen-minute suppression still sees a live token — so the double-click §5
protected against is still a complete no-op. The order is `ADR-0048` §2's own placement rule at a
third endpoint: the maximum runs *"before Argon2 runs and before the identifier is looked up"*, and
here the token **is** the identifier; the minimum joins it because §2 makes reset one of the two
endpoints it applies at. **The register's disclosure worry was checked and cut the other way**:
because the branch is chosen entirely by the caller's own password, the `422` is byte-identical for
a live token, an expired one and a string the caller invented, so `400`-versus-`422` reports nothing
about `password_reset` — every other order makes it a liveness report, and the pre-check shape makes
liveness observable without consumption, which is the property `ADR-0031` chose 256 bits to avoid
having to defend. Costs recorded rather than discovered: **a `422` no longer proves the link is
alive** and nothing else does either, so a player can be refused twice for one attempt and
`STORY-0417`'s form must move from *password refused* to *link expired* without contradicting
itself; **a stranger holding no token can make the endpoint answer `422`**, licensed only while the
policy stays a published pure function — the day a breach corpus or any row-reading rule joins it
(`ADR-0048` leaves that open) the endpoint must be budgeted or the rule moved behind the lookup; and
§5 now reads wrong on its own, corrected in a status line. **`TASK-041620` is unchanged and needs no
re-cut** — the step lands in front of `consume` — with one fixture constraint: every request in
`ResetPasswordRouteTest`, including the two expecting `400`, must carry a `newPassword` of 8–128
code points. **`TASK-041629` gains the check and loses one named test**,
`aBadTokenStillAnswersFourHundredNotFourHundredAndTwentyTwo`, whose order this reverses; what it
defended is asserted the other way round — a fabricated token and a live one produce
indistinguishable `422`s. `TASK-041617` transcribes the corrected sentence rather than §5's.
**Unblocks `TASK-041629`**, and left `DEC-075` as the only decision this epic still carried open —
itself answered the same day by `ADR-0081`, above, which is why the table is empty.
Nothing is the product owner's or the human's: the words a form uses are already `STORY-0412`'s
under `ADR-0031`'s *What this does not settle* and `ADR-0048` §7.

`DEC-073` — raised on 2026-08-25 when `STORY-0416` was split, blocking `TASK-041628` — was answered
on the same day by
[`ADR-0079`](../../docs/adr/ADR-0079-five-to-attach-ten-to-forget-and-the-attach-budget-is-the-only-mail-cap.md):
**five to attach, ten to forget, and the attach budget is the only cap on the mail it causes.**
`forgot-password` admits **10** per remote address per rolling **60 000 ms**; `recovery-email` admits
**5** per **60 000 ms**; four `ServerConfig` values in the existing pattern, one `AttemptBudget`
instance each. The register row's premise held and then cut the other way: invisible collateral is a
reason to be *generous*, since a limiter nobody can perceive is one nobody can pace around. What
decided the numbers is that **`ADR-0031` §5's fifteen-minute rule covers one of the two mail paths**
— `TASK-041613` builds it inside `PasswordResets.issue`, while `claimPending` returns `Unit` and
writes unconditionally, so a verification mail follows every successful attach for ever. So on
`forgot-password` the address budget **adds nothing** the durable rule does not already do better,
and is set where it cannot bite a player who has lost their password and is told mail is coming; on
`recovery-email` it is the **only** cap on mail to a caller-chosen recipient and a second door to the
current-password guess `ADR-0074` priced at ten a minute, so five keeps the front door cheaper. **An
over-budget attempt still counts** — one rule for every limiter here, and the rule that actually
defeats a sprayer who gets no feedback to pace against. Placement differs per endpoint and is fixed
in §3. The key was never open: §5 fixes `origin.remoteAddress`. The named cost is that an attacker
can switch off password recovery for everybody behind one address, silently, while each of them is
told the product is sending mail. **Unblocks `TASK-041628`**, raises no `DEC`, and leaves one
residual for the planner: the attach path has no per-account resend suppression, which on the best
reading of §5 is a defect against `TASK-041607`, `TASK-041608` and `TASK-041625` rather than a new
question, due before `EPIC-07` configures a sender.

`DEC-071` — raised on 2026-08-25 when `STORY-0416` was split, blocking `TASK-041624` and
`TASK-041625` — was answered the same day by
[`ADR-0078`](../../docs/adr/ADR-0078-the-mail-is-the-only-real-check-on-an-address.md): **the mail is
the only real check on an address, so the syntax rule refuses almost nothing.** The asymmetry the
register row named was resolved on `ADR-0031`'s own text rather than on taste — §3 requires
verification before an address can do anything, and §2 notes that *"a player whose address is stored
has, by construction, received mail at it"*, so an exact deliverability check already exists and a
syntax rule can only report earlier and can only be wrong in the direction that costs an account.
**The predicate is four clauses**: at least one `@`, the first code point is not `@`, the last is not
`@`, no ASCII control character, and at most **254 code points** — RFC 5321's path limit in the unit
`ADR-0029` §2 and `ADR-0048` §1 already fixed. No separate minimum: `a@b` is the shortest string that
passes. The control-character clause is **not a syntax rule and says so**; no `addr-spec` holds one
in any position, so it denies no mailbox, and it exists because a line terminator inside an address
is the one thing this predicate could hand `EPIC-07`'s unwritten transport that would harm somebody
outside this game. **The rule runs where an address enters and nowhere else** — `forgot-password`
keeps its unconditional `202` and consults no predicate, which is `ADR-0048` §2's *never on the
lookup path* one endpoint pair over and the single property that keeps a later tightening free.
**Nothing is canonicalised**: `emailAddressOrNull` returns the input unchanged, deliberately
departing from `ADR-0048` §5 and `ADR-0029` §2, because those strings are compared and an address is
a delivery target whose stored form §2 already fixed. Deliverability, DNS and MX, domain spelling,
disposable-address lists, role addresses, plus-address stripping, unicode conversion, a dot in the
domain, quoting, and any whitespace that is not a control character are all **deliberately not
checked**, each written down as a decision. The refusal is **`400` with an empty body**, identical to
a failed decode, and the client says one sentence naming no mailbox, no domain and no other account;
the one constraint on the silent path is that **a `202` may not be rendered as recovery being on**,
since §3 leaves `hasRecoveryEmail` false. Both fixture tables ship in §6. The costs, recorded rather
than discovered: **this endpoint's only feedback now fires almost never**, so nearly every mistake
becomes `202` and silence; `Bob Smith <bob@example.com>` and a trailing space are accepted and
undeliverable; the predicate is **never an invariant over `recovery_email`**, which is the price of
the reversibility; two rows can be one mailbox via plus-addressing or normalisation, which `ADR-0063`
tolerates until the ladder is public; and a permissive rule hands `EPIC-07` a higher bounce rate,
which is where `ADR-0031` §7 already put deliverability. **`TASK-041601`'s parked catalog assertion
now has its condition met** — the answer admits non-ASCII — so the follow-up it declined to file
becomes a ticket, and that is the planner's. Unblocks `TASK-041624` and `TASK-041625`. Nothing in it
was the human's: no clause needs a paid service, the one alternative that would is refused on that
ground, and nothing is sent to any address under any answer. Raises no `DEC`.

`DEC-072` — raised on 2026-08-25 when `STORY-0416` was split, blocking `TASK-041625`, `TASK-041626`
and `TASK-041627` — was answered the same day by
[`ADR-0077`](../../docs/adr/ADR-0077-no-sender-is-an-implementation-and-detachment-is-a-decorator.md):
**no sender is an implementation, detachment is a decorator, and a test binds neither.** The wiring
holds `NoRecoveryMailer`, a `public object` with two empty bodies, rather than a null — so no route
branches on whether a sender exists, which is the property `TASK-041627` was written to prove and
which now belongs to the type instead of to three handlers each remembering the same `if`.
Detachment is `DetachedRecoveryMailer(delegate, scope, log)`, a decorator over the *same* port, so no
route file holds a `CoroutineScope`, a `launch` or a `Job`, and the wiring — not the handler —
decides whether to apply it. The delivery scope is a **supervisor child of the application's job**,
built in `duelServer` beside `scheduleSweeps`: `ADR-0025`'s rule applied unchanged, so shutdown
cancels every in-flight send, one failure cancels no sibling, and it is deliberately not the
application scope itself, which also carries a ticker that never completes. **The server may exit
with mail pending**, and pending mail is cancelled rather than drained. A failed send is logged
once — the member name and `failure::class.simpleName`, with no message, no stack trace, no
`player_id` and no success line, because a transport's own exception is the likeliest place an
address ever reaches a log and `ADR-0031` §6.3 admits no exception. **Nothing above the port is
retried and no row is compensated**: retry is transport-shaped and stays `EPIC-07`'s *behind* the
port, and a compensating delete would destroy live tokens for mail that arrived. `baseUrl` becomes a
`ServerConfig` field where absent is the default and malformed refuses to start, and `RecoveryLinks`
is the only place either URL is built. **What a test can await**, the clause that decided the shape:
the test binds an **undecorated** recording double, so the send is an ordinary suspend call in the
handler and both *one mail was sent, with this link* and *no mail was sent* are list comparisons with
no join, no channel and no timeout — **absence forced it**, since no await proves a negative and four
of this story's criteria assert one. §5's `202`-before-the-send ordering stays a **review criterion**
and is gated by nothing, exactly as `TASK-041626`'s Proof predicted. The sharp cost, recorded rather
than discovered: **a lost reset mail buys the player fifteen minutes of silence** they cannot
distinguish from anything else, because the row is committed before the send can fail and §5
suppresses the retry. `TASK-041625`, `TASK-041626` and `TASK-041627` are unblocked as far as this
decision goes — `TASK-041625` still waits on `DEC-071` — and `TASK-041627` is now **bigger than its
Files table**, which is the planner's to re-cut. No transport was chosen and no bill implied; the one
clause that could not be answered provider-independently, the retry policy, was named and left to
`EPIC-07`. Raises `DEC-075`, answered above.

`DEC-069` — raised on 2026-08-23 when `STORY-0405` was split, blocking `TASK-040523` and nothing
else — was answered on 2026-08-24 by
[`ADR-0074`](../../docs/adr/ADR-0074-sign-in-is-ten-wrong-passwords-a-minute-reserved-before-the-hash.md):
**ten failed sign-ins per remote address per rolling sixty seconds**, defaults `10` and `60000` under
`AUTH_SIGN_IN_MAX_ATTEMPTS` and `AUTH_SIGN_IN_WINDOW_MILLIS`, on a second `AttemptBudget` instance.
The pair is `ADR-0022` §2's rather than sign-up's five per fifteen minutes, because the arithmetic
shows the defence is nearly insensitive across that range while the shared-address collateral is not,
and because fifteen minutes combined with *over budget still counts* is what turns a burst into an
indefinite lockout. The budget is **reserved before the hash and refunded when the password was
right** — `ADR-0027` §6 meters failures, the pool is only protected if the check precedes the hash,
and the peek-then-record shape that appears to reconcile them lets concurrent requests from one
address burst unbounded Argon2 work. `AttemptBudget` therefore gains one method, `refund`, in
`TASK-040519`, which is unstarted; `TASK-040520` gains sign-in's config pair beside sign-up's; and
`TASK-040523` is unblocked at five files with `atomic:`, the same compiler gate `TASK-040521`
names, and waits only on `TASK-040522` like every other ticket in the chain. Over budget answers exactly as a wrong password does, so nothing reaches the wire and
`ADR-0056` §1 stands. The cost is recorded rather than left to be found: eleven wrong passwords from
one address inside a minute deny sign-in to everyone behind it for sixty seconds past the last
attempt, and they are told only that their password is wrong. **No account is ever locked** — the key
is the address, which is why nothing in this decision was the product owner's.

`DEC-052` and `DEC-053` — both raised on 2026-08-19 when `STORY-0413` was split, each blocking
exactly one ticket at the end of that chain — were answered the same day and are recorded below.
`TASK-041312` and `TASK-041313` stay `blocked` only until a planner transcribes what
[`ADR-0059`](../../docs/adr/ADR-0059-the-record-is-searched-when-the-player-submits.md) §5 and
[`ADR-0060`](../../docs/adr/ADR-0060-the-record-is-its-own-screen-and-the-lobby-is-the-door.md) §7
name into them; neither ADR moves anything already written in either ticket, and both stay at three
files.

`DEC-051` — raised on 2026-08-18 when `STORY-0411` was split, blocking `TASK-041114` and the three
tickets behind it — was answered the same day by
[`ADR-0058`](../../docs/adr/ADR-0058-where-a-name-would-be-the-client-prints-no-name.md): where a
display name would be printed and the player holds none, the client prints **`No name`** — the same
two words, on every surface, about every player, whatever the reason there is no name. All four
tickets stand exactly as split, because the answer is a **single string** returned by the one
`nameOrNone` function `TASK-041114` was already written around; `TASK-041114` is `backlog` and its
first acceptance criterion now names the ADR. The answer deliberately reaches past the two surfaces
asked about: `STORY-0413`'s history rows and `EPIC-05`'s leaderboard rows are third-person, so the
warmer second-person treatment that reads best on a profile strip (*You*, *Your rival*) was rejected
for one that survives its own inheritance. Two costs are recorded rather than left to be found —
a list of nameless rivals cannot be scanned, which `ADR-0052` §5 requires anyway, and `No name` is
itself registrable as a display name, since `ADR-0029` §3 refuses invisible characters and not
ordinary words and `ADR-0051` §5 ships the blocklist empty.

`DEC-050` — raised on 2026-08-18 when `STORY-0409` was split, blocking **one** acceptance criterion
of that story and no ticket — was answered the same day by
[`ADR-0057`](../../docs/adr/ADR-0057-a-cursor-names-the-filter-it-was-drawn-under.md). All eleven
tickets stand exactly as split: `TASK-040909` still ships an endpoint that accepts `after` beside a
filter under the weaker contract, and the answer is built on top of it by a re-plan of the story
(`ADR-0057` §9 lists the four things that re-plan contains). The due date moved with the answer —
the cursor's encoding is negotiated by no protocol version, so the change has to land before
`STORY-0413` puts a cursor in a browser, which is later than *"before `STORY-0409` closes"* and is
the deadline that actually binds.

`DEC-049` — open before that one — was raised on 2026-08-17 by
[`ADR-0055`](../../docs/adr/ADR-0055-sign-up-is-budgeted-by-address-and-over-budget-says-so.md)
while answering `DEC-048`, and answered the same day by
[`ADR-0056`](../../docs/adr/ADR-0056-a-throttled-sign-up-says-so-and-keeps-what-was-typed.md). It
blocked no ticket and no story.

Six decisions were raised and answered on 2026-08-17: `DEC-045` by
[`ADR-0050`](../../docs/adr/ADR-0050-revoking-the-device-signs-out-everywhere-but-here.md),
`DEC-042` — open since 2026-08-16 and the only thing blocking `STORY-0410` — by
[`ADR-0051`](../../docs/adr/ADR-0051-a-name-is-registered-before-it-is-held.md), `DEC-046`,
raised by `ADR-0051` the same day, by
[`ADR-0052`](../../docs/adr/ADR-0052-a-takedown-is-told-to-the-player-it-happened-to.md),
`DEC-047`, raised by `ADR-0052` the same day, by
[`ADR-0053`](../../docs/adr/ADR-0053-the-profile-says-the-name-was-removed.md), and `DEC-044` —
which came out of splitting `STORY-0403` and blocked nothing — by
[`ADR-0054`](../../docs/adr/ADR-0054-a-raised-argon2-cost-is-a-ledger-entry-and-a-rehash.md), and
`DEC-048` — raised the same day when `STORY-0404` was split, and blocking none of its fourteen
tickets — by
[`ADR-0055`](../../docs/adr/ADR-0055-sign-up-is-budgeted-by-address-and-over-budget-says-so.md), and
`DEC-049` — raised by `ADR-0055` the same day — by
[`ADR-0056`](../../docs/adr/ADR-0056-a-throttled-sign-up-says-so-and-keeps-what-was-typed.md). All
seven have moved to the table below. **Nothing in this epic is blocked on a decision, no decision is
open, and no story waits on a human.**

The eighteen answered ones are kept here with their answers because the story table still cites
them, and because what each ADR *constrains* is this epic's work.

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
| `DEC-046` | [ADR-0052](../../docs/adr/ADR-0052-a-takedown-is-told-to-the-player-it-happened-to.md) | **Told, on the surface where a name is set, in four sentences, and nobody else is told anything.** The notice shows when the player holds no name **and** a name has been retired from them, and ends when they set a new one: *"Your display name was removed. A person running Poker Duels removed it — not a bug, and not another player. That name cannot be used again, by you or by anyone. Choose a new one whenever you like."* **No reason** — `ADR-0051` records no actor, no reason and no log — and no appeal, no contact route, no apology, no accusation, and the removed string is never echoed back. The telling is **derived from state, never delivered**: no notification, no *seen* flag, nothing to dismiss, and no column — `ADR-0051` §1's `retired_from` already records the fact, so `ADR-0052` amends §6's silence default and §1's *"nothing in production reads it"*, and one boolean about the caller crosses the wire (shape is **`DEC-047`**, answered by `ADR-0053` below). A new name may be set **immediately**; nothing is withheld. `STORY-0410` gains **one** thing — `profileOf` answers the retired-from fact, with two criteria from two distinct fixtures and one negative criterion that a duel line for an opponent whose name was retired is byte-identical to one for an opponent who never set a name — and gains **nothing** on the write path: `SetNameResult` keeps three cases, the endpoint keeps its four codes, `retire_display_name` takes no third argument, the operator types no reason. `STORY-0411` gains a **fourth state** on the name surface (never a modal, never over the duel table) and two copy corrections: `409` becomes *"That name is not available. Try another."* — never *taken*, which is false for the one player most likely to trigger it — and the permanence line becomes *"A name is chosen once. You cannot change it later, and it can be taken away."* Costs recorded rather than discovered: the mistaken victim has the mistake confirmed to their face with no remedy; *why?* is raised and unanswerable, with no inbox anywhere in the product; a player who never opens the name surface is still never told; and an operator can no longer take a name away quietly |
| `DEC-047` | [ADR-0053](../../docs/adr/ADR-0053-the-profile-says-the-name-was-removed.md) | **One boolean on the profile, correlated to the caller's own row.** `ProfileResponse` gains `displayNameRemoved: Boolean` — non-null, and **with no default value**, because `ContentNegotiation`'s `Json` has `encodeDefaults = false` while `protocolJson` has it `true`, so a defaulted field would be present in every test's JSON and absent from the wire for the ~100% of players whose answer is `false`. It is `true` **iff** the caller holds no display name **and** a name has been retired from them: the two `null` states `ADR-0052` requires to be distinguishable differ in this column, and the bit **goes quiet** when the player sets a new name. Computed by **one correlated `EXISTS`** in the existing profile `SELECT` — one round trip, and **never a `LEFT JOIN`**, because a player may hold two retired names and a join returns two rows for one profile — correlated to `p.id` rather than to a second bind parameter. `ADR-0051` §8's migration gains **one partial index**, `name_registry_retired_from_idx ... WHERE retired_from IS NOT NULL`, which adds nothing to the hot write path (a `TAKEN` row has `retired_from IS NULL` and is not in it) and is one line today against a second migration once `V<n>` is immutable. **`PROTOCOL_VERSION` does not move** — the ledger fingerprint hashes `protocolDeclarations()`, rooted at `ClientMessage`/`ServerMessage` only, and `ProfileResponse` is reachable from neither. `STORY-0410` gains: the field; the `SELECT` expression; the index line; a `docs/protocol.md` row (which `HttpEndpointDocumentationTest` does **not** enforce — it checks documented ⇒ exists, not the reverse); the literal `false` in `PostgresProfileWrites`, since `NameSet` describes a player who holds a name; and **three criteria, of which the first two must be two players in one database** — the uncorrelated mis-implementation `EXISTS (SELECT 1 FROM name_registry WHERE reason = 'RETIRED')` makes every caller read `true` once anybody is retired, and passes two tests that each hold one fixture. `DuelSummaryResponse` gains nothing and `retired_from` may appear in exactly one main source file, because `RECENT_DUELS_SQL` already holds the **opponent's** `player` row and the same `EXISTS` pasted there publishes a takedown to a stranger. **Amends `ADR-0051` §1's *"No read path exposes this table"***, retiring that sentence as an absolute; every other refusal in §1 survives verbatim. Costs recorded rather than discovered: a semijoin on the hottest read forever; a third index on that table, earning nothing until the registry is large and unremovable once merged; the wire deliberately unable to say *this player was once moderated*; and the impossible pair prevented by a SQL string rather than by the type |
| `DEC-041` | [ADR-0049](../../docs/adr/ADR-0049-a-device-binding-is-a-row-and-revoking-is-final.md) | The device→profile edge **leaves `player`** for its own `device_binding(device_id, player_id, bound_at, revoked_at)` table, keyed by the natural pair, with two partial unique indexes on `WHERE revoked_at IS NULL` — one live binding per device, one per player. Revocation is one `UPDATE ... SET revoked_at` against a table that is not the ledger, so `player` is byte-identical across it and `ADR-0030` §2 gains no fourth writer; the column it protected no longer exists. Revoking is **final** (an `ADR-0029`-shaped trigger refuses `revoked_at → NULL`; the composite key refuses the old pair), and a revoked device may still mint a **fresh** profile. `STORY-0406` gains the migration, `DELETE /api/me/device` (session required — `401`; `409` with no credential; `204` either way otherwise), a rewritten `PlayerDirectory.resolve`, and the orphan-profile assertion on its concurrency test. `STORY-0412` gets `ProfileResponse.deviceRouteLive`. Sessions and sockets are untouched, which raised **`DEC-045`** — answered below |
| `DEC-045` | [ADR-0050](../../docs/adr/ADR-0050-revoking-the-device-signs-out-everywhere-but-here.md) | **One button, and it means both.** `DELETE /api/me/device` keeps `ADR-0049` §5's route, verb, guards and `204`, and gains one statement in the same transaction: `DELETE FROM auth_session WHERE player_id = ? AND token_hash <> ?`, run **unconditionally**, whether or not a binding row was updated. The excluded row is the caller's own, found by hashing the token they presented, so `ADR-0037`'s *"revocation does not kill the revoking session"* holds by construction — and the screen therefore says **"everywhere except here"**, never "everywhere". `STORY-0406` **changes**, correcting `ADR-0049`'s *"ships under either answer"*: the `DELETE` lands with the endpoint, plus one criterion — a second session held by the same player stops working immediately while the revoking session still works, asserted with both tokens. `STORY-0412` gains one action labelled *Stop this device signing in*, offered only while `deviceRouteLive`, behind a confirmation stating three facts (it cannot be undone; you are signed out on every other device and stay signed in here; your password is then the only way back) — and **no session count and no list**, which would be the devices screen `ADR-0027` §2 declined. No schema change, no migration, no new route, no `ProfileResponse` field, no `PROTOCOL_VERSION` step. Costs recorded rather than discovered: a player who has already revoked can end no session at all; a cheap act now costs an irreversible one; a duel already running on a swept device plays to its end |
| `DEC-044` | [ADR-0054](../../docs/adr/ADR-0054-a-raised-argon2-cost-is-a-ledger-entry-and-a-rehash.md) | **A closed, append-only ledger of the costs this project has shipped, and a rehash on the next successful sign-in — and nothing is built until the day the cost is raised.** `STORY-0403` changes in **no** way: `TASK-040306`'s parser, `TASK-040308`'s `matches`, `TASK-040309`'s bound and `TASK-040312`'s `verify` all ship exactly as written, and today's strict refusal stands, because with one ledger entry the floor, ascent and legacy round-trip tests all quantify over a single element and prove nothing. What is settled is the day itself. The parser keeps its **whole-string comparison** — one literal becomes *n*, generated by the same `section()` the encoder uses, so no number parser ever runs on the verify path. What refuses a downgrade is **not** a comparison with the current cost (every historical entry is weaker than current — that is what raising means) but a fixed `ARGON2_FLOOR` equal to the first set ever shipped, which never moves: appending `m=8` makes it *current* and reddens `Argon2PhcEncodeTest`'s literals, inserting it breaks the strict ascent in `m × t`, and **prepending** it — which an ascent check alone allows — is below the floor. The rehash lives in **`PostgresCredentials.verify`** as one compare-and-set `UPDATE` whose every `SQLException` is swallowed, never in `matches` (no `DataSource` in the hasher) and never in the endpoint (`ADR-0027` §1). The trigger is the first edit to the three cost constants, and §8 lists that PR's eight items — including re-minting `DUMMY_PHC`, which otherwise silently guts `ADR-0027` §6's enumeration defence in two different ways with every test green, and re-deriving `ARGON2_MAX_PARALLEL`, since 19456 → 65536 KiB takes peak memory across four slots from ~76 MiB to ~256 MiB on a host `ADR-0027` sized for 19. Costs recorded rather than discovered: `Credentials.verify` stops being a read while its signature still says `PlayerId?`; the weakest parameters ever shipped stay acceptable forever under a green test asserting they are accepted; and **the raise protects active accounts only** — a dormant row keeps the old cost indefinitely, and the only thing that fixes it is a forced reset, which `ADR-0031`'s optional recovery makes an account deletion and which is therefore **the product owner's call** on the day it is wanted |
| `DEC-043` | [ADR-0048](../../docs/adr/ADR-0048-a-password-has-one-rule-and-it-is-length.md) | One rule — **8 to 128 code points**, counted after NFC normalisation — and nothing else: no composition rule, no character rule, no breach corpus, no strength meter, and nothing is trimmed. `STORY-0404` enforces the minimum at sign-up and answers **`422`** with an empty body, distinct from the handle's `400` and `409`; `STORY-0405` applies only the **maximum**, before Argon2 and before the identifier lookup, answering exactly as a wrong password does. `STORY-0416`'s reset uses the identical rule. **NFC is applied in the one place a secret becomes bytes**, so sign-up and sign-in cannot disagree — permanent from the first stored hash. `STORY-0403` is unaffected: `TASK-040307`'s *"no `init`, no `require`"* stands and ASCII is NFC-invariant, so the published-vector tests do not move. `STORY-0412` states the rule before the field is filled and shows no meter |
| `DEC-048` | [ADR-0055](../../docs/adr/ADR-0055-sign-up-is-budgeted-by-address-and-over-budget-says-so.md) | **Budgeted by remote address, and over budget answers `429`** — not one of the four lies. The budget meters **the hash, not the request**: 5 requests per address per rolling 15 minutes may reach `Credentials.create`, and `401`/`400`/`422`/the guard's `409` consume nothing, because `ADR-0022` and `ADR-0027` §6 meter failure to defend a *search space* while this meters spending to defend a *rate*. Only `201` and the **taken-handle `409`** cost Argon2 — `PostgresCredentials.create` hashes before the insert finds the collision — and the taken-handle path writes no row, so one device id and one known handle is an **unbounded** stream of hashes, while success is self-limited by `holdsCredential`. The `429` is a real disclosure shipped knowingly: weaker than the `409` `ADR-0031` §5 already spends, unreadable without being spent, and pacing under a rate budget is compliance rather than evasion. Address alone — device id and player dilute by free minting, and the pair is a conjunction that helps the NAT case not at all. **Nothing new guards the pool**; a depth-capped queue is deferred on a measurement. **`STORY-0404` is unchanged**, its three out-of-scope notes stand verbatim, and **`STORY-0405` builds `AttemptBudget`, two config values and `ADR-0027` §6's sign-in budget on one type** — this project has no limiter at all today, `ADR-0022`'s included. Due date corrected to a condition: no deployment may expose the endpoint without it. Costs: five requests deny sign-up to a whole NAT; the `429` is precedent; the budget map has no hard cap; successful sign-ins stay unmetered. Raises `DEC-049` |
| `DEC-049` | [ADR-0056](../../docs/adr/ADR-0056-a-throttled-sign-up-says-so-and-keeps-what-was-typed.md) | **The `429` is a third kind of outcome on the sign-up form, and never the generic failure.** Success, a refusal about something the player typed, and this — a refusal about neither them nor their input. `500`, `503`, a timeout and a rejected `fetch` keep the *unavailable* treatment `web-client/src/profile/api.ts` already gives an unknown status; no other status joins the throttled state. The mapping is the substance, because the wrong answer is the **absence of a branch**: today's client maps everything but `200` and `401` to *unavailable*, so a deliberate refusal would read as a broken product and produce the immediate retry that `ADR-0055` §1 says **extends the window**. The copy may state three facts — not right now and **from this connection**; **nothing you typed was refused**; nothing is lost and nothing is required (`ADR-0036`) — and never five: **no time** (no deadline, no countdown, no *15 minutes*, no *5 attempts*: there is no `Retry-After`, the window rolls, and the numbers are env vars that go stale silently), no field verdict, **no claim the handle is still free** (the budget check sits before `Credentials.create`, so the taken-handle test was never reached), no accusation, and no mechanism or fault language. The form **keeps both fields including the password**, marks neither, leaves submit enabled, offers no *Retry now*, and **never retries by itself**. **One message for two people, written for the bystander** — nothing distinguishes the second player behind a NAT and nothing will, so the product will never tell an abuser to stop. A `429` spends nothing, **including `ADR-0036`'s offer**, which only *"Not now"* dismisses. `STORY-0404` gains **nothing**; `STORY-0405` gains **nothing on the wire and one prohibition** — the body stays empty by product decision too, so nobody adds a `message` field to help the screen; `STORY-0412` gains the state, the mapping, the preserved fields, the copy brief and five criteria (four statuses asserted together; both field values after a `429`; no field marked; no request without a further submit; **the message contains no digit**); `STORY-0415` gains one prohibition. Costs: `ADR-0055`'s disclosure gets **louder**, printed in a sentence rather than hidden in a status; the player is told to wait and never how long; a typed password sits in a form on the shared machine that produced the refusal; the only deterrent a message could carry is given up; a state that may never render in v0.1 is designed and maintained; and an `EPIC-07` front-end `429` is indistinguishable from this one |
| `DEC-050` | [ADR-0057](../../docs/adr/ADR-0057-a-cursor-names-the-filter-it-was-drawn-under.md) | The encoded cursor gains a third component — an 11-character fingerprint of the filter it was drawn under — checked by the re-encode line that already decides validity, so a cursor replayed under another filter is a flat `400`. `STORY-0409`'s eleven tickets are **unchanged**; the story is re-planned on top of them for four more (`DuelFilter` fingerprint plus a golden vector, the three-part cursor, the route's new filter-then-cursor order, and the document). `STORY-0413` must clear its cursor whenever the filter changes and treat a `400` on `after` as *restart the walk*, never as an error shown to the player. Deadline: before `STORY-0413` ships, because no protocol version negotiates the cursor's encoding |
| `DEC-052` | [ADR-0059](../../docs/adr/ADR-0059-the-record-is-searched-when-the-player-submits.md) | **The search fires on submit; typing sends nothing.** The box sits in a `<form>`, and exactly two acts search: Enter in the box, and a submit button reading `SEARCH = "Search"` — the word `TASK-041305` reserved for the ticket that needs it. Emptying the box is a search like any other, and no separate *Clear* control is added. `ADR-0057` binds the cursor to its filter and `TASK-041307` drops the rows with it, so a debounced pause is not a wasted request — it discards the player's place in the record and searches a term (*Hal*) they had not finished typing (*Halvard*); it would also fire an unindexed `POSITION` scan per pause against a read `STORY-0409` recorded as *"not yet fast"*, and put the first timer **inside** the component tree, where the client has never had one. Chosen as the cheaper reversal on thin evidence: a debounce added later is additive, one removed later is a behaviour players learned. `TASK-041312` gains one Scope bullet, the word, the test `asks nothing while the player types, and once when the search is submitted` and one `verify:` line — **three files still**, because finding the button by its accessible name pins the string. Costs: the box and the rows can disagree with nothing saying so; widening back takes a deliberate submit; it feels older than every other search box the player used today |
| `DEC-053` | [ADR-0060](../../docs/adr/ADR-0060-the-record-is-its-own-screen-and-the-lobby-is-the-door.md) | **The record is its own screen, replacing the first screen — in by one control reading *Your duels*, out by one reading *Back*.** The door sits beneath the profile strip and outside it, is a `<button>` while the client has no URLs, takes its word from `HISTORY_HEADING` so the destination has one spelling, and **does not depend on the profile read** — the strip renders `null` when that read fails, and the way to a screen may not vanish with it (`ADR-0036` gates nothing else either). *Back* is rendered by the swap, **never by `HistoryScreen`**, so the screen knows nothing about navigation and the affordance is assertable with no transport. The door is offered only on the branch holding *Create a duel room*, and a duel that starts outranks the record, because a player who opened it mid-hand would not fold — a `PLAYING` room is never reaped for idleness — they would simply leave their rival at a table nothing ends. The lobby does not grow a section: `STORY-0412`, `STORY-0415` and `EPIC-05` all inherit this, and four stacked sections are the whole product on the screen a new player sees first — cheap one at a time, irreversible in aggregate. `TASK-041313` gains the door, the way back and the test `leaves the first screen for the record, and comes back to it`; its *"exactly one of `App.tsx` and `Lobby.tsx`"* criterion falls to `Lobby`, which already knows whether a duel is in progress. Costs: the first screen becomes the only door and will crowd; the record has **no address**, so nothing links to it, a reload lands on the first screen and browser *Back* leaves the client; `STORY-0415`'s offer opens a screen from the **result** screen, so whatever holds *which screen is showing* must be reachable from there too. **Raises `DEC-054`** |

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
