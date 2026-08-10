# ADR-0004 — Branching and ticket workflow

- **Status:** Accepted
- **Date:** 2026-08-10

## Context

Most of the commits on this project will be written by agents. That changes what a version
control process is for. It is less about coordinating people and more about making sure that
every change is small, attributable to a stated intention, and reviewable in isolation — and
that no agent can quietly push something unreviewed.

It also has to work with the second product: the history should read as evidence of how the
work was organised.

## Decision

### Branches

`main` and `develop`, both protected, no direct pushes from anyone including the owner.

- `develop` is the default branch and the target of every working branch.
- `main` receives only release PRs from `develop`, and is tagged.
- Working branches are named `<type>/<TICKET-ID>-<slug>` and live for one ticket.

### Merges

**Squash merge only.** Merge commits and rebase merges are disabled at the repository level.
Head branches are deleted automatically.

The rationale is specific to how this project is built: an agent's working branch is a record
of it feeling its way to an answer, including the attempts that did not work. That is not
history worth keeping. One ticket becomes one commit, and `git log` on `develop` reads as the
list of things that were actually decided and delivered.

### Tickets

Tickets live in the repository as markdown, under `tasks/`, as **Epic → Story → Task**. Not in
GitHub Issues, not in Jira.

They are in-repo because an agent reading a ticket should not need a network call, an API token
or a tool integration to do it — and because a ticket and the code that implements it should be
reviewable in the same diff. A ticket changing in the same PR that implements it is a feature,
not a problem.

Every PR links exactly one ticket. A CI check validates ticket structure so the backlog cannot
rot into free-form notes.

## Consequences

**Gained**

- No unreviewed code can reach an integration branch.
- One ticket, one commit, one reversion — `git revert` on `develop` cleanly removes a feature.
- The backlog is versioned with the code, diffable, greppable, and available offline.
- The commit log doubles as the project's delivery record for the case study.

**Cost**

- Branch protection on a private repository requires a paid GitHub plan. If the repository is
  private and on the free plan, the rules cannot be enforced by the platform and the process
  degrades to convention. Making the repository public removes the constraint — and this
  project is intended to be public anyway.
- Squash merging loses intermediate commits. Accepted, and mostly the point.
- A markdown backlog has no burndown chart, no notifications, no filters. `tasks/BOARD.md` is
  maintained by hand instead. Acceptable at this size; would not be at ten people.

## Alternatives considered

- **Trunk-based on `main` alone** — fewer moving parts, and defensible for a solo project.
  Rejected: with agents generating most changes, a staging branch that can be broken and fixed
  without touching the released line is worth its small cost.
- **GitHub Issues as the backlog** — better tooling, worse agent ergonomics. Every ticket read
  becomes an API call, and tickets stop being diffable alongside code.
- **Full GitFlow with release and hotfix branches** — solves problems this project does not
  have.
