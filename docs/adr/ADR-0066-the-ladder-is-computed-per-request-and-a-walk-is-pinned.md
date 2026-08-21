# ADR-0066 — The ladder is computed per request, and a walk is pinned to the instant it began

- **Status:** Accepted
- **Date:** 2026-08-21
- **Resolves:** `DEC-061` — is a season standing computed per request or materialised, and what does
  a page guarantee over an ordering that is **recomputed** while it is walked? Both halves, and the
  second is the one this ADR exists for: `STORY-0408`'s *total and disjoint* **cannot be inherited**
  here and is not claimed
- **Builds on:** [`ADR-0061`](ADR-0061-a-season-is-a-calendar-month-and-the-coin-never-resets.md) §3
  and §4, whose *"nothing writes a season down, so nothing can disagree about one"* is the argument
  §1 applies to the standing itself;
  [`ADR-0064`](ADR-0064-tied-players-share-one-rank-and-row-order-is-not-a-ranking.md) §1–§2 and §4,
  which make the rank a whole-ladder function and forbid a key that measures play;
  [`ADR-0065`](ADR-0065-the-ladder-hands-a-player-their-own-row.md) §3, which puts a second
  whole-season aggregate in the same response and leaves the mechanism here;
  [`ADR-0057`](ADR-0057-a-cursor-names-the-filter-it-was-drawn-under.md), whose cursor contract this
  extends to a second ordering and whose §7 names *"a leaderboard page"* as a case to revisit — §7
  below shows the condition it names is **not met** and no keyed MAC is added;
  [`ADR-0062`](ADR-0062-two-clocks-and-a-date-comes-from-java-time-clock.md), which is the clock the
  cutoff is read from. **None of them is superseded or amended.**
- **Constrains:** `STORY-0502`, which is unblocked here and gains criteria named in §9;
  `STORY-0503`, which is untouched
- **No migration, no index, no `PROTOCOL_VERSION` step, no write-path change, and nothing reaches
  `poker-engine`.** One ticket is **named and not written** — see §8.

## Context

`EPIC-05` ships an ordered, paged ladder. Three decisions merged in the two days before this one
fixed what it must contain and left exactly how to produce it open.

[`ADR-0061`](ADR-0061-a-season-is-a-calendar-month-and-the-coin-never-resets.md) §4 made a standing
`SUM(duel_result.coin_delta)` over the duels whose `duel.finished_at` falls in the current calendar
month — an aggregate over a join, not an `ORDER BY` over `player.coin_balance`, and it named the
absence of an index for it as a cost.
[`ADR-0063`](ADR-0063-nothing-gates-a-place-and-the-farm-is-accepted-until-the-ladder-is-public.md)
§1 removed every eligibility predicate, so the result set is the whole active population of the
month. [`ADR-0064`](ADR-0064-tied-players-share-one-rank-and-row-order-is-not-a-ranking.md) §1 made
the rank `1 + the number of players standing strictly higher` — a function of the whole ladder, not
of the page — and §4 required the key that makes the order total to be a fact about a row's
**identity**. [`ADR-0065`](ADR-0065-the-ladder-hands-a-player-their-own-row.md) §3 then put a second
whole-season aggregate in the same response: the requesting player's own rank, served whether or not
their row is on the page drawn.

Both of the last two recorded, in their own consequence sections, that the bill lands here.

### The forces

1. **The expensive part cannot be avoided, so the usual argument for a cursor does not apply.** A
   keyset cursor normally earns its keep by letting page forty start from an index instead of
   counting past thirty-nine pages. Here the rank of the first row of page forty is *by definition*
   the number of players standing strictly above it across the whole season (`ADR-0064` §1), and the
   self standing is a second such number for a player who may be nowhere on the page (`ADR-0065`
   §3). **Every page computes the whole month's aggregate whatever the paging scheme is.** So the
   choice between paging schemes is not a choice about cost. It is only a choice about what a walk
   promises.
2. **`STORY-0408`'s guarantee came free from an immutable ordering, and this ordering is not one.**
   `docs/protocol.md` promises of `GET /api/me/duels` that pages are *"total and disjoint — every
   duel appears exactly once, with no gap and no duplicate, even when a duel finishes between two
   requests"*. That is true there for a reason that does not travel: the ordering is
   `finished_at DESC, id DESC` over a column nothing rewrites, so a duel that finishes mid-walk
   sorts **above** the position the walk holds and can never enter it, and no existing row ever
   moves. The history cursor is therefore already an *as-of*, smuggled in: its own `finishedAt`
   bounds the rest of the walk to rows older than it. A standings ordering is keyed on
   `SUM(coin_delta)`, which changes for two players every time any duel anywhere finishes. Nothing
   about the position a standings cursor names bounds what the aggregate will say next time.
   **Claiming `STORY-0408`'s sentence here without doing something to earn it would be false**, and
   false in a way no ordinary test would catch, because a fixture in which nothing finishes mid-walk
   passes under every design considered here.
3. **A materialised standing is a second place a season can be written down.** `ADR-0061` §3's
   reason for storing no season was *"nothing writes a season down, so nothing can disagree about
   one"*, and §7's promise that a finished season recomputes **exactly** holds only while the ledger
   is the only copy. Any stored standing is keyed by a season, is a second answer to a question the
   ledger already answers, and drifts silently when the two disagree — on a product with no
   operator, no admin surface and nobody to notice.
4. **There is no measurement anywhere in this product.** No load test, no timing, no row counts
   beyond a handful of fixtures. Every argument for caching, materialising or indexing available
   today is an argument from imagination, and `EPIC-05` said so in as many words when it framed this
   question: *"premature caching is the obvious wrong first move."*
5. **The two things a reader can observe are not equally bad, and both are observable.** On a moving
   ordering a walk can return a player twice or never return them at all. A repeat is visible and
   looks like a bug; a miss is invisible and looks like nothing. `ADR-0064` §2 makes this worse
   before it makes it better: a repeated *rank* across a page boundary is legal there, so a client
   cannot use a repeat as a signal that anything went wrong.

### The deadline

**Free today; the cursor's payload is the part that stops being free.** No client has ever sent a
standings cursor, because the endpoint does not exist. `ADR-0057`'s *"The deadline, honestly"*
applies verbatim to this one: the ladder response is reachable from neither socket message root, so
`PROTOCOL_VERSION` does not cover it, there is no handshake in which a client could learn that the
cursors it holds have become invalid, and the only mitigation for changing the encoding is to change
it while nobody holds one. Deciding what the cursor carries is therefore a now-or-expensive
decision, and it is the reason this ADR settles the cursor's contents rather than leaving them to a
ticket.

## Decision

### 1. A standing is computed per request, from the ledger, and nothing stores one

The ladder is one read over `duel_result` joined to `duel`, computed when it is asked for. There is
**no `season_standing` table, no materialised view, no summary column, no cache, no refresh job and
no third ticker sweep**, and the duel-recording transaction (`PostgresDuelResultStore.record`) gains
nothing: it writes the same one `duel` row, two `duel_result` rows and two balance moves it writes
today.

This is `ADR-0061` §3's argument applied one level down. A season is derived because a stored season
could disagree with the ledger; a standing is a function of the same rows and disagrees in the same
way if it is written down twice. The consequence that matters most is the one §5 buys: **a duel that
commits is on the next ladder read, with nothing in between** — no refresh window, no sweep period,
no invalidation to get wrong — which is what `STORY-0506` asserts end to end.

The shape, sketched so the ticket has no room to invent a different one:

```sql
WITH standing AS (
    SELECT dr.player_id, SUM(dr.coin_delta)::int AS coins
    FROM duel_result dr
    JOIN duel d ON d.id = dr.duel_id
    WHERE d.finished_at >= ?    -- the season's first instant (Season.start)
      AND d.finished_at <  ?    -- the walk's cutoff (§2), never the season's end
    GROUP BY dr.player_id
)
SELECT s.player_id, p.display_name, s.coins,
       rank() OVER (ORDER BY s.coins DESC) AS rank
FROM standing s
JOIN player p ON p.id = s.player_id
...
```

SQL's `rank()` **is** `ADR-0064` §1's competition rank — one plus the number of rows ordered
strictly above — and `dense_rank()` is the convention that ADR rejected. The rank is therefore not
an expression anybody has to invent, and the window is over the whole `standing` CTE rather than
over the page.

### 2. One walk, one cutoff: the cursor carries the instant the walk began

A request that arrives **without** a cursor mints one: `asOf = Instant.now(clock)`, from the
injected `java.time.Clock` (`ADR-0062`), which is also where the season comes from
(`currentSeason(clock)`). Every request in the same walk carries that same instant back, inside the
cursor, and the query's upper bound is **the cutoff, never the season's end**:

```kotlin
public data class StandingsCursor(val asOf: Instant, val coins: Int, val playerId: UUID) {
    public fun encoded(): String   // base64url, unpadded, "$asOf|$coins|$playerId"
}

public fun standingsCursorOrNull(raw: String, season: Season): StandingsCursor?
```

The decoding contract is `ADR-0057`'s, unchanged in every property: opaque, **not** unforgeable, and
valid exactly when it is the string this server would itself have issued — one canonical re-encode
check, `StandingsCursor(asOf, coins, playerId).takeIf { it.encoded() == raw }`, plus §7's season
check. There is no filter and therefore no fingerprint: the ladder takes no client-supplied
predicate (`ADR-0063` §1 left it none to take), and the one part of the set definition the client
does hand back — the cutoff — is carried **literally**, because the server needs its value rather
than evidence of it. `limit` stays outside the cursor and may change mid-walk, exactly as
`ADR-0057` §6 has it.

The window is **half-open**, `[season.start, asOf)`, like the season itself (`ADR-0061` §1): a duel
finishing exactly at the cutoff instant belongs to the next walk, not this one.

`nextCursor` is minted from the row **actually served last**, with the same `asOf`, using the
`limit + 1` probe-row idiom `recentDuelsPage` already uses and for the reason its KDoc gives: a
cursor minted from the probe names a row one past the page served, and the row between the two would
never be returned — *"silently, and forever"*.

### 3. The order is `coins DESC, player_id DESC`, and the key is forced rather than chosen

The tiebreak key is `player.id`. `ADR-0064` §4 requires a fact about **identity**; keyset paging
additionally requires that the key be **unique** and **immutable for the duration of a walk**, and
those three requirements together leave exactly one candidate in this schema:

- `display_name` is nullable — `ADR-0063` §2 makes a nameless row ordinary — and it is **mutable**:
  a takedown (`ADR-0051`, `ADR-0038`) sets it to `null` mid-walk, which moves the row and breaks the
  walk for everybody after it.
- `player.created_at` is immutable but not unique, so it does not make the order total on its own.
- `player.id` is unique, immutable, never null, and carries no meaning a player could learn — which
  `ADR-0064`'s alternative 1 names as the specific harm of a legible sort key.

The direction is `DESC` on both components so the whole key runs one way and the page predicate is a
**single row-value comparison**, `(s.coins, s.player_id) < (?, ?)` — the same idiom `DUELS_AFTER_SQL`
already uses, with the same property that no row tying on the first component is skipped. A mixed
`coins DESC, player_id ASC` would need the two-branch spelling
`coins < ? OR (coins = ? AND player_id > ?)`, which is where an off-by-one lives. The direction of
an arbitrary identity key means nothing, so it is chosen for the property that does mean something.

The order among equals is never rendered and no field carries it (`ADR-0064` §3, §4).

### 4. What a walk promises, and what it does not

A **walk** is the sequence of pages a client draws by following `nextCursor` from a response it
asked for **without** one. It ends when `nextCursor` is `null`. A client that drops the cursor and
asks again has begun a **new** walk, with a new cutoff.

**The promise.** Over one walk, every player who had a row on the ladder **as it stood committed at
that walk's cutoff** is returned **exactly once** — no player twice, no player skipped — and every
row carries the standing and the rank that player held at the cutoff. Two properties follow and are
worth stating because a test can check each:

- **Ranks never decrease down a walk.** The only way page *k+1* begins with the rank page *k* ended
  on is `ADR-0064` §2's tie spanning the boundary, which is legal and is not a duplicate row.
- **The self standing (`ADR-0065` §3) is byte-identical on every page of the walk**, because it is
  computed under the same cutoff. `ADR-0065` §1's *"it does not change as the player walks pages"*
  is therefore delivered by the server rather than by the client remembering.

**The first refusal: a walk is not live.** A duel that finishes **after** the cutoff appears in no
page of that walk. Its winner's row shows the standing they held before it; a player whose first
duel of the season finishes mid-walk has **no row anywhere in the walk**; page forty is exactly as
old as page one. This is not a defect to be fixed later — it is the mechanism. Seeing the new duel
means starting a new walk, which is what a client does by dropping the cursor.

**The second refusal, and this is the one nobody would guess: exactly-once has an exception with a
name.** `PostgresDuelResultSink` stamps `finished_at` when it begins recording and the row becomes
visible when that transaction commits, so a duel can be committed **after** a page was drawn while
carrying a `finished_at` **before** the cutoff. Such a duel changes the pinned ladder underneath the
rest of the walk, and then:

- its **winner**, if the walk has not reached them yet, is lifted above the cursor and is
  **never returned** — a row the reader never sees;
- its **loser**, if the walk has already returned them, is pushed below the cursor and is
  **returned a second time** — a row the reader sees twice.

**Both are accepted.** What the cutoff buys is not that they are impossible; it is that their window
is **the width of one duel-recording transaction** rather than the width of the walk, and that the
window is a property of the write path, which is short and one transaction, rather than of how long
a human spends reading. Under a live-recomputed ordering the same two anomalies apply to every duel
finished by anybody at any point during the walk — *the players who win while you read vanish from
your walk, and the ones who lose appear in it twice* — and that is the version this decision
refuses.

The cutoff pins **the ledger, not the profile**: a display name taken down mid-walk reads as taken
down on later pages. Nothing about a name is in the ordering (§3), so this moves no row.

**In `docs/protocol.md`**, the endpoint carries two sentences and not one: the promise, and the two
refusals. `STORY-0502` already requires the guarantee to be written there; §9 says what it must now
say.

### 5. The rank is recomputed on every page, and the cursor carries none

Every page computes `rank()` over the whole `standing` CTE. The cursor carries the last row's
`coins` and `player_id` and **no rank**, and nothing anywhere adds a page offset to a row index.
This is `ADR-0064` §2 restated as a mechanism, and it is why §1's aggregate is unavoidable on every
page rather than only on the first.

### 6. Two statements, one cutoff, one response

The page and the requesting player's own standing (`ADR-0065` §3) are **two statements on one
connection, both bounded by the same `[season.start, asOf)` window**, and no explicit transaction.
They agree because the window is closed, not because they share a snapshot — the cutoff does the
work an isolation level would otherwise have to do, and it does it across requests as well as within
one, which no isolation level can.

That is more than `ADR-0065` §3 asked for; it asked only that the two be in one response. The relief
it granted — *"not required to be drawn from the same instant"* — is therefore unused, and a future
optimisation that folds the two into one statement is permitted precisely because it changes no
observable contract.

The self statement runs only when the request's device resolves to a player. Its three answers are
`ADR-0065` §4's, and the third is decided **before** any SQL runs: an unknown device is *no self
line at all*, which is a fact about the request rather than about the ladder.

### 7. A cursor from outside the current season is a flat `400`, and no MAC is added

`standingsCursorOrNull` refuses a cursor whose `asOf` does not satisfy `season.contains(asOf)` for
the season the server's clock is in. One refusal path, same vocabulary as every other refusal on this
family of reads (`ADR-0057` §5): `400 Bad Request`, empty body, nothing read, indistinguishable from
a cursor that does not decode, and the remedy is the same — drop the cursor and ask for the first
page. **A walk that crosses a month boundary is refused at the boundary and restarted**, which is
correct rather than unfortunate: the alternative is serving August's ladder in September, and
`STORY-0502` is explicitly forbidden to serve any season but the current one (`ADR-0061` §7,
`DEC-060`).

`ADR-0057` §7 names *"a leaderboard page"* among the cases where an unkeyed fingerprint becomes
insufficient. **The condition it states is not met here, and the reason is worth writing down so
nobody has to re-derive it.** That sentence's condition is *"anything where forging a position leaks
somebody else's rows"* — and the ladder leaks nothing, because `ADR-0065` §4 makes the page
**identical for every reader**, profile or no profile. Every row a forger could reach is a row the
server would hand them for asking normally. So the fingerprint discipline stays what
`TASK-040801` chose: opaque, not unforgeable, a consistency check and never an authorisation
control. The day a ladder read is narrowed by who is asking — a *page containing me* parameter, a
friends-only ladder, anything `DEC-057` might one day license — that sentence engages and the answer
is a keyed MAC in a new ADR.

What a forger can do, stated rather than discovered: mint a cursor with any `asOf` **inside the
current month** and read this season's ladder as it stood at that instant. It leaks nothing (the
data was public at that instant and every row of it is public now), it can never reach a different
season (the lower bound comes from the server's clock and is not in the cursor), and the worst it
achieves is a degraded walk for the forger — the same conclusion `ADR-0057` §7 reached about its own
cursor. It offers a player nothing: no screen, no parameter and no documented capability, so
`DEC-060`'s question — whether a **finished** season is ever reachable — is untouched.

### 8. What is deliberately not built

- **No index.** `duel (finished_at)` is the only index this query could use — `duel_result`'s primary
  key `(duel_id, player_id)` already serves the join in the direction the query drives it, and an
  index on `duel_result (player_id)` would buy the self statement nothing, because §5 makes even one
  player's rank a whole-ladder aggregate. **Named, not written** (`CLAUDE.md` #4): one ticket for
  the planner — *an index for the season window*, one new `V7__` migration, carrying an
  `EXPLAIN`-backed measurement in its `verify:` block rather than a hunch. It is not part of
  `STORY-0502` and is not a prerequisite for it: the tables hold hundreds of rows, nothing in this
  product has ever timed this read, and an index added on imagination is a permanent write cost on
  the one transaction where a coin moves. The trigger is whichever comes first — a measurement that
  shows the read is slow, or the ladder being served on a public address, which is the same event
  `ADR-0063`'s accepted risk expires at.
- **No cache and no pre-render.** `ADR-0065` already made the response per-requester; caching the
  page half while the self half is per-request would put two lifetimes in one response. `EPIC-07`
  pays the caching bill if there is one.
- **No snapshot held across requests.** A repeatable-read transaction spanning a walk would make §4's
  exception impossible, and it would do so by holding a Postgres connection open, idle, per walking
  client, and by giving the server the paging state `ADR-0057` deliberately refuses to keep.
- **No rate limit on the ladder read.** It belongs with `ADR-0063`'s expiring acceptance, at the same
  event, and is not raised as a decision here.

### 9. What `EPIC-05` gains, exactly

**`STORY-0502`** loses `DEC-061` — its last gate — and is ready to split. Its *"Paging is total and
disjoint or it says why not"* note is replaced by §4's guarantee, its *"one query per page"* note by
§6, and it gains criteria that no fixture without a mid-walk write can pass:

- **A duel that finishes mid-walk does not disturb the walk.** Draw page one; record a duel whose
  **winner** sits on a later page and whose **loser** was on the page already served; walk to the
  end. Every player of the ladder as of page one comes back **exactly once**, and no row returned
  carries the new duel's coins. A fixture in which the mid-walk duel touches only players already
  served cannot fail this, so the fixture puts one player on each side of the cursor.
- **The ranks of a later page are the cutoff's ranks**, asserted on the same fixture: the moved
  player's rank is the one they held before the duel, and the ranks a walk returns never decrease.
- **A new walk sees the duel the old walk could not.** The same duel, a request with **no** cursor,
  and the row is there — with no refresh, sweep or job in between. This is the criterion a
  materialised or periodically-refreshed ladder fails.
- **A cursor whose instant is outside the season the server's clock is in is `400`, empty body,
  nothing read** — asserted with `Clock.fixed` on either side of a month boundary, and with a cursor
  that decodes perfectly, so the refusal is about the season and not about the encoding.
- **The self standing is identical on every page of a walk**, asserted across two pages for a player
  who is on neither.
- **The documented exception is asserted rather than assumed away.** A duel written mid-walk with a
  `finished_at` **before** the cutoff, whose loser was already served, is returned a second time.
  The test pins §4's second refusal so it is known rather than discovered; if a later design removes
  the anomaly, this test fails and the sentence in `docs/protocol.md` changes with it. It asserts
  what the product **does**, not what it wishes.
- **`docs/protocol.md`** states the promise and both refusals, not the word *total* on its own.

**`STORY-0503`** is untouched: it renders what it is sent, walks with the cursor it is given, and
gains no *as of* label — the screen's only time word stays `ADR-0061` §6's season name. One rule for
whenever it grows a refresh control: **refreshing drops the cursor**, because re-requesting the same
page under the same cutoff is by construction the same answer.

**`STORY-0506`** is untouched and its end-to-end assertion is what §1 exists to keep true.

## Consequences

**What it buys.**

- **A guarantee that can be tested rather than hoped for.** Every clause of §4 has a fixture behind
  it, including the exception. The alternative in force yesterday — inherit `STORY-0408`'s sentence
  — would have shipped a true-looking claim that only a mid-walk write could falsify, and no
  criterion in `STORY-0502` was going to write one.
- **Consistency costs nothing extra.** Force 1 is the whole argument: the whole-month aggregate is
  paid on every page whatever the paging scheme, so pinning it is one more comparison in a `WHERE`
  clause that already had one. This is the rare case where the stronger property is also the cheaper
  code.
- **The ledger stays the only copy.** `ADR-0061` §7's *"a finished season recomputes exactly"* is
  still true tomorrow; nothing anywhere can disagree with `duel_result` about a standing, because
  nothing else holds one.
- **A duel is on the ladder the instant it commits.** No refresh window to explain to a player who
  just won.
- **It is cheap to reverse in the direction it is likely to be reversed.** Materialising later is
  additive — a table, a backfill from the ledger, the same wire contract — and this ADR is superseded
  rather than amended. The cursor's payload is the part that is *not* cheap to change, which is why
  §2 fixes it now.

**What it costs.**

- **Every page of every walk is a full-month aggregate, and there are two of them per page.** Page
  forty is exactly as expensive as page one — an unindexed pass over the month's `duel` rows joined
  to their results, sorted whole, plus a second identical pass for the self standing (§6) whenever
  the requester has a profile. A twenty-page walk is forty passes. On a **public, unauthenticated**
  read (`ADR-0063` §1 removed every predicate) with no rate limit (§8), the cheapest way to load
  this server is to walk the ladder in a loop, and this decision makes that worse rather than better
  by refusing to cache. **This is the real cost, it is unmeasured, and §8 names the index as a ticket
  rather than pretending the measurement exists.**
- **Later pages are stale and nothing says so.** Page forty is as old as page one, a player watching
  a rival climb sees a ladder from minutes ago, and the screen prints no *as of*. What stops this
  being a product change is that the reader cannot change their **own** standing mid-walk —
  `ADR-0060` makes the ladder its own screen, so a duel cannot be played during a walk — so the
  staleness a reader can observe is always about somebody else, which `ADR-0065` §3 already accepted
  in writing.
- **Exactly-once has an exception, permanently and in the contract.** §4's second refusal means the
  sentence in `docs/protocol.md` can never be the clean one the history endpoint carries, and a
  reader **can** see a row twice or miss one. Both are accepted. The narrowest honest statement of
  the promise is *"exactly once over the ladder as it was committed at the cutoff"*, and the words
  *as it was committed* are load-bearing.
- **A walk is refused twelve times a year.** A cursor held across a month boundary is `400`, and so
  is one held from yesterday's month. Every client of this endpoint must implement *restart the walk*
  — a path `GET /api/me/duels` has (`ADR-0057` §5) but which fires there only after a client bug,
  and fires here on the calendar.
- **The cursor is bigger and now carries a value the server trusts from the client.** The history
  cursor carries a position; this one carries a position **and the set's upper bound**. That is what
  makes §7's forged-`asOf` paragraph necessary, and it is a permanent obligation on any future change
  to this endpoint: widen what the cursor decides and re-run §7's argument.
- **Two statements do the same expensive work twice.** §6 chose clarity over a folded single
  statement. It is a constant factor on an unmeasured read, and it is a constant factor this decision
  chose on purpose.

**What it forecloses.**

- **A live walk.** No page after the first can ever show a duel that finished during the walk, by
  construction. Any future *live ladder* — a `ServerMessage` that pushes changes, a polling screen
  that appends — is a new decision and not a ticket, because it contradicts §4 rather than extending
  it.
- **Serving a page from an index alone.** As long as the rank is `ADR-0064` §1's, no design can make
  page forty cheaper than page one. That is inherited rather than caused here, and §1 makes it
  permanent for as long as §1 stands.
- **Changing the cursor's encoding cheaply**, from the first time a client sends one. `ADR-0057`'s
  deadline reasoning applies unchanged and there is no version to bump.

**What this does not settle**, each with the id it goes to rather than a pointer.

- **`DEC-057`** — whether a row leads anywhere. **Untouched.** §7 notes only that a ladder narrowed
  by who is asking would re-engage `ADR-0057` §7's MAC condition.
- **`DEC-060`** — whether a finished season is ever reachable. **Untouched.** §7's cutoff cannot
  reach one, by construction.
- **When the index is worth building.** §8 names the ticket and the trigger, and deliberately leaves
  the measurement to the ticket that takes it.
- **Anything about how a duel is played.** No rule here reaches `poker-engine`. A walk over a list of
  results is not a fact about a game.

## Alternatives considered

**1. A keyset walk with no cutoff — the ladder is recomputed live on every page, and the guarantee is
stated honestly as the weak one.** The strongest case in the set and the one this decision came
closest to taking. It is the smallest possible thing: no instant in the cursor, no month-boundary
refusal, no forged-`asOf` paragraph, one less value the server trusts from a client — and every page
is *live*, which is the property a reader would assume they were getting. Its weakness is real but
narrow: keyset paging is absolute rather than relative, so a player whose standing does not change is
still returned exactly once no matter what happens to everybody else, and only the players who
actually duel mid-walk can double or vanish. Rejected on force 1 and force 5 together. The
consistency it declines is **free** — the aggregate is computed whole on every page regardless, so
the cutoff costs one comparison — and what it buys with that saving is a guarantee whose only honest
form is conditional: *each player exactly once, unless their standing changed*. A conditional
guarantee is one a test passes for the wrong reason, because the natural fixture holds still. Worse,
under `ADR-0064` §2 a repeated rank across a boundary is already legal, so the one visible symptom of
the anomaly is indistinguishable from correct behaviour, and the invisible symptom — a player never
returned — is exactly the *"silently, and forever"* failure `recentDuelsPage`'s KDoc was written to
prevent. Had this been chosen, the question *may a player be missing from a walk?* would have gone to
the product owner, because that is a promise rather than a mechanism; choosing the stronger guarantee
is what keeps this decision inside its own boundary.

**2. `LIMIT`/`OFFSET` over the pinned aggregate.** Genuinely strong here, and stronger than it is
anywhere else in this codebase: because the whole aggregate is computed on every page anyway (force
1), an offset costs nothing extra, the cursor could be an integer, and with the cutoff of §2 the walk
is exactly as total and disjoint as the keyset version. Rejected on how the two behave in §4's
exception window, which is the only place they differ: under an offset, a single row moving across a
boundary shifts every row after it, so a player who did nothing at all can be skipped or repeated,
and the exception clause would have to say *any player after the moved one* instead of *the two
players of that duel*. It also breaks the one-idiom rule this codebase has kept — `DUELS_AFTER_SQL`'s
row-value comparison — for no gain.

**3. A materialised `season_standing` table, maintained inside the duel-recording transaction.** The
strongest case for materialising: the ladder becomes an index scan over a table with hundreds of
rows instead of an aggregate over every duel of the month; it is exact rather than periodic, so a
duel is on the ladder the moment it commits, which is the one property §1 refuses to give up; and it
would let a future ladder scale past anything this product will plausibly see. Rejected on three
counts, and the third is decisive. It stores a season, which `ADR-0061` §3 forbids in as many words —
*"no season table, no season column"* — and gives a season a place to be written down inconsistently.
It adds a write to the **one** transaction in this product where a coin moves and where a
double-award is the failure the ledger shape exists to prevent, in exchange for a read nobody has
timed. And it **does not answer the half of `DEC-061` that is hard**: a materialised standing still
changes while it is walked, so §2's cutoff — or §4's weak guarantee — would still be needed on top of
it. Materialising buys speed, not consistency, and speed is the thing there is no evidence about.

**4. A materialised view refreshed on a schedule (`REFRESH MATERIALIZED VIEW CONCURRENTLY`).** The
most elegant option on paper, and the only one that solves both halves of `DEC-061` at once: no
write-path change at all, the read becomes trivial, and the ladder is *fixed between refreshes*, so
the walk is total and disjoint for free with no cutoff, no instant in the cursor and no exception
clause. Rejected because the staleness is the wrong shape: it is stale for **everybody**, including
the player who just won, so a duel finishing does not move the ladder until a job runs — which
`STORY-0506` asserts end to end, `ADR-0061` §5 refused to introduce a job for, and this product has
no operator to notice when one stops. It also needs a stored season to key the view on, and it turns
*how stale may the ladder be?* into a question somebody has to answer with a number and tell players
about, which is a product question this design never has to ask.

**5. A repeatable-read transaction held open across the walk, or a Postgres exported snapshot.** The
only option that makes §4's exception genuinely impossible, and it is the textbook answer to paging a
moving set. Rejected because it requires the server to hold a connection open and idle for as long as
a human takes to read — per walking client, from a fixed-size pool, on a public unauthenticated read
— and because it is exactly the paging state `ADR-0057` refuses to keep: *"there is no table of
issued cursors and no session-scoped walk; the cursor is the state."* The cutoff is that same idea
done with an immutable column instead of a connection, which is why it survives a restart, a deploy
and a client that walks tomorrow.

**6. Order by the display name's collation, which `ADR-0064` §4 explicitly permits.** The strongest
case is that it is the only tiebreak key that would make the order among equals *legible* — an
alphabetical block of tied players reads as deliberate rather than random, and `ADR-0029` §1 already
pins a deterministic collation for exactly this kind of read. Rejected on mechanism rather than on
product grounds: the name is nullable and `ADR-0063` §2 makes a nameless row ordinary, so the key
does not make the order total; and the name is **mutable** — `ADR-0051`'s takedown sets it to `null`
— so a takedown landing mid-walk moves a row across a cursor boundary and skips or repeats a player
who has nothing to do with it. A paging key must be immutable for the length of a walk, and only
`player.id` is.

**7. Fold the page and the self standing into one statement.** The strongest case is that it halves
the work per page and makes the two answers share a snapshot by construction rather than by argument.
Rejected for now on shape, not on merit: expressing it needs either a `UNION ALL` whose two branches
must agree on column shape with the page's `ORDER BY … LIMIT` wrapped in a subquery, or the self row
carried as extra columns on every page row — which produces no self answer at all when a page is
empty, a case §6's two-statement form handles without a special case. It stays available as a pure
optimisation precisely because §2's cutoff already makes the two agree, so taking it later changes no
observable behaviour and needs no ADR.
