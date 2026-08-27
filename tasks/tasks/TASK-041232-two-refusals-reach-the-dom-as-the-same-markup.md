---
schema: 2
id: TASK-041232
title: Two refusals reach the DOM as the same markup, attributes included
type: task
status: done
parent: STORY-0412
module: web-client
estimate: S
tier: haiku
review: standard
files_touched: 2
labels: [client, account, ui, security]
depends_on: [TASK-041225]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test -- src/account/SignInForm.test.tsx 2>&1 | grep -qE 'Tests +7 passed \(7\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- src/account/SignUpForm.test.tsx 2>&1 | grep -qE 'Tests +12 passed \(12\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'puts nothing in the DOM that tells the two refusals apart'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'puts nothing in the DOM that tells two unavailable-handle refusals apart'
  - cd web-client && grep -q 'container.innerHTML' src/account/SignInForm.test.tsx
  - cd web-client && grep -q 'container.innerHTML' src/account/SignUpForm.test.tsx
  - cd web-client && test "$(grep -o 'data-' src/account/SignInForm.tsx src/account/SignUpForm.tsx | wc -l | tr -d ' ')" = 0
  - cd web-client && npm run check
---

## Goal

Two refusals a player cannot tell apart in words become two refusals nothing in the DOM can tell
apart either — attributes included, not only rendered text.

## Files

| File | Action |
| --- | --- |
| `web-client/src/account/SignInForm.test.tsx` | modify |
| `web-client/src/account/SignUpForm.test.tsx` | modify |

Read, and do not edit: `web-client/src/account/SignInForm.tsx`;
`web-client/src/account/SignUpForm.tsx`; `web-client/src/account/account-text.ts`;
[`TASK-041225`](TASK-041225-the-sign-in-form.md) — its `## Notes` and its `## Tests` row are the
reason this ticket exists and the reason it was not folded into that one.

**No production source changes.** Both components are already correct; this ticket adds the gate
that would notice if they stopped being. A `verify:` line greps both `.tsx` files for `data-` and
requires zero, so a coder who "fixes" a red test by editing a component fails the gate.

## Why this is a new ticket and not a widening of `TASK-041225`

`TASK-041225`'s anti-enumeration test, `says the same sentence to a wrong password and to an
unknown handle`, compares **rendered text** — `screen.getByRole("status").textContent`. Its
sibling `marks neither field, because the server named neither` was strengthened in that same
ticket to a whole-`container.textContent` equality and does not help, because `textContent` does
not see attributes. Measured: a `data-reason` attribute on the status paragraph, with the visible
sentence untouched, leaves **all six** of that file's tests green.

That ticket's `## Tests` row scopes its criterion to what is *"on screen"* and to *"rendered
text"*, so the narrower guard **meets the criterion as written**. This is a recorded limit, not an
unmet requirement and not a live hole: an attacker enumerating handles reads the network response
directly and never needs the DOM. Nobody should file it as a bug against `TASK-041225`, and
nobody should retro-widen that ticket's scope to cover it.

## Scope

- One new test in each of the two files, appended to the existing `describe` block. Nothing
  already in either file moves: no assertion is edited, deleted or weakened, and the two counts go
  6 → 7 and 11 → 12 purely by addition.
- Both tests have the same shape: submit twice, capture `container.innerHTML` after each refusal,
  and assert the two markups are **identical strings**.
- **Both attempts type the same handle and the same password.** This is load-bearing and needs the
  comment the `## Tests` section specifies: React reflects a controlled input's `value` into the
  DOM *attribute*, so two attempts that typed different strings produce different markup over the
  player's own keystrokes, and the equality could never hold. Holding the typing fixed leaves the
  server's answer as the only variable — which is the one under test.
- The two mocked outcomes carry **different** `reason` strings that the real outcome types do not
  declare, assigned through a variable so TypeScript's excess-property check does not strip them.
  `SignInForm.test.tsx` already has `refusedBecause` for exactly this; reuse it rather than adding
  a second helper.
- Each test asserts the refusal sentence is **present** in the captured markup before asserting
  the equality. Without that, a component rendering no status at all satisfies the equality
  trivially.

## Out of scope

- **Changing either component.** They pass today; a red test here means the test is wrong.
- **Normalising, scrubbing or regex-stripping the captured markup.** If the equality does not
  hold, the fixture is varying something it should be holding fixed — almost certainly the typed
  credentials. Fix the fixture, never the comparison. A normalisation added to make this pass is
  the exact failure this ticket exists to prevent.
- **The player's own password sitting in the DOM as `value="…"`.** That is React's controlled-input
  behaviour, is true of every form in this client, and is not a leak of anything the server said.
  Not ticketed.
- **Any server-side or transport-side enumeration surface.** `ADR-0027` §6 owns that and it is
  merged; this ticket is only about the client not rebuilding what the wire closed.

## Tests

`web-client/src/account/SignInForm.test.tsx`, in the existing `describe("signing in")`:

| Test | Proves |
| --- | --- |
| `puts nothing in the DOM that tells the two refusals apart` | Two submissions with identical typed credentials, answered `refused` with two **different** `reason` strings, produce byte-identical `container.innerHTML`. Attributes included, which `textContent` cannot see. Guarded against vacuity by asserting `SIGN_IN_REFUSED` is in the captured markup first |

`web-client/src/account/SignUpForm.test.tsx`, in the existing
`describe("signing up for an account")`:

| Test | Proves |
| --- | --- |
| `puts nothing in the DOM that tells two unavailable-handle refusals apart` | The same property for the one sign-up outcome that collapses two world-states: two `unavailable-handle` answers with different `reason` strings produce byte-identical `container.innerHTML`, with `HANDLE_UNAVAILABLE` asserted present first |

**Why sign-up gets this test, and what it is worth.** `SignUpForm` maps six outcome kinds to six
different sentences, so most of its refusals are *supposed* to be distinguishable and no equality
applies to them. The one exception is visible in the merged copy itself: `HANDLE_UNAVAILABLE` is
*"That handle is taken, or this profile already has a password."* — one sentence over two
different world-states, which is the same collapse sign-in makes. So the property is real and
gated the same way. It is **not** claimed to carry sign-in's threat model: sign-up already tells
the player in words that the handle is taken, and its caller holds a profile already. Recorded as
a regression net over the same shape, and counted as that rather than as coverage of an attack.

Measured, so the coder can predict correctly: with these two tests present,
`src/account/SignInForm.test.tsx` reports **7**, `src/account/SignUpForm.test.tsx` reports **12**,
and the whole client suite is **98 files, 767 tests**.

## Why `innerHTML` and not the alternatives

Three shapes were probed against the real components before this ticket was written. Two of them
are ruled out by measurement, not by taste.

- **Comparing `innerHTML` across `TASK-041225`'s existing scenario** — the shape first suggested —
  **fails on the honest component today.** That scenario types different handles and passwords for
  the two attempts, React reflects each into the `value` attribute, and the two markups differ over
  the player's keystrokes. Measured: not equal, with no leak present. Holding the typing fixed is
  what makes the strict form usable; normalising the markup is what makes it a lie.
- **A sweep for the reason string** (assert neither `reason` appears anywhere in the DOM) is not
  brittle and is also **too weak**. Measured: against a leak that renders a *derived* value —
  `data-reason-code={reason.length}`, never the reason itself — the sweep stays **green** while
  the equality reddens. A sweep can only see a leak that copies the server's words verbatim.
- **Enumerating element attributes by hand** is the same differential with more code and a
  hand-maintained exclusion list for the `value` attributes — and the exclusion list is the thing
  that gets widened later to silence a real failure.

**The cost, stated honestly, and it is not the one that was predicted.** A whole-`innerHTML`
equality against a *recorded golden string* would redden on every incidental markup change. This
is not that: it is an equality between **two renders of the same component in the same state**, so
an incidental change moves both sides identically. Measured — adding a wrapper `<div>` and changing
a `className` on both forms leaves **all 19 tests green**. The real cost is different and is why
the fixture comment is mandatory: a later editor who "improves" the fixture by varying the typed
handles gets a red that is not a leak, and the comment is what stops them from reaching for a
normalisation.

## Acceptance criteria

- [ ] `signing in > puts nothing in the DOM that tells the two refusals apart` passes
- [ ] `signing up for an account > puts nothing in the DOM that tells two unavailable-handle
      refusals apart` passes
- [ ] Both tests compare `container.innerHTML`, not `textContent` and not a normalised derivative
      of either
- [ ] Both tests assert the refusal sentence is present in the captured markup before comparing
- [ ] Both tests type the **same** handle and the **same** password for both attempts, with the
      comment explaining that React reflects `value` into the DOM attribute
- [ ] The two mocked outcomes carry **different** `reason` strings
- [ ] `npm run test -- src/account/SignInForm.test.tsx` reports `Tests  7 passed (7)`, all six
      pre-existing tests unedited
- [ ] `npm run test -- src/account/SignUpForm.test.tsx` reports `Tests  12 passed (12)`, all
      eleven pre-existing tests unedited
- [ ] The `data-` zero-gate in `verify:` exits 0 — neither component was edited
- [ ] No file outside the two listed differs
- [ ] Every command in `verify:` exits 0

## Proof

Every step below was **measured on `1a8e0add`** with the finished tests in place, not predicted.
Re-run them; a step whose result differs from what is written is a finding about this ticket.

1. **The leak this ticket exists to catch.** On both components, hold the whole settled outcome in
   state and render `data-reason={reason}` on the `<p role="status">`, leaving its text untouched.
   **Both new tests redden by name, and exactly the 17 pre-existing tests stay green** —
   `2 failed | 17 passed (19)`. This is the acceptance test for the ticket: before it, the same
   mutation left all six sign-in tests and all eleven sign-up tests green.
2. **Not a string search.** Replace that attribute with `data-reason-code={reason?.length}`, so the
   server's words never reach the DOM at all. **Both new tests still redden**, again
   `2 failed | 17 passed (19)`. A test that swept for the reason string would pass here — that is
   the measurement that chose the equality over the sweep.
3. **Not brittle to markup.** With no leak, wrap the status paragraph in a
   `<div className="w-full rounded-small bg-surface px-2 py-1">` and add `text-muted` to the
   paragraph, on both forms. **All 19 tests stay green.** Both sides of the equality move
   together, which is why the differential form does not pay the golden-string cost.
4. **Not vacuous.** On `SignInForm` alone, suppress the status paragraph entirely (`{false && …}`).
   **Four tests redden and the new one is among them by name** — the presence assertion is what
   fires, since an equality over two renders showing no refusal would otherwise hold trivially.
   The other three are `says the same sentence…`, `marks neither field…` and `tells a refusal from
   a broken server`.
5. **The fixture rule is load-bearing.** In the new sign-in test, change the second attempt to type
   a different handle, leaving both components honest. **It reddens alone** —
   `1 failed | 6 passed (7)` — because React reflects `value` into the DOM attribute. Restore it.
   This is the red a future editor will hit, and the reason the fixture comment is a criterion
   rather than a nicety: the tempting repair is to normalise the markup, and that repair silently
   deletes step 1.

## Notes

`grep -c` counts matching **lines**, not occurrences, and exits 1 when nothing matches. The
zero-expectation in `verify:` is therefore written as
`test "$(grep -o … | wc -l | tr -d ' ')" = 0`, whose pipeline exits 0 and prints `0` on no match —
measured. `NO_COLOR=1` pins the count greps for the same reason `TASK-041225` pins them.

Measured while writing this: `data-testid` appears in **no** file under `web-client/src`, so the
`data-` zero-gate forbids nothing this client already does.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.
