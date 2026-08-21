# ADR-0064 — Tied players share one rank number, and the order rows sit in is not a ranking

- **Status:** Accepted
- **Date:** 2026-08-21
- **Resolves:** `DEC-058` — when two players hold the same balance, do they share a rank number, or
  does something break the tie into distinct positions, and if so what? **Derived from the vision;
  the human did not state this call.** One sentence licenses it: *"**A leaderboard.** Ranked results
  over a season."* What the ladder ranks is a **result**, and two players whose season standings are
  the same integer have the same result. Giving them different numbers would rank something other
  than the result — a UUID, a collation, a registration date — and the vision does not say the
  product ranks any of those. The supporting sentence, which decides nothing on its own, is
  *"Poker is not a game of pure skill and we are not going to pretend otherwise"*: a fabricated
  distinction between two equal records is exactly a pretence
- **Builds on:** [`ADR-0014`](ADR-0014-duel-coin-economy.md), which is **neither superseded nor
  amended**. Its refusal to floor the balance — *"it silently collapses two very different players
  onto the same value… hiding the number does not change the record — it just makes the record
  untrue"* — is the same principle applied in the other direction here: separating two players the
  record holds equal is the same untruth mirrored. Its reservation of *"an asymmetric award weighted
  by opponent strength"* for a **superseding** ADR is why §4 forbids a tiebreak that measures play.
  [`ADR-0061`](ADR-0061-a-season-is-a-calendar-month-and-the-coin-never-resets.md) §4 supplies the
  number being tied on — the `SUM(coin_delta)` inside the month — and its monthly window is why ties
  are the opening state of every season rather than a corner case.
  [`ADR-0063`](ADR-0063-nothing-gates-a-place-and-the-farm-is-accepted-until-the-ladder-is-public.md)
  §1 maximises the tied population by admitting everyone who finished one duel.
  [`ADR-0002`](ADR-0002-server-authoritative.md) is why the rank is a field rather than something a
  reader counts
- **Constrains:** `STORY-0502` (the rank is computed against the whole ladder, not the page, and
  gains criteria in §7), `STORY-0503` (the screen prints repeated numbers unchanged), and `DEC-061`,
  which is **constrained, not answered** — see *What this does not settle*
- **No wire change beyond a field `STORY-0502` was already going to carry, no migration, no
  `PROTOCOL_VERSION` step.**

## Context

`EPIC-05` ships an ordered, paged ladder with *"a rank the server computes"*. The rank has never
been defined, and two merged decisions have since made the undefined case the **normal** case rather
than an edge.

[`ADR-0061`](ADR-0061-a-season-is-a-calendar-month-and-the-coin-never-resets.md) made a season one
calendar month and the ladder a `SUM(coin_delta)` window over it, so **the ladder empties on the
first of every month**. That ADR named this decision in its own costs: *"`DEC-058` gets more urgent,
not less. Ties were already the common case; a window that restarts monthly keeps most of the ladder
on `0`, `1` and `−1` for most of every season."*
[`ADR-0063`](ADR-0063-nothing-gates-a-place-and-the-farm-is-accepted-until-the-ladder-is-public.md)
then refused every eligibility gate, so a player with exactly one win and a player with exactly one
win both have rows and are indistinguishable by score.

The concrete screen this decision has to survive is therefore not hypothetical. On the second day of
a month, with `wins − losses` over one or two duels each, a ladder of four hundred people holds
perhaps five distinct numbers. Whatever a rank means, it means it there.

### The forces

1. **The standing is a small integer and there is no second dimension.** `ADR-0014` fixed the
   arithmetic at `wins − losses`, and `EPIC-05`'s out-of-scope table forbids inventing a rating
   here: *"inventing one here would silently supersede a merged ADR"*. So the ladder has exactly one
   measure, it is coarse, and collisions on it are not a failure of the measure — they are what the
   measure says.
2. **Paging needs a total order; the player does not.** A page that is total and disjoint requires
   *some* deterministic key beyond the standing, or two pages can drop or repeat a player. That key
   has to exist. Nothing follows from its existence about what number is printed — and conflating
   the two is the specific way this decision can be got wrong, because the cheap implementation
   (rank = page offset + row index) silently answers a product question with a transport detail.
3. **Any tiebreak the player can *read* is a ranking rule.** If tied rows are separated by
   something, players will work out what, and they will be right about the mechanism. If the
   separator is a UUID, the ladder ranks by UUID. If it is duels played, the ladder has a second
   ranking rule that `ADR-0014` reserves for an ADR superseding it, and it points somewhere nobody
   chose — a tiebreak on fewer duels rewards playing less, on a ladder whose vision line is
   *"Skill decides whether you come back tomorrow."*
4. **A ladder that cannot name a leader is uncomfortable, and the discomfort is real information.**
   Two days into a season nobody has separated themselves, because two days is not enough duels for
   `wins − losses` to say anything. A display that manufactures a first place out of a sort key is
   the product pretending to know something it does not.
5. **Nothing here is worth an irreversible commitment.** The query is unwritten, the screen is
   unwritten, and no player has ever seen a rank. Changing which of the three ranking conventions
   is printed is one expression in one query plus the criteria that assert it, with no data to
   migrate — so the tiebreak between defensible shapes is which one is honest, not which one is
   safe to undo, because all three are.

### The deadline

**Free today; it stops being free the first time a player reads a rank.** `STORY-0502` is unsplit
and `STORY-0503` has not started, so the number costs nothing to define now. Afterwards, changing it
takes a place away from somebody who held it — `ADR-0036`'s *"withdrawing a capability players have
is worse than never offering it"* applies to a position on a ladder as much as to anything else.

## Decision

### 1. A rank is one plus the number of players standing strictly higher

For a player on a season's ladder:

> **rank = 1 + the number of players on that ladder whose season standing is strictly greater.**

Players with equal standings therefore read **the same rank**, and the next distinct standing reads
the rank it would have had if the tie had never existed. Three players tied at the top read `1`,
`1`, `1`, and the fourth reads `4`. The ladder prints `3, 3, 5` and never `3, 4, 5`, and never
`3, 3, 4`.

This is competition ranking, and the property that earns it is that the number means something a
player can state: *there are exactly `rank − 1` players ahead of me this season.* Nothing else
printed on this screen answers that question.

### 2. The rank is a field on the row; the row's position in the page is not shown and is not it

They are two different numbers and only the rank is a product fact. The rank is computed by the
server and travels in the response ([`ADR-0002`](ADR-0002-server-authoritative.md)); the position of
a row inside a page is transport, is never rendered, and is never used to derive a rank.

Two consequences that an implementation will otherwise get wrong, stated so they are not
rediscovered inside a ticket:

- **A page may begin with the rank the previous page ended on.** A block of tied players spanning a
  page boundary is ordinary. The same rank number appearing on two pages is **not** a duplicate row,
  and `STORY-0502`'s totality and disjointness guarantee is about **players**, never about rank
  numbers.
- **The first row of the ladder is the only row guaranteed to read `1`**, and even then only when
  the ladder is non-empty. A page's first row carries whatever rank it carries.

### 3. Nothing on the screen breaks a tie

There is no secondary column, no ordering the screen explains, and no *tied since* or *fewer duels*
qualifier. Two tied rows differ in name and in nothing else the player is shown.

### 4. The order tied rows are emitted in is arbitrary, invisible, and not a measure of play

Which key gives the ladder a deterministic total order is **the architect's**, at `STORY-0502`'s
split, and is part of `DEC-061`. This decision does not choose it and constrains it in exactly one
product-facing way:

> The tiebreak key is a fact about **who a row is** — its player id, its display name's collation,
> when the profile was created — and never a fact about **how that player did**: not duels played,
> not who they beat, not who reached the standing first.

A key of the second kind is a second ranking rule wearing an implementation's clothes, and
`ADR-0014` reserves a second ranking rule for an ADR that supersedes it. A key of the first kind
carries no meaning, which is the point: the printed number is the ranking, and the order within a
number is not one.

### 5. A tie is marked by the repeated number and by nothing else, in v0.3

A tied row prints its rank exactly as an untied row does. No `=`, no `T1`, no *tied with 12 others*,
no count of the block, no styling that sets a tie apart. The repetition is what says it.

This is the cheapest sentence in this decision to reverse — a marker is a string in the ladder's
text module and a field on the row — and it is written down so that a coder adding one is widening
scope rather than filling a gap.

### 6. What this looks like on the second day of a season

Not an illustration; the routine state, and the case `STORY-0503` must render without special
casing. Take a September ladder two days in, 404 players, most of whom have finished one or two
duels:

| Season standing | Players | Rank they all read |
| --- | --- | --- |
| `+2` | 4 | `1` |
| `+1` | 190 | `5` |
| `0` | 20 | `195` |
| `−1` | 182 | `215` |
| `−2` | 8 | `397` |

The first screenful reads `1 1 1 1 5 5 5 5 5 5 5 5 5 5 5 5 5 5 5 5`, and page two is twenty more
rows reading `5`. Ranks `2`, `3` and `4` do not exist and neither does anything between `5` and
`195`. In the **first hours** of a season it is starker still: nobody has more than one win, so the
ladder holds two numbers — every winner reads `1` and every loser reads a rank in the hundreds —
and no row anywhere reads `2`.

That screen is accepted as the honest rendering of a ladder two days old, not softened. The season
name required by `ADR-0061` §6 is what tells the reader why it looks like that, and it is the only
thing that does.

### 7. What `EPIC-05` gains, exactly

- **`STORY-0502`** loses `DEC-058` from its gate and stays blocked on `DEC-059` and `DEC-061`. It
  gains three criteria: the rank is §1's competition rank, asserted against a fixture holding a tie
  and the skip after it; the rank is **not** the row's offset, asserted by a page whose ranks are
  not consecutive; and a tie spanning a page boundary repeats a rank across two pages while
  returning each player exactly once. Its existing *"the rank is correct on the second page"*
  criterion stands and is sharpened rather than replaced.
- **`STORY-0503`** loses `DEC-058` and stays blocked on `DEC-059`. It gains one criterion — the
  screen prints repeated rank numbers verbatim, neither de-duplicating them, nor renumbering them,
  nor marking them — and its criterion asserting *"a page whose first row is not rank 1"* is
  restated so it also covers a page whose first row repeats the previous page's rank.
- **`STORY-0506`** is untouched: a duel still moves the winner `+1` and the loser `−1`, and this
  decision changes no arithmetic.

## Consequences

**What it buys.**

- **The number is true.** A player reading rank `215` knows 214 players are ahead of them this
  season. No convention where tied players get distinct numbers can say that, and none where they
  share a compressed number can either.
- **No second ranking rule enters through the back door.** §4 makes the one thing that could
  silently supersede `ADR-0014` — a tiebreak that measures play — a written refusal rather than an
  omission a ticket can fill.
- **A displayed rank and a page position can never be conflated again.** §2 is the sentence that
  stops the cheap implementation, and the cheap implementation is wrong exactly where it is hardest
  to notice: page two.
- **The early-season ladder is legible as what it is.** A column of identical numbers says *nobody
  has separated themselves yet*, which is true, and which a manufactured first place would hide.
- **It is cheap to reverse.** Dense or ordinal ranking is one expression in one query and the
  criteria that assert it. No data, no migration, no wire break.

**What it costs.**

- **The rank stops being free, and `DEC-061` gets harder.** Under ordinal ranking the rank *is* the
  page offset plus the row index — a number the endpoint already holds, needing no extra work at
  all. A competition rank is a function of the **whole ladder**: to number the first row of page
  forty, the server must know how many players are strictly ahead of it across the entire season
  aggregate, not across the page it drew. Whatever `DEC-061` chooses, it now has to produce that
  number, and a keyset cursor can no longer carry the previous page's position forward as if it were
  a rank. **This is the real cost of this decision** and it lands on somebody else's open question.
- **The ladder cannot name a leader for the first days of every month, and this is now permanent
  rather than incidental.** With `ADR-0061`'s monthly window and `ADR-0063`'s absent gate, §6's
  screen recurs twelve times a year. A player cannot tell where in a 190-row block of `5`s they sit,
  or whether they are in it at all — and a player with no display name, which `ADR-0063` §2 makes
  the default, cannot find themselves by scanning at all. That makes `DEC-059` load-bearing, and
  this decision does not answer it.
- **A gap in the numbers reads as a bug.** After a 190-way tie the next row jumps from `5` to `195`,
  and §5 prints nothing that explains it. Anyone who has not met competition ranking before will
  reasonably conclude that rows are missing, and the first place they will look is the paging.
- **A season can end with no single winner.** Several players tied at rank `1` is an ordinary
  outcome, especially in a short or quiet month. Any future surface that wants *the winner of
  August* has to cope with a set rather than a player — that is `DEC-060`'s problem now, and its
  option list is narrower than it was.
- **Every "top N" the product might ever want is ill-defined.** *Top three* is three people on a
  mature ladder and three hundred on day two. Nothing depends on this today; everything that ever
  says *top* will.
- **The size of a tie is invisible.** A player who lands in the middle of a block sees `5, 5, 5` and
  cannot tell whether four people or four hundred share it. The count is not on the wire and §5
  prints nothing that carries it.

**What it forecloses.**

- **A meaningful tiebreak, without a superseding ADR.** Head-to-head, fewer duels for the same net,
  first to reach the standing — all of them are now a change to `ADR-0014`'s territory rather than a
  refinement of a read path.
- **Rank as a synonym for row number**, anywhere, on any surface, forever. The two numbers are
  separate for good, including on whatever `DEC-059` puts on the profile strip.
- **A ladder that always has exactly one first place.** Deliberately.

**What this does not settle**, each with the id it goes to rather than a pointer.

- **`DEC-057`** — whether a row leads anywhere, and what a stranger may read. Untouched. This
  decision fixes the number a row prints and says nothing about what the row does.
- **`DEC-059`** — whether a player sees their own standing, and where. **Sharpened, not answered,
  and this is the second time.** `ADR-0063` §2 made *scroll and find yourself* useless for a
  nameless player; §6 here makes it useless for everybody, because a rank shared by 190 people
  locates nobody. *Nothing — the player scrolls* remains a permissible answer, but it is now an
  answer that has to be argued for rather than one that falls out.
- **`DEC-060`** — whether a finished season is ever reachable from a screen. **Sharpened, not
  answered.** `ADR-0061` §7 already left the first season's winner celebrated by nothing; this
  decision adds that there may not be *a* winner to celebrate. *A single remembered winner*, one of
  the shapes that question lists, is not always well defined and would need a rule for a shared
  first place before it could ship.
- **`DEC-061`** — **the architect's. Constrained, not answered, and deliberately not answered.**
  Whether a standing is computed per request or materialised, what key gives the ladder its total
  order, and what a page guarantees over an ordering that is recomputed while it is walked all stay
  open and all stay theirs. This decision adds two requirements to whatever they choose: the rank is
  computed against the whole ladder rather than the page (§1, §2), and the tiebreak key is about
  identity rather than performance (§4).
- **Whether a tie is ever marked** with a glyph, a count, or a *tied* word. §5 says not in v0.3.
  This is deliberately **not** a new `DEC`: it is a string in the ladder's text module and a
  field, it blocks nothing, and raising a decision point for it would be process for its own sake.
  If it is ever wanted it is an ordinary ticket against `STORY-0503`'s screen.
- **Anything about how a duel is played.** No rule here reaches `poker-engine`. Where a player
  stands is a server fact about a record of duels, and a rank is a fact about a list.

## Alternatives considered

**1. Ordinal ranking — every row a distinct number, the tie broken by the sort key (`3, 4, 5`).**
The strongest case in the set, and it is strong on four independent counts. It is the **cheapest
possible implementation**: the rank is the page offset plus the row index, a number the endpoint
holds already, so `DEC-061` gets *easier* instead of harder and no query ever has to look outside
the page it drew. Every row gets a distinct number, so *top ten* is always ten people and a season
always has exactly one winner for `DEC-060` to name. No player ever reads a gap and wonders what
happened to ranks 6 through 194. And a ladder where every position is unique is what almost every
ranked product on the internet shows. Rejected because the number would be false in precisely the
place it is read most — in §6's ladder it prints `5` for one player on `+1` and `194` for another,
and the thing separating them is a UUID or a collation. `ADR-0014`
refused to floor a balance because doing so *"silently collapses two very different players onto the
same value… hiding the number does not change the record — it just makes the record untrue"*; this
is that untruth mirrored, and it is worse in one way, because it makes the invisible sort key
load-bearing. A player who worked out that alphabetically earlier names sit higher would be right
about the mechanism and would have learned something the product never meant to say.

**2. Dense ranking — tied players share, and the next distinct standing is the next integer
(`3, 3, 4`).** The kindest option to read on exactly the screen this decision has to survive: with
five distinct standings on day two the whole ladder is ranks `1` to `5`, no player reads a
hundreds-wide jump, nobody mistakes a skip for missing rows, and the numbers stay small and calm in
a product whose positioning is *"Dark, quiet, fast, minimal."* It shares §1's honesty about ties
exactly, and differs only in what it does after one. Rejected because it answers a question nobody
asked: a dense rank counts the **distinct standings** above you, not the **players**, and with
integer standings clustered on `−1`, `0` and `1` that number is nearly constant however many people
are ahead. In §6's ladder a dense rank would tell the 182 players on `−1` that they are **fourth**,
with 214 players ahead of them. Being told you are fourth out of 404 when you are 215th is a worse
lie than a hundreds-wide skip, and it leans towards flattery, which is the direction this product
least wants to lean.

**3. Break the tie on something earned — fewer duels for the same net, head-to-head, or who reached
the standing first.** The only option that gives every player a distinct number *and* makes the
distinction deserved, which is what football leagues do with goal difference and what chess does
with tiebreak systems; *the same net record from fewer duels* genuinely is the better performance,
and it is computable inside the aggregate the ladder already runs. Rejected on authority rather than
on taste: it makes a position depend on something other than the coin, and `ADR-0014` reserves a
second ranking dimension — *"a rating rather than a count, or an asymmetric award weighted by
opponent strength"* — for an ADR that **supersedes** it, which this is not. `EPIC-05`'s out-of-scope
table says the same thing in the epic's own words. It also creates an incentive nobody chose: a
tiebreak on duel count means the way to hold your rank is to stop playing, on a ladder whose vision
line is *"Skill decides whether you come back tomorrow."*

**4. Print no rank at all — the ladder is an ordered list and the order is the answer.** The most
honest option available, in one sense: no number can be wrong if no number is printed, it is the
smallest thing that could work, and a ranked list with no numerals would sit comfortably next to
the reference points the vision names. Rejected because it does not remove the problem, it hides it.
With no number, the **only** thing distinguishing two rows is the order they were sent in — which
under §4 is an arbitrary key carrying no meaning, so the display would consist entirely of the one
thing this decision says means nothing. It also puts the reader back to counting rows to find a
position, which is `EPIC-05`'s named non-negotiable inverted — *"a client that numbers rows `1..n`
from the page it received… is wrong on page two"* — with the counting moved from the client into the
player's head, where it is wrong in the same way and nobody can test it.

**5. Suppress the ladder until the standings separate — show nothing, or a message, until somebody
leads.** Squarely addresses the real discomfort: §6's screen is not very useful, and a *"the season
is still young"* state would be honest about that. Rejected because it withholds the one thing the
epic exists to ship, on the days it is most likely to be looked at, and because the threshold that
decides *separated enough* is exactly the kind of eligibility rule `ADR-0063` refused this morning,
relocated from individual rows to the whole screen. `STORY-0503` already requires an empty ladder to render as
an empty ladder *"not as an error and not as a spinner"*; a nearly-flat ladder gets the same
treatment for the same reason.
