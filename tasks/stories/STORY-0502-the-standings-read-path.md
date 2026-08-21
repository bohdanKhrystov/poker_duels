---
id: STORY-0502
title: The standings read path — ordered, paged, and a rank the server computes
type: story
status: blocked
parent: EPIC-05
module: poker-server
labels: [server, http, read-path, leaderboard]
depends_on: [STORY-0501]
---

## Goal

One HTTP endpoint answers *where does everybody stand*: an ordered page of players with their duel
coins and a rank the server worked out, scoped to a season, walkable to the end without gaps or
duplicates.

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
- **The key that gives the ladder a deterministic total order is `DEC-061`'s, and it is invisible.**
  `ADR-0064` §4 constrains it in one product-facing way and chooses nothing: it is a fact about who
  a row is — player id, name collation, profile age — and never about how that player did, because a
  tiebreak on duels played or on who reached a standing first is a second ranking rule and
  `ADR-0014` reserves one for an ADR that supersedes it.
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
- **Paging is total and disjoint or it says why not.** `STORY-0408` pinned that property for
  `GET /api/me/duels` and [`ADR-0057`](../../docs/adr/ADR-0057-a-cursor-names-the-filter-it-was-drawn-under.md)
  bound a cursor to the filter that drew it. **It cannot be inherited here**: a standings order is
  keyed on a number that changes while the walk is in progress, because somebody wins a duel between
  page one and page two. Whatever this story guarantees, it guarantees explicitly.
- **One query per page**, as the history read does.
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
  season window and whatever paging `DEC-061` settles, this query adds **no `WHERE` at all**, and a
  predicate that appears in a ticket here is a defect rather than a refinement. Two properties of §4
  follow and are criteria below: a rank is a position among **everyone** who played the season, and
  the standings of a whole season **sum to exactly zero**.
- **The ladder's number is not `player.coin_balance`**, and from the second season on they differ for
  anyone who played in both. `EPIC-05`'s non-negotiable about the two agreeing is contradicted on
  purpose by that ADR, in writing, and the criteria below are rewritten to match rather than left to
  fail.

**Still blocked on one product decision**, which changes the query rather than decorating it, and on
one the answer to `DEC-055` raised. Two of the original gate are **answered**: `DEC-056` — the
eligibility predicate — by `ADR-0063`, and `DEC-058` — the tie — by
[`ADR-0064`](../../docs/adr/ADR-0064-tied-players-share-one-rank-and-row-order-is-not-a-ranking.md),
which settles that *rank* and *position in the page* are two numbers and only the first is served:

- `DEC-059` — whether a player can learn their own standing without walking to it. One player's rank
  is a different query from a page of them, and if the answer puts a standing on the profile strip
  it is a field on `ProfileResponse` rather than anything on this endpoint. It now also decides
  *which* number the strip shows, since there are two.
- `DEC-061` — **the architect's, raised by `ADR-0061`.** Is a standing computed per request or
  materialised, and what does a page guarantee over an ordering that is *recomputed* while it is
  walked? The epic had parked these as two unnumbered questions for this story to raise at split
  time; the ADR supplied their premise and merged them, so this story now **waits** on them instead.

## Tasks

| ID | Title | Status |
| --- | --- | --- |
| — | *Not split. Blocked on `DEC-059` and `DEC-061` — run `/plan-story STORY-0502` once they are answered. `DEC-056` is answered by `ADR-0063`, `DEC-058` by `ADR-0064`.* | — |

## Acceptance criteria

These hold under every answer the open decisions can give; the answers add more. The four that name
`ADR-0061` are what `DEC-055`'s answer added or rewrote, and the three naming `ADR-0064` are what
`DEC-058`'s answer added.

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
- [ ] Walking every page returns each eligible player exactly once — no gap, no duplicate — under
      whatever guarantee this story states, and the guarantee is written in `docs/protocol.md`.
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
      including which season it serves and that it serves no other.
- [ ] `./gradlew :poker-engine:check` passes with no change to `poker-engine`.

## Out of scope

- **The screen** — `STORY-0503`. This story ships no `.tsx`.
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
  `DEC-061` chooses the key, but a key of the form *fewer duels*, *beat a stronger opponent* or
  *reached it first* is a second ranking rule, is not this story's to add, and would supersede
  `ADR-0014` by accident. The order among equals is never presented, and no field on the response
  carries it.
- **A count of how many players share a rank, or any marker on a tied row** — `ADR-0064` §5: v0.3
  prints the repeated number and nothing else, so this response carries no tie field.
