---
schema: 2
id: TASK-060405
title: The flow vocabulary earns a component card
type: task
status: ready
parent: STORY-0604
module: design
estimate: S
tier: opus
review: light
files_touched: 1
labels: [design]
depends_on: [TASK-060404]
verify:
  - 'head -1 design/components/flow-actions.html | grep -q "<!-- @dsCard group=\"Components\" -->"'
  - grep -q '<title>' design/components/flow-actions.html
  - grep -q 'var(--pd-track-code)' design/components/flow-actions.html
  - grep -q 'focus-visible' design/components/flow-actions.html
  - '! grep -q "http" design/components/flow-actions.html'
---

## Goal

The four flow screens share a vocabulary with no component home: the standalone `.btn`
call-to-action, the display-size `.code` well, the `.linkline` path-and-copy row, and
the refusal panel. Each screen's copy is commented "until it earns a component card"
(#456 reviews, rounds 1 and 2: `.code`/`.linkline` born without a home forces the next
screen to re-derive — the join screen's chip had in fact re-derived the tracking as
`0.1em` before landing). This ticket is the home.

## Files

| File | Action |
| --- | --- |
| `design/components/flow-actions.html` | create — the canonical card for the flow screens' shared vocabulary |

## Scope

- One card, Components group: the standalone action pair (fill and ghost, with
  `:focus-visible`), the code well (`--pd-track-code` with its centering indent), the
  linkline row, and the refusal panel — each with the states it owns and a line on
  when a screen may copy it.
- Values consume the card's inlined tokens; geometry matches the merged screens
  byte-for-byte, so the screens' existing copies are already faithful.

## Out of scope

- Editing the four screens to relabel their copies as faithful — that sweep rides the
  epic-close alignment pass, once this card is the recorded canonical.
- The in-duel action bar — a different component with different mechanics.

## Tests

None — structural gates in `verify:`.

## Acceptance criteria

- [ ] Every command in `verify:` exits 0.

## Definition of done

Standard, with `EPIC-06`'s recorded deviation: the review is visual, in claude.ai/design.
