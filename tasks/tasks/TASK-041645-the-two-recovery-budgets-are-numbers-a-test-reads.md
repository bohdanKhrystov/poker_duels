---
schema: 2
id: TASK-041645
title: The two recovery budgets are numbers a test reads, not numbers a reviewer swaps unnoticed
type: task
status: done
parent: STORY-0416
module: poker-server
estimate: S
tier: haiku
review: standard
files_touched: 1
labels: [server, config, auth, security, test]
depends_on: [TASK-041644]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.config.ServerConfigTest.forgotPasswordIsTenAMinuteWithNothingConfigured'
  - ./gradlew :poker-server:test --tests 'duels.poker.server.config.ServerConfigTest.recoveryEmailIsFiveAMinuteWithNothingConfigured'
  - ./gradlew :poker-server:test --tests 'duels.poker.server.config.ServerConfigTest.eachRecoveryBudgetReadsItsOwnKeys'
  - ./gradlew :poker-server:test --tests 'duels.poker.server.config.ServerConfigTest.theEnvironmentOverridesBothRecoveryBudgets'
  - ./gradlew :poker-server:test --tests 'duels.poker.server.config.ServerConfigTest'
  - ./gradlew :poker-server:ktlintCheck
  - ./gradlew :poker-server:detekt
---

## Goal

Swapping `ADR-0079`'s two budget numbers fails the build, which it does not do today.

## Why this exists

`TASK-041628` shipped five-to-attach and ten-to-forget and **no test asserts either number**. Its
reviewer proved it rather than suspecting it: the two defaults were swapped in `ServerConfig.kt` and
both `verify:` classes were run — **build succeeded, zero failures.** Every test in
`RecoveryBudgetsTest` builds its own explicit low `AttemptLimits`, because that ticket's *Tests*
intro mandates *limits set low*. That is right for exercising the mechanism and blind to the policy.
So *that a budget applies* is gated; *which budget applies where* is not.

That ticket carries the criterion *"`ServerConfigTest` covers both new pairs' env-then-file-then-
default precedence"* while its *Files* table excludes `ServerConfigTest.kt`, with none of the
explicit carve-outs it wrote for `ServerComponents.kt` and `Application.kt`. The coder refused to
widen and was right to — nothing merged was failing, and `ADR-0070` §4's propagation exception
excludes *adds a test*. It is merged, so the fourth *Files* row it needed can no longer be added to
it. This is that row, as its own ticket.

`ADR-0079` fixes the four numbers and the reason each is what it is: `forgot-password` is generous at
**10 / 60000** because `ADR-0031` §5's fifteen-minute per-account rule already caps bombing one
victim; `recovery-email` is **5 / 60000** because it is the only cap on mail to a caller-chosen
recipient *and* a second door to the current-password guess `ADR-0074` priced at ten. Those are two
different arguments landing on two different numbers, which is exactly what a swap destroys silently.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/test/kotlin/duels/poker/server/config/ServerConfigTest.kt` | modify |

Read, and do not edit:
`poker-server/src/main/kotlin/duels/poker/server/config/ServerConfig.kt` — the four `DEFAULT_*`
constants at lines 145–159, the four `*_KEY` names, the four env names, and `from`'s `resolve(…)`
calls that read them;
`docs/adr/ADR-0079-five-to-attach-ten-to-forget-and-the-attach-budget-is-the-only-mail-cap.md`.

## Scope

- **Four test methods added** to the existing `ServerConfigTest`. No existing test changes, no
  assertion moves and none is weakened — the class already covers the port, the database, the room
  timeouts, the base URL and the sign-up and sign-in budgets, and none of those reads a recovery key.
- **Every fixture goes through `ServerConfig.from(config) { … }`**, in the shape
  `fallsBackToTheDefaultBaseUrl` and `signUpLimitsCarriesBothNumbers` already use. **Not the
  constructor.** `TASK-041628`'s `## Notes` records the closing fixture as
  `ServerConfig().forgotPasswordLimits()` and **that does not compile**: `ServerConfig` has nine
  parameters with no default — `port`, `maxFrameLength`, `maxFrameNestingDepth`, the four database
  fields and the two room timeouts — so there is no zero-argument construction. Correcting it also
  makes the test stronger, because the numbers exist in **two** places and only one of them ships:
  `from` resolves through `DEFAULT_FORGOT_PASSWORD_MAX_ATTEMPTS` and its three siblings, and
  `Application` builds every real `ServerConfig` that way. The data class's own parameter defaults
  exist so that test construction sites compile, and its own comment says so.
- **The expected numbers are written as literals** — `10`, `5`, `60_000L` — never as
  `ServerConfig.DEFAULT_FORGOT_PASSWORD_MAX_ATTEMPTS`. A test that reads the same constant it is
  checking passes under every mutation of that constant, which is the whole failure being closed
  here.
- **No fixture value in the two configured tests may equal a default.** `10`, `5` and `60000` are
  forbidden as configured inputs: a value the mutation leaves unchanged cannot detect it, and a test
  configuring `10` for forgot-password cannot tell reading the key from falling back.
- Each new test carries a one-line comment naming `ADR-0079` and which of its two arguments the
  number comes from, so the next person to change a number reads the reason beside the assertion.

## Out of scope

- **Changing any number.** `ADR-0079` fixed all four; this ticket writes down what is already
  shipped. If a test reddens on first run, **stop and report it** — that means the merged config and
  the merged ADR disagree, which is a finding and not a fix.
- **Any change to `ServerConfig.kt`, `RecoveryRoutes.kt` or `RecoveryBudgetsTest.kt`.** The *Files*
  table has one row. `RecoveryBudgetsTest` keeps building its own low limits: it tests the mechanism,
  this tests the policy, and merging the two would make each worse.
- **A test that the data class's parameter defaults agree with the `DEFAULT_*` constants.** They are
  written twice today and could drift, but the constructor defaults are reachable only from test
  construction sites, so a divergence cannot reach a running server. Recorded as a known,
  bounded duplication rather than gated — asserting it needs a nine-argument construction whose noise
  would outweigh what it buys. **A refusal, not an omission.**
- Sign-up's and sign-in's budgets. `signUpLimitsCarriesBothNumbers` covers one pair's assembly
  already, and neither number moves here.

## Tests

`ServerConfigTest` — four methods added to the merged class, beside `signUpLimitsCarriesBothNumbers`.

| Test | Proves |
| --- | --- |
| `forgotPasswordIsTenAMinuteWithNothingConfigured` | `ServerConfig.from(MapApplicationConfig()) { null }.forgotPasswordLimits()` equals `AttemptLimits(10, 60_000L)`, asserted against those literals. `ADR-0079`'s generous half |
| `recoveryEmailIsFiveAMinuteWithNothingConfigured` | The same call's `recoveryEmailLimits()` equals `AttemptLimits(5, 60_000L)`. **A second expected value that differs from the first**, which is what makes the pair able to catch a swap: with the two defaults exchanged, both tests redden, and either alone would leave the other's number free |
| `eachRecoveryBudgetReadsItsOwnKeys` | One `MapApplicationConfig` carrying all **four** keys with four **distinct** non-default values — `3` / `111000` and `7` / `222000` — and both limits asserted. A `from` that wired forgot-password's key into recovery-email's field, or read one key twice, reddens here and passes both tests above |
| `theEnvironmentOverridesBothRecoveryBudgets` | The same four keys in the file **and** four different values in the env lookup: both limits answer the env values. Closes `TASK-041628`'s env-then-file half for both pairs in one test, and a `resolve` call given the wrong env name reddens |

## Acceptance criteria

- [ ] `ServerConfigTest.forgotPasswordIsTenAMinuteWithNothingConfigured` passes
- [ ] `ServerConfigTest.recoveryEmailIsFiveAMinuteWithNothingConfigured` passes
- [ ] `ServerConfigTest.eachRecoveryBudgetReadsItsOwnKeys` passes, asserting **both** limits from one
      config carrying **four distinct** values
- [ ] `ServerConfigTest.theEnvironmentOverridesBothRecoveryBudgets` passes, asserting **both** limits
- [ ] The whole `ServerConfigTest` class passes
- [ ] The two default tests assert the **literals** `10`, `5` and `60_000L`; neither names
      `DEFAULT_FORGOT_PASSWORD_MAX_ATTEMPTS`, `DEFAULT_FORGOT_PASSWORD_WINDOW_MILLIS`,
      `DEFAULT_RECOVERY_EMAIL_MAX_ATTEMPTS` or `DEFAULT_RECOVERY_EMAIL_WINDOW_MILLIS`
- [ ] No configured value in the two configured tests is `10`, `5` or `60000`, and no two of the
      four values in `eachRecoveryBudgetReadsItsOwnKeys` are equal
- [ ] Every fixture builds through `ServerConfig.from(…)`; the file gains no `ServerConfig(` call
- [ ] The merged tests in the file are byte-unchanged; `git diff` shows additions only
- [ ] Every command in `verify:` exits 0

## Proof

1. **Swap the two `DEFAULT_*` attempt constants in `ServerConfig.kt`** — forgot-password to `5`,
   recovery-email to `10`. This is the reviewer's mutation, run again.
   **Before this ticket: nothing reddens**, across `RecoveryBudgetsTest` and `ServerConfigTest` both.
   **After it: `forgotPasswordIsTenAMinuteWithNothingConfigured` and
   `recoveryEmailIsFiveAMinuteWithNothingConfigured` both redden**, each on its own number. The other
   two new tests configure their own values and stay green — which is correct, and is why the two
   default tests are not folded into them. Record both runs in the PR. Revert.
2. Swap **one** of the two — forgot-password to `5`, recovery-email left at `5`.
   **`forgotPasswordIsTenAMinuteWithNothingConfigured` reddens alone.** Run it: it is the step that
   shows the two expected values being *different numbers* is what does the work. Had `ADR-0079`
   landed on ten and ten, this pair would gate nothing and the ticket would need a third fixture.
   Revert.
3. In `from`, read `RECOVERY_EMAIL_MAX_ATTEMPTS_KEY` for both max-attempt values.
   **`eachRecoveryBudgetReadsItsOwnKeys` reddens alone**, on the forgot-password limit — the two
   default tests pass, because with nothing configured both keys are absent and both fall back
   correctly. This is the mutation the defaults cannot see and the reason that test carries four
   distinct values rather than two. Revert.
4. In `from`, pass `AUTH_FORGOT_PASSWORD_MAX_ATTEMPTS` where
   `AUTH_RECOVERY_EMAIL_MAX_ATTEMPTS` belongs.
   **`theEnvironmentOverridesBothRecoveryBudgets` reddens alone.** `eachRecoveryBudgetReadsItsOwnKeys`
   passes: it sets no env value, so the wrong name resolves to `null` and the file value is read
   either way. Revert.
5. Rewrite `forgotPasswordIsTenAMinuteWithNothingConfigured` to assert
   `AttemptLimits(ServerConfig.DEFAULT_FORGOT_PASSWORD_MAX_ATTEMPTS, ServerConfig.DEFAULT_FORGOT_PASSWORD_WINDOW_MILLIS)`,
   then apply step 1 again.
   **Nothing reddens.** Written down because it is the shape a reviewer will ask for — *"use the
   named constant, not a magic number"* — and on a golden value it inverts: the constant is the thing
   under test, so a test that reads it asserts that a number equals itself. Restore the literals.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.

## Notes

**This closes a gap that was demonstrated, not suspected — and the demonstration is repeatable.**
`TASK-041628` shipped budgets of ten a minute to forget and five to attach, and no test asserted
either number: every test there builds its own low `AttemptLimits`, because that ticket's Tests intro
mandates *limits set low*. Right for the mechanism, blind to the policy. A reviewer swapped the two
defaults and **every gate stayed green**. Swapping them now reddens both default tests, confirmed
independently at review.

**The fixture recorded in `TASK-041628`'s notes did not compile, and a planner caught it before a
coder met it.** `ServerConfig` has **nine parameters with no defaults**, so `ServerConfig()` does not
exist. The tests go through `ServerConfig.from(MapApplicationConfig()) { … }` — which is also the
**stronger** path, because the numbers live in two places and only the `DEFAULT_*` constants that
`from` resolves are the ones `Application` ships. Asserting against a directly-constructed object
would have gated a value nothing uses.

**The defaults are asserted as literals, not against the constants.** `AttemptLimits(10, 60_000L)` and
`AttemptLimits(5, 60_000L)` are written out; referencing `DEFAULT_*` would make the assertion a
tautology that passes whatever the constant holds — leaving the swap invisible and closing nothing.
That distinction has now decided four tests this run, and it is the check most likely to be wrong
while looking right.

**One test carries the field-assignment property alone, and the others structurally cannot.**
`eachRecoveryBudgetReadsItsOwnKeys` configures all four keys with distinct non-default values (3,
111000, 7, 222000), so a resolver reading the right key and assigning to the wrong field reddens it.
The two default tests configure nothing, so both fields receive defaults regardless of assignment
order — they cannot see it, and that is not a deficiency in them.

**No global, no class-load ordering.** `ServerConfig.from` takes its environment reader as a
**parameter**, which is what made a class-load caching concern **foreclosed** rather than ungated when
it was raised on `TASK-041632`.
