---
schema: 2
id: TASK-141009
title: The confirmation states the sign-out it is about to make
type: task
status: ready
parent: STORY-1410
module: web-client
estimate: S
tier: sonnet
review: standard
files_touched: 3
labels: [client, account]
depends_on: [TASK-141008]
verify:
  - cd web-client && npm ci
  - sh -c 'cd web-client && NO_COLOR=1 npx vitest run src/account/SignOutControl.test.tsx 2>&1 | grep -qF "SignOutControl.test.tsx  (8 tests)"'
  - sh -c 'cd web-client && NO_COLOR=1 npx vitest run src/account/AccountScreen.test.tsx 2>&1 | grep -qF "AccountScreen.test.tsx  (20 tests)"'
  - sh -c 'cd web-client && NO_COLOR=1 npx vitest run src/App.test.tsx 2>&1 | grep -qF "App.test.tsx  (36 tests)"'
  - sh -c 'cd web-client && test $(grep -c "signOutWarning" src/account/SignOutControl.tsx) -eq 2'
  - sh -c 'cd web-client && ! grep -q "SIGN_OUT_WARNING" src/account/SignOutControl.tsx'
  - sh -c 'cd web-client && test $(grep -c "signOutHandsANewProfile={false}" src/account/AccountScreen.tsx) -eq 1'
  - sh -c 'cd web-client && B=$(mktemp) && cp src/account/SignOutControl.tsx "$B" && perl -0pi -e "s|signOutWarning\(props.signOutHandsANewProfile\)|signOutWarning(false)|" src/account/SignOutControl.tsx && ! cmp -s src/account/SignOutControl.tsx "$B" || { cp "$B" src/account/SignOutControl.tsx; exit 2; }; NO_COLOR=1 npx vitest run src/account/SignOutControl.test.tsx >/dev/null 2>&1; rc=$?; cp "$B" src/account/SignOutControl.tsx; [ "$rc" -ne 0 ]'
  - git diff --exit-code -- web-client/src/account/account-text.ts web-client/src/lobby/Lobby.tsx web-client/src/account/AccountScreen.test.tsx
  - cd web-client && npm run check
  - cd web-client && npm run build
  - python3 .github/scripts/lint_tickets.py
---

## Goal

`SignOutControl` is told which of the two sign-outs it is offering, states the sentence that
applies, and hands the **same** answer to the call it makes — so the words and the act cannot
disagree.

## Files

| File | Action |
| --- | --- |
| `web-client/src/account/SignOutControl.tsx` | modify |
| `web-client/src/account/SignOutControl.test.tsx` | modify |
| `web-client/src/account/AccountScreen.tsx` | modify |

Probed at `c6a41e6d`: adding the required prop and widening the `signOut` prop type names exactly
these three files under `tsc --noEmit` — `AccountScreen.test.tsx` is **not** among them, because
`AccountScreen`'s own props do not change here.

Read, do not edit: `docs/adr/ADR-0135-the-server-says-which-sign-out-this-is-and-the-browser-forgets-one-key.md`
§7, `web-client/src/account/account-text.ts`.

## Scope

- `SignOutControl`'s props become:

  ```ts
  readonly signedIn: boolean;
  readonly signOutHandsANewProfile: boolean;
  readonly signOut: (handsANewProfile: boolean) => Promise<SignOutOutcome>;
  ```

  `signOutHandsANewProfile` is **required**. `ADR-0135` §7's reason for making the same value
  required on `signOut` applies here unchanged: a component that can be mounted without it is a
  component whose absence is invisible from the outside.
- The confirming step renders `{signOutWarning(props.signOutHandsANewProfile)}` — the module's own
  branch, never a ternary written here. `SIGN_OUT_WARNING` is no longer imported by this file; the
  `! grep -q` gate says so.
- `confirm` calls `props.signOut(props.signOutHandsANewProfile)` — **the same expression** that
  chose the sentence. That is the property worth having: no state exists in which the confirmation
  says one thing and the call does another.
- `props.signedIn` still gates whether the control renders at all, and gates **nothing else**.
  `ADR-0135` §7: *"`signedIn` decides whether a sign-out exists at all, and never which of the two
  it is."*
- The component KDoc's sentence *"it shows `SIGN_OUT_WARNING` and a confirming control"* is now
  false and is rewritten.
- `AccountScreen` passes `signOutHandsANewProfile={false}` — a literal, with a one-line comment
  naming `TASK-141013` as the ticket that replaces it. `false` is `ADR-0135` §6's keep, so the
  screen's behaviour is byte-identical after this merge.

## Out of scope

- **`AccountScreen`'s own props.** It gains its prop in `TASK-141013`; here it only forwards a
  literal, which is what keeps `AccountScreen.test.tsx` out of this ticket's budget — pinned at
  **20** to say so.
- **`AccountCalls` and `main.tsx`.** `TASK-141010`. Here `props.signOut` is typed as taking the
  boolean, and a zero-argument function is still assignable to that type, so nothing upstream
  breaks.
- **`Lobby.tsx`.** In the `git diff --exit-code` gate.
- **`account-text.ts`.** Landed in `TASK-141008`; in the same gate.

## Tests

`SignOutControl.test.tsx`, **5 today → 8**. Every merged render gains the new prop; **one merged
test's input flips and its assertion does not move**, and no assertion anywhere is deleted or
weakened.

The merged tests:

| Merged test | What moves |
| --- | --- |
| `offers nothing to a browser that is not signed in` | its render gains `signOutHandsANewProfile={**true**}` and its assertion is unchanged. This **strengthens** it into `ADR-0135` §7's rule: a browser that is not signed in is offered nothing *even when the answer says a new profile is coming* |
| `offers the control to a browser holding a session`, `warns before it acts, and acts on nothing until it is confirmed`, `calls once, and only from the confirming control`, `puts no browser dialog between the press and the act` | each render gains `signOutHandsANewProfile={false}`; every `SIGN_OUT_WARNING` assertion stays exactly as it is, because `signOutWarning(false)` **is** `SIGN_OUT_WARNING` |

The three new tests:

| Test | Proves |
| --- | --- |
| `states the new-profile sentence when that is the sign-out on offer` | with `signOutHandsANewProfile={true}`, after one press: `getByText(SIGN_OUT_HANDS_A_NEW_PROFILE)` finds it and `queryByText(SIGN_OUT_WARNING)` is `null`. Both directions, because a component rendering both sentences passes a positive-only assertion |
| `states the returning sentence when the profile stays` | the mirror image with `{false}`: `getByText(SIGN_OUT_WARNING)` and `queryByText(SIGN_OUT_HANDS_A_NEW_PROFILE)` is `null` |
| `the press carries the answer the sentence stated` | two renders in one test: with `{true}` the confirming press calls `signOut` with `true`, with `{false}` it calls it with `false` — asserted as `toHaveBeenCalledWith(true)` / `toHaveBeenCalledWith(false)`, not as a call count. One render cannot tell a threaded prop from a constant |

## What would still pass if the coder were wrong

- **Rendering `SIGN_OUT_WARNING` unconditionally** — the shipped line, left alone — passes all five
  merged tests and `states the returning sentence when the profile stays`, and fails
  `states the new-profile sentence when that is the sign-out on offer`.
- **Branching on `props.signedIn` instead of on the answer** passes every test in which the two
  happen to agree and fails the `{true}` test, which renders with `signedIn={true}` and both
  polarities of the other prop.
- **Calling `props.signOut(false)` while rendering the new-profile sentence** — the disagreement
  `ADR-0135` §7 is written to prevent — passes both sentence tests and fails
  `the press carries the answer the sentence stated`. A `verify:` mutation says the same without
  trusting the reading: it rewrites `signOutWarning(props.signOutHandsANewProfile)` to
  `signOutWarning(false)`, proves with `! cmp -s` that the file changed, requires the suite to
  fail, and restores the file either way.
- **Writing the ternary inline in the component** passes every test here and is caught by the two
  `grep` gates: `signOutWarning` must appear **twice** in `SignOutControl.tsx` (the import and the
  call) and `SIGN_OUT_WARNING` must appear **not at all**.
- **Forgetting the literal in `AccountScreen`** is a `tsc` failure, not a silent pass, because the
  prop is required — and the `grep -c "signOutHandsANewProfile={false}"` gate pins it at exactly
  one, so a coder who wired something real here instead has left this ticket's scope.

## Acceptance criteria

- [ ] `SignOutControl.test.tsx` reports `(8 tests)` and all pass
- [ ] `AccountScreen.test.tsx` reports `(20 tests)` and `App.test.tsx` reports `(36 tests)`, all
      passing
- [ ] `grep -c "signOutWarning"` in `SignOutControl.tsx` is 2 and `SIGN_OUT_WARNING` appears 0 times
- [ ] `grep -c "signOutHandsANewProfile={false}"` in `AccountScreen.tsx` is 1
- [ ] The mutation gate exits 0: with the sentence pinned to `signOutWarning(false)`,
      `SignOutControl.test.tsx` fails, and the file is restored
- [ ] `git diff --exit-code` over `account-text.ts`, `Lobby.tsx` and `AccountScreen.test.tsx`
      exits 0
- [ ] `cd web-client && npm run check` and `npm run build` exit 0
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md).
