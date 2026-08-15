---
schema: 2
id: TASK-060406
title: The typed-code door
type: task
status: done
parent: STORY-0604
module: design
estimate: S
tier: opus
review: light
files_touched: 1
labels: [design]
depends_on: [TASK-060402]
verify:
  - 'head -1 design/screens/enter-code.html | grep -q "<!-- @dsCard group=\"Screens\" -->"'
  - grep -q '<title>' design/screens/enter-code.html
  - grep -q 'var(--pd-track-code)' design/screens/enter-code.html
  - grep -q 'focus-visible' design/screens/enter-code.html
  - '! grep -q "http" design/screens/enter-code.html'
---

## Goal

The front door offers "I have a code", and no card designs what it opens: the typed-code
entry surface. The #461 review found the gap by its symptom — the rate-limit refusal
existed for a path that reaches ten failed joins essentially only by hand-typing, yet
the screen a player types on was never drawn.

## Files

| File | Action |
| --- | --- |
| `design/screens/enter-code.html` | create |

## Scope

- One frame: the code entry — eight glyphs in `--pd-font-mono` at display size with
  `--pd-track-code`, the same visual language as the create screen's code well, one
  accent action.
- Entry-side refusals reuse the join screen's honest copy verbatim (`UNKNOWN_ROOM`
  covers the mistype; the muted rate-limit line); a note traces them to the join card
  rather than restating them as a second source.

## Out of scope

- The join screen itself — `TASK-060402`. Any code-format validation UI beyond the
  glyph count — the wire's refusal is the validator (ADR-0022).

## Tests

None — structural gates in `verify:`.

## Acceptance criteria

- [ ] Every command in `verify:` exits 0.

## Definition of done

Standard, with `EPIC-06`'s recorded deviation: the review is visual, in claude.ai/design.
