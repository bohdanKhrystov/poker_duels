---
schema: 2
id: TASK-060303
title: The wordmark card
type: task
status: done
parent: STORY-0603
module: design
estimate: S
tier: opus
review: light
files_touched: 1
labels: [design]
depends_on: []
verify:
  - 'head -1 design/graphics/wordmark.html | grep -q "<!-- @dsCard group=\"Brand\" -->"'
  - grep -q '<title>' design/graphics/wordmark.html
  - grep -q -- '--pd-coin' design/graphics/wordmark.html
  - grep -q -- '--pd-track-caps' design/graphics/wordmark.html
  - '! grep -q "http" design/graphics/wordmark.html'
---

## Goal

The name, locked up: coin mark + bold "Poker" + quiet "Duels" in the system stack, shown at
three sizes, on dark and on card-face.

## Files

| File | Action |
| --- | --- |
| `design/graphics/wordmark.html` | create |

## Scope

- The lockup as CSS: coin at 0.92em leftmost, scaling with the type.
- Dark primary, light-surface variant, and the mark alone.

## Out of scope

- A drawn logotype or bespoke face — refused in the story's design notes.
- Marketing pages — not this epic.

## Tests

None — structural gates in `verify:`.

## Acceptance criteria

- [ ] Every command in `verify:` exits 0.

## Definition of done

Standard, with `EPIC-06`'s recorded deviation: the review is visual, in claude.ai/design.

## Deviations

The #448 review corrected the card before merge: the lockup baseline-aligns (the coin
opts back to center) so it drops into running text; the flex gap alone owns the word
spacing (the &nbsp; doubled it); the mono caption stack becomes the full --pd-font-mono
token; the light-surface muted color reuses --pd-text-faint instead of a hex born on the
card; the eyebrow regains weight 500; one .coin rule owns the gradient for both coins on
the card; and the glint moves to 36% 30% to match duel-coin.svg's cx/cy exactly.
