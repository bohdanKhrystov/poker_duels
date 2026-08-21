---
id: EPIC-05
title: Ranking, duel coins and leaderboard
type: epic
status: ready
module: poker-server, web-client
labels: [server, client, leaderboard, seasons, read-path]
---

## Goal

`docs/vision.md` sells v0.3 in three words — *"Leaderboard and seasons"* — and describes the thing
itself in one line: *"A leaderboard. Ranked results over a season."* This epic is that line and
nothing past it. A player opens the ladder from the first screen, reads an ordered list of rivals
against the duel coins the server already pays, finds where they stand, and watches a duel won move
them up it.

The coin is finished work and this epic does not reopen it.
[`ADR-0014`](../../docs/adr/ADR-0014-duel-coin-economy.md) fixes its arithmetic at `wins − losses`
— signed, unclamped, negative when it should be —
[`ADR-0015`](../../docs/adr/ADR-0015-a-draw-writes-two-result-rows.md) writes a draw as two rows of
zero rather than as nothing, and `EPIC-02` has been paying both since 2026-08-14. **What this epic
adds is an ordering, a scope for that ordering, and the one screen that shows it.**

When the epic closes, one test plays a duel between two profiles and the ladder moves both of them
by exactly what `ADR-0014`'s arithmetic says — one coin up, one coin down — with nothing minted and
nothing destroyed in between. That test is the epic; everything else is how it is made to pass.

## Why now

**The roadmap says so.** v0.2 is `EPIC-04` and v0.3 is this. Nothing before it is waiting on it,
and it is the last milestone the vision describes before *"Friends, statistics, replay viewer"*.

**Two ADRs wrote data for this epic and have been waiting.**
[`ADR-0014`](../../docs/adr/ADR-0014-duel-coin-economy.md) chose a signed net balance over a win
count with this in mind, in as many words: *"the leaderboard in `EPIC-05` will be sorted by
whichever one this decision produces."*
[`ADR-0029`](../../docs/adr/ADR-0029-a-display-name-is-unique-and-permanent.md) kept an ordinary
deterministic collation on `display_name` so that *"`ILIKE` and a pattern-ops index remain
available to `EPIC-05`'s leaderboard and to `STORY-0409`"*. Both are already in the schema.

**A gate falls due.** [`ADR-0012`](../../docs/adr/ADR-0012-device-bound-anonymous-profiles.md)
records that device ids are trivially minted, that *"against a ranked ladder with coins that is a
farming and smurfing vector"*, that it is acceptable while there is no public leaderboard, and that
*"it must not still be true when the leaderboard goes public — that is a gate on `EPIC-05`,
recorded here so it is not rediscovered late."* This is the epic where it stops being somebody
else's problem.

**`EPIC-04` parked two things here by name**: smurf and multi-account defence, and *"viewing
another player's profile or history … it needs a name per leaderboard row and owns what a row links
to. Here, `/api/me` means me."*

**And the ladder now has names to print.** A leaderboard of UUIDs is not a leaderboard.
`STORY-0401`, `STORY-0402`, `STORY-0410`, `STORY-0411` and `STORY-0413` have all merged, so
`player.display_name` exists, the read path carries it, `nameOrNone` prints `No name` where one is
absent ([`ADR-0058`](../../docs/adr/ADR-0058-where-a-name-would-be-the-client-prints-no-name.md)),
and [`ADR-0060`](../../docs/adr/ADR-0060-the-record-is-its-own-screen-and-the-lobby-is-the-door.md)
has already shown the client how a second screen is reached and left. **This epic needs none of
`EPIC-04`'s credential chain** — `ADR-0036` makes an anonymous profile fully ranked — so the
stories still open there, including everything queued behind `STORY-0405`, gate nothing here.

## Scope

Stated as obligations rather than as design. Four of the seven had their shape fixed by
[`ADR-0061`](../../docs/adr/ADR-0061-a-season-is-a-calendar-month-and-the-coin-never-resets.md),
which answers `DEC-055`:

- **A season, represented once.** Something the server can name and bound, and a rule that
  attributes a finished duel to exactly one of them. `ADR-0061` §1–§3: a **derived range**, one
  calendar month in UTC, half-open, identified by its month, stored nowhere.
- **The standings read path.** An ordered, paged, server-computed ladder over plain HTTP, whose
  walk states its own guarantee rather than inheriting `GET /api/me/duels`'.
  [`ADR-0066`](../../docs/adr/ADR-0066-the-ladder-is-computed-per-request-and-a-walk-is-pinned.md)
  answers `DEC-061`: the standing is computed **per request** and a walk is **pinned to the instant
  it began**, so it returns every player of the ladder as it stood at that cutoff exactly once — and
  is not live, and carries one named exception.
  [`ADR-0065`](../../docs/adr/ADR-0065-the-ladder-hands-a-player-their-own-row.md) adds one
  obligation to it and answers `DEC-059`: the same response also tells the requesting player where
  **they** stand, so this read carries **two aggregates**, not one.
- ~~**Whatever gates a place on it.**~~ Discharged by
  [`ADR-0063`](../../docs/adr/ADR-0063-nothing-gates-a-place-and-the-farm-is-accepted-until-the-ladder-is-public.md),
  which answers `DEC-056`: **nothing gates a place** — the ladder is `ADR-0061` §4's set narrowed by
  nothing, a nameless player has a row that reads `No name`, and two profiles that only ever duel
  each other are an ordinary pair. `ADR-0012`'s gate is discharged as the second of the two shapes
  this obligation allowed — a **written, dated acceptance** of the residual risk, expiring at an
  event rather than a date: the first time the ladder is served on a public address.
- **The ladder as a screen**, reached from the first screen and left the way `ADR-0060` says a
  second screen is left — and, by `ADR-0065`, a screen that hands the player their own row in a
  self line above the rows rather than asking them to find it.
- **Whatever a row leads to** (`DEC-057`), including nothing.
- ~~**What happens when a season ends**, and what of the record survives it.~~ `ADR-0061` §5: a
  boundary does **nothing** — nothing is written, reset or archived, and the record survives it by
  never having been touched. This obligation is discharged by a decision rather than by code, which
  is why `STORY-0505` is `dropped`.
- **One end-to-end test** that plays a duel and reads both players' positions afterwards.

## Out of scope

| Not here | Where |
| --- | --- |
| A rating, an Elo, points, or any award weighted by opponent strength | Nowhere yet, and deliberately. [`ADR-0014`](../../docs/adr/ADR-0014-duel-coin-economy.md) says a floating balance *"is a new ADR **superseding** this one"*. The word *Ranking* in this epic's title means **a position on a ladder sorted by coins**, not a second number — inventing one here would silently supersede a merged ADR |
| A second currency, chip purchases, bonuses, streak rewards, anything to spend a coin on | Nowhere. `docs/vision.md`, *What it is not* |
| Matchmaking, ranked pairing, a public room list | **later**, by the vision's own roadmap row. A ladder ranks players; it does not pair them. `STORY-0206` said *"`EPIC-05` and later"* and the roadmap settles which |
| Friends, rivals, head-to-head statistics, the replay viewer | v0.4 |
| Persisting the full `MatchLog` so a ladder row could open a replay | `DEC-008`, unanswered, and EPIC-08 |
| Putting the leaderboard on the public internet | EPIC-07. This epic **builds** the ladder; `ADR-0012`'s gate is phrased *"when the leaderboard goes public"*, so the deadline for the countermeasure is EPIC-07's deployment, not this epic's last merge. Recorded so the gate is not rediscovered late a second time |
| Any change to the rules of poker, or to `poker-engine` | Nowhere. The engine gains no season, no rank and no coin |
| The visual language of the ladder, any art | EPIC-06. This epic composes `design/tokens/tokens.css`; it authors no colour |
| Live ladder updates pushed over the socket | Nowhere yet. This epic adds no `ServerMessage` and takes no `PROTOCOL_VERSION` step: `docs/protocol.md` says the HTTP endpoints are what *"the lobby reads before any socket exists"*, and a ladder is read by a client that may hold no socket at all. A pushed ladder is a new decision, not an optimisation |
| Making an account required to rank | Nowhere. [`ADR-0036`](../../docs/adr/ADR-0036-an-account-is-offered-never-required.md) is **the human's call** and closes it: an anonymous profile *"takes a leaderboard place, exactly as it does today"* |

## Non-negotiables this epic is most likely to break

- **A rank is a server fact.** [`ADR-0002`](../../docs/adr/ADR-0002-server-authoritative.md). The
  client never sorts a page, never renumbers rows and never derives a position from what it holds.
  A client that numbers rows `1..n` from the page it received is asserting a game fact, and it is
  wrong on page two — which is exactly the shape of bug this rule exists to catch.
  [`ADR-0064`](../../docs/adr/ADR-0064-tied-players-share-one-rank-and-row-order-is-not-a-ranking.md)
  makes the same bug reachable from the **server** side: a rank and a row's position in a page are
  two different numbers, tied players share the first and never the second, and an endpoint that
  numbers a page by its offset is the same defect one layer down.
- **Chip conservation, at ladder scale.** Nothing here mints or destroys a coin. The ladder is a
  *read*, and after `ADR-0061` §5 there is no longer **any** place in this epic that could change a
  balance: a boundary runs no code. A ladder whose arithmetic disagrees with `duel_result` is still
  the defect this epic exists to avoid.
- **The ladder's number is not the profile strip's number, and this is deliberate.** This epic was
  written saying *"a ladder that prints a number the player's own profile strip disagrees with is
  the defect this epic exists to avoid"*, and
  [`ADR-0061`](../../docs/adr/ADR-0061-a-season-is-a-calendar-month-and-the-coin-never-resets.md)
  **contradicts that sentence on purpose**: the strip prints the all-time counter, the ladder prints
  the season standing, and from the second season onwards they differ for anyone who played in both.
  What keeps it honest is the label — §6 requires the ladder to name the season it is showing, taken
  from the response and never from the browser's clock. A row that prints a season standing *without*
  saying it is one is the defect that sentence was reaching for, and it is still a defect.
  [`ADR-0065`](../../docs/adr/ADR-0065-the-ladder-hands-a-player-their-own-row.md) §2 keeps the two
  apart by surface as well as by label: the strip prints the all-time counter and gains **nothing**,
  every number on the ladder screen — rows and the player's own self line alike — is the season
  standing, and `ADR-0060`'s screen swap means the two are never visible at the same time. A season
  number on the profile strip is now a change to that decision, not a field somebody adds.
- **A negative balance is ordinary, and this is the first surface that sorts by one.**
  `ADR-0014`: a new player's first loss puts them at −1, *"that is intended, and it is the case to
  check first when the display work lands"*. Not clamped, not hidden, not filtered off the bottom
  of the ladder, and not an error state.
- **`No name` is one string from one function.**
  [`ADR-0058`](../../docs/adr/ADR-0058-where-a-name-would-be-the-client-prints-no-name.md): the
  client branches on a null display name in exactly one place, `nameOrNone` in
  `web-client/src/profile/name-text.ts`. A ladder row uses it or the decision is already broken.
  **Whether a nameless player has a row at all is a different question** — `ADR-0058` parks it here
  by name — and answering one does not answer the other.
- **No path turns a name into an identity.** `ADR-0029` §7, the reason `GET /api/me/duels` searches
  names and returns *duels*. A leaderboard hands out names and ids together by design, which is the
  first time that rule is pushed from the other side; whatever `DEC-057` permits, it permits
  deliberately and in writing.
- **The engine learns nothing.** No season, rank, ladder or standing type crosses into
  `poker-engine`, and its dependency allowlist does not move. A duel is played by two seats; where
  those seats stand is a server fact and always was.
- **A ladder page is harder than a history page, and the guarantee is stated rather than
  inherited.** `STORY-0408` pinned *total and disjoint* for `GET /api/me/duels` and
  [`ADR-0057`](../../docs/adr/ADR-0057-a-cursor-names-the-filter-it-was-drawn-under.md) bound a
  cursor to the filter that drew it. A standings page is ordered by a number that **changes while it
  is being read** — somebody wins a duel between page one and page two — so that property does not
  travel.
  [`ADR-0066`](../../docs/adr/ADR-0066-the-ladder-is-computed-per-request-and-a-walk-is-pinned.md)
  pays for it explicitly instead: the cursor carries the instant the walk began and the query is
  bounded by that cutoff, so a walk enumerates one fixed ladder exactly once. What it does **not**
  promise is written down with it — a walk is not live, and a duel committed after a page was drawn
  but stamped before the cutoff can still leave one player unreturned and another returned twice.
- **Migrations are immutable**, and this epic may add one. A change to the schema is a new
  `V<n>__` file, numbered at merge time, never an edit to a merged one. `ADR-0061` §3 removes the
  one this epic looked most likely to need: a season is derived, so `STORY-0501` adds no migration.

## The data was already there, whatever the answer

Worth having stated before anyone argued about urgency: **no answer to `DEC-055` needed a backfill,
and no duel already played was lost to any of them.** `duel.finished_at` is `NOT NULL` on every row,
`duel_result.coin_delta` is the signed per-duel award, and `player.coin_balance` is the running
all-time total. So an all-time ladder would have been an `ORDER BY` over a column that exists, and a
ladder over any window whatever is a `SUM(coin_delta)` over a join that exists. `ADR-0061` took the
second, and the note stands as written: the decision was expensive because it fixed what the product
*means*, not because delay cost data.

## Stories

Written on 2026-08-19 with the epic's central product question open — the same way `EPIC-04` was
written, whose own record says it was opened *"when six of its seven decisions were open"* and that
two stories were later added and two re-cut when the answers landed. The same is expected here and
the cost is recorded rather than hidden: **`STORY-0504` and `STORY-0505` may not survive their
decisions**, and the honest end for a story whose premise turns out to be false is `dropped`, not
deleted. `STORY-0505` did not survive `DEC-055` — `dropped` on the same day, file kept, reason on
it.

All six were `blocked` on the day they were written, and all six traced back to `DEC-055`. That was
not a scheduling accident: a query cannot scope itself to a season that has no definition, and a
screen cannot print a number nobody has decided the meaning of. `ADR-0061` answered it the same day,
and the prediction above came true within hours — **one of the two stories named as unlikely to
survive its decision did not**.

| ID | Title | Depends on | Gated by | Status |
| --- | --- | --- | --- | --- |
| [STORY-0501](../stories/STORY-0501-a-season-is-a-bounded-thing.md) | A season is a bounded thing, and every finished duel belongs to one | — | — | **ready** |
| [STORY-0502](../stories/STORY-0502-the-standings-read-path.md) | The standings read path — ordered, paged, and a rank the server computes | 0501 | — | ready |
| [STORY-0503](../stories/STORY-0503-the-ladder-is-a-screen.md) | The ladder is a screen, reached from the first screen and left by one control | 0502 | — | blocked |
| [STORY-0504](../stories/STORY-0504-what-a-row-leads-to.md) | What a row leads to — another player, seen by a stranger | 0503 | `DEC-057` | blocked — may be `dropped` |
| [STORY-0505](../stories/STORY-0505-a-season-ends-and-the-record-survives-it.md) | A season ends, and the record survives it | 0502 | — | **dropped** — `ADR-0061` §5 |
| [STORY-0506](../stories/STORY-0506-a-duel-moves-a-rank.md) | A duel moves a rank, end to end | 0503 | — | blocked |

`STORY-0506` is the only one whose acceptance criteria were writable in full on the day the epic was
written: a duel played through the server moves both players' standings by exactly what `ADR-0014`
says, and the coins on the ladder sum to the coins in `duel_result`. That property held under every
answer `DEC-055` could give, which is why it is the epic's closing test — and it survived the answer
untouched, losing only its dependency on the story that did not.

## What can run in parallel

Almost nothing, and the chain is honest rather than pessimistic:

- `0501 → 0502 → 0503` is a real chain. A query cannot scope itself to something with no
  representation, and a screen cannot render a page the endpoint does not serve yet.
- ~~**`0503` and `0505` are the one genuine parallel pair.**~~ Gone with `0505`. The epic is now a
  straight line, which is a real loss of throughput and the price of there being no crossing to
  write.
- `0504` extends both halves of `0503` and queues behind it rather than beside it.

**Critical path:** `0501 → 0502 → 0503 → 0506`, and it now begins with a ticket rather than with a
decision. That is the single most useful thing to know about scheduling this epic today.

## Questions the story split would have raised, numbered as `DEC-061` and answered

They were left unnumbered because asking them before `DEC-055` would have asked them with the wrong
premise. `ADR-0061` supplied the premise — a standing is a `SUM(coin_delta)` over a window, not an
`ORDER BY` over a column — and the two collapsed into one decision, because the second's answer
follows the first's:

- **The architect's** — is a standing computed per request or materialised? There is no scale problem
  yet and premature caching would be the wrong first move, but the answer differs sharply between a
  window aggregate and a stored column, and `ADR-0061` chose the aggregate.
- **The architect's** — what does a page guarantee over an ordering that is *recomputed* as well as
  moving? Materialise and the ordering is a column again with `ADR-0057`'s discipline nearly intact;
  compute per request and `STORY-0408`'s *total and disjoint* cannot be inherited at all.

`STORY-0502` no longer raises these at split time, and no longer waits either.
[`ADR-0066`](../../docs/adr/ADR-0066-the-ladder-is-computed-per-request-and-a-walk-is-pinned.md),
2026-08-21, answers both halves. **Per request**, from the ledger, with nothing storing a standing —
so a duel is on the ladder the instant it commits, and `ADR-0061` §3's *"nothing writes a season
down, so nothing can disagree about one"* holds one level down. **And a walk is pinned**: the cursor
carries the instant the walk began, the query's upper bound is that cutoff rather than the season's
end, and a walk therefore returns every player of *the ladder as it stood committed at the cutoff*
exactly once. `STORY-0408`'s sentence is **not** inherited and not claimed: a walk is **not live**,
and exactly-once carries one named exception — a duel committed after a page was drawn but stamped
before the cutoff can leave its winner never returned and its loser returned twice, bounded by the
width of one duel-recording transaction rather than of the walk.

## Inherited risk: the first screen is the only door

[`ADR-0060`](../../docs/adr/ADR-0060-the-record-is-its-own-screen-and-the-lobby-is-the-door.md)
made the first screen the way to every other screen, and named the cost in advance: *"the first
screen becomes the only door and will crowd"*, with `STORY-0412`, `STORY-0415` and `EPIC-05` all
inheriting it, so *"the end state is every screen the product has, stacked"* on the screen a new
player sees first. The ladder's door is a fifth control on that screen, and `STORY-0503` is where
the aggregate stops being cheap.

This epic does not reopen `ADR-0060` — it inherits it. It does, however, **strengthen the case for
answering `DEC-054`**: a leaderboard row that leads to another player (`DEC-057`) is a *link*, and a
client with no addresses cannot express one. `DEC-054` is already due before `STORY-0412` is split,
which is well before anything here starts.

## Open decisions

Five were raised on 2026-08-19 when this epic was written, all five the product owner's. **`DEC-055`
is answered** — [`ADR-0061`](../../docs/adr/ADR-0061-a-season-is-a-calendar-month-and-the-coin-never-resets.md),
same day — and the epic is unblocked. **`DEC-056` is answered too** —
[`ADR-0063`](../../docs/adr/ADR-0063-nothing-gates-a-place-and-the-farm-is-accepted-until-the-ladder-is-public.md),
2026-08-21 — which discharges the gate `ADR-0012` put on this epic and settles what `ADR-0058` parked
here: nothing gates a place, and a nameless player has a row. **And `DEC-058` is answered** —
[`ADR-0064`](../../docs/adr/ADR-0064-tied-players-share-one-rank-and-row-order-is-not-a-ranking.md),
2026-08-21: tied players share one rank number, `1 + the number of players standing strictly higher`,
and the order rows sit in is not a ranking — which leaves the tiebreak key inside `DEC-061` rather
than answering half of somebody else's question. **And `DEC-059` is answered** —
[`ADR-0065`](../../docs/adr/ADR-0065-the-ladder-hands-a-player-their-own-row.md), 2026-08-21: the
ladder screen renders one **self line** above the rows stating the player's own rank and season
standing, served with the page, so `STORY-0502` ships **two aggregates in one response**; the
profile strip is **untouched** and gains no season number, which closes what `STORY-0311` and
`ADR-0061` §6 both parked here; and nothing marks the player's row in the list. **`STORY-0503` is
now gated by no decision at all** and waits only on `STORY-0502` landing. That leaves **one** of the
original five open — `DEC-057`, which blocks only `STORY-0504`. Two more were raised **by**
`DEC-055`'s answer: `DEC-060`, the product owner's, blocking nothing; and `DEC-061`, the architect's,
which is the epic's two previously unnumbered questions merged into one. **`DEC-061` is answered** —
[`ADR-0066`](../../docs/adr/ADR-0066-the-ladder-is-computed-per-request-and-a-walk-is-pinned.md),
2026-08-21: the ladder is **computed per request** with nothing storing a standing, and **a walk is
pinned to the instant it began**, returning every player of the ladder as it stood at that cutoff
exactly once, at the price of a walk that is not live and one named exception where a row can still
be seen twice or missed. **Nothing gates `STORY-0502` now**, and the epic's critical path begins with
a split rather than a decision. It names one ticket for the planner — an index for the season window,
`duel (finished_at)`, with a measurement — and raises no `DEC`.

A seventh, **`DEC-062`**, was raised at `STORY-0501`'s split and **answered the same day** —
[`ADR-0062`](../../docs/adr/ADR-0062-two-clocks-and-a-date-comes-from-java-time-clock.md): the server
has two clocks, `ServerClock` measures durations and an injected `java.time.Clock` reports dates, so
every read in this epic that needs *now* as a **date** — `TASK-050106`, `STORY-0502`'s route, the
ladder — takes a `java.time.Clock` and never the elapsed one. It amends `ADR-0061` §3, which named
an instrument that cannot answer the question, and unblocks `TASK-050106`.

That there were five is not a sign the epic was written badly — **every one of them was pointed here
by a merged document before this epic existed.** `ADR-0012` gated its smurf vector on `EPIC-05`;
`ADR-0058` parked *"whether a nameless player appears on a leaderboard at all"* here; `EPIC-04`
parked *"what a row links to"* here; `STORY-0311` parked *"season standing"* here; and the vision
named seasons in the roadmap and defined none. This is the backlog of deferred product questions
coming due at once, which is what a v0.3 milestone is.

| ID | Question | Blocks |
| --- | --- | --- |
| `DEC-057` | **The product owner's** — does a leaderboard row lead anywhere: is another player's profile visible to a stranger, and what is on it? `EPIC-04` parked this here in as many words — *"viewing another player's profile or history … it needs a name per leaderboard row and owns what a row links to. Here, `/api/me` means me."* Settle whether a row is inert text or opens something; if it opens something, what a stranger may read — display name, coin balance, duels played, win/loss record, the duel list itself, which `GET /api/me/duels` today serves only to the player it belongs to; and how that sits with `ADR-0029` §7's *"no code path turns a name into an identity"*, which is why history search returns duels and never players. *A row is inert* is a complete answer and ends `STORY-0504` as `dropped` | `STORY-0504` — whether it exists at all |
| `DEC-060` | **The product owner's, raised by [`ADR-0061`](../../docs/adr/ADR-0061-a-season-is-a-calendar-month-and-the-coin-never-resets.md) §7** — does a **finished** season ever become reachable from a screen, and how is one chosen? The ADR settles that a finished season is never *gone*: its standings recompute exactly from `duel` and `duel_result` rows nothing rewrites, and nothing is archived because an archive would preserve nothing the ledger does not. What it deliberately does not settle is whether a player is ever given a way to ask for one. As shipped, on the first day of a month the previous season's ladder is computable and unreachable, and **nothing anywhere records who won it** — the first season this product runs ends with its winner celebrated by nothing. Settle whether the product ever shows a past season and, if so, how one is named and chosen: a selector, a *last season* line, a single remembered winner, or nothing. Note the cost on the other side — a selector is one more control on a screen `ADR-0060` already said would crowd. *Never* is a complete answer and needs saying out loud rather than falling out | nothing today; the deadline is the first season boundary after the ladder ships |

**Inherited, not raised here:** `DEC-054` — the architect's — whether the client grows
URL-addressable routes and a working browser *Back*. Due before `STORY-0412` is split, which is
before anything in this epic starts, and `DEC-057` makes it sharper: a row that leads to another
player is a link, and a client with no addresses has none to give it.

## Definition of done

- [ ] Every story is `done` or `dropped`, and any story `dropped` says which decision killed it.
- [ ] A player who has never signed up can open the ladder from the first screen and leave it again.
- [ ] `STORY-0506` passes: a duel played end to end moves both players' standings by exactly what
      `ADR-0014` says, and the coins on the ladder sum to the coins in `duel_result`.
- [ ] A negative balance appears on the ladder, asserted, in its correct position.
- [ ] The ladder **names the season it is showing**, from the response rather than the browser's
      clock (`ADR-0061` §6) — the one thing that keeps a season standing from being mistaken for the
      all-time counter the profile strip prints.
- [ ] **A player learns where they stand without finding their row.** A player with no display name,
      tied with a hundred others, opens the ladder and reads their own rank and season standing from
      the self line ([`ADR-0065`](../../docs/adr/ADR-0065-the-ladder-hands-a-player-their-own-row.md)
      §1) — and a player who finished no duel this season is told they have no place rather than
      shown a `0` or a rank (§4).
- [x] `ADR-0012`'s gate is discharged in writing — either by a rule in the code, or by an accepted
      risk with a named deadline. Not silently.
      [`ADR-0063`](../../docs/adr/ADR-0063-nothing-gates-a-place-and-the-farm-is-accepted-until-the-ladder-is-public.md),
      2026-08-21: the second shape — an accepted risk, expiring at the event `ADR-0012` itself names
      rather than at a date. It carries one obligation out of this epic, which the planner files as a
      ticket: `EPIC-07`'s definition of done gains a line requiring the acceptance to be re-affirmed
      in writing or replaced by a countermeasure before the ladder is served on a public address.
- [ ] `docs/protocol.md` contracts every endpoint this epic adds, including what each refuses.
- [ ] `poker-engine` is untouched by every commit in the epic.

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
