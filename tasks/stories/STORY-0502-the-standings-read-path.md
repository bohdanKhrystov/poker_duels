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

**Blocked on four decisions**, each of which changes the query rather than decorating it:

- `DEC-055` — the scope. An all-time ladder is an `ORDER BY` over `player.coin_balance`; a season
  ladder is a `SUM(duel_result.coin_delta)` over a join to `duel`. Different query, different index,
  different tests.
- `DEC-056` — the eligibility predicate, including whether a player with no display name has a row.
- `DEC-058` — whether tied players share a rank number, which decides whether *rank* and *position
  in the page* are one field or two.
- `DEC-059` — whether a player can learn their own standing without walking to it. One player's rank
  is a different query from a page of them, and if the answer puts a standing on the profile strip
  it is a field on `ProfileResponse` rather than anything on this endpoint.

**Two architect decisions will be raised when this is split**, not before, because their premise is
`DEC-055`'s answer: what a cursor over a mutable ordering guarantees, and whether a standing is
computed per request or materialised.

## Tasks

| ID | Title | Status |
| --- | --- | --- |
| — | *Not split. Blocked on `DEC-055`, `DEC-056`, `DEC-058` and `DEC-059` — run `/plan-story STORY-0502` once they are answered.* | — |

## Acceptance criteria

These hold under every answer the three decisions can give; the answers add more.

- [ ] Players come back in coin order, asserted against a fixture holding at least one positive, one
      zero and one **negative** balance — three inputs, so the ordering cannot pass on a constant.
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
- [ ] The coin totals the endpoint reports agree with `player.coin_balance` for every player in the
      fixture. A ladder that disagrees with the profile strip is the defect this epic exists to
      avoid.
- [ ] `docs/protocol.md` contracts the endpoint, every parameter, and what each one refuses.
- [ ] `./gradlew :poker-engine:check` passes with no change to `poker-engine`.

## Out of scope

- **The screen** — `STORY-0503`. This story ships no `.tsx`.
- **Another player's profile or their duel list** — `STORY-0504`, and only if `DEC-057` says so.
  `GET /api/me/duels` stays `me`-only in this story, asserted by leaving its tests untouched.
- **Ending a season, or archiving one** — `STORY-0505`.
- **A `ServerMessage` that pushes ladder changes** — nowhere yet; the epic's out-of-scope table says
  why, and this story's *no `PROTOCOL_VERSION` step* is the enforcing consequence.
- **A rating or points** — `ADR-0014` says a floating balance supersedes it. Not here.
