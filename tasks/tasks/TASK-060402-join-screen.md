---
schema: 2
id: TASK-060402
title: The join screen
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
  - 'head -1 design/screens/join-duel.html | grep -q "<!-- @dsCard group=\"Screens\" -->"'
  - grep -q '<title>' design/screens/join-duel.html
  - grep -q -- '--pd-accent-fill' design/screens/join-duel.html
  - '! grep -q "http" design/screens/join-duel.html'
---

## Goal

The link opened: who challenges you, the room code confirming where you are, your editable
display name, and one decision — take the seat.

## Files

| File | Action |
| --- | --- |
| `design/screens/join-duel.html` | create |

## Scope

- One frame: "ImKate challenges you", the code small in mono, the name field prefilled and
  edit-in-place, one accent action.
- A second small frame for the refusals a code can earn (room full, expired) — stated
  plainly, one line each.

## Out of scope

- The create side — `TASK-060401`. Rate-limit copy (`TOO_MANY_ATTEMPTS`) — with the
  refusal frame, one muted line, no dedicated design.

## Tests

None — structural gates in `verify:`.

## Acceptance criteria

- [ ] Every command in `verify:` exits 0.

## Definition of done

Standard, with `EPIC-06`'s recorded deviation: the review is visual, in claude.ai/design.
