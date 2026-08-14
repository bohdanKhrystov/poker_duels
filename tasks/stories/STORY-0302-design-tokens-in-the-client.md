---
id: STORY-0302
title: The design tokens are the client's only colours and sizes
type: story
status: ready
parent: EPIC-03
module: web-client
labels: [client, design, styling]
depends_on: [STORY-0301]
---

## Goal

The client's styling layer is built from `design/tokens/tokens.css`: every colour, spacing step,
radius and font a screen uses resolves to a `--pd-` custom property, and a check fails the build
when a component writes a literal instead.

## Why

`EPIC-06` exists so that `EPIC-03` does not invent values — its definition of done says the client
"builds its first screen without inventing a single color or size". That coupling has to be
structural, and it has to exist *before* the first screen: by the third screen there are three
different greys and no diff that shows when it happened.

## Design notes

- `design/tokens/tokens.css` is canonical and lives **outside** `web-client/` (`ADR-0024`: the
  repository is the source of truth, and the token sheet is born there and nowhere else). It carries
  57 `--pd-` properties today — surfaces (`--pd-bg`, `--pd-surface`, `--pd-hairline`), text, the
  single steel accent (`--pd-accent`), outcome colours (`--pd-win`, `--pd-loss`, `--pd-warn`), the
  card face and back (`--pd-card-face`, `--pd-suit-red`, `--pd-suit-black`, `--pd-card-back`), the
  coin (`--pd-coin`) and the type and spacing scales.
- How the sheet reaches the bundle — imported across the directory boundary, or copied in by a build
  step — is a ticket-level choice constrained by `DEC-022`'s toolchain. Whichever it is: the file is
  **never hand-edited inside `web-client/`**, and if a copy exists, a check that runs in the
  client's own test command asserts the copy is byte-identical to the source. A drifted copy is the
  exact failure `ADR-0024` set up its greps to catch.
- Tailwind's theme **references** the custom properties rather than restating their values, so a
  colour changed in `design/` is picked up here without a second edit. Restating a hex in the theme
  config is the same defect as writing one in a component.
- The client adds no token. A value the design system does not have is an `EPIC-06` ticket, not a
  client ticket — that is what keeps the two epics from forking the palette.
- Dark only. `STORY-0601` scoped light out epic-wide; no `prefers-color-scheme` branch, no theme
  switch, no second palette "for later".
- The enforcement is the point of the story: a lint rule or a test that fails on a hex, `rgb()` or
  `hsl()` literal outside the token layer. It must exclude `protocol.gen.ts` and the vendored token
  sheet, and it must be part of a command a ticket's `verify:` block can run.

## Tasks

Split into schema-2 tickets on 2026-08-14. Strictly ordered: every ticket after the first touches a
file an earlier one wrote, so exactly one is startable at a time.

| ID | Title | Status |
| --- | --- | --- |
| [TASK-030201](../tasks/TASK-030201-vendor-the-token-sheet-byte-for-byte.md) | Vendor the token sheet into the client, byte for byte | ready |
| [TASK-030202](../tasks/TASK-030202-the-check-fails-on-a-colour-literal.md) | The client's check fails on a colour literal outside the token layer | backlog |
| [TASK-030203](../tasks/TASK-030203-tailwind-installs-and-its-vite-plugin-runs.md) | Tailwind installs from the lockfile and its Vite plugin runs | backlog |
| [TASK-030204](../tasks/TASK-030204-the-tokens-and-tailwind-reach-the-bundle.md) | The tokens and Tailwind reach the bundle through one stylesheet | backlog |
| [TASK-030205](../tasks/TASK-030205-the-themes-colours-are-the-tokens.md) | The theme's colours are the tokens and nothing else | backlog |
| [TASK-030206](../tasks/TASK-030206-the-themes-sizes-are-the-tokens.md) | The theme's type, spacing and radii are the tokens and nothing else | backlog |
| [TASK-030207](../tasks/TASK-030207-prettier-sorts-tailwind-classes.md) | Prettier sorts Tailwind classes and still never reads the generated file | backlog |
| [TASK-030208](../tasks/TASK-030208-the-app-root-is-styled-through-the-theme.md) | The app root is styled through the theme, proven by a test | backlog |
| [TASK-030209](../tasks/TASK-030209-contributing-says-the-token-sheet-is-a-copy.md) | CONTRIBUTING says the client's token sheet is a copy | backlog |

**Copy, not import** (`TASK-030201`): `web-client/` is the Vite root, and Vite's `server.fs.allow`
default resolves to that directory — the lockfile and `package.json` are what its workspace search
finds. A CSS source outside the root is therefore one Vite version from being denied in the dev
server, the build and Vitest at once. The copy keeps the client hermetic; a byte comparison in the
client's own suite is what stops it drifting.

## Acceptance criteria

- [ ] The client's checks fail when a component or stylesheet outside the token layer contains a
      hex, `rgb()` or `hsl()` colour literal, and pass when it uses a `--pd-` property.
      → `TASK-030202`, inside `npm run test` and therefore inside `npm run check`
- [ ] If the token sheet is copied into `web-client/`, a check asserts the copy is byte-identical to
      `design/tokens/tokens.css` and fails when the source changes. → `TASK-030201`
- [ ] A component styled through the theme renders with the token-derived class or variable in a
      unit test — the wiring is proven by a test, not by inspection. → `TASK-030208`, with the build
      assertion that the class compiled to `var(--pd-fs-title)`
- [ ] `./gradlew :poker-server:verifyProtocolTypes` still passes: no formatter added here touches
      the generated protocol file. → every ticket's `verify:` block, and `TASK-030207` in particular

## Out of scope

- Designing anything — new colours, new sizes, the table's composition — `EPIC-06`.
- A light theme, print styles, marketing pages.
- Fonts fetched from a network at runtime; the token sheet's stacks are what the client uses.
- Any screen. This story styles nothing but its own proof.
