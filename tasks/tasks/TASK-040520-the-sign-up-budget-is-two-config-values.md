---
schema: 2
id: TASK-040520
title: The two auth budgets are four configuration values with defaults
type: task
status: ready
parent: STORY-0405
module: poker-server
estimate: S
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

Each auth budget's two numbers are configuration with defaults, not literals, so an operator whose
players sit behind one NAT raises them with an environment variable rather than a deploy. Sign-up's
pair and sign-in's are separate values because they are separate budgets with separate reasons.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/config/ServerConfig.kt` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/config/ServerConfigTest.kt` | modify |

Read `ADR-0055` §2's config bullet and `ADR-0074` §1's table. Nothing else.

## Scope

- Four fields on `ServerConfig`, in the pattern the file already establishes — a `DEFAULT_`, a
  `_KEY` and an `_ENV` constant each, parsed through the same `resolve` and `requireNotNull`
  shape as `sweepPeriodMillis`:
  - `signUpMaxAttempts` / `auth.signUpMaxAttempts` / `AUTH_SIGN_UP_MAX_ATTEMPTS`, default `5`
  - `signUpWindowMillis` / `auth.signUpWindowMillis` / `AUTH_SIGN_UP_WINDOW_MILLIS`, default `900000`
  - `signInMaxAttempts` / `auth.signInMaxAttempts` / `AUTH_SIGN_IN_MAX_ATTEMPTS`, default `10`
  - `signInWindowMillis` / `auth.signInWindowMillis` / `AUTH_SIGN_IN_WINDOW_MILLIS`, default `60000`
- A `public fun signUpLimits(): AttemptLimits` and a `public fun signInLimits(): AttemptLimits`
  beside the existing `roomTimeouts()`, so callers take one value rather than two loose numbers.
- **The numbers are not re-derived here.** Sign-up's five per fifteen minutes comes from `ADR-0055`
  §1 and meters the requests that reach the hash; sign-in's ten per sixty seconds comes from
  `ADR-0074` §1 and meters the ones that fail. **They differ on purpose** — `ADR-0074` §1 gives the
  reason, and a reviewer who "harmonises" them has undone the decision.

## Out of scope

- Both call sites — sign-up's is `TASK-040521`, sign-in's is `TASK-040523`. This ticket adds four
  values and calls neither budget.

## Tests

`ServerConfigTest` — new methods only, in the shape the file already uses.

| Test | Proves |
| --- | --- |
| `theSignUpBudgetDefaults` | with an empty config and empty environment, `signUpMaxAttempts` is `5` and `signUpWindowMillis` is `900000` |
| `theSignUpBudgetComesFromTheEnvironment` | `AUTH_SIGN_UP_MAX_ATTEMPTS=9` and `AUTH_SIGN_UP_WINDOW_MILLIS=1000` are read — **both, with values that are neither the default nor each other's**, so neither field can be reading the other's source |
| `theSignUpBudgetComesFromTheConfigFile` | the two `auth.` keys are read when the environment is empty |
| `theSignInBudgetDefaults` | with an empty config and empty environment, `signInMaxAttempts` is `10` and `signInWindowMillis` is `60000` |
| `theSignInBudgetComesFromTheEnvironment` | `AUTH_SIGN_IN_MAX_ATTEMPTS=7` and `AUTH_SIGN_IN_WINDOW_MILLIS=1234` are read — four values now in play, none equal to another, so no field can be reading a neighbour's source |
| `theSignInBudgetComesFromTheConfigFile` | the two `auth.signIn*` keys are read when the environment is empty |
| `theTwoBudgetsAreSeparateValues` | setting **only** `AUTH_SIGN_UP_MAX_ATTEMPTS` leaves `signInMaxAttempts` at `10`, and setting only `AUTH_SIGN_IN_WINDOW_MILLIS` leaves `signUpWindowMillis` at `900000` — the pairs cannot be one pair read twice |
| `signUpLimitsCarriesBothNumbers` | `signUpLimits()` equals `AttemptLimits(signUpMaxAttempts, signUpWindowMillis)` for a non-default pair |
| `aNonNumericBudgetIsRefused` | `AUTH_SIGN_UP_MAX_ATTEMPTS=many` throws, matching every other numeric key in this file |

## Acceptance criteria

- [ ] All five test methods above pass
- [ ] `grep -rn "signIn" poker-server/src/main/kotlin/duels/poker/server/config/ServerConfig.kt`
      finds nothing
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.
