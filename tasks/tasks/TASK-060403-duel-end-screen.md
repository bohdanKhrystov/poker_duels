---
schema: 2
id: TASK-060403
title: The duel-end screen
type: task
status: done
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

Someone won: "Victory" and "Defeat" frames with the coin's true movement — the winner
gains one, nobody is debited (the coin counts duels won; `docs/vision.md`) — hands
played, the outcome, and the one action that matters — Rematch.

## Files

| File | Action |
| --- | --- |
| `design/screens/duel-end.html` | create |

## Scope

- Two frames: Victory (hero size, `--pd-win`, coin +1) and Defeat (`--pd-loss`, the
  coin goes to the winner — no debit exists — never softened, same layout).
- "17 hands · 12 minutes" meta line; Rematch as the accent fill; "Back to lobby" ghost.

## Out of scope

- The rematch handshake states — `TASK-060404`. Decision-quality analysis — EPIC-08.

## Tests

None — structural gates in `verify:`.

## Acceptance criteria

- [ ] Every command in `verify:` exits 0.

## Definition of done

Standard, with `EPIC-06`'s recorded deviation: the review is visual, in claude.ai/design.

## Deviations

- The ticket's original goal and scope said "coin ±1"/"coin −1" — contradicting the
  recorded product rule (the duel coin is a counter of duels won, not a balance:
  `docs/vision.md`; `docs/duel-rules.md` awards the winner only). Review round 1
  caught it; ticket and card were corrected together at the source, per the
  `TASK-060204` precedent. The defeat frame states the coin going to the winner.
- "The final stacks" render as prose ("you took the whole stack"): a freezeout's end
  stacks are degenerate — all or nothing — so numbers would be ceremony. Recorded
  here rather than silently substituted.
- In-frame commentary (`.how`) was removed: commentary lives in the mono notes;
  frames carry only real screen copy. "Back to lobby" restored per the scope.
