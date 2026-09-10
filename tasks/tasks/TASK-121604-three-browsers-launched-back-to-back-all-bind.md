---
schema: 2
id: TASK-121604
title: Three browsers launched back to back all bind
type: task
status: backlog
parent: STORY-1216
estimate: XS
tier: haiku
review: light
files_touched: 1
labels: [process, qa, harness]
depends_on: [TASK-121601]
verify:
  - bash -n scripts/qa/stack.sh
  - bash scripts/qa/stack-selftest.sh
  - python3 .github/scripts/lint_tickets.py
---

## Goal

`scripts/qa/stack.sh chrome-up` either brings the browser up or fails saying so, and stops
sometimes failing to bind when three are launched back to back and succeeding on isolated retry.

## Why this is backlog rather than in the fix set

`qa` filed it as a note, called it local contention, and worked around it by retrying. It is real
and it is small; it is not what cost `STORY-1216` round 1, and scheduling it out of a symptom would
be work manufactured. `EPIC-12` §Termination rule 2's spirit, applied to a harness ticket: repair
what justified the interruption, file the rest.

`ADR-0089` §4's harness class: **no production code may be changed here.**

## Files

| File | Action |
| --- | --- |
| `scripts/qa/stack.sh` | modify |

## Scope

- `chrome-up` today launches Chrome into the background and then polls `/json/version` for 30
  seconds. If Chrome exits during launch, the poll simply times out and the message blames the
  port. Distinguish the two: if the launched process is gone before the port ever answers, say
  **that**, and say which port and which profile directory.
- Retry the launch **once** on that specific failure, after a short delay, before dying — the
  observed behaviour is that an isolated retry succeeds.
- Do not add a global serialisation or a lock. Three browsers per round is the documented shape and
  a lock would make a whole round wait on one slow start.

## Out of scope

- `chrome-down`, which works and uses CDP `Browser.close` rather than a denied verb.
- Any change to the number of browsers a round allocates.

## Tests

`scripts/qa/stack-selftest.sh` — one new case, in the `A1`…`A6` convention. The existing stub
directory already puts a fake `curl` on `PATH`; add a fake `chrome` launcher the same way.

| Case | Proves |
| --- | --- |
| `D1 chrome-up retries once and then reports the launch, not the port` | with the stub launcher failing on its first call and succeeding on its second, `chrome-up` exits 0; with it failing on both, `chrome-up` exits non-zero and its message names the profile directory rather than only the port |

Two inputs, not one: a case that only proved the retry could not tell a retry from a launcher that
never failed.

## Acceptance criteria

- [ ] `scripts/qa/stack-selftest.sh` passes and contains case `D1`.
- [ ] `D1` is shown red against the unfixed script — `cp` aside, restore, run, `cp` back. Use `cp`,
      never `git checkout --`.
- [ ] `bash -n scripts/qa/stack.sh` exits 0.
- [ ] Every command in `verify:` exits 0.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.
