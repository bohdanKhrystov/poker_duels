---
schema: 2
id: TASK-030209
title: CONTRIBUTING says the client's token sheet is a copy
type: task
status: done
parent: STORY-0302
module: web-client
estimate: XS
tier: haiku
review: light
files_touched: 1
labels: [client, design, docs]
depends_on: [TASK-030208]
verify:
  - grep -q 'web-client/src/styles/tokens.css' CONTRIBUTING.md
  - grep -q 'design/tokens/tokens.css' CONTRIBUTING.md
  - grep -q 'ADR-0024-design-follows-the-code-workflow.md' CONTRIBUTING.md
  - test -f design/tokens/tokens.css
  - test -f web-client/src/styles/tokens.css
  - test -f docs/adr/ADR-0024-design-follows-the-code-workflow.md
  - cd web-client && npm run check
  - ./gradlew :poker-server:verifyProtocolTypes
---

## Goal

A contributor who opens `web-client/src/styles/tokens.css` learns from `CONTRIBUTING.md` that it is
a copy, before they edit it.

## Files

| File | Action |
| --- | --- |
| `CONTRIBUTING.md` | modify |

## Scope

- Add a short paragraph to the existing **The web client** section, after the `npm run dev` line and
  before the `./gradlew check` line. Three facts, no more:
  1. `web-client/src/styles/tokens.css` is a byte-for-byte copy of `design/tokens/tokens.css`; edit
     the source under `design/` and re-copy it — a test in the client's suite fails when the two
     differ.
  2. Every colour and size in the client comes from a `--pd-` property or a Tailwind utility mapped
     to one; `npm run check` fails on a hex, `rgb()`, `hsl()` or `oklch()` literal written anywhere
     else.
  3. A value the design system does not have is an `EPIC-06` ticket, not a client change.
- Link `design/tokens/tokens.css` and
  [`ADR-0024`](docs/adr/ADR-0024-design-follows-the-code-workflow.md), matching the link style the
  section already uses for `ADR-0026`.

## Out of scope

- Rewriting anything else in `CONTRIBUTING.md`.
- `README.md`, `docs/architecture.md`, `design/README.md` — the last is `EPIC-06`'s file and is not
  touched by this story at all.
- A script that re-copies the token sheet.

## Proof

| Command | Proves |
| --- | --- |
| the three `grep`s | both paths and the ADR filename are actually written in the document |
| the three `test -f`s | each of those is a path that exists. A grep proving a filename appears in prose says nothing about whether the file is there — this is the pair that catches a link to a renamed ADR |
| `npm run check` | the documentation ticket changed no behaviour |

Watch it fail: misspell the ADR filename by one character and the matching `test -f` goes red while
the `grep` stays green — which is exactly why both are here.

## Acceptance criteria

- [ ] `CONTRIBUTING.md`'s web client section says `web-client/src/styles/tokens.css` is a copy of
      `design/tokens/tokens.css` and must not be edited in place
- [ ] It says `npm run check` fails on a colour literal outside the token layer
- [ ] It says a missing value is an `EPIC-06` ticket
- [ ] It links `ADR-0024`, and that file exists
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
