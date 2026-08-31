---
schema: 2
id: TASK-121010
title: The `sign-in` screen's route out is the card's link, not body text
type: task
status: done
parent: STORY-1210
module: web-client
estimate: XS
tier: haiku
review: light
files_touched: 2
labels: [qa, uat, bug, medium]
depends_on: [TASK-121004]
verify:
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose src/lobby/Lobby.test.tsx 2>&1 | grep -qF "the forgotten-password route out is a link, not body text"
  - cd web-client && NO_COLOR=1 npm run --silent test -- src/lobby/Lobby.test.tsx
  - cd web-client && NO_COLOR=1 npm run --silent check
  - python3 .github/scripts/lint_tickets.py
---

## Goal

*Forgot your password?* is drawn as the card's `.link` — accent-coloured, small, with a pointer
cursor — so the way to recover a password is visibly a way and not a sentence.

## The defect

Round 2 of `/qa-cycle uat regression`, 2026-08-30, commit `07df9e7f`, against
`design/screens/sign-in.html` — merged the day before, by `TASK-120905`, and the newest merged
source for this screen. What the round read off the live screen:

```
<button type="button">Forgot your password?</button>
{"cls":null,"color":"rgb(236, 233, 227)","padding":"0px","fontSize":"15px"}
```

Card, `sign-in.html:71–72`: `class="link"`, and

```
.link { align-self: flex-start; background: none; border: none; padding: 0; cursor: pointer;
  font-family: var(--pd-font-ui); font-size: var(--pd-fs-small); color: var(--pd-accent); }
```

As shipped it is the body colour at body size with no cursor — indistinguishable from the copy
around it. The round measured `padding: 0px` on this element and the card's rule asks for
`padding: 0`, so **what is missing is colour, size and cursor**, and adding those three is the whole
change.

**Where the control lives.** `Lobby.tsx:387–389`, inside `SignInScreenBody`;
`grep -rn 'FORGOT_PASSWORD_LABEL' web-client/src` returns `Lobby.tsx` — the import at line 33, the
use at line 388 — and no other component. `sign-in.html`'s margin note about this control is
`TASK-121007`'s to correct, and correcting a card is a design ticket, never a client one.

**Why `medium`, and not re-graded here.** Round 2's grade, held. The card's three frames are
transcribed, and this is a secondary route out diverging inside a transcribed screen. A round's
severities are its own; this ticket restates one, it does not set one.

## Why this ticket is one of two

`TASK-121005` was filed at round 2's triage covering **both** of this screen's undressed controls —
this route out and the *Sign in* submit. As written it could not be worked: its `verify:` block
required a new test in `SignInForm.test.tsx` while its `## Files` table named `Lobby.tsx`,
`SignInForm.tsx` and `Lobby.test.tsx` and not that test file, so its own definition of done — every
`verify:` command exits 0 — was unreachable inside its own scope. A coder took it and blocked before
writing code, correctly.

A fourth file is not the repair. `ADR-0068` caps a ticket at three, and `atomic:` buys an exemption
only by naming a **merged gate** that fails on the smaller commit. There is none: the two controls
live in different components with independent suites, and either half is green on its own. So the
ticket splits. `TASK-121005` keeps the submit, because `TASK-121006`, `TASK-121106` and `STORY-1211`
already point at that half by that id, and this ticket takes the route out with round 2's finding
unchanged.

**The round's finding set does not move.** One finding, still `medium`, still the same two controls
on one screen (`STORY-1210` §Tasks, amended 2026-08-31). `EPIC-12` §Termination rule 1 freezes what
a round may *repair*; neither half is scheduled by this cycle, and nothing here re-scopes, re-grades
or adds a defect round 2 did not report. This half keeps the `depends_on` onto `TASK-121004`,
because that edge only ever existed to stop two coders holding `Lobby.tsx` at once.

## Files

| File | Action |
| --- | --- |
| `web-client/src/lobby/Lobby.tsx` | modify |
| `web-client/src/lobby/Lobby.test.tsx` | modify |

## Scope

- **Dress the button at `Lobby.tsx:387` as the card's `.link`**: add a `className` carrying the
  accent colour, the small size and the pointer cursor — `text-accent text-small cursor-pointer`.
  Those are the three properties the card's rule has that preflight does not already give the
  element.
- **It stays a `<button type="button">` with the same `onClick`.** There is no URL for an anchor to
  carry, and an anchor without one is a lie to the keyboard and to a screen reader (`ADR-0060` §2's
  reasoning, applied to the same shape). The card says the same thing in the comment above `.link`.
- Structure and behaviour stay exactly as they are: same element, same position under the form,
  same handler, same string.
- One new test in `Lobby.test.tsx`, named exactly as §Tests names it.
- No new token, no new value, no arbitrary length literal (`ADR-0091` §4's fourth client guard
  refuses `-[380px]`; `-[var(--pd-…)]` passes).

## Out of scope

- **The `sign-in` submit.** The other half of round 2's finding, in another component:
  `TASK-121005`, which owns `SignInForm.tsx`. Nothing in `SignInForm.tsx` is this ticket's.
- **The two sign-in field labels.** Centred and full-bright where the card draws them left-aligned
  and muted. That is **round 3's** finding, filed as `TASK-121106` against `SignInForm.tsx` — a
  different file and a different round. Fixing them here would make round 3's record wrong about
  what round 3 found (`EPIC-12` §Termination rule 1).
- **The control's alignment.** `.link` also carries `align-self: flex-start`, which positions it
  inside the card's own column. The round reported colour, size and cursor; alignment was not filed
  and is not guessed at here.
- **The card.** `TASK-121007` owns `sign-in.html`. Change no drawing and no margin note.
- **`ForgotPasswordForm`.** This ticket dresses the control that opens the recovery form, never the
  form it opens.
- **`Back`'s own treatment.** Rendered by `Lobby.tsx`'s swap, and **no card draws it** — it
  contradicts nothing and is deliberately not filed (`STORY-1210` §*What was not filed*).
- **Where `Back` goes.** Whether the sign-in screen's *Back* returns to the account screen or the
  lobby is `DEC-091`, the product owner's, raised at round 1 and unanswered. Do not change
  navigation.
- **The front door.** Same file, different card: `TASK-121004`, which this ticket depends on so two
  coders never hold `Lobby.tsx` at once.
- **Every string.** `FORGOT_PASSWORD_LABEL` is owned by `recovery-text.ts`. **Change no literal.**

## Tests

`Lobby.test.tsx` — reach the screen the way the merged cases already do:
`window.location.hash = "#/sign-in"`, then `renderLobbyWithAccount(accountCallsFixture())`, then
`screen.getByRole("button", { name: FORGOT_PASSWORD_LABEL })`. Put the new case beside
`offers the way to a forgotten password under the sign-in form, refused or not` (line 1953), the
merged case for the same control.

| Test | Proves |
| --- | --- |
| `the forgotten-password route out is a link, not body text` | the button's class list contains `text-accent`, `text-small` **and** `cursor-pointer` — three membership checks against named tokens: a colour, a size and the cursor, because an accent-coloured control at body size is not the card's `.link` and a small control in body colour is not either. Not a text query and not a non-emptiness check: the control already renders the right words, so finding it by text is green before the change and after it, and *"carries some class"* would be satisfied by any string at all |

**Why the `verify:` block runs the suite unpiped.** `--reporter=verbose` prints a test's name whether
it passed or failed, and the exit code of a piped run is `grep`'s, not the suite's. The grep proves
the named test **exists**; the second command runs the file with **no pipe**, so its exit code is
the suite's and proves it **passes**. `NO_COLOR=1` is set because ANSI escapes break a fixed-string
grep.

## Acceptance criteria

- [ ] `Lobby.test.tsx > the forgotten-password route out is a link, not body text` passes, and makes
      all three membership assertions §Tests names
- [ ] Reverting the one `className` at `Lobby.tsx:387` reddens that test
- [ ] Every merged test in `Lobby.test.tsx` still passes, unedited: the cases that find this control
      do it by role and name and none of them asserts a class, so no assertion moves
- [ ] **By hand, on a live stack** — press *Account*, then *Sign in*, and see an accent-coloured
      *Forgot your password?* that reads as a link rather than as a sentence
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
