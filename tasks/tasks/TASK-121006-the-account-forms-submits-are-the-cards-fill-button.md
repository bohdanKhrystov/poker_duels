---
schema: 2
id: TASK-121006
title: The `account` screen's two form submits are the card's fill button, not a smaller one
type: task
status: done
parent: STORY-1210
module: web-client
estimate: XS
tier: sonnet
review: light
files_touched: 3
labels: [qa, uat, bug, medium]
depends_on: [TASK-121003]
verify:
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose src/account/AccountScreen.test.tsx 2>&1 | grep -qF "both account forms submit with the card's fill button"
  - cd web-client && NO_COLOR=1 npm run --silent test -- src/account/AccountScreen.test.tsx src/account/SignUpForm.test.tsx src/account/RecoveryEmailForm.test.tsx
  - cd web-client && NO_COLOR=1 npm run --silent check
  - python3 .github/scripts/lint_tickets.py
---

## Goal

*Give this profile a password* and *Attach a recovery address* carry the treatment
`design/screens/account.html` draws for them, so the account screen's two committing actions look
like the primary actions they are.

## The defect

Round 2 of `/qa-cycle uat regression`, 2026-08-30, commit `07df9e7f`, against
`design/screens/account.html` — merged the day before, by `TASK-120904`.

Both submits carry a class. It is the wrong one:

```
BUTTON "Give this profile a password"
cls "rounded-small border border-hairline bg-surface px-4 py-2 text-small"
bg "rgb(28, 26, 24)"  pad "4px 12px"  radius "4px"

BUTTON "Attach a recovery address"   — identical
```

Card, `account.html:92, 129`: `<button class="btn fill">`, and
`.btn.fill { background: var(--pd-accent-fill); color: var(--pd-on-accent); }` at `.btn`'s own
padding and radius (`rounded-medium`, the client's standard button box). So the two actions that
commit a password and an address render smaller and quieter than the ghost controls beside them
will once `TASK-121003` lands.

**This is a divergence, not an absence**, which is why it is `medium` and not part of the `high`
ticket. The buttons are visibly buttons; they are the wrong component.

## Files

| File | Action |
| --- | --- |
| `web-client/src/account/SignUpForm.tsx` | modify |
| `web-client/src/account/RecoveryEmailForm.tsx` | modify |
| `web-client/src/account/AccountScreen.test.tsx` | modify |

## Scope

- **Dress both submits as the card's `.btn.fill`**: `bg-accent-fill` and `text-on-accent` at the
  client's standard button padding and radius — the same treatment `TASK-121005` gives the sign-in
  submit and `TASK-121004` gives the front door's primary. One vocabulary across every primary
  button in the product.
- Structure and behaviour stay exactly as they are: same elements, same order, same handlers, same
  strings, same disabled and pending logic.
- No new token, no new value, no arbitrary length literal (`ADR-0091` §4's fourth client guard
  refuses `-[380px]`; `-[var(--pd-…)]` passes).

## Out of scope

- **The fields.** Both forms' inputs carry `w-full rounded-small border border-hairline px-3 py-2`,
  which the card draws as `.field .box` with a surface. Not reported this round and not filed on
  a guess; if a later round measures it, it is that round's finding.
- **Every string.** `account-text.ts` owns them. **Change no literal.**
- **When either form appears.** That a claimed profile is still offered the claim form is
  `TASK-120601`; that *Attach a recovery address* asks for a *Current password* on a device that has
  none is `DEC-090`, the product owner's, raised at round 1 and unanswered. This ticket changes how
  a control looks and never which ones appear.
- **`Sign in` and `Sign out`.** Unclassed, `high`, `TASK-121003` — which this ticket depends on so
  two coders never hold the account screen at once.
- **`SignInForm.tsx`'s submit.** Same wrong component, different screen and card: `TASK-121005`.

## Tests

`AccountScreen.test.tsx` — one test here rather than one in each form's own file, because
`AccountScreen` renders both forms and a single assertion over the rendered tree cannot be satisfied
by dressing one of the two.

| Test | Proves |
| --- | --- |
| `both account forms submit with the card's fill button` | with a profile that shows the sign-up form and one that shows the recovery form, **each** submit's class list contains `bg-accent-fill` and `text-on-accent` — two named tokens, two controls, four assertions. Named tokens rather than non-emptiness, because both buttons already carry a class and a non-empty check passes today |

**Why the `verify:` block runs three suites unpiped.** `--reporter=verbose` prints a test's name
whether it passed or failed, and the exit code of a piped run is `grep`'s, not the suite's. The grep
proves the named test **exists**; the second command runs the account screen's suite plus both
forms' own suites with **no pipe**, so its exit code is theirs and proves a dressing change did not
break their merged behaviour tests. `NO_COLOR=1` is set because ANSI escapes break a fixed-string
grep.

## Acceptance criteria

- [ ] `AccountScreen.test.tsx > both account forms submit with the card's fill button` passes, and
      asserts both controls rather than one
- [ ] Reverting either `SignUpForm.tsx` or `RecoveryEmailForm.tsx` alone reddens it
- [ ] Every merged test in `SignUpForm.test.tsx` and `RecoveryEmailForm.test.tsx` still passes
- [ ] **By hand, on a live stack** — press *Account* and see both submits as filled accent buttons
      the size of every other primary button in the product
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
