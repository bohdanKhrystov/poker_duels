---
schema: 2
id: TASK-120912
title: The result screen's Not now is dressed like the control beside it
type: task
status: done
parent: STORY-1209
module: web-client
estimate: XS
tier: haiku
review: light
files_touched: 2
labels: [qa, uat, bug, low]
depends_on: []
verify:
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose src/result/AccountOffer.test.tsx 2>&1 | grep -qF "both of the offer's controls are dressed"
  - cd web-client && NO_COLOR=1 npm run --silent test -- src/result/AccountOffer.test.tsx
  - cd web-client && NO_COLOR=1 npm run --silent check
  - python3 .github/scripts/lint_tickets.py
---

## Goal

The offer's two controls are dressed as a pair, so *Not now* reads as a control rather than as a
line of text beside a button.

## The defect

Round 1 of `/qa-cycle uat regression`, 2026-08-30, commit `c05ee695`.

`AccountOffer.tsx` dresses one of its two controls and not the other:

    <a class="rounded-medium border border-hairline px-5 py-4 leading-tight font-medium text-text"
       href="/#/account">Keep them with a password</a>
    <button type="button">Not now</button>

The dismissal ships with no `class` at all — the same bare-native-element pattern the lobby has
wholesale (`TASK-120901`), here on one control of a screen that is otherwise transcribed.

## Why `low`

`uat` reported `low`. **Severity unchanged.** It is cosmetic, inside a screen whose card is
otherwise followed, on a control that works. The player can dismiss the offer, and does not lose
anything by its being plain. This is what `low` is for, and it is filed to the backlog and never
scheduled by this cycle.

## Files

| File | Action |
| --- | --- |
| `web-client/src/result/AccountOffer.tsx` | modify |
| `web-client/src/result/AccountOffer.test.tsx` | modify |

## Scope

- **Dress *Not now* from the client's existing token vocabulary**, as the ghost sibling of the
  control beside it — no new token, no new value, no arbitrary length literal.
- Nothing else in the component moves: same elements, same order, same handlers, same strings.

## Out of scope

- **The headline's size.** *Your duel coins are only in this browser* renders at `text-display`, the
  same weight as *Victory*. Whether a post-verdict nudge should share the verdict's type weight is
  the product owner's, promoted from this round as its own `DEC`, and it is **not** repaired here.
- **The lobby.** Same class of defect, its own ticket, `TASK-120901`.
- **`design/screens/duel-end.html`.** The card does not draw this section at all; that is card debt
  under `ADR-0091` §5 and it is named in `TASK-120911`'s *Out of scope*.

## Tests

`AccountOffer.test.tsx`

| Test | Proves |
| --- | --- |
| `both of the offer's controls are dressed` | *Keep them with a password* **and** *Not now* each carry a non-empty `class` — both, so the already-dressed link cannot carry the assertion by itself |

## Acceptance criteria

- [ ] `AccountOffer.test.tsx > both of the offer's controls are dressed` passes
- [ ] Reverting `AccountOffer.tsx` alone reddens it
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
