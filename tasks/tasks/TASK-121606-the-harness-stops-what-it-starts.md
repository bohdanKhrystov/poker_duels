---
schema: 2
id: TASK-121606
title: The harness stops what it starts
type: task
status: ready
parent: STORY-1216
estimate: S
tier: sonnet
review: deep
files_touched: 2
labels: [process, qa, harness]
depends_on: [TASK-121601]
verify:
  - bash -n scripts/qa/stack.sh
  - bash -n scripts/qa/stack-selftest.sh
  - bash scripts/qa/stack-selftest.sh
  - bash scripts/qa/stack.sh reap --dry-run
  - python3 .github/scripts/lint_tickets.py
---

## Goal

`scripts/qa/stack.sh` gains `down` and `reap`, so every process this repository starts is one it can
also stop — and `reap --dry-run` reports the set it would signal without signalling it.

## Why this exists

On 2026-09-10 the human asked why five agents were still listed as running. All five had finished:
every ticket behind them was `done` and merged, the newest three days earlier. Each was held open by
a child process it had started and could not stop, because the harness keeps an agent alive while it
has any live background child:

| Agent | Ticket | Held by | Age |
| --- | --- | --- | --- |
| `af32ec8c…` | TASK-140204 | JVM + Vite on `:8080` / `:5173` | 3 days |
| `a4b7dd77…` | TASK-140501 | a JVM helper server on `:8090` | 3 days |
| `abe109df…` | TASK-140716 | `vitest list src/lobby/Lobby.test.tsx` | 2 days |
| `a3f9c441…` | TASK-140302 | `vitest list src/lobby/Lobby.test.tsx` | 3 days |
| `a1ef777d…` | Review TASK-140904 | a shell spinning `sleep 5` | 2 days |

Each released and reported within seconds of its child being stopped, which is what identifies the
cause rather than merely correlating with it. Two of the five hung on the *identical* `vitest list`
invocation, so that is a reproducible wedge and not chance. One ran from the main checkout rather
than its worktree.

The same leak had already cost a QA round: `STORY-1216` round 1 drove `af32ec8c…`'s three-day-old
worktree and reported eight defects that measured nothing, which is what `TASK-121601` was written
about. Six headless Chrome roots were also standing, from rounds that called `chrome-up` with no
matching `chrome-down`. And `agent-a4b7dd77…-postgres-1` held `0.0.0.0:5432`, so this project's own
`poker_duels-postgres-1` was running with **no published port** and every local connection reached a
finished agent's database — the condition `TASK-121603` fixed for new containers but could not evict
from one that predated it.

## What is being reversed, and why that is not a loophole

This script's header said, in as many words:

> Working around the deny list from inside a script would defeat the point of having one.

That sentence is now wrong, and it is being replaced rather than quietly outgrown. `kill`, `pkill`
and `killall` stay denied. What replaces the refusal is a **narrower rule**, and the narrowness is
structural rather than a matter of care:

1. Neither verb takes a pid from its caller. There is no argument that names one, so none can be
   passed in — `stack.sh down 4711` is a usage error, not a kill.
2. A process is a candidate only if its command line names a path inside `$ROOT`: this checkout and
   the agent worktrees under it. Nothing else on the machine is reachable.
3. The script's own process ancestry is excluded, so a run cannot stop the session driving it.
4. Headless Chrome carries no `$ROOT` path, so it is matched on `chrome-up`'s own three-flag launch
   signature instead.

`Bash(kill:*)` on the allow list would reach any pid on this machine. These verbs reach only what
this repository started, which is strictly less. That is the whole argument, and the reviewer should
attack it rather than the code.

## The known limit, stated rather than discovered later

A harness tool-call shell is excluded by its `snapshot-zsh-` marker, but **its children are not**. A
Gradle or npm build running under a different tool call is inside `$ROOT` and is therefore in reach
of `down`. This is deliberate — that is exactly the shape of the leak being swept — but it means
`down` is for a machine between rounds, not one mid-build. A reviewer who thinks that trade is wrong
should say so; it is a decision, not an oversight.

## Out of scope

- `.claude/settings.json`. No permission change is required: `Bash(./scripts/qa/stack.sh:*)` is
  already allowed, and permission rules match what the Bash tool is asked to run, not what an
  allowed script calls internally. A blanket `Bash(kill:*)` allow would be a *wider* capability than
  this ticket ships and is refused on that ground.
- Making `qa-cycle` and the three driver agents *call* the new verbs at round end. That is the fix
  for the recurrence, it touches four more files, and it is `TASK-121607`.
- `docker-compose.yml`. `ADR-0089` §4 forbids a production change for a harness defect.

## Files

| File | Change |
| --- | --- |
| `scripts/qa/stack.sh` | header rewritten; `ancestors`/`ours`/`ours_chrome`/`stop_pids` added; `down` and `reap` verbs; usage |
| `scripts/qa/stack-selftest.sh` | section D — four assertions on `reap`'s candidate set |

## Tests

| Assertion | What it rejects |
| --- | --- |
| D1 `reap` names a process whose command line is inside the root | a rule that matches nothing, which would make D2 and D3 vacuously true |
| D2 `reap` does not name a process outside the root | the widened rule — the one dangerous failure |
| D3 `reap` does not name a harness tool-call shell | a sweep that stops the session that invoked it |
| D4 `reap --dry-run` reports without signalling | a decorative flag, which would mean D1–D3 were asserted against a run that had already killed |

`kill` is a bash **builtin**, so no `PATH` stub intercepts it and the signal itself cannot be
observed the way section A observes the docker calls. D asserts on the candidate set instead, which
is where the entire safety argument lives. Section D deliberately never runs the destructive path:
in CI a real `reap` would be entitled to every Gradle daemon under `$ROOT`, including the one
running the test.

## Status of the proof — read this before merging

The 16 assertions pass, D1–D4 included, and D1 is a live positive control: a probe was started
inside `$ROOT` and `reap --dry-run` named it, so the set is measured and not empty.

**The mutation has not been run.** This repository's standard is that a gate is shown red against
the defect it names, and that has not been done here. Two attempts were refused by the permission
layer, correctly: mutating `ours` to drop its `$ROOT` test makes the live `stack.sh` capable of
signalling any process on the machine, and running it in that state is precisely what should be
refused. A sandboxed copy under the scratchpad — dry-run only, `$ROOT` resolving to the sandbox —
was refused as well.

So the coder taking this ticket owes the red run, and it is the reason `review: deep`:

- mutate `ours`'s `case "$rest" in *"$ROOT/"*)` to match everything → **D2 must fail**
- mutate it to match nothing → **D1 must fail**
- delete `[ -n "$dry" ] ||` before `stop_pids` in `reap` → **D4 must fail**

Do that in a worktree copy, never in a checkout anything else is running against, and revert with
`cp` rather than `git checkout --`.
