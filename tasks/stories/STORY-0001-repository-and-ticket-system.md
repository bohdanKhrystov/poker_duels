---
id: STORY-0001
title: Repository, documents and ticket system
type: story
status: ready
parent: EPIC-00
labels: [process, meta]
---

## Goal

Clone the repository, read `CLAUDE.md`, and be able to pick up a ticket and deliver it without
asking anyone anything.

## Why

This is the bootstrap. It is the one piece of work that cannot follow the process, because it
creates it — everything after it can and must.

## Design notes

- Tickets are markdown in the repository rather than GitHub Issues, for the reasons in
  [`ADR-0004`](../../docs/adr/ADR-0004-branching-and-ticket-workflow.md). The deciding factor is
  that an agent reading a ticket should need no network call and no API token, and that a ticket
  and its implementation should be reviewable in one diff.
- Documents are memory. They are kept small and non-overlapping so that a task can name the two
  or three worth reading rather than gesturing at a folder.
- The backlog is linted in CI. A ticket system with no enforcement decays into free-form notes
  within a month, and the decay is invisible until someone needs it.
- Decisions carried over from the design conversation are written up as ADRs now, while the
  reasoning is fresh, rather than reconstructed later from the code.

## Tasks

| ID | Title | Status |
| --- | --- | --- |
| [TASK-000101](../tasks/TASK-000101-bootstrap-repository.md) | Bootstrap repository, documents and ticket system | in-review |
| [TASK-000102](../tasks/TASK-000102-enable-branch-protection.md) | Enable branch protection on main and develop | blocked |
| [TASK-000106](../tasks/TASK-000106-the-board-and-the-ticket-file-are-one-register.md) | The board and the ticket file are one register, and the linter reads both | backlog |

> **This table is a third register and it is out of step.** It names three of this story's six
> tickets, and the cells above were written before `TASK-000103`, `TASK-000104` and `TASK-000105`
> existed. `tasks/BOARD.md` and each ticket file are the two registers `TASK-000106` puts a gate
> between; a story file's own task table is deliberately **not** one of them, for the reason that
> ticket's `## Out of scope` gives. Left as found rather than quietly repaired: every cell here is a
> claim about a status, and this is not the pull request that gets to make one.

## Acceptance criteria

- [ ] `main` and `develop` are protected; direct pushes are refused for everyone.
      *Blocked by the free-plan/private-repo constraint — see TASK-000102.*
- [ ] Squash merge is the only merge method, and head branches delete on merge.
- [ ] `tasks/` holds the conventions, templates, the board, and a specified first epic.
- [ ] The ticket linter runs on every pull request and fails on a malformed backlog.
- [ ] `CLAUDE.md` alone is enough to orient an agent that has read nothing else.
- [ ] A task can only reach `done` via a reviewed pull request merged into `develop`, and that
      rule is written down in every place an agent might look.

## Out of scope

- The Gradle build and its CI — STORY-0101, deliberately, so that this story stays about
  process and that one stays about tooling.
- Writing epics beyond EPIC-01.
