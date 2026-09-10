---
schema: 2
id: TASK-121603
title: One database, wherever stack.sh is run from
type: task
status: ready
parent: STORY-1216
estimate: XS
tier: sonnet
review: standard
files_touched: 2
labels: [process, qa, harness]
depends_on: [TASK-121601]
verify:
  - bash -n scripts/qa/stack.sh
  - bash scripts/qa/stack-selftest.sh
  - python3 .github/scripts/lint_tickets.py
---

## Goal

`scripts/qa/stack.sh db-up`, `db-down`, `db-container` and `status` name **one** compose project
whatever checkout the script is invoked from, so a round opened in an agent worktree talks to the
database that is already up instead of trying to start a second one on a port that is taken.

## Why this exists

`compose` derives its project name from the working directory's basename, and `stack.sh` sets
`ROOT` from `${BASH_SOURCE[0]}` — so every worktree's copy drives a *different* project.
`docker-compose.yml` publishes a fixed host port, `"5432:5432"`, so the second project's postgres
cannot bind and `db-up` dies with `compose up failed`. Three postgres containers exist on the
machine `STORY-1216` ran on — `poker_duels-postgres-1`, `agent-a3397725a81f02dca-postgres-1`,
`agent-a4b7dd7713c66b740-postgres-1` — one per checkout that ever ran the verb. `qa` reported the
failure as a note and worked around it; it is step 1 of the mechanism that made round 1 void.

`ADR-0089` §4's harness class: **no production code may be changed here.** `docker-compose.yml` is
not touched — the port stays published where every merged document says it is.

## Files

| File | Action |
| --- | --- |
| `scripts/qa/stack.sh` | modify |
| `scripts/qa/stack-selftest.sh` | modify |

## Scope

- Give `compose()` an explicit project name and pass it on **every** invocation, both branches of
  the plugin/standalone fallback: `docker compose -p "$COMPOSE_PROJECT" …` and
  `docker-compose -p "$COMPOSE_PROJECT" …`.
- `COMPOSE_PROJECT` is one literal at the top of the script, beside `ROOT`. It is **not** derived
  from `ROOT`, from `basename`, or from anything a worktree changes — deriving it is the defect.
- Update the `db_container()` comment, which currently explains that the name is derived and must
  not be written down. That explanation is now false and a stale comment is worse than none.
- Add a self-test case in the file's existing shape asserting the recorded call log.

## Out of scope

- **`docker-compose.yml`.** Not opened. Neither the fixed host port nor the volume name moves.
- **Cleaning up the two orphaned containers.** They are a human's to remove; `rm` and every kill
  verb are denied here, and a script that tore down a database it did not start would be a worse
  defect than the one being fixed.
- **The `served` command** — `TASK-121601`, which touches the same two files. Land that first.

## Tests

`scripts/qa/stack-selftest.sh` — two new cases, in the `A1`…`A6` convention.

| Case | Proves |
| --- | --- |
| `C1 every compose call names the project explicitly` | across `db-up`, `db-down`, `db-container` and `status`, **every** recorded line in the stub's call log carries `-p poker_duels`; a count of matching lines equal to the total number of recorded compose lines, never a presence check |
| `C2 the project name does not follow the checkout` | the same assertion holds when `stack.sh` is invoked through a symlink placed in a differently named temporary directory — a fixture run only from the repository root cannot tell a literal from a derivation |

**`C2` is the one that matters.** `C1` alone passes against a `COMPOSE_PROJECT="$(basename
"$ROOT")"` that happens to evaluate to `poker_duels` in the checkout the test runs in — which is
exactly today's defect wearing a new name.

## Acceptance criteria

- [ ] `scripts/qa/stack-selftest.sh` passes and contains cases `C1` and `C2` above.
- [ ] `C1` asserts a **count** — recorded compose lines carrying `-p poker_duels` equals total
      recorded compose lines — not the presence of one matching line.
- [ ] **Both new cases are shown red against the unfixed script.** `cp` the fixed
      `scripts/qa/stack.sh` aside, restore the pre-ticket version, run the self-test, record that
      `C1` and `C2` fail, then `cp` the fixed file back. Use `cp`, never `git checkout --`. Paste
      the failing output in the PR body.
- [ ] `git diff --exit-code docker-compose.yml` exits 0 — this ticket does not modify it.
- [ ] `bash -n scripts/qa/stack.sh` exits 0.
- [ ] Every command in `verify:` exits 0.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.
