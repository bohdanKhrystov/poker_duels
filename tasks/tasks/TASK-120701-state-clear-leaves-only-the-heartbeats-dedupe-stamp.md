---
schema: 2
id: TASK-120701
title: state --clear leaves only the heartbeat's dedupe stamp
type: task
status: done
parent: STORY-1207
estimate: S
tier: sonnet
review: standard
files_touched: 3
labels: [process, qa, uat, meta]
depends_on: []
verify:
  - python3 .github/scripts/lint_tickets.py
  - python3 -m unittest discover -s scripts/notify -t scripts/notify -p 'test_*.py'
  - cd scripts/notify && python3 -m unittest test_cli.StateClearTest
  - cd scripts/notify && python3 -m unittest test_cli.StateClearTest.test_clear_still_removes_the_current_epic test_cli.StateClearTest.test_clear_leaves_the_cron_flag_unknown test_cli.StateClearTest.test_clear_drops_a_note_from_an_earlier_run test_cli.StateClearTest.test_clear_keeps_the_heartbeat_dedupe_stamp test_cli.StateClearTest.test_clear_leaves_only_the_dedupe_stamp
  - python3 -m unittest discover -s scripts/notify -t scripts/notify -p 'test_cli.py' 2>&1 | grep -q "^Ran 13 tests"
  - awk '/^## /{s=0} /^## The run-state breadcrumb$/{s=1} s && index($0,"last_report_at"){f=1} END{exit f?0:1}' docs/notifications.md
---

## Goal

After `python3 scripts/notify/notify.py state --clear`, `.claude/run-state.json` holds
`last_report_at` and nothing else — so no field of a finished run can be read as a fact about the
next one.

## The defect, measured

2026-08-30, immediately after a `/qa-cycle regression` teardown whose last step was
`notify.py state --clear`. `.claude/run-state.json` then read:

```json
{"cron_armed": true,
 "epics": ["EPIC-12"],
 "last_report_at": "2026-08-29T21:11:36.245377+00:00",
 "note": "qa-cases built and running; EPIC-04 suite in review (PR#1169), EPIC-05 next"}
```

- **`current_epic` is gone, and that is the half that works.** It is what silences the `Stop`
  hook. **Any change that breaks it is worse than the bug** — `test_clear_still_removes_the_current_epic`
  exists to pin it.
- **`cron_armed` stayed `true`** while the heartbeat cron had just been deleted in the same
  teardown. The whole value of that flag is that it can be trusted:
  `.claude/skills/qa-cycle/SKILL.md` says to pass `--cron-armed armed` *"only once `CronCreate`
  has returned successfully, never optimistically"*. A clear that leaves it set defeats the same
  care at the other end of the run.
- **`note` survived from an earlier session entirely** — it describes `qa-cases` work and a PR
  that merged the day before. A stale note is worse than none, because the next reader has no way
  to tell it is stale.

## What `--clear` keeps, and why — this is decided, not open

**It keeps `last_report_at`, and removes every other field**, including `epics` and `started_at`.

- **`last_report_at` stays** because it is cross-run bookkeeping rather than a fact about the run:
  it is the heartbeat's dedupe stamp (`run_state.stamp_report`, and `heartbeat.py`'s window).
  Clearing it would let the next run's first heartbeat fire immediately as though none had ever
  been sent — a duplicate report about a run that has ended. That is a different bug wearing this
  ticket's number, so do not clear it.
- **`cron_armed` becomes `None`, meaning *unknown* — never `False`.** `--clear` cannot know
  whether its caller has deleted the cron yet; `qa-cycle`'s teardown runs `CronDelete` *after*
  `state --clear`. `run_state.py` documents the three-state design for exactly this: *"True,
  False, or None meaning unknown — three states, never two. A report that renders an unknown cron
  as 'not armed' tells the human the run is over when it may not be."* Writing `False` would be
  the optimism the arming end forbids, pointed the other way.
- **`epics` goes** because `lifecycle.py` renders it as `still queued: …` in a budget report, and
  a queue attributed to no run is the same stale breadcrumb as the note.
- **`started_at` goes** for the same reason: it dates a run that has ended.

`run_state.save` omits every field whose value is `None`, so setting a field to `None` removes the
key from the JSON rather than writing a null. `epics` defaults to `[]`, which `save` also drops.

## Files

| File | Action |
| --- | --- |
| `scripts/notify/notify.py` | modify — `cmd_state`'s `if args.clear:` branch only |
| `scripts/notify/test_cli.py` | modify — one new `StateClearTest` class |
| `docs/notifications.md` | modify — one sentence in §*The run-state breadcrumb* |

You may **read** `scripts/notify/run_state.py` (the `FIELDS` tuple and `save`) and
`scripts/notify/test_run_state.py` (the temp-path pattern the new tests follow).

## Scope

- In `cmd_state`, the `if args.clear:` branch clears **every** field except `last_report_at`:
  `current_epic`, `current_story`, `epics`, `cron_armed`, `started_at`, `note`. Derive the list
  from `run_state.FIELDS` rather than retyping it, so a field added later is cleared by default —
  a new breadcrumb that survives a clear is this bug again.
- `--clear` still wins over every other flag in the same invocation, exactly as today: it is
  applied last, so `state --epic EPIC-12 --clear` clears.
- Add `StateClearTest` to `scripts/notify/test_cli.py` with the five methods named in *Tests*,
  each patching `notify.STATE_PATH` to a `tempfile.TemporaryDirectory()` path. **No test may write
  the repository's own `.claude/run-state.json`** — a live cycle may be reading it.
- In `docs/notifications.md` §*The run-state breadcrumb*, replace the half-sentence *"`--clear` at
  the end so the stop hook falls silent again"* with one that also says what survives: the clear
  removes every field except `last_report_at`, the heartbeat's dedupe stamp.

## Out of scope

- **Clearing `last_report_at`.** Decided above. A patch that clears it fails review even with
  green tests, because `test_clear_keeps_the_heartbeat_dedupe_stamp` would have to be deleted to
  land it.
- **Setting `cron_armed` to `False`.** Decided above: `None` is *unknown* and that is the honest
  value at clear time.
- **Any change to `run_state.py`.** `save`'s drop-None behaviour and `FIELDS` are correct and are
  what makes this a three-line fix. Adding a `clear()` helper there is a wider diff for no gate.
- **Any change to the `Stop` hook, `heartbeat.py`, `lifecycle.py` or `report.py`.** The hook's
  silence already works and this ticket must not touch what makes it work.
- **Any change to `qa-cycle`'s teardown order.** `heartbeat --force` first, `state --clear` last
  is correct and is not what failed here.
- **Adding a `--clear` flag to any other subcommand**, or a `state --show`. Not asked for.
- **Any new test module.** `test_cli.py` is where CLI-level tests already live and CI already
  discovers it.

## Tests

`StateClearTest` in `scripts/notify/test_cli.py`. Every test stamps a state, runs
`notify.main(["state", "--clear"])`, and reads the file back.

| Test | Proves |
| --- | --- |
| `test_clear_still_removes_the_current_epic` | the half that already works keeps working — `current_epic` is absent after a clear, so the `Stop` hook falls silent |
| `test_clear_leaves_the_cron_flag_unknown` | `cron_armed` is not `True` after a clear — the measured defect |
| `test_clear_drops_a_note_from_an_earlier_run` | `note` is absent after a clear — the measured defect |
| `test_clear_keeps_the_heartbeat_dedupe_stamp` | `last_report_at` is unchanged after a clear, character for character — the decision above, pinned so a later "tidy up" cannot quietly take it |
| `test_clear_leaves_only_the_dedupe_stamp` | the JSON's key set after a clear is exactly `{"last_report_at"}` — written first through `run_state.save` with **all seven** fields set to distinct non-default values, `started_at` included, because the CLI has no flag for that one |

**Set every field before clearing, never a subset.** A fixture that leaves a field at its default
cannot tell "the clear removed it" from "it was never there" — the fifth test is the one that
would otherwise pass vacuously, and it is the strong one.

## Acceptance criteria

- [ ] `StateClearTest.test_clear_still_removes_the_current_epic` passes.
- [ ] `StateClearTest.test_clear_leaves_the_cron_flag_unknown` passes.
- [ ] `StateClearTest.test_clear_drops_a_note_from_an_earlier_run` passes.
- [ ] `StateClearTest.test_clear_keeps_the_heartbeat_dedupe_stamp` passes.
- [ ] `StateClearTest.test_clear_leaves_only_the_dedupe_stamp` passes.
- [ ] `test_cli.py` runs **13** tests — 8 today plus these 5. Measured on 2026-08-30 at commit
      `cfcc6a4e`: `Ran 8 tests`.
- [ ] `docs/notifications.md` §*The run-state breadcrumb* names `last_report_at` as what survives.
- [ ] The whole `scripts/notify` suite is green, which is what `tickets.yml` runs.
- [ ] Every command in `verify:` exits 0.

**On gate 4 and gate 5.** Naming the class and the five methods as unittest ids is a real gate: a
missing class or method is an `AttributeError` and exit 1 — measured on 2026-08-30, where
`python3 -m unittest test_cli.StateClearTest` exits **1** today. `-k` is **not** used anywhere
here: `python3 -m unittest discover … -k NoSuchTest` prints `Ran 0 tests … OK` and exits **0**,
so a filter would be a gate that cannot fail. Gate 5's count is piped into `grep -q`, so the
pipeline's status is grep's — deliberate, and it is why gate 2 runs the suite *unpiped* on its own
line: a failing test still prints `Ran 13 tests`, so the count gate alone would not see a red run.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
