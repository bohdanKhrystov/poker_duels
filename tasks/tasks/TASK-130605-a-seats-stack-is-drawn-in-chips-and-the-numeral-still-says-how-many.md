---
schema: 2
id: TASK-130605
title: A seat's stack is drawn in chips, and the numeral still says how many
type: task
status: backlog
parent: STORY-1306
module: web-client
estimate: S
tier: haiku
review: standard
files_touched: 2
labels: [client, table]
depends_on: [TASK-130604]
verify:
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npm run --silent check
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/table/SeatPlate.test.tsx 2>&1 | grep -qE '^ *Tests +15 passed \(15\)$'
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/table/no-derivation.test.tsx 2>&1 | grep -qE '^ *Tests +7 passed \(7\)$'
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/table/DuelTable.test.tsx 2>&1 | grep -qE '^ *Tests +27 passed \(27\)$'
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/table/null-view.test.tsx 2>&1 | grep -qE '^ *Tests +7 passed \(7\)$'
  - awk '{ n += gsub(/<ChipPile/, "&") } END { exit (n != 1) }' web-client/src/table/SeatPlate.tsx
  - awk '{ n += gsub(/aria-label/, "&") } END { exit (n != 1) }' web-client/src/table/SeatPlate.tsx
  - awk '{ n += gsub(/formatChips\(props.seat.stack\)/, "&") } END { exit (n != 1) }' web-client/src/table/SeatPlate.tsx
  - awk '{ n += gsub(/chip-disc/, "&") } END { exit (n < 2) }' web-client/src/table/SeatPlate.test.tsx
  - python3 .github/scripts/lint_tickets.py
---

## Goal

Every seat plate on the duel table draws a pile of chips beside its stack, and the mono numeral
that has always said how many still says it — including on a busted seat, where the numeral reads
`0` and there is no pile at all.

## Why this is the whole change

`SeatPlate.tsx` already prints `formatChips(props.seat.stack)` in a `font-mono tabular-nums` span
at the end of its flex row. This ticket puts `<ChipPile />` immediately before that span, keyed on
the stack, and changes nothing else — not `onTurn`, not `seatStatus`, not the last-act pill, not
the button, not the numeral.

**Keyed on `props.seat.stack`, and that is the only mechanism.** Changing the key remounts the
pile, which replays `pd-chip-flight` once; an unchanged stack changes no key and interrupts
nothing. That is `ADR-0102`'s shape kept: the client chooses when to paint, never what the fact
is — the numeral is already the server's new one at the instant the flight starts, so the motion
is decoration over a change that has **already happened** and no quantity is ever withheld. The
component card's caption says it in four words: *the numeral has already moved*.

**`ADR-0115` §1 holds by construction.** The stack's value is in the numeral, which is not the
animation. Stop the animation — by the sheet's reduced-motion block or by anything else — and the
seat still says how many chips it has.

## What is already true, measured on `develop` 2026-09-03

- `SeatPlate.test.tsx` reports **12**; `no-derivation.test.tsx` **7**; `DuelTable.test.tsx` **27**;
  `null-view.test.tsx` **7**.
- **The blast radius is empty, probed rather than reasoned about.** A silent `aria-hidden` pile
  planted in `SeatPlate`, `PotStrip` *and* `DuelTable`'s bet line at once left the whole client
  suite at **1053 of 1053** green. No merged assertion in this repository observes what this
  ticket adds, so no test file outside the two below is in the budget.
- **The two guards that would catch a pile that spoke, and which one it is.** Giving the planted
  pile an `aria-label` carrying the seat's own stack reddened **exactly one** test —
  `SeatPlate.test.tsx`'s `speaks the mark to nobody`, which pins `[aria-label]` at 1 and `[title]`
  at 0 on the plate. `no-derivation.test.tsx` did **not** catch it, because the stack is a number
  the view carries and its sweep admits it. `no-derivation.test.tsx` *did* catch a pile speaking a
  **derived** figure (2 failures). Both files are pinned below, and neither's assertions move:
  the pile is `aria-hidden`, prints nothing and adds no number.
- `SeatPlate.tsx` carries exactly one `aria-label` today, `the button`.

## Files

| File | Action |
| --- | --- |
| `web-client/src/table/SeatPlate.tsx` | modify |
| `web-client/src/table/SeatPlate.test.tsx` | modify |
| `web-client/src/table/ChipPile.tsx` | read |
| `web-client/src/table/view-fixture.ts` | read |

## Scope

- **`SeatPlate.tsx`:** import `ChipPile`, and immediately before the stack numeral's span render

  ```tsx
  {props.seat.stack > 0 && <ChipPile key={props.seat.stack} />}
  ```

  Nothing else in the component moves. `formatChips(props.seat.stack)` stays exactly where and as
  it is, and a gate pins it at one occurrence.
- **`> 0`, not truthiness.** A busted seat draws no pile; the numeral `0` still stands, because
  *nothing to draw* is not *nothing to say*. That is the component card's `a busted seat draws no
  chips` row, implemented.
- **The plate keeps exactly one `aria-label`.** No name, no title, no text for the pile. Gated.
- **Run `npm run format` before `format:check`** — `prettier-plugin-tailwindcss` reorders class
  lists.
- **No new prop, no `data-testid`, no test-only door** (`ADR-0100` §5). The tests query
  `container.querySelectorAll(".chip-pile")` and `.chip-disc`.

## Out of scope

- **The pot and the bet line.** `PotStrip` is `TASK-130606`, `DuelTable`'s bet line is
  `TASK-130607`.
- **`null-view.test.tsx`.** `TASK-130608` amends `EPIC-13`'s standing contract. This ticket cannot
  redden it — `Lobby.tsx` renders `WaitingTable` when `view === null`, and `WaitingTable` draws its
  own seat rows and never mounts `SeatPlate` — and it is pinned at 7 to prove that rather than
  assume it.
- **A pile whose size depends on the stack.** Refused in `TASK-130601` and `TASK-130604`, and the
  second test below is what enforces it here.
- **`app.css` and the token sheet.** Merged upstream; a wrong value is a repair ticket.
- **The rival's bet line and the hero's absent one.** `TASK-130607`.

## Tests

`SeatPlate.test.tsx` — **3** added to the 12 it has, so the file reports **15**. Each uses the
file's existing `plate()` helper.

| Test | Proves |
| --- | --- |
| `draws a pile beside the stack, and the numeral still says how many` | with `stack: 13400`: exactly one `.chip-pile`, and `getByText("13,400")` still resolves. The pile is added **beside** the fact, never in place of it — `ADR-0115` §1 in one assertion |
| `draws the same pile for a small stack and a large one` | **two inputs, neither the fixture's default of 500**: `stack: 150` and `stack: 13400` each render exactly **3** `.chip-disc`. A pile whose size read off the amount would differ, and would have invented a denomination the server never sent |
| `draws no pile for a busted seat, and still says nothing` | with `stack: 0`: zero `.chip-pile`, `getByText("0")` resolves, and `container.querySelectorAll("[aria-label], [title]")` has length 0 — the plate's one label is the button's, and this fixture has none |

The 12 merged tests do not move. `speaks the mark to nobody` counts `[aria-label]` at 1 and
`[title]` at 0 under the plate; the pile carries neither, measured — planting it left the whole
suite green, and planting a *speaking* pile reddened precisely that test, which is why it is worth
keeping exactly as written.

`no-derivation.test.tsx` (7), `DuelTable.test.tsx` (27) and `null-view.test.tsx` (7) are pinned
unmoved, all three measured at this commit.

## Acceptance criteria

- [ ] `SeatPlate.test.tsx` reports `Tests  15 passed (15)`
- [ ] `SeatPlate.draws a pile beside the stack, and the numeral still says how many` passes
- [ ] `SeatPlate.draws the same pile for a small stack and a large one` passes
- [ ] `SeatPlate.draws no pile for a busted seat, and still says nothing` passes
- [ ] `no-derivation.test.tsx` still reports `Tests  7 passed (7)`
- [ ] `DuelTable.test.tsx` still reports `Tests  27 passed (27)`
- [ ] `null-view.test.tsx` still reports `Tests  7 passed (7)`
- [ ] `SeatPlate.tsx` contains exactly one `<ChipPile`, exactly one `aria-label` and exactly one
      `formatChips(props.seat.stack)`
- [ ] `cd web-client && npm run check` exits 0
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
