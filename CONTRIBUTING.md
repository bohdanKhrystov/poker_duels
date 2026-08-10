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
| `main` | Released code. Only ever receives release PRs from `develop`. | yes |
| `develop` | Integration branch and the default target for all work. | yes |

Neither protected branch accepts direct pushes, from anyone, including the repository owner.

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

## The loop for one ticket

```
pick ticket           status: ready → in-progress
  └─ branch from develop
  └─ implement + tests
  └─ open PR into develop, link the ticket   status: in-review
  └─ review (self or reviewing agent)
  └─ squash merge, branch deleted            status: done
  └─ update tasks/BOARD.md
```

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
