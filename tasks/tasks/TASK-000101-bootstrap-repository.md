---
id: TASK-000101
title: Bootstrap repository, documents and ticket system
type: task
status: in-review
parent: STORY-0001
estimate: M
labels: [process, meta]
depends_on: []
---

## Goal

Turn an empty repository into one where an agent can be handed a ticket ID and get to work.

## Context

This is the only ticket in the project written after the work it describes, because it *is* the
process being created. Everything from here follows the rules it sets down.

## Scope

- `CLAUDE.md` — the working agreement, kept short because it is always in context.
- `CONTRIBUTING.md` — branch model, PR rules, commit format.
- `README.md` — what this project is.
- `docs/` — vision, architecture, duel rules, workflow.
- `docs/adr/` — the five founding decision records, plus the index and the open-decision
  register.
- `tasks/` — conventions, templates, board, and EPIC-01 fully broken down.
- `.github/` — PR template, ticket linter, and the workflow that runs it.
- `.claude/settings.json` — a permission allowlist for the commands this project uses
  constantly, so that ordinary work does not generate a prompt per command, paired with a deny
  list for the operations no agent should perform unattended. Rationale in
  [`docs/workflow.md`](../../docs/workflow.md#permissions).
- `.gitignore` for a Kotlin/Gradle and Node repository.

## Out of scope

- Gradle, the build, and build CI — `TASK-010101` through `TASK-010103`.
- Any production code.
- Epics beyond EPIC-01.

## Acceptance criteria

- [x] `main` and `develop` exist, and `develop` is the default branch.
- [x] Both are protected: no direct pushes, PR required, linear history.
- [x] Squash merge is the only merge method; head branches delete on merge.
- [x] `python3 .github/scripts/lint_tickets.py` passes and fails a malformed ticket.
- [x] The linter runs on every PR into `develop` and `main`.
- [x] EPIC-01 is broken into 8 stories and 29 tasks, each with scope, out-of-scope,
      acceptance criteria and tests.
- [x] Exactly one task is `ready`, and `BOARD.md` names it.
- [x] Every architectural decision carried over from the design conversation has an ADR.
- [x] `DEC-001` is registered as an open decision rather than silently answered.

## Tests

The linter is the test. It is verified in both directions: it passes on the backlog as
committed, and it correctly rejected four tickets that claimed `ready` while their dependencies
were unfinished — which is how those four came to be corrected before this PR was opened.

## Definition of done

Standard, per [`tasks/README.md`](../README.md): build green, tests green, status `done`,
`BOARD.md` updated, squash-merged into `develop` by a PR linking this ticket.
