---
schema: 2
id: TASK-110201
title: The run-state breadcrumb the agent stamps
type: task
status: done
parent: STORY-1102
estimate: S
tier: haiku
review: standard
files_touched: 2
labels: [process, notifications]
depends_on: [TASK-110104]
verify:
  - python3 -m unittest discover -s scripts/notify -t scripts/notify -p test_run_state.py
  - grep -c "    def test_" scripts/notify/test_run_state.py
---

## Goal

The three facts a report needs and the repository cannot know — which epics are being worked,
when the last report went out, and whether the resume cron was armed — survive between agent
turns in a file that no reader may assume exists.

## Files

| File | Action |
| --- | --- |
| `scripts/notify/run_state.py` | create |
| `scripts/notify/test_run_state.py` | create |

## Scope

- `load(path) -> RunState` and `save(path, state)`, JSON at `.claude/run-state.json`.
- Fields, **every one optional**: `epics` (list of ids), `current_epic`, `current_story`,
  `last_report_at` (ISO-8601 UTC), `cron_armed` (`True`, `False`, or absent meaning unknown),
  `started_at`, `note`.
- `load` returns an empty `RunState` for a missing file, an empty file, malformed JSON, or JSON
  that is not an object. It raises nothing: a corrupt breadcrumb must degrade the report, never
  prevent it.
- `save` writes atomically — temp file in the same directory, then `os.replace` — because a
  report reading a half-written file is exactly the race this is used in.
- `save` creates the parent directory if absent.
- `stamp_report(path, when)` updates only `last_report_at`, preserving every other field, so a
  heartbeat cannot erase the cron flag.
- Add `.claude/run-state.json` to `.gitignore`. *(Counted in the two files: the ignore entry is
  a one-line edit and the ticket would otherwise leak a state file into every diff.)*

## Out of scope

- Deciding *when* a report is due — `TASK-110301` owns the window arithmetic.
- Any reading of `BOARD.md` — `TASK-110202`.

## Tests

`test_run_state.py`

| Test | Proves |
| --- | --- |
| `test_missing_file_loads_empty` | a first run on a fresh machine still reports |
| `test_malformed_json_loads_empty` | a corrupt breadcrumb degrades rather than raises |
| `test_json_array_loads_empty` | valid JSON of the wrong shape is handled too |
| `test_round_trips_every_field` | save then load returns what went in |
| `test_cron_armed_false_is_not_lost_as_unknown` | `False` survives the round trip distinctly from absent |
| `test_stamp_report_preserves_other_fields` | stamping a heartbeat does not erase `cron_armed` |
| `test_save_creates_the_parent_directory` | a machine with no `.claude/` still works |

## Acceptance criteria

- [ ] `test_missing_file_loads_empty` passes
- [ ] `test_malformed_json_loads_empty` passes
- [ ] `test_json_array_loads_empty` passes
- [ ] `test_round_trips_every_field` passes
- [ ] `test_cron_armed_false_is_not_lost_as_unknown` passes
- [ ] `test_stamp_report_preserves_other_fields` passes
- [ ] `test_save_creates_the_parent_directory` passes
- [ ] `.gitignore` contains `.claude/run-state.json`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md).
