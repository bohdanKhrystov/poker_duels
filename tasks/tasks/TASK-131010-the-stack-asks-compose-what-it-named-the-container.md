---
schema: 2
id: TASK-131010
title: The stack asks compose what it named the container
type: task
status: done
parent: STORY-1310
module: web-client
estimate: S
tier: sonnet
review: standard
files_touched: 3
labels: [qa, harness, refresh, infra]
depends_on: [TASK-131002]
verify:
  - bash scripts/qa/stack-selftest.sh
  - awk '{ n += gsub(/poker_duels-postgres-1/, "&") } END { exit (n > 0) }' docs/test-plan.md
  - awk '{ n += gsub(/db-container/, "&") } END { exit (n < 1) }' docs/test-plan.md
  - awk '{ n += gsub(/SELECT b\.device_id/, "&") } END { exit (n < 1) }' docs/test-plan.md
  - awk '/^---$/ { fm++; next } fm == 1 && /^verify:/ { inv = 1; next } fm == 1 && inv && /^ / { if (index($0, "drive" ".mjs") || index($0, "stack" ".sh")) { print FILENAME ": " $0; bad = 1 } next } fm == 1 { inv = 0 } END { exit bad ? 1 : 0 }' tasks/tasks/TASK-1310*.md
  - awk '/^\| `P[1-6]/ { n++ } END { exit (n != 7) }' tasks/stories/STORY-1310-the-refresh-paths-nobody-drove.md
  - python3 .github/scripts/lint_tickets.py
---

## Goal

`scripts/qa/stack.sh db-up` succeeds in the checkout it is actually run from, because the postgres
container's name is asked of `compose` rather than written down — so every drive in this story, and
every QA round an agent runs from a worktree, can bring the database up at all.

## The defect, measured on 2026-09-04 in this worktree

`scripts/qa/stack.sh:23` reads `DB_CONTAINER="poker_duels-postgres-1"`. `compose` names containers
after the **project**, which defaults to the checkout directory's basename. Run from
`.claude/worktrees/agent-a73380fa3b5353c90`, `compose up -d` printed:

```
 Container agent-a73380fa3b5353c90-postgres-1 Created
 Container agent-a73380fa3b5353c90-postgres-1 Started
```

so `db-up`'s readiness loop calls `docker exec poker_duels-postgres-1 pg_isready` against a
container that does not exist, sixty times, and then dies with *"database never accepted
connections"* — a message that names the wrong cause. **`db-up` cannot succeed in any worktree.**
`status` has the same literal on line 123 and reports `db: down` against a database that is up.

`TASK-131003`'s coder hit this at the first command of its drive and changed no files. Its
acceptance says *"Who runs the measurement: the implementer, before opening the PR, on a running
stack"*, so the script has to work where the implementer runs, and today it does not.

## Which derivation, judged rather than assumed

Three candidates were run against a live daemon in this worktree. The measurements, not the
reasoning, decide it:

| candidate | measured | verdict |
| --- | --- | --- |
| `compose ps -q postgres` | prints the container id; **exits 0 with empty stdout when nothing is up**, so it is safe under `set -euo pipefail` in a command substitution | **take this one** |
| `compose exec -T postgres pg_isready …` | works, and removes the name entirely — but pays compose's service resolution on **every** poll of a sixty-iteration loop instead of once | rejected: same answer, slower loop, and it leaves no id for `docs/test-plan.md` to name |
| the directory basename | re-implements compose's project-name normalisation from the outside, and is wrong the moment anyone sets `COMPOSE_PROJECT_NAME`, passes `-p`, adds a top-level `name:` to `docker-compose.yml`, or checks out into a directory with a character compose strips | rejected |

**"Needs compose to have run" is not a cost here.** `db-up` calls `compose up -d` immediately
before resolving, and `status`'s honest answer when compose has never run *is* `down` — measured:
`compose ps -q postgres` exits 0 with no output, `docker exec "" pg_isready` exits 1, and the
existing `&& echo up || echo down` prints `down`.

**It must go through the existing `compose()` shim, not through a literal `docker compose`.** On
this machine `docker compose version` fails — the CLI plugin is not installed, only the standalone
`docker-compose` binary (measured: `docker: unknown command: docker compose`;
`Docker Compose version 5.5.0`). The shim on line 21 already handles that, and it is why the
self-test exercises **both** branches.

## `docker-compose.yml`'s fixed `5432:5432` — agreed, leave it

Asked directly: **yes, leave it.** Two stacks cannot run at once for reasons that have nothing to do
with Postgres — `drive.mjs` hard-codes `localhost:5173`, the relay owns `5173`/`5273`, the server
owns `8080`, and `chrome-up` is given fixed CDP ports. Making 5432 dynamic would loosen the least
binding constraint of the six.

It would also have made the incident that prompted this **harder** to find, not easier: what held
the port was an orphaned container from a merged ticket, and a dynamic port would have brought a
second stack up cheerfully beside the stale one, on a different volume, with nobody told. A fixed
port is a collision alarm, and the alarm worked.

**There is a real second-order cost, and it is the volume rather than the port.** Because the
project name is per-directory, each worktree also gets its own named volume. Measured on this
machine today: `agent-a39fff996e6895d3d_poker-duels-postgres`,
`agent-ab2dcc073900e567e_poker-duels-postgres`, `agent-ac470d75a79fe6560_poker-duels-postgres` —
three volumes belonging to worktrees that no longer exist, beside `poker_duels_poker-duels-postgres`.
`db-down` is `compose down`, which removes the container and the network and **keeps** the volume.
That is out of scope here and is not ticketed; it is written down so the next person who finds four
volumes knows why.

## Files

| File | Action |
| --- | --- |
| `scripts/qa/stack.sh` | modify |
| `scripts/qa/stack-selftest.sh` | create |
| `docs/test-plan.md` | modify |

Read, and nothing else: `scripts/qa/delay.mjs`'s `selftest()` and `selftestCut()` for the
convention this repository already has for a hermetic self-check that says what each assertion
proves.

**On the size.** This is `S` at its ceiling: roughly 25 changed lines in `stack.sh`, one line in
`docs/test-plan.md`, and about 100 in the new harness, which is most of it. It is deliberately not
split. A ticket holding only the fix would have no gate that can fail without Docker, and a ticket
holding only the harness would gate a defect that is still present. Keep the harness lean — one
stub, one case runner, six assertions — and do not grow it past about 110 lines.

## Scope

- **Derive the name.** Replace the `DB_CONTAINER` literal with a function that asks compose:

  ```bash
  db_container() { compose ps -q postgres 2>/dev/null || true; }
  ```

  The `|| true` is not decoration: `compose ps` exiting non-zero for any reason other than
  *"nothing is up"* would otherwise kill the script under `set -e` in the middle of `status`, whose
  whole job is to report `down` without dying.
- **`db-up` resolves once, after `compose up -d`, and fails fast on empty.** `up -d` has already
  created the container by the time it returns, so one resolution is enough; if it comes back empty,
  `die` immediately rather than sleeping sixty times against a name nobody has. Then poll with
  `docker exec "$id" pg_isready -U poker -d poker_duels`, unchanged apart from the id.
- **`status` resolves fresh**, because nothing has run `up` for it, and prints `down` when the
  resolution is empty.
- **Add a `db-container` subcommand** that prints the resolved id on stdout and nothing else, and
  `die`s when there is none. It is the seam the self-test asserts on and the thing
  `docs/test-plan.md` calls.
- **Add it to the `usage` heredoc**, one line, in the shape of the lines already there.
- **`docs/test-plan.md`**: the `05-03` database read currently opens
  `docker exec poker_duels-postgres-1 psql -U poker -d poker_duels -At -c \`. It becomes
  `docker exec "$(scripts/qa/stack.sh db-container)" psql -U poker -d poker_duels -At -c \`.
  **One line changes in that file and nothing else does** — the SQL, the prose above it and the
  prose below it all stand.
- **`scripts/qa/stack-selftest.sh`**: the harness below.

## The self-test, and why it is a separate file

`bash scripts/qa/stack-selftest.sh` puts a stub directory first on `PATH`, runs
`scripts/qa/stack.sh` as a **child process**, and asserts on what the stub recorded. No Docker
daemon is contacted, so it is a real red/green on a machine with Colima stopped and in CI.

**Two reasons it is its own file, in the order they matter.** First, a harness that has to control
`PATH` and `argv` around the script under test wants to be outside it — `delay.mjs` could self-test
in-process because Node spawns its own servers; a shell script stubbing its own `docker` while
running is a confusion, not a convenience. Second, and this one is a hard constraint rather than a
preference: `TASK-131001`, `TASK-131002` and `TASK-131003` all carry a merged gate that fails if any
`verify:` line under `tasks/tasks/TASK-1310*.md` contains the text `stack` `.sh`, and this ticket's
file matches that glob. Naming the script under test in a `verify:` block would turn three merged
tickets red. `stack-selftest.sh` does not contain that substring, and that is not an evasion of
`ADR-0089` §2b: §2b forbids a **browser drive** standing between a pull request and `develop`, and
this harness starts no browser, needs no daemon and reads no screen.

**Do not rewrite that gate, in this ticket or any other.** It is written as
`index($0, "stack" ".sh")` — two concatenated literals — precisely so it does not match itself.

**Constraints on the harness.**

- **bash 3.2.** That is what `/bin/bash` is on this machine (measured: `3.2.57(1)-release`). No
  associative arrays, no `mapfile`, no `${var^^}`.
- **Run `stack.sh`, never `source` it.** Sourcing shares the shell and the `set -e` state, and its
  `case` would run in the harness's own process.
- **One stub file serving both `docker` and `docker-compose`**, deciding what it is from `$0`. It
  appends every invocation to a log, answers `compose version` according to an environment variable
  so both branches of the `compose()` shim can be driven, prints a canned id for `ps -q postgres`,
  and exits 0 for `exec` — the point is what was *called*, never whether a database answered. Stub
  `curl` too, so `status` does not wait on `8080`.
- **Every assertion prints one line naming itself** on success and, on failure, the case label and
  the recorded call log. The exit code is the gate; the labels are so a red run says which case.

`stack-selftest.sh`

| Assertion | Proves | Today |
| --- | --- | --- |
| `A1 db-up probes the id compose named (plugin branch)` | with the stub answering `pd-selftest-alpha-1`, the log holds `docker exec pd-selftest-alpha-1 pg_isready` | **red** |
| `A2 db-up probes a different id compose named (standalone branch)` | the same with `pd-selftest-beta-2` and `docker compose version` failing, so a **second, different** id and the other shim branch are both covered — one canned id cannot tell a derivation from a new constant | **red** |
| `A3 status probes the id compose named` | the second call site is fixed too: with `pd-selftest-gamma-3`, `status` logs `docker exec pd-selftest-gamma-3 pg_isready` and prints `db:     up` | **red** |
| `A4 db-container prints exactly the id and nothing else` | stdout of `stack.sh db-container` equals `pd-selftest-delta-4` exactly — the seam `docs/test-plan.md` interpolates | **red** — the subcommand does not exist |
| `A5 db-up and db-container fail in under 10s when compose names no container` | with the stub printing nothing, both exit non-zero and the elapsed time is under 10 seconds, so the empty case is a diagnosis and not a sixty-second stall — and a `\|\| id="poker_duels-postgres-1"` fallback cannot survive it, because the stub's `exec` would succeed and `db-up` would exit 0 | **red** |
| `A6 no recorded call names the retired literal` | the concatenated logs of A1–A5 contain no `poker_duels-postgres-1` | **red** |

**A6 is redundant with A1–A5 and is kept anyway**, at two lines, because the failure it catches
directly — a derivation with the old name as a fallback — is the most likely wrong turn here, and a
gate that names it is worth more than one that catches it sideways.

**What none of these prove.** They do not prove a real Postgres answers, and they must not try: the
only honest check of that needs a daemon, and a gate needing a daemon passes vacuously wherever
there is not one. Whether the container actually accepts connections is settled the first time
`TASK-131003` runs `db-up` for real.

## The negative control, which is not optional

Reverse the derivation — put `poker_duels-postgres-1` back as a literal — run
`bash scripts/qa/stack-selftest.sh`, and **paste the failing output into the PR body**, then
restore. It was run before this ticket was written, against a copy of the current script, and it
prints:

```
SELFTEST FAIL: db-up did not probe pd-selftest-alpha-1
--- calls ---
docker compose version
docker compose up -d
docker exec poker_duels-postgres-1 pg_isready -U poker -d poker_duels
```

A harness whose red run nobody has seen is a green run nobody should believe.

## Out of scope

- **Any change to `docker-compose.yml`.** The fixed `5432:5432` stays, for the reasons above, and
  the per-project volume accumulation is not ticketed.
- **`COMPOSE_PROJECT_NAME`.** Do not set it, and do not add `-p poker_duels` to the `compose()`
  shim. It would make `db-up` pass in a worktree by pointing every worktree at **one shared
  container and one shared volume**, which is a different behaviour wearing this fix's clothes —
  and note honestly that the self-test would **not** catch it, because the id would still come from
  compose. This paragraph is the only thing standing against it.
- **The Chrome path, the ports `5173`/`8080`, and everything else `ADR-0089`'s Consequences calls a
  machine-local dependency.** One item on that list is being removed; the rest stand.
- **`ADR-0089` itself.** Its Consequences paragraph describes what was true when it was written and
  is a record, not a decision that the name must be a literal. It is not edited here.
- **`tasks/tasks/TASK-120402-*.md`'s copy of the same command.** A merged ticket is a record of what
  was done; it is not rewritten.
- **Driving anything.** No browser is opened by this ticket and no `P` row is filled. `TASK-131003`
  is next and owns `P1`.

## Acceptance criteria

- [ ] `bash scripts/qa/stack-selftest.sh` exits 0 and prints a line for each of `A1`–`A6`
- [ ] The PR body contains the failing output of the negative control — the derivation reverted to
      the literal, the harness red at `A1`, and the recorded call log showing
      `docker exec poker_duels-postgres-1 pg_isready`
- [ ] `scripts/qa/stack.sh` contains no `poker_duels-postgres-1`, in code or in a comment about it
- [ ] `scripts/qa/stack.sh db-container` is listed in the `usage` heredoc
- [ ] `git diff --stat docs/test-plan.md` shows exactly one line changed, and the `05-03` SQL block,
      its heading prose and the paragraph below it are byte-identical
- [ ] Every command in `verify:` exits 0

## What the gates can and cannot check, stated the way this story states it

| Gate | Proves | Today |
| --- | --- | --- |
| `bash scripts/qa/stack-selftest.sh` | the name is derived and both call sites use it, on both `compose()` branches, with no daemon | **red** — checks a claim |
| no `poker_duels-postgres-1` in `docs/test-plan.md` | the doc's copy of the defect is gone | **red** — checks a claim |
| `db-container` appears in `docs/test-plan.md` | it was replaced rather than deleted | **red** — weak on its own; only the pair means anything |
| `SELECT b.device_id` still appears in `docs/test-plan.md` | `05-03`'s query was not deleted to satisfy the pair above | green — a regression guard, and the reason the pair is not enough |
| no `verify:` block under `tasks/tasks/TASK-1310*.md` names the drive script or the stack script | `ADR-0089` §2b still holds across the story, including for this ticket | green — a regression guard |
| the story's table still has seven `P` rows | nothing here disturbed the record | green — a regression guard |

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket: `verify` green,
review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into `develop`.
