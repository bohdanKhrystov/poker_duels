---
id: STORY-0502
title: The standings read path — ordered, paged, and a rank the server computes
type: story
status: done
parent: EPIC-05
module: poker-server
labels: [server, http, read-path, leaderboard]
depends_on: [STORY-0501]
---

## Goal

One HTTP endpoint answers *where does everybody stand* and, in the same response, *where do I
stand*: an ordered page of players with their duel coins and a rank the server worked out, scoped to
a season, and walkable to the end under the guarantee
[`ADR-0066`](../../docs/adr/ADR-0066-the-ladder-is-computed-per-request-and-a-walk-is-pinned.md) §4
states — every player of the ladder *as it stood at the instant the walk began* returned exactly
once, not a live ladder and not without exception — plus the requesting player's own rank and season
standing, served whether or not their row is on the page drawn (`ADR-0065`).

## Why

This is the epic. Everything else either feeds it (`STORY-0501`), renders it (`STORY-0503`), hangs
off a row of it (`STORY-0504`) or proves it (`STORY-0506`).
[`ADR-0014`](../../docs/adr/ADR-0014-duel-coin-economy.md) chose a signed net balance over a win
count *for this query* — *"the leaderboard in `EPIC-05` will be sorted by whichever one this
decision produces"* — and [`ADR-0029`](../../docs/adr/ADR-0029-a-display-name-is-unique-and-permanent.md)
kept a deterministic collation on `display_name` so this read would have an index to use.

## Design notes

**Settled, and the tasks must respect all of it:**

- **Plain HTTP, not the socket.** `docs/protocol.md`: the HTTP endpoints *"carry no `type`
  discriminator and are not `ServerMessage`s. The lobby reads them before any socket exists."* A
  ladder is read by a client holding no socket. This story takes **no `PROTOCOL_VERSION` step** and
  appends no row to `docs/protocol-versions.md`, so it never contends for `ADR-0047`'s lock.
- **The rank is computed by the server and travels in the response.**
  [`ADR-0002`](../../docs/adr/ADR-0002-server-authoritative.md). The client is given a position; it
  never derives one. A response that omits the rank and expects the reader to count is the defect
  this rule exists to prevent, and it is invisible until page two.
- **A rank is `1 + the number of players standing strictly higher`, and it is not the row's offset.**
  [`ADR-0064`](../../docs/adr/ADR-0064-tied-players-share-one-rank-and-row-order-is-not-a-ranking.md)
  §1–§2 answers `DEC-058`: tied players read the **same** rank and the next distinct standing reads
  the rank it would have had anyway — `3, 3, 5`, never `3, 4, 5` and never `3, 3, 4`. The rank is a
  function of the **whole ladder**, not of the page, so it cannot be the page offset plus a row
  index; a tie spanning a page boundary makes page two begin with the rank page one ended on, and
  that is correct rather than a duplicate. Totality and disjointness are properties of **players**,
  never of rank numbers.
- **The order is `coins DESC, player_id DESC`, and the key is invisible.** `ADR-0066` §3 picks
  `player.id` and explains why it is forced rather than chosen: `ADR-0064` §4 requires a fact about
  **identity** — never about how that player did, because a tiebreak on duels played or on who
  reached a standing first is a second ranking rule and `ADR-0014` reserves one for an ADR that
  supersedes it — and keyset paging additionally requires a key that is **unique** and **immutable
  for the length of a walk**. `display_name` is neither (it is nullable by `ADR-0063` §2 and a
  takedown sets it to `null` mid-walk, moving a row); `created_at` is not unique. Both components run
  `DESC` so the page predicate is one row-value comparison, `(coins, player_id) < (?, ?)` —
  `DUELS_AFTER_SQL`'s idiom — rather than the two-branch spelling a mixed direction would need.
- **The read follows the shape `EPIC-04` already built.** A port in
  `duels.poker.server.http` with a Postgres implementation in `duels.poker.server.db`, so no route
  holds a `DataSource` (`ADR-0011`), alongside `ProfileReads`/`PostgresProfileReads`. The response
  type is the wire type, as `ProfileReads`' KDoc explains — no parallel domain type nobody reads.
- **Nothing on this path creates anything.** `ProfileReads`' rule holds here too: a crawler walking
  the ladder mints no `player` row.
- **Negative balances are ordinary and sort where they belong.** `ADR-0014`: a first loss is `−1`.
  Not clamped, not filtered out of the tail, not an error.
- **A null display name is `null` on the wire.** `ADR-0029` §6 — the server fabricates no
  placeholder, and `No name` is a client string (`ADR-0058`). A nameless player **does** have a row —
  [`ADR-0063`](../../docs/adr/ADR-0063-nothing-gates-a-place-and-the-farm-is-accepted-until-the-ladder-is-public.md)
  §2 — and that is still a different question from what the field carries, which is `null`.
- **A walk is pinned to the instant it began, and the guarantee is stated rather than inherited.**
  [`ADR-0066`](../../docs/adr/ADR-0066-the-ladder-is-computed-per-request-and-a-walk-is-pinned.md)
  §2 and §4 answer `DEC-061`. `STORY-0408`'s *total and disjoint* **cannot be inherited** — a
  standings order is keyed on a number that changes while the walk is in progress — so it is paid for
  instead: a request with **no** cursor mints `asOf = Instant.now(clock)`, every cursor in that walk
  carries the same instant back, and the query's upper bound is **that cutoff, never the season's
  end**. What this endpoint promises, in the words `docs/protocol.md` must carry:
  - **The promise.** Over one walk — the pages drawn by following `nextCursor` from a response asked
    for without one — every player who had a row on the ladder **as it stood committed at that
    walk's cutoff** is returned **exactly once**, carrying the standing and rank they held then. So
    ranks never decrease down a walk, and the only page that begins with the rank the previous page
    ended on is `ADR-0064` §2's tie across a boundary.
  - **A walk is not live.** A duel finishing after the cutoff is in **no** page of that walk; a
    player whose first duel of the season lands mid-walk has **no row anywhere** in it; page forty is
    as old as page one. Seeing it means dropping the cursor and starting a new walk.
  - **Exactly-once has one named exception.** `PostgresDuelResultSink` stamps `finished_at` when it
    starts recording and the row is visible when that transaction commits, so a duel can commit
    **after** a page was drawn while carrying a `finished_at` **before** the cutoff. It moves the
    pinned ladder: that duel's **winner**, if not yet reached, is lifted above the cursor and is
    **never returned**; its **loser**, if already returned, is pushed below it and is **returned
    twice**. Both are accepted, and both are asserted below rather than assumed away.
- **A cursor from outside the current season is a flat `400`.** `standingsCursorOrNull` refuses a
  cursor whose `asOf` fails `season.contains(asOf)` for the season the server's clock is in — same
  vocabulary as every other refusal on this family of reads (`ADR-0057` §5): `400`, empty body,
  nothing read, indistinguishable from a cursor that does not decode, remedy is to drop it and ask
  for the first page. A walk crossing a month boundary is therefore **restarted**, not served
  August's ladder in September. The cursor is `ADR-0066` §2's `StandingsCursor(asOf, coins,
  playerId)`, base64url and unpadded, valid exactly when it re-encodes to itself — `ADR-0057`'s
  contract, with **no filter fingerprint** because this read takes no client-supplied filter, and
  with `limit` outside it as `ADR-0057` §6 has it.
- **Two statements, one cutoff, one response** (`ADR-0066` §6). The page and the requesting player's
  own standing (`ADR-0065` §3) are two statements on one connection, both bounded by the same
  `[season.start, asOf)` window and no explicit transaction: they agree because the window is closed,
  not because they share a snapshot. `nextCursor` is minted from the row **actually served last**,
  using `recentDuelsPage`'s `limit + 1` probe-row idiom and for the reason its KDoc gives.
- **Nothing stores a standing** (`ADR-0066` §1). No `season_standing` table, no materialised view, no
  summary column, no cache, no refresh job, no third ticker sweep, and **no change to
  `PostgresDuelResultStore`** — a duel is on the ladder the instant it commits, with nothing in
  between. The read is one `WITH standing AS (SUM(coin_delta) …)` over the window; SQL's `rank()`
  **is** `ADR-0064` §1's competition rank, and `dense_rank()` is the convention that ADR rejected.
- **The engine learns nothing**, and no route parameter reaches `poker-engine`.

**The scope is settled, by
[`ADR-0061`](../../docs/adr/ADR-0061-a-season-is-a-calendar-month-and-the-coin-never-resets.md), and
it is the season window rather than the all-time column:**

- **A standing is `SUM(duel_result.coin_delta)` over the duels whose `duel.finished_at` falls in the
  season** (§4), not an `ORDER BY` over `player.coin_balance`. Same arithmetic as `ADR-0014`, same
  sign, same absence of a floor.
- **The ladder is results, not players** (§4). A player has a row in a season exactly when they
  finished at least one duel in it. Nobody is listed at zero for a season they did not play, so this
  is not a `LEFT JOIN` over every `player` row. A player whose only duel that season was a **draw**
  *does* have a row, at `0`, because `ADR-0015` writes a draw as two rows of zero — that is the case
  a fixture must contain, because it is the one an obvious implementation gets wrong.
- **The endpoint serves the current season** (§6), computed from an injected `java.time.Clock`
  — [`ADR-0062`](../../docs/adr/ADR-0062-two-clocks-and-a-date-comes-from-java-time-clock.md),
  never `ServerClock`, which measures elapsed time and knows no date — and **the response
  names the season it computed** so the client never derives one from the browser's clock
  (`ADR-0002`). The field's name and shape are the architect's; that there is one is not.
- **The eligible set is narrowed by nothing.**
  [`ADR-0063`](../../docs/adr/ADR-0063-nothing-gates-a-place-and-the-farm-is-accepted-until-the-ladder-is-public.md)
  §1 answers `DEC-056`: there is **no eligibility predicate** — no minimum duels, no minimum
  standing, no account, no display name, no profile age, no opponent-diversity rule. Beyond the
  season window and `ADR-0066` §2's cutoff and page predicate, this query adds **no `WHERE` at
  all**, and a
  predicate that appears in a ticket here is a defect rather than a refinement. Two properties of §4
  follow and are criteria below: a rank is a position among **everyone** who played the season, and
  the standings of a whole season **sum to exactly zero**.
- **The ladder's number is not `player.coin_balance`**, and from the second season on they differ for
  anyone who played in both. `EPIC-05`'s non-negotiable about the two agreeing is contradicted on
  purpose by that ADR, in writing, and the criteria below are rewritten to match rather than left to
  fail.

**This read answers with two things, not one**, by
[`ADR-0065`](../../docs/adr/ADR-0065-the-ladder-hands-a-player-their-own-row.md), which answers
`DEC-059`:

- **The page, and the requesting player's own rank and season standing**, in **one response** to
  **one request**. The player's own standing is served whether or not their row is in the page
  drawn, so it cannot be derived from the rows — a player on page forty asks for page one and must
  still be told rank `812` (§1, §3).
- **It is required on the request that opens the ladder** — the one with no cursor — and is **not**
  required on later pages, and it is **not** required to be drawn from the same instant as the page
  (§3). A standing one duel stale is an ordinary answer. `ADR-0066` §6 gives more than was asked
  for and it costs nothing to keep: **two statements under the same cutoff**, so the two answers
  agree, and the self standing is carried on every page of a walk unchanged. The permission to be
  inconsistent goes unused rather than being relied on.
- **Three answers, and the third is not a zero** (§4): a rank and a standing for a player who
  finished a duel this season; **no place this season** — no rank, and never `0` — for a profile
  that finished none, because `0` is a real standing a draw earns (`ADR-0015`); and **nothing at
  all** for a request carrying no known device, which is the ordinary state of a first visit.
- **The page itself is identical in all three.** No row is added, removed, reordered or filtered by
  who asked. The ladder stays readable by a client with no profile, and it never becomes personalised
  in any other way.
- **`ProfileResponse` gains no field and `GET /api/me` gains no aggregate** (§2). The profile strip
  keeps the all-time counter; the season number lives only on this endpoint's answers. A rank added
  to the profile read is a change to a merged decision, not a convenience.

**No decision gates this story. `DEC-061` — the last one — is answered by
[`ADR-0066`](../../docs/adr/ADR-0066-the-ladder-is-computed-per-request-and-a-walk-is-pinned.md)**:
computed per request with nothing storing a standing (§1), a walk pinned to the instant it began
(§2), the guarantee and its two refusals written down (§4), the rank recomputed against the whole
ladder on every page with **no rank in the cursor** (§5), and two statements under one cutoff for the
page and the self standing (§6). Its §8 names one ticket this story does **not** carry — an index for
the season window — and §9 lists the criteria it added, all of which are below.

## Tasks

| ID | Title | Status |
| --- | --- | --- |
| [TASK-050201](../tasks/TASK-050201-the-composition-root-owns-the-wall-clock.md) | The composition root owns the one wall clock, and no component mints its own | ready |
| [TASK-050202](../tasks/TASK-050202-a-standings-cursor-carries-the-walks-cutoff.md) | A standings cursor carries the walk's cutoff, and one from another season does not decode | backlog |
| [TASK-050203](../tasks/TASK-050203-the-wire-shape-a-row-a-season-and-a-self-standing.md) | The wire shape — a row, the season, and a self standing that is never a zero | backlog |
| [TASK-050204](../tasks/TASK-050204-the-port-and-the-query-one-ordered-page.md) | The port and the query — one ordered page of the season's ladder, narrowed by nothing else | backlog |
| [TASK-050205](../tasks/TASK-050205-tied-players-share-a-rank-and-a-rank-is-not-an-offset.md) | Tied players share a rank, a rank is not a row's offset, and a tie may span a page boundary | backlog |
| [TASK-050206](../tasks/TASK-050206-the-ladder-is-results-not-players.md) | The ladder is results, not players — a draw earns a row, one duel is enough | backlog |
| [TASK-050207](../tasks/TASK-050207-the-window-not-the-column-and-a-season-sums-to-zero.md) | The number is the window and not the column, and a season's standings sum to zero | backlog |
| [TASK-050208](../tasks/TASK-050208-the-port-answers-one-players-own-standing.md) | The port answers one player's own whole-season standing | backlog |
| [TASK-050209](../tasks/TASK-050209-the-route-answers-a-page-and-pins-the-walk.md) | The route answers a page, names its season, and pins the walk to one cutoff | backlog |
| [TASK-050210](../tasks/TASK-050210-the-page-the-route-serves-and-the-self-it-carries.md) | The probe row, the last page, the empty ladder, and the self object's three shapes | backlog |
| [TASK-050211](../tasks/TASK-050211-the-routes-refusals-a-bad-limit-a-bad-cursor-and-last-months-walk.md) | The route's refusals — a bad limit, a bad cursor, and a walk from last month | backlog |
| [TASK-050212](../tasks/TASK-050212-the-shipped-server-installs-the-ladder-route.md) | The shipped server installs the ladder route, on the wall clock the root owns | backlog |
| [TASK-050213](../tasks/TASK-050213-over-http-every-player-once-and-page-twos-ranks.md) | Over HTTP against the database — every player exactly once, and page two's ranks | backlog |
| [TASK-050214](../tasks/TASK-050214-a-duel-that-commits-mid-walk-is-in-no-page-of-it.md) | A duel stamped at the cutoff is in no page of the walk, and the ranks stay the cutoff's | backlog |
| [TASK-050215](../tasks/TASK-050215-the-named-exception-and-the-walk-that-sees-it.md) | The named exception — the loser twice, the winner never, and a new walk that sees both | backlog |
| [TASK-050216](../tasks/TASK-050216-the-response-tells-a-player-where-they-stand.md) | The response tells a player where they stand — on the page drawn, and off it | backlog |
| [TASK-050217](../tasks/TASK-050217-three-answers-one-page-and-a-read-that-creates-nothing.md) | Three answers about the reader, one page for everybody, and a read that creates nothing | backlog |
| [TASK-050218](../tasks/TASK-050218-the-document-contracts-the-ladder-and-its-promise.md) | The document contracts the ladder — every parameter, the promise, and both refusals | backlog |
| [TASK-050219](../tasks/TASK-050219-nothing-stores-a-standing.md) | Nothing stores a standing — no table, no column, no materialised view, no migration | backlog |

## Acceptance criteria

Every decision this story waited on is answered, so these are the whole list. The four that name
`ADR-0061` are what `DEC-055`'s answer added or rewrote, the three naming `ADR-0064` are what
`DEC-058`'s answer added, those naming `ADR-0065` are `DEC-059`'s, and those naming `ADR-0066` are
`DEC-061`'s. **The paging ones are the reason this story waited**: each names the second input that
stops it passing on a fixture where nothing moves.

- [ ] Players come back in coin order, asserted against a fixture holding at least one positive, one
      zero and one **negative** season standing — three inputs, so the ordering cannot pass on a
      constant.
- [ ] The rank is a field in the response and is correct on the **second** page, asserted by walking
      to it. A client counting rows would be wrong here and the test says so.
- [ ] **Tied players read the same rank, and the next distinct standing skips.** Asserted against a
      fixture holding two players on the same season standing followed by a third on a lower one:
      the first two read the same number and the third reads that number plus two — `3, 3, 5`. A
      fixture with no tie in it cannot fail this, so the tie is in the fixture (`ADR-0064` §1).
- [ ] **The rank is not the row's offset.** Asserted on a page whose ranks are not consecutive —
      `1, 1, 3` is enough — so an implementation that numbers rows from the page offset fails on
      page **one** rather than surviving to production (`ADR-0064` §2).
- [ ] **A tie spanning a page boundary repeats a rank across two pages and each player exactly
      once.** Asserted by a fixture whose tied block is larger than one page: page two begins with
      the rank page one ended on, and the union of the pages holds every player once. A repeated
      rank is not a duplicate row, and a test that treats it as one is asserting the wrong property
      (`ADR-0064` §2).
- [ ] **Walking every page of a still ladder returns each eligible player exactly once** — no gap,
      no duplicate — asserted over a fixture spanning at least three pages.
- [ ] **A duel that finishes mid-walk does not disturb the walk** (`ADR-0066` §4). Draw page one;
      record a duel whose **winner** sits on a later page and whose **loser** was on the page already
      served — stamped at or after the cutoff, which a `Clock.fixed` makes exactly the cutoff and the
      half-open window therefore excludes; walk to the end. Every player of the ladder *as of page one* comes back **exactly
      once**, and **no row returned carries the new duel's coins**. A fixture whose mid-walk duel
      touches only players already served cannot fail this, so one player sits on each side of the
      cursor — this is the criterion that separates a pinned walk from a live one, and the reason
      `STORY-0408`'s guarantee could not simply be inherited.
- [ ] **The ranks a later page carries are the cutoff's ranks**, asserted on that same fixture: the
      moved player's rank is the one they held before the mid-walk duel, and no rank returned by the
      walk is smaller than one returned before it.
- [ ] **A new walk sees the duel the old walk could not.** The same duel, a request with **no**
      cursor, and the row is there with the new coins — no refresh, sweep, job or wait in between.
      This is the criterion a materialised or periodically-refreshed ladder fails (`ADR-0066` §1).
- [ ] **A cursor whose `asOf` is outside the season the server's clock is in is `400`, empty body,
      nothing read** — asserted with `Clock.fixed` on either side of a month boundary, using a cursor
      that decodes perfectly, so the refusal is about the season and not about the encoding
      (`ADR-0066` §7). A cursor that does not decode is the same `400`, asserted beside it.
- [ ] **The documented exception is asserted, not assumed away** (`ADR-0066` §4). A duel written
      mid-walk carrying a `finished_at` **before** the cutoff, whose loser was already served, brings
      that loser back a second time. The test pins what the endpoint **does** so the exception is
      known rather than discovered; if a later design removes it, this test fails and the sentence in
      `docs/protocol.md` changes with it.
- [ ] **The guarantee and both refusals are written in `docs/protocol.md`** — the promise, *a walk is
      not live*, and the named exception. The word *total* on its own is not the contract here.
- [ ] **No standing is stored anywhere** (`ADR-0066` §1): no migration in the branch, no
      `season_standing` table, no materialised view, no cache, and `PostgresDuelResultStore`'s tests
      are untouched, so the duel-recording transaction writes exactly what it writes today.
- [ ] A player with no display name **has a row**, and it carries `null` rather than a placeholder,
      asserted beside a named player in the same page (`ADR-0063` §2 — the branch `DEC-056` chose;
      the alternative test, asserting their absence, is not written).
- [ ] **One duel is enough, and nothing else is required.** A player who finished exactly one duel in
      the season is on the page, asserted beside a player with several — and no request parameter,
      profile field or credential changes either answer. This is the criterion that catches an
      eligibility predicate being invented inside a ticket (`ADR-0063` §1).
- [ ] **The season's standings sum to exactly zero** over the whole ladder, asserted by walking every
      page of a fixture holding at least one draw and one decisive duel. `ADR-0063` §4: every duel
      writes two rows summing to zero and both players are listed, so a ladder whose total is
      non-zero has either lost a row or invented one.
- [ ] **The self standing is identical on every page of one walk**, asserted across two pages for a
      player who is on neither — it is drawn under the same cutoff, so `ADR-0065` §1's *"it does not
      change as the player walks pages"* is delivered by the server rather than by the client
      remembering (`ADR-0066` §6).
- [ ] **The requesting player's own rank and season standing come back with the page, for a player
      who is not on the page requested** — asserted against a fixture where they sit on a later page,
      and again where they sit on the page drawn — where the two numbers must equal the row that
      player has in it. Two inputs, because one fixture whose player is on the page cannot tell a real
      whole-ladder aggregate from an echo of the rows (`ADR-0065` §1).
- [ ] **A profile that finished no duel this season is told it has no place**, and is not given a
      rank and not given `0` — asserted beside a player whose only duel that season was a **draw**,
      who *does* have a rank and a standing of `0`. These are the two answers an obvious
      implementation collapses into one (`ADR-0065` §4, `ADR-0015`).
- [ ] **A request carrying no known device gets the page and no self standing**, asserted — and the
      page it gets is the same page a known device gets, row for row, asserted against the same
      fixture. The ladder is readable without a profile and is narrowed by nobody (`ADR-0065` §4).
- [ ] Reading the ladder creates nothing: a request from an unknown device leaves the `player` row
      count unchanged, asserted.
- [ ] An empty ladder is `200` with an empty page, not `404` — the same shape `GET /api/me/duels`
      chose.
- [ ] The coin totals the endpoint reports equal the `SUM(coin_delta)` inside the season window for
      every player in the fixture — **not** `player.coin_balance`. Asserted with a fixture holding a
      player whose duels span **two** seasons, whose ladder number is therefore smaller than their
      `coinBalance`: one fixture where the two happen to agree cannot tell a window from a column.
      (`ADR-0061` §4 and its recorded cost — this criterion replaces the *"agree with
      `player.coin_balance`"* one the story was written with, which that ADR contradicts on purpose.)
- [ ] The window excludes the neighbouring season in both directions: a duel finished just before the
      season began and one finished just after it ended are both absent from the page, and one
      finished inside it is present. Three inputs. **Inherited from `STORY-0505`**, which was dropped
      because a boundary runs no code — this assertion was the one thing it owned that nothing else
      did.
- [ ] A player whose only duel in the season was a **draw** has a row, at `0`, asserted beside a
      player who did not play that season and has none. (`ADR-0015` and `ADR-0061` §4: results, not
      players.)
- [ ] The response names the season it was computed for, and a test asserts the endpoint answers for
      the season the server's clock is in — moved, not waited for.
- [ ] `docs/protocol.md` contracts the endpoint, every parameter, and what each one refuses,
      including which season it serves and that it serves no other, **and the three answers the
      requesting player's own standing has** — a rank and a standing, no place this season, or absent
      (`ADR-0065` §4).
- [ ] `./gradlew :poker-engine:check` passes with no change to `poker-engine`.

## Out of scope

- **The screen** — `STORY-0503`. This story ships no `.tsx`.
- **An index for the season window** — **named, not written** (`ADR-0066` §8, `CLAUDE.md` #4). One
  ticket for the planner: `duel (finished_at)` in a new `V7__` migration, carrying an
  `EXPLAIN`-backed measurement in its `verify:` block rather than a hunch. It is **not** a
  prerequisite for this story — the tables hold hundreds of rows, nothing here has ever been timed,
  and an index added on imagination is a permanent write cost on the one transaction where a coin
  moves. `duel_result`'s primary key already serves the join in the direction this query drives it.
- **Caching, materialising or pre-rendering the ladder** — `ADR-0066` §1 and §8. The response is
  per-requester (`ADR-0065`), so there is no public document to cache; `EPIC-07` pays that bill if
  there is one. A ticket here that adds a table, a view or a cache is contradicting a merged ADR.
- **A live walk** — `ADR-0066` §4. No page after the first shows a duel that finished during the
  walk, by construction, and no `ServerMessage` pushes a ladder change (the epic's out-of-scope table
  says why). Making a walk live is a new decision, not a ticket.
- **Another player's profile or their duel list** — `STORY-0504`, and only if `DEC-057` says so.
  `GET /api/me/duels` stays `me`-only in this story, asserted by leaving its tests untouched.
- **Ending a season, or archiving one** — nowhere. `ADR-0061` §5: a boundary runs no code, nothing is
  archived, and `STORY-0505` is `dropped`.
- **Serving a season other than the current one** — nowhere in v0.3 (`ADR-0061` §7). This endpoint
  takes no season parameter and a past season is unreachable, which is `DEC-060`'s to revisit. The
  data is not gone; the route is simply not offered.
- **A `ServerMessage` that pushes ladder changes** — nowhere yet; the epic's out-of-scope table says
  why, and this story's *no `PROTOCOL_VERSION` step* is the enforcing consequence.
- **A rating or points** — `ADR-0014` says a floating balance supersedes it. Not here.
- **A tiebreak that measures play** — `ADR-0064` §4. The query needs a deterministic total order and
  `ADR-0066` §3 chose `player.id` for it, but a key of the form *fewer duels*, *beat a stronger
  opponent* or
  *reached it first* is a second ranking rule, is not this story's to add, and would supersede
  `ADR-0014` by accident. The order among equals is never presented, and no field on the response
  carries it.
- **A count of how many players share a rank, or any marker on a tied row** — `ADR-0064` §5: v0.3
  prints the repeated number and nothing else, so this response carries no tie field.
- **A standing, a rank or a season on `GET /api/me`** — `ADR-0065` §2. `ProfileResponse` is untouched
  by this story and the profile strip keeps the all-time counter; the whole-ladder aggregate stays off
  the route that runs on every lobby load. Asserted by leaving `ProfileReads`' tests untouched.
- **A *jump to me* parameter, a page-containing-player-X query, or a ladder total** — `ADR-0065` §5
  and §7. The self standing is the whole answer in v0.3; each of these is an ordinary ticket if it is
  ever wanted, and none is a gap in this endpoint.
- **A standing for anybody but the requester** — `ADR-0065` §3. The requester is identified by the
  `X-Device-Id` header the other reads already carry; **no `playerId` parameter**, because an endpoint
  answering *what is player X's standing* for any X asked is `DEC-057`'s question and this story does
  not pre-empt it.
