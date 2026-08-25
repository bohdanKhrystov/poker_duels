---
schema: 2
id: TASK-041225
title: The sign-in form, and one sentence for both ways it can be refused
type: task
status: backlog
parent: STORY-0412
module: web-client
estimate: S
tier: sonnet
review: standard
files_touched: 2
labels: [client, account, ui]
depends_on: [TASK-041224]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test -- src/account/SignInForm.test.tsx 2>&1 | grep -qE 'Tests +6 passed \(6\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'says the same sentence to a wrong password and to an unknown handle'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'marks neither field, because the server named neither'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'offers one credential and holds no space for another'
  - cd web-client && npm run check
---

## Goal

A player on a browser that has never seen their account can type a handle and a password and get in,
and a refusal tells them exactly as much as the server did — which is that the pair did not match.

## Files

| File | Action |
| --- | --- |
| `web-client/src/account/SignInForm.tsx` | create |
| `web-client/src/account/SignInForm.test.tsx` | create |

Read, and do not edit: `web-client/src/account/SignUpForm.tsx` (the shape to follow);
`web-client/src/account/account-text.ts`;
[`ADR-0041`](../../docs/adr/ADR-0041-a-handle-and-a-password-are-the-only-credential.md);
[`ADR-0027`](../../docs/adr/ADR-0027-the-session-outranks-the-device-id.md) §6.

## Scope

- One export:

  ```ts
  export function SignInForm(props: {
    readonly signIn: (handle: string, password: string) => Promise<SignInOutcome>;
  }): ReactElement;
  ```

- Two inputs with `HANDLE_LABEL` and `PASSWORD_LABEL`, one submit labelled `SIGN_IN_LABEL`. Same
  shape as the sign-up form, deliberately: one credential, no provider row, no divider, no space held
  open (`ADR-0041`).
- `refused` renders `SIGN_IN_REFUSED` and **nothing else**: neither field is marked, neither is
  named, and no alternative is suggested. The server made an unknown handle and a wrong password
  indistinguishable, and a screen that guessed which would rebuild the oracle in words.
- `failed` renders `SIGN_UP_FAILED` — the same *that did not go through* sentence, because a broken
  server is a broken server on either form.
- **No throttled state.** `ADR-0056` §1: sign-in has none, because `ADR-0027` §6 makes its
  over-budget answer identical to a wrong password. A client sharing one error mapper across the two
  forms must not manufacture one here.
- On `signed-in` the module reloads the document, so the form renders nothing special for success —
  it just stops offering submit while the call is in flight.
- A second submit while one is in flight sends nothing, guarded by a `useRef` as `SignUpForm` does.

## Out of scope

- **Anything about the anonymous profile this browser already holds.** `ADR-0030` §6 makes signing
  into another account from a device that owns a profile legal and coherent, and the coins stay where
  they are. The form says nothing about it; `STORY-0414` proves the property end to end.
- **A *forgot password* door.** `STORY-0417` owns that screen and `ADR-0081` fixed its neighbours'
  slugs. **A refusal, not an omission** — a criterion greps for it.
- The screen this form sits on, its heading and its address — `TASK-041226` and `TASK-041227`, behind
  `DEC-077`. This component is renderable and testable on its own, which is `ADR-0060` §4's rule.

## Tests

`web-client/src/account/SignInForm.test.tsx`, describe block `"signing in"`.

| Test | Proves |
| --- | --- |
| `sends the handle and the password the player typed` | Two **different** strings arrive at the double in order |
| `says the same sentence to a wrong password and to an unknown handle` | Two submissions, both answered `refused`, put the identical `SIGN_IN_REFUSED` on screen, and the rendered text matches neither `/handle/i` nor `/password/i` as the subject of a verdict. The one sentence, gated on the screen as well as in the copy module |
| `marks neither field, because the server named neither` | After a `refused`, neither input carries `aria-invalid` and no message sits beside either one |
| `tells a refusal from a broken server` | `refused` and `failed` render **different** sentences, asserted not equal, in one test |
| `offers one credential and holds no space for another` | Exactly two credential inputs and one submit; the rendered text matches none of `/continue with/i`, `/google/i`, `/apple/i`, `/github/i`, `/forgot/i`. The last pattern is `STORY-0417`'s door, refused here on purpose |
| `sends nothing on a second submit while one is in flight` | Two submits before the promise settles: call count `1` |

Six tests in a new file: `npm run test -- src/account/SignInForm.test.tsx` reports **6**.

## Acceptance criteria

- [ ] `signing in > sends the handle and the password the player typed` passes with two different
      strings
- [ ] `signing in > says the same sentence to a wrong password and to an unknown handle` passes,
      asserting the identical sentence twice and both absent patterns
- [ ] `signing in > marks neither field, because the server named neither` passes
- [ ] `signing in > tells a refusal from a broken server` passes, including the inequality
- [ ] `signing in > offers one credential and holds no space for another` passes over all five
      patterns
- [ ] `signing in > sends nothing on a second submit while one is in flight` passes with a call count
      of `1`
- [ ] `grep -ci 'throttl\|forgot\|reset' web-client/src/account/SignInForm.tsx` returns `0`
- [ ] `npm run test -- src/account/SignInForm.test.tsx` reports `Tests  6 passed (6)`
- [ ] No file outside the two listed differs
- [ ] Every command in `verify:` exits 0

## Proof

1. Render *"We do not know that handle."* for `refused`.
   **`says the same sentence to a wrong password and to an unknown handle` reddens** on the sentence
   assertion and on the `/handle/i` pattern, and `tells a refusal from a broken server` still passes.
   Run it: the two-sentence version is what a helpful reviewer asks for, and it is the oracle the
   server spent a design closing.
2. Set `aria-invalid` on the password input for `refused`.
   **`marks neither field, because the server named neither` reddens alone.** Nothing about the
   sentence moves — marking a field is a verdict with no words, and only this test sees it.
3. Add a `throttled` branch mapping some status to a throttled sentence.
   **`npm run typecheck` reddens**, because `SignInOutcome` has no such kind. That is the gate, and
   it is the compiler rather than a test — which is why `sign-in.ts`'s union has three members and
   not seven.
4. Render `SIGN_UP_FAILED` for `refused` as well as for `failed`.
   **`tells a refusal from a broken server` reddens on the inequality alone.** Both individual
   renders still look right, which is what a collapsed mapping always looks like.
5. Add a *Forgot your password?* link.
   **`offers one credential and holds no space for another` reddens** on the `/forgot/i` pattern.
   Keep it red for a moment: `STORY-0417` will delete that pattern and add the door in the same PR,
   and the pattern being here is what makes that a deliberate edit rather than a quiet arrival.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.
