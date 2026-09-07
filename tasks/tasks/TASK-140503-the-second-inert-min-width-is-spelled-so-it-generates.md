---
schema: 2
id: TASK-140503
title: The second inert min-width is spelled so it generates, and a gate closes the class
type: task
status: backlog
parent: STORY-1405
module: web-client
estimate: XS
tier: haiku
review: light
files_touched: 2
labels: [client, table, bug]
depends_on: [TASK-140502]
verify:
  - cd web-client && npm ci && FORCE_COLOR=0 NO_COLOR=1 npm run --silent check
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/table/ActionBar.test.tsx > "${TMPDIR:-/tmp}/bar-after.txt" 2>&1; grep -qF 'Tests  37 passed (37)' "${TMPDIR:-/tmp}/bar-after.txt"
  - cd web-client && cp src/table/ActionBar.tsx "${TMPDIR:-/tmp}/ActionBar.m1.tsx" && perl -0pi -e 's/w-\[7ch\] min-w-\[0px\]/w-[7ch] min-w-0/' src/table/ActionBar.tsx && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/table/ActionBar.test.tsx > "${TMPDIR:-/tmp}/bar-m1.txt" 2>&1; cp "${TMPDIR:-/tmp}/ActionBar.m1.tsx" src/table/ActionBar.tsx; cmp -s "${TMPDIR:-/tmp}/ActionBar.m1.tsx" src/table/ActionBar.tsx && grep -qF 'Tests  1 failed | 36 passed (37)' "${TMPDIR:-/tmp}/bar-m1.txt" && grep -qF 'lets the typed total give ground rather than push the row wide' "${TMPDIR:-/tmp}/bar-m1.txt"
  - cd web-client && npm run build > /dev/null && cat dist/assets/*.css | grep -qF '.min-w-\[0px\]{min-width:0}'
  - sh -c '! grep -rq "min-w-0" web-client/src'
  - awk 'index($0, "min-w-[0px]") { n++ } END { exit (n != 1) }' web-client/src/table/ActionBar.tsx
  - python3 .github/scripts/lint_tickets.py
---

## Goal

The other `min-w-0` in this client — `ActionBar.tsx:149`, on the typed-total field — is spelled so
that it generates a rule, and a tree-wide gate makes the whole class of defect impossible: the string
`min-w-0` appears nowhere under `web-client/src/`.

## Why this is a ticket

`TASK-140502` repaired one site because it was measured to cut a stack figure off the screen.
**`ActionBar.tsx:149` is the same inert utility**, found by the same grep, and it is the only other
one in the tree:

```
web-client/src/table/ActionBar.tsx:149  className="min-w-0 ml-auto w-[7ch] …"
web-client/src/table/SeatPlate.tsx:50   className="min-w-0 flex-1"           ← repaired by TASK-140502
```

`src/styles/app.css:55`'s `--spacing: initial;` means both compile to nothing (see `TASK-140502`'s
*The defect, measured*). This one has **no measured consequence today** — rendered at 390 × 664
against the built stylesheet at the widest figures `DuelFormat.DEFAULT` can produce (pot 20,200,
`Blinds 200/400 · Hand 47`, `Fold` / `Call 200` / `Raise to 800` / `All in 13,000`, the sizing row
and the typed total), the document reads **390 / 390** and nothing overflows. So this is not a
defect repair; it is closing the door that let the first one in, and the **tree-wide gate is the
point**. A gate that can only be written once both sites are clean is a gate that has to be written
last, which is why this is its own ticket rather than a fourth file on the one before it.

## Files

| File | Action |
| --- | --- |
| `web-client/src/table/ActionBar.tsx` | modify |
| `web-client/src/table/ActionBar.test.tsx` | modify |

**Nothing else is opened.** `SeatPlate.tsx` is already right when this starts and is not touched.

## Scope

- **`web-client/src/table/ActionBar.tsx:149`** — `min-w-0` becomes `min-w-[0px]`. **Prettier moves
  it**: `prettier-plugin-tailwindcss` sorts the arbitrary value differently from the bare step, so
  after `npm run format` the attribute reads exactly:

  ```tsx
            className="ml-auto w-[7ch] min-w-[0px] rounded-medium border border-hairline bg-transparent px-3 py-2 text-right font-mono leading-tight text-text tabular-nums disabled:border-hairline disabled:text-text-faint"
  ```

  Measured: without running `npm run format`, `format:check` fails on this file and only this file.
  Nothing else on the line changes — same classes, same order otherwise, same `w-[7ch]`.

- **`web-client/src/table/ActionBar.test.tsx`** — one new test, inserted immediately above
  `it("offers no control when there is no turn", …)`:

  `lets the typed total give ground rather than push the row wide` — render through the file's own
  `bar()` helper, take `getByRole("textbox", { name: "the total" })`, and assert its class list
  contains `min-w-[0px]`.

## Out of scope

- **`SeatPlate.tsx`.** `TASK-140502`'s, already merged when this starts.
- **`w-[7ch]`, `ml-auto`, and every other class on that input.** The field's width, its alignment and
  its disabled treatment are untouched; only the minimum changes, and only from *nothing* to *zero*.
- **Any other utility that `--spacing: initial;` might silently drop.** The tree-wide gate here
  covers `min-w-0` and nothing else. A general check — every utility used in `src/` generates a rule
  — is a bigger idea, it is **not ticketed**, and inventing one here would be scope this ticket did
  not measure.
- **`design/`, the engine and the server.** Nothing crosses the socket, so this is not `atomic:`.

## Tests

`ActionBar.test.tsx` — one new test, taking the file from **36** to **37**.

| Test | Proves |
| --- | --- |
| `lets the typed total give ground rather than push the row wide` | the field carries a `min-width` that actually generates |

**Written and run at planning time.** Measured against the change applied:

| Gate | Proves | Reading |
| --- | --- | --- |
| `npx vitest run src/table/ActionBar.test.tsx` | the file is whole and the test is new | `Tests  37 passed (37)` |
| `min-w-[0px]` → `min-w-0` on that input | the test depends on the spelling | `Tests  1 failed \| 36 passed (37)`, naming `lets the typed total give ground rather than push the row wide` |
| `npm run build` then `grep -F '.min-w-\[0px\]{min-width:0}'` over `dist/assets/*.css` | the utility generates CSS — the assertion a class-name test cannot make | present |
| `! grep -rq "min-w-0" web-client/src` | **the tree-wide gate.** `min-w-[0px]` does not contain the string `min-w-0`, so this can only pass when every site is spelled the way that generates | **red** until this ticket lands; red today at 2 sites, red after `TASK-140502` at 1 |
| `min-w-[0px]` appears **1** time in `ActionBar.tsx` | it landed once and nowhere else | 0 today — **red** |
| `npm run check` | typecheck, lint, format and the whole suite | green — `124 files, 1169 tests`, from `1168` after `TASK-140502` |

The mutation gate restores the file and `cmp`s it before reporting, so a red run cannot leave the
tree edited.

## Acceptance criteria

- [ ] `ActionBar.test.tsx` reports `Tests  37 passed (37)`
- [ ] Reverting the spelling gives `Tests  1 failed | 36 passed (37)` naming
      `lets the typed total give ground rather than push the row wide`, and the file is restored
      afterwards
- [ ] `! grep -rq "min-w-0" web-client/src` exits 0 — the string appears nowhere under `src/`
- [ ] After `npm run build`, `web-client/dist/assets/*.css` contains `.min-w-\[0px\]{min-width:0}`
- [ ] `min-w-[0px]` appears exactly **1** time in `ActionBar.tsx`
- [ ] `cd web-client && FORCE_COLOR=0 NO_COLOR=1 npm run --silent check` exits 0, reporting
      `1169 passed (1169)`
- [ ] The diff touches exactly two files
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
