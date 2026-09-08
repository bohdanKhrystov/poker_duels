---
schema: 2
id: TASK-140705
title: The third list takes its offices, and the arity reaches a million
type: task
status: backlog
parent: STORY-1407
module: web-client
estimate: S
tier: sonnet
review: light
files_touched: 2
labels: [client, profile, minting]
depends_on: [TASK-140704]
verify:
  - cd web-client && npm ci
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npm run --silent check
  - cd web-client && test "$(grep -c '^import' src/profile/name-vocabulary.ts || true)" = "0"
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/profile/name-vocabulary.test.ts > "${TMPDIR:-/tmp}/nv-after.txt" 2>&1; grep -qF 'Tests  6 passed (6)' "${TMPDIR:-/tmp}/nv-after.txt"
  - cd web-client && grep -qF 'toEqual([50, 100, 200])' src/profile/name-vocabulary.test.ts
  - cd web-client && cp src/profile/name-vocabulary.ts "${TMPDIR:-/tmp}/nv.fixed.ts" && perl -0pi -e 's{^    "[^"]+",\n(  \],\n\];)}{$1}m' src/profile/name-vocabulary.ts && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/profile/name-vocabulary.test.ts > "${TMPDIR:-/tmp}/m8.txt" 2>&1; cp "${TMPDIR:-/tmp}/nv.fixed.ts" src/profile/name-vocabulary.ts; cmp -s "${TMPDIR:-/tmp}/nv.fixed.ts" src/profile/name-vocabulary.ts && grep -qF 'Tests  1 failed | 5 passed (6)' "${TMPDIR:-/tmp}/m8.txt" && grep -qF 'holds the lists this story has shipped so far' "${TMPDIR:-/tmp}/m8.txt"
  - python3 .github/scripts/lint_tickets.py
---

## Goal

The third list takes its other hundred — **offices**, the figures a person can be — and the
vocabulary is whole: `50 × 100 × 200 = 1_000_000`, exactly `ADR-0137` §4's floor. The arity
assertion becomes `[50, 100, 200]`, and from here **one deleted word puts the vocabulary under the
floor** the next ticket asserts.

## Files

| File | Action |
| --- | --- |
| `web-client/src/profile/name-vocabulary.ts` | modify |
| `web-client/src/profile/name-vocabulary.test.ts` | modify |

Read [`ADR-0137`](../../docs/adr/ADR-0137-a-name-suggestion-is-drawn-in-the-browser.md) §§3–4 and
`TASK-140702`, whose *Scope* fixes every rule an entry obeys. **Nothing outside the table above is
changed.**

## Scope

- **A hundred more entries appended to the third list**: an **office** — a role, a trade, a rank, a
  post. `Warden`, `Envoy`, `Scribe`, `Herald`, `Mason`, `Drover` are the shape. They join the
  hundred beasts `TASK-140704` shipped, in the same array, because a name draws **one** word from
  this list.
- **The fold test now spans both halves.** `repeats no name inside a list under a case fold` runs
  over the whole two hundred, so an office that repeats a beast — or another office — reddens it.
  That is the one shape test whose meaning changes when a list grows, and it is why this ticket
  re-runs the suite rather than assuming the previous green.
- **One line of the test file moves**: the arity assertion becomes `toEqual([50, 100, 200])`.
- **Why the shape is 50 × 100 × 200 and not 100 × 100 × 100**: the product is the same million, and
  the split is what let four data tickets each stay inside a ticket's line budget while the lists
  that are hardest to curate stay the shortest. The arity is data (`ADR-0137` §3) — a curator may
  re-balance it later with no code change, provided the floor holds.
- **Exactly at the floor is deliberate.** `ADR-0137` §4 makes the product the whole of the rarity
  guarantee; with no slack, the next ticket's floor test reddens on a single deleted word, which is
  precisely the *"a curator could halve the list while tidying it and nothing would fail"* failure
  that ADR refuses.

## Out of scope

- **`SUGGESTION_SPACE_FLOOR`, `suggestName` and the floor test** — `TASK-140706`, the next ticket.
- **Any change to the five shape tests**, and any fourth list.

## Tests

`web-client/src/profile/name-vocabulary.test.ts`, `describe("the name vocabulary")` — the same six,
with one assertion moved.

| Test | Proves |
| --- | --- |
| `holds the lists this story has shipped so far` | the vocabulary is `[50, 100, 200]`, whose product is exactly `1_000_000` |
| `repeats no name inside a list under a case fold` | the two hundred figures are two hundred **names**, so the product counts what a draw can reach |
| the other four | unchanged, over three hundred and fifty entries |

## Acceptance criteria

- [ ] `NAME_VOCABULARY` has three lists of 50, 100 and 200 entries, and
      `holds the lists this story has shipped so far` asserts exactly `toEqual([50, 100, 200])`
- [ ] `npx vitest run src/profile/name-vocabulary.test.ts` reports `Tests  6 passed (6)`
- [ ] Deleting the last entry makes `holds the lists this story has shipped so far` — and only it —
      fail: `Tests  1 failed | 5 passed (6)`, and the file is restored byte-for-byte
- [ ] No entry of the third list repeats another under a case fold, across both halves
- [ ] `name-vocabulary.ts` still holds no `import`
- [ ] `cd web-client && npm run check` exits 0
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
