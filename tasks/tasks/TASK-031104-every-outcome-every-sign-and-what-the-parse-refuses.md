---
schema: 2
id: TASK-031104
title: Every outcome, every sign, and what the duel parse refuses
type: task
status: done
parent: STORY-0311
module: web-client
estimate: S
tier: haiku
review: light
files_touched: 1
labels: [client, http, profile, tests]
depends_on: [TASK-031103]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test 2>&1 | grep -qE 'Tests +336 passed \(336\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'reads every outcome the server can send'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'takes the coin delta signed, including the zero of a draw'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'answers unavailable when a row is not a duel'
  - cd web-client && npm run check
---

## Goal

The recent-duels parse is proven for **every** value the wire can carry, not for the one row
`TASK-031103` happened to use — and the branch that refuses a row is shown to fire.

## Why this exists as its own ticket

`TASK-031103` proves the shape with a single `WON`, `+1` row. Three claims in it are universal —
*every outcome*, *the delta is signed*, *a bad row is refused* — and a universal claim is a promise
to enumerate. Its file was already at its budget, so the enumeration is here, against the same parse
and with no production change.

## Files

| File | Action |
| --- | --- |
| `web-client/src/profile/recent-duels.test.ts` | modify — three tests added, none changed |
| `web-client/src/profile/recent-duels.ts` | read — the parse under test |

## Scope

- Three tests in the existing `describe("the recent duels read")` block, using the helpers already
  in the file.
- **No production file is touched.** Every branch these tests reach was written by `TASK-031103`;
  if one of them is red on first run, that is a defect found, reported in the PR, and fixed here
  only if the fix is a single line — otherwise it is its own ticket.
- No existing test is renamed or has an assertion changed.

## Out of scope

- Rendering any of these values. `TASK-031105` turns an outcome into a word.
- A row's `duelId`, which nothing displays and nothing yet uses.
- Adding fields to `RecentDuel`.

## Tests

`web-client/src/profile/recent-duels.test.ts`, in the existing describe block.

| Test | Proves |
| --- | --- |
| `reads every outcome the server can send` | one body carrying three rows, `"WON"`, `"LOST"` and `"DREW"`, parses to three duels whose outcomes are exactly those three in that order. All three named — the wire has exactly three and the type claims all three |
| `takes the coin delta signed, including the zero of a draw` | three rows with `coinDelta` `1`, `-1` and `0` parse to `1`, `-1` and `0`. `-1` survives because it is what a loss is; `0` survives because `ADR-0015` makes a draw a real result rather than a missing one |
| `answers unavailable when a row is not a duel` | four bodies, each written out: a row with no `outcome`; a row whose `outcome` is `"TIED"`; a row whose `coinDelta` is `"one"`; a body whose `duels` is not an array. Each answers `{ kind: "unavailable" }` |

Three tests added. Three hundred and thirty-three exist, so the suite reports **336**.

## Proof

| Command | Proves |
| --- | --- |
| `Tests 336 passed (336)` | three ran and nothing else moved |
| the three `--reporter=verbose` greps | every name above exists |
| `npm run check` | typechecks under `strict`, lints, is formatted |

**Name the edit that makes each assertion red** — run each, quote two in the PR, revert:

1. Accept any string as an `outcome` → `answers unavailable when a row is not a duel` fails on the
   `"TIED"` case while its other three cases still pass.
2. Return `Math.abs(coinDelta)` → `takes the coin delta signed, including the zero of a draw` fails
   on `-1` only.
3. Return only the first row → `reads every outcome the server can send` fails on the length.

## Acceptance criteria

- [ ] `the recent duels read > reads every outcome the server can send` passes and names all three
      outcome words
- [ ] `the recent duels read > takes the coin delta signed, including the zero of a draw` passes and
      covers `1`, `-1` and `0`
- [ ] `the recent duels read > answers unavailable when a row is not a duel` passes and writes out
      all four rejected bodies
- [ ] No production file differs — `recent-duels.ts`, `profile.ts` and `api.ts` are byte-identical
- [ ] No existing test in the file is renamed, removed, or has an assertion changed
- [ ] `npm run --silent test` reports `Tests  336 passed (336)`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
