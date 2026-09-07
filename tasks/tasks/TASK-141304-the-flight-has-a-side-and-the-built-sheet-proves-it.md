---
schema: 2
id: TASK-141304
title: The flight has a side, and the built stylesheet proves it
type: task
status: backlog
parent: STORY-1413
module: web-client
estimate: S
tier: haiku
review: standard
files_touched: 3
labels: [client, table, design]
depends_on: [TASK-141303]
verify:
  - cd web-client && npm ci && FORCE_COLOR=0 NO_COLOR=1 npm run --silent check
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/table/ChipPile.test.tsx > "${TMPDIR:-/tmp}/cp-after.txt" 2>&1; grep -qF 'Tests  5 passed (5)' "${TMPDIR:-/tmp}/cp-after.txt"
  - cd web-client && npm run build > /dev/null && cat dist/assets/*.css | grep -qF '.chip-flight-down{animation-name:pd-chip-flight-down;animation-duration:var(--pd-motion-chip-flight);animation-timing-function:var(--pd-motion-chip-ease);animation-iteration-count:1;animation-fill-mode:both}'
  - cd web-client && cat dist/assets/*.css | grep -qF '.chip-flight-up{animation-name:pd-chip-flight-up;animation-duration:var(--pd-motion-chip-flight);animation-timing-function:var(--pd-motion-chip-ease);animation-iteration-count:1;animation-fill-mode:both}'
  - cd web-client && cat dist/assets/*.css | grep -qF '@keyframes pd-chip-flight-down{0%{transform:translateY(calc(var(--pd-motion-chip-travel) * -1))}to{transform:none}}'
  - cd web-client && cat dist/assets/*.css | grep -qF '@keyframes pd-chip-flight-up{0%{transform:translateY(var(--pd-motion-chip-travel))}to{transform:none}}'
  - cd web-client && cat dist/assets/*.css | grep -qF '@media (prefers-reduced-motion:reduce){*,:before,:after{transition:none!important;animation:none!important}}'
  - cd web-client && cp src/styles/app.css "${TMPDIR:-/tmp}/app.m1.css" && perl -0pi -e 's/\@utility chip-flight-up \{/\@utility chip-flight-upside \{/' src/styles/app.css && npm run build > /dev/null 2>&1; cat dist/assets/*.css | grep -qF '.chip-flight-up{animation-name:pd-chip-flight-up;'; r=$?; FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/table/ChipPile.test.tsx > "${TMPDIR:-/tmp}/cp-m1.txt" 2>&1; cp "${TMPDIR:-/tmp}/app.m1.css" src/styles/app.css; cmp -s "${TMPDIR:-/tmp}/app.m1.css" src/styles/app.css && [ "$r" -ne 0 ] && grep -qF 'Tests  5 passed (5)' "${TMPDIR:-/tmp}/cp-m1.txt"
  - cd web-client && cp src/styles/tokens.css "${TMPDIR:-/tmp}/tokens.m2.css" && perl -0pi -e 's/\@media \(prefers-reduced-motion: reduce\)/\@media (prefers-reduced-motion: no-preference)/' src/styles/tokens.css && npm run build > /dev/null 2>&1; cat dist/assets/*.css | grep -qF '@media (prefers-reduced-motion:reduce){*,:before,:after{transition:none!important;animation:none!important}}'; r=$?; FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/table > "${TMPDIR:-/tmp}/table-m2.txt" 2>&1; cp "${TMPDIR:-/tmp}/tokens.m2.css" src/styles/tokens.css; cmp -s "${TMPDIR:-/tmp}/tokens.m2.css" src/styles/tokens.css && [ "$r" -ne 0 ] && grep -qF 'Tests  253 passed (253)' "${TMPDIR:-/tmp}/table-m2.txt"
  - cd web-client && cp src/table/ChipPile.tsx "${TMPDIR:-/tmp}/cp.m3.tsx" && perl -0pi -e 's/  below: "chip-flight-up",/  below: "chip-flight-down",/' src/table/ChipPile.tsx && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/table/ChipPile.test.tsx > "${TMPDIR:-/tmp}/cp-m3.txt" 2>&1; cp "${TMPDIR:-/tmp}/cp.m3.tsx" src/table/ChipPile.tsx; cmp -s "${TMPDIR:-/tmp}/cp.m3.tsx" src/table/ChipPile.tsx && grep -qF 'Tests  1 failed | 4 passed (5)' "${TMPDIR:-/tmp}/cp-m3.txt" && grep -qF 'arrives from below when the chips come from below' "${TMPDIR:-/tmp}/cp-m3.txt"
  - sh -c '! grep -rq "chip-flight\"" web-client/src'
  - sh -c '! grep -Eq "matchMedia|setTimeout|requestAnimationFrame|useEffect|useState" web-client/src/table/ChipPile.tsx'
  - awk 'index($0, "chip-flight-down") { n++ } END { exit (n != 1) }' web-client/src/table/ChipPile.tsx
  - awk 'index($0, "chip-flight-up") { n++ } END { exit (n != 1) }' web-client/src/table/ChipPile.tsx
  - awk 'index($0, "--pd-motion-chip") { n++ } END { exit (n != 4) }' design/tokens/tokens.css
  - awk 'index($0, "--pd-motion") { n++ } END { exit (n != 8) }' design/tokens/tokens.css
  - cmp -s design/tokens/tokens.css web-client/src/styles/tokens.css
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/e2e > "${TMPDIR:-/tmp}/e2e-after.txt" 2>&1; grep -qF 'Test Files  7 passed (7)' "${TMPDIR:-/tmp}/e2e-after.txt" && grep -qF 'Tests  55 passed (55)' "${TMPDIR:-/tmp}/e2e-after.txt"
  - python3 .github/scripts/lint_tickets.py
---

## Goal

The chip flight gains a **side**. `app.css` carries two keyframes and two utilities where it carried
one of each, `ChipPile` takes `from?: "above" | "below"` and maps it through a literal side table,
and the gates that say so read the **built stylesheet** rather than a class list.

## Why this is a ticket

`TASK-141301`–`TASK-141303` merged the vocabulary into the three cards; this transcribes it. It is
the seam where the client gains a declaration the next ticket reads, and it is three files because
the utility, the component and the component's tests are each one — `tsc` does not force any of
them, the tests do: the merged assertion at `ChipPile.test.tsx:29` names the old class.

**A class-name assertion cannot see whether the rule exists.** That is the defect class this epic has
already been bitten by twice — `min-w-0` compiling to nothing (`TASK-140502`) and the class-name test
that could not tell (`TASK-140503`) — and it is why this ticket's centre of gravity is a `npm run build`
followed by `grep -F` over `dist/assets/*.css`. Measured: with `@utility chip-flight-up` renamed
away, the built rule vanishes and **all five** `ChipPile` tests still pass.

**One trap, found by probing, so nobody rediscovers it.** Spelling the class dynamically —
`` className={`chip-pile chip-flight-${side}`} `` — does **not** drop the rule, because Tailwind's
content scan reads `ChipPile.test.tsx` as well and the test's own literals keep it alive. That is
why the side table holds **whole class names** and why the mutation that proves the gate is a rename
of the utility rather than a rewrite of the component.

## Files

| File | Action |
| --- | --- |
| `web-client/src/styles/app.css` | modify |
| `web-client/src/table/ChipPile.tsx` | modify |
| `web-client/src/table/ChipPile.test.tsx` | modify |

**Nothing else is opened.** `SeatPlate.tsx` is `TASK-141305`'s. `PotStrip.tsx` and `DuelTable.tsx`
are **not** opened: `from` is optional and its default is the direction they already had, so their
call sites and their tests stay valid and untouched. `web-client/src/styles/tokens.css` and
`design/` are not edited — the two mutations that write to them restore and `cmp` before reporting
(*a mutation is an experiment, not a change*).

## Scope

- **`web-client/src/styles/app.css`.** In the chip block, `@keyframes pd-chip-flight` becomes two
  keyframes and `@utility chip-flight` becomes two utilities. The `@utility chip-pile`,
  `@utility chip-disc` and `.chip-disc + .chip-disc` rules are **byte-unchanged**, and so is every
  other block in the file:

  ```css
  @keyframes pd-chip-flight-down {
    from {
      transform: translateY(calc(var(--pd-motion-chip-travel) * -1));
    }
    to {
      transform: none;
    }
  }

  @keyframes pd-chip-flight-up {
    from {
      transform: translateY(var(--pd-motion-chip-travel));
    }
    to {
      transform: none;
    }
  }
  ```

  and, after the disc rules, the two utilities — each carrying the same five declarations
  `chip-flight` carried, differing only in `animation-name`:

  ```css
  @utility chip-flight-down {
    animation-name: pd-chip-flight-down;
    animation-duration: var(--pd-motion-chip-flight);
    animation-timing-function: var(--pd-motion-chip-ease);
    animation-iteration-count: 1;
    animation-fill-mode: both;
  }
  ```

  The block comment above them is rewritten to say what the pair is for, in the component card's own
  words: they differ only in the side the chips come from, both land on the frame a still pile
  already has, and the sheet's one `prefers-reduced-motion` block stills both.

- **`web-client/src/table/ChipPile.tsx`.** A `FLIGHT` side table of **whole class names**, an
  optional `from` prop defaulting to `"above"`, and the existing KDoc extended rather than replaced.
  The whole file, which is what `npm run format` produces and what `eslint --max-warnings 0` accepts:

  ```tsx
  const FLIGHT: Record<"above" | "below", string> = {
    above: "chip-flight-down",
    below: "chip-flight-up",
  };

  export function ChipPile(props: { from?: "above" | "below" }): ReactElement {
    return (
      <span
        aria-hidden="true"
        className={`chip-pile ${FLIGHT[props.from ?? "above"]}`}
      >
  ```

  `aria-hidden="true"`, the three `chip-disc` spans and the empty text content do not change: the
  pile goes on stating nothing, which is what lets `ADR-0115` §1 call the whole flight garnish.

- **`web-client/src/table/ChipPile.test.tsx`.** Line 29's `toContain("chip-flight")` becomes
  `toContain("chip-flight-down")` — the one merged assertion this change invalidates, and it moves
  because the class it names no longer exists, not because it was weakened. Two tests are added
  after it, each asserting the spelling it wants **and the absence of the other**:
  `arrives from above when the chips come from above` (rendered `from="above"`) and
  `arrives from below when the chips come from below` (rendered `from="below"`). The file goes
  **3 → 5**.

## Out of scope

- **Every call site.** No component passes `from` in this ticket; `SeatPlate` does in
  `TASK-141305`, and `PotStrip` and `DuelTable` never will (`STORY-1413`'s *Out of scope*: the pot
  has no side, and the bet line already takes its chips from above).
- **Minting a token.** All three `--pd-motion-chip-*` values are reused unchanged; a gate pins the
  sheet's motion lines at **8** and its chip motion lines at **4** so a fourth cannot slip in.
- **`web-client/src/styles/tokens.css` and `design/`.** Not edited. Both are byte-identical to their
  merged state when this ticket reports, and `cmp -s` between the sheet and its vendored copy is in
  `verify:`.
- **Any clock, effect or media query in JavaScript.** `ChipPile` gains no `useEffect`, `useState`,
  `setTimeout`, `requestAnimationFrame` or `matchMedia`, and a gate says so. The reduced-motion
  answer is the sheet's one block, per `ADR-0115` §4.
- **Anything under `web-client/src/e2e/`.** Not edited, not re-recorded; the count gate is in
  `verify:`.

## Tests

`ChipPile.test.tsx` — **3 → 5**.

| Test | Proves |
| --- | --- |
| `carries the flight class beside the pile class` *(moved)* | the default pile carries `chip-pile` and `chip-flight-down` |
| `arrives from above when the chips come from above` | `from="above"` gives `chip-flight-down` and **not** `chip-flight-up` |
| `arrives from below when the chips come from below` | `from="below"` gives `chip-flight-up` and **not** `chip-flight-down` |

Nothing else in the file changes, and no assertion is weakened: the two existing tests
(`draws three discs inside one pile`, `says nothing to anybody`) are untouched.

**Written and run at planning time**, against the change applied and then reverted:

| Gate | Proves | Reading |
| --- | --- | --- |
| `npx vitest run src/table/ChipPile.test.tsx` | the file is whole and both tests are new | `Tests  5 passed (5)` |
| build, then five `grep -F` over `dist/assets/*.css` | the two utilities and the two keyframes **generate**, and the block that stills them is in the same sheet | all five present |
| **Mutation 1** — `@utility chip-flight-up` → `@utility chip-flight-upside`, rebuild | the built-CSS gate bites, and a class-name test cannot | the `.chip-flight-up{…}` grep exits **non-zero** while `ChipPile.test.tsx` still reads `Tests  5 passed (5)` |
| **Mutation 2** — `@media (prefers-reduced-motion: reduce)` → `no-preference` in the vendored sheet, rebuild | the reduce gate reads generated output rather than source | the reduce grep exits **non-zero** while `src/table` reads `Tests  253 passed (253)`. *(`src/styles/tokens.test.ts`'s byte-identity check also reddens — expected, and the reason the mutation restores and `cmp`s before reporting.)* |
| **Mutation 3** — `below: "chip-flight-up"` → `"chip-flight-down"` | the side table is read, not decoration — **two inputs that disagree** | `Tests  1 failed \| 4 passed (5)`, naming `arrives from below when the chips come from below` |
| `! grep -rq 'chip-flight"' web-client/src` | the undirected spelling is gone from the tree | red today at **2** sites (`ChipPile.tsx:11`, `ChipPile.test.tsx:29`), green after |
| `chip-flight-down` and `chip-flight-up` each **1** line in `ChipPile.tsx` | each class is named once, in the side table, and nowhere assembled | measured |
| `npx vitest run src/e2e` | the recorded-frame suites are untouched by a change with no clock in it | `7 passed (7)`, `55 passed (55)` |
| `npm run check` | typecheck, lint, format and the whole suite | green — `124 files, 1168 tests`, from `1166` |

Every mutation restores its file and `cmp`s it before the gate reports, so a red run cannot leave the
tree edited.

## Acceptance criteria

- [ ] `ChipPile.test.tsx` reports `Tests  5 passed (5)`
- [ ] After `npm run build`, `dist/assets/*.css` contains, as exact strings, `.chip-flight-down{…}`,
      `.chip-flight-up{…}`, `@keyframes pd-chip-flight-down{…}`, `@keyframes pd-chip-flight-up{…}`
      and `@media (prefers-reduced-motion:reduce){*,:before,:after{transition:none!important;animation:none!important}}`
- [ ] Mutation 1 makes the `.chip-flight-up{…}` gate fail while `ChipPile.test.tsx` stays
      `Tests  5 passed (5)`, and `app.css` is restored byte-for-byte
- [ ] Mutation 2 makes the reduce gate fail while `src/table` stays `Tests  253 passed (253)`, and
      `tokens.css` is restored byte-for-byte
- [ ] Mutation 3 gives `Tests  1 failed | 4 passed (5)` naming
      `arrives from below when the chips come from below`, and `ChipPile.tsx` is restored
- [ ] `! grep -rq 'chip-flight"' web-client/src` exits 0
- [ ] `ChipPile.tsx` names `chip-flight-down` on exactly **1** line and `chip-flight-up` on exactly
      **1**, and contains no `matchMedia`, `setTimeout`, `requestAnimationFrame`, `useEffect` or
      `useState`
- [ ] `design/tokens/tokens.css` has **8** `--pd-motion` lines and **4** `--pd-motion-chip` lines,
      and is byte-identical to `web-client/src/styles/tokens.css`
- [ ] `npx vitest run src/e2e` reports `Test Files  7 passed (7)` and `Tests  55 passed (55)`
- [ ] `cd web-client && FORCE_COLOR=0 NO_COLOR=1 npm run --silent check` exits 0, reporting
      `1168 passed (1168)`
- [ ] The diff touches exactly three files
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
