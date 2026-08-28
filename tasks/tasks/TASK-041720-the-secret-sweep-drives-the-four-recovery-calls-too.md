---
schema: 2
id: TASK-041720
title: The secret sweep drives the four recovery calls too
type: task
status: done
parent: STORY-0417
module: web-client
estimate: S
tier: sonnet
review: deep
files_touched: 2
labels: [client, account, recovery, security, test]
depends_on: [TASK-041719]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test -- src/account/no-secret-in-a-url.test.ts 2>&1 | grep -qE 'Test Files +1 passed \(1\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- src/account/no-secret-in-a-url.test.ts 2>&1 | grep -qE 'Tests +5 passed \(5\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'puts no handle, password or token in any path it requests'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'sends no player id in any body it writes'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'puts no secret in anything it logs'
  - test "$(grep -oF 'calls.length).toBe(8)' web-client/src/account/no-secret-in-a-url.test.ts | wc -l | tr -d ' ')" = 5
  - test "$(grep -oF 'try {' web-client/src/account/no-secret-in-a-url.test.ts | wc -l | tr -d ' ')" = 0
  - grep -qF 'attachRecoveryEmail' web-client/src/account/no-secret-in-a-url.md
  - cd web-client && npm run check
---

## Goal

The merged secret sweep drives **eight** account calls instead of four, so the recovery address, the
new password and the two mailed tokens are held to the same rule the handle, the password and the
session token already are — and its `expect()`s are no longer inside a `try`.

## Files

| File | Action |
| --- | --- |
| `web-client/src/account/no-secret-in-a-url.test.ts` | modify |
| `web-client/src/account/no-secret-in-a-url.md` | modify |

Read, and do not edit:

- `web-client/src/account/no-secret-in-a-url.test.ts` as it stands — five tests, `driveAllFourCalls`,
  `SECRETS`, `FORBIDDEN_BODY_KEYS`, `CALL_LABELS`, and the four `expect(calls.length).toBe(4)`
  assertions this ticket moves.
- `web-client/src/account/no-secret-in-a-url.md` — what the sweep claims to cover and what it says it
  cannot reach.
- The four recovery modules' signatures: `attachRecoveryEmail`, `forgotPassword`, `verifyEmail`,
  `resetPassword`.
- [`ADR-0081`](../../docs/adr/ADR-0081-a-mailed-link-is-a-fragment-route-and-the-token-is-the-segment-behind-the-slug.md)
  §2 — a recovery link contains no `?` at all, and the token reaches no server in a URL.

## Scope

- **Four more distinctive literals**, in the merged `zqx-…-zqx` idiom:

  ```ts
  const ADDRESS = "zqx-address-zqx";
  const VERIFY_TOKEN = "zqx-verify-token-zqx";
  const RESET_TOKEN = "zqx-reset-token-zqx";
  const NEW_PASSWORD = "zqx-new-password-zqx";
  ```

  **All four join `SECRETS`**, taking it from three entries to seven. Every existing assertion loops
  over `SECRETS`, so the four new values are covered by all five tests with no assertion rewritten.
- **`driveAllFourCalls` becomes `driveEveryAccountCall`** and drives eight, in an order that keeps
  the merged one intact: `signUp`, `signIn`, `attachRecoveryEmail`, `forgotPassword`, `verifyEmail`,
  `resetPassword`, `revokeThisDevice`, `signOut`. `signOut` stays last for the merged reason — it
  forgets the stored session token — and the four new calls go before `revokeThisDevice` so the token
  is still present when its turn comes.
- **`CALL_LABELS` gains the four names**, in the same order, so a failure still names the call.
- **The recorder is answered eight times**: `202`, `202`, `204`, `204` for the four new calls, added
  to the merged four answers.
- **The four `expect(calls.length).toBe(4)` become `toBe(8)`** — one in each of the five tests. This
  is the presence half of the sweep and it is what stops a drive that silently skipped a call.
- **The `try`/`finally` in `puts no secret in anything it logs` is removed.** Its three
  `expect(...).toBe(false)` calls currently sit inside a `try`, and a failing assertion is itself a
  throw, so a failure there is swallowed by the `finally` and the test goes green. Restore the spies
  with `afterEach` and `vi.restoreAllMocks()` instead, and a `verify:` line pins `try {` at zero in
  the file.
- **`no-secret-in-a-url.md` records what changed**: eight calls, seven secrets, and that the token
  travelling in a fragment is a surface this sweep **can** reach — it already asserts
  `window.location.href` after every call — while a real browser's `Referer` remains one it cannot.

## Out of scope

- **Any production file.** If a sweep assertion reddens against merged code, that is the finding:
  report it and file a ticket rather than editing outside this budget.
- **Adding a sixth test.** The five loop over `SECRETS` and over `calls`; widening the fixtures is
  what widens the coverage, and a sixth test asserting the same thing over the same data would read
  as more rigour and be less.
- **`FORBIDDEN_BODY_KEYS`.** Unchanged — none of the four new bodies carries an identifier, and the
  merged loop already covers every body it finds.

## Tests

`web-client/src/account/no-secret-in-a-url.test.ts`. **The count does not move: five tests before and
five after.** All five change, and this ticket owns every one of them, because widening the drive is
what invalidates their `calls.length` assertions.

| Test | What moves |
| --- | --- |
| `puts no handle, password or token in any path it requests` | `calls.length` 4 → 8. Its presence half gains a clause: one call's body contains `ADDRESS` and one contains `RESET_TOKEN`, so the four new secrets are proven to have travelled somewhere legitimate before their absence from paths is asserted. Its absence loop is unchanged and now covers 8 × 7 pairs |
| `sends no player id in any body it writes` | `calls.length` 4 → 8. The two `body).toBeUndefined()` assertions for `revokeThisDevice` and `signOut` keep their meaning but move index; the `namedBodies` list gains the four new bodies |
| `leaves no secret in the address bar after any of the four calls` | `calls.length` and `hrefsAfterEachCall.length` 4 → 8. **The test's name does not change**, because a merged test pinned by name is a gate a coder can satisfy by writing a new test — the `verify:` lines above name three of the five and the file's count pins all five |
| `puts no secret in anything it logs` | `calls.length` 4 → 8, **and the `try`/`finally` is deleted**. The three assertions move out of the `try`; spy restoration moves to `afterEach` |
| `sends the device id and the session token in headers and nowhere else` | `calls.length` 4 → 8; the destructure of the recorded calls moves index. Its positive half is unchanged — `signUp` still carries `X-Device-Id`, and `revokeThisDevice` and `signOut` still carry `Authorization` |

**No `try` anywhere in this file when the ticket lands, and no `expect()` inside one.**

## Acceptance criteria

- [ ] `cd web-client && NO_COLOR=1 npm run --silent test -- src/account/no-secret-in-a-url.test.ts 2>&1 | grep -qE 'Tests +5 passed \(5\)'`
      and `… | grep -qE 'Test Files +1 passed \(1\)'` — **exactly five**, unmoved: this ticket widens
      the fixtures and adds no test. Both lines, because a collection error prints a *passing* `Tests`
      count with no failure line at all
- [ ] `test "$(grep -oF 'calls.length).toBe(8)' web-client/src/account/no-secret-in-a-url.test.ts | wc -l | tr -d ' ')" = 5`
      — **all five** tests assert eight calls. Four would mean one test still drives the old fixture
      and covers none of the new secrets
- [ ] `test "$(grep -oF 'try {' web-client/src/account/no-secret-in-a-url.test.ts | wc -l | tr -d ' ')" = 0`
      — no assertion in this file is inside a `try`. A sweep in this repository shipped that way and
      planting a forbidden key left all six of its tests green
- [ ] `cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'puts no handle, password or token in any path it requests'`
      and the two other named tests pass
- [ ] `grep -qF 'attachRecoveryEmail' web-client/src/account/no-secret-in-a-url.md`
      — the note says what the sweep now drives
- [ ] `cd web-client && npm run check` exits 0
- [ ] No production file differs, and no file outside the two listed differs
- [ ] Every command in `verify:` exits 0

## Proof

Run each step, record what you measured, and revert it. **These mutations are experiments, not
changes.** Steps 1–4 edit files **outside** this ticket's `Files` table — mutate, measure, revert,
and confirm `git status` is clean of them before opening the PR.

1. **Put the reset token in a query** — `"/api/auth/reset-password?token=" + request.token` in
   `reset-password.ts`. Predict: `puts no handle, password or token in any path it requests` reddens,
   naming `resetPassword`. Record the message; the label is what makes this sweep usable.
2. **Log the address** — `console.error("attach", request.address)` in `attach-recovery-email.ts`.
   Predict: `puts no secret in anything it logs` reddens. **Run this both with and without the
   `try`/`finally` removed** — with the `try` in place, predict it goes **green**, which is the whole
   reason that edit is in this ticket. Record both runs.
3. **Put the verification token in a header** — `{ "X-Token": request.token }` in `verify-email.ts`.
   Predict: `sends the device id and the session token in headers and nowhere else` does **not**
   redden, because its negative half loops over `bearerSecrets` and not over `SECRETS`. Record that:
   it is a real gap, and the honest response is to say so in `no-secret-in-a-url.md` rather than to
   widen a test outside this ticket's plan.
4. **Skip one call in the drive** — return early before `verifyEmail`. Predict: all five tests redden
   on `calls.length`. This is the presence half, and it is why the count is in every test rather than
   in one.
5. **Drop one of the four new literals from `SECRETS`.** Predict: **nothing reddens** unless a
   mutation from step 1 or 2 is also applied. Record the green run — a sweep's coverage is invisible
   until something leaks, which is why steps 1–3 exist and why a coverage claim without them is not
   evidence.
6. **Vacuity check on the address-bar test.** Assert `hrefsAfterEachCall.length` is 8 and then make
   the drive push nothing. Predict: the length assertion reddens. Without it, the per-href loop would
   pass over an empty array.

> **A red run names a prefix, not a set.** Vitest stops reporting past its first hard failure.
>
> **Expect `The 'NO_COLOR' env is ignored due to the 'FORCE_COLOR' env being set.` on stderr.** It is
> wrong — measured, 0 escape bytes with it and 74 without — so every `grep -qE` needs it.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.

## Notes

**The `try` removal is the most valuable line in this ticket.** `puts no secret in anything it logs`
currently wraps its three `expect(...).toBe(false)` calls in a `try` whose `finally` restores the
spies. A failing `expect` throws, the `finally` runs, and the throw is **not** rethrown by that
construct only because there is no `catch` — but any `catch` added later, or any assertion helper
that swallows, turns the whole sweep green. This exact shape shipped elsewhere in this epic and
planting a forbidden key left all six of its tests passing. Step 2 measures it here rather than
asserting it.

**Step 3's predicted gap is recorded, not closed.** The header check for arbitrary secrets loops over
`bearerSecrets` (the device id and the session token) and not over `SECRETS`, so a recovery token
placed in a header is caught by `TASK-041709`'s own test and by nothing here. Widening that loop is a
one-line change to a merged assertion in a test this ticket already edits — and it is deliberately
**not** done, because the loop's positive half asserts specific header names for specific calls and
changing what it iterates changes what its failure messages mean. File it as a follow-up if the
measurement confirms the prediction.
