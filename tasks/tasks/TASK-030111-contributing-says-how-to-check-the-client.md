---
schema: 2
id: TASK-030111
title: CONTRIBUTING says how to install and check the web client
type: task
status: done
parent: STORY-0301
module: web-client
estimate: XS
tier: haiku
review: light
files_touched: 1
labels: [client, docs]
depends_on: [TASK-030110]
verify:
  - grep -c 'npm run check' CONTRIBUTING.md | grep -qx 1
  - grep -c 'npm ci' CONTRIBUTING.md | grep -qx 1
  - grep -c 'npm run dev' CONTRIBUTING.md | grep -qx 1
  - grep -c '.nvmrc' CONTRIBUTING.md | grep -qx 1
  - cd web-client && npm ci && npm run check
---

## Goal

A contributor who has only ever run `./gradlew check` can find out, from `CONTRIBUTING.md`, that the
client has its own command and what it is.

## Why

`ADR-0026` names this as the price of the decision: *"`./gradlew check` no longer proves the whole
repository; a contributor must know `npm run check` exists."* The cost is only acceptable if the
knowledge is written down where people already look.

## Files

| File | Action |
| --- | --- |
| `CONTRIBUTING.md` | modify |

## Scope

- A short subsection under **Local setup**, after *The development database*, covering exactly four
  things:
  - Node comes from `web-client/.nvmrc` (`nvm use` in that directory picks it up); CI reads the same
    file, so CI governs what green means.
  - `npm ci` from `web-client/` installs from the committed lockfile.
  - `npm run check` is the one local command for the client — typecheck, lint, format check and
    tests — and `npm run dev` starts the dev server, which proxies `/api` and `/ws` to a Ktor server
    on `localhost:8080`.
  - `./gradlew check` proves the JVM side only. The two toolchains are separate on purpose and each
    has its own CI job.
- Keep it to the register of the surrounding document — short, imperative, no tutorial.

## Out of scope

- `docs/architecture.md`, `README.md`, `docs/workflow.md`. One file, one place.
- Restating `ADR-0026`. Link it at most once; the ADR is where the reasoning lives.
- Any behaviour change. No script, no config, no workflow is touched by this ticket.

## Proof

| Command | Proves |
| --- | --- |
| the four `grep -c ... \| grep -qx 1` commands | `npm ci`, `npm run check`, `npm run dev` and `.nvmrc` are each named exactly once — documentation that says the command without naming the version pin, or twice in different words, fails |
| `npm ci && npm run check` | the commands the document now tells people to run actually work from a clean install |

## Acceptance criteria

- [ ] `CONTRIBUTING.md` names `npm ci`, `npm run check`, `npm run dev` and `.nvmrc`, once each
- [ ] It says plainly that `./gradlew check` does not cover the client
- [ ] No file other than `CONTRIBUTING.md` changes
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
