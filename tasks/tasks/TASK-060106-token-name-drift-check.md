---
schema: 2
id: TASK-060106
title: A drift check that diffs every card's inlined token names against the sheet
type: task
status: done
parent: STORY-0601
module: design
estimate: S
tier: opus
review: light
files_touched: 1
labels: [design]
depends_on: [TASK-060101]
verify:
  - test -x design/check-drift.sh
  - ./design/check-drift.sh
---

## Goal

Renaming a token in `tokens.css` while any card still inlines or prints the old name fails a
command instead of drifting silently — the guarantee ADR-0024 §2 makes, delivered as one
script instead of per-ticket grep lists.

## Files

| File | Action |
| --- | --- |
| `design/check-drift.sh` | create |

## Scope

- For every `design/**/*.html`: extract each `--pd-*` name it mentions; every name must be
  declared in `design/tokens/tokens.css`. Unknown name → exit 1, naming file and token.
- Report a summary line; exit 0 when clean. No dependencies beyond POSIX tools.

## Out of scope

- Value-drift checking (inlined hex vs sheet) — a later sharpening if name checks prove
  insufficient.
- CI wiring — which workflow runs this is a decision for the next infra ticket, not here.
- Aligning the older cards' chrome (px sizes, eyebrow weights) — noted from review of
  TASK-060103, still unticketed.

## Tests

None — the script is its own gate: `verify:` runs it over the real cards.

## Acceptance criteria

- [ ] Every command in `verify:` exits 0.
- [ ] Renaming any `--pd-` token in a scratch copy makes the script exit 1.

## Definition of done

Standard, with `EPIC-06`'s recorded deviation: the review is visual, in claude.ai/design.
