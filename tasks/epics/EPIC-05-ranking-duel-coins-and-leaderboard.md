---
id: EPIC-05
title: Ranking, duel coins and leaderboard
type: epic
status: blocked
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

Stated as obligations rather than as design, because the shape of four of the seven is
[`DEC-055`](#open-decisions)'s to fix:

- **A season, represented once.** Something the server can name and bound, and a rule that
  attributes a finished duel to exactly one of them. Whether that is a table, a configured window
  or a derived range is `DEC-055`'s.
- **The standings read path.** An ordered, paged, server-computed ladder over plain HTTP, with the
  same totality and disjointness discipline `STORY-0408` pinned for `GET /api/me/duels`.
- **Whatever gates a place on it.** `ADR-0012`'s gate, discharged rather than forgotten — either as
  a rule in the query or as a written, dated acceptance of the residual risk (`DEC-056`).
- **The ladder as a screen**, reached from the first screen and left the way `ADR-0060` says a
  second screen is left.
- **Whatever a row leads to** (`DEC-057`), including nothing.
- **What happens when a season ends**, and what of the record survives it (`DEC-055`).
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
- **Chip conservation, at ladder scale.** Nothing here mints or destroys a coin. The ladder is a
  *read*. The one place in the whole epic that could change a balance is whatever `DEC-055` says
  happens at a season boundary, which is why that half of the question is asked out loud rather
  than absorbed. A ladder that prints a number the player's own profile strip disagrees with is the
  defect this epic exists to avoid.
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
- **A ladder page is harder than a history page.** `STORY-0408` pinned *total and disjoint* for
  `GET /api/me/duels` and [`ADR-0057`](../../docs/adr/ADR-0057-a-cursor-names-the-filter-it-was-drawn-under.md)
  bound a cursor to the filter that drew it. A standings page is ordered by a number that **changes
  while it is being read** — somebody wins a duel between page one and page two — so the same
  property is strictly harder here, and the story that ships paging owes a stated guarantee rather
  than an inherited one.
- **Migrations are immutable**, and this epic may add one. A change to the schema is a new
  `V<n>__` file, numbered at merge time, never an edit to a merged one.

## The data is already there, whatever the answer

Worth stating before anyone argues about urgency: **no answer to `DEC-055` needs a backfill, and no
duel already played is lost to any of them.** `duel.finished_at` is `NOT NULL` on every row,
`duel_result.coin_delta` is the signed per-duel award, and `player.coin_balance` is the running
all-time total. So an all-time ladder is an `ORDER BY` over a column that exists, and a ladder over
any window whatever is a `SUM(coin_delta)` over a join that exists. The decision below is expensive
because it fixes what the product *means*, not because delay costs data.

## Stories

Written on 2026-08-19 with the epic's central product question open — the same way `EPIC-04` was
written, whose own record says it was opened *"when six of its seven decisions were open"* and that
two stories were later added and two re-cut when the answers landed. The same is expected here and
the cost is recorded rather than hidden: **`STORY-0504` and `STORY-0505` may not survive their
decisions**, and the honest end for a story whose premise turns out to be false is `dropped`, not
deleted.

Every one of the six is `blocked`, and all six trace back to `DEC-055`. That is not a scheduling
accident: a query cannot scope itself to a season that has no definition, and a screen cannot print
a number nobody has decided the meaning of.

| ID | Title | Depends on | Gated by | Status |
| --- | --- | --- | --- | --- |
| [STORY-0501](../stories/STORY-0501-a-season-is-a-bounded-thing.md) | A season is a bounded thing, and every finished duel belongs to one | — | `DEC-055` | blocked |
| [STORY-0502](../stories/STORY-0502-the-standings-read-path.md) | The standings read path — ordered, paged, and a rank the server computes | 0501 | `DEC-055`, `DEC-056`, `DEC-058`, `DEC-059` | blocked |
| [STORY-0503](../stories/STORY-0503-the-ladder-is-a-screen.md) | The ladder is a screen, reached from the first screen and left by one control | 0502 | `DEC-056`, `DEC-058`, `DEC-059` | blocked |
| [STORY-0504](../stories/STORY-0504-what-a-row-leads-to.md) | What a row leads to — another player, seen by a stranger | 0503 | `DEC-057` | blocked — may be `dropped` |
| [STORY-0505](../stories/STORY-0505-a-season-ends-and-the-record-survives-it.md) | A season ends, and the record survives it | 0502 | `DEC-055` | blocked — may be `dropped` |
| [STORY-0506](../stories/STORY-0506-a-duel-moves-a-rank.md) | A duel moves a rank, end to end | 0503, 0505 | — | blocked |

`STORY-0506` is the only one of the six whose acceptance criteria are already writable in full: a
duel played through the server moves both players' standings by exactly what `ADR-0014` says, and
the coins on the ladder sum to the coins in `duel_result`. That property holds under every answer
`DEC-055` can give, which is why it is the epic's closing test.

## What can run in parallel

Almost nothing, and the chain is honest rather than pessimistic:

- `0501 → 0502 → 0503` is a real chain. A query cannot scope itself to something with no
  representation, and a screen cannot render a page the endpoint does not serve yet.
- **`0503` and `0505` are the one genuine parallel pair.** The screen is `web-client/`; the end of a
  season is `poker-server/` plus a migration. They share no file and can be worked at once.
- `0504` extends both halves of `0503` and queues behind it rather than beside it.

**Critical path:** `0501 → 0502 → 0503 → 0506`, and it begins with a decision rather than with a
ticket. That is the single most useful thing to know about scheduling this epic today.

## Questions the story split will raise, deliberately not numbered yet

Registering these now would ask them with the wrong premise, because what they *mean* depends on
`DEC-055`. They are written down so the next planner run does not rediscover them, and each is
noted on the story that will raise it:

- **The architect's, at `STORY-0502`** — what does a cursor over a *mutable* ordering guarantee?
  Over a finished season's standings the ordering is immutable and the question is nearly trivial;
  over a live all-time balance it is not, and `STORY-0408`'s *total and disjoint* cannot simply be
  inherited. `ADR-0057`'s answer for history filters is the nearest precedent, not the answer.
- **The architect's, at `STORY-0502`** — is a standing computed per request or materialised? There
  is no scale problem yet and premature caching would be the wrong first move, but the answer
  differs sharply between a window aggregate and a stored column.

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

Five, raised on 2026-08-19 when this epic was written, and **all five are the product owner's.**
`DEC-055` blocks the whole epic; the other four block one or two stories each and are written so
that a single product-owner run can answer all five in dependency order.

That there are five is not a sign the epic was written badly — **every one of them was pointed here
by a merged document before this epic existed.** `ADR-0012` gated its smurf vector on `EPIC-05`;
`ADR-0058` parked *"whether a nameless player appears on a leaderboard at all"* here; `EPIC-04`
parked *"what a row links to"* here; `STORY-0311` parked *"season standing"* here; and the vision
named seasons in the roadmap and defined none. This is the backlog of deferred product questions
coming due at once, which is what a v0.3 milestone is.

| ID | Question | Blocks |
| --- | --- | --- |
| `DEC-055` | **The product owner's** — what is a season, and what does one do to a duel coin? `docs/vision.md` says *"Ranked results over a season"* and lists *season* in the product's vocabulary; nothing else in the repository defines one. Four parts, because the schema, every read and the screen differ between the answers: **(1)** what bounds a season — a calendar period, a fixed span from a start an operator sets, or something ended by hand — and whether v0.3 ships a season that ever ends at all; **(2)** what a boundary does to `player.coin_balance` — resets it, making the coin a per-season score, or leaves it alone and makes a season a *window* over `duel_result.coin_delta`, making the coin all-time and a season a query. This is the load-bearing half: a reset would be **the first thing in the product that destroys a coin**, and while `ADR-0014` calls the balance a net record, `docs/vision.md`'s *What it is* calls the coin *"a counter of duels won"* that is *"not a balance"* — if the answer resets, say whether that changes the vision's *What it is* and escalate rather than derive; **(3)** whether a finished season stays readable, as an archived standing per player, or is simply gone; **(4)** whether the ladder shows the current season only, or a season ladder and an all-time one. No answer needs a backfill — `duel.finished_at` and `duel_result.coin_delta` reconstruct any window — so this is expensive because it fixes meaning, not because data is at risk | the whole epic — every story reads or writes the number a ladder is sorted by |
| `DEC-056` | **The product owner's** — what, if anything, gates a place on the leaderboard, given that device ids are free and an account may **not** be required? [`ADR-0012`](../../docs/adr/ADR-0012-device-bound-anonymous-profiles.md) says device ids are trivially minted, that this is *"a farming and smurfing vector"* against a ranked ladder, and that *"it must not still be true when the leaderboard goes public"*. [`ADR-0036`](../../docs/adr/ADR-0036-an-account-is-offered-never-required.md) — **the human's call** — closes the obvious countermeasure: an anonymous profile *"takes a leaderboard place, exactly as it does today"* and declining an account costs *"no withheld leaderboard place"*. So something else carries the gate, or the risk is accepted out loud. Settle: whether a profile needs a minimum number of duels (or any other threshold) before it has a place; **whether a player who has set no display name appears at all** — [`ADR-0058`](../../docs/adr/ADR-0058-where-a-name-would-be-the-client-prints-no-name.md) parks exactly this question here, and the answer decides whether `No name` is ever printed on a ladder row; whether two profiles that only ever duel each other are treated like any other pair; and, if the residual risk is accepted for v0.3, what that acceptance is written against so `ADR-0012`'s gate is **discharged rather than forgotten** — `EPIC-07`'s deployment is the deadline the ADR's own wording implies. An answer that requires an account contradicts `ADR-0036` and is the human's, not the product owner's | `STORY-0502`, `STORY-0503` |
| `DEC-057` | **The product owner's** — does a leaderboard row lead anywhere: is another player's profile visible to a stranger, and what is on it? `EPIC-04` parked this here in as many words — *"viewing another player's profile or history … it needs a name per leaderboard row and owns what a row links to. Here, `/api/me` means me."* Settle whether a row is inert text or opens something; if it opens something, what a stranger may read — display name, coin balance, duels played, win/loss record, the duel list itself, which `GET /api/me/duels` today serves only to the player it belongs to; and how that sits with `ADR-0029` §7's *"no code path turns a name into an identity"*, which is why history search returns duels and never players. *A row is inert* is a complete answer and ends `STORY-0504` as `dropped` | `STORY-0504` — whether it exists at all |
| `DEC-058` | **The product owner's** — when two players hold the same balance, what is a player told: do they share one rank number, or does something break the tie into distinct positions, and if so what? Only half of this is product. Paging needs *some* deterministic total order or a ladder page is neither total nor disjoint, and which key provides it is the architect's at split time; what the **player reads** — `3, 3, 5` against `3, 4, 5` — is not, and it decides whether a displayed rank and a page position are the same number or two. Ties are the common case here, not the corner: with `wins − losses` over few duels, most of the ladder is stacked on 0, 1 and −1 | `STORY-0502`, `STORY-0503` |
| `DEC-059` | **The product owner's** — does a player see **their own** standing, and where? `STORY-0311`'s profile strip parked *"leaderboard, rating, season standing"* here by name, and the strip is the obvious home for a rank beside the coin balance it already prints. The alternatives are not equivalent: a rank on the strip is a field on `ProfileResponse` and a second query on a route that runs on every lobby load; a marked row on the ladder is a client concern and useless once the player is on page forty; a *jump to me* control is a third endpoint. And *nothing — the player scrolls* is a real answer for a ladder with two players on it. Settle which, because it decides whether `STORY-0502` ships one query or two. The vision is the argument for asking rather than assuming: *"Ranked results over a season"* is about where **you** stand, and an anonymous profile takes a place like anyone else (`ADR-0036`), so *never* would need saying out loud rather than falling out | `STORY-0502`, `STORY-0503` |

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
- [ ] `ADR-0012`'s gate is discharged in writing — either by a rule in the code, or by an accepted
      risk with a named deadline. Not silently.
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
