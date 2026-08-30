---
schema: 2
id: TASK-121103
title: The duels screen's checked filter, faint date and outcome weight are the card's
type: task
status: backlog
parent: STORY-1211
module: web-client
estimate: S
tier: sonnet
review: standard
files_touched: 2
labels: [qa, uat, bug, medium]
depends_on: []
verify:
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose src/history/HistoryScreen.test.tsx 2>&1 | grep -qF "the checked outcome filter is told apart from the unchecked ones"
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose src/history/HistoryScreen.test.tsx 2>&1 | grep -qF "a row's date is fainter than the opponent beside it"
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose src/history/HistoryScreen.test.tsx 2>&1 | grep -qF "the outcome word carries the card's weight"
  - cd web-client && NO_COLOR=1 npm run --silent test -- src/history/HistoryScreen.test.tsx
  - cd web-client && NO_COLOR=1 npm run --silent check
  - python3 .github/scripts/lint_tickets.py
---

## Goal

The three cues `design/screens/duels.html` draws that survived `TASK-121001` — the checked filter's
distinction, the date's faintness and the outcome word's weight — are on the shipped screen.

## The defect

`TASK-121001` dressed this screen and its gate went green; these are the properties it **never
promised**. Its `Scope` was *"dress the filter fieldset, its four radio labels…"* and *"the outcome
word carries the card's win/loss colour"* — not the checked state, not the date's step, not weight.
**This is a remainder, not a regression**, and the distinction is the one round 2 drew about
`TASK-120901`.

| card | shipped |
| --- | --- |
| `.radio.on { color: var(--pd-text); font-weight: 500 }` | all four labels are `text-small text-text-muted`, identical whether checked or not (`HistoryScreen.tsx:156,165,174,183`) |
| `.row .when { color: var(--pd-text-faint) }`, `.row .opp { color: var(--pd-text) }` | both spans are classless, so both compute `rgb(236,233,227)` (`HistoryScreen.tsx:227-228`) |
| `.row .outcome-word { font-weight: 500 }` | `outcomeColour` gives `text-loss`/`text-win` and no weight |

## Files

| File | Action |
| --- | --- |
| `web-client/src/history/HistoryScreen.tsx` | edit |
| `web-client/src/history/HistoryScreen.test.tsx` | edit |

## Scope

- **The checked radio label carries the card's distinction** — `text-text` and `font-medium` — and
  the unchecked three keep `text-text-muted`. Drive it off the same `state.filter.outcome`
  comparison the `checked` prop already uses; do not add state.
- **The date span carries `text-text-faint`** and the opponent span carries `text-text`, so the two
  compute different colours.
- **The outcome word carries `font-medium`** alongside the colour it already has.
- No new token, no new value, no arbitrary length literal (`ADR-0091` §4's fourth client guard).

## Out of scope

- **Every string on this screen.** `outcomeWord`, `coinDeltaText`, `nameOrNone` and `finishedAtText`
  own their words. **Change no literal.**
- **The date's locale.** Three rounds have ruled it not a defect; `docs/test-plan.md` §*Settled, and
  not a finding* carries it. Do not force a locale.
- **The screen's own `Show more`**, which is also classless. Round 3's report did not name it, and
  `EPIC-12` §Termination rule 1 freezes a round's set at triage — it is recorded in `STORY-1211`
  §*Owed to a later round*, not smuggled in here.
- **`Back`.** No card draws it; it is the subject of this round's promoted `DEC`.

## Tests

`HistoryScreen.test.tsx`

| Test | Proves |
| --- | --- |
| `the checked outcome filter is told apart from the unchecked ones` | with `All` checked, its label's class list carries `text-text` **and** `font-medium` while the `Won` label carries neither — both halves, so a blanket restyle of all four cannot pass |
| `a row's date is fainter than the opponent beside it` | in one rendered row, the date span carries `text-text-faint` and the opponent span carries `text-text` — asserted on the two spans of the **same** row, so one class present anywhere cannot carry it |
| `the outcome word carries the card's weight` | the element holding `outcomeWord` carries `font-medium` in addition to its `text-win`/`text-loss` colour |

## Acceptance criteria

- [ ] `HistoryScreen.test.tsx > the checked outcome filter is told apart from the unchecked ones` passes
- [ ] `HistoryScreen.test.tsx > a row's date is fainter than the opponent beside it` passes
- [ ] `HistoryScreen.test.tsx > the outcome word carries the card's weight` passes
- [ ] Every command in `verify:` exits 0
