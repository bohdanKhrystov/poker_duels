---
schema: 2
id: TASK-130607
title: A bet's chips stand at the rival's bet line, and there is still only one bet line
type: task
status: ready
parent: STORY-1306
module: web-client
estimate: S
tier: haiku
review: standard
files_touched: 2
labels: [client, table]
depends_on: [TASK-130606]
verify:
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npm run --silent check
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/table/DuelTable.test.tsx 2>&1 | grep -qE '^ *Tests +30 passed \(30\)$'
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/table/no-derivation.test.tsx 2>&1 | grep -qE '^ *Tests +7 passed \(7\)$'
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/table/null-view.test.tsx 2>&1 | grep -qE '^ *Tests +7 passed \(7\)$'
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/lobby/Lobby.test.tsx 2>&1 | grep -qE '^ *Tests +80 passed \(80\)$'
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/e2e/whole-duel.test.tsx 2>&1 | grep -qE '^ *Tests +8 passed \(8\)$'
  - awk '{ n += gsub(/<ChipPile/, "&") } END { exit (n != 1) }' web-client/src/table/DuelTable.tsx
  - awk '{ n += gsub(/<BetLine/, "&") } END { exit (n != 1) }' web-client/src/table/DuelTable.tsx
  - awk '{ n += gsub(/formatChips\(props.committed\)/, "&") } END { exit (n != 1) }' web-client/src/table/DuelTable.tsx
  - awk '{ n += gsub(/p \.chip-pile/, "&") } END { exit (n < 3) }' web-client/src/table/DuelTable.test.tsx
  - python3 .github/scripts/lint_tickets.py
---

## Goal

When the rival has chips out on this street, a pile stands at their bet line beside the figure the
view carries — and the hero, who has no bet line and gains none, has their commitment stated by the
pot figure exactly as `ADR-0107` §1 already states it.

## The asymmetry, named so nobody repairs it by accident

`DuelTable.tsx` renders **one** `<BetLine>`, for the rival, and has since it was written. The hero
has none: their own street chips are inside `Pot 2,850` (`ADR-0107` §1's total, which sums both
seats' `committedThisStreet`) and their stack numeral falls by the same amount. Both merged screen
cards draw exactly that — `class="bet-line"` reads 2 on `duel-table.html` and 3 on
`duel-table-states.html`, one per frame.

**Adding a hero bet line would be a new surface on a screen that stands 0.09375 px from the fence**
(`ADR-0103`, `ADR-0106` §4, `STORY-1215`), and it would owe a card, a fit measurement and a
verdict. It is not in this story. The third test below pins the count at one so the asymmetry is a
decision on record rather than an omission somebody quietly fixes.

So *"when bet is maid chips going to the pot"* is drawn twice over, from both ends: the rival's
chips appear at their bet line, and every seat's committed chips appear in the pot's own arriving
pile (`TASK-130606`).

## What is already true, measured on `develop` 2026-09-03

- `DuelTable.test.tsx` reports **27**; `no-derivation.test.tsx` **7**; `null-view.test.tsx` **7**;
  `Lobby.test.tsx` **80**; `whole-duel.test.tsx` **8**.
- `DuelTable.tsx` renders exactly one `<p>` — `BetLine`'s — so `p .chip-pile` is an exact selector
  for the bet line's pile and matches neither the seat plates' (inside `div`s) nor the pot's.
- **The blast radius is empty, probed:** a silent pile planted here and in the two other components
  at once left the whole client suite at **1053 of 1053** green.
- `BetLine` already reserves its height when there is nothing to say
  (`min-h-[calc(var(--pd-fs-small)*var(--pd-lh-body))]`), so nothing below it moves when the pile
  appears — the fit argument this ticket owes, and it is structural rather than measured.

## Files

| File | Action |
| --- | --- |
| `web-client/src/table/DuelTable.tsx` | modify |
| `web-client/src/table/DuelTable.test.tsx` | modify |
| `web-client/src/table/ChipPile.tsx` | read |
| `web-client/src/table/view-fixture.ts` | read |

## Scope

- **`DuelTable.tsx`, inside `BetLine` only:** import `ChipPile` and render it first inside the
  existing `props.committed > 0` fragment, before the word `committed`:

  ```tsx
  {props.committed > 0 && (
    <>
      <ChipPile key={props.committed} />
      committed{" "}
      <span className="font-mono text-text tabular-nums">
        {formatChips(props.committed)}
      </span>
    </>
  )}
  ```

  Nothing else in the file moves — not the seat blocks, not the centre block's `flex-1`, not the
  hand widths, not `revealStep`, not `lastAct`, and not `BetLine`'s own `min-h`.
- **Keyed on `props.committed`**, so a raise remounts the pile and replays `pd-chip-flight` once,
  and a frame that leaves the commitment alone changes no key and interrupts nothing. The figure
  beside it is already the server's new one when the flight starts.
- **No second `<BetLine>`.** A gate pins the component at one call site.
- **The table gains no `aria-label`, no `title` and no new string.**
- **Run `npm run format` before `format:check`.**
- **No `data-testid` and no test-only prop** (`ADR-0100` §5).

## Out of scope

- **A bet line for the hero.** Refused above; a new surface, a card and a fit measurement, and not
  this story's. Not yet ticketed.
- **Retiring the rival's bet line.** `ADR-0107` §4's question is drawn on `TASK-130603`'s frame and
  the human's eye answers it (`ADR-0024` §3). This ticket ships the **additive** half only: the
  line, its word and its figure are untouched. Acting on the verdict is a later ticket that exists
  only if the verdict asks for one.
- **The seat plates and the pot.** `TASK-130605` and `TASK-130606`, both merged.
- **`null-view.test.tsx`.** `TASK-130608` amends it; this ticket pins it at 7 to show it did not
  move.

## Tests

`DuelTable.test.tsx` — **3** added to the 27 it has, so the file reports **30**.

| Test | Proves |
| --- | --- |
| `stands chips at the rival's bet line beside the server's own figure` | with the rival's `committedThisStreet: 400`: `container.querySelectorAll("p .chip-pile")` has length **1**, and the figure `400` is printed beside the word `committed`. The pile is added to the fact, never in place of it |
| `draws no chips at a bet line with nothing on it` | with the rival's `committedThisStreet: 0`: `p .chip-pile` has length **0**, and `container.querySelectorAll(".chip-pile").length` is still greater than 0 — the seat plates and the pot still have theirs. Without that second half this would be an assertion about a selector that could match nothing anywhere |
| `gives the hero no bet line of their own` | with the **hero** committed `900` and the rival committed `0`: `container.querySelectorAll("p")` has length **1** and `p .chip-pile` has length **0**. The hero's 900 is stated by the pot figure, which the same render prints — a second bet line would be a surface no card draws |

The 27 merged tests do not move: none queries a class list or a `<p>` count, and the pile adds no
text node, no label and no number. Measured, not assumed — the plant left the file at 27 green.

`no-derivation.test.tsx` (7), `null-view.test.tsx` (7), `Lobby.test.tsx` (80) and
`whole-duel.test.tsx` (8) are pinned unmoved. `whole-duel.test.tsx` is one of `ADR-0100` §3's
recorded-frame suites and is named here for exactly that reason: **the flight is CSS over a
transition that already happened, so no frame is re-recorded, no schedule is added, `boot.ts` is
not opened and `drive-duel.tsx` is not touched.**

## Acceptance criteria

- [ ] `DuelTable.test.tsx` reports `Tests  30 passed (30)`
- [ ] `DuelTable.stands chips at the rival's bet line beside the server's own figure` passes
- [ ] `DuelTable.draws no chips at a bet line with nothing on it` passes
- [ ] `DuelTable.gives the hero no bet line of their own` passes
- [ ] `no-derivation.test.tsx` still reports `Tests  7 passed (7)`
- [ ] `null-view.test.tsx` still reports `Tests  7 passed (7)`
- [ ] `Lobby.test.tsx` still reports `Tests  80 passed (80)`
- [ ] `whole-duel.test.tsx` still reports `Tests  8 passed (8)`
- [ ] `DuelTable.tsx` contains exactly one `<ChipPile`, exactly one `<BetLine` and exactly one
      `formatChips(props.committed)`
- [ ] `cd web-client && npm run check` exits 0
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
