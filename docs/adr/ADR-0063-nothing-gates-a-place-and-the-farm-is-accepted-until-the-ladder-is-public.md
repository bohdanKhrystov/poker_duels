# ADR-0063 — Nothing gates a place on the ladder, and the farm is accepted until the ladder is public

- **Status:** Accepted
- **Date:** 2026-08-21
- **Resolves:** `DEC-056` — what, if anything, gates a place on the leaderboard, given that device
  ids are free and an account may not be required? **Derived from the vision; the human did not
  state this call.** Three sentences license it. *"**A leaderboard.** Ranked results over a
  season."* is why the listed thing is a **result** rather than a qualified player —
  [`ADR-0061`](ADR-0061-a-season-is-a-calendar-month-and-the-coin-never-resets.md) §4 already read
  it that way and left this decision only the question of whether to narrow the set. *"It started
  as a personal need: the author wanted to play quick heads-up duels against his sister"* is why two
  profiles that only ever duel each other are an ordinary pair: that pair is the product's founding
  case, and no server can tell it from a farm. And *"Poker is not a game of pure skill and we are
  not going to pretend otherwise… Variance is not a defect to be engineered away"* is the supporting
  — not deciding — argument against a qualification threshold
- **Builds on:** [`ADR-0061`](ADR-0061-a-season-is-a-calendar-month-and-the-coin-never-resets.md) §4
  (a player has a row in a season exactly when they finished at least one duel in it; *"`DEC-056`
  may narrow this set further; it may not widen it"* — this decision narrows it by nothing);
  [`ADR-0058`](ADR-0058-where-a-name-would-be-the-client-prints-no-name.md), whose *What this does
  not settle* parks *"whether a nameless player appears on a leaderboard at all"* here by name, and
  whose §2 already says what a listed nameless player's row prints;
  [`ADR-0036`](ADR-0036-an-account-is-offered-never-required.md) — **the human's call** — which this
  decision applies rather than works around: an anonymous profile takes a leaderboard place, and now
  a nameless one does too;
  [`ADR-0012`](ADR-0012-device-bound-anonymous-profiles.md), whose farming and smurfing gate this
  discharges in writing
- **Amends nothing.** `ADR-0012`'s clause — *"it must not still be true when the leaderboard goes
  public"* — is **applied at the event it names**, not weakened. `EPIC-05`'s out-of-scope table had
  already relocated that event to `EPIC-07`'s deployment; §5 ratifies the relocation and makes it
  checkable
- **Constrains:** `STORY-0502` (the standings query gains no eligibility predicate), `STORY-0503`
  (the screen filters no row it was sent, and prints `No name` where a name is absent),
  `STORY-0506` (its fixture needs no threshold to clear)
- **No wire change, no migration, no `PROTOCOL_VERSION` step.** This decision removes work rather
  than adding it: the answer is a `WHERE` clause that does not exist

## Context

`ADR-0012` shipped device-bound anonymous profiles and wrote down what they cost: *"Device ids are
trivially minted. Anyone can create unlimited profiles. Against a ranked ladder with coins that is a
farming and smurfing vector."* It called that acceptable *"now, when there are no users and no public
leaderboard"*, and gated the countermeasure on this epic. `EPIC-05` is the epic. The gate falls due.

Then `ADR-0036` — the human's — closed the obvious countermeasure in terms: an anonymous profile
*"duels, earns duel coins and takes a leaderboard place, exactly as it does today"*, and declining an
account costs *"no withheld leaderboard place"*. So the ladder cannot be made to require an account,
and whatever carries the gate has to be something else — or there is nothing, and the risk is
accepted out loud.

### The forces

1. **`ADR-0061` changed the ground under this question.** A season is one calendar month and the
   ladder is a `SUM(coin_delta)` window over it, so the ladder **empties on the first of every
   month**. A lifetime ladder would have made a farm permanent; a monthly one makes it renewable —
   the farmer has to re-earn the top every month, and so does everybody else. The stake in any one
   month is smaller, and so is the value of any rule that guards it.
2. **A farmed duel is not just certain, it is fast — and this is the finding that decides the
   threshold question.** `duel-rules.md` puts an honest duel at *"20–45 hands, roughly 5–15
   minutes"*. Two colluding profiles do not play that duel: one shoves all-in preflop, the other
   calls or is called, and the freezeout is over in two or three hands and well under a minute. So a
   farm's advantage over honest play is not one factor but two — certainty **and** an order of
   magnitude in rate. Any rule that counts **duels** is therefore cleared by the farmer first and by
   the honest player last.
3. **The product cannot see collusion, and the rule that would is aimed at the founding pair.** Two
   profiles that only ever duel each other is precisely *"the author wanted to play quick heads-up
   duels against his sister"*. The server sees two device ids playing each other repeatedly, which is
   the first success condition working exactly as designed. There is no signal that separates the
   sister from the smurf.
4. **A nameless player is the product's default state, not its edge case.** `ADR-0058` established
   that in full: v0.1 ships *"No accounts."*, `ADR-0036` keeps an account optional forever, and
   `ADR-0029` makes choosing a name a deliberate, permanent, uncommon act. A rule that lists only
   named players lists a minority of the people who played.
5. **But a ladder of `No name` rows cannot be read as a list of people.** `ADR-0058` named this cost
   for the history list — *"five duels against five different nameless players are five identical
   lines"* — and a leaderboard is the surface where it hurts most, because the ladder's apparent job
   is to say *who* is where.
6. **The whole consequence of a farmed ladder is a wrong name at the top of a list.** No money moves
   — the vision forbids it in *What it is not* — no account is compromised, no regulator or court has
   anything to look at, and nothing outside the software changes. That is exactly why this risk is
   the product owner's to accept rather than an escalation.
7. **Nothing here is worth an irreversible commitment.** There are no players. A predicate added to
   one query is a day's work whenever it is wanted; a predicate *removed* from a shipped ladder takes
   away places people have held, which `ADR-0036` itself calls the worse direction — *"withdrawing a
   capability players have is worse than never offering it."*

### The deadline, honestly

**Free today, and it stops being free on the day a stranger can load the ladder.** The query does not
exist yet: `STORY-0502` is unsplit and blocked on this decision, so a predicate costs nothing to add
now and nothing to leave out. What changes is not the calendar but the audience — a ladder nobody can
see cannot be farmed for status nobody reads. That is why §5's acceptance is written against an
**event** rather than a date, and why *"decide now"* here means *"unblock two stories"* rather than
*"the window is closing"*.

## Decision

### 1. Nothing gates a place

**The ladder lists exactly the set `ADR-0061` §4 defines, narrowed by nothing.** A player has a row
in a season when they finished at least one duel in it, and there is no second condition.

The standings query carries **no eligibility predicate**: no minimum duels played, no minimum or
maximum standing, no account, no credential, no display name, no minimum profile age, no
opponent-diversity rule, no discount for a repeated rival, and no exclusion of a profile that has
only ever lost. Beyond the season window (`ADR-0061` §2) and whatever paging `DEC-061` settles,
`STORY-0502` adds no `WHERE` to the read at all, and `STORY-0503` filters no row it was sent.

One duel is enough. A player whose only duel of the season was a **draw** has a row at `0`, which is
`ADR-0061` §4 and `ADR-0015` and is unchanged here.

### 2. A player with no display name has a row, and it reads `No name`

This is the question `ADR-0058` parked here, answered in the listed direction. A nameless player
appears on the ladder like anybody else, in their correct position, and the row prints exactly what
`nameOrNone(displayName)` returns — `ADR-0058` §2's inheritance, now due, and its §4, *rendered,
never hidden*. The wire still carries `null`; the server fabricates no placeholder (`ADR-0029` §6).

Two nameless players produce two rows that differ only in rank and standing and are otherwise
indistinguishable to a reader. That is accepted deliberately, and it is the first cost named below.

A display name is **not** a credential, so requiring one would not have contradicted `ADR-0036` on
its letter. It is refused on the merits — see alternative 2 — and one of those merits is that it
would have made the human's decision mean less than it says.

### 3. Two profiles that only ever duel each other are an ordinary pair

No cap, no discount, no diminishing return, no opponent-diversity requirement, and no flag. Every
duel between the same two players pays `ADR-0014`'s coin and lands on the ladder exactly like a duel
between strangers.

The reason is force 3: that pair is the product's founding use case, the server holds no fact that
separates it from a farm, and a rule written against it would be a rule written against the first
success condition.

### 4. A rank means what it appears to mean, and a season's standings sum to zero

Two properties follow from §1 and are stated as rules because a later gate would quietly break both:

- **A rank is a position among everyone who played that season**, not among a filtered subset of
  them. Rank 3 on a gated ladder would mean *third among the qualified*, which is a different number
  from the one the player's duels earned and which nothing on the screen could explain.
- **The standings of a whole season sum to exactly `0`.** Every finished duel writes two
  `duel_result` rows that sum to zero — `+1`/`−1`, or `0`/`0` for a draw (`ADR-0015`) — and under §1
  both of those players are on the ladder. Nothing is minted and nothing is hidden, which turns
  `EPIC-05`'s *chip conservation at ladder scale* into an arithmetic identity rather than an
  aspiration. `STORY-0506`'s conservation criterion is what checks it.

The second property has one known expiry, recorded rather than discovered: if account deletion ever
lands ([`ADR-0039`](ADR-0039-v01-offers-no-account-deletion.md) keeps it open for after v0.1), a
deleted player's rows leave the ladder while their opponents' stay, and the sum stops being zero.
That is the same soft spot `ADR-0061` already named — deletion silently edits finished seasons — and
it belongs to whichever ADR ships deletion, not to this one.

### 5. `ADR-0012`'s gate is discharged here, and the acceptance is written against an event

**The risk, stated plainly.** A person with two browser profiles can hold rank 1 of any month they
choose. Each farmed duel is a real duel that pays a real coin, it takes under a minute (force 2), and
the sacrificial profile sinks to the bottom of the same ladder — so the farm is visible in the data
and forbidden by nothing. An honest player's expected season standing is near zero, because
`wins − losses` against real opponents is a random walk. The farmer is therefore not merely ahead;
they are uncatchable by honest play, and the top of the first ladder this product ever shows can
belong to somebody who never beat anybody.

**Why nothing in v0.3 stops it.** Every measure that would bite is unavailable or costs more than the
harm: matchmaking is *later* by the roadmap's own row; a rating or an opponent-weighted award is an
ADR that **supersedes** `ADR-0014` by that ADR's own terms; an account requirement is `ADR-0036`'s
and closed; a per-opponent cap is aimed at the founding pair (alternative 3); and a rate limit is a
gate on playing rather than on a place, which is not the question that was asked (alternative 4).

**The acceptance.** For v0.3 the vector is **accepted, unmitigated, on purpose**. It is acceptable
because the ladder has no audience: `EPIC-05` builds it, `EPIC-07` deploys it, and until then the
whole population of the leaderboard is people the author invited personally.

**It ends at an event, not a date.** The acceptance expires **the first time the ladder is served on
a public address** — `EPIC-07`'s deployment, which is where `EPIC-05`'s own out-of-scope table
already placed `ADR-0012`'s deadline. Nothing about this decays with time on a ladder nobody can
load, so a calendar date would be the wrong instrument and would expire while the risk was still
zero.

**What re-opens it earlier.** Either of these, whichever comes first:

- the ladder becoming readable by anyone who was not personally invited, whatever ships it; or
- a season ending with a standing shaped like a farm — a player whose season standing equals the
  number of duels they finished, over more duels than one evening holds. This is a **signal to
  look**, never a rule and never an automatic anything: one much stronger player against one regular
  rival produces the same shape, and that player has done nothing wrong.

**This ADR does not license a public ladder with the vector open.** It settles that no *place gate*
carries the countermeasure and that v0.3 ships without one. Whether a public deployment ships with
one, and of what shape, is a decision nobody has made yet.

**Named, not written** (`CLAUDE.md` #4): one documentation ticket for the planner — `EPIC-07`'s
definition of done gains a line requiring this acceptance to be **re-affirmed in writing or replaced
by a countermeasure** before the ladder is served on a public address. That line is how `ADR-0012`'s
gate stops being `EPIC-05`'s and becomes checkable somewhere it can still be acted on, which is the
whole of what *"recorded here so it is not rediscovered late"* asked for.

## Consequences

**What it buys.**

- **Two stories unblock and get smaller.** `STORY-0502` ships one query with no eligibility rule and
  `STORY-0503` renders what it is sent; the branch each was holding open — *list them or not* —
  collapses to one test in each rather than two designs.
- **The ladder is the ledger.** §4's zero-sum identity is a property a test can assert over the whole
  ladder, and it is the strongest single check that the ladder and `duel_result` have not drifted.
- **The product keeps one rule for one kind of player.** There is no second class of participant, no
  provisional state, no *unranked* mode, and nothing to explain on a screen `ADR-0060` already said
  would crowd. `ADR-0036`'s promise is applied rather than routed around.
- **`ADR-0012`'s gate is discharged in writing, dated, with an expiry** — the outcome `EPIC-05`'s
  definition of done asks for, and the one the epic said must not happen *silently*.

**What it costs.** These are chosen, not overlooked.

- **The farm stays possible, and now stays possible on purpose.** Before this ADR the vector was an
  unanswered question; after it, it is a decision. The first leaderboard the product ever shows can
  be topped by two browser profiles and an hour of shoving, and nothing in v0.3 detects, discounts,
  refuses or even records it.
- **`No name` can be the top row, and a ladder of them cannot be read.** `ADR-0058`'s history-list
  cost lands here at full strength, on the surface where it hurts most: a reader cannot tell whether
  rank 1 and rank 4 are one person or two, and for the product's *default* player that is what the
  whole ladder looks like. The alternative that fixed it — requiring a name — is spent, and if this
  judgement is wrong, this is where it went wrong.
- **A nameless player cannot find themselves on the ladder.** With no name to look for, *where do I
  stand* is unanswerable by scanning, however short the ladder is. This makes `DEC-059` load-bearing
  rather than a nicety — it is now the **only** way a nameless player reads their own standing — and
  this ADR deliberately does not answer it.
- **A place gate is more expensive to add than it was to refuse.** Every later gate now removes rows
  from players who have had them, and `ADR-0036`'s own sentence about withdrawing capabilities will
  be pointed at whoever proposes one. The cheap moment to gate the ladder is this one, and it is
  being spent on not gating it.
- **The top of the ladder is noise for the first days of every season.** With `ADR-0061`'s monthly
  window and no threshold, one win on the first of the month is rank 1 at `+1`. That compounds the
  cost `ADR-0061` called its sharpest, and the vision's *variance is a feature* line makes it
  defensible rather than desirable.
- **A rank now carries less information than it looks like it carries.** *Rank 1* means *largest
  `wins − losses` this month*, which is a fact about volume and opponent choice as much as about
  play, and the product prints it without qualification anywhere.

**What it forecloses.**

- **A qualification threshold in v0.3**, and any later one starts by taking places away.
- **Reading the ladder as a list of names.** It is a list of results, and some results have no name
  attached; any feature that wants a ladder of identifiable people needs `DEC-057` or a name rule and
  cannot assume either.
- **Treating a season standing as evidence of anything** while the vector is open — including, in
  particular, any later feature that would reward a high standing. There is nothing to reward with in
  this product by design, and this is a reason to keep it that way.
- **A per-opponent cap or discount**, which alternative 3 shows is not merely unattractive but
  unavailable: it is an award weighted by opponent, which `ADR-0014` reserves for a superseding ADR.

## Alternatives considered

**1. A minimum number of duels finished in the season.** The strongest case in the set, and the one
the register named first. It is what every ranked ladder does — placement matches, provisional
ratings, a qualifying period — and it fixes the visible silliness of one lucky win topping an empty
ladder on the first of a month. It is a single `HAVING COUNT(*) >= n`, it costs nothing to write and
nothing to remove, and the vision's *"Skill decides whether you come back tomorrow"* is a fair
argument that volume ought to count for something. Rejected on force 2, which is decisive and was not
obvious until the duel format was read: **a threshold taxes the honest player and not the farmer.** A
farmed duel is under a minute and an honest one is five to fifteen, so any `n` is cleared by two
colluding profiles in the time one real pair plays a single duel — the rule filters out exactly the
casual player who duelled twice and left, while the vector it was proposed against walks through it.
It also makes `ADR-0061`'s sharpest cost worse: on a ladder that routinely holds two players and
empties monthly, a threshold means the ladder is empty for longer, and *empty* is already the routine
state rather than the corner case.

**2. A display name is required for a row.** The closest call, and it lost narrowly. Its case is real
and worth writing down properly: it makes every row identifiable, which is what a ladder appears to
be *for*; it does not contradict `ADR-0036`, because a name is not a credential and a nameless player
would still duel, still earn coins and still have a standing; a name is unique and permanent
(`ADR-0029`) and is burned forever when taken away (`ADR-0051`), so it is the **only** per-profile
cost this product can impose today, and a farmer running many smurfs would spend one every time; and
Lichess, the vision's own reference point, does not put anonymous players on a leaderboard. Rejected
on three counts. **It buys almost nothing against the actual vector** — the farm needs one name for
the profile that wins, once, on one form, and the sacrificial profile does not need a row at all, so
the rule costs a farmer a minute and a casual player their place. **It withholds the ladder from the
player the product ships for**, since `ADR-0058` established that nameless is the default state and
not an unfinished one, and the withholding is silent: a player who wins duels, sees the coin on their
strip and finds no row has been told nothing, and telling them means new copy on the screen
`ADR-0060` said would crowd. And **it is `ADR-0036`'s rejected alternative one step down** — *ranked
requires an account* refused, *ranked requires a form* adopted — which would make a merged human
decision mean less than it says, on a question the human was not asked. If a ladder of `No name` rows
turns out to be the worse failure, this is the alternative to revisit, and it is cheap: one predicate
and one test.

**3. Cap what one opponent can contribute to a season standing** — at most `n` net coins from any
single rival per season. This is the only proposal in the list that actually bites: a two-profile
farm is exactly one opponent, and the cap converts an unbounded farm into `n`. It needs no accounts,
no matchmaking, no operator and no new state, and it is computable inside the aggregate the ladder
already runs. Rejected on two independent grounds, either of which would be enough. It is **aimed at
the vision's founding pair** — two people who only ever duel each other — and it would zero most of
their season, which is the product punishing its own first success condition. And it makes a coin's
value depend on **who** you beat, which `ADR-0014` reserves for an ADR that *supersedes* it in as
many words — *"an asymmetric award weighted by opponent strength"* — so it is not available as a
filter on a read path in any case, and dressing it as one would supersede a merged ADR by accident.

**4. A rate limit on how fast duels may be finished.** The countermeasure that matches the real
mechanism, since force 2 shows the farm's edge is speed. Honest duels take five to fifteen minutes,
so a generous limit would never be felt by a real player, and unlike everything above it does not
sort players into kinds at all. Rejected **on scope rather than on merit**: it is a gate on
*playing*, not a gate on *a place*, so it is not an answer to `DEC-056` — and it would add server
state, a refusal a player can hit, and a threshold nobody can calibrate against zero players. It is
recorded here because it is the shape to start from if §5's acceptance is ever revisited, and because
deciding it needs its own `DEC`, raised then rather than now.

**5. Requiring an account.** The strongest integrity story available: every ranked result attached to
a recoverable identity, and the shape every ranked product on the internet uses. **Not available.**
`ADR-0036` is the human's call and closes it in terms — an anonymous profile *"takes a leaderboard
place, exactly as it does today"*. Recorded so a future reader can see it was weighed and why it is
not on the table, and so that anyone who wants it knows it is a change to that ADR and therefore the
human's.

## What this does not settle

- **`DEC-057`** — whether a row leads anywhere, and what a stranger may read. Untouched. This ADR
  decides *who has a row*, never what the row carries or does; what a row prints beyond a rank, a
  name and a standing is `STORY-0502`'s shape and `DEC-057`'s question.
- **`DEC-058`** — what two tied players read. Untouched, and unchanged in urgency: refusing a
  threshold keeps the small-standing rows that make ties the common case.
- **`DEC-059`** — whether a player sees their own standing, and where. Untouched in substance and
  **sharper in consequence**: §2 puts nameless players on the ladder, so for the default player
  `DEC-059` is now the only way to answer *where do I stand*. Deliberately not answered here — one
  run, one decision.
- **`DEC-060`** — whether a finished season is ever reachable from a screen. Untouched.
- **`DEC-061`** — the architect's: per request or materialised, and what a page guarantees. Untouched,
  and marginally easier: §1 means the aggregate has no eligibility predicate to preserve across a
  page boundary.
- **Whether a countermeasure ever ships, and what shape.** Not decided by anybody. §5 names the event
  that forces the question and alternative 4 names the shape to start from; the decision itself is a
  new `DEC` raised at that point, not one raised now against a risk that is currently zero.
- **Whether `No name` is ever refused as a display name.** `ADR-0058` left it open — a blocklist entry
  or a write-path rule — and it stays open. §2 makes the impersonation hole visible on one more
  surface without widening it.
- **Anything about how a duel is played.** No rule here reaches `poker-engine`, and none could: where
  a player stands is a server fact about a record of duels.
