---
schema: 2
id: TASK-141403
title: The bar is pinned at a frame where the seat has already committed
type: task
status: backlog
parent: STORY-1414
module: web-client
estimate: S
tier: haiku
review: standard
files_touched: 2
labels: [client, table, test]
depends_on: [TASK-141402]
verify:
  - cd web-client && npm ci && FORCE_COLOR=0 NO_COLOR=1 npm run --silent check
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/table/ActionBar.test.tsx > "${TMPDIR:-/tmp}/bar-after.txt" 2>&1; grep -qF 'Tests  36 passed (36)' "${TMPDIR:-/tmp}/bar-after.txt"
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/table/bar-no-derivation.test.tsx > "${TMPDIR:-/tmp}/guard-after.txt" 2>&1; grep -qF 'Tests  4 passed (4)' "${TMPDIR:-/tmp}/guard-after.txt"
  - cd web-client && cp src/table/ActionBar.tsx "${TMPDIR:-/tmp}/ActionBar.fixed.tsx" && perl -0pi -e 's/dialled \?\? 0,\s*props\.committedThisStreet/dialled ?? 0, 0/s' src/table/ActionBar.tsx && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/table/ActionBar.test.tsx > "${TMPDIR:-/tmp}/bar-before.txt" 2>&1; FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/table/bar-no-derivation.test.tsx > "${TMPDIR:-/tmp}/guard-before.txt" 2>&1; cp "${TMPDIR:-/tmp}/ActionBar.fixed.tsx" src/table/ActionBar.tsx; cmp -s "${TMPDIR:-/tmp}/ActionBar.fixed.tsx" src/table/ActionBar.tsx && grep -qF "FAIL  src/table/ActionBar.test.tsx > the action bar > prices the call button from the acting seat's own commitment" "${TMPDIR:-/tmp}/bar-before.txt" && grep -qF 'Tests  1 failed | 35 passed (36)' "${TMPDIR:-/tmp}/bar-before.txt" && grep -qF 'AssertionError: expected [ 600, 1200, 13400, 1200 ] to include 400' "${TMPDIR:-/tmp}/guard-before.txt" && grep -qF 'Tests  1 failed | 3 passed (4)' "${TMPDIR:-/tmp}/guard-before.txt"
  - python3 .github/scripts/lint_tickets.py
---

## Goal

The rendered action bar is pinned at two frames where the acting seat has **already committed** —
the only frames where the price and the total are different numbers — and the never-derives guard
admits the price while refusing `callTo`. A bar that dropped the commitment on its way to
`actionText` now fails.

## Files

| File | Action |
| --- | --- |
| `web-client/src/table/ActionBar.test.tsx` | modify |
| `web-client/src/table/bar-no-derivation.test.tsx` | modify |

Read [`STORY-1414`](../stories/STORY-1414-call-says-what-it-costs.md)'s *Design notes* and
[`ADR-0122`](../../docs/adr/ADR-0122-call-names-the-price-and-raise-to-names-the-total.md) §5.
`web-client/src/table/turn-fixture.ts` (33 lines) holds `aTurn` and `aLegalActions`. **No production
file is opened and none is changed** — this ticket adds two tests and nothing else.

## Scope

- **Why this is its own ticket, and not test lines inside `TASK-141402`.** With the price computed
  inside `actionText`, a caller that passed a literal `0` would satisfy every assertion in
  `action-text.test.ts`. The wiring needs an assertion of its own, made through the real component,
  and `TASK-141402`'s irreducible three files (the function, its caller, and the caller's
  type-forced unit test) left no room for it. This ticket is that assertion.
- **Two frames, two different non-zero commitments, neither of them zero.** A single frame passes a
  bar that hard-codes one number; a zero commitment passes both the old behaviour and the new. Use
  `callTo` 600 against committed 200 and `callTo` 925 against committed 75.
- **Assert the number that must *not* be there.** Each frame asserts the full button row **and**
  that the total (600, 925) appears nowhere in the bar's text. The positive assertion alone would
  pass a bar that printed both.
- **Add exactly two tests, one per file.** No merged test in either file is renamed, reordered,
  deleted or weakened. All three of `ActionBar.test.tsx`'s merged `Call 400` assertions (lines 57,
  479, 658) render at commitment 0, where `callTo - 0 === callTo`, so they are already correct and
  must stay exactly as they are.
- **`render` binds its queries to the document, not to one container.** Two frames in one test means
  `unmount()` between them, or the second `getByRole("group", { name: "actions" })` matches two
  elements and throws.
- Run `npm run format` before `npm run check`.

## Out of scope

- **Every production file.** `ActionBar.tsx` is *mutated* by the fourth `verify:` command and
  restored by it — that is an experiment, not a change, and `git status` must be clean of it when
  the command returns.
- **The `off` state, the sizing row's own arithmetic and the typed field.** `ADR-0127` settled the
  first; `ADR-0101` §§1–2 and the merged tests at `ActionBar.test.tsx:244` and `:370` already pin
  the second and third, including at `committedThisStreet: 200` and `: 75`.
- **`no-derivation.test.tsx`** — that is the *table's* guard and it renders no `ActionBar`. Its
  admitted quantity is `ADR-0107` §5's pot sum and nothing here touches it.
- **The last-act mark.** `TASK-141404`. If `SeatPlate` appears in your diff, the ticket has been
  widened.

## Tests

`ActionBar.test.tsx` → `describe("the action bar")`, added immediately before
`renders no button for an action the server withheld`:

| Test | Proves |
| --- | --- |
| `prices the call button from the acting seat's own commitment` | Through the `bar(...)` helper: with `aLegalActions({ allowed: ["FOLD", "CALL", "RAISE", "ALL_IN"], callTo: 600 })`, `potIncludingStreet: 1400` and `committedThisStreet: 200`, the actions group's button texts are exactly `["Fold", "Call 400", "Raise to 1,200", "All in 13,400"]` and `container.textContent` does not match `/600/`. Then `unmount()`, and with `callTo: 925` and `committedThisStreet: 75` they are exactly `["Fold", "Call 850", "Raise to 1,200", "All in 13,400"]` and the text does not match `/925/`. Two commitments that differ from each other and from zero, so a constant, a dropped prop and a hard-coded seat all fail |

`bar-no-derivation.test.tsx` → `describe("the bar offers and derives nothing")`, added immediately
before `offers no control the turn did not allow`:

| Test | Proves |
| --- | --- |
| `admits the acting seat's call price and no other figure the turn does not carry` | Renders `ActionBar` directly with `aTurn({ legalActions: aLegalActions({ callTo: 600 }) })`, `potIncludingStreet={1400}` and `committedThisStreet={200}`. With `price = turn.legalActions.callTo - 200`, `numbersOnScreen(container)` **contains** `price`, does **not** contain `turn.legalActions.callTo`, and every number in it is one of `price`, `minBetTo`, `minRaiseTo`, `allInTo`. This is `ADR-0122` §5's *one further named quantity* under test: the guard's three merged cases all render at commitment 0, where the price **is** `callTo`, so none of them can see this quantity at all |

**Both tests must be red against a bar that drops the commitment, and the `verify:` block proves it
rather than takes your word for it.** The fourth command copies `ActionBar.tsx` aside, rewrites the
`actionText` call's fourth argument to `0`, runs each of the two files separately, restores the file
byte-for-byte (`cmp -s`), and then requires four captured strings: the `FAIL` line naming each test
by its full path, and each file's own `Tests  1 failed | …` count. Per-file counts, deliberately —
a whole-run total is wrong the moment a third file joins the command. All four strings were
**measured** by running that probe against these exact fixtures.

The second and third commands pin the absolute counts at **36** (35 merged plus one) and **4**
(3 merged plus one).

## Acceptance criteria

- [ ] `prices the call button from the acting seat's own commitment` passes, asserting the full
      four-button row at 600/200 and at 925/75, and the absence of 600 and 925 respectively
- [ ] `admits the acting seat's call price and no other figure the turn does not carry` passes,
      asserting that 400 is on screen, 600 is not, and nothing outside the four allowed figures is
- [ ] `ActionBar.test.tsx` holds exactly 36 tests and `bar-no-derivation.test.tsx` exactly 4, all
      passing
- [ ] With `ActionBar.tsx`'s fourth argument to `actionText` replaced by `0`, exactly one test in
      each file fails — asserted by the fourth `verify:` command, which restores the file afterwards
- [ ] No merged test in either file is renamed, reordered, deleted or weakened, and lines 57, 479
      and 658 of `ActionBar.test.tsx` still read `Call 400`
- [ ] The diff touches exactly two files, both test files
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
