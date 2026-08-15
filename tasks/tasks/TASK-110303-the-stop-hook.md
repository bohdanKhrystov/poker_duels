---
schema: 2
id: TASK-110303
title: The Stop hook, and the one line that registers it
type: task
status: done
parent: STORY-1103
estimate: S
tier: haiku
review: standard
files_touched: 3
labels: [process, notifications]
depends_on: [TASK-110302]
verify:
  - bash scripts/notify/hooks/stop_hook.sh < /dev/null
  - python3 -m unittest discover -s scripts/notify -t scripts/notify -p test_hook.py
  - grep -q "settings.local.json" docs/notifications.md
---

## Goal

When the run stops, the report is sent by the harness rather than by an agent that may have
nothing left to say.

## Files

| File | Action |
| --- | --- |
| `scripts/notify/hooks/stop_hook.sh` | create |
| `scripts/notify/test_hook.py` | create |
| `docs/notifications.md` | edit |

## Scope

- `stop_hook.sh` reads the hook payload on stdin, `cd`s to the repository root resolved from its
  own location — never from `$PWD`, since a hook's working directory is not guaranteed — and
  calls `notify.py stop`.
- **Exits 0 on every path**: empty stdin, non-JSON stdin, missing `notify.py`, missing Python. A
  non-zero `Stop` hook interferes with the session it is reporting on, which is a worse outcome
  than a missed message.
- It sends only when a run is in flight — `.claude/run-state.json` exists and carries a
  `current_epic`. Otherwise it exits 0 silently. Without this the human gets a Telegram message
  every time any Claude Code turn ends in this repository, and the channel becomes noise inside a
  day.
- `docs/notifications.md` gains the registration block for `.claude/settings.local.json`,
  verbatim and pasteable, and states why it is not in `.claude/settings.json`: that file is
  deny-listed to the agent by this repository's own permissions, and the rule is not worth
  weakening for a notifier.

## Out of scope

- Registering the hook. The document carries the line; a human pastes it. An agent that edits
  its own permission files to grant itself a capability is the pattern this repository's deny
  list exists to prevent.
- A `SessionEnd` hook. `Stop` covers the case that matters; a second hook doubles the messages.

## Tests

`test_hook.py`

| Test | Proves |
| --- | --- |
| `test_hook_exits_zero_on_empty_stdin` | the degenerate payload |
| `test_hook_exits_zero_on_malformed_json` | a payload shape change cannot break the session |
| `test_hook_is_silent_without_run_state` | no message when no run is in flight |
| `test_hook_is_silent_when_run_state_has_no_epic` | a stale breadcrumb is not a run |
| `test_hook_invokes_stop_when_a_run_is_in_flight` | the wiring actually calls `notify.py stop` |
| `test_hook_resolves_the_repo_from_its_own_path` | it works when invoked from another directory |

## Acceptance criteria

- [ ] All six tests above pass by name
- [ ] `bash scripts/notify/hooks/stop_hook.sh < /dev/null` exits 0
- [ ] `docs/notifications.md` contains the `.claude/settings.local.json` block
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md).
