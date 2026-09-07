---
schema: 2
id: TASK-140204
title: The front door stands in the card's column
type: task
status: done
parent: STORY-1402
module: web-client
estimate: XS
tier: sonnet
review: standard
files_touched: 2
labels: [client, lobby, bug, manual-verify]
depends_on: [TASK-140203]
verify:
  - cd web-client && npm ci && FORCE_COLOR=0 NO_COLOR=1 npm run --silent check
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/lobby/Lobby.test.tsx > "${TMPDIR:-/tmp}/col-after.txt" 2>&1; grep -qF 'Tests  104 passed (104)' "${TMPDIR:-/tmp}/col-after.txt"
  - cd web-client && cp src/lobby/Lobby.tsx "${TMPDIR:-/tmp}/Lobby.col.tsx" && perl -0pi -e 's|(\n    <section className=)"mx-auto flex w-full max-w-\[380px\] flex-col items-center gap-4 p-6"(>\n      \{/\* ADR-0098)|$1"p-6"$2|' src/lobby/Lobby.tsx && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/lobby/Lobby.test.tsx > "${TMPDIR:-/tmp}/col-strip.txt" 2>&1; cp "${TMPDIR:-/tmp}/Lobby.col.tsx" src/lobby/Lobby.tsx; cmp -s "${TMPDIR:-/tmp}/Lobby.col.tsx" src/lobby/Lobby.tsx && grep -qF 'Tests  1 failed | 103 passed (104)' "${TMPDIR:-/tmp}/col-strip.txt" && grep -qF "the front door stands in the card's column" "${TMPDIR:-/tmp}/col-strip.txt"
  - cd web-client && cp src/lobby/Lobby.tsx "${TMPDIR:-/tmp}/Lobby.col2.tsx" && perl -0pi -e 's|(\n      <button\n        type="button"\n        className="rounded-medium border border-transparent bg-accent-fill.*?\n      </button>)|\n      <div>$1\n      </div>|s' src/lobby/Lobby.tsx && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/lobby/Lobby.test.tsx > "${TMPDIR:-/tmp}/col-wrap.txt" 2>&1; cp "${TMPDIR:-/tmp}/Lobby.col2.tsx" src/lobby/Lobby.tsx; cmp -s "${TMPDIR:-/tmp}/Lobby.col2.tsx" src/lobby/Lobby.tsx && grep -qF 'Tests  1 failed | 103 passed (104)' "${TMPDIR:-/tmp}/col-wrap.txt" && grep -qF "the wordmark and the way into a duel are that column's own children" "${TMPDIR:-/tmp}/col-wrap.txt"
  - awk 'index($0, "className=") && index($0, "max-w-[380px]") { n++ } END { exit (n != 7) }' web-client/src/lobby/Lobby.tsx
  - awk 'index($0, "className=") && index($0, "min-h-[100dvh]") { n++ } END { exit (n != 1) }' web-client/src/lobby/Lobby.tsx
  - sh -c 'test "$(grep -c "className=\"p-6\"" web-client/src/lobby/Lobby.tsx)" = "1"'
  - awk '{ m=$0; while ((i=index(m,"Play duel"))>0) { n++; m=substr(m,i+9) } } END { exit (n != 1) }' web-client/src/lobby/Lobby.tsx
  - python3 .github/scripts/lint_tickets.py
---

## Goal

The front door's `<section>` carries the same column every other branch of `Lobby.tsx` carries, so
the wordmark and the primary control stop touching. Measured, the clearance between the two boxes
goes from **0.000 px to 12.000 px**, at 390 × 664 and at 720 × 900 alike.

## The defect, measured

**It was reproduced before this ticket was written, and the mechanism is not the one the word
"overlaps" suggests.** The front-door markup of `Lobby.tsx:388–456` was rendered against the
client's own built stylesheet in headless Chrome at both shapes, with the section's element children
joined without whitespace, because JSX elides newline-only whitespace between elements and a
pretty-printed reproduction does not — `Lobby.tsx:437` says so in as many words, and at 720 px that
one space is the whole difference between *beside* and *touching*.

Let `clearance = max(gapX, gapY)`, where `gapX = max(m.left, p.left) − min(m.right, p.right)` and
`gapY = max(m.top, p.top) − min(m.bottom, p.bottom)` for the wordmark's rect `m` and the primary
control's rect `p`. Two axis-aligned boxes are separated exactly when `clearance > 0`, and
`clearance` is then the gap in the separating axis.

| shape | `develop` | with the column |
| --- | --- | --- |
| 390 × 664 | mark `[24, 69]`, button `[69, 112.25]` — stacked, `gapY` **0.000** | `gapY` **12.000** |
| 720 × 900 | mark right `227.781`, button left `227.781` — side by side, `gapX` **0.000**, and the button sits inside 37.5 px of the mark's own 45 px band | `gapY` **12.000** |

**`clearance` is 0.000 at both shapes today.** At phone width the button's top border sits *on* the
wordmark's bottom edge; at laptop width its left border sits *on* the wordmark's right edge while
occupying the same horizontal band. That is the same neighbourhood `TASK-121303` measured one row
lower, where three doors abutted because *"the parent supplies no layout — no flex, no grid, no gap,
nothing"*. This is that section, one row up.

**What the defect is not: occlusion.** `document.elementFromPoint` was sampled at ten points across
the `Poker` and `Duels` glyph boxes, at both shapes, before and after. **Nothing is ever painted over
the wordmark** — the probe returns a node inside the `<h1>` in all forty readings. Do not write an
occlusion assertion: it is green today, it would be a gate that cannot fail, and this ticket says so
here precisely so that nobody re-derives it.

## Files

| File | Action |
| --- | --- |
| `web-client/src/lobby/Lobby.tsx` | modify |
| `web-client/src/lobby/Lobby.test.tsx` | modify |

Read [`ADR-0091`](../../docs/adr/ADR-0091-design-gets-no-agent-a-new-screen-owes-a-card-and-adoption-is-gated-where-it-is-consumed.md)
§2 only if you need the reason the card came first. **Nothing else is opened.** In particular
`design/screens/create-duel.html` is *not* opened: its `.frame` already draws the column, it was not
changed by `TASK-140201`, and you are transcribing a class list this ticket spells out in full.

## Scope

- **`web-client/src/lobby/Lobby.tsx:388`** — one attribute. The line becomes exactly:

  ```tsx
      <section className="mx-auto flex w-full max-w-[380px] flex-col items-center gap-4 p-6">
  ```

  That is byte-for-byte the class list the six other branches of this file already carry (`:133`,
  `:144`, `:168`, `:192`, `:210`, `:239`), in Prettier's own order — checked, `format:check` passes
  on it unmodified. **This is the whole source change.** No child of the section moves, no child
  gains or loses a class, and the `<h1>`, the button, the `<form>`, the profile strip, the name
  surface and the three-door `<div>` all stay exactly where and what they are.
- **`web-client/src/lobby/Lobby.test.tsx`** — two new tests, inserted immediately above
  `it("the front door's controls are dressed, not bare", …)`:

  `the front door stands in the card's column` — take `screen.getByRole("heading", { level: 1 })`,
  walk `.closest("section")`, split its `className`, and assert it contains each of `mx-auto`,
  `flex`, `w-full`, `max-w-[380px]`, `flex-col`, `items-center`, `gap-4` — **seven separate
  assertions in a loop, not one combined string compare**, so a section dressed with three of the
  seven fails on the fourth and names it.

  `the wordmark and the way into a duel are that column's own children` — assert
  `heading.parentElement` and `screen.getByRole("button", { name: "Play duel" }).parentElement` are
  both that same `<section>`. A column governs only its own children; this is what would catch a
  later refactor that wraps the button in a `<div>` and silently takes it out of the gap.

## Out of scope

- **The card's 16 px against the client's 12 px.** `gap-4` binds to `--pd-space-4` = **12 px**
  (`app.css:59`), while `create-duel.html`'s `.frame` draws `gap: var(--pd-space-5)` = **16 px**.
  The client takes **`gap-4`**, because `STORY-1402`'s merged Design notes name that exact class
  list and six sibling branches already carry it — making the front door the only 16 px column in a
  file of six 12 px columns would swap one inconsistency for another. The 4 px card↔client
  divergence is file-wide, predates this epic, applies to all seven branches at once, and is **not
  ticketed**. It is not answered here and no ticket may answer it by changing one branch.
- **Occlusion, `scrollHeight`, gutters and the fold.** Measured clean or owned elsewhere; the phone
  fit below 390 px is `DEC-136` and `STORY-1405`.
- **The `Account` door's font size.** `edits1.png`'s *bigger font* annotation lands inside the open
  **`DEC-094`**, already the product owner's. The three doors render with `className=""` today and
  **stay that way** — growing one of them here would answer `DEC-094` by increment, which is the one
  thing this ticket may not do.
- **The profile strip's `No profile yet.`** — answered by `ADR-0125` §5 and landing in `STORY-1408`,
  not here. `<ProfileStrip>` is rendered by this section and is not touched.
- **`ADR-0118` §1's empty `/`.** The `if (standing === "unknown") return <></>;` branch at `:385`
  is byte-unchanged; the column belongs to the front-door section alone.
- **Any string.** `Play duel` is already in place from `TASK-140202`; a gate pins it at 1.
- **`design/`** — the card is right and was right before this story started.

## Tests

`Lobby.test.tsx` — two new tests, taking the file from **102** to **104**.

| Test | Proves |
| --- | --- |
| `the front door stands in the card's column` | the front-door `<section>` carries all seven column utilities |
| `the wordmark and the way into a duel are that column's own children` | the mark and the primary control are direct children of that section, so the column's gap actually separates *them* |

**Both were written and run at planning time, and each has its own mutation.** Against `develop` the
first fails; with the column applied both pass; and the two mutations below redden **exactly one
test each**, which is what proves they are independent rather than two spellings of one assertion:

| Gate | Proves | Reading |
| --- | --- | --- |
| `npx vitest run src/lobby/Lobby.test.tsx` | the file is whole and the two tests are new | `Tests  104 passed (104)` |
| the column classes stripped back to `"p-6"` | test one depends on the classes | `Tests  1 failed \| 103 passed (104)`, naming `the front door stands in the card's column` |
| the button wrapped in a bare `<div>` | test two depends on the parentage, and **not** on the classes — the column survives this mutation intact | `Tests  1 failed \| 103 passed (104)`, naming `the wordmark and the way into a duel are that column's own children` |
| `max-w-[380px]` on `className=` lines is **7** | the column landed on the seventh branch and no sibling was disturbed — it is 6 on `develop` | **red** today |
| `className="p-6"` appears **once** | the bare section is gone; the one left is the result branch at `:257`, whose child `DuelResult` carries its own column | **red** today (2) |
| `min-h-[100dvh]` on `className=` lines is 1 | `ADR-0103` §5's floor was not deleted while editing layout | green — a regression guard |
| `npm run check` | typecheck, lint, format and the whole suite | green — `1161 passed (1161)` |

Each mutation gate restores the file and `cmp`s it before reporting, so a red run cannot leave the
tree edited.

## Acceptance criteria

### Gated

- [ ] `Lobby.test.tsx` reports `Tests  104 passed (104)`
- [ ] Stripping the column back to `"p-6"` gives `Tests  1 failed | 103 passed (104)` naming
      `the front door stands in the card's column`, and the file is restored afterwards
- [ ] Wrapping the primary control in a `<div>` gives `Tests  1 failed | 103 passed (104)` naming
      `the wordmark and the way into a duel are that column's own children`, and the file is
      restored afterwards
- [ ] `className=` lines carrying `max-w-[380px]` in `Lobby.tsx` number **7**; those carrying
      `min-h-[100dvh]` number **1**; `className="p-6"` appears **once**; `Play duel` appears once
- [ ] `cd web-client && FORCE_COLOR=0 NO_COLOR=1 npm run --silent check` exits 0, reporting
      `1161 passed (1161)`
- [ ] The diff touches exactly two files

### Measured by hand, and pasted into the PR body

`ADR-0089` forbids a browser in any `verify:` block — *"a QA round, never a gate"* — and jsdom
computes no layout, so the readings below are **acceptance criteria with a named runner and named
numbers**, not gates. A criterion nobody records is a criterion nobody discharged.

**Who runs it:** the implementer, before opening the PR, on the running stack, with
`node scripts/qa/drive.mjs <port> size <w> <h>` and `… eval`. Run it **twice at each shape — once
before your change and once after** — and paste all four readings. The expression is:

```js
(() => {
  const m = document.querySelector('h1[aria-label="Poker Duels"]').getBoundingClientRect();
  const p = [...document.querySelectorAll("button")]
    .find((b) => b.textContent.trim() === "Play duel").getBoundingClientRect();
  const gapX = Math.max(m.left, p.left) - Math.min(m.right, p.right);
  const gapY = Math.max(m.top, p.top) - Math.min(m.bottom, p.bottom);
  return JSON.stringify({ shape: [innerWidth, innerHeight], gapX, gapY, clearance: Math.max(gapX, gapY) });
})()
```

- [ ] **Before**, at 390 × 664 and at 720 × 900, on the front door: `clearance` is **0.000** at
      both. This is the reproduction. The static prediction above says 0.000; a browser reading of
      `clearance ≥ 8` **before** your change means the defect did not reproduce on the running stack
      — **stop, paste the reading, and say so in the PR body rather than changing the layout**
      (`ADR-0103` §3's stop rule). Do not go looking for a different thing to fix.
- [ ] **After**, at 390 × 664 and at 720 × 900: `clearance` **≥ 8.000**, the predicted reading being
      exactly **12.000** at both
- [ ] The bar is 8, not "greater than zero", for `ADR-0106` §2's reason: one CSS pixel is the
      smallest interval into which no readable thing fits, so a criterion that merely separated the
      boxes would call a hairline a fix. Today's reading is a flat 0.000 at both shapes, so no
      sub-pixel reading is in play and `ADR-0106`'s tolerance is neither invoked nor widened.
- [ ] At both shapes, after, the wordmark's rect lies inside the viewport — `left ≥ 0` and
      `right ≤ innerWidth` — so no glyph is clipped by the column that was added
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.

The human's visual verdict on the rendered front door is theirs to give and **may trail the merge**
(`ADR-0024` §3, `ADR-0091` §3); this ticket does not wait at a pane.
