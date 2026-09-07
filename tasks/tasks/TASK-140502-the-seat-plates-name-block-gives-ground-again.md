---
schema: 2
id: TASK-140502
title: The seat plate's name block gives ground again
type: task
status: backlog
parent: STORY-1405
module: web-client
estimate: S
tier: sonnet
review: standard
files_touched: 3
labels: [client, table, bug, manual-verify]
depends_on: [TASK-140501]
verify:
  - cd web-client && npm ci && FORCE_COLOR=0 NO_COLOR=1 npm run --silent check
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/table/SeatPlate.test.tsx > "${TMPDIR:-/tmp}/plate-after.txt" 2>&1; grep -qF 'Tests  21 passed (21)' "${TMPDIR:-/tmp}/plate-after.txt"
  - cd web-client && cp src/table/SeatPlate.tsx "${TMPDIR:-/tmp}/SeatPlate.m1.tsx" && perl -0pi -e 's/min-w-\[0px\] flex-1/min-w-0 flex-1/' src/table/SeatPlate.tsx && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/table/SeatPlate.test.tsx > "${TMPDIR:-/tmp}/plate-m1.txt" 2>&1; cp "${TMPDIR:-/tmp}/SeatPlate.m1.tsx" src/table/SeatPlate.tsx; cmp -s "${TMPDIR:-/tmp}/SeatPlate.m1.tsx" src/table/SeatPlate.tsx && grep -qF 'Tests  1 failed | 20 passed (21)' "${TMPDIR:-/tmp}/plate-m1.txt" && grep -qF 'lets the name block give ground, so the row can fit' "${TMPDIR:-/tmp}/plate-m1.txt"
  - cd web-client && cp src/table/SeatPlate.tsx "${TMPDIR:-/tmp}/SeatPlate.m2.tsx" && perl -0pi -e 's/block truncate font-medium/block font-medium/' src/table/SeatPlate.tsx && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/table/SeatPlate.test.tsx > "${TMPDIR:-/tmp}/plate-m2.txt" 2>&1; cp "${TMPDIR:-/tmp}/SeatPlate.m2.tsx" src/table/SeatPlate.tsx; cmp -s "${TMPDIR:-/tmp}/SeatPlate.m2.tsx" src/table/SeatPlate.tsx && grep -qF 'Tests  1 failed | 20 passed (21)' "${TMPDIR:-/tmp}/plate-m2.txt" && grep -qF 'truncates the name rather than wrapping it' "${TMPDIR:-/tmp}/plate-m2.txt"
  - cd web-client && npm run build > /dev/null && cat dist/assets/*.css | grep -qF '.min-w-\[0px\]{min-width:0}'
  - sh -c 'test "$(grep -c "min-w-0" web-client/src/table/SeatPlate.tsx)" = "0"'
  - awk 'index($0, "min-w-[0px]") { n++ } END { exit (n != 1) }' web-client/src/table/SeatPlate.tsx
  - python3 -c "import re,sys;t=open('tasks/epics/EPIC-14-the-name-the-showdown-and-the-fit.md',encoding='utf-8').read();p=t.split('### The 390 × 664 reading');sys.exit(0 if len(p)==2 and 'TASK-140502' in p[1] and len(re.findall(r'[|] *[0-9]+ */ *[0-9]+ *[|] *[0-9]+ */ *[0-9]+ *[|]', p[1]))>=4 else 1)"
  - python3 .github/scripts/lint_tickets.py
---

## Goal

The block holding a seat's name and status is allowed to shrink again, so the plate's row stops
overflowing and the **stack figure stops being painted past the screen edge**. Measured, the
document's `scrollWidth` at 390 falls from **443** to **390** against a `clientWidth` of 390, and no
plate's last child lands outside the viewport. One class, two tests, and the after-row of
`TASK-140501`'s receipt.

## The defect, measured

**One utility generates no CSS, and everything else follows from that.**
`web-client/src/table/SeatPlate.tsx:50` is:

```tsx
      <span className="min-w-0 flex-1">
```

`src/styles/app.css:55` sets `--spacing: initial;`, which switches off Tailwind's bare spacing
scale on purpose. `min-w-0` compiles to `min-width: calc(var(--spacing) * 0)` and is **dropped**.
Measured on the built stylesheet, the only `min-w` rule in `web-client/dist/assets/*.css` is
`.min-w-\[2ch\]{min-width:2ch}` — an **arbitrary** value, which needs no theme entry — while
`.flex-1{flex:1}`, `.truncate{…}` and `.last-act{…}` are all present, and named steps still work
(`.min-h-7{min-height:var(--spacing-7)}`). **The bare `0` is the one that vanishes.**

Without `min-width: 0` the block keeps flexbox's automatic minimum size, cannot shrink below its own
min-content, and the row overflows the plate instead. Measured, it sits at a floor of **64.94 px** in
every shape read. The plate's other children — the last-act mark, the clock figure, the dealer pill,
`Timebank 3:00`, the pile, the stack — are all `white-space: nowrap` or joined by a non-breaking
space, so none of them can give either, and the tail is painted off screen:

| the plate carries | plate `scrollWidth` / `clientWidth` | stack figure past the edge |
| --- | --- | --- |
| on turn · `Raise to 10,000` · `24` · `Timebank 3:00` · pile · `10,000` | **434** / 379 | **+50.25** |
| off turn · `Raise to 10,000` · `D` · `Timebank 3:00` · pile · `10,000` | **437** / 379 | **+53.19** |
| on turn · `Raise to 200` · `24` · `Timebank 3:00` · pile · `1,500` | **405** / 379 | **+21.36** |
| on turn · `24` · `D` · `Timebank 3:00` · pile · `10,000` — **no mark** | 379 / 379 | −21.00 |
| off turn · `Raise to 10,000` · `D` · pile · `10,000` — **no clock** | 379 / 379 | −21.00 |

The last two rows are why this was never caught: the plate as `ADR-0106` measured it fits, and the
plate as `ADR-0113` measured it fits. It takes the last-act mark and the timebank **together**.

**The merged card already specifies the behaviour this restores.**
`design/components/seat-and-pot.html` — copied faithfully into `design/screens/duel-table.html` —
declares `.seat .who { flex: 1; min-width: 0; }` and says in its own comment that the mark, the
clock and the timebank *"ride as children of `.seat`'s own flex row … `.who` gives ground by
truncating the name first and the plate never grows a pixel taller."* Measured at the same strings,
the card's `.who` shrinks to **0** and its phone frame reads **390 / 390**. This is drift from a
merged card back to a merged card — conformance, not design — so **no file under `design/` is
opened.**

## Files

| File | Action |
| --- | --- |
| `web-client/src/table/SeatPlate.tsx` | modify |
| `web-client/src/table/SeatPlate.test.tsx` | modify |
| `tasks/epics/EPIC-14-the-name-the-showdown-and-the-fit.md` | modify |

The epic is in the budget because this ticket changes the number `TASK-140501` wrote down, and
`ADR-0088` §3's receipt is only worth something if the ticket that moves it records the move. Read
[`ADR-0121`](../../docs/adr/ADR-0121-the-table-reflows-never-scales-and-the-shape-is-measured-on-the-device.md)
§§1 and 3 only if you need the reason there is no transform anywhere in this diff. **Nothing else is
opened.**

## Scope

- **`web-client/src/table/SeatPlate.tsx:50`** — one attribute. The line becomes exactly:

  ```tsx
        <span className="min-w-[0px] flex-1">
  ```

  Checked: `format:check` passes on that spelling unmodified, so no other line moves. **This is the
  whole source change.** No child of the plate is added, removed, reordered or reclassed; the
  `truncate` on the name span stays; no `style`, no `transform`, no width, no font size.

  **The spelling is forced by a merged gate, not chosen.** Teaching the theme a zero step is not
  available: `src/styles/theme.test.ts` admits only `--x-*: initial;`, `--x: initial;` and
  `--x: var(--pd-…);` inside `@theme static`, so `--spacing-0: 0px` fails it, and
  `--spacing-0: var(--pd-space-0)` would need a token `tokens.css` does not have — **minting**,
  which `ADR-0103` §4 reserves for the human. `min-w-[0px]` is the arbitrary-value idiom this same
  file family already uses at `min-w-[2ch]`.

- **`web-client/src/table/SeatPlate.test.tsx`** — two new tests, inserted immediately above
  `it("shows the button only on the seat that has it", …)`:

  `lets the name block give ground, so the row can fit` — take the plate's root by
  `container.querySelector(".border-l-2")`, read its **first** child, and assert its class list
  contains `min-w-[0px]` and `flex-1` as **two separate assertions in a loop**, not one combined
  string compare; then assert that **exactly one** child of the row carries `flex-1`, so a later
  refactor that makes a second child elastic fails here.

  `truncates the name rather than wrapping it` — assert the span holding the name carries
  `truncate`. Without it a name that gives ground **wraps** instead, and the plate grows taller
  against a height budget that has 17.891 px of slack (`STORY-1405`'s *Design notes*). This is the
  assertion that stops the plausible wrong fix.

- **`tasks/epics/EPIC-14`** — one row appended to `TASK-140501`'s `### The 390 × 664 reading`
  table, naming this ticket, carrying the same beat as B3 with both axis pairs after the change. The
  `verify:` gate requires the block to mention `TASK-140502` and to hold at least **four** rows.

## Out of scope

- **`ActionBar.tsx:149`'s `min-w-0`**, which is inert for the same reason and repaired by
  `TASK-140503` together with the tree-wide gate. Measured, the bar and the pot strip fit at 390 at
  the widest figures the format can produce, so nothing here waits on it. **Do not touch that file**
  — a `verify:` gate in `TASK-140503` counts on it still being wrong when that ticket starts.
- **`design/`.** The card is right; see *The defect, measured*. Nothing here opens it.
- **The name being drawn at 0 px at the widest row.** That is what the merged card does at the same
  strings, and `R3` is about **amounts** — which is what this puts back on screen. If the human
  wants the name kept, that is a **card** change first and a client change second, and their visual
  verdict may trail this merge (`ADR-0024` §3, `ADR-0091` §3). Do not invent a floor width here.
- **Any transform, `zoom` or font multiplier.** `ADR-0121` §1 forecloses them, and none is needed:
  measured, this change costs the height axis nothing.
- **`ADR-0103` §3's give order.** It is not spent — no whitespace tightens, no card narrows, no
  number shrinks. If your after-reading still overflows, you have reached the end of the list:
  **stop and register a `DEC`** (`ADR-0121` §5), do not take the next thing you see.
- **The e2e suites.** No file under `web-client/src/e2e/` is edited and `scripted-duel.gen.json` is
  byte-unchanged.

## Tests

`SeatPlate.test.tsx` — two new tests, taking the file from **19** to **21**.

| Test | Proves |
| --- | --- |
| `lets the name block give ground, so the row can fit` | the name block carries a `min-width` that actually generates, and is the row's only elastic child |
| `truncates the name rather than wrapping it` | giving ground costs width, never height |

**Both were written and run at planning time, and each has its own mutation.** Measured against the
change applied:

| Gate | Proves | Reading |
| --- | --- | --- |
| `npx vitest run src/table/SeatPlate.test.tsx` | the file is whole and the two tests are new | `Tests  21 passed (21)` |
| `min-w-[0px]` → `min-w-0` | test one depends on the spelling, not merely on the class being there | `Tests  1 failed \| 20 passed (21)`, naming `lets the name block give ground, so the row can fit` |
| `block truncate font-medium` → `block font-medium` | test two depends on the truncation and **not** on test one's classes | `Tests  1 failed \| 20 passed (21)`, naming `truncates the name rather than wrapping it` |
| `npm run build` then `grep -F '.min-w-\[0px\]{min-width:0}'` over `dist/assets/*.css` | **the utility generates CSS.** A class-name assertion alone passes for `min-w-0`, which generates nothing — this is the only gate that can tell the two apart, and it is the whole defect | present — **absent today** |
| `min-w-0` appears **0** times in `SeatPlate.tsx`; `min-w-[0px]` appears **1** | the old spelling is gone and the new one landed once | 1 and 0 today — both **red** |
| `npm run check` | typecheck, lint, format and the whole suite | green — `124 files, 1168 tests`, from `1166` today |

Each mutation gate restores the file and `cmp`s it before reporting, so a red run cannot leave the
tree edited.

## Acceptance criteria

### Gated

- [ ] `SeatPlate.test.tsx` reports `Tests  21 passed (21)`
- [ ] Reverting the spelling to `min-w-0` gives `Tests  1 failed | 20 passed (21)` naming
      `lets the name block give ground, so the row can fit`, and the file is restored afterwards
- [ ] Removing `truncate` from the name span gives `Tests  1 failed | 20 passed (21)` naming
      `truncates the name rather than wrapping it`, and the file is restored afterwards
- [ ] After `npm run build`, `web-client/dist/assets/*.css` contains
      `.min-w-\[0px\]{min-width:0}`
- [ ] `min-w-0` appears **0** times in `SeatPlate.tsx` and `min-w-[0px]` appears exactly **1**
- [ ] `EPIC-14`'s `### The 390 × 664 reading` block names `TASK-140502` and holds at least **four**
      rows carrying both axis pairs
- [ ] `cd web-client && FORCE_COLOR=0 NO_COLOR=1 npm run --silent check` exits 0, reporting
      `1168 passed (1168)`
- [ ] The diff touches exactly three files

### Measured on the running stack, and pasted into the PR body

`ADR-0089` §2 bars a browser from any `verify:` block and jsdom computes no layout, so the readings
below are **acceptance criteria with a named runner and named numbers**, not gates.

**Who runs it:** the implementer, before opening the PR, at `TASK-140501`'s beat **B3**, with the
same two profiles at `size 390 664` and the same `eval` expression that ticket names. Run it
**twice — once before your change and once after** — and paste both.

- [ ] **Before**, at B3: `scrollWidth > clientWidth`, matching `TASK-140501`'s recorded reading
      within a pixel or two. If it does not reproduce, **stop** and say so in the PR body; do not
      change the class against a defect that is not there
- [ ] **After**, at B3: `scrollWidth ≤ clientWidth` **and** `scrollHeight ≤ clientHeight`, and every
      plate's `overhang` is **negative**. The predicted reading is `scrollWidth` **390** against 390
      — measured against the real component at the three reproducing shapes, the overhangs move
      `+50.25 → −14.69`, `+53.19 → −11.75` and `+21.36 → −21.00`
- [ ] **After**, at B3: `scrollHeight / clientHeight` is **unchanged** from the before reading. This
      change buys width and is measured to spend no height; a height that moved means something else
      moved with it
- [ ] `ADR-0106` §1 applies per axis: a `scrollWidth − clientWidth` of exactly **1** is judged on
      `document.documentElement.getBoundingClientRect().width`, and a true excess strictly under one
      CSS pixel is a fit. A reading of **2 or more** on either axis is `R2` `not met` with no second
      read
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.

The human's visual verdict on a rival's name drawn at zero width is theirs to give and **may trail
the merge** (`ADR-0024` §3, `ADR-0091` §3); this ticket does not wait at a pane.
