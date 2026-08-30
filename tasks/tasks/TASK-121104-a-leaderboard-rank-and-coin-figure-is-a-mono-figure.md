---
schema: 2
id: TASK-121104
title: A leaderboard rank and coin figure is the card's mono figure
type: task
status: done
parent: STORY-1211
module: web-client
estimate: XS
tier: sonnet
review: standard
files_touched: 2
labels: [qa, uat, bug, medium]
depends_on: []
verify:
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose src/ladder/LadderScreen.test.tsx 2>&1 | grep -qF "a row's rank and coins are mono figures, and the rank is the muted one"
  - cd web-client && NO_COLOR=1 npm run --silent test -- src/ladder/LadderScreen.test.tsx
  - cd web-client && NO_COLOR=1 npm run --silent check
  - python3 .github/scripts/lint_tickets.py
---

## Goal

A leaderboard row's rank and coin figures are the card's mono, tabular figures — the rank muted and
right-aligned — rather than left-aligned UI text at full brightness.

## The defect

`TASK-121002` split the row into its three parts and dressed the self line and *Show more*; round 3
confirms both of those fixed. It did **not** promise the figures' type treatment, so this is a
remainder, not a regression.

`design/screens/leaderboard.html`:

```
.row .rank  { font-family: var(--pd-font-mono); font-variant-numeric: tabular-nums;
              color: var(--pd-text-muted); min-width: 2ch; text-align: right; flex-shrink: 0; }
.row .coins { font-family: var(--pd-font-mono); font-variant-numeric: tabular-nums;
              min-width: 3ch; text-align: right; }
```

Shipped, from `LadderScreen.tsx:111-114`, the rank and coin spans carry no class at all and compute
`font-family: -apple-system…`, `color: rgb(236,233,227)`, `text-align: start`. Ranks and coin counts
are columns of digits a reader scans down; a proportional font makes them ragged.

## Files

| File | Action |
| --- | --- |
| `web-client/src/ladder/LadderScreen.tsx` | edit |
| `web-client/src/ladder/LadderScreen.test.tsx` | edit |

## Scope

- **The rank span** carries the client's mono and tabular-numerals utilities, `text-text-muted`, and
  right alignment with the card's `2ch` minimum expressed through a token, never a bare length.
- **The coins span** carries the same mono and tabular-numerals utilities and right alignment. It
  keeps its full brightness — the card mutes the rank and not the coins.
- No new token, no new value, no arbitrary length literal (`ADR-0091` §4's fourth client guard
  refuses `-[380px]`; `-[var(--pd-…)]` passes).

## Out of scope

- **Every string on this screen.** `LADDER_HEADING`, `MORE`, `seasonName`, `rowLine`, `selfLine` and
  `NO_PLACE_THIS_SEASON` are `ladder-text.ts`'s. **Change no literal.**
- **The self line in its no-place state** — `TASK-121107`, which has to change a merged assertion and
  should not be tangled with a type fix.
- **The order tied rows sit in.** `ADR-0064` §4 settles it as arbitrary. Add no sort, no secondary
  column, no tie marker.

## Tests

`LadderScreen.test.tsx`

| Test | Proves |
| --- | --- |
| `a row's rank and coins are mono figures, and the rank is the muted one` | in one rendered row: the rank span's class list carries the mono utility, the tabular-numerals utility **and** `text-text-muted`; the coins span carries the mono and tabular utilities and **not** `text-text-muted`. Both spans of the same row, and the negative half too — otherwise muting everything passes |

## Acceptance criteria

- [ ] `LadderScreen.test.tsx > a row's rank and coins are mono figures, and the rank is the muted one` passes
- [ ] The two `LadderScreen.test.tsx` cases `TASK-121002` merged still pass, unedited
- [ ] Every command in `verify:` exits 0
