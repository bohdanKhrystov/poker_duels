---
schema: 2
id: TASK-121105
title: The account screen's field labels are the card's left-aligned muted labels
type: task
status: backlog
parent: STORY-1211
module: web-client
estimate: XS
tier: sonnet
review: standard
files_touched: 3
labels: [qa, uat, bug, low]
depends_on: [TASK-121006]
verify:
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose src/account/AccountScreen.test.tsx 2>&1 | grep -qF "both account forms left-align their fields and mute their labels"
  - cd web-client && NO_COLOR=1 npm run --silent test -- src/account/AccountScreen.test.tsx src/account/SignUpForm.test.tsx src/account/RecoveryEmailForm.test.tsx
  - cd web-client && NO_COLOR=1 npm run --silent check
  - python3 .github/scripts/lint_tickets.py
---

## Goal

Every field label on the `account` screen reads as the card's label — left-aligned and muted —
instead of inheriting the panel's centring at full brightness.

## The defect

`design/screens/account.html` centres the **panel** and then left-aligns the **fields** inside it:

```
.field     { display: flex; flex-direction: column; gap: var(--pd-space-2); text-align: left; }
.field .lbl { font-size: var(--pd-fs-small); color: var(--pd-text-muted); }
```

Shipped, `SignUpForm.tsx:96` and `RecoveryEmailForm.tsx:92` put `text-center` on the whole panel and
add nothing to undo it on the field block, and every label is bare `text-small` with no muted colour
— computing `textAlign: center, color: rgb(236,233,227)`. A centred label sits over the middle of the
box it names rather than at its edge, and at full brightness it competes with the value typed under
it.

**Why `low`.** The label is still a legible label naming a field the player can fill; nothing is
unreachable and nothing is misread. The anchor is `TASK-121006`, this round's sibling: round 2 graded
a *primary submit rendered as the wrong component* `medium`, and an alignment-and-colour step on a
label cannot outrank it.

## Files

| File | Action |
| --- | --- |
| `web-client/src/account/SignUpForm.tsx` | edit |
| `web-client/src/account/RecoveryEmailForm.tsx` | edit |
| `web-client/src/account/AccountScreen.test.tsx` | edit |

## Scope

- **Left-align the field block**, not the panel. The card centres the panel deliberately — the
  heading, the lede and the buttons stay centred; only `.field` turns. Add `text-left` on the wrapper
  that holds a label and its input.
- **Every field label carries `text-text-muted`** alongside its `text-small`.
- Both forms, because the card's `.field` rule governs both, and one dressed form cannot carry the
  assertion.

## Out of scope

- **The two submit buttons.** `TASK-121006` owns them, is still open, and is this ticket's
  `depends_on` so two coders never hold `AccountScreen`'s tree at once.
- **`Sign in` and `Sign out`.** `TASK-121003` fixed both; round 3 confirms them dressed.
- **Every string.** `account-text.ts` owns them. **Change no literal.**
- **Whether `Attach a recovery address` is offered at all on a passwordless device.** That is
  `DEC-090`, open, the product owner's.

## Tests

`AccountScreen.test.tsx` — one case over the whole screen, the shape `TASK-121006` used for the same
two forms, so a fix applied to one form and not the other cannot pass.

| Test | Proves |
| --- | --- |
| `both account forms left-align their fields and mute their labels` | on the rendered account screen: the wrapper holding *Handle*/*Password* **and** the wrapper holding the recovery address each carry `text-left`, and **every** `<label>` inside those wrappers carries `text-text-muted`. Enumerated over all of them, not asserted of the first — a universal name is a promise to check every one |

## Acceptance criteria

- [ ] `AccountScreen.test.tsx > both account forms left-align their fields and mute their labels` passes
- [ ] The panel itself still carries `text-center`, so the heading and buttons are unmoved
- [ ] Every command in `verify:` exits 0
