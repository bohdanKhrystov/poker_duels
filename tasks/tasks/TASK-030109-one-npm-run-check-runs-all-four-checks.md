---
schema: 2
id: TASK-030109
title: One npm run check runs every check CI will run
type: task
status: done
parent: STORY-0301
module: web-client
estimate: XS
tier: haiku
review: light
files_touched: 1
labels: [client, build, toolchain]
depends_on: [TASK-030108]
verify:
  - cd web-client && npm run check
  - cd web-client && npm run build
  - cd web-client && NO_COLOR=1 npm run --silent check 2>&1 | grep -qE 'Tests +3 passed \(3\)'
  - node -e "const s=require('./web-client/package.json').scripts.check; if (s !== 'npm run typecheck && npm run lint && npm run format:check && npm run test') process.exit(1)"
  - ./gradlew :poker-server:verifyProtocolTypes
---

## Goal

`npm run check` from `web-client/` is the one local command for the client, running typecheck, lint,
format check and tests in that order — the command `TASK-030110` puts in CI.

## Files

| File | Action |
| --- | --- |
| `web-client/package.json` | modify |

## Scope

- Add exactly one script:

  ```json
  "check": "npm run typecheck && npm run lint && npm run format:check && npm run test"
  ```

- `ADR-0026` writes that script out as `tsc --noEmit && eslint --max-warnings 0 . && prettier
  --check . && vitest run`. These are the same four commands in the same order: each named script
  already holds its tool's exact invocation, and delegating keeps every flag — `--max-warnings 0`
  above all — defined in exactly one place instead of two that can drift apart.
- Nothing else changes. Every script it composes already exists and is already green.

## Out of scope

- The CI workflow — `TASK-030110`, the next ticket.
- A `check` script anywhere else, and any Gradle task that shells out to npm. `ADR-0026` is explicit
  that nothing runs through Gradle and `settings.gradle.kts` is untouched.
- Adding `build` to `check`. CI runs them as two steps, so a bundle-only breakage is reported as a
  bundle-only breakage.

## Proof

| Command | Proves |
| --- | --- |
| `npm run check` | all four checks pass in one invocation |
| `npm run check` prints `Tests  3 passed (3)` | the tests really ran inside `check` — the failure mode where an aggregate script exits 0 having skipped the slow half |
| the `node -e` comparison | `check` is the exact composition above, not a narrower subset that would let CI pass on less than it claims |
| `npm run build` | the two commands CI will run, run here first |

Check it fails when it should: break the format of any source file (add a stray blank line), run
`npm run check`, and it goes red at the format step. Revert.

## Acceptance criteria

- [ ] `cd web-client && npm run check` exits 0
- [ ] The `check` script is exactly
      `npm run typecheck && npm run lint && npm run format:check && npm run test`
- [ ] `npm run check` output contains `Tests  3 passed (3)`
- [ ] `cd web-client && npm run build` exits 0
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
