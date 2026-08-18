---
schema: 2
id: TASK-040905
title: The search term is not a language, and an unnamed opponent is not a match
type: task
status: ready
parent: STORY-0409
module: poker-server
estimate: XS
tier: haiku
review: standard
files_touched: 1
labels: [server, db, read-path, history, search, tests]
depends_on: [TASK-040904]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.db.PostgresProfileReadsTest' -PrequireDocker=true
  - ./gradlew :poker-server:check -PrequireDocker=true
---

## Goal

Three assertions pin the two properties of the search that `TASK-040904`'s implementation gets for
free and a later change to `ILIKE` would silently take away.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/test/kotlin/duels/poker/server/db/PostgresProfileReadsTest.kt` | modify — three tests, no production change |
| `poker-server/src/main/kotlin/duels/poker/server/db/PostgresProfileReads.kt` | read — the clause under test |

## Scope

**No production code changes in this ticket.** All three tests pass the moment `TASK-040904` is
merged, and that is the point: they are what makes the choice of `POSITION` over `ILIKE` a pinned
guarantee rather than an accident of one commit. Each one is written against a named wrong
implementation, and each fails against it — measured against `postgres:16-alpine` before this ticket
was written, not assumed:

| Term | `POSITION` answers | `ILIKE '%' \|\| term \|\| '%'` answers |
| --- | --- | --- |
| `%Sure` | `100%Sure` only | `100%Sure` **and** `1004Sure` |
| `a_b` | `a_b` only | `a_b` **and** `axb` |

Use the file's own `setPlayerDisplayName`, which writes `player.display_name` directly. A name like
`100%Sure` never passes `canonicalDisplayNameOrNull`'s character rules on the way in through
`PUT /api/me/name`, but it satisfies every `CHECK` the column carries (`1..32` characters, `btrim`ed,
NFC) and so is a legitimate row — and a term a client sends is under no such control at all, which
is why the search must be tested against one.

## Out of scope

- Any change to `PostgresProfileReads.kt`. If a test here fails, the fix belongs in `TASK-040904`'s
  clause, not in a new escape rule.
- Refusing `%` or `_` in `opponentSearchOrNull`. They are ordinary characters in a name and are
  matched, not rejected — `TASK-040902` deliberately refuses neither.
- Any change to an existing test or helper in this file.

## Tests

`PostgresProfileReadsTest`, `-PrequireDocker=true`. Each builds its own two-opponent fixture with
`playerDirectory.resolve`, `setPlayerDisplayName` and `finishedDuel(winner = 0, opponent = …,
finishedAt = …)`, with `finishedAt` at or after `2026-08-13T10:00:00Z`.

| Test | Proves |
| --- | --- |
| `aPercentInASearchTermMatchesLiterally` | opponents named `100%Sure` and `1004Sure`, one duel each; the term `%Sure` returns exactly the `100%Sure` duel's id. Fails against `ILIKE '%' \|\| term \|\| '%'`, which returns both — the specific wrong implementation this test exists to catch |
| `anUnderscoreInASearchTermMatchesLiterally` | opponents named `a_b` and `axb`, one duel each; the term `a_b` returns exactly the `a_b` duel's id. Fails against the same wrong implementation, which returns both. `%` alone would not catch it: `_` is the wildcard a naive escape most often forgets |
| `anOpponentWithNoNameMatchesNoSearch` | one opponent named `Halvard` and one with `display_name` left `NULL`, one duel each; the term `a` returns exactly the `Halvard` duel's id and the unnamed opponent's duel is absent. Fails against a clause written `… OR p.display_name IS NULL` — the plausible "don't lose the unnamed ones" repair — and against any `COALESCE(p.display_name, …)` whose placeholder happens to contain the term. The server invents no name to match against (`ADR-0029` §6) |

Each asserts the exact list of duel ids returned, never `size` alone: with two rows in the fixture, a
size of one cannot say *which* one.

## Acceptance criteria

- [ ] `PostgresProfileReadsTest.aPercentInASearchTermMatchesLiterally` passes
- [ ] `PostgresProfileReadsTest.anUnderscoreInASearchTermMatchesLiterally` passes
- [ ] `PostgresProfileReadsTest.anOpponentWithNoNameMatchesNoSearch` passes
- [ ] Each of the three fixtures holds **two** opponents, so each assertion names one id out of two
      candidates rather than confirming the only row present
- [ ] `poker-server/src/main/kotlin/` is not in the diff
- [ ] Every test already in `PostgresProfileReadsTest` passes with its assertions unchanged
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
