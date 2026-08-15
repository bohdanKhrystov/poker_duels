---
schema: 2
id: TASK-060122
title: The design gate runs in CI
type: task
status: ready
parent: STORY-0601
module: design
estimate: XS
tier: opus
review: light
files_touched: 1
labels: [design]
depends_on: []
verify:
  - 'grep -qE "^[^#]*design/check-drift\.sh" .github/workflows/tickets.yml'
  - ./design/check-drift.sh
---

## Goal

Nothing in CI runs `design/check-drift.sh` — its only callers are sixteen ticket
verify blocks (#511 review). Six clauses of invariant guard the design system, and
every one of them is observed only when somebody happens to work a design ticket: a
card can drift into `develop` through any PR that touches no design ticket, and a
regression in the gate itself ships unobserved. That is exactly how three separate
defects in `TASK-060121`'s attempts reached review instead of CI.

## Files

| File | Action |
| --- | --- |
| `.github/workflows/tickets.yml` | edit — the docs/tickets job also runs `./design/check-drift.sh` |

## Scope

- It joins the existing lightweight job (the one already running `lint_tickets.py`),
  not the Gradle or client jobs: the gate is stock shell and needs no toolchain.
- The verify matches the path on any non-comment line, so both `run: ./design/…` and
  a `run: |` block scalar satisfy it while a commented-out mention does not (#511
  review found the first spelling too weak and too strict at once).

## Out of scope

- The gate's remaining silent edges — `TASK-060123`; and how it reads values —
  `DEC-035`.

## Tests

None — structural gates in `verify:`.

## Acceptance criteria

- [ ] Every command in `verify:` exits 0.

## Definition of done

Standard, with `EPIC-06`'s recorded deviation: the review is visual, in claude.ai/design.
