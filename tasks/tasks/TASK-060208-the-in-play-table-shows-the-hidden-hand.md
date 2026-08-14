---
schema: 2
id: TASK-060208
title: The in-play table shows the hidden hand
type: task
status: ready
parent: STORY-0602
module: design
estimate: XS
tier: opus
review: light
files_touched: 1
labels: [design]
depends_on: [TASK-060204]
verify:
  - grep -q 'class="back"' design/screens/duel-table.html
  - grep -q 'oppcards' design/screens/duel-table.html
  - grep -q -- '--pd-card-back:' design/screens/duel-table.html
  - '! grep -q "http" design/screens/duel-table.html'
---

## Goal

The merged in-play screen gains the opponent block the states screens already have — the
two face-down minis and the reserved bet/muck line — so every table state shares one
skeleton and nothing appears from nowhere at a transition.

## Files

| File | Action |
| --- | --- |
| `design/screens/duel-table.html` | edit — opponent backs at the 40px reference; the bet line moves into the shared reserved slot |

## Scope

- Markup and the faithful `.back`/`.oppcards`/`.bet-line` copies; the only :root addition
  is `--pd-card-back: #35567e`, which the back requires.

## Out of scope

- Component changes — the card component already defines the back.

## Tests

None — structural greps in `verify:`.

## Acceptance criteria

- [ ] Every command in `verify:` exits 0.

## Definition of done

Standard, with `EPIC-06`'s recorded deviation: the review is visual, in claude.ai/design.
