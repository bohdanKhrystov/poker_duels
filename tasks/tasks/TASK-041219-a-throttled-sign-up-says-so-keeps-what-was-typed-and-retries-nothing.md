---
schema: 2
id: TASK-041219
title: A throttled sign-up says so, keeps what was typed, and retries nothing
type: task
status: ready
parent: STORY-0412
module: web-client
estimate: S
tier: sonnet
review: standard
files_touched: 2
labels: [client, account, ui]
depends_on: [TASK-041218]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test -- src/account/SignUpForm.test.tsx 2>&1 | grep -qE 'Tests +11 passed \(11\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'tells a deliberate refusal from a broken product, on the screen'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'keeps both fields exactly as they were typed'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'sends nothing more until the player submits again'
  - cd web-client && npm run check
---

## Goal

`ADR-0056` lands on the screen: a `429` is a third kind of outcome, the password stays in the box,
neither field is marked, and the client never presses submit on the player's behalf.

## Files

| File | Action |
| --- | --- |
| `web-client/src/account/SignUpForm.tsx` | modify |
| `web-client/src/account/SignUpForm.test.tsx` | modify |

Read, and do not edit:
[`ADR-0056`](../../docs/adr/ADR-0056-a-throttled-sign-up-says-so-and-keeps-what-was-typed.md) §§1–6;
`web-client/src/account/account-text.ts`.

## Scope

- The form renders **three** kinds of outcome, not two (`ADR-0056` §1): success; a refusal about
  something the player typed; and the throttled state, which is about neither the player nor their
  input. `throttled` maps to the third and **nothing else does**.
- After a `throttled`:
  - **Both fields keep their values, the password included, exactly as typed.** Nothing is cleared,
    re-masked or regenerated (§3).
  - **Neither field is marked.** No `aria-invalid`, no per-field message, no error class on either
    input (§3, and §Forecloses makes per-field treatment permanent once refused).
  - **The submit control stays enabled** (§3). A disabled button needs a deadline the client does not
    have.
  - **No automatic retry of any kind** — no timer, no backoff, no poll, no *retrying…* (§3). An
    over-budget request still counts, so an unattended retry lengthens the wait.
  - **No *Retry now* affordance** (§3).
  - The message clears on the next submit, and that submit's outcome replaces it.
- Nothing else about the form moves. The seven tests `TASK-041218` shipped stay as they are.

## Out of scope

- **Any countdown, timer or *try again in N*.** `ADR-0056` §Forecloses refuses it permanently unless
  `Retry-After` ships, and it has not. **A refusal, not an omission** — the no-digit gate is in
  `TASK-041211` and the no-timer gate is here.
- **A `429` anywhere but sign-up.** Sign-in's over-budget answer is a wrong password (`ADR-0027` §6)
  and `forgot-password`'s is a `202`. A client that shared one error mapper across forms must not
  manufacture a throttled state on either; `sign-in.ts` has no `throttled` kind, which is the gate.
- **Spending the offer.** `ADR-0056` §5: a `429` is not a dismissal, and `STORY-0415` owns the offer.

## Tests

`web-client/src/account/SignUpForm.test.tsx`, four tests added to the existing describe block, taking
the file to **11**.

| Test | Proves |
| --- | --- |
| `tells a deliberate refusal from a broken product, on the screen` | Four outcomes in one test: `throttled` renders `SIGN_UP_THROTTLED`, and `failed` — reached from a `500`, a `503` and a rejected fetch through the double — renders `SIGN_UP_FAILED` each time, with the throttled sentence absent. `ADR-0056` §6's first criterion verbatim: **all four asserted together**, so a mapping returning one constant cannot pass |
| `keeps both fields exactly as they were typed` | After a `throttled`, the handle input holds the handle and the password input holds the password — asserted on both values, with a **password that is not the fixture default** and that contains characters a re-mask would eat. `ADR-0056` §6's second criterion |
| `marks neither field and says nothing beside either one` | After a `throttled`: neither input has `aria-invalid`, and the rendered text contains no second sentence beyond `SIGN_UP_THROTTLED` — the message count on screen is exactly one. §6's third criterion |
| `sends nothing more until the player submits again` | After a `throttled`, `vi.advanceTimersByTime(120_000)` and `await` a settled tick: the double's call count is still `1`, the submit control is still enabled, and no control matching `/retry/i` is on screen. §6's fourth criterion, asserted by **request count after the timers have been advanced** |

## Acceptance criteria

- [ ] `signing up for an account > tells a deliberate refusal from a broken product, on the screen`
      passes, asserting all **four** outcomes together
- [ ] `signing up for an account > keeps both fields exactly as they were typed` passes, with a
      password that is not the fixture default
- [ ] `signing up for an account > marks neither field and says nothing beside either one` passes
- [ ] `signing up for an account > sends nothing more until the player submits again` passes, with
      the call count asserted **after** the timers are advanced, the control still enabled, and no
      retry affordance
- [ ] The seven tests from `TASK-041218` pass unchanged
- [ ] `grep -cEi 'setTimeout|setInterval|retry' web-client/src/account/SignUpForm.tsx` returns `0`
- [ ] `grep -c 'throttled' web-client/src/account/sign-in.ts` returns `0`
- [ ] `npm run test -- src/account/SignUpForm.test.tsx` reports `Tests  11 passed (11)`
- [ ] No file outside the two listed differs
- [ ] Every command in `verify:` exits 0

## Proof

1. Map `throttled` to `SIGN_UP_FAILED` — delete the branch and let it fall through.
   **`tells a deliberate refusal from a broken product, on the screen` reddens** on the throttled row
   and on the absence assertion. This is `ADR-0056` §Context's *"one missing branch … invisible in
   review"*, reproduced deliberately. Revert.
2. Clear the password field on a `throttled`.
   **`keeps both fields exactly as they were typed` reddens on the password half alone**, and only
   because that test types a password of its own. Type nothing into the password and watch it pass —
   an empty field cannot tell *kept* from *cleared*, which is why §6's criterion says *"a password
   that is not the fixture default"* and why this ticket repeats it.
3. Disable the submit control while the throttled message is showing.
   **`sends nothing more until the player submits again` reddens** on the enabled assertion, and
   nothing else moves — the call count is still `1`, so the half of the test that looks like the
   point does not catch it.
4. Add a `setTimeout` that resubmits after sixty seconds.
   **`sends nothing more until the player submits again` reddens** on the call count, and only
   because the test advances the timers first. Remove the `advanceTimersByTime` and the mutation
   passes — the advance is the whole assertion, and it is the one thing about this test that looks
   optional.
5. Set `aria-invalid` on the handle input for every non-success outcome.
   **`marks neither field and says nothing beside either one` reddens** on the throttled render, and
   `TASK-041218`'s refusal tests still pass, because none of them looks at the attribute.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.
