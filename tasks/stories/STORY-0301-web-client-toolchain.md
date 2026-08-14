---
id: STORY-0301
title: The web-client toolchain and its first green check
type: story
status: ready
parent: EPIC-03
module: web-client
labels: [client, build, foundation]
depends_on: []
---

## Goal

`web-client/` becomes a real project: `npm ci` installs a locked dependency set, an app root mounts
in a browser, and typecheck, lint, unit test and production build each exit 0 — locally and in CI,
where the client's own typecheck replaces today's ad-hoc `npx tsc` on the generated protocol file.
Nothing in it knows what poker is.

## Why

Every other story in the epic ends with *"a test renders something and asserts what it shows"*.
Until there is a project that builds and a test runner that runs, none of those acceptance criteria
mean anything.

It is also the last cheap moment to get the CI shape right. `ADR-0020` deliberately left a
temporary step in `.github/workflows/build.yml` — a bare `npx --package=typescript@5.6.3 tsc
--noEmit --strict web-client/src/protocol/protocol.gen.ts` under Node 20 — and said it *"folds into
`web-client`'s own typecheck once `EPIC-03` gives it one"*. This is that moment. Left alone, the
repository grows two ways of typechecking TypeScript and the ad-hoc one silently stops covering
anything.

## Unblocked by

[`ADR-0026`](../../docs/adr/ADR-0026-vite-and-npm-drive-the-web-client.md), which resolves `DEC-022`:
Vite + npm on Node 24 pinned once in `web-client/.nvmrc`, Vitest in `jsdom`, ESLint 9 and Prettier 3
with `src/protocol/protocol.gen.ts` ignored **by path** in both, Vite's proxy carrying `/api` and
`/ws` to Ktor on 8080, and a parallel `client` CI job running `npm ci`, `npm run check`,
`npm run build` while Gradle stays JVM-only. No ticket below decides any of that; they implement it.

## Design notes

- **`web-client/src/protocol/protocol.gen.ts` already exists**, is generated
  (`./gradlew :poker-server:generateProtocolTypes`) and is byte-compared on every `./gradlew check`.
  This story scaffolds *around* it. No ticket here may edit, move, rename, reformat, sort the
  imports of, or regenerate that file. The formatter and the linter must exclude it **by path**, and
  the story's verify block runs `./gradlew :poker-server:verifyProtocolTypes` so that a formatter
  that reaches it fails the ticket rather than the next unrelated PR.
- `tsconfig` is `strict`, and the generated file must be inside the typechecked set — that is what
  makes it safe to delete the CI step.
- The CI edit and the replacement land in **one ticket**: the ad-hoc step is removed in the same
  diff that adds the command covering it, never before and never after.
- CI pins `node-version: '20'`; the workstation has Node 26 and npm 11. The version `DEC-022`'s ADR
  names goes in both `package.json`'s `engines` and the workflow, in the same ticket, so they cannot
  disagree.
- The app root mounts one trivial component. It renders no card, reads no protocol type, and opens
  no socket — those are `0302` and `0303`.
- No test in this epic may reach the network or bind a port. The scaffold's example test sets that
  precedent.
- `design/tokens/tokens.css` is *not* wired in here. The styling layer is `STORY-0302`; a scaffold
  that quietly imports a colour has already started that story badly.

## Tasks

| ID | Title | Status |
| --- | --- | --- |
| [TASK-030101](../tasks/TASK-030101-manifest-node-pin-and-locked-install.md) | A manifest, a Node pin and a locked install for web-client | ready |
| [TASK-030102](../tasks/TASK-030102-prettier-never-reads-the-generated-file.md) | Prettier formats the client and never reads the generated file | backlog |
| [TASK-030103](../tasks/TASK-030103-strict-tsconfig-keeps-the-generated-file-in-the-program.md) | A strict tsconfig that keeps the generated file inside the typechecked program | backlog |
| [TASK-030104](../tasks/TASK-030104-an-app-root-that-mounts-one-component.md) | An app root that mounts one trivial component | backlog |
| [TASK-030105](../tasks/TASK-030105-vite-builds-a-bundle-that-contains-the-app.md) | Vite builds a production bundle that contains the app | backlog |
| [TASK-030106](../tasks/TASK-030106-vitest-runs-one-component-test-in-jsdom.md) | Vitest renders the app in jsdom and asserts what it shows | backlog |
| [TASK-030107](../tasks/TASK-030107-eslint-lints-the-client-and-not-the-generated-file.md) | ESLint lints the client and never the generated file | backlog |
| [TASK-030108](../tasks/TASK-030108-the-dev-server-proxies-api-and-ws-to-ktor.md) | The dev server proxies /api and /ws to the Ktor server | backlog |
| [TASK-030109](../tasks/TASK-030109-one-npm-run-check-runs-all-four-checks.md) | One npm run check runs every check CI will run | backlog |
| [TASK-030110](../tasks/TASK-030110-ci-gains-a-client-job-and-drops-the-ad-hoc-tsc.md) | CI gains a client job and drops the ad-hoc npx tsc in the same diff | backlog |
| [TASK-030111](../tasks/TASK-030111-contributing-says-how-to-check-the-client.md) | CONTRIBUTING says how to install and check the web client | backlog |

The chain is strictly linear — `030101 → 030102 → … → 030111` — because almost every ticket edits
`web-client/package.json` and two of them edit `vite.config.ts`. Nothing here may run in parallel.

Three deliberate orderings, each of which cost a dispatch somewhere else if reversed:

- **Prettier lands second, before any source file exists.** A formatter introduced late has to
  reformat files belonging to earlier tickets, which is a budget overrun by construction. Every file
  after `TASK-030102` is written under Prettier's rules and every later ticket's `verify` re-runs
  `format:check`.
- **ESLint lands after the app root**, because `eslint .` over a directory whose only TypeScript is
  the ignored generated file is an error, not a pass.
- **Each ticket's `verify` block grows to match CI.** By `TASK-030109` it is exactly what the
  `client` job runs. A verify narrower than CI let four defects through in `EPIC-02`.

**`TASK-030110` needs a repository setting after it merges**: `client` must be added to `develop`'s
required checks. No file expresses it and no `verify` command can prove it — until a human or the
driver applies it, a red `client` job blocks nothing.

`tailwindcss`, `@tailwindcss/vite` and `prettier-plugin-tailwindcss` are in `ADR-0026`'s toolchain
but **not installed here**: the plugin needs a stylesheet to read, and the stylesheet is
`STORY-0302`'s, which brings all three with it.

## Acceptance criteria

- [ ] `npm ci` in `web-client/` succeeds from a clean clone against a committed lockfile.
- [ ] The typecheck, lint, unit-test and production-build commands each exit 0, and each is named in
      `package.json` so a ticket's `verify:` block can call it.
- [ ] A deliberately broken use of a `protocol.gen.ts` type fails the client's typecheck — proving
      the generated file is inside the typechecked set.
- [ ] `.github/workflows/build.yml` no longer runs a bare `npx tsc` on the generated file, and CI
      runs the client's own commands instead.
- [ ] `./gradlew :poker-server:verifyProtocolTypes` passes, and `protocol.gen.ts` is byte-identical
      to its state before this story.
- [ ] `./gradlew check` is still green.
- [ ] The `client` job is on `develop`'s required-checks list — a repository setting a human or the
      driver applies when `TASK-030110` merges, not a file any ticket can change.

## Out of scope

- Any screen, any component beyond the mounting example — every later story.
- The styling layer and the design tokens — `STORY-0302`.
- The socket, the protocol client, device identity — `STORY-0303`.
- A browser end-to-end runner — `DEC-024`, and possibly never in this epic.
- Dockerfile, static hosting, TLS, serving the bundle in production — `EPIC-07`.
