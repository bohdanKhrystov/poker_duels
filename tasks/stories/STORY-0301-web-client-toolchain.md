---
id: STORY-0301
title: The web-client toolchain and its first green check
type: story
status: blocked
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

## Blocked on

`DEC-022`. The build tool and dev server, the package manager and lockfile policy, the Node version,
the test runner, the lint and format tooling, how the dev server reaches `/ws` and `/api`, and
whether the client's checks are their own CI job or run through Gradle are all open. `ADR-0003`
fixes React, TypeScript and Tailwind and nothing else; the rest is not a ticket's to improvise.

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
| — | *Not yet split. Run `/plan-story STORY-0301` once `DEC-022` is answered.* | — |

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

## Out of scope

- Any screen, any component beyond the mounting example — every later story.
- The styling layer and the design tokens — `STORY-0302`.
- The socket, the protocol client, device identity — `STORY-0303`.
- A browser end-to-end runner — `DEC-024`, and possibly never in this epic.
- Dockerfile, static hosting, TLS, serving the bundle in production — `EPIC-07`.
