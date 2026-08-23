---
schema: 2
id: TASK-040520
title: The sign-up budget is two configuration values with defaults
type: task
status: backlog
parent: STORY-0405
module: poker-server
estimate: XS
tier: haiku
review: light
files_touched: 2
labels: [server, config, auth, rate-limit]
depends_on: [TASK-040519]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.config.ServerConfigTest'
  - ./gradlew :poker-server:ktlintCheck
---

## Goal

The sign-up budget's two numbers are configuration with defaults, not literals, so an operator whose
players sit behind one NAT raises them with an environment variable rather than a deploy.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/config/ServerConfig.kt` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/config/ServerConfigTest.kt` | modify |

Read `ADR-0055` §2's config bullet. Nothing else.

## Scope

- Two fields on `ServerConfig`, in the pattern the file already establishes — a `DEFAULT_`, a
  `_KEY` and an `_ENV` constant each, parsed through the same `resolve` and `requireNotNull`
  shape as `sweepPeriodMillis`:
  - `signUpMaxAttempts` / `auth.signUpMaxAttempts` / `AUTH_SIGN_UP_MAX_ATTEMPTS`, default `5`
  - `signUpWindowMillis` / `auth.signUpWindowMillis` / `AUTH_SIGN_UP_WINDOW_MILLIS`, default `900000`
- A `public fun signUpLimits(): AttemptLimits` beside the existing `roomTimeouts()`, so callers
  take one value rather than two loose numbers.
- **The numbers come from `ADR-0055` §1 and are not re-derived here.** Five requests per fifteen
  minutes per address, metering the requests that reach the hash rather than the ones that fail.

## Out of scope

- The call site — `TASK-040521`.
- Any sign-in budget value. `DEC-069` is open and this ticket adds no key for it; adding one
  "while we are here" would be the guess that decision exists to prevent.

## Tests

`ServerConfigTest` — new methods only, in the shape the file already uses.

| Test | Proves |
| --- | --- |
| `theSignUpBudgetDefaults` | with an empty config and empty environment, `signUpMaxAttempts` is `5` and `signUpWindowMillis` is `900000` |
| `theSignUpBudgetComesFromTheEnvironment` | `AUTH_SIGN_UP_MAX_ATTEMPTS=9` and `AUTH_SIGN_UP_WINDOW_MILLIS=1000` are read — **both, with values that are neither the default nor each other's**, so neither field can be reading the other's source |
| `theSignUpBudgetComesFromTheConfigFile` | the two `auth.` keys are read when the environment is empty |
| `signUpLimitsCarriesBothNumbers` | `signUpLimits()` equals `AttemptLimits(signUpMaxAttempts, signUpWindowMillis)` for a non-default pair |
| `aNonNumericBudgetIsRefused` | `AUTH_SIGN_UP_MAX_ATTEMPTS=many` throws, matching every other numeric key in this file |

## Acceptance criteria

- [ ] All five test methods above pass
- [ ] `grep -rn "signIn" poker-server/src/main/kotlin/duels/poker/server/config/ServerConfig.kt`
      finds nothing
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.
