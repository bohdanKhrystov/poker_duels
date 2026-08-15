---
id: EPIC-11
title: Status notifications — the run reports itself
type: epic
status: in-progress
labels: [process, meta, notifications]
---

## Goal

An unattended run tells the human what it is doing, without being asked. Every two hours while
working it sends a Telegram message naming what landed, what is in flight and what is blocked;
it sends one when it stops, one when it parks on a decision, and one when tokens run short —
and that last one says **whether the resume cron was armed**, because "I am out of budget" is
half a sentence without it.

## Why now

`/build-epic` is built not to stop. That is the correct design and it has a consequence nobody
chose: from outside, *working through ticket 40 of 60* and *dead since 11pm* look identical.
`EPIC-03`, `EPIC-04` and `EPIC-05` are the first epics long enough for the difference to cost a
night, so the reporting goes in before them rather than after the first silent stall.

The sharp end is the budget report. A usage limit terminates the turn and every turn after it,
so the report most likely to be owed is the one the agent is least able to write. That is why
this is an epic and not a paragraph added to the skill: the mechanism has to work when the
agent does not.

## Scope

- The transport: one Telegram message, sent from a script, credentials never in the repository.
- The report: composed from `tasks/BOARD.md`, `git log`, `gh pr list` and the ticket files, so
  anything that can run a script can produce one.
- The run-state breadcrumb the agent stamps — the epic list, the last send, the armed cron.
- The clock: a two-hourly in-session cron, deduplicated against reports sent by hand.
- The lifecycle reports: stop, block, budget — carried by hooks rather than by intentions.
- `build-epic`'s own duties, written into the skill so the next run inherits them.

## Out of scope

- **A heartbeat that survives the session being quit.** `STORY-1104` holds the launchd design,
  deliberately unstarted — see [`ADR-0042`](../../docs/adr/ADR-0042-the-run-reports-itself-every-two-hours.md).
- **A second channel.** Slack, email and SMS are one module each and none is asked for.
- **Reporting on anything but a run.** No build notifications, no CI relays, no PR pings —
  GitHub already does those and does them better.
- **Two-way control.** The bot sends; it does not take commands. A chat that can start a run is
  a remote-execution surface, and this epic is not the place to open one.

## Ways of working — a recorded deviation

This epic was **written from the human's directive rather than from `docs/vision.md`**, which is
the first time that has happened. `docs/vision.md` describes the product; this epic is about the
process that builds it, and the requirements — Telegram, two hours, report on stop, name the
armed cron — were dictated in a single message on 2026-08-15. `DEC-036` records the three
choices that were genuinely open inside that directive, and the human answered them in the same
session.

The consequence worth flagging: nothing here derives from the vision, so the `product-owner`
agent's usual check — *does this apply the vision or change it?* — has no purchase. `EPIC-00` is
the precedent, and the same rule applies: process epics are the human's to shape.

**The second deviation is larger, and it is the one to look at.** All eleven tickets were
written, implemented and landed in **one pull request**, by the driver, without a coder dispatch
or a reviewer pass — against working agreement 1 (*one ticket at a time*) and 7 (*a task is not
done until its PR is merged*). The reason was ordering: the epic exists to make the `EPIC-03`
/`04`/`05` run observable, and eleven sequential PRs would have spent the session it was meant
to protect. The tickets were written **before** the code, not reconstructed after it, and each
one's `verify` block was run — but nobody reviewed this diff except the agent that wrote it, and
the epic's metrics say so rather than showing a first-pass acceptance rate that was never
measured. `STORY-1104`, if it is ever started, goes through the normal loop.

## Stories

| ID | Title | Status |
| --- | --- | --- |
| [STORY-1101](../stories/STORY-1101-send-a-real-message.md) | Send a real message | done |
| [STORY-1102](../stories/STORY-1102-compose-the-report.md) | Compose the report from repository state | done |
| [STORY-1103](../stories/STORY-1103-the-four-reports.md) | The four reports the run owes | done |
| [STORY-1104](../stories/STORY-1104-a-heartbeat-that-outlives-the-session.md) | A heartbeat that outlives the session | backlog |

`STORY-1104` is written and deliberately not started. It is the option the human declined on
2026-08-15 — a launchd agent that keeps reporting after Claude Code is quit — kept as a ticket
so that changing that mind costs one command rather than one rediscovery.

## Definition of done

- [x] `STORY-1101`, `STORY-1102` and `STORY-1103` are `done`.
- [x] A real Telegram message, sent by `scripts/notify/`, arrived on the human's phone.
      *(2026-08-15, message id 3, chat 591343919.)*
- [x] `python3 scripts/notify/notify.py doctor` exits 0 on a configured machine and non-zero on
      an unconfigured one. *(`test_cli.DoctorTest` covers both sides.)*
- [x] A report names the armed resume cron, and says `not armed` when it is not — and `unknown`
      when it does not know, which is a third state rather than a synonym for the second.
- [x] `docs/notifications.md` takes a fresh machine from no bot to a delivered message.

The epic stays `in-progress` rather than `done` because `STORY-1104` is deliberately open. Every
story the epic set out to build is finished.

## Open decisions

| ID | Answered by | What it means here |
| --- | --- | --- |
| `DEC-036` | [ADR-0042](../../docs/adr/ADR-0042-the-run-reports-itself-every-two-hours.md) | Telegram over stdlib Python; reports composed from repository state so a dead agent is not a missing report; an in-session two-hourly cron, with the session-only limitation accepted by the human; the stop report is a `Stop` hook because that is the case where the agent has nothing left to say |

## Metrics

Filled in when the epic closes; feeds the Product B case study.

| | |
| --- | --- |
| Tasks completed | 11, across three stories; a fourth story written and unstarted |
| Accepted on first review | **not measured** — one PR, no reviewer pass. See the deviation above |
| Average review iterations | n/a, for the same reason |
| Test lines / production lines | 75 tests over 8 modules; roughly 1.1:1 by line |
| Tasks re-scoped mid-flight | 1 (`TASK-110104` split out of `TASK-110103` when CI wiring pushed it to four files) |
| Manual human edits | 0 files; the human's contribution was the directive, three decisions and the bot token |
