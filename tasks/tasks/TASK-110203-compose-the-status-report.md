---
schema: 2
id: TASK-110203
title: Compose the status report, degrading section by section
type: task
status: done
parent: STORY-1102
estimate: S
tier: sonnet
review: standard
files_touched: 2
labels: [process, notifications]
depends_on: [TASK-110202]
verify:
  - python3 -m unittest discover -s scripts/notify -t scripts/notify -p test_report.py
  - python3 scripts/notify/notify.py report
  - grep -c "    def test_" scripts/notify/test_report.py
---

## Goal

`scripts/notify/report.py` builds the four-section message — done, in progress, blocked, budget —
from repository state, and produces a report on a machine where nothing works.

## Files

| File | Action |
| --- | --- |
| `scripts/notify/report.py` | create |
| `scripts/notify/test_report.py` | create |

## Scope

- `compose(repo, state, now, runner) -> str`, sections in this order:
  **DONE** (commits on `develop` since `state.last_report_at`, or the last 2h when absent),
  **IN PROGRESS** (open PRs from `gh pr list`, plus `in-progress`/`in-review` tickets),
  **BLOCKED** (blocked tickets with their `DEC-NNN`), **BUDGET** (the armed-cron line).
- `runner` is an injected callable running a subprocess, so every test is offline. Default runs
  `git` and `gh` with a short timeout.
- **Each section degrades alone.** No `gh` on the box, `gh` not authenticated, a git repository
  with no commits, a missing board — each replaces its own section with one honest line naming
  what was unavailable, and leaves the others intact.
- The cron line reads `resume cron: armed` / `not armed` / `unknown` from `state.cron_armed`.
  Absent is `unknown` and is never rendered as `not armed` — the two mean different things and
  conflating them is the failure this section exists to prevent.
- The report opens with a header naming the repository, the current epic from the run state, and
  the timestamp.
- Longer than 4096 characters, it drops whole trailing sections, appends a line saying which were
  dropped, and never splits a section mid-list.

## Out of scope

- Sending — `STORY-1101` owns the wire; `compose` returns a string.
- Per-check CI status. `gh pr list` gives open PRs; per-check state is more API calls than a
  status message justifies.

## Tests

`test_report.py`

| Test | Proves |
| --- | --- |
| `test_all_four_sections_are_present` | the contract of the message shape |
| `test_commits_since_last_report_are_listed` | the DONE section uses the stamp, not a fixed window |
| `test_missing_gh_degrades_only_its_section` | the other three still render |
| `test_git_failure_degrades_only_its_section` | same, from the other side |
| `test_blocked_section_names_the_decision` | `DEC-036` reaches the message |
| `test_cron_armed_true_renders_armed` | the positive case |
| `test_cron_armed_false_renders_not_armed` | the negative case |
| `test_cron_armed_absent_renders_unknown` | absent is never `not armed` |
| `test_oversized_report_drops_whole_sections` | truncation respects section boundaries |
| `test_oversized_report_says_what_it_dropped` | the human is told the message is partial |
| `test_composes_with_empty_state_and_no_tools` | a report exists when nothing is available |

## Acceptance criteria

- [ ] All eleven tests above pass by name
- [ ] `python3 scripts/notify/notify.py report` prints a report and exits 0
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md).
