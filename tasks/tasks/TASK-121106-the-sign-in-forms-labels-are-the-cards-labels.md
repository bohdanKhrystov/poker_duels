---
schema: 2
id: TASK-121106
title: The sign-in screen's field labels are the card's left-aligned muted labels
type: task
status: backlog
parent: STORY-1211
module: web-client
estimate: XS
tier: sonnet
review: standard
files_touched: 2
labels: [qa, uat, bug, low]
depends_on: [TASK-121005]
verify:
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose src/account/SignInForm.test.tsx 2>&1 | grep -qF "the sign-in fields are left-aligned and their labels are muted"
  - cd web-client && NO_COLOR=1 npm run --silent test -- src/account/SignInForm.test.tsx
  - cd web-client && NO_COLOR=1 npm run --silent check
  - python3 .github/scripts/lint_tickets.py
---

## Goal

The `sign-in` form's two field labels read as the card's labels — left-aligned and muted — instead of
inheriting the panel's centring at full brightness.

## The defect

`design/screens/sign-in.html` carries the same pair of rules `account.html` does:

```
.field       { display: flex; flex-direction: column; gap: var(--pd-space-2); text-align: left; }
.field label { font-size: var(--pd-fs-small); color: var(--pd-text-muted); }
```

Shipped, `SignInForm.tsx:69` puts `text-center` on the panel and `:82,:92` leave both labels at bare
`text-small`, computing `textAlign: center, color: rgb(236,233,227)`.

**Why `low`, and why it is its own ticket.** Same reasoning as `TASK-121105`: a legible label in the
wrong place and the wrong step, below round 2's `medium` for a submit rendered as the wrong
component. It is **not** folded into `TASK-121005` because that ticket was filed and frozen at round
2's triage — `EPIC-12` §Termination rule 1 fixes a round's set when it is triaged, and editing a
merged, open ticket's scope a round later rewrites the trail rather than extending it. It carries a
`depends_on` onto `TASK-121005` instead, which is the round-2 convention for two tickets on one file.

## Files

| File | Action |
| --- | --- |
| `web-client/src/account/SignInForm.tsx` | edit |
| `web-client/src/account/SignInForm.test.tsx` | edit |

## Scope

- **Left-align the field block**, not the panel: add `text-left` on the wrapper holding a label and
  its input, and leave the panel's `text-center` alone so the heading and the refusal box do not move.
- **Both labels carry `text-text-muted`** alongside their `text-small`.

## Out of scope

- **The submit button.** `TASK-121005` owns it, is still open, and is this ticket's `depends_on` —
  the same file, held by one coder at a time.
- **`Forgot your password?`.** `TASK-121010`, in `Lobby.tsx`. It and `TASK-121005` were one ticket
  until the 2026-08-31 split, which is why this ticket's `depends_on` names only the half that
  touches `SignInForm.tsx`.
- **The refusal box.** Round 2 confirmed it matches the card exactly — `role="status"`, above the
  fields, values retained. Do not touch it.
- **Where `Back` returns to.** That is `DEC-091`, open, the product owner's.
- **Every string.** **Change no literal.**

## Tests

`SignInForm.test.tsx`

| Test | Proves |
| --- | --- |
| `the sign-in fields are left-aligned and their labels are muted` | the wrapper holding *Handle* and *Password* carries `text-left`, and **both** labels carry `text-text-muted`; the panel still carries `text-center`, so the fix is the field block and not a blanket un-centring |

## Acceptance criteria

- [ ] `SignInForm.test.tsx > the sign-in fields are left-aligned and their labels are muted` passes
- [ ] Every command in `verify:` exits 0
