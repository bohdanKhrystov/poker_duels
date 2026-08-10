---
schema: 2
id: TASK-000103
title: Token-lean agent workflow — planner, coder, reviewer, driver
type: task
status: in-progress
parent: STORY-0001
estimate: S
tier: opus
review: standard
files_touched: 3
labels: [process, meta, agents]
depends_on: []
verify:
  - python3 .github/scripts/lint_tickets.py
  - python3 .github/scripts/lint_tickets.py --startable
---

## Goal

`/build-epic EPIC-01` runs the poker engine to merged PRs on cheap models, stopping only for
decisions a human must make.

## Context

- [`docs/adr/ADR-0007-token-lean-agent-workflow.md`](../../docs/adr/ADR-0007-token-lean-agent-workflow.md)
  — the design and the evidence behind it.

## Why

Measured on the first ticket: `/code-review high` cost **132 379 tokens on a documentation PR**,
exhausted the session limit, and returned an inconclusive result because 14 of its 18 agents died
mid-run. A background fork cost **185 227 tokens** re-deriving context from cold. The process
built in `TASK-000101` is correct and unaffordable.

## Scope

- Three subagents: `planner` (Opus), `coder` (Haiku), `reviewer` (Haiku).
- Three skills: `build-epic` (goal-level driver), `next-ticket` (one ticket), `plan-story`
  (Opus planning pass, once per story).
- Ticket schema 2: `tier`, `review`, `files_touched`, and an executable `verify` block.
- Linter support for both schemas, plus `--startable` for the driver.
- `ADR-0007`, amending `ADR-0006`.
- `STORY-0101` migrated to schema 2 as the first consumer.

## Out of scope

- The other seven EPIC-01 stories. Migration is lazy, one story at a time, via `/plan-story` —
  so later stories benefit from what earlier ones teach us.
- Any poker code. The first real ticket is `TASK-010101`.
- Branch protection — still `TASK-000102`, still blocked.

## Tests

The linter is the test, and it is verified in both directions: it accepts the backlog as
committed, and it rejects each schema-2 violation.

| Check | Rejects |
| --- | --- |
| estimate | `M` on a schema-2 task |
| tier | anything outside haiku / sonnet / opus |
| files_touched | outside 1..3 |
| verify | missing or empty |

## Acceptance criteria

- [x] `python3 .github/scripts/lint_tickets.py` exits 0 on the migrated backlog.
- [x] `--startable` reports exactly one ticket, `TASK-010101`, with its schema, tier and review
      level.
- [x] Each of the four schema-2 rules above rejects a deliberately broken ticket, verified by
      breaking and restoring one.
- [x] The frontmatter parser reads block-sequence `verify:` lists whose commands contain colons.
- [x] Legacy schema-1 tickets still validate, so migration can stay lazy.
- [ ] Review passed, CI green, PR merged.

## Definition of done

Standard, per [`tasks/README.md`](../README.md): `verify` green, review passed, CI green, status
`done`, `BOARD.md` updated, squash-merged into `develop`. Not done until the PR is merged.
