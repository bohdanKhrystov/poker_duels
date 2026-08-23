---
schema: 2
id: TASK-031312
title: The duel screen says none of the words this copy refuses
type: task
status: backlog
parent: STORY-0313
module: web-client
estimate: S
tier: sonnet
review: standard
files_touched: 1
labels: [client, duel, ui, presence, copy]
depends_on: [TASK-031311]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test 2>&1 | grep -qE 'Tests +619 passed \(619\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'says exactly one of the four lines, in each of the four states'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'puts none of the refused words in front of a player'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'reports every refused word when one is planted'
  - cd web-client && npm run check
---

## Goal

A guard in the tradition of `no-derivation.test.tsx`: whatever else the duel screen grows, it says
one of `ADR-0046`'s lines and none of the words `ADR-0046` §5 refuses.

## Files

| File | Action |
| --- | --- |
| `web-client/src/lobby/presence-copy.test.tsx` | create |
| `web-client/src/table/no-derivation.test.tsx` | read — `wordsOnScreen` and `spokenOnScreen`, the two walkers this copies |
| `docs/adr/ADR-0046-the-table-says-away-timed-out-and-back.md` | read — §2's table and §5's table |

## Scope

- One new test file, rendering the **whole duel screen** — `<Lobby>` under `<DuelProvider>` with a
  store the test drives — because the criterion is about what reaches a player, and a player reads
  the composed screen. `HistoryScreen` and `ProfileStrip` are not mounted by the duel branch, which
  is why this guard can be strict about words those screens legitimately use.
- Two local helpers, copied from `no-derivation.test.tsx` rather than imported (that file exports
  nothing): a `TreeWalker` over text nodes joined by a space, and a sweep of every `aria-label` and
  `title`. **Both**: a screen reader says the second and `textContent` cannot see it, and joining
  text nodes without a separator runs one element's last word into the next so `\b` stops seeing a
  boundary the eye sees plainly. Both facts are recorded in that file, measured.
- The refused set is `ADR-0046` §5, written as one regex per phrase, case-insensitive, with word
  boundaries: `opponent`, `disconnected`, `connection lost`, `offline`, `left`, `quit`, `abandoned`,
  `gave up`, `forfeit`, `forfeited`, `sitting out`, `sit out`, `auto-fold`, `auto-check`, and any
  `!`. **Phrases, not fragments**: `\bout\b` would match `Timed out`, which is the one place the
  words *timed out* are correct.
- The four states are driven by frames, not by props: `AWAY`, `ABSENT`, `PRESENT` after an `AWAY`,
  and `PRESENT` on a fresh store. All four with this client at **seat 1**.

## Out of scope

- Scanning source. The rule is about copy, not identifiers: the wire type is still called
  `OpponentPresence` and `duel-state.ts` still has a field named `rivalPresence` — a grep over `.ts`
  files would fail on the protocol module and would be measuring the wrong thing (`STORY-0313`'s
  ninth criterion says so in as many words).
- Any screen but the duel screen. The lobby's own panels, the result screen and the history screen
  are other stories' copy.
- Adding or changing a string. This ticket writes tests only; if a refused word is found, that is a
  finding to report, not a licence to edit copy outside the ticket's files.

## Tests

`web-client/src/lobby/presence-copy.test.tsx`, one describe block: `"the duel screen's presence
copy"`.

`the duel screen's presence copy`

| Test | Proves |
| --- | --- |
| `says exactly one of the four lines, in each of the four states` | for each of the four states: the state's own line from `ADR-0046` §2 is on screen **verbatim**, and the other two lines are not, and for the fresh-`PRESENT` state none of the three is. Written as a table of `[frames, expectedLine \| null]` and looped, so a fifth state cannot be added without a row. The three sentences appear as literals in this file, not as an import from `presence-text.ts`: a constant shared by the encoder and its test lets one typo pass on both sides at once |
| `puts none of the refused words in front of a player` | for each of the four states, the joined text **and** the joined `aria-label`/`title` sweep match none of the refused patterns. Both sweeps asserted separately, so a word that reaches only a screen reader still fails |
| `reports every refused word when one is planted` | the sweep is not vacuous. For **each** refused pattern in turn, a detached `<p>` carrying a sentence containing that word is appended to the rendered container and the sweep reports it — a loop over the whole set, because a single planted word proves one pattern compiles and says nothing about the other thirteen. Asserted against the count of patterns, so adding a pattern without a plant fails |

Three tests. Six hundred and sixteen exist after `TASK-031311`, so the suite reports **619**.

## Proof

| Command | Proves |
| --- | --- |
| `Tests 619 passed (619)` | three ran and the six hundred and sixteen before them still do |
| the three `--reporter=verbose` greps | each exists by name |
| `npm run check` | the file typechecks and Prettier accepts the long literals |

**Name the edit that makes each assertion red:**

1. In `presence-text.ts`, change `Your rival is away.` to `Your opponent is away.` → **both** of the
   first two tests fail: the first on the verbatim line, the second on the `opponent` pattern.
   Revert. Two failures from one edit is the intended overlap, not redundancy — the first test
   catches a *changed* string and the second catches a *new* one the first has no row for.
2. Replace the `aria-label`/`title` sweep with the text sweep alone, and plant
   `aria-label="your opponent is offline"` on the notice → `puts none of the refused words in front
   of a player` **passes**, which is the measurement to record: without the spoken sweep this guard
   is half a guard. Restore the sweep and confirm it then fails.
3. Reduce the third test's loop to a single planted word → nothing fails, and that is the point of
   writing it as a loop: a passing planted-word test with one word says nothing about the rest, so
   the assertion is tied to the pattern count.

Quote all three in the PR, including that mutation 2 was green.

## Acceptance criteria

- [ ] `the duel screen's presence copy > says exactly one of the four lines, in each of the four states` passes
- [ ] `the duel screen's presence copy > puts none of the refused words in front of a player` passes
- [ ] `the duel screen's presence copy > reports every refused word when one is planted` passes
- [ ] All four states are driven by applying frames to a store, and none by passing props to a
      component directly
- [ ] The three `ADR-0046` §2 sentences appear in this file as string literals, not imported
- [ ] The third test loops over every refused pattern and asserts the reported count equals the
      pattern count
- [ ] No file outside `web-client/src/lobby/presence-copy.test.tsx` differs from `develop`
- [ ] `npm run --silent test` reports `Tests  619 passed (619)`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
