---
schema: 2
id: TASK-060111
title: The drift check compares values, not only names
type: task
status: ready
parent: STORY-0601
module: design
estimate: S
tier: opus
review: light
files_touched: 1
labels: [design]
depends_on: [TASK-060108]
verify:
  - ./design/check-drift.sh
  - sh -c 'T=$(mktemp -d); cp -R design "$T/design"; perl -pi -e "s/0 1px 3px/0 1px 4px/" "$T/design/components/playing-card.html"; ! "$T/design/check-drift.sh" >/dev/null 2>&1'
---

## Goal

`check-drift.sh` compares only `--pd-` *names* against the sheet, so a card's inlined
`:root` copy of a value can drift silently — the class of defect the #431 review caught
by hand (#433 review, PLAUSIBLE finding). The gate learns to compare values: every
`--pd-NAME: VALUE` a card declares must equal the sheet's declaration for that name,
whitespace-normalized outside quoted strings.

## Files

| File | Action |
| --- | --- |
| `design/check-drift.sh` | edit — add the value comparison to the existing per-file loop |

## Scope

- Extract `--pd-NAME: VALUE;` declarations from each card and from the sheet; compare
  values per name, normalizing whitespace runs outside quoted strings (the sheet writes
  `rgba(0, 0, 0, 0.4)`, cards write `rgba(0,0,0,0.4)`; both are one value).
- A card declaring a name the sheet lacks is already an error; a card *using* a token it
  does not declare stays legal (self-containment governs what cards inline, not this gate).
- Stock macOS/Linux tools only, like the rest of the script; keep the self-test pattern —
  the verify's second command proves a drifted copy fails.
- The tree is value-aligned today (probed 2026-08-14, zero mismatches), so this lands as
  a pure gate change.

## Out of scope

- Changing any card — if the new comparison finds a mismatch, that is a fresh ticket.
- De-duplicating the inline-copy convention itself (design/README.md, ADR-0024 §2 stand).

## Tests

None — the verify runs the gate on the aligned tree and proves the negative path on a
mutated scratch copy. The copy is left for the OS to purge: agent permission profiles
here deny `rm`, and a cleanup trap would stall an autonomous run on a prompt — ~100KB
per run under `mktemp -d` is the recorded trade-off.

## Acceptance criteria

- [ ] Every command in `verify:` exits 0.

## Definition of done

Standard, with `EPIC-06`'s recorded deviation: the review is visual, in claude.ai/design.
