# Contributing

The same rules apply to humans and to agents. There is no "quick fix straight to develop".

## Branches

```
feature/TASK-010301-five-card-evaluator
        │
        │  pull request  →  squash merge
        ▼
     develop        ← default branch, protected, always green
        │
        │  release pull request  →  squash merge
        ▼
      main          ← released code, protected, tagged
```

| Branch | Purpose | Protected |
| --- | --- | --- |
| `main` | Released code. Only ever receives release PRs from `develop`. | not yet — see below |
| `develop` | Integration branch and the default target for all work. | not yet — see below |

Neither branch accepts direct pushes. **Follow that rule even though nothing currently enforces
it.**

> ### ⚠ Protection is not enforced yet
>
> GitHub refuses branch protection and rulesets on a private repository on the free plan
> (`403 Upgrade to GitHub Pro or make this repository public`). `poker_duels` is both, so today
> the branch model is convention rather than enforcement — a direct push to `develop` would
> succeed.
>
> Tracked as [`TASK-000102`](tasks/tasks/TASK-000102-enable-branch-protection.md), blocked on
> either making the repository public or upgrading to Pro. The rules are written and ready to
> apply the moment the constraint lifts.
>
> What **is** already enforced, because it needs no paid plan: `develop` is the default branch,
> squash merge is the only permitted merge method, and head branches are deleted on merge.

### Working branch names

One branch per ticket. The ticket ID goes in the branch name:

```
feature/TASK-010301-five-card-evaluator
fix/TASK-010502-min-raise-off-by-one
chore/TASK-010102-detekt-baseline
docs/STORY-0104-engine-contract-notes
```

Prefix is one of `feature`, `fix`, `chore`, `docs`, `test`, `refactor`.

## Pull requests

- **Target `develop`.** Only release PRs target `main`.
- **One ticket per PR.** If a PR closes more than one ticket, it was too big.
- **Squash and merge only.** Merge commits and rebase-merge are disabled on the repository.
  The squashed commit message becomes the permanent history, so make the PR title good.
- **Head branch is deleted on merge.**
- **Required before merge:** the `tickets` check passes, the PR body links its ticket, and the
  ticket's acceptance criteria are all ticked.

### PR title = the squash commit

Use Conventional Commits, with the ticket ID as a trailer-ish suffix:

```
feat(engine): evaluate five-card hands into a comparable rank (TASK-010302)
fix(engine): correct min-raise when facing an all-in short raise (TASK-010502)
chore(ci): add ticket frontmatter linter (TASK-000102)
```

Types: `feat`, `fix`, `refactor`, `test`, `docs`, `chore`, `perf`, `build`, `ci`.
Scopes follow the module: `engine`, `server`, `web`, `cli`, `ai`, `analysis`, `ci`, `tasks`, `docs`.

## Review is mandatory

**Every pull request is reviewed before it merges. No exceptions** — not for documentation, not
for one-line changes, not when the author is sure it is fine.

Review effort is priced by the ticket's `review:` field — `light` and `standard` use a reviewer
subagent, `deep` adds `/code-review low`. **Never run `/code-review high` from a loop**: measured
at 132k tokens on a single documentation PR. See
[`ADR-0007`](docs/adr/ADR-0007-token-lean-agent-workflow.md).

Handling what it finds:

| Finding | Action |
| --- | --- |
| Real defect in this diff | Fix it here, push, review again |
| Real problem outside this ticket | Open a new ticket in `backlog`. Do **not** fix it here |
| You disagree | Say why in the PR. Never silently ignore it |

A clean review is a normal result. A skipped review is a process failure — and because these
numbers feed the case study in `docs/workflow.md`, it gets recorded as one.

**The PR merges itself** when `verify` exits 0, the review passes and CI is green. No human reads
the code before it lands on `develop` — a deliberate trade for autonomy, argued in full in
[`ADR-0007`](docs/adr/ADR-0007-token-lean-agent-workflow.md). Every ticket is one squashed commit,
so a bad merge is one `git revert`.

## The loop for one ticket

```
pick ticket                                  status: in-progress
  └─ branch from develop
  └─ implement + tests
  └─ build and tests green locally
  └─ update the ticket and BOARD.md in the same commit
  └─ push, open PR into develop, link the ticket    status: in-review
  └─ verify commands exit 0
  └─ review (light / standard / deep) ──► findings? ──► fix, push, re-review
  └─ CI green
  └─ auto squash merge, branch deleted              status: done
```

**A task is finished when its PR is merged into `develop` — not when the code is written.**
There is no "done except for the PR". While the PR is open the ticket is `in-review`, and the
next task does not start.

If you discover work that is out of the ticket's scope: **do not do it**. Write a new ticket in
`tasks/` and carry on with the one you have. Scope creep is the main way this process fails.

## Releasing

1. Open a PR from `develop` into `main` titled `release: vX.Y.Z`.
2. Squash merge it.
3. Tag `main` with `vX.Y.Z`.

## Local setup

```sh
git clone https://github.com/bohdanKhrystov/poker_duels.git
cd poker_duels
git checkout develop
```

Toolchain requirements arrive with `TASK-010101`; until then the repository is documentation
and tickets only.

### The development database

Start a PostgreSQL container with `docker compose up -d`, and stop it with `docker compose down -v`. The image matches the one the test suite uses, and the credentials need no configuration:

```sh
# Start
docker compose up -d
# Stop
docker compose down -v
```

The database is at `localhost:5432`, username `poker`, password `poker`.

The test suite does **not** use this container — Testcontainers starts its own fresh database for each run. To run tests, you need Docker installed, not a running compose stack. The command `./gradlew check` will start the container automatically and clean it up afterward.

If Docker is not available:
- `./gradlew check` *skips* database tests with a message and stays green, so work on the engine
  and the protocol is not blocked.
- `./gradlew check -PrequireDocker=true` *fails* instead. This is what CI runs, because a test
  suite that skips silently in CI is a test suite that has stopped testing.

### The web client

Node is pinned in `web-client/.nvmrc`; CI reads the same file. From the repository root:

```sh
cd web-client
npm ci
npm run check
```

To start the development server—proxying `/api` and `/ws` to `localhost:8080`—run `npm run dev`.

`./gradlew check` proves the JVM side only. The toolchains are separate on purpose (see [`ADR-0026`](docs/adr/ADR-0026-vite-and-npm-drive-the-web-client.md)), each with its own CI job.
