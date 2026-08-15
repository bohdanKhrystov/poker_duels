---
schema: 2
id: TASK-110304
title: build-epic gains its reporting duties
type: task
status: done
parent: STORY-1103
estimate: S
tier: sonnet
review: standard
files_touched: 2
labels: [process, notifications, meta]
depends_on: [TASK-110303]
verify:
  - grep -q "notify.py heartbeat" .claude/skills/build-epic/SKILL.md
  - grep -q "cron-armed" .claude/skills/build-epic/SKILL.md
  - python3 .github/scripts/lint_tickets.py
---

## Goal

The next `/build-epic` run reports without being told to, because the skill says it must.

## Files

| File | Action |
| --- | --- |
| `.claude/skills/build-epic/SKILL.md` | edit |
| `tasks/BOARD.md` | edit |

## Scope

- A new section, **Report while you run**, placed next to *Arm the resume before you need it*
  because they share a cause: both are things that must be set up while the budget is healthy.
- It states the four reports and when each is owed:
  - **heartbeat** — arm a second recurring cron at run start, beside the resume job, firing
    `notify.py heartbeat` every two hours; and call it with `--force` at each epic boundary
  - **stop** — carried by the `Stop` hook; the driver writes `current_epic` into the run state at
    the start so the hook knows a run is in flight, and clears it at the end so it stops
  - **blocked** — `notify.py blocked --decision DEC-NNN` when an epic parks on a human decision
  - **budget** — `notify.py budget --cron-armed armed|not-armed|unknown` the moment a usage
    warning appears, **before** doing anything else
- The budget rule is written with its reason: the report must go out while a turn can still run,
  because a usage limit ends the turn and every turn after it. A plan that reports after the
  limit never reports.
- It restates the `--cron-armed` value the driver passes: `armed` only once `CronCreate`
  returned successfully, never optimistically.
- `BOARD.md` gains the `EPIC-11` row and its story table.

## Out of scope

- Changing anything about how tickets are dispatched, batched, landed or merged. This adds
  reporting; it must not touch the loop.
- Per-ticket notifications. The skill already forbids narrating each dispatch, and a message per
  merge would undo that at the human's expense.

## Tests

No test file — this is a skill document and a board. The verify block asserts the two commands
the skill must name, and that the backlog still lints.

## Acceptance criteria

- [ ] `.claude/skills/build-epic/SKILL.md` names `notify.py heartbeat` and `--cron-armed`
- [ ] The skill states that the budget report goes out *before* the limit, and why
- [ ] `BOARD.md` carries the `EPIC-11` row and its four stories
- [ ] `python3 .github/scripts/lint_tickets.py` exits 0
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md). This PR changes the agent's own operating
rules, so it gets its own line in the run's final report — see the skill's *Merging* section.
