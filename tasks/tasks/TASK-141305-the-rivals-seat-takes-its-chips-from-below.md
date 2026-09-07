---
schema: 2
id: TASK-141305
title: The rival's seat takes its chips from below
type: task
status: backlog
parent: STORY-1413
module: web-client
estimate: XS
tier: haiku
review: light
files_touched: 2
labels: [client, table, bug]
depends_on: [TASK-141304]
verify:
  - cd web-client && npm ci && FORCE_COLOR=0 NO_COLOR=1 npm run --silent check
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/table/SeatPlate.test.tsx > "${TMPDIR:-/tmp}/sp-after.txt" 2>&1; grep -qF 'Tests  22 passed (22)' "${TMPDIR:-/tmp}/sp-after.txt"
  - cd web-client && cp src/table/SeatPlate.tsx "${TMPDIR:-/tmp}/sp.m1.tsx" && perl -0pi -e 's/from=\{props\.isViewer \? "above" : "below"\}/from="below"/' src/table/SeatPlate.tsx && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/table/SeatPlate.test.tsx > "${TMPDIR:-/tmp}/sp-m1.txt" 2>&1; cp "${TMPDIR:-/tmp}/sp.m1.tsx" src/table/SeatPlate.tsx; cmp -s "${TMPDIR:-/tmp}/sp.m1.tsx" src/table/SeatPlate.tsx && grep -qF 'Tests  1 failed | 21 passed (22)' "${TMPDIR:-/tmp}/sp-m1.txt" && grep -qF 'takes the chips from the side of the table the middle is on' "${TMPDIR:-/tmp}/sp-m1.txt"
  - cd web-client && cp src/table/SeatPlate.tsx "${TMPDIR:-/tmp}/sp.m2.tsx" && perl -0pi -e 's/from=\{props\.isViewer \? "above" : "below"\}/from="above"/' src/table/SeatPlate.tsx && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/table/SeatPlate.test.tsx > "${TMPDIR:-/tmp}/sp-m2.txt" 2>&1; cp "${TMPDIR:-/tmp}/sp.m2.tsx" src/table/SeatPlate.tsx; cmp -s "${TMPDIR:-/tmp}/sp.m2.tsx" src/table/SeatPlate.tsx && grep -qF 'Tests  1 failed | 21 passed (22)' "${TMPDIR:-/tmp}/sp-m2.txt" && grep -qF 'takes the chips from the side of the table the middle is on' "${TMPDIR:-/tmp}/sp-m2.txt"
  - awk 'index($0, "from={props.isViewer ? \"above\" : \"below\"}") { n++ } END { exit (n != 1) }' web-client/src/table/SeatPlate.tsx
  - sh -c '! grep -q "from=" web-client/src/table/PotStrip.tsx'
  - sh -c '! grep -q "ChipPile from" web-client/src/table/DuelTable.tsx'
  - sh -c '! grep -Eq "matchMedia|requestAnimationFrame" web-client/src/table/SeatPlate.tsx'
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/e2e > "${TMPDIR:-/tmp}/e2e-after.txt" 2>&1; grep -qF 'Test Files  7 passed (7)' "${TMPDIR:-/tmp}/e2e-after.txt" && grep -qF 'Tests  55 passed (55)' "${TMPDIR:-/tmp}/e2e-after.txt"
  - python3 .github/scripts/lint_tickets.py
---

## Goal

The winner's chips arrive from the middle of the table at **both** seats. The viewer's plate sits
below the centre block and keeps the flight it had; the rival's plate sits above it and takes its
chips from below — which is the arrow the human drew, and the last piece of `EPIC-14` item 2c.

## Why this is a ticket

`SeatPlate` is the only call site that has a side. `TASK-141304` left `from` optional with the
default every existing site already had, so this ticket is one expression and the test that proves
it reads two seats rather than one.

**The side is read off `isViewer`, not threaded from `DuelTable`,** because `DuelTable`'s own KDoc
fixes the arrangement — *"one column, rival above, board between, you below"* — and every
`SeatPlate` this product renders sits in it. A prop threaded down from the table would state the
same fact twice, in a client whose habit is to state a fact once. If the table ever draws a plate
somewhere else, that ticket threads it; nothing today does.

**It carries no fact** (`ADR-0115` §1): the award line and the stack numeral state who took the pot
and how much, and the pile is `aria-hidden` with empty text either way. Under
`prefers-reduced-motion: reduce` the sheet's one block stills both flights and the chips are simply
at the stack — `ADR-0115` §6's own sentence, satisfied without a branch.

## Files

| File | Action |
| --- | --- |
| `web-client/src/table/SeatPlate.tsx` | modify |
| `web-client/src/table/SeatPlate.test.tsx` | modify |

**Nothing else is opened.** `ChipPile.tsx` is already right when this starts. `PotStrip.tsx` and
`DuelTable.tsx` stay untouched and gated so.

## Scope

- **`web-client/src/table/SeatPlate.tsx`** — the pile line gains one prop, and `npm run format`
  wraps it:

  ```tsx
        {props.seat.stack > 0 && (
          <ChipPile
            key={props.seat.stack}
            from={props.isViewer ? "above" : "below"}
          />
        )}
  ```

  The `key` is unchanged, so what makes the pile fly is unchanged: the stack figure moved. Nothing
  else in the file changes — not the plate's classes, not the numeral, not the order of its spans.

- **`web-client/src/table/SeatPlate.test.tsx`** — one test, inserted immediately above
  `it("draws no pile for a busted seat, and still says nothing", …)`:

  `takes the chips from the side of the table the middle is on` — render through the file's own
  `plate()` helper twice, once `{ isViewer: true }` and once `{ isViewer: false }`, at the same
  `stack: 13400`. The viewer's plate holds one `.chip-flight-down` and **zero** `.chip-flight-up`;
  the rival's holds one `.chip-flight-up` and **zero** `.chip-flight-down`. **Two inputs that
  disagree**, or the test cannot tell an expression from a constant.

## Out of scope

- **`PotStrip.tsx` and `DuelTable.tsx`.** The pot receives from both seats, so it has no side, and
  the rival's bet line already takes its chips from her stack above it (`STORY-1413`'s *Out of
  scope*). Both keep the default and are gated as unopened.
- **A flight that depends on whether the stack rose or fell.** It would make the plate remember a
  previous value and would put a fact in the motion. Not ticketed.
- **Any beat, hold or clock.** `ADR-0102` §4's step stands; `STORY-1413`'s *Design notes* answer why
  the award beat needs no declared length. No store file is opened, and `src/e2e` is gated
  unchanged.
- **The seat plate's layout.** Where the pile sits on the plate, and everything else on it, is
  `ADR-0106`'s and does not move.

## Tests

`SeatPlate.test.tsx` — **19 → 20**.

| Test | Proves |
| --- | --- |
| `takes the chips from the side of the table the middle is on` | the viewer's plate flies `down` and the rival's flies `up`, each with the other spelling absent |

Nothing else in the file changes, and no assertion is weakened — the three existing pile tests
(`draws a pile beside the stack…`, `draws the same pile for a small stack and a large one`,
`draws no pile for a busted seat…`) are untouched and stay green, because they assert `.chip-pile`
and `.chip-disc`, which this ticket does not move.

**Written and run at planning time**, against the change applied and then reverted:

| Gate | Proves | Reading |
| --- | --- | --- |
| `npx vitest run src/table/SeatPlate.test.tsx` | the file is whole and the test is new | `Tests  22 passed (22)` |
| **Mutation 1** — the ternary hard-coded to `from="below"` | the rival's half is not what carries the test | `Tests  1 failed \| 21 passed (22)`, naming the new test |
| **Mutation 2** — hard-coded to `from="above"` | neither is; the expression is read | `Tests  1 failed \| 21 passed (22)`, naming the new test |
| the ternary appears exactly **1** time in `SeatPlate.tsx` | one statement, at one seat | measured |
| `PotStrip.tsx` has no `from=`, `DuelTable.tsx` no `ChipPile from` | the two sites that must keep the default did | measured |
| `npx vitest run src/e2e` | the recorded-frame suites cannot see a CSS class | `7 passed (7)`, `55 passed (55)` |
| `npm run check` | typecheck, lint, format and the whole suite | green — `124 files, 1169 tests`, from `1168` |

Both mutations restore the file and `cmp` it before the gate reports.

## Acceptance criteria

- [ ] `SeatPlate.test.tsx` reports `Tests  22 passed (22)`
- [ ] Hard-coding `from="below"` gives `Tests  1 failed | 21 passed (22)` naming
      `takes the chips from the side of the table the middle is on`, and the file is restored
- [ ] Hard-coding `from="above"` gives the same reading, naming the same test, and the file is
      restored
- [ ] `from={props.isViewer ? "above" : "below"}` appears exactly **1** time in `SeatPlate.tsx`
- [ ] `PotStrip.tsx` contains no `from=` and `DuelTable.tsx` no `ChipPile from`
- [ ] `SeatPlate.tsx` contains no `matchMedia` and no `requestAnimationFrame`
- [ ] `npx vitest run src/e2e` reports `Test Files  7 passed (7)` and `Tests  55 passed (55)`
- [ ] `cd web-client && FORCE_COLOR=0 NO_COLOR=1 npm run --silent check` exits 0, reporting
      `1169 passed (1169)`
- [ ] The diff touches exactly two files
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
