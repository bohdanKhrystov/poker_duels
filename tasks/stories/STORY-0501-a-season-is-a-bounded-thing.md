---
id: STORY-0501
title: A season is a bounded thing, and every finished duel belongs to one
type: story
status: blocked
parent: EPIC-05
module: poker-server
labels: [server, seasons, persistence]
depends_on: []
---

## Goal

One place in the server says which season is current, and which season a given finished duel falls
in. Nothing reads it yet. After this story, every later query can scope itself to a season by
asking rather than by deriving.

## Why

It is a prerequisite, and the reason is specific rather than tidy-mindedness: a standings query
cannot scope itself to something with no representation, and if two queries each work out a season
boundary their own way they will disagree the first time one of them is off by an hour, a time
zone, or an inclusive endpoint. `EPIC-02` shipped `duel.finished_at` as `TIMESTAMPTZ NOT NULL` on
every row, so the raw material exists; what does not exist is a single answer to *which season is
that*.

## Design notes

**Settled, and the tasks must respect all of it:**

- **A season is a server fact.** [`ADR-0002`](../../docs/adr/ADR-0002-server-authoritative.md): no
  client asserts which season it is, which season a duel was in, or when one ends. A query
  parameter naming a season is a *request*, and the server decides whether it is a real one.
- **Time comes from `ServerClock.nowMillis()`** (`duels.poker.server.time.ServerClock`), never from
  `Instant.now()` or a system clock read inline. A season boundary is the first piece of product
  behaviour that is a function of the wall clock, and it must be testable without waiting.
- **No backfill, under any answer.** `duel.finished_at` is `NOT NULL` on every row already written
  and `duel_result.coin_delta` is the signed award, so any window over the record is reconstructible
  from what is stored. A task that proposes rewriting existing rows has misread the schema.
- **If a migration is needed it is a new `V<n>__` file, numbered at merge time**, never an edit to a
  merged one. `V1`–`V6` are taken and `EPIC-04` still has unlanded migrations (`STORY-0416`), so the
  number is claimed when this branch merges and not before.
- **The engine learns nothing.** No season type, constant or boundary crosses into `poker-engine`.

**Blocked on `DEC-055`, and this is what the answer decides here:** whether a season is a row, a
configured window or a derived range; whether it has a name or a number a player ever reads;
whether *current* is a query or a stored flag; and whether the boundary is inclusive at the start,
the end, or neither. Until that lands there is nothing to split — every candidate task is a
different piece of work under each answer, which is the definition of a story that is not ready.

## Tasks

| ID | Title | Status |
| --- | --- | --- |
| — | *Not split. Blocked on `DEC-055` — run `/plan-story STORY-0501` once it is answered.* | — |

## Acceptance criteria

These hold under every answer `DEC-055` can give; the answer adds more, it does not replace them.

- [ ] Asking which season a duel belongs to returns the same answer for the same duel every time,
      asserted twice against a fixed clock.
- [ ] Two duels that finished a known distance apart, straddling a boundary, are attributed to
      different seasons — and two either side of *no* boundary to the same one. Both directions
      asserted, from two inputs, so the assertion cannot pass on a constant.
- [ ] A duel that finished exactly on a boundary instant is attributed to exactly one season, and
      the test names which.
- [ ] Nothing this story adds changes `player.coin_balance` or any `duel_result` row: a test reads
      both before and after exercising every path added here and asserts they are byte-identical.
- [ ] The season is derived from `ServerClock`, asserted by a test that moves the clock rather than
      by sleeping.
- [ ] `./gradlew :poker-engine:check` passes with no change to `poker-engine`.

## Out of scope

- **Reading a season over HTTP** — `STORY-0502`. This story adds no route, so it can add no route
  test; the refusal is real work moved, not a rule dropped.
- **What happens when a season ends** — `STORY-0505`, even if `DEC-055` answers that a boundary
  resets balances. This story adds a boundary; it does not act on one, which is why the
  no-coin-moved criterion above is a test rather than a sentence.
- **A season a player can read the name of** — that is a screen, `STORY-0503`.
- **Rating, points, or any second number** — `ADR-0014` says a floating balance supersedes it, and
  no story in this epic does that.
