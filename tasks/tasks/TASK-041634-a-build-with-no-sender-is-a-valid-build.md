---
schema: 2
id: TASK-041634
title: A build with no sender is a valid build
type: task
status: done
parent: STORY-0416
module: poker-server
estimate: S
tier: sonnet
review: standard
files_touched: 3
labels: [server, mail, wiring, concurrency]
depends_on: [TASK-041633]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.mail.NoSenderConfiguredTest' -PrequireDocker=true
  - ./gradlew :poker-server:test --tests 'duels.poker.server.DuelServerRoutesTest' -PrequireDocker=true
  - ./gradlew :poker-server:ktlintCheck
  - ./gradlew :poker-server:detekt
---

## Goal

The shipped server boots and answers all five recovery endpoints with **no mail sender configured**
— which is every developer machine and every CI run — and the detached delivery it composes is a
child of the application's job.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/ServerComponents.kt` | modify |
| `poker-server/src/main/kotlin/duels/poker/server/Application.kt` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/mail/NoSenderConfiguredTest.kt` | create |

`ServerComponents` was probed by the ticket this one was re-cut from: it takes a new required field
with **zero propagation**, because every construction site in the repository goes through the
`serverComponents(…)` factory. Give the field a default anyway if that probe no longer holds.

Read, and do not edit:
`poker-server/src/main/kotlin/duels/poker/server/mail/NoRecoveryMailer.kt`;
`poker-server/src/main/kotlin/duels/poker/server/mail/DetachedRecoveryMailer.kt`;
`poker-server/src/test/kotlin/duels/poker/server/DuelServerRoutesTest.kt` — the boot-the-real-server
fixture this file copies, and the every-registered-route-answers assertion this install must not
break;
`docs/adr/ADR-0077-no-sender-is-an-implementation-and-detachment-is-a-decorator.md` §3, §7 and §8;
`docs/adr/ADR-0025-one-ticker-coroutine-drives-both-sweeps.md`.

## Scope

- `ServerComponents` gains one field holding the **undecorated** mailer, bound to
  `NoRecoveryMailer`, and `serverComponents(…)` sets it. Undecorated because the scope the decorator
  needs does not exist until `Application` runs, and because `ADR-0077` §8 says so.
- `Application.duelServer` builds the delivery scope beside its existing `scheduleSweeps` call, per
  `ADR-0077` §3 **verbatim**:

  ```kotlin
  val delivery = CoroutineScope(
      coroutineContext + SupervisorJob(coroutineContext.job) + CoroutineName("recovery-mail"),
  )
  ```

  then wraps `components`' mailer in `DetachedRecoveryMailer(…, delivery, log)` and passes the
  result to `recoveryRoutes`. A **child of the application's job**, so shutdown cancels every
  in-flight send; a **supervisor**, so one failed send reaches no sibling and never the application;
  and **its own scope**, because the application's job also carries the sweep ticker, which never
  completes.
- `recoveryRoutes` gains a `mailer` parameter at its call site here. If `TASK-041625` or
  `TASK-041626` already added it, this ticket only passes the decorated value.
- `NoSenderConfiguredTest` boots `duelServer` the way `DuelServerRoutesTest` does, with no mail
  configuration present.

## Out of scope

- **Any assertion about a mail.** `ADR-0077` §7 is categorical: no test asserts about a send through
  `duelServer`, because `duelServer` composes the decorator and a test booted that way would have to
  join a scope it does not hold. Presence and absence of mail are `TASK-041625`, `TASK-041626` and
  `TASK-041631`, all against undecorated doubles. This file asserts that the server **runs**.
- **`ADR-0031` §5's `202`-before-the-send ordering.** `ADR-0077` §7 records that it is not gated and
  cannot be under the test engine, and `TASK-041626`'s Proof step 3 already predicted the null
  result. It stays a review criterion; **the PR must say the reviewer read the handler**, and no
  latency assertion may be invented to close it.
- **Draining pending mail at shutdown.** `ADR-0077` §3 accepts that the server may exit with mail
  cancelled rather than drained, and defers a drain window until a transport's latency exists to
  measure. **Gated below** — a `withTimeoutOrNull(join)` added in a shutdown hook would otherwise
  pass every test here.
- A startup log line or health check announcing a configured sender — `ADR-0031`'s Consequences,
  `EPIC-07`.
- Any transport, SDK, dependency or credential — `EPIC-07`, and the human's.
- Constructing `RecoveryLinks`. Nothing delivers a link until a transport exists (`TASK-041633`).

## Tests

`NoSenderConfiguredTest`

| Test | Proves |
| --- | --- |
| `theServerStartsWithNoSenderConfigured` | `duelServer` boots with no mail configuration present and `GET /health` answers. The plainest statement of `ADR-0031` §7, and the one this whole seam exists to keep true |
| `everyRecoveryEndpointAnswers` | Each of the **five** recovery endpoints, called on the booted server, returns the status its own ticket specifies — not a `500` and not a `404`. Enumerate all five by path and method; a list that quietly lost one would otherwise pass having checked four |
| `noRouteFileBranchesOnWhetherASenderIsConfigured` | Sweeping `RecoveryRoutes.kt`: it contains no `NoRecoveryMailer`, no `null` check on a mailer and no `isConfigured`. `ADR-0077` §1's property is that the branch lives in the wiring, once, and a route cannot reintroduce it |
| `theDeliveryScopeIsASupervisorChildOfTheApplication` | Stopping the application completes the delivery scope's job, and a delivery that fails does **not** complete the application's. Asserted on the jobs, not on a mail |
| `nothingShutsTheDeliveryScopeDownExplicitly` | Sweeping `Application.kt`: no `withTimeoutOrNull`, no `joinAll`, no stored `Job` field and no `GlobalScope`. `ADR-0025`'s rule — structured concurrency *is* the lifecycle — restated where it is easiest to undo |

## Acceptance criteria

- [ ] All five `NoSenderConfiguredTest` tests pass
- [ ] `DuelServerRoutesTest` passes **unchanged** — this install adds routes and moves no existing
      assertion
- [ ] `everyRecoveryEndpointAnswers` names all **five** endpoints and asserts a count of five before
      calling any of them
- [ ] `theDeliveryScopeIsASupervisorChildOfTheApplication` asserts **both** directions: application
      down cancels delivery, delivery failure does not cancel the application
- [ ] `Application.kt` contains exactly one `CoroutineScope(` and it carries `SupervisorJob(` and
      `CoroutineName("recovery-mail")`
- [ ] `Application.kt` contains no `GlobalScope`
- [ ] No file under `poker-server/src/main` branches on whether a sender is configured
- [ ] No new dependency appears in `poker-server/build.gradle.kts`
- [ ] No test in this file asserts that a mail was or was not sent
- [ ] Every command in `verify:` exits 0

## Proof

1. Build the delivery scope with `Job(coroutineContext.job)` instead of `SupervisorJob(…)`.
   **`theDeliveryScopeIsASupervisorChildOfTheApplication` reddens** on the second direction: a
   failing delivery now cancels the application. `theServerStartsWithNoSenderConfigured` stays green,
   because nothing sends. Revert.
2. Build it with `CoroutineScope(Dispatchers.IO)` — parentless, and the shape that looks tidiest.
   **`theDeliveryScopeIsASupervisorChildOfTheApplication` reddens** on the first direction: stopping
   the application leaves the scope alive. Run this one; it is the `GlobalScope`-by-another-name that
   `ADR-0025` and `ADR-0077` §3 both refuse, and no other test in this repository can see it.
   Revert.
3. Bind the mailer to `null` in `ServerComponents` and have `recoveryRoutes` skip the send when it
   is `null`.
   **`noRouteFileBranchesOnWhetherASenderIsConfigured` reddens alone**, and every status assertion in
   this file still passes. This is `ADR-0077`'s rejected alternative, it is the *honest*-looking
   design, and the wire cannot distinguish it. Revert.
4. Add `withTimeoutOrNull(5_000) { delivery.coroutineContext.job.children.forEach { it.join() } }`
   to a shutdown path.
   **`nothingShutsTheDeliveryScopeDownExplicitly` reddens alone.** `ADR-0077` §3 leaves this
   available for `EPIC-07` and refuses it now for want of a latency to measure; without this test
   it lands as a tidy-up. Revert.
5. Remove one endpoint from `everyRecoveryEndpointAnswers`' list.
   **The count assertion reddens**, *expected 5, actual 4*, rather than the test quietly checking
   four. Run it: a name dropped from an enumeration is the way this test stops covering what its
   name promises.
6. Wrap the `DetachedRecoveryMailer` around the mailer **inside** `serverComponents(…)` instead of
   in `Application`, using a scope built there.
   **`theDeliveryScopeIsASupervisorChildOfTheApplication` reddens** on the first direction. Record
   it: this is the arrangement `ADR-0077` rejects under *"a delivery scope owned by
   `ServerComponents`"*, and it is attractive because it would leave `Application.kt` untouched.
   Revert.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.

## Notes

**This closes a gap four tickets deferred here, and the closure was measured rather than assumed.**
`TASK-041631` shipped six tests and its coder disclosed that **none** could catch an exception
escaping to cancel the **caller's** scope — its test scope was itself a `SupervisorJob`, so an escape
registered as *"into the scope I was given"* rather than reaching the parent. It named this ticket as
the owner; a reviewer read this ticket and agreed it should gate the property; and now the swap is
run: replacing `SupervisorJob(coroutineContext.job)` with `Job(…)` reddens
`theDeliveryScopeIsASupervisorChildOfTheApplication` on
`assertTrue(appJob.isActive, "a failed delivery cancelled the application's job")`.

**The distinction that makes it a real gate**: the test observes the application's job **surviving an
actual failed delivery**, not merely that the delivery scope is a supervisor by construction. A
structural assertion would pass even while a failure propagated. Confirmed independently at review.

**Proof step 3 was refused, and the refusal named its own cost.** Executing it needs an edit to
`RecoveryRoutes.kt`, outside the *Files* table, so it was not run even transiently. The coder then
disclosed the precision gap that leaves: the `forbiddenAnywhere` token sweep (`mailer == null`,
`mailer != null`, `null == mailer`, `null != mailer`, `mailer?.`) would not catch
`when (mailer) { null -> … }`. **That shape is unreachable** — `mailer` is typed `RecoveryMailer`,
not `RecoveryMailer?`, so Kotlin rejects the null branch at compile time. No ticket needed, and the
reasoning is recorded so the question is not reopened.

**A deviation from this ticket's literal text, and the text is what is wrong.** Applied whole-file,
the *"contains no `NoRecoveryMailer`"* check **fails on correct code**: `RecoveryRoutes.kt`
legitimately defaults `mailer: RecoveryMailer = NoRecoveryMailer`, which `ADR-0077` §1 describes as
*an object, never a null* and its own KDoc documents at length. The check is scoped to the handler
bodies after `routing {` — where a reintroduced branch would live — while the null-check tokens stay
whole-file. Review confirmed a branch reintroduced in a handler still reddens.

**Proof step 2 underclaims its own coverage.** It says of the `CoroutineScope(Dispatchers.IO)`
mutation that *"no other test in this repository can see it"*; `nothingShutsTheDeliveryScopeDown
Explicitly` reddens too, because the source no longer contains `SupervisorJob(`. Measured by coder and
reviewer both. Not a defect — a ticket claiming less than it delivers, recorded so the next reader
trusts the measurement over the prose.

**The build-wide gates were run although the block does not name them.** `:poker-server:check` passed
across 1719 tests including `verifyDuelScript` and `verifyProtocolTypes`. Two tickets in this story
failed CI on exactly such a gate, and this one touches `Application.kt`, where that bites hardest.
