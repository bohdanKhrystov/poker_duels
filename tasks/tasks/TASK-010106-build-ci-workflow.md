---
schema: 2
id: TASK-010106
title: Build and test CI workflow for pull requests
type: task
status: done
parent: STORY-0101
module: build
estimate: XS
tier: haiku
review: light
files_touched: 1
labels: [ci]
depends_on: [TASK-010104, TASK-010105]
verify:
  - python3 -c "import sys,re; s=open('.github/workflows/build.yml').read(); sys.exit(0 if 'gradlew check' in s and 'pull_request' in s else 1)"
  - ./gradlew check
---

## Goal

Every pull request runs `./gradlew check` and reports a status check that branch protection can
require.

## Context

- `.github/workflows/tickets.yml` — the existing backlog linter. Follow its shape: same trigger
  style, same concurrency block.

## Files

| File | Action |
| --- | --- |
| `.github/workflows/build.yml` | create |

> Pushing this file needs a token with the `workflow` scope. If the push is rejected, that is
> not a code failure — report it and stop.

## Scope

- Triggered on pull requests targeting `develop` and `main`.
- Checkout, pinned JDK via `actions/setup-java`, Gradle dependency caching.
- Runs `./gradlew check`.
- Uploads the test report as an artifact when the run fails.
- Concurrency group per branch, cancelling superseded runs.

## Out of scope

- Publishing, releases, deployment.
- Matrix builds across JDKs. One pinned toolchain is the point.
- Marking the check required in branch protection — that is `TASK-000102`, still blocked.

## Tests

None. The `verify` commands check that the workflow declares the right trigger and command, and
that `check` passes locally.

## Acceptance criteria

- [ ] `.github/workflows/build.yml` triggers on `pull_request` to `develop` and `main`.
- [ ] It runs `./gradlew check`.
- [ ] It uses `actions/setup-java` with an explicitly pinned JDK version.
- [ ] It declares a concurrency group that cancels superseded runs.
- [ ] `./gradlew check` exits 0 locally.

## Definition of done

Standard, per [`tasks/README.md`](../README.md): `verify` green, review passed, CI green, status
`done`, `BOARD.md` updated, squash-merged into `develop`. Not done until the PR is merged.
