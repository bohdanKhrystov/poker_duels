---
schema: 2
id: TASK-130505
title: Both ends of the interval, the repeated refusal, and the button that never prints a different amount
type: task
status: ready
parent: STORY-1305
module: web-client
estimate: S
tier: sonnet
review: standard
files_touched: 1
labels: [client, table, action-bar]
depends_on: [TASK-130504]
verify:
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npm run --silent check
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/table/ActionBar.test.tsx 2>&1 | grep -qE '^ *Tests +36 passed \(36\)$'
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/table/bar-no-derivation.test.tsx 2>&1 | grep -qE '^ *Tests +3 passed \(3\)$'
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/table/null-view.test.tsx 2>&1 | grep -qE '^ *Tests +6 passed \(6\)$'
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/e2e/whole-duel.test.tsx 2>&1 | grep -qE '^ *Tests +8 passed \(8\)$'
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/lobby/Lobby.test.tsx 2>&1 | grep -qE '^ *Tests +80 passed \(80\)$'
  - awk 'index($0, "20,000 is over the maximum of 13,400.") { n++ } END { exit (n < 1) }' web-client/src/table/ActionBar.test.tsx
  - awk 'index($0, "0 is under the minimum of 1,200.") { n++ } END { exit (n < 1) }' web-client/src/table/ActionBar.test.tsx
  - awk 'index($0, "That is not an amount.") { n++ } END { exit (n < 2) }' web-client/src/table/ActionBar.test.tsx
  - awk 'index($0, "The server says it is seat 1") { n++ } END { exit (n < 2) }' web-client/src/table/ActionBar.test.tsx
  - awk 'index($0, "aria-label=\"the total\"") { n++ } END { exit (n != 1) }' web-client/src/table/ActionBar.tsx
  - awk 'index($0, "Math.floor") { n++ } END { exit (n != 2) }' web-client/src/table/ActionBar.tsx
  - sh -c 'grep -q "readTypedAmount" web-client/src/table/ActionBar.tsx && ! grep -qE "Math\.(min|max)|clamp" web-client/src/table/ActionBar.tsx'
  - sh -c 'grep -q "reachTheAmount" web-client/src/e2e/drive-duel.tsx && ! grep -qE "fireEvent\.change|getByLabelText|\.value =" web-client/src/e2e/drive-duel.tsx'
  - python3 .github/scripts/lint_tickets.py
---

## Goal

The typed field's remaining `ADR-0111` clauses become named tests: both ends of the server's stated
interval send, the over-stack refusal quotes the maximum, a plain `0` is a number while an empty
field and a negative are not, a refused press is safely repeatable, the action button never prints
an amount the player did not propose, and the server's own word still reaches the bar.

## Why this is a test-only ticket

`TASK-130504` lands the behaviour complete and safe — nothing illegal is ever sent, and every
refused press says why — but its four tests are the ones a coder needs to build the thing. These six
are the ones a **reviewer** needs to believe it, and together they would have pushed that ticket
past `S`. They are split for size and for nothing else: the story is not done without them.

**The first of them is the one `ADR-0111` asked for by name.** §Consequences: *"if the bar's reading
is ever stricter than the server's rule, a legal amount is wrongly refused and no server message
will ever say so, because nothing is sent; nothing in the type system polices this."* The interval's
two endpoints, driven through the real bar and asserted on the real frame, are what polices it.

## What is already true, measured on `develop` 2026-09-03 plus `TASK-130504`

- `ActionBar.test.tsx` is **26** on `develop` and **30** after `TASK-130504`; this ticket takes it
  to **36**.
- On `develop` the file already contains `The server says it is seat 1's turn.` **once** (in
  `states a rejection in the server's own numbers`) and `is under the minimum of` **three** times;
  `is over the maximum of` and `That is not an amount.` are both at **0**, so the two new literal
  gates below cannot pass on the merged file.
- `aLegalActions()` gives `callTo 400`, `minBetTo 175`, `minRaiseTo 1200`, `allInTo 13400` — four
  mutually independent numbers, so a figure the bar worked out for itself lands outside the set.
- The `bar()` helper renders with `potIncludingStreet 2850` and `committedThisStreet 0`; `pot` on
  that frame computes `3,650`.
- `Live` is keyed on `${handNumber}:${actionSequence}:${rejectionCount}`, so a rejection remounts
  the bar and clears any standing local sentence by construction.

## Files

| File | Action |
| --- | --- |
| `web-client/src/table/ActionBar.test.tsx` | modify |
| `web-client/src/table/ActionBar.tsx` | read |
| `web-client/src/table/typed-amount.ts` | read |
| `web-client/src/table/turn-fixture.ts` | read |
| `docs/adr/ADR-0111-an-illegal-typed-amount-is-refused-in-the-servers-own-numbers.md` | read |

## Scope

- **Six tests added to `describe("the action bar")`**, exactly as named below, using the file's
  existing `bar()` helper and reaching the field through its accessible name, `the total`.
- **Every expected sentence is written as a literal string.** Never assembled from
  `rejectionText`, `formatChips` or the module's own constants: the wording is what is under test.
- **No production file is opened.** `ActionBar.tsx` is read-only here, and three gates pin that its
  field, its two `Math.floor`s and its refusal of a clamp are all where `TASK-130504` left them.
- **No merged assertion moves and none is weakened.** The thirty tests already in the file assert
  the buttons, the presets, the sent-lock, the remount, and the server's own notices; this ticket
  only adds.

## Out of scope

- **Changing behaviour.** If one of these six goes red, that is a defect in `TASK-130504` and a
  repair ticket against it — not a quiet edit to `ActionBar.tsx` from inside this diff.
- **`null-view.test.tsx`.** `TASK-130506`; the gate pins it at 6 here.
- **`web-client/src/e2e/drive-duel.tsx` and every scripted-duel suite** (`ADR-0100` §5,
  `ADR-0111` §6). Not opened; gated.
- **A stepper, a step, `DEC-102`, and every clamping courtesy** (`ADR-0111` §§1, 3, 5).

## Tests

`ActionBar.test.tsx`, `describe("the action bar")` — six added to the thirty, so the file reports
**36**.

| Test | Proves |
| --- | --- |
| `sends both ends of the interval and one total inside it` | typing exactly `1,200` (this turn's `minRaiseTo`) sends `to: 1200`; a fresh bar typed exactly `13,400` (this turn's `allInTo`) sends `to: 13400`; a fresh bar typed `7,000` sends `to: 7000`. Then the same three shapes against a **second** turn — `allowed: ["CHECK","BET"]`, `minBetTo: 350`, `allInTo: 8200` — sending `350`, `8200` and `4,000`. Two intervals, six sends: a reading stricter than the server's at either end reddens here, and `ADR-0111` says no server message would ever tell you otherwise |
| `refuses an entry over the stack, sends nothing, and quotes the maximum` | typing `20000` and pressing `Raise to` calls `send` zero times, leaves the field's value exactly `"20000"`, and puts `20,000 is over the maximum of 13,400.` on the bar — the ceiling is this turn's own `allInTo`, formatted the way the table formats every chip figure |
| `takes a plain zero as a number, and an empty field and a negative as neither` | on three fresh bars: `0` pressed gives `0 is under the minimum of 1,200.`; an emptied field gives `That is not an amount.`; `-500` gives `That is not an amount.`. `send` is called zero times on all three. `ADR-0111` §3 names all three cases individually, and the zero is the one that looks like the other two and is not |
| `refuses the same entry the same way twice, and takes no lock` | typing `500`, pressing `Raise to`, then pressing `Raise to` **again**: `send` is called zero times after both presses, the sentence `500 is under the minimum of 1,200.` stands after both, the field's value is still exactly `"500"`, and every action button is still enabled after both — no sent-lock was taken, so the refusal is safely repeatable (`ADR-0111` §1) |
| `prints no amount on the button while the entry is refused, and never a different one` | with `500` typed, the `Raise to` button's `textContent` is exactly `Raise to` and matches no `/\d/`; with `20000` typed, the same; with `3,000` typed the button reads `Raise to 3,000` again. Three states in one flow, so the assertion cannot pass by the button being permanently figureless — this is `ADR-0111` §7 and `ADR-0100` §2's read-before-click contract, which a clamp coming back through the paint would break |
| `keeps the server's own word until the entry is refused, and gives it back` | rendered with `rejection={{ type: "NotYourTurn", seatToAct: 1 }}` the bar says `The server says it is seat 1's turn.`; typing `500` and pressing `Raise to` replaces it with `500 is under the minimum of 1,200.` and the server's sentence is gone; typing `1,200` brings the server's sentence back and the local one is gone. The local sentence is about the entry the player is holding **now**, so it wins while it stands and is cleared by the next keystroke — and `rejection-text.ts` keeps saying everything the reading cannot know (`ADR-0111` §4) |

The four literal gates in `verify:` are **supplementary**. What stops an assertion from being
deleted is the exact count — `Tests  36 passed (36)` — together with the six named criteria below;
the `n < k` greps only refuse a test that builds its expected sentence instead of writing it out.

## Acceptance criteria

- [ ] `src/table/ActionBar.test.tsx` reports `Tests  36 passed (36)`
- [ ] `the action bar.sends both ends of the interval and one total inside it` passes
- [ ] `the action bar.refuses an entry over the stack, sends nothing, and quotes the maximum` passes
- [ ] `the action bar.takes a plain zero as a number, and an empty field and a negative as neither`
      passes
- [ ] `the action bar.refuses the same entry the same way twice, and takes no lock` passes
- [ ] `the action bar.prints no amount on the button while the entry is refused, and never a
      different one` passes
- [ ] `the action bar.keeps the server's own word until the entry is refused, and gives it back`
      passes
- [ ] `src/table/bar-no-derivation.test.tsx` still reports `3`, `src/table/null-view.test.tsx`
      still `6`, `src/e2e/whole-duel.test.tsx` still `8`, `src/lobby/Lobby.test.tsx` still `80`
- [ ] `ActionBar.tsx` is unchanged: still exactly one `aria-label="the total"`, still exactly two
      `Math.floor`, still mentions `readTypedAmount`, still contains no `Math.min`, `Math.max` or
      `clamp`
- [ ] `web-client/src/e2e/drive-duel.tsx` contains no `fireEvent.change`, no `getByLabelText` and
      no `.value =`
- [ ] `cd web-client && npm run check` exits 0
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
