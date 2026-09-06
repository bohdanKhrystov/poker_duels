---
schema: 2
id: TASK-140202
title: The front door's primary control says Play duel
type: task
status: done
parent: STORY-1402
module: web-client
estimate: S
tier: haiku
review: light
files_touched: 3
labels: [client, lobby]
depends_on: [TASK-140201]
verify:
  - cd web-client && npm ci && FORCE_COLOR=0 NO_COLOR=1 npm run --silent check
  - awk 'index($0, "Create a duel room") { n++ } END { exit (n != 0) }' web-client/src/lobby/Lobby.tsx
  - awk 'index($0, "Create a duel room") { n++ } END { exit (n != 0) }' web-client/src/App.test.tsx
  - awk 'index($0, "Create a duel room") { n++ } END { exit (n != 0) }' web-client/src/lobby/Lobby.test.tsx
  - awk '{ m=$0; while ((i=index(m,"Play duel"))>0) { n++; m=substr(m,i+9) } } END { exit (n != 1) }' web-client/src/lobby/Lobby.tsx
  - awk '{ m=$0; while ((i=index(m,"Play duel"))>0) { n++; m=substr(m,i+9) } } END { exit (n != 18) }' web-client/src/App.test.tsx
  - awk '{ m=$0; while ((i=index(m,"Play duel"))>0) { n++; m=substr(m,i+9) } } END { exit (n != 10) }' web-client/src/lobby/Lobby.test.tsx
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/lobby/Lobby.test.tsx src/App.test.tsx > "${TMPDIR:-/tmp}/rename-after.txt" 2>&1; grep -qF 'Tests  138 passed (138)' "${TMPDIR:-/tmp}/rename-after.txt"
  - cd web-client && cp src/lobby/Lobby.tsx "${TMPDIR:-/tmp}/Lobby.renamed.tsx" && perl -0pi -e 's/        Play duel\n/        Create a duel room\n/' src/lobby/Lobby.tsx && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/lobby/Lobby.test.tsx src/App.test.tsx > "${TMPDIR:-/tmp}/rename-before.txt" 2>&1; cp "${TMPDIR:-/tmp}/Lobby.renamed.tsx" src/lobby/Lobby.tsx; cmp -s "${TMPDIR:-/tmp}/Lobby.renamed.tsx" src/lobby/Lobby.tsx && grep -qF 'Tests  17 failed | 121 passed (138)' "${TMPDIR:-/tmp}/rename-before.txt"
  - awk 'index($0, "className=") && index($0, "max-w-[380px]") { n++ } END { exit (n != 6) }' web-client/src/lobby/Lobby.tsx
  - python3 .github/scripts/lint_tickets.py
---

## Goal

The front door's filled control reads `Play duel`, and the two merged test files that drive it by
name drive it by the new name. Nothing else about the front door changes — not its layout, not its
handler, not one other string.

## Why these three files and not four

**Measured on `develop` at `77a09060`, by renaming `Lobby.tsx` alone and running the suite.**
`cd web-client && npx vitest run` went from `1159 passed (1159)` to `17 failed | 1142 passed
(1159)`, and the 17 failures fell in exactly two files: `Lobby.test.tsx` (8) and `App.test.tsx` (9).
So `npm run check` — a merged gate on every pull request — **refuses every proper subset of these
three files**, and they are one ticket.

They are also only three, which is the ordinary cap, so **this ticket declares no `atomic:`**. The
fourth and fifth places the old string lives — `web-client/src/e2e/whole-duel.test.tsx` and
`docs/test-plan.md` — are **not** held by that gate: the same probe left `whole-duel.test.tsx`
green, because its one reference is a *negative* observation. `TASK-140203` owns those two, and the
reason it is a separate ticket is that no gate binds them to this one.

## Files

| File | Action |
| --- | --- |
| `web-client/src/lobby/Lobby.tsx` | modify |
| `web-client/src/App.test.tsx` | modify |
| `web-client/src/lobby/Lobby.test.tsx` | modify |

The merged card is the source of the word: `design/screens/create-duel.html:86`, landed by
`TASK-140201`. You do not need to open it — the word is `Play duel`, and it is the human's own,
given on 2026-09-06. Do not improve on it.

## Scope

- **`web-client/src/lobby/Lobby.tsx:408`** — the button's text node. `Create a duel room` becomes
  `Play duel`. The `<button>`'s `type`, `className` and `onClick` are untouched; it still sends
  `{ type: "CreateRoom" }`.
- **`web-client/src/App.test.tsx`** — all **18** occurrences of the literal `"Create a duel room"`
  become `"Play duel"`. Seventeen are the `name` of a `getByRole` / `findByRole` / `queryByRole`
  button query; one, at `:1239`, is inside a text filter (`text !== "Create a duel room" && …`).
  All are a plain string swap. **No assertion changes shape, none is added, none is removed, and
  none is weakened** — a `queryByRole(…)).toBeNull()` stays a `toBeNull()`.
- **`web-client/src/lobby/Lobby.test.tsx`** — the same swap, all **10** occurrences.
- **Reformat afterwards.** `Play duel` is nine characters shorter than `Create a duel room`, so
  Prettier reflows the call sites that were wrapped for width, and `npm run check`'s `format:check`
  step **fails until you do**. This was measured: without it, `prettier --check` reports
  `Code style issues found in 3 files`. Run
  `npx prettier --write src/App.test.tsx src/lobby/Lobby.test.tsx` from `web-client/`. The reflow is
  expected and is part of the diff — the measured shape is `App.test.tsx` 54 changed lines,
  `Lobby.test.tsx` 32, `Lobby.tsx` 2. **Do not hand-rewrap, and do not revert the rename to make the
  formatter quiet.**

## Out of scope

- **The front door's layout.** The `<section className="p-6">` at `:388` keeps exactly the class it
  has. `TASK-140204` gives it the column, and a `verify:` gate here pins `max-w-[380px]` on
  `className=` lines at **6** — today's number — so adding the column inside this ticket fails it.
- **`web-client/src/e2e/whole-duel.test.tsx` and `docs/test-plan.md`** — `TASK-140203`. They still
  read `Create a duel room` after this merges, and that is deliberate: no gate holds them, so
  putting them here would be a fourth and fifth file bought with nothing.
- **`Join the duel`, the `Room code` label, the refusal strings, the profile strip and the three
  doors.** Not one of them changes. `App.test.tsx:1239` names `Join the duel` on the same line as
  the string you are replacing — replace only the one.
- **`docs/adr/`.** Seven merged ADRs name *Create a duel room* as a record of what was true when
  they were written; an ADR is superseded, not edited (`ADR-0098` §3). A grep of `docs/adr/` for
  this string is a widened scope.
- **Adding or deleting any test.** The suite is `1159 passed (1159)` before this ticket and
  `1159 passed (1159)` after it.
- **`design/`** — `TASK-140201`, already merged when this starts.

## Tests

No test is written and no test is deleted. The two merged test files are **edited**, and they are in
the Files table and in `files_touched` for that reason: 28 of their assertions observe the string
this ticket changes, so they are its blast radius, not collateral.

The gate that decides the ticket is the **mutation** in `verify:` — it renames the source back and
requires the two files to go red *in a measured way*:

| Gate | Proves | Reading |
| --- | --- | --- |
| `npm run check` | typecheck, lint, **format** and the whole suite | green after — `1159 passed (1159)` |
| the two files together, unmutated | the rename left nothing behind | `Tests  138 passed (138)` |
| the two files with `Lobby.tsx` renamed **back** | the tests genuinely depend on the string, and the count is the one measured on `develop` | `Tests  17 failed \| 121 passed (138)` |
| `Create a duel room` is 0 in each of the three files | no occurrence was missed | **red** today |
| `Play duel` is 1, 18 and 10 | none was added and none was dropped — the counts survive Prettier's reflow, which was checked | **red** today |
| `max-w-[380px]` on `className=` lines is 6 | the layout was not touched while the string was | green — a regression guard |

The mutation gate restores the file and `cmp`s it before reporting, so a red run cannot leave the
tree edited.

## Acceptance criteria

- [ ] `web-client/src/lobby/Lobby.tsx` contains `Play duel` exactly once and `Create a duel room`
      zero times
- [ ] `web-client/src/App.test.tsx` contains `Play duel` exactly 18 times and `Create a duel room`
      zero times
- [ ] `web-client/src/lobby/Lobby.test.tsx` contains `Play duel` exactly 10 times and
      `Create a duel room` zero times
- [ ] `npx vitest run src/lobby/Lobby.test.tsx src/App.test.tsx` reports `Tests  138 passed (138)`
- [ ] With `Lobby.tsx`'s label renamed back, the same command reports
      `Tests  17 failed | 121 passed (138)`, and the file is restored afterwards
- [ ] `className=` lines carrying `max-w-[380px]` in `Lobby.tsx` still number **6**
- [ ] `cd web-client && FORCE_COLOR=0 NO_COLOR=1 npm run --silent check` exits 0, reporting
      `1159 passed (1159)`
- [ ] The diff touches exactly three files, all under `web-client/src/`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
