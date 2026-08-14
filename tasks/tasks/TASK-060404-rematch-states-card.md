---
schema: 2
id: TASK-060404
title: The rematch states
type: task
status: ready
parent: STORY-0604
module: design
estimate: S
tier: opus
review: light
files_touched: 1
labels: [design]
depends_on: [TASK-060403]
verify:
  - 'head -1 design/screens/rematch-states.html | grep -q "<!-- @dsCard group=\"Screens\" -->"'
  - grep -q '<title>' design/screens/rematch-states.html
  - grep -q -- '--pd-accent' design/screens/rematch-states.html
  - '! grep -q "http" design/screens/rematch-states.html'
---

## Goal

The handshake drawn: I offered, they offered, both offered — three quiet states on the
duel-end layout, because a rematch starts only when both seats want it.

## Files

| File | Action |
| --- | --- |
| `design/screens/rematch-states.html` | create |

## Scope

- Mine-offered: button becomes "Rematch offered — waiting for ImKate", subtle accent.
- Theirs-offered: "ImKate offers a rematch" over the button, which stays live.
- The button flips sides with the dealer, per `STORY-0206`'s rematch rule — noted on card.

## Out of scope

- Timeout/abandon during the offer — the room reaper's silence is v0.1 behavior.

## Tests

None — structural gates in `verify:`.

## Acceptance criteria

- [ ] Every command in `verify:` exits 0.

## Definition of done

Standard, with `EPIC-06`'s recorded deviation: the review is visual, in claude.ai/design.
