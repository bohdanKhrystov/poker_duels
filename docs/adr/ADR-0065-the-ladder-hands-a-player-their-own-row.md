# ADR-0065 — The ladder hands a player their own row, and the profile strip keeps the all-time coin

- **Status:** Accepted
- **Date:** 2026-08-21
- **Resolves:** `DEC-059` — does a player see **their own** standing, and where: on the profile strip
  beside the coin balance, marked on the ladder, behind a *jump to me* control, or nowhere?
  **Derived from the vision; the human did not state this call.** One sentence licenses the *whether*:
  *"**A leaderboard.** Ranked results over a season."* A ranked result is a result **belonging to a
  player**, and a ladder on which the player it belongs to cannot locate their own result ranks
  results for everybody except the person who produced them. A second sentence licenses the *which
  number*: *"**One duel coin per win.** Not chips, not currency, not a balance. A counter of duels
  won."* That counter is what the profile strip prints
  ([`ADR-0061`](ADR-0061-a-season-is-a-calendar-month-and-the-coin-never-resets.md) §5), and putting
  a monthly window number beside it, on the one screen that names no season, would make the strip a
  place where two different numbers wear the same label. The supporting sentence, which decides
  nothing on its own, is *"Luck decides a hand. Skill decides whether you come back tomorrow"* —
  what a player comes back to read is their own standing
- **Builds on:** [`ADR-0064`](ADR-0064-tied-players-share-one-rank-and-row-order-is-not-a-ranking.md),
  which defines the number this decision surfaces and, in its own costs, made scanning useless: *"a
  player cannot tell where in a 190-row block of `5`s they sit, or whether they are in it at all."*
  [`ADR-0063`](ADR-0063-nothing-gates-a-place-and-the-farm-is-accepted-until-the-ladder-is-public.md)
  §2 made a nameless row ordinary, so scanning by name fails first for the players least able to find
  themselves another way.
  [`ADR-0061`](ADR-0061-a-season-is-a-calendar-month-and-the-coin-never-resets.md) §4 and §6 supply
  the two numbers and the season label that tells them apart, and explicitly park *"whether the strip
  also learns a season number"* here.
  [`ADR-0060`](ADR-0060-the-record-is-its-own-screen-and-the-lobby-is-the-door.md) is why the strip
  and the ladder are never on screen together, and why nothing a player needs may hang off a profile
  read that is allowed to fail.
  [`ADR-0002`](ADR-0002-server-authoritative.md) is why the standing is computed by the server and
  travels in the response. None of them is superseded or amended
- **Constrains:** `STORY-0502` (the ladder read answers with the page **and** the requesting player's
  own standing — two aggregates, one response), `STORY-0503` (the screen renders a self line and
  marks no row), and `DEC-061`, which is **constrained further, not answered** — see *What this does
  not settle*
- **No migration, no `PROTOCOL_VERSION` step, and no endpoint beyond the one `STORY-0502` was
  already going to ship.** `ProfileResponse` gains no field; the ladder response gains one object it
  was not going to carry

## Context

`EPIC-05` specifies an endpoint and a screen that answer *where does everybody stand*. Nothing in
the epic answers *where do **I** stand* except scrolling until you see yourself — and that was
assumed rather than decided, on a ladder that had two people on it in everyone's head.

Two decisions merged on 2026-08-21 destroyed the assumption, and both destroyed it in the **normal**
case rather than in an edge case.

[`ADR-0063`](ADR-0063-nothing-gates-a-place-and-the-farm-is-accepted-until-the-ladder-is-public.md)
§2 puts a nameless player on the ladder, where their row prints `No name`
([`ADR-0058`](ADR-0058-where-a-name-would-be-the-client-prints-no-name.md)). A player with no
display name — which is every player until they choose one, and the default — cannot scan for
their own name because they do not have one, and cannot scan for `No name` because it is not theirs
alone.

[`ADR-0064`](ADR-0064-tied-players-share-one-rank-and-row-order-is-not-a-ranking.md) §6 then removed
the fallback. On the second day of a season, a 404-player ladder holds about five distinct rank
numbers and 190 people read `5`. Even a player **with** a name is looking for one row in a wall of
identical numbers, and even after finding it has learned nothing the row above it does not also say.
Both of those ADRs recorded, in their own consequence sections, that this made `DEC-059`
load-bearing rather than a nicety. This decision is the one that has to cope with it.

### The forces

1. **Two numbers, one of which is only true when it is labelled.** `ADR-0061` §4 made the ladder a
   `SUM(coin_delta)` inside a calendar month; §5 left `player.coin_balance` an all-time counter that
   never resets. From the second season onward they disagree for anyone who played in both, **on
   purpose**, and `EPIC-05`'s own non-negotiable says the thing that keeps that honest is the season
   label: *"A row that prints a season standing without saying it is one is the defect that sentence
   was reaching for, and it is still a defect."* Only one surface in this product carries a season
   label — the ladder screen, by §6. The profile strip carries none and was never designed to.
2. **The rank is expensive now, and the placement decides how often it is paid.** `ADR-0064` named
   this as its real cost: a competition rank is a function of the **whole ladder**, not of a page.
   Whoever computes one player's rank runs a whole-season aggregate on a join with no index built
   for it (`DEC-061`). `GET /api/me` runs on **every lobby load, for every player, whether or not
   they care about the ladder**; the ladder read runs when somebody asks for the ladder. Those are
   very different bills for the same number.
3. **A profile read is allowed to fail, and the ladder is not allowed to depend on it.**
   `ADR-0060` settled that the strip renders `null` when `GET /api/me` fails and that the way to a
   screen may not vanish with it. Anything hung off the strip inherits that failure; anything hung
   off the ladder read does not.
4. **The first screen is full.** `ADR-0060` predicted *"the first screen becomes the only door and
   will crowd"*, and the ladder's door is already the fifth control on it. Adding a rank to the strip
   adds a number to the busiest screen in the product to answer a question that is only asked on
   another one.
5. **The founding case needs none of this.** *"The author wanted to play quick heads-up duels against
   his sister"* — a two-row ladder tells you where you stand by existing. Any answer here has to
   avoid making that screen worse, and none of the options makes it better.
6. **Nothing here is worth an irreversible commitment.** The endpoint is unwritten, the screen is
   unwritten, no player has ever read a rank. But the asymmetry is real: adding a way to find
   yourself later is additive, while **taking away a standing a player has read is a withdrawal**,
   and `ADR-0036` already says *"withdrawing a capability players have is worse than never offering
   it"*.

### The deadline

**Free today, and not free once `STORY-0502` merges.** The story is unsplit and this decision changes
the shape of the response it is about to build. Afterwards the same answer costs a wire change, a
second endpoint, or both, plus the client work to consume it — and if the answer had been *nothing*,
a player would have read a ladder they could not find themselves on first.

## Decision

### 1. The ladder shows a player their own standing; it never asks them to find it

The ladder screen renders **one self line**, above the list of rows and below the season name,
stating the requesting player's own **rank** and **season standing** — the same two numbers their
ladder row carries.

It is rendered whether or not that player's row is anywhere in the page on screen, and it does not
change as the player walks pages. The ladder hands a player their row rather than hiding it in the
list, which is the only shape that survives §6 of `ADR-0064`: *there are exactly `rank − 1` players
ahead of me this season* is a true and useful sentence even when 189 other people can say it too.

The words are the ladder's text module's, like every other visible string on that screen
(`STORY-0503`). Two refusals, so they are not filled in inside a ticket: the line states the two
numbers as a statement about **this season** and **nothing evaluative** — no encouragement, no
*doing well*, no *closing on the leader*, no comparison to a named player — and it uses the
vocabulary the vision fixes, *duel*, *season*, *rival*, never *buy-in* or *bankroll*.

### 2. The number is the season standing; the profile strip is untouched

- **The profile strip prints `player.coin_balance`, the all-time counter, labelled exactly as it is
  today.** It gains no rank, no season standing and no season name. `ProfileResponse` gains no field
  and `GET /api/me` gains no aggregate. This settles what `ADR-0061` §6 parked and what `STORY-0311`
  parked before it: the answer to *"leaderboard, rating, season standing"* on the strip is **no**.
- **Everything on the ladder screen — every row, and the self line — is the season standing** of
  `ADR-0061` §4.
- **A player tells them apart by the season and by the screen.** The ladder names its season in
  ordinary English from the response (`ADR-0061` §6), the self line is worded as a statement about
  *this season*, and the two surfaces are **never visible at the same time**: `ADR-0060` makes the
  ladder its own screen that the lobby swaps to, so the strip's number is off screen whenever the
  ladder's number is on it.

The two numbers still disagree, deliberately, from the second season onward. What this decision
guarantees is that they are never presented side by side and that the one bounded by a month always
appears under the name of that month.

### 3. It is served by the ladder read, in the same response as the page

The requesting player's own standing is carried by **the ladder endpoint**, in the same response as
the page of rows — one request, one response. Consequences of that, stated so they are not
re-derived:

- **A ladder that renders always carries the self line**, when the player has one. There is no second
  request that can fail alone, arrive late, or leave the screen showing rows with a blank where the
  player's own standing goes.
- **It is required on the request that opens the ladder** — the one with no cursor — and is **not**
  required on later pages. The screen keeps the one it was given while it walks. Whether later pages
  carry it anyway is the architect's.
- **It is not required to be drawn from the same instant as the page.** A standing that is one duel
  stale is an ordinary answer on a ladder that moves while it is read. If `DEC-061` makes the two
  one snapshot cheaply, good; the product does not demand it.
- **The standing served is the requester's own, and the requester is identified by the credential
  they already send** — the `X-Device-Id` header `STORY-0311`'s reads carry, unchanged. **No player
  id in a query parameter**: an endpoint that answers *what is player X's standing* for any X asked
  is `DEC-057`'s question — whether a stranger may look a player up — and this decision does not
  authorise it, touch it, or pre-empt it.
- **How the server produces it is `DEC-061`'s** — a second statement, a second CTE over the same
  aggregate, or a window function. This decision requires the number, not the mechanism.

### 4. Three states, and the third is not a zero

- **The player finished at least one duel this season** → the self line states their rank and their
  season standing.
- **The player has a profile but finished no duel this season** → they have **no place on this
  season's ladder** (`ADR-0061` §4: results, not players), and the line says so in one sentence. It
  prints **no rank** and it does **not** print `0`: `0` is a real standing held by players who did
  play — a player whose only duel that season was a draw (`ADR-0015`) — and printing it for somebody
  who played nothing states something false.
- **The request carries no known device** — the ordinary state of a first visit, and the state
  `STORY-0311` renders as *no profile yet* — → **no self line at all**, and the ladder renders as an
  ordinary ladder.

**The page itself is identical in all three states.** No row is added, removed, reordered or filtered
by who asked; the ladder stays readable by a client with no profile, and reading it still creates
nothing (`ProfileReads`' rule, and `STORY-0502`'s criterion).

### 5. Nothing else marks the player, in v0.3

No highlight on the player's row where it appears in a page, no *jump to me*, no scroll-to-my-row,
no page-containing-me parameter. The self line is the whole answer.

This is the cheapest sentence in this decision to reverse — a marker is a class and a comparison the
client can already make from ids it holds, and a jump is a parameter on an endpoint that does not
exist yet — and it is written down so that a coder adding one is widening scope rather than filling
a gap. It is also the sentence with the least evidence behind it: on the day-two ladder a marker
marks a row the player is almost never looking at, and a jump lands them in the middle of 190
identical rows.

### 6. The self line duplicating a page row is correct, not a paging defect

If the player's row is on the page they are looking at, they appear twice: once in the self line,
once in the list. That is intended. `STORY-0502`'s totality and disjointness are properties of the
**page** (`ADR-0064` §2 says the same thing about repeated rank numbers), and the self line is not a
row of the page. A test that reads the second appearance as a duplicate is asserting the wrong
property.

### 7. What the self line does not carry in v0.3

Named because each is one plausible line away and each is a further commitment: **no ladder total**
(*5th of 404* is a third aggregate over the same window), **no movement** (*up 3 since yesterday*
needs a yesterday nobody stores), **no streak**, **no count of the players sharing the rank**
(`ADR-0064` §5 refused that for rows and this is the same refusal), and **no link anywhere** —
whether anything on the ladder leads to a player at all is `DEC-057`'s.

Each of them is an ordinary ticket if it is ever wanted. None of them is a gap this decision left
by accident.

### 8. What `EPIC-05` gains, exactly

- **`STORY-0502`** loses `DEC-059` from its gate and stays blocked on `DEC-061` alone. **It ships
  two aggregates in one response**, not one: the page, and the requesting player's own rank and
  season standing. It gains criteria — the self standing is served for a player who is **not**
  on the page requested, asserted from a fixture where they are on a later page; it is absent, and
  the page unchanged, for a request with no known device; a player with a profile who played no duel
  that season gets the no-place answer rather than a rank or a `0`, asserted beside a player whose
  only duel was a draw and who therefore has a rank and a `0`; and the self standing equals the row
  that player has in the page when they are in it — two inputs, one player on the page and one off
  it, because a single fixture cannot tell a real aggregate from an echo of the page.
- **`STORY-0503`** loses `DEC-059` and is blocked only on `STORY-0502` landing. It gains criteria —
  the self line is rendered from the response's own field and never derived by matching
  the player's id against the rows on screen (`ADR-0002`, and it would be wrong on every page the
  player is not on); the three states of §4 each render, asserted separately, with the no-place state
  printing no number at all; and no row in the list carries a marker identifying the reader, asserted
  on a page that contains the reader's own row.
- **`STORY-0311`'s parked line is answered**, in the direction of *no*. Its out-of-scope entry
  *"Leaderboard, rating, season standing — `EPIC-05`"* comes back settled: the strip gains none of
  the three, and `ProfileStrip.tsx` is untouched by this epic. The story needs no edit — its sentence
  is still true, it simply now has an answer.
- **`STORY-0506`** is untouched. The end-to-end test still moves two players by `+1` and `−1`.

## Consequences

**What it buys.**

- **The one question a player actually has is answered in one place, and answered truthfully.** *Where
  do I stand this season?* is answered by two numbers on the screen that already names the season,
  for the nameless player and the tied player alike, without either of them scrolling.
- **The expensive number is computed where it is asked for.** `ADR-0064` made a rank a whole-ladder
  aggregate; this puts it on the read that is only made when somebody opens the ladder, instead of on
  `GET /api/me`, which runs on every lobby load for every player forever.
- **The two numbers can never be mistaken for each other on a screen**, because they are never on one
  screen, and the one that is bounded by a month always appears under the name of that month.
- **A failed profile read cannot hide a standing.** The self line rides on the ladder response, so it
  survives exactly the failure `ADR-0060` designed the door to survive.
- **It is cheap to reverse and cheap to extend.** Removing the line is deleting a field and a
  component; adding a marker, a jump, or a total later is additive and does not take anything away
  from anybody.

**What it costs.**

- **`STORY-0502` stops being one query, and `DEC-061` gets harder for the second time in a day.** The
  endpoint must produce a rank for **one player who may not be on the page it drew** — a second
  whole-season aggregate over the same unindexed `SUM(coin_delta)` join, on an ordering the architect
  has not yet decided how to page. `ADR-0064` already made the page's ranks a whole-ladder function;
  this adds a second consumer of the same expensive shape. **This is the real cost of this decision,
  and like the last one it lands on somebody else's open question.**
- **The lobby still does not tell you where you stand.** A player who wants their standing opens the
  ladder, every time, through a door on a screen `ADR-0060` already said would crowd. If checking a
  standing turns out to be the daily habit the vision's *"come back tomorrow"* line describes, this
  is the decision that made it cost a click — and the fix is a new decision reopening §2, not a
  ticket somebody files.
- **The self line can disagree with the player's own row in the same response.** If the two aggregates
  are not drawn from one snapshot — which §3 permits — a duel finishing between them shows a player
  rank `5` on the line and rank `6` on their row, on one screen, at the same time. It is rare, it is
  self-correcting on the next load, and it is a real thing somebody will screenshot.
- **The ladder response is now per-requester.** It was the one read in this product that would have
  been identical for everybody and cacheable as a public document; it is not any more. `EPIC-07` pays
  that bill, not this epic.
- **The two-player ladder gets a line it does not need.** The founding case — two rows, one of them
  yours — now renders a self line restating the row directly beneath it. Accepted: the screen that
  has to work is the 404-row one.
- **Knowing your rank makes not finding your row more noticeable, not less.** A player told they are
  rank `5` may reasonably go looking for which of the 190 rows reading `5` is theirs, and §5 ships
  nothing that answers. This decision sharpens the question it deliberately does not solve.
- **One more line at the top of a screen that will crowd.** The self line sits where rank 1 would
  otherwise be the first thing read.

**What it forecloses.**

- **A ladder response that is the same for everybody**, and with it any plan to cache or pre-render
  it as one document, unless a later decision splits the self standing back out into its own request.
- **The profile strip as a season surface, without a new decision.** After this, a season number on
  the strip is a change to §2 rather than a field somebody adds because it was easy — and it would
  put a monthly window beside an all-time counter, which is the arrangement §2 exists to prevent.
- **Deriving anything about the reader from the page.** §4's *the page is identical in all three
  states* means the ladder read can never quietly become personalised — no *your neighbours*, no
  window centred on the reader — without a decision that says so.

**What this does not settle**, each with the id it goes to rather than a pointer.

- **`DEC-057`** — whether a row leads anywhere, and what a stranger may read. **Untouched.** The self
  line leads nowhere and is text; if `DEC-057` later makes a row a link, whether the self line becomes
  one is that decision's, not this one's.
- **`DEC-060`** — whether a finished season is ever reachable. **Untouched, and slightly sharpened by
  implication:** a player now reads their standing for the current season and, on the first of the
  month, watches it disappear with everyone else's. What the product says about that, if anything,
  is still `DEC-060`'s.
- **`DEC-061`** — **the architect's, and this decision constrains it further without answering any
  part of it.** Exactly how: (a) the endpoint must produce a competition rank for a **single named
  player** as well as for the rows of a page, and that player may be on no page the request drew, so
  a design that derives ranks only while walking an ordered page cannot serve it; (b) the two answers
  travel in one response, so whatever produces them does so within one request; (c) they are **not**
  required to be consistent with each other, which is a deliberate relief rather than an omission —
  if one snapshot is cheap, take it; if it costs a transaction, do not. Whether it is one statement
  or two, computed per request or materialised, and what a page guarantees while the ordering moves,
  all remain entirely the architect's.
- **Where a self line would go on any other screen.** The duel-end screen, the record, the account
  screen: none of them is asked here and none of them gains one. If the answer is ever *yes*, it is a
  ticket against that screen and it inherits §2's rule about which number and which label.
- **Whether a player is ever told the size of the tie they are in.** §7 refuses it in v0.3, matching
  `ADR-0064` §5's refusal for rows. If both are ever wanted, they are one ticket, not two decisions.

## Alternatives considered

**1. The profile strip carries the rank and the season standing.** The strongest case, and the shape
the question was parked expecting: `STORY-0311` named this surface by name, the strip already prints
a coin number so a rank beside it is one more line, and it is the screen a player sees on **every**
visit — so a player learns where they stand without opening anything, which is exactly what a daily
habit wants. Rejected on three counts, any one of which is enough. It puts a month-bounded number
beside an all-time counter on the one screen that names no season, which `EPIC-05`'s own
non-negotiable calls a defect in as many words. It puts `ADR-0064`'s whole-ladder aggregate on
`GET /api/me`, the highest-traffic read in the product, run on every lobby load by every player
including the ones who never open the ladder — the most expensive placement available for the number
`ADR-0064` had just finished making expensive. And it hangs a standing off a read `ADR-0060`
explicitly allows to fail. The vision sentence that seals it is the one about the coin: the strip's
number is *"a counter of duels won"*, and a season standing is not that.

**2. A *jump to me* control.** The strongest case is that it answers a different and arguably better
question — *find my row* rather than *what is my number* — it is the shape long lists conventionally
use, and it scales to a ladder where scrolling is genuinely how a player browses. Rejected because it
is the most expensive option and, today, the least effective: it needs a *which page contains player
X* query over a cursor scheme `DEC-061` has not chosen, and on the second day of a season it lands
the player in the middle of 190 rows reading `5` where §5 marks nothing — so the player pays a third
query to arrive somewhere that tells them nothing their rank did not. It is also strictly additive
later, on top of the self line, if a ladder ever gets long enough for browsing to be the point.

**3. The player's own row is highlighted where it appears, and nothing else.** The strongest case is
that it costs the server **nothing** — the client already knows its own player id from
`GET /api/me` and can compare — it is purely a client concern, and it is what a reader instinctively
expects a leaderboard to do. Rejected because it only works when the player's row is on the page in
front of them, which is almost never: page one of a 404-row ladder is twenty rows, and the player is
somewhere in the other 384. It answers the question exactly when the question is easiest. Kept in
mind as the cheap addition it is; §5 refuses it for now only so it is not invented silently.

**4. Nothing — the player scrolls.** The strongest case is genuinely strong and was the status quo:
it ships `STORY-0502` as one query, adds no field, no line and no cost, keeps the ladder response
identical for every reader, and the vision nowhere promises a player a personal standing. On the
founding two-player ladder it is *correct* — you see both rows at once. Rejected because the ladder
this product will actually serve is not that one, and on the ladder it will serve, scrolling fails
twice over for structural reasons that are now merged decisions rather than guesses: a nameless
player has no name to scan for (`ADR-0063` §2, `ADR-0058`), and 190 rows share a rank
(`ADR-0064` §6). A leaderboard that ranks results and hides yours from you is not the *"Ranked
results over a season"* the vision sells; and the day-two screen is not a transient state, it recurs
twelve times a year.

**5. Both the strip and the ladder.** The strongest case is reach: the number appears wherever a
player might look for it, and the strip's copy is the one that catches a player who never opens the
ladder. Rejected because it pays every cost of alternative 1 — the aggregate on every lobby load, the
unlabelled season number beside an all-time counter — to save a click on a screen the player is one
control away from, and it doubles the number of surfaces where a monthly window can be mistaken for a
counter of duels won.

**6. A separate `GET /api/me/standing`, called by the ladder screen when it opens.** The strongest
case is real architecture: it keeps the ladder response requester-independent and cacheable, it
separates *the ladder* from *me*, and it would be reusable by any later surface that wants a standing
— including the strip, if §2 is ever reversed. Rejected on the product-visible difference, which is
the half that is mine: two requests for one screen means the screen can render complete without the
line, and the player sees a ladder that is silent about them while a second request is in flight or
after it has failed. `STORY-0502` would be building the same aggregate either way, so the saving is
in coupling rather than in work. The sentence to preserve if it is ever split back out is the product
one — **a ladder never renders rows while staying silent about the reader** — and a decision that
moves the field to a second request owes an account of how it keeps that true.
