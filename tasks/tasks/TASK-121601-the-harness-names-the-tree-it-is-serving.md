---
schema: 2
id: TASK-121601
title: The harness names the tree it is serving
type: task
status: ready
parent: STORY-1216
estimate: S
tier: sonnet
review: standard
files_touched: 2
labels: [process, qa, harness]
depends_on: []
verify:
  - bash -n scripts/qa/stack.sh
  - bash scripts/qa/stack.sh served
  - bash scripts/qa/stack-selftest.sh
  - python3 .github/scripts/lint_tickets.py
---

## Goal

`scripts/qa/stack.sh served` prints, for each of `:5173` and `:8080`, the filesystem root of the
checkout whose process is listening and the short commit that checkout's `HEAD` is at — so a round
can tell what it is about to drive instead of assuming it.

## Why this exists

`STORY-1216` round 1 reported `COMMIT: ea4bd05c` and drove a dev server and a duel server both
rooted in `/Users/…/.claude/worktrees/agent-af32ec8cf22f8819c`, whose branch is at `c3e1a830` —
**92** commits behind on `web-client/` and `poker-server/`. `wait-server` and `wait-web` both
passed, because both ask only whether *something* answers on the port. Nothing in the harness could
have caught it. This is `ADR-0089` §4's harness class: **no production code may be changed here.**

## Files

| File | Action |
| --- | --- |
| `scripts/qa/stack.sh` | modify |
| `scripts/qa/stack-selftest.sh` | modify |

## Scope

- Add a `served` command to `stack.sh`'s `case`, and a line for it in the `usage` heredoc.
- For each of the two ports — web `5173`, server `8080` — resolve the listening process's
  executable path or command line to a checkout root, and print one line per port:
  - `web: <abs path> <short commit>` when resolved;
  - `web: (nothing listening)` when no process holds the port;
  - `web: (unresolved)` when a process holds it but no checkout root can be read from it.
  Same three shapes for `server:`.
- **Resolution is read-only and uses no denied verb**: `lsof -ti tcp:<port> -sTCP:LISTEN` for the
  pid and `ps -o command= -p <pid>` for the command line. The checkout root is the longest prefix
  of a path in that command line that contains a `.git` entry; the commit is
  `git -C "<root>" rev-parse --short HEAD`.
- **Exit 0 whenever both lines were printed**, including when either is `(nothing listening)` or
  `(unresolved)` — this command must be runnable in CI with no stack up. Exit non-zero only when
  the command itself could not run.
- Add self-test cases to `stack-selftest.sh` in the file's existing shape: stub `lsof`, `ps` and
  `git` onto `PATH` ahead of the real ones, run `stack.sh served` as a **child** process, and
  assert on stdout.

## Out of scope

- **Refusing a round on a mismatch.** That is `TASK-121602`, which reads this command's output.
  `served` reports; it never decides.
- **Starting, stopping or moving anything.** No new process, no port change, no `strictPort` on
  `web-client/vite.config.ts` — that file is production and `ADR-0089` §4 forbids touching it for a
  harness defect (`STORY-1216` §*What is deliberately out of scope*).
- **`db-up`'s compose project**, which is `TASK-121603` and touches the same two files. Land this
  one first.

## Tests

`scripts/qa/stack-selftest.sh` — four new cases, in the file's `A1`…`A6` convention, each printing
what it proves on success and the recorded call log on failure.

| Case | Proves |
| --- | --- |
| `B1 served names the checkout and commit behind each port` | with `lsof` stubbed to a pid and `ps` stubbed to a command line under a fixture checkout, both lines carry that fixture's absolute path and the short commit `git` was asked for |
| `B2 served says so when nothing is listening` | with `lsof` stubbed to exit 1 for both ports, stdout is exactly `web: (nothing listening)` and `server: (nothing listening)`, and the exit code is **0** |
| `B3 served says so when a listener names no checkout` | with `ps` stubbed to a command line holding no path under a `.git` ancestor, both lines read `(unresolved)` and the exit code is **0** |
| `B4 served reports the two ports independently` | `lsof` stubbed to answer for `5173` and not for `8080` yields one resolved line and one `(nothing listening)` line — a fixture that answers for both cannot tell a per-port read from a single read reused twice |

**`B4` is not decoration.** A `served` that resolved `:5173` once and printed the same answer for
`:8080` would pass `B1`, `B2` and `B3`. Two inputs, or the case is vacuous.

## Acceptance criteria

- [ ] `bash scripts/qa/stack.sh served` exits 0 with **no stack running** and prints exactly two
      lines, one beginning `web: ` and one beginning `server: `.
- [ ] `bash scripts/qa/stack.sh served` today, before this ticket, exits **2** (the `usage`
      branch) — measured at `ea4bd05c` on 2026-09-10. The first `verify:` line is therefore a gate
      that is red before the diff and green after, not a gate that cannot fail.
- [ ] `scripts/qa/stack-selftest.sh` passes and contains cases `B1`, `B2`, `B3` and `B4` above.
- [ ] **The new cases are shown red against the unfixed script.** Copy `scripts/qa/stack.sh` aside
      with `cp`, restore the pre-ticket version over it, run `stack-selftest.sh`, record that
      `B1`–`B4` fail, and copy the fixed file back. Use `cp`, never `git checkout --`: a git
      restore in a worktree deletes the submission. Paste the failing output in the PR body.
- [ ] `bash -n scripts/qa/stack.sh` exits 0.
- [ ] Every command in `verify:` exits 0.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.
