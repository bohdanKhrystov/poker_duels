---
schema: 2
id: TASK-060105
title: The design directory README and sync procedure
type: task
status: done
parent: STORY-0601
module: design
estimate: XS
tier: opus
review: light
files_touched: 1
labels: [design]
depends_on: []
verify:
  - test -f design/README.md
  - grep -q 'f943b442-533a-4a81-b9f9-99c8a348b524' design/README.md
  - grep -q '@dsCard' design/README.md
  - grep -q 'tokens.css' design/README.md
---

## Goal

A fresh Claude session can sync `design/` to the claude.ai/design project — and pull edits
back — by reading one README, without rediscovering the project id or the card conventions.

## Files

| File | Action |
| --- | --- |
| `design/README.md` | create |

## Scope

- What `design/` is, and that `tokens.css` is the only place a design value is born.
- The card conventions: `@dsCard` first-line marker, self-contained HTML, inline token
  copies that must match `tokens.css`.
- The claude.ai/design project name and id, and the sync loop in both directions
  (`DesignSync`: read → finalize_plan → write; pull via list_files/get_file).

## Out of scope

- The tokens and cards themselves — the other four tasks.
- Automation of the sync — a session runs it; a script is not yet ticketed.

## Tests

None — structural gates in `verify:` (file exists, records the project id, documents the
marker and the canonical sheet).

## Acceptance criteria

- [ ] Every command in `verify:` exits 0.
- [ ] A session given only this README repeats the sync.

## Definition of done

Standard, per [`tasks/README.md`](../README.md), with the epic's recorded deviation: the
review is visual, in claude.ai/design.
