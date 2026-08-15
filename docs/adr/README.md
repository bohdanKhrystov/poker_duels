# Architecture decision records

One file per significant decision. Small, dated, immutable.

An ADR is never edited to change its decision. If a decision is reversed, write a new ADR that
supersedes the old one and mark the old one `Superseded by ADR-NNNN`. The wrong turns are as
valuable as the right ones — especially for Product B.

## When to write one

Write an ADR when a choice:

- constrains something outside its own module,
- was contested, or had a plausible alternative,
- will make a future reader ask *"why is it like this?"*.

Do not write one for choices the code already makes obvious.

## Naming

`ADR-NNNN-short-kebab-title.md`, sequential, never reused.

## Template

```markdown
# ADR-NNNN — Title

- **Status:** Proposed | Accepted | Superseded by ADR-MMMM
- **Date:** YYYY-MM-DD

## Context
The forces at play. What makes this a real decision.

## Decision
What we are doing. Present tense, unambiguous.

## Consequences
What this buys, what it costs, and what it forecloses.

## Alternatives considered
Each with the reason it was not chosen.
```

## Index

| ADR | Title | Status |
| --- | --- | --- |
| [0001](ADR-0001-event-sourced-engine-contract.md) | Event-sourced engine contract | Accepted |
| [0002](ADR-0002-server-authoritative.md) | The server is authoritative | Accepted |
| [0003](ADR-0003-technology-stack.md) | Technology stack | Accepted |
| [0004](ADR-0004-branching-and-ticket-workflow.md) | Branching and ticket workflow | Accepted |
| [0005](ADR-0005-analysis-behind-an-interface.md) | Hand analysis sits behind an interface | Accepted |
| [0006](ADR-0006-mandatory-review-gate.md) | Every task ends in a reviewed, merged pull request | Amended by 0007 |
| [0007](ADR-0007-token-lean-agent-workflow.md) | Token-lean agent workflow | Accepted |
| [0008](ADR-0008-loser-mucks-at-showdown.md) | The loser mucks at showdown | Accepted |
| [0009](ADR-0009-match-events-are-their-own-hierarchy.md) | Match events are their own hierarchy | Accepted |
| [0010](ADR-0010-engine-takes-a-serialization-dependency.md) | The engine may depend on kotlinx.serialization | Accepted |
| [0011](ADR-0011-postgres-in-v01.md) | PostgreSQL lands in v0.1 | Accepted — amends 0003 |
| [0012](ADR-0012-device-bound-anonymous-profiles.md) | Anonymous profiles, bound to a device | Accepted |
| [0013](ADR-0013-disconnect-grace-period.md) | A dropped connection gets a grace period, then folds | Amended by 0023, 0028 |
| [0014](ADR-0014-duel-coin-economy.md) | The winner takes a coin, the loser gives one, a draw pays nothing | Accepted |
| [0015](ADR-0015-a-draw-writes-two-result-rows.md) | A draw writes two result rows of zero, not no rows | Accepted |
| [0016](ADR-0016-a-room-is-serialised-by-its-own-mutex.md) | A room is serialised by its own mutex, not by an actor | Accepted |
| [0017](ADR-0017-the-server-says-when-a-duel-ends.md) | The server says when a duel ends | Accepted |
| [0018](ADR-0018-a-second-socket-adopts-the-seat.md) | A second socket adopts the seat, and the first is closed | Accepted |
| [0019](ADR-0019-the-duel-table-records-hands-played.md) | The duel table records how many hands were played | Accepted |
| [0020](ADR-0020-typescript-protocol-from-serial-descriptors.md) | TypeScript protocol types are emitted from the serial descriptors | Accepted |
| [0021](ADR-0021-a-profile-gains-a-display-name.md) | A profile gains a player-chosen display name | Accepted |
| [0022](ADR-0022-the-room-code-is-the-invite.md) | The room code is the invite, and failed joins are budgeted | Accepted |
| [0023](ADR-0023-an-absent-seat-checks-when-nothing-is-owed.md) | An absent seat checks when nothing is owed, folds when facing a bet | Accepted — amends 0013; amended by 0028 |
| [0024](ADR-0024-design-follows-the-code-workflow.md) | Design follows the code workflow, in the repository, mirrored to claude.ai/design | Accepted — §2's "size" bounded by 0033 |
| [0025](ADR-0025-one-ticker-coroutine-drives-both-sweeps.md) | One ticker coroutine on the application scope drives both sweeps | Accepted |
| [0026](ADR-0026-vite-and-npm-drive-the-web-client.md) | Vite and npm drive the web client, and its checks are their own CI job | Accepted |
| [0027](ADR-0027-the-session-outranks-the-device-id.md) | A session token outranks a device id, and the handshake carries it | Accepted — amended by 0030 |
| [0028](ADR-0028-the-wire-names-an-absent-opponent.md) | The wire names an absent opponent, and marks every action taken for one | Accepted |
| [0029](ADR-0029-a-display-name-is-unique-and-permanent.md) | A display name is unique, case-insensitively, and permanent once set | Accepted — amended by 0038 |
| [0030](ADR-0030-a-claim-adds-a-credential-and-moves-nothing.md) | A claim adds a credential and moves nothing; a sign-in swaps identity and moves nothing | Accepted — amended by 0037 |
| [0031](ADR-0031-an-optional-verified-recovery-email.md) | An optional recovery email, proven before it can do anything, and a handle that is not a name | Accepted |
| [0032](ADR-0032-react-subscribes-to-a-store-it-does-not-own.md) | React subscribes to a store it does not own, and the tab's one connection is wired at boot | Accepted |
| [0033](ADR-0033-component-anatomy-is-born-in-its-canonical-card.md) | A component's anatomy is born in its canonical card; the sheet holds the vocabulary | Accepted — bounds 0024 §2 |
| [0034](ADR-0034-the-value-gate-reads-css-string-aware.md) | The value gate reads CSS regions, string-aware, and fails rather than guesses | Accepted |
| [0035](ADR-0035-a-duel-is-a-freezeout.md) | A duel is a freezeout | Accepted |
| [0036](ADR-0036-an-account-is-offered-never-required.md) | An account is offered after a first win, and never required | Accepted |
| [0037](ADR-0037-the-device-is-a-credential-until-revoked.md) | The device stays a credential until the player revokes it | Accepted — amends 0030 |
| [0038](ADR-0038-a-name-is-screened-when-set-and-can-be-taken-away.md) | A display name is screened when set, can be taken away, and is burned when it is | Accepted — amends 0029 |
| [0039](ADR-0039-v01-offers-no-account-deletion.md) | v0.1 offers no account deletion, and the schema keeps every answer open | Accepted |
| [0040](ADR-0040-a-duel-may-be-watched-without-hole-cards.md) | A duel may be watched live, minus every hole card | Accepted |
| [0041](ADR-0041-a-handle-and-a-password-are-the-only-credential.md) | A handle and a password are the only credential v0.1 ships | Accepted |

## Open decisions

Questions deliberately left open are marked `DEC-NNN` in the document they affect.

**Who answers one** — see [`docs/workflow.md`](../workflow.md#who-answers-a-dec):

| Kind | Answered by |
| --- | --- |
| **Technical** — where a type lives, schema shape, wire format, concurrency, failure semantics | the `architect` agent |
| **Product** — what a player sees, what a duel *is*, which risks inside the software are acceptable | the `product-owner` agent, deriving from [`docs/vision.md`](../vision.md) |
| **Vision** — money; what the product *is* or *is not*; the roadmap's shape; risk with consequences outside the software | the human, and nothing else |

A `DEC-NNN` can be listed in **four** places: this file, [`tasks/BOARD.md`](../../tasks/BOARD.md),
and the `## Open decisions` table of any epic under `tasks/epics/`. The PR that answers a decision
**strikes every row it answers, in the same PR.** A strike deferred to somebody else's next PR is a
strike nobody makes — `DEC-035` sat listed open and answered simultaneously for weeks that way.

| ID | Question | Where | Due |
| --- | --- | --- | --- |
| DEC-002 | What performance budget does the hand evaluator carry, how is it measured, and does `HandRank` become a packed integer? | `../../tasks/stories/STORY-0103-hand-evaluator.md` | before STORY-0108 |
| DEC-008 | Is the full `MatchLog` persisted in v0.1, and where — a column, a table per hand, or object storage? | `../../tasks/stories/STORY-0209-postgres-schema-and-migrations.md` | before EPIC-08 |
| DEC-023 | **The architect's** — how does a rematch reach the server from a browser? `RoomRegistry.offerRematch` exists and `ADR-0022` gives a finished room five minutes to accept one, but the wire has no rematch message in either direction: `ClientMessage` is `Hello \| CreateRoom \| JoinRoom \| Act`, and nothing tells a seat that the opponent has offered or that a rematch has begun. Does the protocol gain `OfferRematch` and a server-side offered/started frame — and does `PROTOCOL_VERSION` move — or does the client re-`JoinRoom` a finished room, or is it HTTP? And which epic owns the server half? | [`STORY-0309`](../../tasks/stories/STORY-0309-rematch.md) | before STORY-0309 |
| DEC-024 | **The architect's** — does `EPIC-03` ship an automated browser end-to-end test: two real browser contexts against a running server and database, playing a duel to a winner? If so, what runs it and in which CI job, given `./gradlew check -PrequireDocker=true` already carries the socket-level end-to-end suite. If not, the two-browser proof is a manual check in v0.1 and `STORY-0312`'s frame-driven test is the automated ceiling. | [`EPIC-03`](../../tasks/epics/EPIC-03-web-client.md) | before EPIC-03 closes |
| DEC-032 | **The architect's** — how does the client test browser storage, and is the developer Node version pinned? Node 24+ defines an inert `localStorage` global (absent `--localstorage-file`) that shadows jsdom's under Vitest, so `typeof localStorage === "undefined"` while `sessionStorage` works. `TASK-030304` sidesteps it by injecting a `Storage`, which its own contract already required — but `TASK-030307`, `TASK-030309` and `STORY-0311` each touch storage, and `STORY-0311` sends the same value as an HTTP header. Open: does anything ever exercise a *real* `Storage`, and does the repo pin a developer Node version rather than only CI's (`.nvmrc` says 24, this workstation runs 26)? `DEC-022` foresaw the split; this is the first time it has bitten. | [`TASK-030304`](../../tasks/tasks/TASK-030304-the-device-id-lives-under-one-key-this-module-owns.md) | before STORY-0311 |

## Answered decisions

| ID | Question | Answered by |
| --- | --- | --- |
| DEC-003 | When the big blind cannot cover its own blind, does the bar for the round stand at the nominal big blind or at the amount actually posted? | [`../duel-rules.md`](../duel-rules.md) — the bar is what was actually posted, so a big blind all-in for 60 at 50/100 leaves 60 to match and the small blind owes 10 more rather than 50. Most cardrooms hold the bar at the full blind; we do not, because the number a player faces should be genuinely at stake. A rules call, not an architecture one, so it is written into the rules rather than an ADR |
| DEC-004 | Whose cards are shown at showdown, and does the loser's hand ever become public? | [ADR-0008](ADR-0008-loser-mucks-at-showdown.md) — the loser mucks: only the winner's hand is revealed, and a mucked hand appears in no event, for anyone |
| DEC-005 | Where does a match-level event live? | [ADR-0009](ADR-0009-match-events-are-their-own-hierarchy.md) — its own `MatchEvent` hierarchy |
| DEC-006 | Where does event-log serialisation live, and in what format? | [ADR-0010](ADR-0010-engine-takes-a-serialization-dependency.md) — kotlinx.serialization, inside the engine, behind a narrowed guard |
| DEC-007 | How are the TypeScript protocol types generated, and what stops the checked-in output drifting? | [ADR-0020](ADR-0020-typescript-protocol-from-serial-descriptors.md) — an owned emitter over the `SerialDescriptor`s; a byte-comparing verify task on `check` fails CI on drift |
| DEC-010 | Do room and lobby messages belong to STORY-0202's protocol, or extend the sealed hierarchies? | [ADR-0017](ADR-0017-the-server-says-when-a-duel-ends.md) — later stories extend the existing hierarchies |
| DEC-011 | A device opens a second socket while one is live — refuse, adopt, or allow both? | [ADR-0018](ADR-0018-a-second-socket-adopts-the-seat.md) — the new socket adopts the seat, the old is closed |
| DEC-013 | Is a per-room `Mutex` enough once a duel runs inside the room? | [ADR-0016](ADR-0016-a-room-is-serialised-by-its-own-mutex.md) — the mutex stays; no actor |
| DEC-014 | Does the `duel` table gain a `hands_played` column? | [ADR-0019](ADR-0019-the-duel-table-records-hands-played.md) — yes, now, via V2 while the table is empty |
| DEC-015 | How does the end of a duel reach a client? | [ADR-0017](ADR-0017-the-server-says-when-a-duel-ends.md) — a new `ServerMessage.DuelFinished` |
| DEC-012 | Is holding a room code sufficient authorisation to take the second seat, or does joining need rate limiting or host confirmation? | [ADR-0022](ADR-0022-the-room-code-is-the-invite.md) — the code is the invite (the human's call); `RoomRegistry.join` budgets failed attempts at 10 per player per minute |
| DEC-016 | What names the opponent in a result line? | [ADR-0021](ADR-0021-a-profile-gains-a-display-name.md) — a profile gains a player-chosen display name (the human's call); nullable `player.display_name`, joined into the read path; product rules split to `DEC-017` |
| DEC-020 | What does an absent seat do at a decision point where `Fold` is illegal? | [ADR-0023](ADR-0023-an-absent-seat-checks-when-nothing-is-owed.md) — it checks; fold only when facing a bet, the action read from the engine's `legalActions`; `poker-engine` unchanged; amends ADR-0013 |
| DEC-021 | How is design work run — conversationally, or through the ticket lifecycle? | [ADR-0024](ADR-0024-design-follows-the-code-workflow.md) — ordinary ticketed work; `design/` is canonical and claude.ai/design is a render surface |
| DEC-019 | What drives `RoomRegistry.reap()` and `expireGracePeriods()` in production, with what period, scope and failure behaviour? | [ADR-0025](ADR-0025-one-ticker-coroutine-drives-both-sweeps.md) — one ticker coroutine on the application scope in `module()`; one configured period (`sweepPeriodMillis`, default 1 s), fixed delay, expiry then delivery then reap; a throwing pass is logged and retried next tick, only cancellation at shutdown ends the loop |
| DEC-022 | What is `web-client`'s toolchain, and how do its checks enter CI? | [ADR-0026](ADR-0026-vite-and-npm-drive-the-web-client.md) — Vite + npm on Node 24 (pinned once, in `web-client/.nvmrc`; CI governs); Vitest, ESLint + Prettier with `src/protocol/protocol.gen.ts` ignored by path in both; Vite's proxy carries `/api` and `/ws` to Ktor on 8080; a parallel `client` CI job runs `npm ci`, `npm run check`, `npm run build`, and Gradle stays JVM-only |
| DEC-028 | What is the credential, session and handshake model once a device id is no longer the only credential? | [ADR-0027](ADR-0027-the-session-outranks-the-device-id.md) — a session token outranks a device id and never falls back to it; the token is opaque, stored as its SHA-256 with a 30-day absolute expiry, and presented in `Hello.sessionToken` on the socket and `Authorization: Bearer` over HTTP; `PROTOCOL_VERSION` moves to 3 once, in STORY-0405. What a credential *contains* stays with DEC-027 |
| DEC-018 | Does a player see anything while the duel is paused around them? | [ADR-0028](ADR-0028-the-wire-names-an-absent-opponent.md) — yes: `OpponentPresence` carries PRESENT/AWAY/ABSENT plus a remaining-millis countdown the client renders but never acts on, and `ActedForAbsentSeat` marks **every** action the server took for an absent seat, check as well as fold. Retracts the wire half of ADR-0023's indistinguishability. |
| DEC-026 | What does a claim convert, and what happens when a device with an anonymous profile signs into a different account? | [ADR-0030](ADR-0030-a-claim-adds-a-credential-and-moves-nothing.md) — a claim attaches a credential to the `player` row already resolved (no second profile, no copied `duel_result`, no `UPDATE player`); signing into another account writes only an `auth_session`, leaving `player.device_id` pointing at the anonymous profile, so sign-out restores it by subtraction. No schema change. Amends ADR-0027 |
| DEC-027 | Does an account carry an email, what may it be used for, and can a password be recovered without one? | [ADR-0031](ADR-0031-an-optional-verified-recovery-email.md) — an optional address in its own `recovery_email` table holding **only verified** addresses, feeding a single-use one-hour reset token that revokes every session when consumed. A player signs in with a lowercase login **handle**, never the display name and never the email. Declining the email means no recovery path at all |
| DEC-033 | How does React read the duel store, and where does the single `Connection` live? | [ADR-0032](ADR-0032-react-subscribes-to-a-store-it-does-not-own.md) — one `DuelStore` and one `Connection` per tab, created by framework-free boot wiring (`bootDuelClient` in `src/store/boot.ts`) that `main.tsx` calls once, outside the tree; components read through `useDuelState()` (`useSyncExternalStore` from React core — no store library) and send through `useSend()` from event handlers only; message-triggered sends are boot reactions, so `JoinRoom` after `Welcome` is exactly-once under StrictMode by construction, and no screen ever holds the `Connection` |
| DEC-034 | Do a component's internal em ratios — the wordmark lockup's four — fall under ADR-0024 §2's "every size is born in the sheet", or may they live in the canonical card behind a drift-gate clause? (Minted as `DEC-032` on `tasks/BOARD.md`; renumbered here because this register had taken 032 first — ADR-0033's immutable header cites the original minting) | [ADR-0033](ADR-0033-component-anatomy-is-born-in-its-canonical-card.md) — the anatomy is card-born in `wordmark.html` with the gate pinning every copy's four values; colors stay unconditionally sheet-ward, and any value gaining a second independent consumer is promoted by ordinary ticket |
| DEC-035 | How does `design/check-drift.sh` read a CSS value? (Registered by #523 in **both** registers; `ADR-0034` recorded the answer here but struck neither open row, and its header's claim that the row lived only on the board is wrong — an ADR is immutable, so the correction lives here. Both open rows are struck by the change that adds this line) | [ADR-0034](ADR-0034-the-value-gate-reads-css-string-aware.md) — the awk regex `--pd-NAME:[^;]*;` is deleted and the reader becomes a third stock-perl walker, `VALUES`, scoped to `<style>`/`.css` regions, comment-stripping, string- and escape-aware, ending a declaration at a top-level `;` or its block's `}`, collapsing in-string whitespace so a wrapped string equals its one-line spelling — and refusing, by name, any shape it cannot read rather than returning a partial set. Two premises of the DEC's registration are corrected there: `ADR-0024` carries no stock-tools constraint, and the gate already runs two quote-aware perl walkers |
| DEC-001 | What exactly is one duel? | [ADR-0035](ADR-0035-a-duel-is-a-freezeout.md) — a freezeout: 100 BB, blinds rising every 10 hands, one player holds every chip. **The human's call.** The numbers stay `DuelFormat` configuration and the escalating schedule stays load-bearing, since `TASK-010715`'s termination property depends on it. Fixed-length duels remain implemented and available as configuration |
| DEC-009 | Can a duel be watched, and if so what may a spectator see and when? | [ADR-0040](ADR-0040-a-duel-may-be-watched-without-hole-cards.md) — live, with no hole card that showdown has not revealed. **The human's call.** A spectator has no seat, so the projection gains a third entry point in the engine beside `PlayerView.of` and `visibleTo` — never a sentinel seat and never a filter in transport — carrying its own leak property. Nothing is built; no epic owns spectating yet |
| DEC-017 | Is a display name refused when it is set, and can one ever be taken away? | [ADR-0038](ADR-0038-a-name-is-screened-when-set-and-can-be-taken-away.md) — a blocklist screens at set time, an operator may force-rename afterwards, and a name taken away is **retired forever** rather than released. **The human's call.** Uniqueness gains two more sources of truth; `ADR-0029`'s permanence becomes permanence *to the player*. Homoglyph impersonation is explicitly not solved. Unblocks `STORY-0410` |
| DEC-025 | Is an account ever required to play, or is anonymous play permanent? | [ADR-0036](ADR-0036-an-account-is-offered-never-required.md) — never required; anonymous play stays fully ranked, and the client offers an account after the player's **first win**, dismissible permanently. **The human's call.** `EPIC-04` gains one prompt story and gates nothing |
| DEC-029 | May a player delete their account, and what becomes of the opponent's history rows? | [ADR-0039](ADR-0039-v01-offers-no-account-deletion.md) — no deletion in v0.1, recorded as a position rather than left implied. **The human's call.** `STORY-0403`'s schema must keep both answers reachable, which forbids denormalising a display name into `duel_result` — the constraint with a real cost |
| DEC-030 | After a credential is attached, may the device id still sign in? | [ADR-0037](ADR-0037-the-device-is-a-credential-until-revoked.md) — yes, until the player revokes it from the account screens; revoking does not end the revoking session, and is offered only once a credential exists. **The human's call**, framed as risk acceptance. The account screens must state which routes are live. Amends `ADR-0030` |
| DEC-031 | May a player ever sign in with a third-party account, or is a handle and password the only credential? | [ADR-0041](ADR-0041-a-handle-and-a-password-are-the-only-credential.md) — handle and password only for v0.1 and v0.2, and the account screens are designed for one credential. **The human's call.** Explicitly *not now* rather than *never*: `ADR-0027`'s `credential.kind` is untouched, so adding a provider stays additive in the schema and costs a screen redesign |
