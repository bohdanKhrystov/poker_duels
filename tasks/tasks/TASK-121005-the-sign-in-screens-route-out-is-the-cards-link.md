---
schema: 2
id: TASK-121005
title: The `sign-in` screen's route out is the card's link, and its submit is the card's fill button
type: task
status: backlog
parent: STORY-1210
module: web-client
estimate: S
tier: sonnet
review: standard
files_touched: 3
labels: [qa, uat, bug, medium]
depends_on: [TASK-121004]
verify:
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose src/lobby/Lobby.test.tsx 2>&1 | grep -qF "the forgotten-password route out is a link, not body text"
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose src/account/SignInForm.test.tsx 2>&1 | grep -qF "the sign-in submit is the card's fill button"
  - cd web-client && NO_COLOR=1 npm run --silent test -- src/lobby/Lobby.test.tsx src/account/SignInForm.test.tsx
  - cd web-client && NO_COLOR=1 npm run --silent check
  - python3 .github/scripts/lint_tickets.py
---

## Goal

The two controls `design/screens/sign-in.html` draws — the *Sign in* submit and the
*Forgot your password?* route out — carry the treatments the card gives them, so the way to recover
a password is visibly a way and not a sentence.

## The defect

Round 2 of `/qa-cycle uat regression`, 2026-08-30, commit `07df9e7f`, against
`design/screens/sign-in.html` — **merged the day before, by `TASK-120905`, and the newest merged
source for this screen.**

**The card's own prediction came true, and not in the way the card expected.** Its margin note reads:

> Forgot your password? is FORGOT_PASSWORD_LABEL (recovery-text.ts) — **SignInForm.tsx does not
> render it yet**, so this card draws the gap the next UAT round should catch.

The control **does** render, and always did — from `Lobby.tsx:387–389`'s `SignInScreenBody`, not from
`SignInForm.tsx`. `grep -rn 'FORGOT_PASSWORD_LABEL' web-client/src` returns `Lobby.tsx` and no other
component. So the card was right to draw the control and wrong about where it lives; correcting the
note is `TASK-121007`'s, and the gap that *is* real is this one:

```
<button type="button">Forgot your password?</button>
{"cls":null,"color":"rgb(236, 233, 227)","padding":"0px","fontSize":"15px"}
```

Card, `sign-in.html:71, 102`: `class="link"`, and
`.link { … color: var(--pd-accent); font-size: var(--pd-fs-small); … cursor: pointer; }`. As shipped
it is the body colour at body size with no cursor — indistinguishable from the copy around it.

**And the submit is the wrong component.** `SignInForm.tsx:105` renders
`rounded-small border border-hairline bg-surface px-4 py-2 text-small` where the card draws
`class="btn fill"` — `.btn.fill { background: var(--pd-accent-fill); color: var(--pd-on-accent); }`
at the card's own padding and radius. This is a divergence, not an absence: the control is visibly a
button, just a smaller, quieter one than the card's primary.

**Why `medium` and not `high`, stated because the neighbouring ticket is `high`.** The card's three
frames are transcribed — heading, both field pairs, and the refusal frame (`role="status"`, above
the fields, fields keeping typed values) which the round-2 report confirms matched *exactly*. The
screen's primary control is a visible button. What diverges is two specific elements inside a
transcribed screen, which is the `medium` row of the line `STORY-1209` set and `STORY-1210` applied
unchanged. `TASK-121003` is `high` because none of the `account` card's four buttons reached the
client at all.

## Files

| File | Action |
| --- | --- |
| `web-client/src/lobby/Lobby.tsx` | modify |
| `web-client/src/account/SignInForm.tsx` | modify |
| `web-client/src/lobby/Lobby.test.tsx` | modify |

## Scope

- **Dress *Forgot your password?* as the card's `.link`**: the accent colour and the small font size,
  with a pointer cursor. It stays a `<button>` — there is no URL for an anchor to carry, and an
  anchor without one is a lie to the keyboard and to a screen reader (`ADR-0060` §2's reasoning,
  applied to the same shape).
- **Dress the *Sign in* submit as the card's `.btn.fill`**: `bg-accent-fill` and `text-on-accent` at
  the client's standard button padding and radius, the same treatment `TASK-121004` gives the front
  door's primary.
- Structure and behaviour stay exactly as they are: same elements, same order, same handlers, same
  strings.
- No new token, no new value, no arbitrary length literal (`ADR-0091` §4's fourth client guard
  refuses `-[380px]`; `-[var(--pd-…)]` passes).

## Out of scope

- **Every string on this screen.** `SIGN_IN_HEADING`, `SIGN_IN_LABEL`, `HANDLE_LABEL`,
  `PASSWORD_LABEL`, `SIGN_IN_REFUSED` and `FORGOT_PASSWORD_LABEL` are owned by `account-text.ts` and
  `recovery-text.ts`. **Change no literal.**
- **The card's margin note.** It is wrong about which component renders the control. Correcting a
  card is a design ticket: `TASK-121007`.
- **Where *Back* goes.** Whether the sign-in screen's *Back* returns to the account screen or the
  lobby is `DEC-091`, the product owner's, raised at round 1 and unanswered. Do not change
  navigation.
- **`Back`'s own treatment.** Rendered by `Lobby.tsx`'s swap, and **no card draws it** — it
  contradicts nothing and is deliberately not filed (`STORY-1210` §*What was not filed*).
- **The front door.** Same file, different card: `TASK-121004`, which this ticket depends on so two
  coders never hold `Lobby.tsx` at once.
- **The account screen's two form submits.** Same wrong component, different screen and card:
  `TASK-121006`.

## Tests

| File | Test | Proves |
| --- | --- | --- |
| `Lobby.test.tsx` | `the forgotten-password route out is a link, not body text` | on the sign-in screen, the *Forgot your password?* button's class list contains an accent **colour** utility and a small-font utility — two named tokens, because an accent-coloured control at body size is still not the card's `.link` |
| `SignInForm.test.tsx` | `the sign-in submit is the card's fill button` | the submit's class list contains **both** `bg-accent-fill` and `text-on-accent` — named tokens, not merely non-empty, since the button already carries a class and a non-empty check would pass today |

**The second gate must name tokens, not non-emptiness.** This is the exact shape that let
`TASK-120901`'s repair ship a ghost where the card drew a fill: the control had *a* class, so a
non-empty assertion was green.

**Why the `verify:` block runs both suites unpiped.** `--reporter=verbose` prints a test's name
whether it passed or failed, and the exit code of a piped run is `grep`'s, not the suite's. The two
greps prove the named tests **exist**; the third command runs both files with **no pipe**, so its
exit code is theirs and proves they **pass**. `NO_COLOR=1` is set because ANSI escapes break a
fixed-string grep.

## Acceptance criteria

- [ ] `Lobby.test.tsx > the forgotten-password route out is a link, not body text` passes
- [ ] `SignInForm.test.tsx > the sign-in submit is the card's fill button` passes
- [ ] Reverting `Lobby.tsx` and `SignInForm.tsx` reddens both
- [ ] Every merged test in `SignInForm.test.tsx` and `Lobby.test.tsx` still passes
- [ ] **By hand, on a live stack** — press *Account*, then *Sign in*, and see a filled submit and an
      accent-coloured *Forgot your password?* that reads as a link rather than as a sentence
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
