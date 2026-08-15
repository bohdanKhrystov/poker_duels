---
id: STORY-1102
title: Compose the report from repository state
type: story
status: done
parent: EPIC-11
labels: [process, notifications]
depends_on: [STORY-1101]
---

## Goal

A status report — what landed, what is in flight, what is blocked — built by reading the
repository, so that producing one does not require the agent that did the work to still be
alive.

## Why

This is the half of the epic that makes the budget report possible. If a report can only be
written by the agent, then the one report the human most needs is the one that can never be
sent. Reading `BOARD.md` and `git log` instead means anything that can run a script can answer
*what is happening*.

## Design notes

Per [`ADR-0042`](../../docs/adr/ADR-0042-the-run-reports-itself-every-two-hours.md):

- **Repository state is the source.** `tasks/BOARD.md` for statuses, `git log` for what landed,
  `gh pr list` for what is open, the ticket files for what is blocked and on which `DEC-NNN`.
- **The breadcrumb carries only what the repository cannot know**: the epic list being worked,
  the last-sent stamp, and whether the resume cron was armed. `.claude/run-state.json`,
  gitignored, and every field optional — a report from a machine that has never seen one must
  still be a report.
- **Never raise.** A missing board, a `gh` that is not installed, a git repository with no
  commits: each degrades one section to a single honest line. A composer that throws produces no
  report at all, which is the failure it exists to prevent.
- Telegram's limit is 4096 characters. The report truncates on a section boundary and says it
  did.

## Tasks

| ID | Title | Status |
| --- | --- | --- |
| [TASK-110201](../tasks/TASK-110201-the-run-state-breadcrumb.md) | The run-state breadcrumb the agent stamps | ready |
| [TASK-110202](../tasks/TASK-110202-read-the-board.md) | Read ticket and epic status out of the board | ready |
| [TASK-110203](../tasks/TASK-110203-compose-the-status-report.md) | Compose the status report, degrading section by section | ready |

## Acceptance criteria

- [ ] A report is produced in a repository with no run state, no `gh` and no network.
- [ ] The report names blocked tickets with the `DEC-NNN` each is blocked on.
- [ ] The report states whether the resume cron was armed, including when it does not know.
- [ ] A report longer than 4096 characters is truncated at a section boundary and says so.

## Out of scope

- Sending it — `STORY-1101` owns the wire.
- Deciding *when* to send — `STORY-1103`.
- Reading GitHub Actions or CI status. `gh pr list` gives open PRs; per-check status is more
  API calls than a status message justifies.
