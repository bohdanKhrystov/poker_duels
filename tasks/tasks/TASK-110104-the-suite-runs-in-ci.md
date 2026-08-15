---
schema: 2
id: TASK-110104
title: Run the notifier suite in CI
type: task
status: done
parent: STORY-1101
estimate: XS
tier: haiku
review: light
files_touched: 1
labels: [process, notifications, ci]
depends_on: [TASK-110103]
verify:
  - python3 -m unittest discover -s scripts/notify -t scripts/notify -p 'test_*.py'
  - grep -q "unittest discover -s scripts/notify" .github/workflows/tickets.yml
---

## Goal

The notifier's tests run on every pull request, in the job that already has Python.

## Files

| File | Action |
| --- | --- |
| `.github/workflows/tickets.yml` | edit |

## Scope

- Add one step to the existing `lint backlog` job, after the ticket lint:
  `python3 -m unittest discover -s scripts/notify -t scripts/notify -p 'test_*.py'`.
- The job already pins Python 3.12 and checks out the repository; add nothing else.
- No credentials in CI. Every test in this suite is offline by construction — the transport is
  injected — so a CI run that could send a real message would mean a test that talks to the
  network, which is a defect rather than a feature.

## Out of scope

- A separate workflow or a separate job. This suite is a few hundred milliseconds; a second
  runner costs more in queue time than it saves.
- Coverage measurement.

## Tests

No new test file. The verify block is the test: the whole suite must pass, and the workflow must
actually contain the line that runs it.

## Acceptance criteria

- [ ] `python3 -m unittest discover -s scripts/notify -t scripts/notify -p 'test_*.py'` exits 0
      and reports more than zero tests
- [ ] `.github/workflows/tickets.yml` contains the discover command
- [ ] The `lint backlog` job is green on the pull request
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md).
