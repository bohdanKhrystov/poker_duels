---
schema: 2
id: TASK-050313
title: A flat ladder is an ordinary ladder, a tie is marked by nothing, and no row leads anywhere
type: task
status: backlog
parent: STORY-0503
module: web-client
estimate: S
tier: sonnet
review: standard
files_touched: 1
labels: [client, leaderboard, ui, refusals, tests]
depends_on: [TASK-050312]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'renders a nearly flat ladder exactly as it renders a spread one'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'marks no row on a page of equal ranks, and none as the one the reader stands on'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'renders the heading, the season, the self line and the rows, and nothing else'
  - cd web-client && ! grep -q 'toContain' src/ladder/LadderScreen.test.tsx
  - cd web-client && npm run check
---

## Goal

Everything this story refuses is a failing test if somebody adds it: a softening of a flat ladder, a
tie marker, a highlight on the reader's row, a jump control, a ladder total, a movement line, a link
out of a row.

## Why a refusal needs a test

Six merged decisions refuse things here, and a refusal that ships as prose is a refusal a coder
fills in as a gap. `ADR-0064` §5 (a tie is the repeated number **and nothing else**), `ADR-0064` §6
and its alternative 5 (a nearly-flat ladder gets no *the season is still young* affordance —
suppressing the ladder until standings separate is *"exactly the kind of eligibility rule
`ADR-0063` refused"*), `ADR-0065` §5 (no marker, no jump, no scroll-to-my-row), `ADR-0065` §6 (the
reader appearing twice is correct), `ADR-0065` §7 (no total, no movement, no streak, no tie count),
and `DEC-057` — **open** — which is why a row is text and links nowhere until it is answered.

This ticket adds tests and **changes no production file**. If one fails, the smallest fix that makes
it pass is in scope and nothing else is.

## Files

| File | Action |
| --- | --- |
| `web-client/src/ladder/LadderScreen.test.tsx` | modify — **adds tests only**; no assertion written by `TASK-050307` through `TASK-050312` changes |

## Scope

- Two fixtures, both `season: "2026-09"`, both `self: { rank: 5, coins: 1 }`, both
  `nextCursor: null`, both four rows with names `["Ada", "Bo", null, "Cy"]`:
  - **flat** — the routine second day of a season: ranks `[5, 5, 5, 5]`, coins `[1, 1, 1, 1]`.
  - **spread** — ranks `[2, 3, 4, 6]`, coins `[4, 3, 2, 1]`. Deliberately not `1..n`: no rank
    fixture in this story is the identity sequence, so a position-derived rank cannot pass anywhere
    by coincidence.
- `nextCursor: null` on both, so the *Show more* control renders on neither and a button found
  anywhere in the section is a control somebody added.

## Out of scope

- **Changing `LadderScreen.tsx` or `ladder-text.ts`**, unless one of these three tests fails.
- **Deciding whether a row should ever lead anywhere.** That is `DEC-057`, it is open, and
  `STORY-0504` is where it lands if the answer is yes. This ticket asserts today's answer, which is
  that a row is text.
- **Whether a tie is ever marked.** `ADR-0064` §5 says not in v0.3 and deliberately raised no `DEC`
  for it: it is a string and a field, and an ordinary ticket if it is ever wanted.

## Tests

`web-client/src/ladder/LadderScreen.test.tsx`, same `describe`, three new tests.

| Test | Proves |
| --- | --- |
| `renders a nearly flat ladder exactly as it renders a spread one` | Render the flat fixture, wait for four rows, capture `container.querySelector("section").innerHTML`, unmount; render the spread fixture and capture the same. Strip every text node from both — `html.replace(/>[^<]*</g, "><")` — and assert the two skeletons are **equal**. Identical elements, identical classes, only the words differ. A *the season is still young* banner, a tie glyph element, a grouping wrapper or an extra class on a tied row exists in one and not the other, and reddens |
| `marks no row on a page of equal ranks, and none as the one the reader stands on` | On the flat fixture, whose four rows all carry the rank and standing the self line carries: the list holds **four** `<li>` — none removed for duplicating the reader — every `<li>` has the same `className` as every other, and the list's `textContent` is exactly `"5 Ada 15 Bo 15 No name 15 Cy 1"`, the four row lines and nothing between them. A `=`, a `T5`, a *tied with 3 others*, or a highlight class on any row reddens |
| `renders the heading, the season, the self line and the rows, and nothing else` | On the flat fixture, the section's `textContent` is exactly `"Leaderboard"` + `"September 2026"` + `"You are rank 5 this season, on 1 duel coin."` + the four row lines, concatenated in that order; `queryAllByRole("link")` inside the section is empty, `querySelectorAll("a")` is empty, and `queryAllByRole("button")` inside the section is empty. A ladder total (*5th of 404*), a movement line, a streak, a tie count, a *jump to me* button or a row that links to a player each add to that string or to one of those counts |

The expected strings are written literally, not built by calling `rowLine`, `selfLine` or
`seasonName`. If the concatenation does not match because JSX introduced whitespace, the fix is to
remove the whitespace from the component — not to weaken the assertion to `toContain`.
That is now a **gate, not an instruction**: the last `verify:` line refuses the string
`toContain` anywhere in this test file. `.toContain(` is used nowhere in it, so the check has
no false positives — and without it, a single `toBe` → `toContain` retires all six refusals
while every test still passes and every named test still appears in the reporter output.

## Acceptance criteria

- [ ] `renders a nearly flat ladder exactly as it renders a spread one` passes — adding any element
      or class that renders only when ranks repeat reddens it
- [ ] `marks no row on a page of equal ranks, and none as the one the reader stands on` passes with
      four rows and one shared `className` — appending `=` to a repeated rank reddens it, and so
      does adding a class to a row whose rank equals the self line's
- [ ] `renders the heading, the season, the self line and the rows, and nothing else` passes with the
      exact concatenation and three empty collections — wrapping a row in an `<a>` reddens it, and so
      does adding a *jump to me* button or a *5th of 404* line
- [ ] Every test `TASK-050307` through `TASK-050312` wrote still passes, with no assertion in any of
      them edited
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
