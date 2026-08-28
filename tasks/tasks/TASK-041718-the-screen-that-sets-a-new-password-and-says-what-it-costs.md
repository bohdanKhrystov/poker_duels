---
schema: 2
id: TASK-041718
title: The screen that sets a new password, and says what it costs before it acts
type: task
status: done
parent: STORY-0417
module: web-client
estimate: S
tier: sonnet
review: deep
files_touched: 2
labels: [client, account, recovery, ui, security]
depends_on: [TASK-041711]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test -- src/account/ResetScreen.test.tsx 2>&1 | grep -qE 'Test Files +1 passed \(1\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- src/account/ResetScreen.test.tsx 2>&1 | grep -qE 'Tests +8 passed \(8\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'warns that every session ends before it is asked to do anything'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'reads a refused password and a dead link as two different things'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'lets a refused password be corrected on the same link'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'sends the player onward on success, and never before'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'puts the token and the password on no part of the screen'
  - test "$(grep -oiE 'window\.|location|history|localStorage' web-client/src/account/ResetScreen.tsx | wc -l | tr -d ' ')" = 0
  - test "$(grep -oF 'tokenFromHash' web-client/src/account/ResetScreen.tsx | wc -l | tr -d ' ')" = 0
  - grep -qF 'from "./recovery-text"' web-client/src/account/ResetScreen.tsx
  - cd web-client && npm run check
---

## Goal

`ResetScreen` says that every session is about to end **before** the player acts, sends the token it
was handed with the password they typed, tells a refused password from a dead link, and calls
`onDone` only once the server has answered `204`.

## Files

| File | Action |
| --- | --- |
| `web-client/src/account/ResetScreen.tsx` | create |
| `web-client/src/account/ResetScreen.test.tsx` | create |

Read, and do not edit:

- `web-client/src/account/reset-password.ts` — `ResetPasswordOutcome`'s four kinds, and its KDoc
  saying `password-refused` proves nothing about the link.
- `web-client/src/account/recovery-text.ts` — `RESET_HEADING`, `NEW_PASSWORD_LABEL`,
  `RESET_ENDS_EVERY_SESSION`, `RESET_LINK_DEAD`.
- `web-client/src/account/account-text.ts` — `PASSWORD_REFUSED`, which this screen imports for the
  `422`. It is not re-declared in the recovery module.
- [`ADR-0080`](../../docs/adr/ADR-0080-the-password-is-judged-before-the-token-is-touched.md)
  §Consequences — *"`STORY-0417`'s form must be able to move from *password refused* to *link
  expired* without having contradicted itself"*. The third test below is that sentence.
- [`ADR-0031`](../../docs/adr/ADR-0031-an-optional-verified-recovery-email.md) §4 — the reset issues
  no session and returns no token.
- [`ADR-0083`](../../docs/adr/ADR-0083-the-second-account-screen-is-sign-in-and-its-address-is-never-refused.md)
  §1 and §2 — the destination is ***Sign in*** at `#/sign-in`. **This screen does not name it**; it
  calls `onDone` and the lobby navigates.

## Scope

- **Props: a token, a call, and one thing to do afterwards.**

  ```tsx
  export function ResetScreen(props: {
    readonly token: string | null;
    readonly reset: (token: string, newPassword: string) => Promise<ResetPasswordOutcome>;
    readonly onDone: () => void;
  }): ReactElement;
  ```

  No navigation, no address bar, no storage — `ADR-0060` §4, and a `verify:` line pins `window.`,
  `location`, `history` and `localStorage` at zero occurrences.
- **`RESET_HEADING` and `RESET_ENDS_EVERY_SESSION` render at mount**, before anything is typed and
  before any control is pressed. The story: *"The screen says so before it acts."*
- **One controlled `type="password"` input** labelled `NEW_PASSWORD_LABEL`, and one submit, disabled
  while a request is in flight.
- **With `token === null`: `VERIFY_NO_LINK`'s sibling behaviour** — render the heading and the
  warning, and disable the submit. **No request is sent with an empty token**, because the answer is
  known and the endpoint's `422`/`400` order would make it look like a password problem.
- **One sentence per outcome**: `link-dead` → `RESET_LINK_DEAD`; `password-refused` →
  `PASSWORD_REFUSED` (imported from `account-text.ts`); `failed` → `RESET_LINK_DEAD`; `reset` → no
  sentence at all, because `onDone` runs and the lobby replaces the screen.
- **`onDone()` runs exactly once, and only on `reset`.** Not on any refusal, not on mount, not in a
  render path — from the resolved promise, in the submit handler.
- **The password field is cleared on success and kept on every refusal.** `ADR-0056`'s *keep what was
  typed* applies to the refusals; the success case is about to leave the screen and there is no reason
  for a secret to survive it.
- **A refusal does not disable the form.** `ADR-0080`: a `422` costs no link and the same link works
  on the next submission while it lives, so the player must be able to correct and submit again.

## Out of scope

- **Reading the token from the address, or clearing the fragment.** `TASK-041717` did that for
  `verify`; `TASK-041719` does it here.
- **Naming the destination.** `onDone` is a callback; the words and the address are
  `TASK-041719`'s and `ADR-0083`'s.
- **Storing anything.** No session comes back.
- **Client-side password length checks.** `ADR-0048` §2's rule is the server's, and the `422` carries
  the sentence.
- **A *forgot password* door on this screen.** There is none: `ADR-0087` §4 puts the flow's only
  door on the sign-in screen.

## Tests

`web-client/src/account/ResetScreen.test.tsx`, new. **Eight tests.** Query every sentence through its
**constant**.

```ts
const TOKEN = "zqx-reset-token-zqx";
const TYPED = "zqx-new-password-zqx";
```

| Test | Proves |
| --- | --- |
| `warns that every session ends before it is asked to do anything` | At mount, with no interaction: `RESET_HEADING` and `RESET_ENDS_EVERY_SESSION` are both on screen, and the `reset` spy has been called **zero** times. Both halves — the warning is worthless if it arrives after the request |
| `sends the token it was handed with the password that was typed` | Type `TYPED`, submit. The spy was called once with exactly `(TOKEN, TYPED)`. Two distinct literals, so a swapped pair is visible |
| `reads a refused password and a dead link as two different things` | Two renders, `password-refused` and `link-dead`. Each shows its own sentence and **not** the other's. `ADR-0080` reversed the order these arrive in and `STORY-0417` requires two actionable sentences; a collapse here is invisible to any single-outcome test |
| `lets a refused password be corrected on the same link` | One render. First submit answers `password-refused`; `PASSWORD_REFUSED` is on screen, the input still holds `TYPED`, and the submit control is **enabled**. Type a different password and submit again: the spy's **second** call carries the **same** `TOKEN` and the new password, and the answer `reset` is accepted. This is `ADR-0080` §Consequences' sentence, end to end |
| `sends the player onward on success, and never before` | Four renders in one test, answering `reset`, `link-dead`, `password-refused` and `failed`. `onDone` was called exactly **once** in the first and **zero** times in the other three. One test, four cases, because *called on success* and *called always* are indistinguishable from a single case |
| `refuses to send with no link, and still says what a reset costs` | `token={null}`: the warning is on screen, the submit is disabled, and after attempting to submit the spy was called **zero** times. The count is the assertion — a disabled attribute is a claim about the DOM |
| `puts the token and the password on no part of the screen` | With `token={TOKEN}`, type `TYPED`, answer `link-dead`. The container's `textContent` contains neither literal, and its `innerHTML` contains no `TOKEN` — **the typed password will appear in `innerHTML`**, because React reflects a controlled input's `value` into the DOM attribute, so assert `TOKEN` there and `TYPED` only in `textContent`. That asymmetry was measured on this repository and is why the assertion is split |
| `sends nothing twice, however fast the control is pressed` | With a promise that does not settle, click submit three times: the spy was called **once**. A reset token is single-use, and a double submission would report the second attempt as a dead link |

**No `try` anywhere in the added code, and no `expect()` inside one.** Every asynchronous outcome is
awaited through `findByText` or an `act`-wrapped resolution; no test sleeps on a real clock.

## Acceptance criteria

- [ ] `cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'warns that every session ends before it is asked to do anything'`
      — passes, asserting the call count is **zero** at mount
- [ ] `cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'reads a refused password and a dead link as two different things'`
      — passes, each render asserting the other sentence absent
- [ ] `cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'lets a refused password be corrected on the same link'`
      — passes, with the second call carrying the **same** token
- [ ] `cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'sends the player onward on success, and never before'`
      — passes, one call in the success case and **zero** in the other three
- [ ] `cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'puts the token and the password on no part of the screen'`
      — passes, with the `innerHTML` assertion over the **token** only, for the measured reason above
- [ ] `cd web-client && NO_COLOR=1 npm run --silent test -- src/account/ResetScreen.test.tsx 2>&1 | grep -qE 'Tests +8 passed \(8\)'`
      and `… | grep -qE 'Test Files +1 passed \(1\)'` — **exactly eight**. Both lines, because a
      collection error prints a *passing* `Tests` count with no failure line at all
- [ ] `test "$(grep -oiE 'window\.|location|history|localStorage' web-client/src/account/ResetScreen.tsx | wc -l | tr -d ' ')" = 0`
      and `test "$(grep -oF 'tokenFromHash' web-client/src/account/ResetScreen.tsx | wc -l | tr -d ' ')" = 0`
      — the screen reads no address, touches no history and stores nothing. Both read the whole file,
      comments included, so write the KDoc without those words
- [ ] `grep -qF 'from "./recovery-text"' web-client/src/account/ResetScreen.tsx`
      — the copy comes from the module; `PASSWORD_REFUSED` additionally comes from `account-text.ts`
- [ ] `cd web-client && npm run check` exits 0. The whole-suite total is deliberately not pinned:
      this ticket and `TASK-041712`, `TASK-041713`, `TASK-041716` have pairwise disjoint `Files`
      tables and may be dispatched in one batch
- [ ] No file outside the two listed differs
- [ ] Every command in `verify:` exits 0

## Proof

Run each step, record what you measured, and revert it. **These are experiments, not changes**, and
both files are inside this ticket's budget.

1. **Render the warning only after the first submit.** Predict: `warns that every session ends before
   it is asked to do anything` reddens. This is the mutation that turns a warning into a receipt.
2. **Call `onDone` for every settled outcome.** Predict: `sends the player onward on success, and
   never before` reddens on three of its four cases. Then call it **on mount** instead: predict the
   same test reddens on all four. Run both.
3. **Show `RESET_LINK_DEAD` for a `422`.** Predict: `reads a refused password and a dead link as two
   different things` reddens on both halves, since each asserts the other's absence.
4. **Disable the form after any refusal.** Predict: `lets a refused password be corrected on the same
   link` reddens on the second submit. This is the defect `ADR-0080` §Consequences warns about, and it
   is the reason that test exists rather than a shorter one.
5. **Send with an empty token when `token` is null.** Predict: `refuses to send with no link, and
   still says what a reset costs` reddens on the **call count**. Record whether the disabled assertion
   alone would have caught it.
6. **Render the token** — `<p>{props.token}</p>`. Predict: `puts the token and the password on no part
   of the screen` reddens on `textContent`. Then put it in `data-token={props.token}`: predict
   `textContent` stays clean and `innerHTML` reddens. Run both.
7. **Vacuity check on the `innerHTML` half.** Assert the typed password is absent from `innerHTML`
   with the honest component. Predict: **it fails**, because React reflects a controlled input's
   `value` into the DOM attribute — this was measured on this repository when a similar assertion was
   proposed for the sign-up forms. Record the failure, then keep the split assertion the ticket
   specifies. Do not weaken the token half to match.
8. **Drop the in-flight disable.** Predict: `sends nothing twice, however fast the control is pressed`
   reddens on the call count.

> **A red run names a prefix, not a set.** Vitest stops reporting past its first hard failure.
>
> **Expect `The 'NO_COLOR' env is ignored due to the 'FORCE_COLOR' env being set.` on stderr.** It is
> wrong — measured, 0 escape bytes with it and 74 without — so every `grep -qE` needs it.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.

## Notes

**`review: deep` because two of these properties lose an account when they are wrong.** A warning
that arrives after the sessions are gone, and a `422` reported as a dead link, both send a player who
did everything right to the one place this product cannot help them — `ADR-0031`'s Consequences make
a failed recovery a total loss of the account, its coins and its ladder place.

**Step 7 is a prediction that the obvious stronger assertion is false**, and it is written down so a
coder does not spend a dispatch discovering it. React puts a controlled input's value into the DOM,
so the password *is* in `innerHTML` on the honest component. The token is not, because nothing renders
it — which is exactly the property worth asserting there.

**`onDone` rather than a destination.** `ADR-0083` §1 and §2 fix ***Sign in*** at `#/sign-in`, and
`STORY-0417` says it *"does not name it a second time"*. This screen calls a function; `TASK-041719`
binds it to `open("sign-in")`, in the one file that owns navigation.
