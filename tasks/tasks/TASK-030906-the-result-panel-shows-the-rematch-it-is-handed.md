---
schema: 2
id: TASK-030906
title: The result panel shows the rematch it is handed, and adds none of its own
type: task
status: ready
parent: STORY-0309
module: web-client
estimate: S
tier: haiku
review: standard
files_touched: 2
labels: [client, result, ui]
depends_on: [TASK-030905]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test 2>&1 | grep -qE 'Tests +549 passed \(549\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'adds no rematch of its own'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'puts the rematch it is handed above the way back'
  - cd web-client && ! grep -rqF 'offers no rematch it cannot honour' src
  - cd web-client && npm run check
---

## Goal

`DuelResult` gains the one place a rematch control can sit — between the duel's ledger and the way
back, where `design/screens/duel-end.html` puts it — and still invents nothing to put there.

## Files

| File | Action |
| --- | --- |
| `web-client/src/result/DuelResult.tsx` | modify |
| `web-client/src/result/DuelResult.test.tsx` | modify — **one existing test is replaced by two** |
| `design/screens/duel-end.html` | read — the `.stack2` that puts Rematch above Back to lobby |

## Scope

- `DuelResult`'s props gain `rematch?: ReactNode`, and the panel renders `{props.rematch}` verbatim
  between the meta line and the `<a href="/">` — no wrapper that decides anything, no branch on
  what is inside it.
- **Optional on purpose**, and say so in the KDoc: a panel with no control is a real state, not an
  omission — a client holding no seat has nothing to press, and every test that is about the verdict
  passes nothing. The panel does not know why; it renders what it is handed.
- `import type { ReactElement, ReactNode } from "react";`.
- Nothing else in the file moves: `verdictOf`, `coinLine`, `metaLine`, `verdictColour`, the heading,
  the coin line and the way back are byte-identical.

## This ticket owns the assertion its change unsettles

`DuelResult.test.tsx`'s `offers no rematch it cannot honour` was written by `TASK-030807` when the
wire could not carry one — `DEC-023` was open and `ClientMessage` had four members. `ADR-0044`
answered it, `STORY-0213` shipped `OfferRematch` and `RematchOffered`, and `PROTOCOL_VERSION` is 3
on `develop`. The test's premise is gone and its claim — *this product offers no rematch* — is
exactly what this story falsifies. It does not get to keep standing because it still passes.

It is **replaced by two**, and no assertion is weakened:

1. `adds no rematch of its own` — the same body and the same three assertions as the test it
   replaces (no button, no link, no `/rematch/i` inside the region), rendered with **no `rematch`
   prop**. The claim narrows from *the product offers none* to *the panel adds none*, which is what
   is still true and is now load-bearing: the control comes from outside or not at all.
2. `puts the rematch it is handed above the way back` — new. Rendered with
   `rematch={<button type="button">Rematch</button>}`, the button is inside the region *and*
   **precedes** the `Back to the lobby` link in document order.

Only that one `it` block changes. The other ten keep their names, bodies and fixtures.

## Out of scope

- `RematchControl` itself — `TASK-030907`.
- Passing anything into the slot — `TASK-030910`.
- `result-no-derivation.test.tsx`. It renders `DuelResult` with no `rematch`, so an optional prop
  leaves all three of its tests compiling and passing untouched; do not open it.
- The way back's own treatment. The design makes it the *ghost* button once a bright one exists —
  `TASK-030911`.

## Tests

`web-client/src/result/DuelResult.test.tsx`, describe block `"the result screen"`. One deleted, two
added.

| Test | Proves |
| --- | --- |
| `adds no rematch of its own` | with no `rematch` prop, `queryByRole("button", { name: /rematch/i })` and `queryByRole("link", { name: /rematch/i })` are both `null`, and the region *the result*'s `textContent` matches `/rematch/i` nowhere |
| `puts the rematch it is handed above the way back` | with `rematch={<button type="button">Rematch</button>}`, `getByRole("button", { name: "Rematch" })` is inside the region *the result*, and `button.compareDocumentPosition(back) & Node.DOCUMENT_POSITION_FOLLOWING` is non-zero, where `back` is the `Back to the lobby` link |

The second asserts a **position**, not a presence: a panel that rendered the slot below the way
back, or outside the region, has both elements on screen and still fails.

## Proof

| Command | Proves |
| --- | --- |
| `Tests 549 passed (549)` | one deleted and two added, from 548 |
| the two `--reporter=verbose` greps | both new names exist |
| `! grep -rqF 'offers no rematch it cannot honour' src` | the replaced test is gone rather than renamed alongside a copy |
| `npm run check` | `ReactNode` is imported as a type, and no other call site of `DuelResult` needed touching |

**Name the edit that makes each assertion red:**

1. Move `{props.rematch}` below the `<a href="/">` → `puts the rematch it is handed above the way
   back` fails on the document-position check while both elements are still on screen. Revert.
2. Render `<button type="button">Rematch</button>` unconditionally inside the panel → `adds no
   rematch of its own` fails. Revert.

Quote both in the PR.

## Acceptance criteria

- [ ] `the result screen > adds no rematch of its own` passes
- [ ] `the result screen > puts the rematch it is handed above the way back` passes
- [ ] The string `offers no rematch it cannot honour` appears nowhere under `web-client/src`
- [ ] `DuelResult.tsx` renders `{props.rematch}` between the meta line and the `Back to the lobby` link, and branches on it nowhere
- [ ] The other ten `it` blocks in `DuelResult.test.tsx` are unchanged from `develop`
- [ ] `result-no-derivation.test.tsx` is unchanged from `develop`
- [ ] `npm run --silent test` reports `Tests  549 passed (549)`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
