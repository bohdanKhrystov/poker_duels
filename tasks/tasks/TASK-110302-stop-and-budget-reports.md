---
schema: 2
id: TASK-110302
title: The stop and budget reports, and the cron line
type: task
status: done
parent: STORY-1103
estimate: S
tier: haiku
review: standard
files_touched: 2
labels: [process, notifications]
depends_on: [TASK-110301]
verify:
  - python3 -m unittest discover -s scripts/notify -t scripts/notify -p test_lifecycle.py
  - python3 scripts/notify/notify.py budget --cron-armed unknown --dry-run
  - grep -c "    def test_" scripts/notify/test_lifecycle.py
---

## Goal

The three event reports exist as commands: `stop`, `blocked` and `budget` — and the budget report
never goes out without saying what happened to the resume cron.

## Files

| File | Action |
| --- | --- |
| `scripts/notify/lifecycle.py` | create |
| `scripts/notify/test_lifecycle.py` | create |

## Scope

- `notify.py stop [--reason …]` — a headline, the reason when given, and the same DONE / BLOCKED
  sections as the heartbeat, so a stop report is self-contained.
- `notify.py blocked --decision DEC-NNN [--question …]` — what is parked and what it waits on.
- `notify.py budget --cron-armed armed|not-armed|unknown [--reset-at …]` — the short-on-tokens
  report. `--cron-armed` is **required**; omitting it is a usage error, exit 2. A budget report
  without that line cannot tell the human whether the run resumes itself, which is its whole
  purpose, so the mistake is made unmakeable rather than defaulted.
- `--cron-armed` also writes `cron_armed` into the run state, so a later heartbeat repeats it.
- `--dry-run` prints the message and sends nothing, so the verify block proves the wording
  offline.
- `stop` and `blocked` exit 0 always. `budget` exits 0 once its arguments parse.

## Out of scope

- The hook that calls `stop` — `TASK-110303`.
- Detecting a budget shortage. Nothing can measure that from outside the agent; the agent calls
  this when it sees the warning, and `TASK-110304` writes that duty into the skill.

## Tests

`test_lifecycle.py`

| Test | Proves |
| --- | --- |
| `test_stop_report_names_the_reason` | a given reason reaches the message |
| `test_stop_report_without_a_reason_still_sends` | the hook path has no reason to give |
| `test_stop_report_carries_done_and_blocked` | it is self-contained |
| `test_blocked_report_names_the_decision` | `DEC-NNN` is in the text |
| `test_budget_requires_the_cron_flag` | omitting `--cron-armed` exits 2 |
| `test_budget_renders_armed` | positive case |
| `test_budget_renders_not_armed` | negative case |
| `test_budget_renders_unknown` | the third state is distinct from the second |
| `test_budget_writes_cron_armed_into_run_state` | a later heartbeat can repeat it |
| `test_dry_run_sends_nothing` | the verify command cannot message the human |

## Acceptance criteria

- [ ] All ten tests above pass by name
- [ ] `notify.py budget` with no `--cron-armed` exits 2
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md).
