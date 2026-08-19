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
- **The read follows the shape `EPIC-04` already built.** A port in
  `duels.poker.server.http` with a Postgres implementation in `duels.poker.server.db`, so no route
  holds a `DataSource` (`ADR-0011`), alongside `ProfileReads`/`PostgresProfileReads`. The response
  type is the wire type, as `ProfileReads`' KDoc explains — no parallel domain type nobody reads.
- **Nothing on this path creates anything.** `ProfileReads`' rule holds here too: a crawler walking
  the ladder mints no `player` row.
- **Negative balances are ordinary and sort where they belong.** `ADR-0014`: a first loss is `−1`.
  Not clamped, not filtered out of the tail, not an error.
- **A null display name is `null` on the wire.** `ADR-0029` §6 — the server fabricates no
  placeholder, and `No name` is a client string (`ADR-0058`). Whether a nameless player has a row at
  all is `DEC-056`'s, and is a different question from what the field carries.
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
- **The endpoint serves the current season** (§6), computed from `ServerClock`, and **the response
  names the season it computed** so the client never derives one from the browser's clock
  (`ADR-0002`). The field's name and shape are the architect's; that there is one is not.
- **`DEC-056` may narrow the eligible set; it may not widen it.**
- **The ladder's number is not `player.coin_balance`**, and from the second season on they differ for
  anyone who played in both. `EPIC-05`'s non-negotiable about the two agreeing is contradicted on
  purpose by that ADR, in writing, and the criteria below are rewritten to match rather than left to
  fail.

**Still blocked on three decisions**, each of which changes the query rather than decorating it, and
on one the answer to `DEC-055` raised:

- `DEC-056` — the eligibility predicate, including whether a player with no display name has a row.
- `DEC-058` — whether tied players share a rank number, which decides whether *rank* and *position
  in the page* are one field or two.
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
| — | *Not split. Blocked on `DEC-056`, `DEC-058`, `DEC-059` and `DEC-061` — run `/plan-story STORY-0502` once they are answered.* | — |

## Acceptance criteria

These hold under every answer the four open decisions can give; the answers add more. The four that
name `ADR-0061` are what `DEC-055`'s answer added or rewrote.

- [ ] Players come back in coin order, asserted against a fixture holding at least one positive, one
      zero and one **negative** season standing — three inputs, so the ordering cannot pass on a
      constant.
- [ ] The rank is a field in the response and is correct on the **second** page, asserted by walking
      to it. A client counting rows would be wrong here and the test says so.
- [ ] Walking every page returns each eligible player exactly once — no gap, no duplicate — under
      whatever guarantee this story states, and the guarantee is written in `docs/protocol.md`.
- [ ] The nameless case is asserted in whichever direction `DEC-056` chose, and there is no third
      option: **either** a player with no display name has a row and it carries `null` — never a
      placeholder — asserted beside a named player in the same page, **or** they have no row and the
      test asserts their absence from a page they would otherwise sort into. One of these two tests
      exists.
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
