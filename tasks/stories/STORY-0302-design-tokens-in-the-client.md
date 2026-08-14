---
id: STORY-0302
title: The design tokens are the client's only colours and sizes
type: story
status: backlog
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

| ID | Title | Status |
| --- | --- | --- |
| — | *Not yet split. Run `/plan-story STORY-0302`.* | — |

## Acceptance criteria

- [ ] The client's checks fail when a component or stylesheet outside the token layer contains a
      hex, `rgb()` or `hsl()` colour literal, and pass when it uses a `--pd-` property.
- [ ] If the token sheet is copied into `web-client/`, a check asserts the copy is byte-identical to
      `design/tokens/tokens.css` and fails when the source changes.
- [ ] A component styled through the theme renders with the token-derived class or variable in a
      unit test — the wiring is proven by a test, not by inspection.
- [ ] `./gradlew :poker-server:verifyProtocolTypes` still passes: no formatter added here touches
      the generated protocol file.

## Out of scope

- Designing anything — new colours, new sizes, the table's composition — `EPIC-06`.
- A light theme, print styles, marketing pages.
- Fonts fetched from a network at runtime; the token sheet's stacks are what the client uses.
- Any screen. This story styles nothing but its own proof.
