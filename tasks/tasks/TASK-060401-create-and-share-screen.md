---
schema: 2
id: TASK-060401
title: The create-and-share screen
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
  - 'head -1 design/screens/create-duel.html | grep -q "<!-- @dsCard group=\"Screens\" -->"'
  - grep -q '<title>' design/screens/create-duel.html
  - grep -q -- '--pd-accent-fill' design/screens/create-duel.html
  - grep -q -- '--pd-font-mono' design/screens/create-duel.html
  - '! grep -q "http" design/screens/create-duel.html'
---

## Goal

The "send a link" moment as one screen: the wordmark, one create action, and — once created
— the room code huge in mono, the share link, one copy action, and the empty opposite seat
waiting.

## Files

| File | Action |
| --- | --- |
| `design/screens/create-duel.html` | create |

## Scope

- Two frames on one card: before (one accent action, "Challenge someone") and after
  (code + link + copy, dashed empty seat, "waiting…" stated, not animated).
- The code in `--pd-font-mono` at display size, letter-spaced for reading aloud.

## Out of scope

- The join side — `TASK-060402`. Any account/profile UI — v0.2.

## Tests

None — structural gates in `verify:`.

## Acceptance criteria

- [ ] Every command in `verify:` exits 0.

## Definition of done

Standard, with `EPIC-06`'s recorded deviation: the review is visual, in claude.ai/design.
