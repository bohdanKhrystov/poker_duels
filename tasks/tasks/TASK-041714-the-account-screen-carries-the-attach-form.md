---
schema: 2
id: TASK-041714
title: The account screen carries the attach form, and only where it can be used
type: task
status: backlog
parent: STORY-0417
module: web-client
estimate: S
tier: haiku
review: standard
files_touched: 2
labels: [client, account, recovery, ui]
depends_on: [TASK-041712, TASK-041713]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test -- src/account/AccountScreen.test.tsx 2>&1 | grep -qE 'Test Files +1 passed \(1\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- src/account/AccountScreen.test.tsx 2>&1 | grep -qE 'Tests +11 passed \(11\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'offers the attach form only with a profile in hand and a call to make'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'offers it whether recovery is already on or not'
  - cd web-client && NO_COLOR=1 npm run --silent test -- src/account/RecoveryEmailForm.test.tsx 2>&1 | grep -qE 'Tests +7 passed \(7\)'
  - test "$(grep -oF 'RecoveryEmailForm' web-client/src/account/AccountScreen.tsx | wc -l | tr -d ' ')" = 2
  - test "$(grep -oF 'attachRecoveryEmail(' web-client/src/account/AccountScreen.tsx | wc -l | tr -d ' ')" = 0
  - test "$(grep -oF 'await' web-client/src/account/AccountScreen.tsx | wc -l | tr -d ' ')" = 0
  - cd web-client && npm run check
---

## Goal

`AccountScreen` renders `RecoveryEmailForm` when it has both a profile and an attach call, and
renders nothing where either is missing — the same withholding rule `SignUpForm` already follows on
that screen.

## Files

| File | Action |
| --- | --- |
| `web-client/src/account/AccountScreen.tsx` | modify |
| `web-client/src/account/AccountScreen.test.tsx` | modify |

Read, and do not edit:

- `web-client/src/account/RecoveryEmailForm.tsx` — its one prop, `attach`.
- `web-client/src/account/AccountScreen.tsx`'s merged `showSignUp` block — the optional-prop pattern
  this copies, and the comment above it saying why the screen withholds rather than the child.
- `web-client/src/account/recovery-text.ts` — `ATTACH_LABEL`, for querying the form in a test.

## Scope

- **One optional prop**, beside `signUp`, `signOut` and `onSignIn`:

  ```tsx
  readonly attachRecoveryEmail?: (
    address: string,
    currentPassword: string,
  ) => Promise<AttachRecoveryOutcome>;
  ```

  Optional for the merged reason: a caller that only cares about the route facts
  (`AccountScreen.test.tsx`'s older tests) need supply none, and every merged test keeps compiling.
- **Rendered when a profile is in hand and the prop is supplied**, and never otherwise:

  ```tsx
  const showAttach =
    profile !== null && profile.kind === "profile" && attachRecoveryEmail !== undefined;
  ```

  The profile guard is not cosmetic: `POST /api/auth/recovery-email` answers `401` for a browser the
  server cannot resolve, and offering a form whose only possible answer is *that did not go through*
  is worse than offering none.
- **Placed after the recovery sentence and before the sign-in door**, so the fact and the control
  that changes it read together.
- **It is offered whether `hasRecoveryEmail` is `true` or `false`.** Attaching a second address
  replaces the pending claim on the server (`ADR-0031` §3, `claimPending`), and a screen that hid the
  form once recovery was on would strand a player whose address stopped working.
- **The component passes the prop straight through** as `attach`; it does not wrap it, log it or
  branch on its result.

## Out of scope

- **Supplying the prop.** `TASK-041715` does that from `Lobby.tsx`.
- **Gating on `signedIn`.** The device id authenticates this endpoint too, so a browser that has never
  signed in can still attach — and `ADR-0031` §3's password check is the server's, not a reason to
  hide the form.
- **Any change inside `RecoveryEmailForm`.** A `verify:` line pins its suite at seven.
- **Any new sentence.** Nothing is added to `recovery-text.ts`.

## Tests

`web-client/src/account/AccountScreen.test.tsx`. **9 tests become 11.** Query the form by its submit
control's accessible name, `ATTACH_LABEL` — the constant, never a literal.

| Test | Proves |
| --- | --- |
| `offers the attach form only with a profile in hand and a call to make` | Four renders. With a profile **and** the prop: the form is present. With a profile and **no** prop: absent. With the prop and `profile={null}`: absent. With the prop and `{ kind: "no-profile" }`: absent. Presence first, so the three absences are withheld forms rather than a component that renders nothing |
| `offers it whether recovery is already on or not` | Two renders, `hasRecoveryEmail: true` and `false`, both with the prop: the form is present in both, and the recovery sentence differs between them — asserted in the same test, so a screen that hid the form when recovery was on reddens here rather than being discovered by a player whose address stopped working |

The nine merged tests pass unchanged: the new prop is optional, so none of them needs a new argument.

**No `try` anywhere in the added code, and no `expect()` inside one.**

## Acceptance criteria

- [ ] `cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'offers the attach form only with a profile in hand and a call to make'`
      — passes, over all four combinations, presence asserted first
- [ ] `cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'offers it whether recovery is already on or not'`
      — passes, and asserts the two recovery sentences differ in the same test
- [ ] `cd web-client && NO_COLOR=1 npm run --silent test -- src/account/AccountScreen.test.tsx 2>&1 | grep -qE 'Tests +11 passed \(11\)'`
      and `… | grep -qE 'Test Files +1 passed \(1\)'` — **exactly eleven**: `TASK-041712`'s nine plus
      these two. Both lines, because a collection error prints a *passing* `Tests` count with no
      failure line at all
- [ ] `cd web-client && NO_COLOR=1 npm run --silent test -- src/account/RecoveryEmailForm.test.tsx 2>&1 | grep -qE 'Tests +7 passed \(7\)'`
      — the child is untouched
- [ ] `test "$(grep -oF 'RecoveryEmailForm' web-client/src/account/AccountScreen.tsx | wc -l | tr -d ' ')" = 2`
      — the import and one element. A second element is a second form on one screen
- [ ] `test "$(grep -oF 'attachRecoveryEmail(' web-client/src/account/AccountScreen.tsx | wc -l | tr -d ' ')" = 0`
      and `test "$(grep -oF 'await' web-client/src/account/AccountScreen.tsx | wc -l | tr -d ' ')" = 0`
      — the screen never **calls** the attach function and never awaits anything. It declares the
      prop, guards on it and hands it on; sending the request is the form's. Both read the whole file
- [ ] `cd web-client && npm run check` exits 0
- [ ] Every merged test in `AccountScreen.test.tsx` passes unchanged. No assertion moves and none is
      weakened
- [ ] No file outside the two listed differs
- [ ] Every command in `verify:` exits 0

## Proof

Run each step, record what you measured, and revert it. **These are experiments, not changes**, and
both files are inside this ticket's budget.

1. **Render the form unconditionally**, dropping the profile guard. Predict: `offers the attach form
   only with a profile in hand and a call to make` reddens on both profile-less renders.
2. **Drop the `attachRecoveryEmail !== undefined` guard.** Predict: it will not typecheck, because the
   prop is optional — record the `tsc` message, then force it with a non-null assertion and predict
   the second render reddens with a thrown error rather than an absence. Note which; a crash and a
   withheld form are different failures and only one is caught by `queryBy…`.
3. **Hide the form when `hasRecoveryEmail` is `true`.** Predict: `offers it whether recovery is
   already on or not` reddens **alone**. If it stays green, the two renders in that test are not
   actually differing on the flag — fix the fixture.
4. **Vacuity check on the absences.** Return `null` from the whole component. Predict: the presence
   assertions redden and every absence stays green. Confirm both tests carry a presence half.

> **Expect `The 'NO_COLOR' env is ignored due to the 'FORCE_COLOR' env being set.` on stderr.** It is
> wrong — measured, 0 escape bytes with it and 74 without — so every `grep -qE` needs it.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.

## Notes

**The two zero gates are about behaviour, not naming.** The prop's name necessarily appears in the
props type and in the destructure, so counting the bare identifier would be a gate on style. Counting
`attachRecoveryEmail(` and `await` instead says the one thing worth saying: this screen decides
**whether** to offer the form and never what happens when it is used. `AccountScreen` holds no state
and awaits nothing today, and it should still hold none after this.
