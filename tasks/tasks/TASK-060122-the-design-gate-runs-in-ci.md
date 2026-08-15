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
  - 'grep -qE "run:[^#]*design/check-drift\\.sh" .github/workflows/tickets.yml'
  - ./design/check-drift.sh
---

## Goal

Nothing in CI runs `design/check-drift.sh` — its only callers are sixteen ticket
verify blocks (#511 review). Six clauses of invariant now guard the design system,
and every one of them is observed only when somebody happens to work a design
ticket: a card can drift into `develop` through any PR that does not touch a design
ticket, and a regression in the gate itself ships unobserved, which is exactly how
this ticket's own predecessor reached review with a silent-pass bug.

## Files

| File | Action |
| --- | --- |
| `.github/workflows/tickets.yml` | edit — the docs/tickets job also runs `./design/check-drift.sh` |

## Scope

- The verify pins a `run:` line, not a mention: a comment naming the gate must not
  satisfy the gate's own ticket (#511 review).
- It joins the existing lightweight job (the one already running `lint_tickets.py`),
  not the Gradle or client jobs: the gate is stock shell and needs no toolchain.
- The step runs on every PR, so the gate's own regressions surface at the PR that
  causes them rather than at the next design ticket.

## Out of scope

- The gate's remaining silent edges — `TASK-060123`.

## Tests

None — structural gates in `verify:`.

## Acceptance criteria

- [ ] Every command in `verify:` exits 0.

## Definition of done

Standard, with `EPIC-06`'s recorded deviation: the review is visual, in claude.ai/design.
