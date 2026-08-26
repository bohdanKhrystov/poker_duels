---
schema: 2
id: TASK-041630
title: A decorator that detaches, over the same port
type: task
status: ready
parent: STORY-0416
module: poker-server
estimate: S
tier: sonnet
review: standard
files_touched: 2
labels: [server, mail, concurrency]
depends_on: [TASK-041626]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.mail.DetachedRecoveryMailerTest.sendVerificationReturnsBeforeItsDeliveryRuns'
  - ./gradlew :poker-server:test --tests 'duels.poker.server.mail.DetachedRecoveryMailerTest.sendPasswordResetDetachesTheSameWay'
  - ./gradlew :poker-server:test --tests 'duels.poker.server.mail.DetachedRecoveryMailerTest.theDeliveryCarriesTheArgumentsItWasGiven'
  - ./gradlew :poker-server:ktlintCheck
  - ./gradlew :poker-server:detekt
---

> **One `--tests` per command, deliberately.** A whole-class filter exits 0 whether or not a named
> method exists — the defect that let `TASK-041616` ship two criteria naming tests nobody had
> written. Putting several patterns in one invocation does not fix it: Gradle fails only when the
> *combined* filter matches nothing, so a real method and an imaginary one together still exit 0.

## Goal

`ADR-0031` §5's *"delivery runs on a detached coroutine"* is one class implementing `RecoveryMailer`,
so no route handler holds a `CoroutineScope`, a `launch` or a `Job` — and the wiring, not the
handler, decides whether to detach at all.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/mail/DetachedRecoveryMailer.kt` | create |
| `poker-server/src/test/kotlin/duels/poker/server/mail/DetachedRecoveryMailerTest.kt` | create |

Read, and do not edit:
`poker-server/src/main/kotlin/duels/poker/server/auth/RecoveryMailer.kt` — the two signatures;
`poker-server/src/main/kotlin/duels/poker/server/mail/NoRecoveryMailer.kt` — the sibling this file
sits beside, and the one-public-type-per-file rule it follows;
`poker-server/src/main/kotlin/duels/poker/server/Application.kt`, specifically `scheduleSweeps` —
`ADR-0025`'s structured-concurrency idiom this reuses;
`docs/adr/ADR-0077-no-sender-is-an-implementation-and-detachment-is-a-decorator.md` §2 and §7.

## Scope

- `public class DetachedRecoveryMailer(delegate: RecoveryMailer, scope: CoroutineScope, log:
  Logger) : RecoveryMailer` in `duels.poker.server.mail`, per `ADR-0077` §2's signature **verbatim**
  — the parameter order and names are the ADR's, not the implementer's.
- Each of the two members `launch`es the delegate's corresponding call into `scope` and **returns
  immediately**. The caller does not join, does not await and learns nothing: §6.2 fixes both
  returns as `Unit` and `TASK-041606` ships a test that fails the build if that changes.
- One shared private helper carrying the member's name, so the two bodies are one line each and the
  `try`/`catch` exists once.
- The `catch` shape is `sweepPass`'s, already in `Application.kt`: rethrow `CancellationException`,
  catch every other `Throwable`, and log once per `ADR-0077` §4 — the member name and
  `failure::class.simpleName`, and nothing else. **`TASK-041631` owns that line's content** and
  lists this file as `modify`; write it as the ADR states it and leave proving it to that ticket.

## Out of scope

- **Building the scope.** `Application.duelServer` owns that, and what it is a child of is
  `ADR-0077` §3 — `TASK-041634`. This class takes a scope and never makes one; **gated below**, as
  a refusal with no assertion of its own.
- **Retry, back-off, a queue, or deleting the token row on failure.** `ADR-0077` §5 forecloses all
  four above the port, on the ground that *the send failed* is not reliably knowable and a
  compensating delete would destroy links that arrived. A transport may retry **below** the port,
  and that stays `EPIC-07`'s. **Gated below.**
- The failure log's content and the scope surviving a failure — `TASK-041631`, a second file over
  this class. Separated because a refusal deserves its own file and its own argument, which is
  `TASK-041602`'s reason unchanged, and because the two together do not fit in `S`.
- Any HTTP test. `ADR-0077` §7: no test asserts about a mail through `duelServer`, and route tests
  bind an **undecorated** recording double, so this class is exercised on its own with no server.
- Draining in-flight sends at shutdown. `ADR-0077` §3 accepts that the server may exit with mail
  pending, and defers a drain window until a transport's latency can be measured.

## Tests

`DetachedRecoveryMailerTest`, `internal`. `runBlocking { }` is the house idiom for a suspending test
here — see `RoomRematchTest` — because **`kotlinx-coroutines-test` is not on this module's
classpath**; do not add it, and do not write `runTest`. Each test builds its own scope as a
supervisor child of `runBlocking`'s job, so `job.children.forEach { it.join() }` after the call is
race-free; `ADR-0077` §7 measured that against this repository rather than assuming it.

**Every test is written as an ordering assertion, never as a wait.** Both members and the delegate
append a marker to one shared `MutableList<String>`; detachment is then the *order* of two markers.
This is deliberate: the obvious design — block the delegate on a `CompletableDeferred` the test
completes later — turns "the decorator forgot to detach" into a **deadlock** rather than a failure,
and a hung `runBlocking` with no `kotlinx-coroutines-test` timeout hangs the CI job instead of
reddening. No test sleeps, no test has a timeout, and no test needs one.

| Test | Proves |
| --- | --- |
| `sendVerificationReturnsBeforeItsDeliveryRuns` | The delegate `yield()`s and then appends `"delivered"`; the caller appends `"returned"` immediately after `sendVerification` comes back; the scope's children are then joined. The list is exactly `["returned", "delivered"]`. A member that forwarded directly produces `["delivered", "returned"]` |
| `sendPasswordResetDetachesTheSameWay` | The identical ordering for the second member, `handle` included. Two members, two tests: a body that forgot to `launch` in one of them is invisible to a test exercising the other |
| `theDeliveryCarriesTheArgumentsItWasGiven` | After joining, the delegate recorded exactly one call whose address, token, **handle** and member name are the ones passed in — four values, three of them distinct strings. The positive control: the ordering tests above are satisfied by a `launch` that delivers nothing at all |

## Acceptance criteria

- [ ] All three `DetachedRecoveryMailerTest` tests pass
- [ ] Each test method returns `Unit` — a `fun x() = runBlocking { … }` whose block ends in a
      value-returning expression is **silently never run** by JUnit; give each an explicit `: Unit`
      or end the block with an assertion
- [ ] `sendVerificationReturnsBeforeItsDeliveryRuns` asserts the **whole list in order**, not that
      it contains two elements
- [ ] `sendPasswordResetDetachesTheSameWay` exists and exercises the second member, `handle`
      included
- [ ] `theDeliveryCarriesTheArgumentsItWasGiven` uses an address, a token **and a handle** that are
      **three distinct strings**, so any argument swap is detectable. `ADR-0082` gave
      `sendPasswordReset`'s third parameter its first real source, and `token` and `handle` are
      both `String` on `RecoveryMailer` — which `ADR-0082` §5 keeps byte-unchanged — so the
      compiler cannot tell a swap of those two from the correct call. Only three distinct values
      can
- [ ] `DetachedRecoveryMailer.kt` contains no `CoroutineScope(`, no `GlobalScope`, no `runBlocking`
      and no `Job(` — it receives a scope and constructs none
- [ ] `DetachedRecoveryMailer.kt` contains no `delay`, no retry loop, no counter and no `DELETE`
- [ ] `CancellationException` is rethrown before the general `Throwable` catch
- [ ] No test in the file calls `delay`, `Thread.sleep`, `withTimeout` or `CompletableDeferred`
- [ ] Every command in `verify:` exits 0

## Proof

1. Delete the `launch` and call the delegate directly, so the member simply forwards.
   **`sendVerificationReturnsBeforeItsDeliveryRuns` and `sendPasswordResetDetachesTheSameWay` both
   redden**, *expected [returned, delivered], got [delivered, returned]*.
   `theDeliveryCarriesTheArgumentsItWasGiven` **stays green** — the mail is still delivered, just
   not detached — which is exactly why the ordering tests and the argument test are separate.
   Revert.
2. `launch` and then `join()` inside the member.
   **The same two tests redden, identically.** Run it and confirm the message is an order and not a
   timeout: if either test hangs instead, the fixture has reintroduced a wait and must be rewritten
   before this merges. That is the failure mode this test design exists to avoid.
3. `launch` in `sendVerification` only, and forward directly in `sendPasswordReset`.
   **`sendPasswordResetDetachesTheSameWay` alone reddens**, and the other two stay green. This is
   why there are two ordering tests and not one.
4. `launch { }` with an **empty** body, delivering nothing.
   **`theDeliveryCarriesTheArgumentsItWasGiven` reddens alone**, *expected 1 call, got 0*, while
   both ordering assertions **pass** — the markers the caller writes are unaffected. Run this one;
   it is the mutation that proves the ordering tests alone gate nothing about delivery.
5. Swap the two catch clauses so `Throwable` is caught before `CancellationException`.
   **Nothing reddens in this file** — no test here cancels a scope. Record it: the rethrow is held
   by `TASK-041631`, which does cancel, and by review here. Do not add a cancellation test to this
   file to close it; it is the next ticket's subject and does not fit in this one's budget.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.
