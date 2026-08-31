---
schema: 2
id: TASK-121005
title: The `sign-in` form's submit is the card's fill button, not a smaller one
type: task
status: ready
parent: STORY-1210
module: web-client
estimate: XS
tier: haiku
review: light
files_touched: 2
labels: [qa, uat, bug, medium]
depends_on: []
verify:
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose src/account/SignInForm.test.tsx 2>&1 | grep -qF "the sign-in submit is the card's fill button"
  - cd web-client && NO_COLOR=1 npm run --silent test -- src/account/SignInForm.test.tsx
  - cd web-client && NO_COLOR=1 npm run --silent check
  - python3 .github/scripts/lint_tickets.py
---

## Goal

The *Sign in* submit carries the treatment `design/screens/sign-in.html` draws for it, so this
screen's committing action looks like every other primary button in the product.

## The defect

Round 2 of `/qa-cycle uat regression`, 2026-08-30, commit `07df9e7f`, against
`design/screens/sign-in.html` — merged the day before, by `TASK-120905`, and the newest merged
source for this screen.

**The submit is the wrong component.** `SignInForm.tsx:105` renders

```
rounded-small border border-hairline bg-surface px-4 py-2 text-small
```

where the card draws `class="btn fill"` — `sign-in.html:54–58`:

```
.btn      { … padding: var(--pd-space-4) var(--pd-space-5);
            border-radius: var(--pd-radius-medium); font-weight: 500; … }
.btn.fill { background: var(--pd-accent-fill); color: var(--pd-on-accent); }
```

This is a divergence, not an absence: the control is visibly a button, just a smaller, quieter one
than the card's primary. `TASK-121006` has since landed exactly that recipe on the account screen's
two submits, so the vocabulary already exists in the client and this ticket adopts it rather than
inventing a second one.

**Why `medium`, and not re-graded here.** Round 2's grade, held. The card's three frames are
transcribed — heading, both field pairs, and the refusal frame (`role="status"`, above the fields,
fields keeping typed values) which the round-2 report confirmed matched *exactly*. What diverges is
one element inside a transcribed screen. A round's severities are its own; this ticket restates one,
it does not set one.

## Why this ticket is one of two

`TASK-121005` was filed at round 2's triage covering **both** of this screen's undressed controls —
this submit and the *Forgot your password?* route out. As written it could not be worked: its
`verify:` block required a new test in `SignInForm.test.tsx` while its `## Files` table named
`Lobby.tsx`, `SignInForm.tsx` and `Lobby.test.tsx` and not that test file, so its own definition of
done — every `verify:` command exits 0 — was unreachable inside its own scope. A coder took it and
blocked before writing code, correctly.

A fourth file is not the repair. `ADR-0068` caps a ticket at three, and `atomic:` buys an exemption
only by naming a **merged gate** that fails on the smaller commit. There is none: the two controls
live in different components with independent suites, and either half is green on its own. So the
ticket splits, `TASK-121005` keeps the submit — the half `TASK-121006`, `TASK-121106` and
`STORY-1211` already point at by this id — and the route out becomes `TASK-121010`, round 2's
finding unchanged and neither more nor less than the other half of this one.

**The round's finding set does not move.** One finding, still `medium`, still the same two controls
on one screen (`STORY-1210` §Tasks, amended 2026-08-31). `EPIC-12` §Termination rule 1 freezes what
a round may *repair*; neither half is scheduled by this cycle, and nothing here re-scopes, re-grades
or adds a defect round 2 did not report. The `depends_on` edge onto `TASK-121004` goes with the
half that touches `Lobby.tsx`, because that edge only ever existed to stop two coders holding that
file at once.

## Files

| File | Action |
| --- | --- |
| `web-client/src/account/SignInForm.tsx` | modify |
| `web-client/src/account/SignInForm.test.tsx` | modify |

## Scope

- **Dress the submit as the card's `.btn.fill`.** Replace the one `className` at
  `SignInForm.tsx:105` with the recipe `TASK-121006` merged on the account screen's two submits —
  copied character for character from `SignUpForm.tsx:128` and `RecoveryEmailForm.tsx:128`, which
  ship it identically:

  ```
  rounded-medium border border-transparent bg-accent-fill px-5 py-4 leading-tight font-medium text-on-accent
  ```

  One vocabulary across every primary button in the product, and no new recipe invented for the
  third one.
- Structure and behaviour stay exactly as they are: same elements, same order, same handlers, same
  strings, `disabled={isSubmitting}` untouched.
- One new test in `SignInForm.test.tsx`, named exactly as §Tests names it.
- No new token, no new value, no arbitrary length literal (`ADR-0091` §4's fourth client guard
  refuses `-[380px]`; `-[var(--pd-…)]` passes).

## Out of scope

- **`Forgot your password?`.** The other half of round 2's finding, in another component:
  `TASK-121010`, which owns `Lobby.tsx`. Nothing in `Lobby.tsx` is this ticket's.
- **The two field labels.** Centred and full-bright where the card draws them left-aligned and
  muted. That is **round 3's** finding, filed as `TASK-121106`, which carries a `depends_on` onto
  this ticket so two coders never hold `SignInForm.tsx` at once. Fixing them here would make round
  3's record wrong about what round 3 found (`EPIC-12` §Termination rule 1). Leave both `<label>`
  elements and the panel's `text-center` at `SignInForm.tsx:69` exactly as they are.
- **The refusal box.** Round 2 confirmed it matches the card exactly — `role="status"`, above the
  fields, values retained. Do not touch it.
- **The two inputs.** They carry `w-full rounded-small border border-hairline px-3 py-2` where the
  card draws `.field .box` with a surface. Not reported this round and not filed on a guess — the
  same call `TASK-121006` made for the account screen's fields.
- **Every string.** `account-text.ts` owns `SIGN_IN_LABEL`, `HANDLE_LABEL`, `PASSWORD_LABEL` and
  `SIGN_IN_REFUSED`. **Change no literal.**
- **The account screen's two submits.** Already the card's fill button: `TASK-121006`, merged.
- **`ForgotPasswordForm.tsx`'s two controls.** They carry the same quiet recipe this ticket replaces
  here. They are a different screen state, they are not in the Files table, and no ticket either
  round filed names that file — so they are not this ticket's to fix, on a guess or otherwise. If a
  later round measures them, it is that round's finding.

## Tests

`SignInForm.test.tsx` — the file already holds a `submitButton()` helper
(`screen.getByRole("button", { name: SIGN_IN_LABEL })`). Use it rather than adding a second query.

| Test | Proves |
| --- | --- |
| `the sign-in submit is the card's fill button` | the submit's class list **contains** `bg-accent-fill`, **contains** `text-on-accent`, and **does not contain** `bg-surface` — three membership checks against named tokens, the third pinning that the quiet recipe went away rather than gained a neighbour. Not a non-emptiness check and not a text query: the control already carries a class and already reads *Sign in*, so both are green before this change and after it |

**Why the `verify:` block runs the suite unpiped.** `--reporter=verbose` prints a test's name whether
it passed or failed, and the exit code of a piped run is `grep`'s, not the suite's. The grep proves
the named test **exists**; the second command runs the file with **no pipe**, so its exit code is
the suite's and proves it **passes**. `NO_COLOR=1` is set because ANSI escapes break a fixed-string
grep.

## Acceptance criteria

- [ ] `SignInForm.test.tsx > the sign-in submit is the card's fill button` passes, and makes all
      three membership assertions §Tests names
- [ ] Reverting the one `className` at `SignInForm.tsx:105` reddens that test
- [ ] Every merged test in `SignInForm.test.tsx` — seven cases — still passes, unedited: none of
      them asserts a class, so no assertion moves
- [ ] **By hand, on a live stack** — press *Account*, then *Sign in*, and see a filled accent submit
      the size of every other primary button in the product
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
