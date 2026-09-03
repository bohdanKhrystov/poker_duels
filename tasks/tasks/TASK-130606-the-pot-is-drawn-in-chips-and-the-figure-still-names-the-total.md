---
schema: 2
id: TASK-130606
title: The pot is drawn in chips, and the award line takes them away again
type: task
status: backlog
parent: STORY-1306
module: web-client
estimate: S
tier: haiku
review: standard
files_touched: 2
labels: [client, table]
depends_on: [TASK-130605]
verify:
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npm run --silent check
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/table/PotStrip.test.tsx 2>&1 | grep -qE '^ *Tests +10 passed \(10\)$'
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/table/no-derivation.test.tsx 2>&1 | grep -qE '^ *Tests +7 passed \(7\)$'
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/table/DuelTable.test.tsx 2>&1 | grep -qE '^ *Tests +27 passed \(27\)$'
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/table/null-view.test.tsx 2>&1 | grep -qE '^ *Tests +7 passed \(7\)$'
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/table/SeatPlate.test.tsx 2>&1 | grep -qE '^ *Tests +15 passed \(15\)$'
  - awk '{ n += gsub(/<ChipPile/, "&") } END { exit (n != 1) }' web-client/src/table/PotStrip.tsx
  - awk '{ n += gsub(/potCommittedToTheHand/, "&") } END { exit (n != 2) }' web-client/src/table/PotStrip.tsx
  - awk '{ n += gsub(/aria-label/, "&") } END { exit (n != 0) }' web-client/src/table/PotStrip.tsx
  - awk '{ n += gsub(/chip-disc/, "&") } END { exit (n < 2) }' web-client/src/table/PotStrip.test.tsx
  - python3 .github/scripts/lint_tickets.py
---

## Goal

The pot strip draws a pile of chips beside the figure `ADR-0107` made it print, and draws **none**
beside the award line — because by then the chips have gone to a stack, and a pile still sitting in
the middle would show a pot that is no longer there.

## Why the award line is the interesting half

`ADR-0095` replaces the `Pot N` figure with `You win 4,850` at `COMPLETE`, stating only amounts
this client actually received. That is the moment the human's sentence describes — *"when player
won chips goin to their stack"* — and the pot's own pile is what has to stop being drawn for the
stack's arriving pile (`TASK-130602`'s two `pile flying` frames on `duel-table-states.html`) to
mean anything.

So the pile's condition is `awardLine === null && total > 0`, and the third test below drives it
with a **non-zero** pot on purpose: with `pot: 4850` the guard is load-bearing, and deleting it
puts a pile back on screen. A fixture with a zero pot would have passed either way.

## The one named sum, and why the hoist matters

`ADR-0107` §5 narrows the never-derives guard by **exactly one** quantity: `view.pot` plus both
seats' `committedThisStreet`. `PotStrip.tsx` computes it once, in `potCommittedToTheHand`. This
ticket hoists that one call into a `const total` used for **both** the printed figure and the
pile's `key`, so the file still calls it exactly once and no second figure is derived anywhere. A
gate pins `potCommittedToTheHand` at **2** occurrences — the function and the one call.

**The pile prints nothing.** `ADR-0115` §1 is satisfied by the figure beside it, which is where
the amount has always been.

## What is already true, measured on `develop` 2026-09-03

- `PotStrip.test.tsx` reports **7**; `no-derivation.test.tsx` **7**; `DuelTable.test.tsx` **27**;
  `null-view.test.tsx` **7**; `SeatPlate.test.tsx` **15** after `TASK-130605`.
- `PotStrip.tsx` carries `potCommittedToTheHand` **2** times and **no** `aria-label` at all.
- **The blast radius is empty, probed:** a silent pile planted in this component (and two others)
  left the whole client suite at **1053 of 1053**. A pile carrying a **derived** figure in a
  `title` reddened `no-derivation.test.tsx` twice — so its 7 below is a live gate, not a formality.

## Files

| File | Action |
| --- | --- |
| `web-client/src/table/PotStrip.tsx` | modify |
| `web-client/src/table/PotStrip.test.tsx` | modify |
| `web-client/src/table/ChipPile.tsx` | read |
| `web-client/src/table/view-fixture.ts` | read |

## Scope

- **`PotStrip.tsx`:** hoist the sum once and mount the pile immediately before the amount span:

  ```tsx
  const total = potCommittedToTheHand(view);
  ...
  {awardLine === null && total > 0 && <ChipPile key={total} />}
  <span className="font-mono text-large tabular-nums">
    {awardLine ?? <>Pot&nbsp;{formatChips(total)}</>}
  </span>
  ```

  Nothing else in the file moves — not `awardLineFor`, not `awardsForHand`, not `STREET_NAMES`,
  not the meta line.
- **Keyed on `total`**, so a pot that grows remounts the pile and replays `pd-chip-flight` once;
  a frame that leaves the total alone changes no key and interrupts nothing. The figure is already
  the server's new one when the flight starts (`ADR-0102` §6 — the client chooses **when** to
  paint, never **what**).
- **The strip gains no `aria-label` and no `title`**, and a gate pins `aria-label` at zero in the
  file. The pile is `aria-hidden` by construction inside `ChipPile`.
- **Run `npm run format` before `format:check`.**
- **No `data-testid`, no test-only prop** (`ADR-0100` §5); the tests query `.chip-pile` and
  `.chip-disc`.

## Out of scope

- **What `Pot` means.** `ADR-0107` fixed it and `TASK-130103` shipped it. This ticket draws the
  figure and does not change it.
- **The award line's words.** `ADR-0095` owns them; nothing here mints or moves a string.
- **The seat plates and the bet line.** `TASK-130605` (merged) and `TASK-130607`.
- **A second sum.** Anything that would need one is out of this story entirely.
- **The runout's pacing.** `props.street` and `revealStep` are untouched; a step is a fact
  arriving and is not motion (`ADR-0115` §3).

## Tests

`PotStrip.test.tsx` — **3** added to the 7 it has, so the file reports **10**.

| Test | Proves |
| --- | --- |
| `draws a pile beside the pot, and the figure still names the total` | with `pot: 2450` and seats committed `125` / `825`: `getByText(/Pot 3,400/)` still resolves **and** exactly one `.chip-pile` is on screen. The pile is beside the fact, never instead of it |
| `draws the same pile for a small pot and a large one` | **two inputs, neither the fixture's default of 30**: `pot: 150` and `pot: 13400` each render exactly **3** `.chip-disc`. A pile sized from the amount would differ, and would have invented a denomination |
| `draws no pile beside the award line` | both halves in one test. With `street: "COMPLETE"`, `pot: 4850`, `handNumber: 14`, `viewerSeat: 0` and a narration of `HandStarted` then `PotAwarded`: `getByText("You win 4,850")` resolves and there are **zero** `.chip-pile`. With the **same view and no narration**: `getByText(/Pot 4,850/)` resolves and there is exactly **one** — the guard on the guard, and the proof that it is the award line and not a zero total doing the work |

The narration literal the third test needs, field for field from `protocol.gen.ts`:

```tsx
narration={[
  { type: "HandStarted", sequence: 1, handNumber: 14, buttonSeat: 0, smallBlind: 75, bigBlind: 150, stacks: [13400, 4550] },
  { type: "PotAwarded", sequence: 2, seat: 0, amount: 4850 },
]}
```

The 7 merged tests do not move: none of them queries a class list, and the pile adds no text node,
no label and no number.

`no-derivation.test.tsx` (7), `DuelTable.test.tsx` (27), `null-view.test.tsx` (7) and
`SeatPlate.test.tsx` (15) are pinned unmoved, all measured.

## Acceptance criteria

- [ ] `PotStrip.test.tsx` reports `Tests  10 passed (10)`
- [ ] `PotStrip.draws a pile beside the pot, and the figure still names the total` passes
- [ ] `PotStrip.draws the same pile for a small pot and a large one` passes
- [ ] `PotStrip.draws no pile beside the award line` passes
- [ ] `no-derivation.test.tsx` still reports `Tests  7 passed (7)`
- [ ] `DuelTable.test.tsx` still reports `Tests  27 passed (27)`
- [ ] `null-view.test.tsx` still reports `Tests  7 passed (7)`
- [ ] `SeatPlate.test.tsx` still reports `Tests  15 passed (15)`
- [ ] `PotStrip.tsx` contains exactly one `<ChipPile`, exactly two `potCommittedToTheHand` and
      **no** `aria-label`
- [ ] `cd web-client && npm run check` exits 0
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
