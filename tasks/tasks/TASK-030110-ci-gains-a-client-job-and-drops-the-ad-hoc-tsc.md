---
schema: 2
id: TASK-030110
title: CI gains a client job and drops the ad-hoc npx tsc in the same diff
type: task
status: done
parent: STORY-0301
module: web-client
estimate: XS
tier: haiku
review: standard
files_touched: 1
labels: [client, ci, protocol]
depends_on: [TASK-030109]
verify:
  - grep -c 'npx --yes --package=typescript' .github/workflows/build.yml | grep -qx 0
  - grep -c 'node-version.*20' .github/workflows/build.yml | grep -qx 0
  - grep -c '^  client' .github/workflows/build.yml | grep -qx 1
  - grep -c 'actions/setup-node' .github/workflows/build.yml | grep -qx 1
  - grep -c 'node-version-file' .github/workflows/build.yml | grep -qx 1
  - grep -c 'npm ci' .github/workflows/build.yml | grep -qx 1
  - grep -c 'npm run check' .github/workflows/build.yml | grep -qx 1
  - grep -c 'npm run build' .github/workflows/build.yml | grep -qx 1
  - grep -c './gradlew check -PrequireDocker=true' .github/workflows/build.yml | grep -qx 1
  - ./web-client/node_modules/.bin/prettier .github/workflows/build.yml > /dev/null
  - cd web-client && npm ci && npm run check && npm run build
  - ./gradlew :poker-server:verifyProtocolTypes
---

## Goal

`.github/workflows/build.yml` runs the client's own checks in a parallel `client` job, and the
ad-hoc `setup-node` + `npx tsc` steps that covered the generated file disappear in the same diff.

## Why one diff

`ADR-0020` pre-authorised the fold and `ADR-0026` fixes the timing: **same PR**, so
`web-client/src/protocol/protocol.gen.ts` is never uncovered on `develop`. Remove the old step
before the new job exists and the file goes untypechecked; add the job first and the repository has
two ways of typechecking TypeScript, which is the state this story exists to end. The new job's
`npm run check` typechecks the generated file because `tsconfig.json` includes it — proven by
`TASK-030103`.

## Files

| File | Action |
| --- | --- |
| `.github/workflows/build.yml` | modify |

## Scope

- **Delete** from the `check` job: the `actions/setup-node@v4` step pinned to `node-version: '20'`
  and the `Typecheck the generated protocol types` step running `npx --yes
  --package=typescript@5.6.3 tsc`. `check` becomes purely JVM — checkout, setup-java,
  gradle-build-action, `./gradlew check -PrequireDocker=true`, upload-artifact — and nothing else
  about it changes. The EOL Node 20 pin leaves with the step that carried it.
- **Add** a second job `client`, a sibling of `check` under `jobs:` so the two run in parallel:

  ```yaml
    client:
      name: client
      runs-on: ubuntu-latest
      defaults:
        run:
          working-directory: web-client
      steps:
        - uses: actions/checkout@v4

        - uses: actions/setup-node@v4
          with:
            node-version-file: web-client/.nvmrc
            cache: npm
            cache-dependency-path: web-client/package-lock.json

        - name: Install from the lockfile
          run: npm ci

        - name: Typecheck, lint, format and test
          run: npm run check

        - name: Production build
          run: npm run build
  ```

- No `paths` filter on the job. `ADR-0026`: it runs unconditionally so the required-checks list
  stays simple and a skipped job can never masquerade as a pass.

## After merge, and not by this ticket

`client` must be added to `develop`'s **required checks**. That is a repository setting, no file
changes, and no `verify` command can prove it — until someone applies it, a red `client` job blocks
nothing. Say so in the PR description so the human or the driver does it at merge time.

## Out of scope

- Any change to the `check` job beyond deleting those two steps — the Gradle command, the Java
  version, the artefact upload and the `concurrency` block all stay as they are.
- A browser end-to-end job. `DEC-024` is unanswered and owns that question.
- Any Gradle task that shells out to npm, and any edit to `settings.gradle.kts`.

## Proof

Every claim is a `grep -c` with an exact expected count, so a partial edit fails:

| Assertion | Proves |
| --- | --- |
| `npx --yes --package=typescript` appears 0 times | the ad-hoc step is gone |
| `node-version.*20` appears 0 times | the end-of-life Node 20 pin is gone |
| `^  client` appears once | the second job exists at job level, not nested inside `check` |
| `actions/setup-node` appears once, with `node-version-file` | Node is set up exactly once, from `.nvmrc`, which is the single pin |
| `npm ci`, `npm run check`, `npm run build` appear once each | the job runs `ADR-0026`'s three commands |
| `./gradlew check -PrequireDocker=true` still appears once | the JVM job was not damaged in passing |
| `prettier` parses `build.yml` | the YAML is syntactically valid — a workflow file that fails to parse does not run, and a PR with no checks can look green |
| `npm ci && npm run check && npm run build` from `web-client` | the exact commands the job will run pass locally first |

## Acceptance criteria

- [ ] `.github/workflows/build.yml` has a `client` job running `npm ci`, `npm run check` and
      `npm run build` with `working-directory: web-client`
- [ ] `setup-node` appears exactly once and reads `node-version-file: web-client/.nvmrc`
- [ ] No `npx ... tsc` step and no `node-version: '20'` remain anywhere in the file
- [ ] The `check` job still runs `./gradlew check -PrequireDocker=true` and nothing else changed
      about it
- [ ] The PR description says `client` must be added to `develop`'s required checks
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
