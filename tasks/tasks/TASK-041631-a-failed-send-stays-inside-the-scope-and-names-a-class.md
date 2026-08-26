---
schema: 2
id: TASK-041631
title: A failed send stays inside the scope, and its log line names a class
type: task
status: ready
parent: STORY-0416
module: poker-server
estimate: S
tier: sonnet
review: deep
files_touched: 2
labels: [server, mail, concurrency, security, privacy]
depends_on: [TASK-041630]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.mail.DetachedRecoveryMailerFailureTest'
  - ./gradlew :poker-server:ktlintCheck
  - ./gradlew :poker-server:detekt
---

## Goal

A send that throws costs one log line naming a member and an exception class, and costs nothing
else: no sibling send, no future send, no application job, and above all no address in a log.

## Why this exists

Both halves are **refusals**, and a refusal produces no assertion by itself.

`ADR-0077` §4 fixes the line at the member name and `failure::class.simpleName` and **nothing else**
— no address (`ADR-0031` §6.3), no exception message and no stack trace, on the express ground that
*"a transport's message is the likeliest place a recipient address will ever appear in this system's
logs"*, no `player_id`, and no success line, because *"a line per delivered mail is a delivery log,
and the thing §6.4 is warning about is a delivery log."* `log.error("send failed", failure)` is one
character of habit away and satisfies every other test in this story.

§3 makes the scope a **supervisor** so one failed send cancels no sibling. Written as an ordinary
`Job`, the first failure kills the scope and every later send is silently dropped — a defect no
status code, no route test and no schema test can observe.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/mail/DetachedRecoveryMailer.kt` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/mail/DetachedRecoveryMailerFailureTest.kt` | create |

`DetachedRecoveryMailer.kt` is listed as `modify` because **this ticket owns that log line's
content**. `TASK-041630` writes the `catch` and the `log.error` call as `ADR-0077` §4 states them;
if what it wrote carries a message, a throwable or an address, correcting it here is this ticket's
job and not a widening — gating a line means owning it. If nothing needs changing, the row stands
and the diff for that file is empty.

A second *test* file rather than more methods in `DetachedRecoveryMailerTest`: `TASK-041602`'s
reason, unchanged — *a refusal needs its own test and its own argument, not a spare assertion at the
bottom of a file about what the thing does.*

Read, and do not edit:
`poker-server/src/test/kotlin/duels/poker/server/mail/DetachedRecoveryMailerTest.kt` — the scope and
recording-delegate fixture this file mirrors;
`poker-server/src/main/kotlin/duels/poker/server/auth/EmailAddress.kt` — `toString()` is already a
fixed redaction (`TASK-041603`), which is why the assertion below is written against `.value` and
not against the rendered object;
`docs/adr/ADR-0077-no-sender-is-an-implementation-and-detachment-is-a-decorator.md` §3 and §4;
`docs/adr/ADR-0031-an-optional-verified-recovery-email.md` §6.3 and §6.4.

## Scope

- `DetachedRecoveryMailerFailureTest`, `internal`, on the same supervisor-child scope and
  recording-delegate fixture as `DetachedRecoveryMailerTest`, plus a recording `org.slf4j.Logger`.
  `runBlocking { }`, as that file uses and for the same reason: **`kotlinx-coroutines-test` is not
  on this module's classpath**, so there is no `runTest` and no test dispatcher. Each test method
  must return `Unit` — a `= runBlocking { … }` ending in a value-returning expression is silently
  never run by JUnit.
- **The recording logger records the formatted message and the throwable argument separately**, so
  a test can assert the throwable is `null` — `log.error(msg, failure)` is the mutation that leaks a
  stack trace and it is invisible to a test that only inspects the rendered string.
  `org.slf4j.helpers.LegacyAbstractLogger` gives this in one small private class:
  `handleNormalizedLoggingCall` receives the pattern, the arguments and the throwable, and the five
  `isXEnabled` members are one line each. `logback-classic` is `runtimeOnly` here and its types are
  **not on the test compile classpath**, so `ListAppender` is not available — do not reach for it.
- The address fixture's `.value` is a distinctive string that appears nowhere else in the file, so
  a substring assertion means what it says.
- Cancellation is asserted by cancelling the scope while a delegate is suspended and observing that
  the delegate's own `CancellationException` is **not** logged — `ADR-0077` §4's rethrow, which
  `TASK-041630`'s Proof step 4 recorded as ungated there.

## Out of scope

- **Retry, back-off and any compensating delete.** `ADR-0077` §5 forecloses them above the port.
  **Gated below** by a send count, because "nothing was retried" is a refusal.
- A **success** log line. §4 forbids one and `TASK-041630` writes none; **gated below** by asserting
  the recorded lines are empty on the happy path, because an added success line breaks no other test
  in this story.
- A `player_id` in the line. §6.4 permits one and §4 declines it; the port does not carry one, so
  this is structural and needs no test.
- Whatever a **transport** logs at its own boundary. §4 leaves that open and it is `EPIC-07`'s.
- The scope's parentage. That it is a supervisor *child of the application's job* is `TASK-041634`'s;
  this file builds its own scope and asserts only supervisor behaviour.

## Tests

`DetachedRecoveryMailerFailureTest`

| Test | Proves |
| --- | --- |
| `aFailureLeavesTheScopeAliveAndASecondSendStillArrives` | A delegate that throws: `sendVerification` returns normally; after joining, the scope's job is neither cancelled nor completed; and a second send on the **same** decorator and the **same** scope then reaches the delegate. **Both halves in one test** — the liveness assertion alone is satisfied by a scope nothing ever uses again, and the second send is what proves it is still usable |
| `theFailureIsLoggedOnceAndTheLineNamesTheMemberAndTheExceptionClass` | Exactly **one** line was recorded; it contains `sendVerification` and the thrown exception's `simpleName` |
| `theFailureLineCarriesNoAddressNoTokenNoMessageAndNoThrowable` | That same line contains neither the address's `.value` nor the token; the exception's **message text** does not appear in it; and the throwable argument passed to the logger is `null`. Four refusals, asserted separately so the failure message says which one broke |
| `nothingIsLoggedWhenASendSucceeds` | A successful send records **zero** lines. §4's no-delivery-log clause, and the control that stops the four assertions above passing against a logger nothing ever calls |
| `aCancelledSendIsNotLoggedAsAFailure` | The scope is cancelled while a delegate is suspended; after the scope's job completes, **zero** lines were recorded. `CancellationException` is rethrown, not caught and reported as a failed send |
| `aFailedSendIsNotRetried` | Across one throwing send, the delegate is called exactly **once**. §5's no-retry, which nothing else in this story can see |

## Acceptance criteria

- [ ] All six `DetachedRecoveryMailerFailureTest` tests pass
- [ ] `theFailureLineCarriesNoAddressNoTokenNoMessageAndNoThrowable` asserts the throwable argument
      is `null`, **separately** from any assertion about the rendered string
- [ ] The thrown exception carries a message that is a distinctive literal, and that literal is
      asserted absent from the recorded line
- [ ] The address fixture's `.value` appears in this file only as the fixture and the assertion —
      it is not a substring of any other literal in the file
- [ ] `nothingIsLoggedWhenASendSucceeds` asserts a count of `0`, and runs against the same recording
      logger the failure tests use
- [ ] `aFailureLeavesTheScopeAliveAndASecondSendStillArrives` reuses the **same** scope and
      decorator instance for the second send as for the failure that preceded it
- [ ] Every test method returns `Unit`
- [ ] `aCancelledSendIsNotLoggedAsAFailure` ends the suspended delivery with `cancelAndJoin()`, so
      the test terminates whether or not the rethrow is intact — a bare `join()` on a coroutine
      suspended forever hangs the CI job instead of reddening
- [ ] No test in the file calls `delay`, `Thread.sleep` or `withTimeout`
- [ ] The file imports nothing from `ch.qos.logback` and nothing from `kotlinx.coroutines.test`
- [ ] Every command in `verify:` exits 0

## Proof

1. Change the log call to `log.error("{} failed", member, failure)` — the SLF4J idiom that attaches
   the throwable and prints a stack trace.
   **`theFailureLineCarriesNoAddressNoTokenNoMessageAndNoThrowable` reddens on the throwable
   assertion alone**, while `theFailureIsLoggedOnceAndTheLineNamesTheMemberAndTheExceptionClass`
   **stays green**, because the rendered message still names the member. Run this: it is the exact
   mutation §4 exists to forbid, it is what a reasonable engineer writes for debuggability, and it
   is invisible to every assertion about the rendered string. Revert.
2. Change the log call to include the address: `log.error("{} failed for {}: {}", member,
   address.value, failure::class.simpleName)`.
   **`theFailureLineCarriesNoAddressNoTokenNoMessageAndNoThrowable` reddens** on the address
   assertion. Note in the PR that `EmailAddress.toString()` is *already* redacted, so the same edit
   written as `"$address"` leaks nothing — which is why this mutation uses `.value` and why the
   assertion does too. Revert.
3. Build the test's scope with `Job(...)` instead of `SupervisorJob(...)`.
   **`aFailureLeavesTheScopeAliveAndASecondSendStillArrives` reddens**, and it should redden on the
   **liveness** assertion first — the job is cancelled — rather than on the second send. If it
   reddens only on the second send, the liveness half is not actually inspecting the job's state
   and must be fixed. This is the defect that silently stops all future mail after one failure, and
   no status code, route test or schema test in this repository can observe it. Revert.
4. Catch `CancellationException` in the general `catch` instead of rethrowing it.
   **`aCancelledSendIsNotLoggedAsAFailure` reddens**, *expected 0 lines, got 1*. This closes
   `TASK-041630`'s Proof step 5, which recorded the rethrow as ungated in that file. Revert.
5. Add a success line: `log.info("{} delivered", member)`.
   **`nothingIsLoggedWhenASendSucceeds` reddens alone**, *expected 0, got 1*. The delivery log
   §6.4 calls *"a mailing list with a different file extension"*, and nothing else in this story
   notices it. Revert.
6. Wrap the delegate call in `repeat(3) { runCatching { send() } }`.
   **`aFailedSendIsNotRetried` reddens alone**, *expected 1 call, got 3*, and
   `theFailureIsLoggedOnceAndTheLineNamesTheMemberAndTheExceptionClass` reddens too if each attempt
   logs. Predict both, run it, and record which appeared. Revert.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.
