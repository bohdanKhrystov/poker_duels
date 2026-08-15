---
id: STORY-1103
title: The four reports the run owes
type: story
status: done
parent: EPIC-11
labels: [process, notifications]
depends_on: [STORY-1102]
---

## Goal

The four reports actually fire: a heartbeat every two hours while working, one when the run
stops, one when it parks on a decision, and one when tokens run short that names whether the
resume cron was armed.

## Why

The previous two stories build a thing that *can* report. This one makes it a duty. Without it
the notifier is a script nobody calls, which is how process tooling usually dies.

## Design notes

Per [`ADR-0042`](../../docs/adr/ADR-0042-the-run-reports-itself-every-two-hours.md):

- **The clock is an in-session recurring cron**, armed at the start of a run beside the resume
  job `build-epic` already schedules. Session-only, and that limitation is accepted — see the
  ADR's consequences, and `STORY-1104` for the option not taken.
- **Deduplicate on the last-sent stamp.** Two senders exist — the agent by hand, the cron on its
  clock — and the human must never receive both. The heartbeat is a no-op if a report went out
  inside the window.
- **The stop report is a `Stop` hook**, executed by the harness, because the case worth reporting
  is the case where the agent has nothing left to say. Registration lives in
  `.claude/settings.local.json`; the logic lives in a versioned script, because
  `.claude/settings.json` is deny-listed to the agent and that rule is not worth weakening here.
- **The budget report always names the cron.** `armed`, `not armed`, or `unknown` — never
  silence. Without that line the human cannot tell "it will pick itself up" from "it is over
  until I touch it", which is the whole reason the report exists.

## Tasks

| ID | Title | Status |
| --- | --- | --- |
| [TASK-110301](../tasks/TASK-110301-heartbeat-dedupes-on-the-window.md) | The heartbeat sends once per window, whoever fires it | ready |
| [TASK-110302](../tasks/TASK-110302-stop-and-budget-reports.md) | The stop and budget reports, and the cron line | ready |
| [TASK-110303](../tasks/TASK-110303-the-stop-hook.md) | The `Stop` hook, and the one line that registers it | ready |
| [TASK-110304](../tasks/TASK-110304-build-epic-reports.md) | `build-epic` gains its reporting duties | ready |

## Acceptance criteria

- [ ] Two heartbeats inside one window send exactly one message.
- [ ] The budget report contains an armed-cron line in all three states.
- [ ] The `Stop` hook script runs from a hook payload on stdin and exits 0 on malformed input.
- [ ] `.claude/skills/build-epic/SKILL.md` names the four reports and when each is owed.
- [ ] `docs/notifications.md` carries the registration line, verbatim and pasteable.

## Out of scope

- Surviving the session being quit — `STORY-1104`.
- Reporting per-ticket. The heartbeat is a digest; `build-epic` already forbids narrating every
  dispatch, and a notifier that pings on each merge would undo that at the human's expense.
