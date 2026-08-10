---
id: TASK-010103
title: Build and test CI workflow for pull requests
type: task
status: backlog
parent: STORY-0101
module: poker-engine
estimate: S
labels: [ci]
depends_on: [TASK-010102]
---

## Goal

Every pull request into `develop` or `main` runs `./gradlew check` and reports a status check
that branch protection can require.

## Context

- [`CONTRIBUTING.md`](../../CONTRIBUTING.md) — the merge rules this check enforces.
- `.github/workflows/tickets.yml` — the existing ticket linter; follow its shape.

## Scope

- A `build` workflow triggered on pull requests targeting `develop` and `main`.
- Checkout, pinned JDK via `actions/setup-java`, Gradle dependency caching.
- Run `./gradlew check`.
- Upload the test report as an artifact when the run fails, so a failure is diagnosable without
  reproducing it locally.
- Concurrency group per branch, cancelling superseded runs.

## Out of scope

- Publishing artifacts, releases, or deployment.
- Running on a schedule or on push to feature branches — pull requests are the gate.
- Matrix builds across JDKs. One pinned toolchain is the point.

## Files

| File | Action |
| --- | --- |
| `.github/workflows/build.yml` | create |

## Acceptance criteria

- [ ] A pull request into `develop` triggers the workflow.
- [ ] The workflow fails when a test fails and passes when they pass.
- [ ] The Gradle cache is used, and a warm run is meaningfully faster than a cold one.
- [ ] The check appears in the pull request UI under a stable name, so it can be marked required
      in branch protection.
- [ ] Pushing a second commit cancels the superseded run.

## Tests

Verified by observation on the pull request that introduces it — deliberately break a test,
confirm the check goes red, fix it, confirm it goes green.

## Follow-up

Once merged, add this check to the required checks on `develop` and `main`.

## Definition of done

Standard, per [`tasks/README.md`](../README.md): build green, tests green, status `done`,
`BOARD.md` updated, squash-merged into `develop` by a PR linking this ticket.
