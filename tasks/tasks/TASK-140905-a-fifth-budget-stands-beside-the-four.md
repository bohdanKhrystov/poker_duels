---
schema: 2
id: TASK-140905
title: A fifth budget stands beside the four
type: task
status: backlog
parent: STORY-1409
module: poker-server
estimate: XS
tier: haiku
review: light
files_touched: 2
labels: [server, account]
depends_on: [TASK-140904]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.ServerComponentsTest' -PrequireDocker=true
  - grep -q 'tests="6" skipped="0" failures="0" errors="0"' poker-server/build/test-results/test/TEST-duels.poker.server.ServerComponentsTest.xml
  - grep -qF nameWriteBudget poker-server/src/main/kotlin/duels/poker/server/ServerComponents.kt
  - grep -qF "config.nameWriteLimits()" poker-server/src/main/kotlin/duels/poker/server/ServerComponents.kt
  - ./gradlew :poker-server:ktlintCheck
  - ./gradlew :poker-server:detekt
  - ./gradlew check -PrequireDocker=true
  - python3 .github/scripts/lint_tickets.py
---

## Goal

`ServerComponents` carries `nameWriteBudget: AttemptBudget`, built from `config.nameWriteLimits()`
over the same `ServerClock` the other budgets take — its **own** instance, so a name write can
never spend sign-in's or sign-up's budget.

## Files

Two. `serverComponents(...)` is the only construction site of the data class in this repository —
measured — so a field with no default breaks nothing else.

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/ServerComponents.kt` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/ServerComponentsTest.kt` | modify |

Read, and do not edit:
[`ADR-0134`](../../docs/adr/ADR-0134-a-rename-spends-before-it-replaces.md) §6;
[`ADR-0055`](../../docs/adr/ADR-0055-sign-up-is-five-attempts-a-quarter-hour-from-one-address.md) §2 —
why the monotonic `ServerClock` and not the wall clock.

## Scope

- **One field on the data class**, `val nameWriteBudget: AttemptBudget`, immediately after
  `signInBudget`, and its argument in the returned `ServerComponents(...)`.
- **One construction in the factory**, `AttemptBudget(config.nameWriteLimits(), clock)`, beside the
  two that ship — the `ServerClock` parameter, never `wallClock`.
- **The comment above those constructions gains one clause**: three instances now, and one shared
  instance would let a name write spend sign-in's budget, which is the failure that comment already
  warns about.

## Out of scope

- **Passing it to any route.** `TASK-140906`.
- **Touching `signUpBudget` or `signInBudget`.**
- **Wiring the two recovery budgets `ServerConfig` already carries.** They are unwired today; that
  is not this story's defect and stays as it is.

## Tests

`ServerComponentsTest` — 5 tests on `develop` at `1c3c7fd9`, 6 after.

| Test | Proves |
| --- | --- |
| `theNameWriteBudgetIsItsOwnInstanceOnItsOwnNumbers` | build components from a config whose `nameWriteMaxAttempts` is `1` and whose `signInMaxAttempts` is `10`; `nameWriteBudget.admit("k")` is `true` then `false`, while `signInBudget.admit("k")` on the **same key** is still `true` — one budget's exhaustion leaves the other untouched |

## What would still pass if the coder got it wrong

- **If `nameWriteBudget` were aliased to `signInBudget`**, a test that only asserted the field is
  non-null, or that admitted once, would pass. The two-budget, same-key assertion is the only shape
  that catches it, and it needs the **same key** — a different key would pass against a shared
  instance too.
- **If it were built from `signInLimits()`**, the first `admit` and the second both answer `true`
  (sign-in allows ten), so the `false` on the second call is what fails. The config the test builds
  therefore sets the two maxima to values that **disagree** — `1` and `10` — because a config where
  both were `5` could not tell the two bundlers apart.
- **If it took `wallClock`**, nothing in this test moves. That is unreachable by a unit test here
  and is a `verify` grep for `config.nameWriteLimits()` plus review, not a test claim.

## Acceptance criteria

- [ ] `ServerComponentsTest.theNameWriteBudgetIsItsOwnInstanceOnItsOwnNumbers` passes
- [ ] `ServerComponentsTest` reports exactly 6 tests, `failures="0" errors="0"` — 5 measured on
      `develop` at `1c3c7fd9` plus the one above
- [ ] `nameWriteBudget` and `config.nameWriteLimits()` both appear in `ServerComponents.kt`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
