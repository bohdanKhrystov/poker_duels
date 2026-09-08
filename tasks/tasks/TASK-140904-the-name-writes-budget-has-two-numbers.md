---
schema: 2
id: TASK-140904
title: The name write's budget has two numbers
type: task
status: done
parent: STORY-1409
module: poker-server
estimate: XS
tier: haiku
review: light
files_touched: 2
labels: [server, config, account]
depends_on: [TASK-140903]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.config.ServerConfigTest' -PrequireDocker=true
  - grep -q 'tests="47" skipped="0" failures="0" errors="0"' poker-server/build/test-results/test/TEST-duels.poker.server.config.ServerConfigTest.xml
  - grep -qF nameWriteLimits poker-server/src/main/kotlin/duels/poker/server/config/ServerConfig.kt
  - grep -qF AUTH_NAME_WRITE_MAX_ATTEMPTS poker-server/src/main/kotlin/duels/poker/server/config/ServerConfig.kt
  - grep -qF AUTH_NAME_WRITE_WINDOW_MILLIS poker-server/src/main/kotlin/duels/poker/server/config/ServerConfig.kt
  - ./gradlew :poker-server:ktlintCheck
  - ./gradlew :poker-server:detekt
  - ./gradlew check -PrequireDocker=true
  - python3 .github/scripts/lint_tickets.py
---

## Goal

`ServerConfig` carries `nameWriteMaxAttempts` (default `5`) and `nameWriteWindowMillis` (default
`60_000`), each resolvable from the environment then the file then the default, and bundles them as
`nameWriteLimits(): AttemptLimits` — the fifth pair, built exactly like the four that ship.

## Files

Two. `ADR-0134` §6 fixes the shape and the numbers; both fields take defaults, so no construction
site anywhere else moves and nothing is `atomic:` here.

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/config/ServerConfig.kt` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/config/ServerConfigTest.kt` | modify |

Read, and do not edit:
[`ADR-0134`](../../docs/adr/ADR-0134-a-rename-spends-before-it-replaces.md) §6;
`poker-server/src/main/kotlin/duels/poker/server/auth/AttemptBudget.kt` — `AttemptLimits`'s two
fields.

## Scope

- **Two fields with defaults**, beside `recoveryEmailWindowMillis`: `nameWriteMaxAttempts: Int = 5`
  and `nameWriteWindowMillis: Long = 60_000L`.
- **Six constants**, following the four shipped pairs exactly:
  `DEFAULT_NAME_WRITE_MAX_ATTEMPTS`, `NAME_WRITE_MAX_ATTEMPTS_KEY` (`"auth.nameWriteMaxAttempts"`),
  `AUTH_NAME_WRITE_MAX_ATTEMPTS`, and the three window equivalents
  (`"auth.nameWriteWindowMillis"`, `AUTH_NAME_WRITE_WINDOW_MILLIS`).
- **Two `resolve` blocks and two `requireNotNull` conversions** in `from`, with messages in the
  house shape (*"name-write max attempts must be an integer, got: …"*), and the two new arguments
  in the returned `ServerConfig(...)`.
- **`nameWriteLimits(): AttemptLimits`**, beside the other four bundlers, with a one-line KDoc.

## Out of scope

- **Building an `AttemptBudget`.** `TASK-140905`.
- **Anything in `ProfileRoutes`.** `TASK-140906`.
- **Changing the four shipped budgets' numbers, keys or env names.**
- **Deciding the numbers.** `ADR-0134` §6 states `5` and `60_000` and says they are configuration
  whose first real player they bite is the evidence they are too tight.

## Tests

`ServerConfigTest` — 44 tests on `develop` at `1c3c7fd9`, 47 after. The three new cases mirror
`recoveryEmailIsFiveAMinuteWithNothingConfigured` and its two neighbours line for line.

| Test | Proves |
| --- | --- |
| `nameWriteIsFiveAMinuteWithNothingConfigured` | with an empty config and an environment that answers nothing, `nameWriteLimits()` is `AttemptLimits(5, 60_000L)` |
| `nameWriteReadsItsTwoNumbersFromTheFile` | with `auth.nameWriteMaxAttempts = "7"` and `auth.nameWriteWindowMillis = "222000"` in the config, `nameWriteLimits()` is `AttemptLimits(7, 222_000L)` |
| `theEnvironmentOutranksTheFileForTheNameWriteBudget` | with **both** the file values above **and** `AUTH_NAME_WRITE_MAX_ATTEMPTS = "4"` / `AUTH_NAME_WRITE_WINDOW_MILLIS = "444000"` in the environment, `nameWriteLimits()` is `AttemptLimits(4, 444_000L)` |

## What would still pass if the coder got it wrong

- **Two inputs that disagree, on purpose.** The file test uses `7 / 222_000` and the environment
  test uses `4 / 444_000` **on top of** the same file values. A `nameWriteLimits()` wired to the
  defaults passes neither; one wired to the file and ignoring the environment passes the second and
  fails the third; and none of the three numbers is `5`, `60_000` or `0`, so no value a bug leaves
  unchanged can satisfy any of them.
- **The two numbers differ from each other in every case** (`7` vs `222000`, `4` vs `444000`), so a
  `nameWriteLimits()` that read the max attempts into both slots reddens.
- **If the two constants were pointed at sign-in's keys**, the first two tests still pass — the
  defaults happen to be `10 / 60_000` for sign-in, not `5 / 60_000`, so the default test catches
  the max-attempts half; the file test catches the rest, because sign-in's keys are absent from the
  config the test builds.

## Acceptance criteria

- [ ] `ServerConfigTest.nameWriteIsFiveAMinuteWithNothingConfigured` passes
- [ ] `ServerConfigTest.nameWriteReadsItsTwoNumbersFromTheFile` passes
- [ ] `ServerConfigTest.theEnvironmentOutranksTheFileForTheNameWriteBudget` passes
- [ ] `ServerConfigTest` reports exactly 47 tests, `failures="0" errors="0"` — 44 measured on
      `develop` at `1c3c7fd9` plus exactly the three above
- [ ] `nameWriteLimits`, `AUTH_NAME_WRITE_MAX_ATTEMPTS` and `AUTH_NAME_WRITE_WINDOW_MILLIS` all
      appear in `ServerConfig.kt`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
