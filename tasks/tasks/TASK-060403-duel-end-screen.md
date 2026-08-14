---
schema: 2
id: TASK-060403
title: The duel-end screen
type: task
status: ready
parent: STORY-0604
module: design
estimate: S
tier: opus
review: light
files_touched: 1
labels: [design]
depends_on: []
verify:
  - 'head -1 design/screens/duel-end.html | grep -q "<!-- @dsCard group=\"Screens\" -->"'
  - grep -q '<title>' design/screens/duel-end.html
  - grep -q -- '--pd-win' design/screens/duel-end.html
  - grep -q -- '--pd-loss' design/screens/duel-end.html
  - '! grep -q "http" design/screens/duel-end.html'
---

## Goal

Someone won: "Victory" and "Defeat" frames with the coin ±1, hands played, the final
stacks, and the one action that matters — Rematch.

## Files

| File | Action |
| --- | --- |
| `design/screens/duel-end.html` | create |

## Scope

- Two frames: Victory (hero size, `--pd-win`, coin +1) and Defeat (`--pd-loss`, coin −1,
  never softened, same layout).
- "17 hands · 12 minutes" meta line; Rematch as the accent fill; "Back to lobby" ghost.

## Out of scope

- The rematch handshake states — `TASK-060404`. Decision-quality analysis — EPIC-08.

## Tests

None — structural gates in `verify:`.

## Acceptance criteria

- [ ] Every command in `verify:` exits 0.

## Definition of done

Standard, with `EPIC-06`'s recorded deviation: the review is visual, in claude.ai/design.
