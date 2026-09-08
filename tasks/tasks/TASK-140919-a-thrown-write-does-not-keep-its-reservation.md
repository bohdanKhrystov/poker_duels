---
schema: 2
id: TASK-140919
title: A thrown write does not keep its reservation
type: task
status: done
parent: STORY-1409
module: poker-server
estimate: S
tier: sonnet
review: deep
files_touched: 3
labels: [server, budget, reliability]
depends_on: [TASK-140906]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.http.ProfileRouteTest' -PrequireDocker=true
  - sh -c 'grep -qF "aWriteThatThrowsKeepsNoBudget" poker-server/src/test/kotlin/duels/poker/server/http/ProfileRouteTest.kt'
  - ./gradlew :poker-server:ktlintCheck
  - ./gradlew :poker-server:detekt
  - ./gradlew check -PrequireDocker=true
  - python3 .github/scripts/lint_tickets.py
---

## Goal

`admit` records a reservation; `refund` returns it when the result is not `NameSet`. If
`writes.setDisplayName` **throws** between the two, the refund never runs and the reservation is
kept for the rest of the window.

The player it is kept against is one whose write failed for a reason that is not theirs — a database
error — and the budget it consumes is the one `ADR-0134` §6 exists to keep *out* of their way.

## Why this is its own ticket

Found by the deep review of `TASK-140906`, probing whether any path leaves a reservation unrefunded.
`ProfileRoutes` has no `try/finally` around the write, and `PostgresProfileWrites.writeName` rethrows
any `SQLException` whose state is not the unique-violation code.

**It is not a regression and `TASK-140906` was right to pass.** `ADR-0134` §6 speaks only in terms of
`SetNameResult` outcomes, and the same unguarded shape already ships in `AuthRoutes`'s sign-in
reserve-then-refund, which that ticket was told to reuse rather than rebuild. So this is a
pre-existing pattern, surfaced by a probe rather than introduced by a diff.

It is worth fixing because a leaked reservation throttles exactly the player §6 protects, and because
a rate limit that quietly tightens after a database blip is the kind of fault nobody reports and
nobody can reproduce.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/http/ProfileRoutes.kt` | modify |
| `poker-server/src/main/kotlin/duels/poker/server/http/AuthRoutes.kt` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/http/ProfileRouteTest.kt` | modify |

Read [`ADR-0134`](../../docs/adr/ADR-0134-a-rename-spends-before-it-replaces.md) §6 and
[`ADR-0074`](../../docs/adr/ADR-0074-a-sign-in-refunds-the-attempt-it-proves.md) §2 — the
reserve-then-refund shape both call sites use. **Nothing outside the table above is changed**; in
particular `AttemptBudget` itself is not opened.

## Scope

The reservation is returned when the write throws, at both call sites.

Whether that is `try/finally`, a `catch` that refunds and rethrows, or restructuring so the refund is
unconditional is the implementer's call — but the exception must **still propagate**. Swallowing a
database error to protect a budget would trade a small fault for a large one.

`AuthRoutes` is in the table because leaving it unguarded while fixing `ProfileRoutes` would leave two
call sites that look alike and behave differently, which is worse than either state.

## Out of scope

- **`AttemptBudget`**, which is correct — it records what it is told to record.
- **Changing what any route answers.** A thrown write still surfaces as it does today.
- **The `429` body or headers** — `ADR-0134` §6 settled those.

## Tests

| Test | What it refuses |
| --- | --- |
| `aWriteThatThrowsKeepsNoBudget` | a reservation kept by a write that failed for a reason the player did not cause |

Drive a `ProfileWrites` that throws, then confirm the player's later writes still succeed to the full
budget. A test that only asserts the throw surfaced would pass against the current code.

## Acceptance criteria

1. The new test **fails against `develop`** and passes with the change; report both.
2. The exception still propagates — a test or an existing case confirms the route does not swallow it.
3. `AuthRoutes`'s sign-in path gets the same treatment, and its existing tests stay green.

## Definition of done

- [ ] Every command in `verify:` exits 0.
- [ ] The new test was observed red before the fix.
- [ ] PR merged into `develop`.
