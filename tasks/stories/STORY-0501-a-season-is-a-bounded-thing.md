---
id: STORY-0501
title: A season is a bounded thing, and every finished duel belongs to one
type: story
status: ready
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

**Settled by [`ADR-0061`](../../docs/adr/ADR-0061-a-season-is-a-calendar-month-and-the-coin-never-resets.md),
which answers `DEC-055` and unblocks this story.** Every question this story was waiting on has an
answer, and the answers are the specification:

- **A derived range, not a row** (§3). No `season` table, no season column, **no migration**, no
  seed, no configuration and no operator. *Which season is it* is a function of
  `ServerClock.nowMillis()`; *which season was that duel in* is a function of `finished_at`. A task
  that proposes a table has misread the ADR as thoroughly as one that proposes a backfill.
- **One calendar month, in UTC** (§1), identified by its month — `2026-08`. That identifier is the
  only one a season has, and it is a wire form: what a *player* reads is `August 2026`, on the
  screen, and that is `STORY-0503`'s.
- **Half-open bounds** (§1): `[first instant of the month, first instant of the next month)`.
  Consecutive seasons neither gap nor overlap, and a duel finishing **exactly** on a boundary
  instant belongs to the **new** season. That sentence is the answer to the third criterion below.
- **Attribution is by `finished_at`, never by a start time** (§2). A duel that began on 31 August
  and ended on 1 September is a September duel in full, because the coin is paid once, at the end.
- **`current` is a computation, not a stored flag** (§3), so two callers cannot disagree about it
  and nothing has to be kept in step.

## Tasks

| ID | Title | Status |
| --- | --- | --- |
| — | *Not split. Unblocked 2026-08-19 by `ADR-0061` — run `/plan-story STORY-0501`.* | — |

## Acceptance criteria

Six were written before `DEC-055` was answered and still hold — one of them now names *which* season
a duel on the boundary falls into, which it could not before. Three are added by
[`ADR-0061`](../../docs/adr/ADR-0061-a-season-is-a-calendar-month-and-the-coin-never-resets.md).

- [ ] Asking which season a duel belongs to returns the same answer for the same duel every time,
      asserted twice against a fixed clock.
- [ ] Two duels that finished a known distance apart, straddling a boundary, are attributed to
      different seasons — and two either side of *no* boundary to the same one. Both directions
      asserted, from two inputs, so the assertion cannot pass on a constant.
- [ ] A duel that finished exactly on a boundary instant is attributed to the **new** season —
      `ADR-0061` §1's half-open bound, asserted at exactly `00:00:00Z` on the first of a month and
      not one millisecond either side of it.
- [ ] Nothing this story adds changes `player.coin_balance` or any `duel_result` row: a test reads
      both before and after exercising every path added here and asserts they are byte-identical.
- [ ] The season is derived from `ServerClock`, asserted by a test that moves the clock rather than
      by sleeping.
- [ ] A season identifier is the month it names — `2026-08` — asserted for at least a December and a
      January, so a year boundary cannot pass on the month arithmetic alone.
- [ ] Attribution uses `finished_at` and nothing else: a duel whose start and finish fall in
      different months is attributed to the month it **finished** in, asserted with a fixture whose
      start would give the other answer.
- [ ] **This story adds no migration.** Asserted by there being no new `V<n>__` file in the branch —
      `ADR-0061` §3 makes a season derived, and a schema change here is a misreading, not a choice.
- [ ] `./gradlew :poker-engine:check` passes with no change to `poker-engine`.

## Out of scope

- **Reading a season over HTTP** — `STORY-0502`. This story adds no route, so it can add no route
  test; the refusal is real work moved, not a rule dropped.
- **What happens when a season ends** — nothing, and `STORY-0505` is `dropped` because of it
  (`ADR-0061` §5). This story adds a boundary and nothing acts on one anywhere, which is why the
  no-coin-moved criterion above is a test rather than a sentence: it is now the *only* place that
  property is checked.
- **A season a player can read the name of** — that is a screen, `STORY-0503`.
- **Rating, points, or any second number** — `ADR-0014` says a floating balance supersedes it, and
  no story in this epic does that.
