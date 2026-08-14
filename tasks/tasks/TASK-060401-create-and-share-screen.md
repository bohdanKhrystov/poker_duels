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
| `design/tokens/tokens.css` | edit — review: the code's tracking is born on the sheet as `--pd-track-code` |
| `web-client/src/styles/tokens.css` | edit — vendored mirror of the sheet; client CI pins byte-identity |

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

## Deviations

- Review round 1 found the code's `letter-spacing: 0.14em` born off-sheet — the join
  screen must render the same code and could only re-derive it. The value moved onto
  the sheet as `--pd-track-code`, which drags the vendored client mirror along (its CI
  test pins byte-identity), so the real footprint is three files; `files_touched` keeps
  the planned figure per the `TASK-060108` precedent.
