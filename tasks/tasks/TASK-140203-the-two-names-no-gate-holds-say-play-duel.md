---
schema: 2
id: TASK-140203
title: The two names no gate holds say Play duel
type: task
status: backlog
parent: STORY-1402
module: web-client
estimate: XS
tier: haiku
review: light
files_touched: 2
labels: [client, docs]
depends_on: [TASK-140202]
verify:
  - cd web-client && npm ci && FORCE_COLOR=0 NO_COLOR=1 npm run --silent check
  - awk 'index($0, "Create a duel room") { n++ } END { exit (n != 0) }' web-client/src/e2e/whole-duel.test.tsx
  - awk 'index($0, "Create a duel room") { n++ } END { exit (n != 0) }' docs/test-plan.md
  - awk '{ m=$0; while ((i=index(m,"Play duel"))>0) { n++; m=substr(m,i+9) } } END { exit (n != 1) }' web-client/src/e2e/whole-duel.test.tsx
  - awk '{ m=$0; while ((i=index(m,"Play duel"))>0) { n++; m=substr(m,i+9) } } END { exit (n != 4) }' docs/test-plan.md
  - sh -c 'test "$(grep -c SMK-02 docs/test-plan.md)" = "2"'
  - sh -c 'test "$(grep -c SMK-04 docs/test-plan.md)" = "2"'
  - sh -c 'test "$(grep -c 05-01 docs/test-plan.md)" = "2"'
  - sh -c '! grep -rqF "Create a duel room" web-client/src docs/test-plan.md design'
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/e2e/whole-duel.test.tsx > "${TMPDIR:-/tmp}/whole-duel.txt" 2>&1; grep -qF 'Tests  8 passed (8)' "${TMPDIR:-/tmp}/whole-duel.txt"
  - python3 .github/scripts/lint_tickets.py
---

## Goal

The last two places outside `docs/adr/` that name the front door's primary control name it
`Play duel`: one negative assertion in the end-to-end suite, and three rows of the browser test
plan.

## Why this is its own ticket

**Because no gate holds these two to `TASK-140202`'s three, and that was measured, not assumed.**
Renaming `Lobby.tsx` alone on `develop` reddened 17 tests in `App.test.tsx` and `Lobby.test.tsx` and
left `src/e2e/whole-duel.test.tsx` **green** — `Tests  8 passed (8)`. `docs/test-plan.md` is prose
and no gate reads it at all. Folding them into `TASK-140202` would have bought a fourth and fifth
file with nothing, and an `atomic:` claim over them would have been false.

**They still have to move, and the e2e one is the interesting half.** `whole-duel.test.tsx:63`
observes `within(container).queryByText("Create a duel room") !== null` mid-duel and asserts the
result is `false` — *the lobby is not on screen while the duel runs*. A negative assertion is only
worth its words if the string it names is a string the product actually renders. Leave it stale and
it reads `false` because nothing anywhere says `Create a duel room` — a tautology that would hold
for a client rendering the whole front door on top of the table. Renaming it **restores** the
assertion; that is the ticket.

## Files

| File | Action |
| --- | --- |
| `web-client/src/e2e/whole-duel.test.tsx` | modify |
| `docs/test-plan.md` | modify |

## Scope

- **`web-client/src/e2e/whole-duel.test.tsx:63`** — one string literal. `"Create a duel room"`
  becomes `"Play duel"`, inside the `midDuel.createRoom` observation. The expectation two dozen
  lines below — `expect(midDuel).toEqual({ createRoom: false, potStrip: true })` — is **unchanged**;
  `false` is still the right answer and this ticket does not touch it.
- **`docs/test-plan.md`** — three rows, **four** occurrences (`05-01` carries two on one line):
  - `SMK-02` (`:44`) — *`#root` renders and the text contains `Create a duel room`*
  - `SMK-04` (`:46`) — *`A click "Create a duel room"`*
  - `05-01` (`:212`) — *`A wait "Create a duel room"`* and *…does not return to `Create a duel
    room`*

  Each names a drive a browser performs, so each must name the string the shipped client renders
  after `TASK-140202`. Nothing else in any of the three rows moves — not the expected outcome
  column, not the failure column, not the source column.
- **Reformat if Prettier asks.** The e2e change is nine characters shorter; run
  `npx prettier --write src/e2e/whole-duel.test.tsx` from `web-client/`. Measured, the reflow is 3
  changed lines.

## Out of scope

- **`expect(midDuel).toEqual({ createRoom: false, potStrip: true })`.** It does not change. Note for
  the record, and **not** for this ticket to fix: `createRoom` is observed once, mid-duel, and only
  ever as `false` — there is no step where the suite observes it `true`, so the field alone cannot
  distinguish a correct string from a misspelt one. Its paired `potStrip: true` is what carries the
  test's meaning today. A positive observation at a step where the front door *is* up would make the
  string load-bearing in both directions; that is a real improvement, it is **not ticketed**, and it
  is not bought here.
- **Every other row of `docs/test-plan.md`.** Three gates pin each of `SMK-02`, `SMK-04` and
  `05-01` at exactly **two** mentions in the file — its own row, and the later screen-coverage table
  that cites it — so a row deleted, duplicated or renumbered fails this ticket.
- **`docs/adr/`, `tasks/` and `tasks/BOARD.md`.** Merged ADRs and closed round stories quote the old
  string as a record of what was true when they were written (`ADR-0098` §3). The final sweep gate
  is scoped to `web-client/src`, `docs/test-plan.md` and `design/` precisely so that a grep of the
  record does not become this ticket's business.
- **Any client source file.** `Lobby.tsx` is not opened; `TASK-140202` already renamed it.

## Tests

No test is written and none is deleted; one string inside one existing test moves. The suite total
is `1159 passed (1159)` before and after.

| Gate | Proves | Today |
| --- | --- | --- |
| `Create a duel room` is 0 in both files | neither reference was missed | **red** |
| `Play duel` is 1 in the e2e file and 4 in the plan | the plan's double occurrence on `05-01` was seen, and nothing was added | **red** |
| `SMK-02`, `SMK-04` and `05-01` each appear exactly twice in the file — once as their row, once in the later coverage table | the rows were edited in place, not deleted, duplicated or rewritten | green — a regression guard |
| `grep -rF "Create a duel room"` over `web-client/src`, `docs/test-plan.md` and `design/` finds nothing | the story's sweep is complete outside the record | **red** |
| `whole-duel.test.tsx` alone reports `Tests  8 passed (8)` | the edited file still passes whole | green — and it was green with the string stale too, which is exactly why this ticket exists |
| `npm run check` | typecheck, lint, format and the whole suite | green |

## Acceptance criteria

- [ ] `web-client/src/e2e/whole-duel.test.tsx` contains `Play duel` exactly once and
      `Create a duel room` zero times, and its `toEqual({ createRoom: false, potStrip: true })` is
      byte-unchanged
- [ ] `docs/test-plan.md` contains `Play duel` exactly four times and `Create a duel room` zero
      times, and `SMK-02`, `SMK-04` and `05-01` each still appear exactly twice in the file
- [ ] `grep -rF "Create a duel room"` finds nothing under `web-client/src`, in `docs/test-plan.md`,
      or under `design/`
- [ ] `npx vitest run src/e2e/whole-duel.test.tsx` reports `Tests  8 passed (8)`
- [ ] `cd web-client && FORCE_COLOR=0 NO_COLOR=1 npm run --silent check` exits 0, reporting
      `1159 passed (1159)`
- [ ] The diff touches exactly two files
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
