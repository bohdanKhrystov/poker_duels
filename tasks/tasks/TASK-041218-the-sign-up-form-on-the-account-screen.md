---
schema: 2
id: TASK-041218
title: The sign-up form — one credential, and the strip is the same profile afterwards
type: task
status: ready
parent: STORY-0412
module: web-client
estimate: S
tier: sonnet
review: standard
files_touched: 2
labels: [client, account, ui]
depends_on: [TASK-041217]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test -- src/account/SignUpForm.test.tsx 2>&1 | grep -qE 'Tests +7 passed \(7\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'leaves the coin balance and the name exactly as they were'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'offers one credential and holds no space for another'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'never fills one field from the other'
  - cd web-client && npm run check
---

## Goal

A player can turn the profile they already have into an account from the account screen, and the
strip beside them still shows the same coins and the same name when they are done.

## Files

| File | Action |
| --- | --- |
| `web-client/src/account/SignUpForm.tsx` | create |
| `web-client/src/account/SignUpForm.test.tsx` | create |

Read, and do not edit: `web-client/src/profile/NameSurface.tsx` (the in-flight guard and the refusal
shape to follow); `web-client/src/account/account-text.ts`;
[`ADR-0041`](../../docs/adr/ADR-0041-a-handle-and-a-password-are-the-only-credential.md);
[`ADR-0031`](../../docs/adr/ADR-0031-an-optional-verified-recovery-email.md) §1.

## Scope

- One export:

  ```ts
  export function SignUpForm(props: {
    readonly signUp: (handle: string, password: string) => Promise<SignUpOutcome>;
  }): ReactElement;
  ```

- Two inputs — `HANDLE_LABEL` on a text input, `PASSWORD_LABEL` on `type="password"` — each with a
  `<label htmlFor>` so a test finds them by their words rather than by a class. One submit control
  labelled `SIGN_UP_LABEL`.
- **The two fields are independent.** Neither is pre-filled from the other and neither is pre-filled
  from the display name. `ADR-0031` §1 and `ADR-0029`: a leaderboard publishes display names, and a
  name that were also a handle would be half a credential. There is no copy anywhere on this form
  suggesting they are the same string.
- **One credential.** No provider row, no *"or continue with…"* divider, no third control, no space
  held open for buttons that do not exist (`ADR-0041`).
- The six refusals map to the six sentences from `account-text.ts`, one at a time: the most recent
  outcome replaces the previous one, never appends. `signed-up` renders `SIGNED_UP` and the form
  stops offering submit.
- A second submit while one is in flight sends nothing, guarded by a `useRef` exactly as
  `NameSurface` does and for the same reason: a credential is attached once.
- No validation before sending. `ADR-0048` §7 publishes the rules so the screen can state them;
  the verdict is the server's.

## Out of scope

- **The throttled state.** `TASK-041219` owns `ADR-0056` §§1–3 in full — the outcome, the preserved
  fields, and the no-auto-retry rule. This ticket renders `SIGN_UP_THROTTLED` for `throttled` and
  stops there.
- **Signing in afterwards.** `docs/protocol.md`: `201` issues no session. The form says so
  (`SIGNED_UP`) and does not navigate.
- **Any recovery address field.** `docs/protocol.md` is explicit that sign-up has none; the recovery
  email is its own endpoint and costs the current password. **A refusal, not an omission** — a
  criterion greps for it.
- Placement on the account screen. `TASK-041222` composes the screen.

## Tests

`web-client/src/account/SignUpForm.test.tsx`, describe block `"signing up for an account"`.

| Test | Proves |
| --- | --- |
| `sends the handle and the password the player typed` | Typing two **different** strings and submitting calls the double with exactly those two, in order |
| `leaves the coin balance and the name exactly as they were` | The form rendered beside a `ProfileStrip` built from `aProfile({ coinBalance: 41, displayName: "Ada" })`: after a `signed-up` outcome, both `41` and `Ada` are still on screen. `STORY-0412`'s first acceptance criterion, and the property `ADR-0030` exists to make true |
| `offers one credential and holds no space for another` | Exactly **two** form controls of type text or password and exactly one submit; and the rendered text matches none of `/continue with/i`, `/or sign (in\|up) with/i`, `/google/i`, `/apple/i`, `/github/i`. `ADR-0041`, gated |
| `never fills one field from the other` | With a profile carrying `displayName: "Ada"` above it, both inputs start empty; and typing into the handle leaves the password empty and vice versa. Fails against a form that seeds the handle from the name — the exact thing `ADR-0031` §1 forbids |
| `says one sentence per refusal, and replaces it on the next attempt` | A `handle-refused` then an `unavailable-handle`: the second sentence is on screen and the first is not. One sentence, never a log |
| `maps each refusal to its own sentence` | `handle-refused`, `unavailable-handle`, `password-refused`, `no-profile` and `failed` each render their own constant, asserted one by one so the failure names which |
| `sends nothing on a second submit while one is in flight` | Two submits before the promise settles: the double's call count is `1` |

Seven tests in a new file: `npm run test -- src/account/SignUpForm.test.tsx` reports **7**.

## Acceptance criteria

- [ ] `signing up for an account > sends the handle and the password the player typed` passes with
      **two different** strings
- [ ] `signing up for an account > leaves the coin balance and the name exactly as they were` passes,
      asserting both after the outcome settles
- [ ] `signing up for an account > offers one credential and holds no space for another` passes,
      asserting the control count **and** all five patterns
- [ ] `signing up for an account > never fills one field from the other` passes, with a display name
      present above the form
- [ ] `signing up for an account > says one sentence per refusal, and replaces it on the next
      attempt` passes
- [ ] `signing up for an account > maps each refusal to its own sentence` passes over all five
- [ ] `signing up for an account > sends nothing on a second submit while one is in flight` passes
      with a call count of `1`
- [ ] `grep -ci 'email\|address' web-client/src/account/SignUpForm.tsx` returns `0`
- [ ] `npm run test -- src/account/SignUpForm.test.tsx` reports `Tests  7 passed (7)`
- [ ] No file outside the two listed differs
- [ ] Every command in `verify:` exits 0

## Proof

1. Initialise the handle input from the profile's `displayName`.
   **`never fills one field from the other` reddens** on the empty-at-start assertion, and only
   because that test renders a profile that **has** a name. Render `aProfile()` — whose `displayName`
   is `null` — and watch the mutation pass: a fixture with no name cannot see a form that copies one.
   That is the trap this test is shaped against. Revert.
2. Render every refusal as `SIGN_UP_FAILED`.
   **`maps each refusal to its own sentence` reddens on four of five rows**, and `says one sentence
   per refusal, and replaces it on the next attempt` reddens too, because the two sentences become
   one. Revert.
3. Append refusals instead of replacing them.
   **`says one sentence per refusal, and replaces it on the next attempt` reddens alone**, on the
   *first sentence is gone* half. A test that only asserted the second sentence was present would
   pass — write it that way once and see.
4. Drop the in-flight `useRef` and keep only the `isSubmitting` state.
   **`sends nothing on a second submit while one is in flight` reddens**, because state has not
   re-rendered by the time the second submit runs. This is the same defect `NameSurface` carries a
   ref for, and it is the reason the ref is named in Scope.
5. Add a disabled *Continue with Google* button.
   **`offers one credential and holds no space for another` reddens** on the pattern check and on the
   control count. Then add it as plain text rather than a button: the count assertion passes and the
   pattern assertion still catches it, which is why the test carries both.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.
