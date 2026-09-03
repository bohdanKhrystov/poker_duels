---
schema: 2
id: TASK-130608
title: No chip before the server has named a stack, and the pile is a drawing after
type: task
status: backlog
parent: STORY-1306
module: web-client
estimate: XS
tier: sonnet
review: deep
files_touched: 1
labels: [client, table, guard]
depends_on: [TASK-130607]
verify:
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npm run --silent check
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/table/null-view.test.tsx 2>&1 | grep -qE '^ *Tests +8 passed \(8\)$'
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/table/DuelTable.test.tsx 2>&1 | grep -qE '^ *Tests +30 passed \(30\)$'
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/lobby/Lobby.test.tsx 2>&1 | grep -qE '^ *Tests +80 passed \(80\)$'
  - awk '{ n += gsub(/chip-pile/, "&") } END { exit (n < 5) }' web-client/src/table/null-view.test.tsx
  - awk '{ n += gsub(/spoken\(/, "&") } END { exit (n < 16) }' web-client/src/table/null-view.test.tsx
  - awk '{ n += gsub(/digitBearing\(/, "&") } END { exit (n < 4) }' web-client/src/table/null-view.test.tsx
  - awk '{ n += gsub(/acting-mark/, "&") } END { exit (n < 3) }' web-client/src/table/null-view.test.tsx
  - awk '{ n += gsub(/last-act/, "&") } END { exit (n < 2) }' web-client/src/table/null-view.test.tsx
  - python3 .github/scripts/lint_tickets.py
---

## Goal

`EPIC-13`'s standing contract says what the chips do on a table that is not a duel: **nothing**.
`null-view.test.tsx` gains the test that proves it, with the positive control that keeps it from
passing for the wrong reason, and one narrow closure the pile has to keep after the view arrives.

## What the answer is, and why it is structural

`Lobby.tsx` renders `WaitingTable` while `state.view === null`, and `WaitingTable` draws its own
seat rows and mounts **neither `SeatPlate`, `PotStrip` nor `DuelTable`**. So no pile can reach the
host-alone screen: the three call sites `TASK-130605`–`TASK-130607` added are all under a branch
that does not run. `ADR-0110` §3 forbids a game fact there and **a stack in chips is a game fact** —
the drawing is a depiction of `SeatView.stack`, and drawing it before the server has named one
would be the client asserting a stack it was never told.

**Unlike the typed total (`TASK-130506`), this surface has no middle state.** The pile appears the
moment a `Snapshot` arrives, exactly like the acting mark and the four `ADR-0110` probes — it waits
on the view and on nothing later. That is why the walk below has two states and not three, and
saying so is the point: the field-style trap `TASK-130506` found is checked for and absent, not
overlooked.

## The closure this test adds, and the one it deliberately does not

`STORY-1304` recorded an open gap in this file: it asserts `spoken()` is closed to `[]` on the null
view, but never checks the surface's own silence **after** the Snapshot, so a mark that gained an
`aria-label` would escape the contract. It also recorded why the naive fix is wrong — a closed-set
assertion after the Snapshot reddens the day any story adds a legitimate label — and what the right
shape is: assert **the surface's own** absence from `spoken()`.

This ticket takes that shape, narrowly: after the Snapshot it asserts that **no element carrying
`chip-pile` and no descendant of one carries an `aria-label` or a `title`**. That is a statement
about this surface alone. It cannot redden because another story labels something else, and it does
redden the moment a pile starts speaking. It does **not** attempt the general repair, which is a
follow-up ticket of its own and not yet ticketed.

## What is already true, measured on `develop` 2026-09-03

- `null-view.test.tsx` reports **7**; `DuelTable.test.tsx` **30** after `TASK-130607`;
  `Lobby.test.tsx` **80**.
- The file carries `spoken(` **15** occurrences, `digitBearing(` **4**, `acting-mark` **3**,
  `last-act` **2**, `chip-pile` **0**.
- `aView()` gives both seats `stack: 500` and `pot: 30`, so a live table renders **three** piles —
  two stacks and the pot — and zero bet-line piles (`committedThisStreet: 0`). The positive control
  therefore asserts `> 0` and never a count, exactly as this file's third test does, so a later
  `EPIC-13` story changing the live table cannot redden it for an unrelated reason.

## Files

| File | Action |
| --- | --- |
| `web-client/src/table/null-view.test.tsx` | modify |
| `web-client/src/table/ChipPile.tsx` | read |
| `web-client/src/table/WaitingTable.tsx` | read |
| `web-client/src/lobby/Lobby.tsx` | read |

## Scope

- **One test added**, in the file's existing idiom, after `marks no last act before the server has
  named one`. It uses `renderNullView("ABCDEFGH")` and the file's own `spoken()` helper; it
  introduces no helper and changes none.
- **Amend the file's opening docstring** with a short paragraph for the chips, in the register of
  the three already there: the pile renders nothing on the host-alone screen because `Lobby.tsx`
  mounts `WaitingTable`, which mounts no `SeatPlate`, no `PotStrip` and no `DuelTable`; it appears
  the moment a view arrives, so it owes no third state; and it is `aria-hidden` with no text node,
  so neither `spoken()` nor the digit sweep changes shape to admit it.
- **Nothing else in the file moves.** No assertion is weakened, no closed set is reopened, no count
  becomes a `> 0` that was exact. Gates pin `spoken(`, `digitBearing(`, `acting-mark` and
  `last-act` at or above their measured counts so the guard cannot be thinned while being added to.
  The new test calls `spoken(container)` **once**, taking that count from 15 to 16, which is what
  the gate requires; `digitBearing(` (4), `acting-mark` (3) and `last-act` (2) do not move.
- **No `data-testid`, no test-only prop** (`ADR-0100` §5).

## Out of scope

- **Repairing the general `spoken()`-after-Snapshot gap** `STORY-1304` recorded. The narrow,
  surface-scoped closure here is deliberately not the general fix; the general one is a follow-up,
  not yet ticketed, and a closed-set assertion after the Snapshot is the wrong shape for it.
- **Any component or stylesheet.** This ticket is one test file. If the test fails, the defect is
  in `TASK-130605`–`TASK-130607` and the repair is a ticket against them.
- **`WaitingTable.test.tsx`.** It tests the host-alone component in isolation; this contract is
  about the **screen**, which is what `renderNullView` renders.
- **A refresh.** A resuming client gets a `Snapshot`, so its piles are drawn from the first frame
  and there is nothing to lose — unlike `ADR-0109`'s mark, which a resume does not rebuild.

## Tests

`null-view.test.tsx` — **1** added to the 7 it has, so the file reports **8**.

| Test | Proves |
| --- | --- |
| `draws no chip before the server has named a stack` | the walk. **Before any `Snapshot`:** `container.querySelectorAll(".chip-pile")` has length **0**, and `spoken(container)` is `[]` — closed, not filtered, for the reason this file's second test already gives: a name spoken through `aria-label` alone passes every digit sweep undetected. **After `store.apply({ type: "Snapshot", view: aView() })`:** `.chip-pile` length is **greater than zero** — the guard on the guard, without which the refusal above would be an assertion about a selector that matches nothing anywhere in this app — and `container.querySelectorAll(".chip-pile [aria-label], .chip-pile[aria-label], .chip-pile [title], .chip-pile[title]")` has length **0**, which is the surface-scoped closure `STORY-1304` said was the right shape |

The 7 merged tests do not move. The pile prints no text node, so
`the copy control's feedback adds one named string and no other` keeps its exhaustive six; it
speaks nothing, so `states no figure the server never stated` keeps both its closed sets; and the
host-alone branch mounts none of the three components that draw one, so nothing this story added
can reach that tree at all.

`DuelTable.test.tsx` (30) and `Lobby.test.tsx` (80) are pinned unmoved.

## Acceptance criteria

- [ ] `null-view.test.tsx` reports `Tests  8 passed (8)`
- [ ] `null-view.draws no chip before the server has named a stack` passes
- [ ] `DuelTable.test.tsx` still reports `Tests  30 passed (30)`
- [ ] `Lobby.test.tsx` still reports `Tests  80 passed (80)`
- [ ] `null-view.test.tsx` mentions `chip-pile` at least 5 times, `spoken(` at least 16 times,
      `digitBearing(` at least 4 times, `acting-mark` at least 3 times and `last-act` at least twice
- [ ] `cd web-client && npm run check` exits 0
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
