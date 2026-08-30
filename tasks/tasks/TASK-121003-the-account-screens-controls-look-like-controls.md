---
schema: 2
id: TASK-121003
title: The `account` screen's *Sign in* and *Sign out* are the card's buttons, not sentences
type: task
status: ready
parent: STORY-1210
module: web-client
estimate: S
tier: sonnet
review: standard
files_touched: 3
labels: [qa, uat, bug, high]
depends_on: []
verify:
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose src/account/AccountScreen.test.tsx 2>&1 | grep -qF "the sign-in door is a drawn button, not a sentence"
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose src/account/AccountScreen.test.tsx 2>&1 | grep -qF "sign out and its confirmation are drawn buttons, not sentences"
  - cd web-client && NO_COLOR=1 npm run --silent test -- src/account/AccountScreen.test.tsx src/account/SignOutControl.test.tsx
  - cd web-client && NO_COLOR=1 npm run --silent check
  - python3 .github/scripts/lint_tickets.py
---

## Goal

The two controls the `account` screen exists for — reaching an account you own, and ending a session
on a browser someone else can reach — are visibly controls, so a player can tell them from the
sentences printed above them.

## The defect

Round 2 of `/qa-cycle uat regression`, 2026-08-30, commit `07df9e7f`, against
`design/screens/account.html` — **merged the day before, by `TASK-120904`, and the newest merged
source for this screen.** Read from the running client, not from the report.

**The card's `.btn` vocabulary reached none of the screen's four buttons.** The card draws
*Give this profile a password* and *Attach a recovery address* as `.btn.fill` (lines 92, 129) and
*Sign in* and *Sign out* as `.btn.ghost` (lines 94, 112). Shipped:

```
BUTTON "Sign in"                     getAttribute("class") → null   bg rgba(0,0,0,0)  pad 0px  radius 0px  border 0px
BUTTON "Sign out"                    getAttribute("class") → null   bg rgba(0,0,0,0)  pad 0px  radius 0px  border 0px
BUTTON "Give this profile a password" cls "rounded-small border border-hairline bg-surface px-4 py-2 text-small"
BUTTON "Attach a recovery address"    cls "rounded-small border border-hairline bg-surface px-4 py-2 text-small"
```

Two carry no class at all; two carry a smaller component the card does not define. `.btn` is three of
the card's own CSS rules and every control it draws uses one.

**The two unclassed buttons are typographically identical to the screen's prose.** Measured on the
live client, not eyeballed:

```
Sign out (button) : 15px / 400 / rgb(236,233,227) / center / -apple-system / cursor: default
Back     (button) : 15px / 400 / rgb(236,233,227) / center / -apple-system / cursor: default
a paragraph       : 13px / 400 / rgb(236,233,227) / center / -apple-system / cursor: auto
```

Not a border, not a background, not a weight, not a colour, not even a pointer cursor separates the
two. A capture at 756 × 469 — above the ~500 px width floor where headless shots clip, so
`ADR-0092` §2's harness-defect test is not in play — shows *Sign out* reading as one more centred
sentence under *Your password signs in to this account.* `UAT-Q3`, *are all options accessible?*, is
the standing question this fails against.

**The consequence is on the screen's own copy.** `AccountScreen.tsx` prints, verbatim: *"Your
password is asked for here because a browser someone else reaches would otherwise become permanent
ownership of this account."* *Sign out* is the control that answers that sentence, and it does not
look like one.

**Source, confirming the DOM read.** `AccountScreen.tsx:126` and `SignOutControl.tsx:39, 42, 50`
render those buttons with **no `className` prop at all**. This is an empty class list, not a class
present and overridden — the panel around them (`AccountScreen.tsx:111`) computes correctly.

## Files

| File | Action |
| --- | --- |
| `web-client/src/account/AccountScreen.tsx` | modify |
| `web-client/src/account/SignOutControl.tsx` | modify |
| `web-client/src/account/AccountScreen.test.tsx` | modify |

## Scope

- **Dress *Sign in*** (`AccountScreen.tsx`) as the card's `.btn.ghost`: a `border-hairline` border,
  the client's standard button padding and radius, and the body text colour — the same treatment
  `Lobby.tsx`'s *Back to the lobby* link already carries.
- **Dress all three of `SignOutControl`'s buttons** — the offered *Sign out*, the confirming
  *Sign out*, and *Cancel*. All three, because a player who reaches the confirmation step meets two
  more undressed controls.
- Structure and behaviour stay exactly as they are: same elements, same order, same handlers, same
  strings, and `SignOutControl`'s two-step `offered`/`confirming` shape is untouched.
- No new token, no new value, no arbitrary length literal (`ADR-0091` §4's fourth client guard
  refuses `-[380px]`; `-[var(--pd-…)]` passes).

## Out of scope

- **The two form submits.** *Give this profile a password* and *Attach a recovery address* carry a
  class — the wrong, smaller one. That is a component-choice divergence, graded `medium`, and it is
  `TASK-121006`. Do not touch `SignUpForm.tsx` or `RecoveryEmailForm.tsx` here; a `high` ticket that
  drags a `medium` in has widened scope.
- **Every string on this screen.** `ACCOUNT_HEADING`, `SIGN_IN_HEADING`, `SIGN_OUT_LABEL`, `CANCEL`,
  `SIGN_OUT_WARNING` and the device lines are `account-text.ts`'s. **Change no literal.**
- **`Back`.** Rendered by `Lobby.tsx`'s swap, not by this screen (`ADR-0060` §4), and **no card
  draws it** — it contradicts nothing and is deliberately not filed (`STORY-1210` §*What was not
  filed*).
- **What the screen decides to show.** That a claimed profile is still offered the claim form is
  `TASK-120601`, open, and its second half needs `ADR-0050` §4 overturned by the `architect`. This
  ticket changes how controls look and never which ones appear.
- **The duels and leaderboard screens.** Same cause, different files: `TASK-121001`, `TASK-121002`.

## Tests

`AccountScreen.test.tsx` — both tests live here rather than split across two files, because
`AccountScreen` renders `SignOutControl` and asserting the rendered tree is what a player meets.

| Test | Proves |
| --- | --- |
| `the sign-in door is a drawn button, not a sentence` | with `signedIn={false}`, the *Sign in* button's class list contains **both** `border-hairline` and a padding utility — named tokens, not merely non-empty, so the gate cannot be satisfied by any class at all |
| `sign out and its confirmation are drawn buttons, not sentences` | with `signedIn={true}` and a `signOut` supplied, the offered *Sign out* carries the same pair; then, after pressing it, **both** the confirming *Sign out* and *Cancel* do too — three separate assertions, because one dressed control standing in for the others is exactly the failure `TASK-120901` shipped |

**The second test presses through the two-step control on purpose.** `SignOutControl` renders one
button before confirmation and two after, and a test that stopped at the first would leave two
undressed controls behind a gate that passed.

**Why the `verify:` block runs the suite unpiped.** `--reporter=verbose` prints a test's name whether
it passed or failed, and the exit code of a piped run is `grep`'s, not the suite's. The two greps
prove the named tests **exist**; the third command runs both account suites with **no pipe**, so its
exit code is theirs and proves they **pass**. `SignOutControl.test.tsx` is run too, unchanged, so a
dressing change that broke its merged behaviour tests cannot land. `NO_COLOR=1` is set because ANSI
escapes break a fixed-string grep.

## Acceptance criteria

- [ ] `AccountScreen.test.tsx > the sign-in door is a drawn button, not a sentence` passes
- [ ] `AccountScreen.test.tsx > sign out and its confirmation are drawn buttons, not sentences`
      passes, and asserts all three of `SignOutControl`'s buttons
- [ ] Reverting `AccountScreen.tsx` and `SignOutControl.tsx` reddens both; the reviewer runs that
      rather than reading it
- [ ] Every merged test in `SignOutControl.test.tsx` still passes
- [ ] **By hand, on a live stack** — press *Account* and see a bordered button where *Sign in* or
      *Sign out* is, distinguishable at a glance from the sentences above it
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
