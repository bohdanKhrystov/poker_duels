---
schema: 2
id: TASK-110301
title: The heartbeat sends once per window, whoever fires it
type: task
status: done
parent: STORY-1103
estimate: S
tier: haiku
review: standard
files_touched: 2
labels: [process, notifications]
depends_on: [TASK-110203]
verify:
  - python3 -m unittest discover -s scripts/notify -t scripts/notify -p test_heartbeat.py
  - grep -c "    def test_" scripts/notify/test_heartbeat.py
---

## Goal

Two things can fire a heartbeat — the agent by hand and the cron on its clock — and the human
receives exactly one message per two-hour window.

## Files

| File | Action |
| --- | --- |
| `scripts/notify/heartbeat.py` | create |
| `scripts/notify/test_heartbeat.py` | create |

## Scope

- `due(state, now, window) -> bool`: `True` when `last_report_at` is absent or older than
  `window` (default two hours).
- `beat(repo, state_path, now, sender, force=False)` composes, sends, and stamps
  `last_report_at` — **only on a successful send.** A failed send that stamps anyway suppresses
  the next two hours as well, converting one lost message into two.
- `--force` skips the due check, for the agent reporting a landing deliberately.
- `--dry-run` prints without stamping. Printing a report is not delivering one, and a preview
  that consumed the window would silently suppress the next real heartbeat for two hours.
- A malformed or future-dated `last_report_at` is treated as due. A clock that has gone backwards
  must not silence the channel indefinitely.
- Exits 0 always.

## Out of scope

- Arming the cron. That is the driver's job — `TASK-110304`.
- The stop and budget reports — `TASK-110302`.

## Tests

`test_heartbeat.py`

| Test | Proves |
| --- | --- |
| `test_first_ever_beat_is_due` | absent stamp sends |
| `test_second_beat_inside_the_window_is_not_due` | the deduplication contract |
| `test_beat_after_the_window_is_due` | the clock still runs |
| `test_force_sends_inside_the_window` | the agent can report a landing |
| `test_successful_send_stamps_the_state` | the window starts on delivery |
| `test_failed_send_does_not_stamp` | a lost message does not cost the next one |
| `test_dry_run_does_not_consume_the_window` | a preview is not a delivery |
| `test_malformed_timestamp_is_due` | a corrupt stamp does not silence the channel |
| `test_future_timestamp_is_due` | a backwards clock does not silence it either |

## Acceptance criteria

- [ ] All nine tests above pass by name
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md).
