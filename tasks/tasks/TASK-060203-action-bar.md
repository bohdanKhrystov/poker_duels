---
schema: 2
id: TASK-060203
title: The action bar
type: task
status: ready
parent: STORY-0602
module: design
estimate: S
tier: opus
review: light
files_touched: 1
labels: [design]
depends_on: []
verify:
  - 'head -1 design/components/action-bar.html | grep -q "<!-- @dsCard group=\"Components\" -->"'
  - grep -q '<title>' design/components/action-bar.html
  - grep -q -- '--pd-accent-fill' design/components/action-bar.html
  - grep -q -- '--pd-focus' design/components/action-bar.html
  - '! grep -q "http" design/components/action-bar.html'
---

## Goal

One self-contained card shows the action bar in its four moments: check/bet, facing a bet
(fold/call/raise), disabled, and off-turn ("Waiting for …").

## Files

| File | Action |
| --- | --- |
| `design/components/action-bar.html` | create |

## Scope

- Fold ghost, Check/Call ghost with amount, Bet/Raise accent fill with amount.
- Sizing row: min · ⅓ · ½ · pot · all-in chips and a stepper.
- One control drawn with the focus outline pair, proving keyboard visibility.

## Out of scope

- The bet-sizing mathematics — the engine's `LegalActions` decides; this draws the shell.
- The table composition — `TASK-060204`.

## Tests

None — structural gates in `verify:`.

## Acceptance criteria

- [ ] Every command in `verify:` exits 0.

## Definition of done

Standard, with `EPIC-06`'s recorded deviation: the review is visual, in claude.ai/design.
