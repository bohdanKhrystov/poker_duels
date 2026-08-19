# ADR-0061 — A season is a calendar month, the ladder is a window over it, and the coin never resets

- **Status:** Accepted — **§3 amended by
  [ADR-0062](ADR-0062-two-clocks-and-a-date-comes-from-java-time-clock.md)**, which corrects the one
  instrument this ADR names and nothing else. §3's *"**Which season is it** is a function of
  `ServerClock.nowMillis()`"* cannot be: that clock is `System.nanoTime()`, elapsed time from an
  arbitrary epoch, and following the sentence literally produces a season in 1970. It reads: *which
  season is it is a function of the instant an injected `java.time.Clock` reports; which season was
  that duel in is a function of `finished_at`*. Everything else in §3 — a season is derived and
  never stored, no table, no column, no migration, no seed, no season in any wire type — and every
  other section of this ADR stand unchanged
- **Date:** 2026-08-19
- **Resolves:** `DEC-055` — what is a season, and what does one do to a duel coin? **Derived from
  the vision; the human did not state this call.** Two sentences license it and they pull opposite
  ways, which is the whole decision. *"**One duel coin per win.** Not chips, not currency, not a
  balance. A counter of duels won."* is why a boundary destroys nothing: a counter that is zeroed
  every month is a per-season score, and choosing that would be **editing** the vision's *What it
  is* rather than applying it. *"**A leaderboard.** Ranked results over a season."* is why the
  ladder is not lifetime: what a season bounds is the ranked **results**. Both sentences are
  literally true at once under exactly one shape — an all-time counter, ranked over a window — and
  that shape is this decision. The roadmap's v0.3 row, *"Leaderboard and **seasons**"*, plural, is
  why v0.3 ships a season that ends rather than one that never does
- **Builds on:** [`ADR-0014`](ADR-0014-duel-coin-economy.md), which is **neither superseded nor
  amended**: `wins − losses`, signed and unclamped, is still the arithmetic, and
  `player.coin_balance` is still the all-time record of it. That ADR reserved the word *supersede*
  for a balance that **floats** — *"a rating rather than a count, or an asymmetric award weighted
  by opponent strength"* — and a season standing is neither: it is the same arithmetic over a
  shorter window. [`ADR-0015`](ADR-0015-a-draw-writes-two-result-rows.md) becomes load-bearing in a
  way it was not before — a draw's two rows of zero are what put a player on a season ladder they
  would otherwise be missing from
- **Constrains:** `EPIC-05` — `STORY-0501` (unblocked here, and now `ready`), `STORY-0502`,
  `STORY-0503` and `STORY-0506`, each of which gains or loses criteria named in §8; and
  `STORY-0505`, which this decision **drops**, because the boundary it was written to run at turns
  out to do nothing
- **Raises:** `DEC-060` (the product owner's — whether a finished season is ever reachable from a
  screen) and `DEC-061` (the architect's — a page over a season aggregate)

## Context

[`docs/vision.md`](../vision.md) sells v0.3 in three words — *"Leaderboard and seasons"* — describes
the thing itself in one line — *"A leaderboard. Ranked results over a season."* — and lists *season*
among the words the product is allowed to use. It defines a season nowhere. `EPIC-05` is six
stories written against that gap and not one of them can start: a query cannot scope itself to
something with no definition, and a screen cannot print a number whose meaning nobody has fixed.

The gap is four questions, asked together because the schema, every read and the screen differ
between the answers: what bounds a season, what a boundary does to `player.coin_balance`, what
survives a boundary, and what the ladder shows. They are cheap to ask late and expensive to answer
wrongly. `duel.finished_at` is `TIMESTAMPTZ NOT NULL` on every row ever written and
`duel_result.coin_delta` is the signed per-duel award, so **no answer needs a backfill and no duel
already played is lost to any of them**. What is at stake is meaning, not data.

### The forces

1. **Two vision sentences want opposite things at the boundary.** *"A counter of duels won"* wants
   a number that never restarts. *"Ranked results over a season"* wants a ranking that does.
   Whichever one loses, a number a player already sees changes meaning.
2. **The product has no operator.** The first success condition is two people and a link. There is
   no admin console, no scheduled human, no configuration surface anybody is expected to visit. A
   season that ends when somebody remembers to end it is a season that never ends — and
   `STORY-0505` had already written down the requirement that *"the first season boundary passes
   without anybody being on call for it"*.
3. **A windowed ladder makes two numbers about one player.** `ProfileStrip.tsx` already prints
   `coinBalanceText(profile.coinBalance)` followed by the words *Duel coins* — the all-time counter,
   on the first screen, today. The moment the ladder is scoped to anything shorter
   than all time, the ladder's number and the strip's number differ — and `EPIC-05`'s own
   non-negotiables say *"a ladder that prints a number the player's own profile strip disagrees
   with is the defect this epic exists to avoid"*. That constraint and a season are not both
   satisfiable; one of them has to be paid for out loud.
4. **A lifetime ladder is won by whoever started first.** With `wins − losses` and no rating, an
   all-time ladder is an accumulation race: the earliest player with the most volume is
   uncatchable, and everybody later is ranked partly by how long they have existed. The vision's
   line is *"Luck decides a hand. Skill decides whether you come back tomorrow."* — a ladder nobody
   can ever catch is a reason not to come back.
5. **Nothing here is worth an irreversible commitment.** There are no players, no measurements and
   no scale problem. The right tie-breaker between two defensible shapes is which one is cheaper to
   undo.
6. **One branch is not this agent's to take.** Resetting `player.coin_balance` at a boundary would
   be the first code in the product that destroys a coin, and it would make the vision's *"a
   counter of duels won… not a balance"* false. That is an amendment to *What it is*, and it
   belongs to the human. It is declined here, not decided — see alternative 3.

## Decision

### 1. A season is one calendar month, in UTC

A season begins at `00:00:00Z` on the first day of a calendar month and ends at the instant the
next one begins. Its bounds are **half-open** — `[first instant of the month, first instant of the
next month)` — so consecutive seasons neither gap nor overlap. Its identifier is the month,
`2026-08`, and that is the only identifier a season has.

There is no configuration for the length, no operator-set start, no manual end and no season zero.
Every instant that has ever existed or ever will belongs to exactly one season, computable from the
instant alone.

### 2. A duel belongs to the season its **finish** falls in

The season of a duel is the season containing `duel.finished_at`. A duel that started on 31 August
and finished on 1 September is a September duel, in full: the coin is paid when the duel ends
([`ADR-0017`](ADR-0017-the-server-says-when-a-duel-ends.md)), and a duel cannot pay into two seasons
because it pays once.

A duel finishing **exactly** at a boundary instant belongs to the **new** season — that is what
half-open means, and `STORY-0501`'s criterion *"the test names which"* is answered by this sentence.

### 3. A season is derived, never stored

There is no `season` table, no season column, no row per season, no seed, no migration, and no
season in any wire type except the label of §6. *Which season is it* is a function of
`ServerClock.nowMillis()`; *which season was that duel in* is a function of `finished_at`. Nothing
writes a season down, so nothing can disagree about one.

`poker-engine` learns none of this. A season is a server fact about a record of duels, not a fact
about a game.

### 4. A standing is a sum over the window, and only players who duelled have one

A player's standing in a season is the sum of their `duel_result.coin_delta` over the duels whose
`finished_at` falls inside that season. Same arithmetic as `ADR-0014`, same sign, same absence of a
floor: a season standing of `−2` is ordinary and sorts where it belongs.

The ladder is *results*, not *players*. A player has a row in a season exactly when they finished at
least one duel in it — nobody is listed at zero for a season they did not play. A player whose only
duel in the season was a **draw** does have a row, at `0`, because `ADR-0015` writes a draw as two
result rows of zero rather than as no rows; that is that decision doing work it was not doing
before. `DEC-056` may narrow this set further; it may not widen it.

### 5. A boundary does nothing at all

No job runs at a boundary. Nothing is written, nothing is rewritten, nothing is archived, nothing is
swept, and no ticker gains a third sweep. The ladder shows a different set of duels on 1 September
than it did on 31 August because the clock moved, and for no other reason.

`player.coin_balance` is **never reset, zeroed, floored or otherwise reduced except by losing a
duel**. It stays what `ADR-0014` made it — the all-time signed count of duels won minus duels lost,
and the number the profile strip prints. This decision adds no code path anywhere that decreases a
coin balance.

### 6. The ladder shows the current season, and says which one

- The ladder shows **the current season and nothing else**. v0.3 ships no all-time ladder, no toggle
  and no season selector.
- The number on a ladder row is the **season standing** of §4, not `player.coin_balance`.
- **The ladder names the season it is showing.** A player reads the month and the year in ordinary
  English — `August 2026` — not the identifier `2026-08`, which is the wire form. The string lives
  in the ladder's text module like every other visible string on that screen.
- **The season travels in the response.** The client never works out which season it is looking at
  from the browser's clock: that is a client asserting a server fact
  ([`ADR-0002`](ADR-0002-server-authoritative.md)), and it is wrong for two hours of every month in
  half the world. The field's name and shape are the architect's at split time; that there is one is
  not.
- The profile strip is **untouched**. It keeps printing the all-time counter, labelled as it is
  today. Whether it also learns a season number is `DEC-059`'s, unchanged by this ADR except that
  the question now has two candidate numbers rather than one.

### 7. A finished season is never gone, and v0.3 offers no way to ask for one

A finished season's standings are computed from `duel` and `duel_result` rows that nothing rewrites,
so they can be recomputed **exactly** at any later date by the same query with an earlier window.
Nothing is archived, because an archive would preserve nothing the ledger does not already hold.

And v0.3 gives a player no way to ask for one. On 1 September the August ladder is computable and
unreachable. That is a deliberate limit rather than an oversight, it is the sharpest cost named in
*Consequences*, and `DEC-060` is where it is revisited.

### 8. What `EPIC-05` gains and loses, exactly

- **`STORY-0501`** is unblocked and `ready`. It needs **no migration** (§3), and its open questions
  are answered: a derived range, not a row; identified by its month; *current* is a computation, not
  a stored flag; the boundary is inclusive at the start and exclusive at the end.
- **`STORY-0502`** loses `DEC-055` from its gate and stays blocked on `DEC-056`, `DEC-058` and
  `DEC-059`. Its scope is fixed as the window sum of §4, and its criterion *"the coin totals the
  endpoint reports agree with `player.coin_balance` for every player"* is **rewritten rather than
  quietly satisfied**: the endpoint agrees with the season sum, and a player holding duels in two
  seasons is the fixture that proves the two numbers are allowed to differ. It also inherits
  `STORY-0505`'s surviving assertion, below.
- **`STORY-0503`** gains one criterion — the screen names the season it is showing, taken from the
  response and never from the browser's clock — and inherits §6's rule that the number on a row is
  not the strip's number.
- **`STORY-0505` is `dropped`** by this decision, which the story records. Its premise — a crossing
  that runs, is idempotent and moves data — is false under §5. Its one assertion no other story
  owns, *the ladder read for a season returns only that season's duels*, moves to `STORY-0502`.
- **`STORY-0506`** loses `STORY-0505` from `depends_on` and keeps every criterion: a duel still moves
  the winner `+1` and the loser `−1` on the ladder, because a duel played today is inside today's
  season.

## Consequences

**What it buys.**

- **The riskiest story in the epic evaporates.** `STORY-0505` was to run unattended, once a month,
  against a database that had never done it, and it was the one place in the product where a coin
  could be destroyed. Under §5 there is no such code, so there is no such risk, and no migration and
  no third ticker sweep are written.
- **Both vision sentences stay literally true.** The coin is still a counter of duels won; the ranked
  results are still over a season. No other shape satisfies both at once.
- **Every past season stays exactly computable**, at any time, without anything having been kept for
  the purpose.
- **The season length is the cheapest thing in this decision to change.** Nothing is stored, so a
  month becoming a fortnight or a quarter is one function and no data — which is precisely why the
  ordinary unit is chosen now rather than argued about.

**What it costs.**

- **The ladder empties on the first of every month.** With two players, the routine state for the
  first days of a season is an empty or near-empty ladder, and `STORY-0502`'s *"an empty ladder is
  `200` with an empty page"* stops being a corner case and becomes the normal one. Nothing in v0.3
  softens it — no *last season* fallback, no carried-over rows. If it hurts, the fix is a longer
  season, not a second ladder.
- **Nobody is recorded as having won a season.** Nothing is archived (§5) and no screen shows a
  finished season (§7), so the first season this product ever runs ends with its winner celebrated
  by nothing at all. This is the sharpest cost of the set, and it is `DEC-060`'s to revisit.
- **Two numbers about one player, on two screens.** The strip prints the all-time counter, the ladder
  prints the season standing, and from the second season onwards they disagree for anyone who played
  in both. `EPIC-05`'s non-negotiable — *"a ladder that prints a number the player's own profile
  strip disagrees with is the defect this epic exists to avoid"* — is **contradicted on purpose**,
  and what stops it being a defect is only the label of §6. Both stories carrying that criterion are
  rewritten in this change rather than left to fail later.
- **The read is an aggregate over a join, not an `ORDER BY` over an indexed column.** The all-time
  ladder would have been nearly free; this one sums `coin_delta` over a window with no index built
  for it, and paging over an ordering that is *recomputed* as well as *moving* is strictly harder
  than `STORY-0408`'s history walk. That is `DEC-061`, and there is no measurement anywhere in this
  product to say what it costs.
- **A UTC boundary meets locale-rendered times.** `finishedAtText` renders instants *"in the reader's
  locale"*, so a player far enough east or west can read a duel as finishing on 1 September and find
  it counted in August. There is no fix that keeps one ladder: localising the boundary per player
  makes the standings not one ordering, and printing UTC everywhere is worse for everything else.
- **Past standings are recomputed, not frozen.** A finished season is exactly as immutable as the
  rows under it. [`ADR-0039`](ADR-0039-v01-offers-no-account-deletion.md) keeps account deletion open
  for after v0.1 and requires the schema not to foreclose one — the day deletion lands, it silently
  edits every season that player appeared in. A stored archive would have been immune to that, and
  this decision gives that immunity up in exchange for having nothing to write.
- **`DEC-058` gets more urgent, not less.** Ties were already the common case; a window that restarts
  monthly keeps most of the ladder on `0`, `1` and `−1` for most of every season.

**What it forecloses.**

- **A lifetime ladder in v0.3.** *Who is the best player who ever played* has no surface, even though
  the number exists on every profile. Additive later, and nothing is destroyed meanwhile.
- **A season as an event** — a name, a theme, an announced start, a story. Nothing is stored, so
  there is nowhere to hang one; giving a season a name later means giving a season a row.
- **The per-season score**, which is not foreclosed on the merits but declined for want of authority:
  see alternative 3. If the human wants a coin that is zeroed at a boundary, the change is to
  `docs/vision.md`'s *What it is*, and this ADR is superseded rather than amended.

**What it does not settle**, each with the id it goes to rather than a pointer.

- `DEC-056` — whether a threshold, or a missing display name, costs a place. Untouched; §4 gives the
  base set it may narrow.
- `DEC-057` — whether a row leads anywhere. Untouched.
- `DEC-058` — what a tied player reads. Untouched in substance, sharpened in urgency above.
- `DEC-059` — whether a player sees their own standing, and where. Untouched, and now carrying one
  extra clause: with two candidate numbers — all-time on the strip, season on the ladder — the answer
  says which of them the strip shows.
- `DEC-060` — **raised here, the product owner's.** Does a finished season ever become reachable from
  a screen, and how is one chosen? Blocks nothing today; the deadline is the first boundary after the
  ladder ships, because that is the day the question stops being hypothetical.
- `DEC-061` — **raised here, the architect's.** Is a season standing computed per request or
  materialised, and what does a page guarantee over an ordering that is recomputed while it is
  walked? `EPIC-05` parked these as two unnumbered questions for `STORY-0502`'s split; this decision
  merges them into one, because the second's answer follows the first's — materialise and the
  ordering is a column again with `ADR-0057`'s discipline nearly intact, compute per request and it
  is not.

## Alternatives considered

**1. The ladder is all-time, and *season* stays a word in the roadmap.** The strongest case in the
set: it is nearly free. `ORDER BY player.coin_balance DESC` over a column that exists, one query, no
join, no aggregate, no index work — and the ladder's number is the profile strip's number by
construction, which makes `EPIC-05`'s "the ladder disagrees with the strip" defect *impossible*
rather than merely avoided. It never shows an empty ladder, and it deletes two stories instead of
one. Rejected because it ships the milestone with a word removed: the vision does not say *a
leaderboard*, it says *"Ranked results over a season"*, and the roadmap row is *"Leaderboard and
**seasons**"*. It also has the product problem force 4 names — an accumulation race the earliest
player wins permanently, which is the opposite of *"skill decides whether you come back tomorrow"*.

**2. A season is a stored row an operator opens and closes.** The only shape in which a season can be
an *event*: it can have a name, an announced start, a deliberate end, and *which season is current*
becomes a single fact to read rather than arithmetic to trust. It is also the shape every ranked game
with real seasons actually uses. Rejected because it needs an operator, an admin surface and somebody
who remembers — three things this product does not have and would have to acquire, on a product whose
first success condition is two people and a link. It buys nothing v0.3 needs and costs nothing to
adopt later: §1's derived rule can become the first row's contents, with no backfill.

**3. A boundary resets `player.coin_balance`; the coin becomes a per-season score.** The simplest
ladder of all: the same `ORDER BY` on the same column as alternative 1, no join and no aggregate, and
the strip's number and the ladder's number are the same number forever — the one property this
decision has to pay a label for. Everybody starts a season level, which is the fairest ladder
available and the strongest possible reason to come back on the first of the month. **Rejected
because it is not this agent's to choose.** `docs/vision.md`'s *What it is* says a duel coin is *"a
counter of duels won"* and *"not a balance"*; a counter that is zeroed monthly is a per-season score,
and taking that branch would change what the product **is** rather than apply it. Two things are
recorded so a future reader is not misled about the argument: the objection is **meaning, not
recoverability** — a reset balance could be recomputed from `duel_result`, and no data would in fact
be lost — and the coin sentence is not decoration, it is one of five bullets in *What it is*. If the
human wants this, it is a vision change first and a superseding ADR second.

**4. A fixed span — 30 days from a start set once.** Every season is exactly the same length, which
calendar months are not, so two seasons become comparable and the schedule is predictable in a way
28-against-31 never is. Rejected because a player cannot work out when it ends without being told and
this product has nowhere to tell them, while every player alive already knows when a month ends. It
also needs a stored epoch, which is alternative 2's operator problem in miniature.

**5. A season ladder *and* an all-time ladder, both in v0.3.** One extra query over a column that
already exists, answering the question a lifetime coin counter openly invites. Rejected because
*"Ranked results over a season"* is one commitment and not two; because choosing between two ladders
needs a control, which is a second decision on a screen `ADR-0060` already said would crowd; and
because the all-time number is not hidden — the profile strip prints it, so a player who wants their
lifetime record has it on the first screen. Additive later, at the cost of one query.

**6. A shorter or a longer season — a week, a quarter, a year.** A week keeps a two-player ladder
permanently fresh and makes the empty-ladder cost trivial; a year all but abolishes that cost. Both
lost to the same argument, which is honestly a weak one and is recorded as weak: the month is the
window whose bounds every player already knows without being told, and with no players and no
measurements there is nothing else to decide it on. §3 is what makes that acceptable — the length is
stored nowhere, so being wrong about it costs one function and no data.
