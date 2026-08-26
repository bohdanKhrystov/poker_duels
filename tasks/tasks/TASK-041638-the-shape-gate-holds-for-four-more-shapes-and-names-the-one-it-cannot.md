---
schema: 2
id: TASK-041638
title: The shape gate holds for four more shapes, and names the one it cannot
type: task
status: ready
parent: STORY-0416
module: poker-server
estimate: S
tier: sonnet
review: standard
files_touched: 1
labels: [server, auth, mail, security, invariant]
depends_on: [TASK-041606]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.auth.RecoveryMailerShapeTest'
  - ./gradlew :poker-server:ktlintCheck
  - ./gradlew :poker-server:detekt
---

## Goal

`RecoveryMailerShapeTest` fails the build when a third way to send mail is added to `RecoveryMailer`
as a property, a member extension, a nested type, a companion member, or an inherited member — and
says in one place which surface it still cannot see.

## Why this exists

`TASK-041606` shipped the test `ADR-0031` §6.2 specifies, asserting over
`KClass.declaredMemberFunctions` exactly as its Scope named. Its landing Notes record that the
reviewer then probed four shapes and **all four passed with every test green**: a property
(`val sendMarketing: suspend (EmailAddress) -> Unit`), a nested interface or object, a companion
member, and the port **extending a second interface** that declares a third function — *declared*
excludes inherited members by definition.

§6.2 makes this test the mechanism carrying the vision's *"Never used for contact or marketing"*.
That promise has two halves: **in a diff a reviewer reads**, which holds for all four, and **a test
asserts it**, which does not. `TASK-041606` filed the follow-up rather than folding it in, because
widening the assertion is a design choice about what *"the public API"* means and that ticket had
already answered the question narrowly on the ADR's own wording. This is that follow-up, and it
answers the wider question: which surfaces are worth asserting, and which are not.

**A fifth shape, found by probing rather than by reading.** `declaredMemberFunctions` also excludes
**member extension functions**. Probed on this repository's toolchain: an interface declaring one
plain function and one `fun String.x()` reports `[plainMember]` from `declaredMemberFunctions` and
`[x]` from `declaredMemberExtensionFunctions`. So
`suspend fun EmailAddress.sendNewsletter(token: VerificationToken)` on `RecoveryMailer` is a third
declared member the shipped test cannot see, and it is not one of the reviewer's four.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/test/kotlin/duels/poker/server/auth/RecoveryMailerShapeTest.kt` | modify |

Four tests and one control are **added**, and the class KDoc's closing paragraph — the one stating
that *"a property-shaped addition to `RecoveryMailer` is outside what this specific assertion can
see"* — is **replaced**, because this ticket makes it false. No existing test method changes, no
assertion moves and none is weakened. `RecoveryMailer.kt` is **not** edited: its KDoc already says a
third member *of any kind* requires an ADR superseding `ADR-0031`, which this ticket makes more true
rather than less.

Read, and do not edit:
`poker-server/src/main/kotlin/duels/poker/server/auth/RecoveryMailer.kt` — the interface under test;
`docs/adr/ADR-0031-an-optional-verified-recovery-email.md` §6, all five mechanisms, so it is clear
which of them this test is and which it is not.

## Scope

Four reflective surfaces are asserted, each through a **named private helper that the control test
also calls** — a helper used by only one of the two is not a control.

1. `declaredMemberProperties` is empty. `val sendMarketing: suspend (EmailAddress) -> Unit` reads at
   a call site exactly like a third function, and an implementer supplies the lambda.
2. `declaredMemberExtensionFunctions` is empty — the surface `declaredMemberFunctions` excludes by
   definition, and the fifth shape above.
3. `nestedClasses` is empty. **One read covers both a nested type and a companion**: probed,
   `nestedClasses` on an interface holding `interface Nested` and a `companion object` reports
   `[Nested, Companion]`. A companion member is callable as `RecoveryMailer.sendNewsletter(...)`,
   which at a call site reads as the port itself sending a third mail. No separate
   `companionObject` assertion is needed and none should be added.
4. `supertypes.map { it.classifier }` equals `listOf(Any::class)` — the largest hole, since
   `RecoveryMailer : Newsletters` makes `sendNewsletter` callable on the port and indistinguishable
   at every call site from a real member. **The assertion is on the whole list, in order.** Probed:
   an interface extending nothing reports `[kotlin.Any]`, and one extending `ProbeBase` reports
   `[ProbeBase, kotlin.Any]` — `Any` is present in **both**, so an assertion phrased as *`Any` is in
   `supertypes`* passes under the mutation and gates nothing.

- **One control type carrying all four forbidden shapes at once**, private to the file, named so it
  cannot collide with `ThreeMemberControl` or anything else in `duels.poker.server.auth` — a private
  top-level *class* is not scoped to its file the way a private top-level function is, as
  `ThreeMemberControl`'s own KDoc records. It needs a base interface to give it a supertype:

  ```kotlin
  private interface ForbiddenShapesBase {
      fun inheritedMember()
  }

  private interface ForbiddenShapesControl : ForbiddenShapesBase {
      val aProperty: Int

      fun EmailAddress.aMemberExtension()

      interface Nested

      companion object
  }
  ```

- The class KDoc's final paragraph is replaced by a list of what this test now sees and, in the same
  place, what it still does not — the *Out of scope* entries below, in one or two sentences each. A
  reader of this file must not have to find this ticket to learn where the gate stops.

## Out of scope

Each of these is a surface deliberately **not** asserted. None produces a test, so each is recorded
here and in the file's KDoc rather than left to be inferred from an absence.

- **A top-level extension function** — `suspend fun RecoveryMailer.sendNewsletter(address:
  EmailAddress)`, declared in any file in any module. At a call site it reads `mailer.sendNewsletter(
  addr)`, indistinguishable from a member, and it is invisible to **every** read over
  `RecoveryMailer::class`, because it belongs to the file that declares it. Closing it needs a
  whole-classpath scan and a new dependency — a different and far more brittle mechanism than the
  one §6.2 specifies, and one that would fail on shading, test sources and any module added later.
  The residual is bounded and should be stated as such: an extension has access to nothing but the
  two members, so it can re-aim a **permitted template** at a chosen mailbox but cannot introduce a
  third mail. That is a real hole and a smaller one than the five this ticket closes.
- **What an implementation's body does.** No shape test sees a body; a `sendVerification` that also
  posts to a marketing endpoint passes every assertion here. `NoRecoveryMailer` and
  `DetachedRecoveryMailer` are `TASK-041627` and `TASK-041630`.
- **A second mail port declared elsewhere** — `interface Newsletters` with its own transport, in its
  own file. §6.2 never claimed to prevent that; its claim is that `RecoveryMailer` is the only one
  and that a second is a new file in a diff a reviewer reads. Un-assertable without the same
  classpath scan.
- **`ADR-0031` §6's other four mechanisms** — the `player_id` key (`TASK-041601`), the address never
  leaving the package (`TASK-041616`, `TASK-041607`), no address in a log line (`TASK-041631`), and
  the migration comment (`TASK-041601`). Each has its own gate; this test is §6.2 and only §6.2.
- **Widening the two existing name assertions.** `theMailerDeclaresExactlyTwoMembers` and
  `neitherMailFunctionReturnsAnything` are correct as shipped and are not touched. In particular
  `neitherMailFunctionReturnsAnything`'s `.single { it.name == … }` lookup stays by-name, which
  `TASK-041606`'s Notes record as deliberate: a generic sweep over whatever exists keeps passing
  after a member is renamed out from under it.

## Tests

`RecoveryMailerShapeTest`, four new methods plus one control.

| Test | Proves |
| --- | --- |
| `theMailerDeclaresNoProperty` | `declaredMemberProperties` of `RecoveryMailer::class` is empty. A `val sendMarketing: suspend (EmailAddress) -> Unit` fails the build |
| `theMailerDeclaresNoMemberExtension` | `declaredMemberExtensionFunctions` is empty. A `suspend fun EmailAddress.sendNewsletter(...)` fails the build, which `theMailerDeclaresExactlyTwoMembers` cannot see |
| `theMailerHasNoNestedTypeAndNoCompanion` | `nestedClasses` is empty. Both a nested `interface Marketing` and a `companion object` holding a send function fail the build, through one read |
| `theMailerExtendsNothing` | `supertypes.map { it.classifier }` equals `listOf(Any::class)`. `RecoveryMailer : Newsletters` fails the build, which no `declared*` read can see |
| `theFourReadsSeeTheShapesTheyClaimTo` | The positive control. The **same four helpers**, over `ForbiddenShapesControl::class`, return `[aProperty]`, `[aMemberExtension]`, a set containing `Nested` and `Companion`, and a supertype list that is not `[Any::class]`. Four assertions that something is empty, over a type that has none of those shapes, all pass when the reflection is wrong; this is the only thing that rules that out |

## Acceptance criteria

- [ ] `RecoveryMailerShapeTest.theMailerDeclaresNoProperty` passes
- [ ] `RecoveryMailerShapeTest.theMailerDeclaresNoMemberExtension` passes
- [ ] `RecoveryMailerShapeTest.theMailerHasNoNestedTypeAndNoCompanion` passes
- [ ] `RecoveryMailerShapeTest.theMailerExtendsNothing` passes
- [ ] `RecoveryMailerShapeTest.theFourReadsSeeTheShapesTheyClaimTo` passes
- [ ] The three pre-existing tests pass **unchanged** — no method body is edited
- [ ] Each of the four new tests and the control call the **same** named private helper for its
      surface; no test reads `RecoveryMailer::class` inline where a helper exists
- [ ] `ForbiddenShapesControl` declares all four forbidden shapes at once, and the control test
      asserts a non-empty or non-`[Any::class]` result for **each of the four**
- [ ] The supertype assertion compares the **whole list**; the file contains no assertion of the
      form *`Any::class` is contained in* the supertypes
- [ ] The file contains no assertion on `declaredMemberProperties.size`,
      `nestedClasses.size` or `supertypes.size` alone — each compares names or classifiers, so the
      failure message identifies the offending surface
- [ ] The class KDoc names the four surfaces now asserted **and** the three that are not, top-level
      extension functions first
- [ ] `ForbiddenShapesControl` and `ForbiddenShapesBase` collide with no other top-level declaration
      in `duels.poker.server.auth`, main or test
- [ ] Every command in `verify:` exits 0

## Proof

Each mutation is applied to `RecoveryMailer.kt`, the suite is run, and the mutation is reverted.
All five predictions were probed on this repository's toolchain before this ticket was written.

1. Add `public val sendMarketing: suspend (EmailAddress) -> Unit` to `RecoveryMailer`.
   **`theMailerDeclaresNoProperty` reddens alone**, naming `sendMarketing`. The three pre-existing
   tests stay green — `declaredMemberFunctions` does not enumerate properties — which is precisely
   the hole `TASK-041606`'s shipped KDoc recorded and declined to close. Revert.
2. Add `public suspend fun EmailAddress.sendNewsletter(token: VerificationToken)` to
   `RecoveryMailer`.
   **`theMailerDeclaresNoMemberExtension` reddens alone**, naming `sendNewsletter`.
   **`theMailerDeclaresExactlyTwoMembers` stays green**: probed, an interface declaring one plain
   function and one member extension reports only the plain one from `declaredMemberFunctions`.
   Run this one — it is the fifth shape, it is not among the four the reviewer probed, and it is the
   whole reason this ticket reads two function surfaces rather than one. Revert.
3. Add `public companion object { public suspend fun sendNewsletter(address: EmailAddress) {} }` to
   `RecoveryMailer`.
   **`theMailerHasNoNestedTypeAndNoCompanion` reddens alone**, *expected [], got [Companion]* —
   probed, `nestedClasses` includes the companion object. Revert.
4. Add `public interface Marketing { public suspend fun send(address: EmailAddress) }` **nested
   inside** `RecoveryMailer`.
   **`theMailerHasNoNestedTypeAndNoCompanion` reddens alone**, *expected [], got [Marketing]*. Run
   both 3 and 4: they share one assertion, and a `nestedClasses` read that somehow saw only
   companions would pass one and fail the other. Revert.
5. **The largest.** Declare `public interface Newsletters { public suspend fun sendNewsletter(
   address: EmailAddress) }` beside `RecoveryMailer`, and make the port
   `public interface RecoveryMailer : Newsletters`.
   **`theMailerExtendsNothing` reddens alone**, *expected [kotlin.Any], got [Newsletters,
   kotlin.Any]*. `theMailerDeclaresExactlyTwoMembers` and `neitherMailFunctionReturnsAnything` both
   stay green, because *declared* excludes inherited members — which is the hole this closes.
   **Run this one**: `mailer.sendNewsletter(addr)` now compiles at every call site in the codebase
   and reads exactly like a real member. Revert.
6. **The vacuity control, and the most important run in this ticket.** Leave `RecoveryMailer`
   unmutated. First, point `theFourReadsSeeTheShapesTheyClaimTo` at `RecoveryMailer::class` instead
   of `ForbiddenShapesControl::class`. **It reddens alone**, on the first of its four assertions,
   while the four new tests all pass. Then the reading that matters: revert that, and instead point
   each of the four new tests at `ThreeMemberControl::class` — which has no property, no member
   extension, no nested type and no supertype but `Any`. **Nothing reddens.** Run it and record it
   in the PR. Four assertions that something is empty, evaluated against a type that has none of
   those shapes, is a test that passes when the reflection is wrong, when the helper ignores its
   argument, and when the wrong `KClass` is read. The control is the only thing standing between
   this ticket and that, and it is the failure mode this whole ticket exists to avoid repeating.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.
