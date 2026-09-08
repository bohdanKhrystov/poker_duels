---
schema: 2
id: TASK-140703
title: The second list is a hundred substances
type: task
status: done
parent: STORY-1407
module: web-client
estimate: S
tier: sonnet
review: light
files_touched: 2
labels: [client, profile, minting]
depends_on: [TASK-140702]
verify:
  - cd web-client && npm ci
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npm run --silent check
  - cd web-client && test "$(grep -c '^import' src/profile/name-vocabulary.ts || true)" = "0"
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/profile/name-vocabulary.test.ts > "${TMPDIR:-/tmp}/nv-after.txt" 2>&1; grep -qF 'Tests  6 passed (6)' "${TMPDIR:-/tmp}/nv-after.txt"
  - cd web-client && grep -qF 'toEqual([50, 100])' src/profile/name-vocabulary.test.ts
  - cd web-client && cp src/profile/name-vocabulary.ts "${TMPDIR:-/tmp}/nv.fixed.ts" && perl -0pi -e 's{^    "[^"]+",\n(  \],\n\];)}{$1}m' src/profile/name-vocabulary.ts && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/profile/name-vocabulary.test.ts > "${TMPDIR:-/tmp}/m6.txt" 2>&1; cp "${TMPDIR:-/tmp}/nv.fixed.ts" src/profile/name-vocabulary.ts; cmp -s "${TMPDIR:-/tmp}/nv.fixed.ts" src/profile/name-vocabulary.ts && grep -qF 'Tests  1 failed | 5 passed (6)' "${TMPDIR:-/tmp}/m6.txt" && grep -qF 'holds the lists this story has shipped so far' "${TMPDIR:-/tmp}/m6.txt"
  - python3 .github/scripts/lint_tickets.py
---

## Goal

`NAME_VOCABULARY` holds a second list — **one hundred substances** — and the arity assertion moves
to `[50, 100]`. Nothing else changes: the five shape tests written by `TASK-140702` already range
over every list, so the new hundred is held by them the moment it lands.

## Files

| File | Action |
| --- | --- |
| `web-client/src/profile/name-vocabulary.ts` | modify |
| `web-client/src/profile/name-vocabulary.test.ts` | modify |

Read [`ADR-0137`](../../docs/adr/ADR-0137-a-name-suggestion-is-drawn-in-the-browser.md) §§3–4 and
`TASK-140702`, whose *Scope* fixes every rule an entry obeys. **Nothing outside the table above is
changed.**

## Scope

- **A second array inside `NAME_VOCABULARY`, after the qualities**, holding **exactly one hundred**
  entries: a **substance or element** — a metal, a stone, a wood, a weather, an ore, an ash.
  `Iron`, `Ash`, `Salt`, `Slate`, `Frost`, `Oak`, `Bronze` are the shape. It is the middle word of
  `Quiet Iron Raven`, so it reads as a material a figure is made of or named for.
- **Every rule `TASK-140702` set still holds, unchanged and untested-again**: capitalised as it will
  appear, at most ten code points, no whitespace of any kind, NFC, and no two entries in this list
  differing only in case. The five shape tests are written over `NAME_VOCABULARY` as a whole and are
  **not edited**.
- **One line of the test file moves**: the arity assertion becomes `toEqual([50, 100])`. A `verify:`
  gate reads that literal out of the file, and a mutation deleting the new list's last entry reddens
  that test and no other.
- **The words are this ticket's one judgement**, under `TASK-140702`'s standard: nothing that names
  a person, a brand, a place with a war in it, or a word whose second reading is worse than its
  first. The human's visual and editorial verdict may trail the merge (`ADR-0091` §3).

## Out of scope

- **The third list** — `TASK-140704` and `TASK-140705`.
- **Editing any of the five shape tests.** If one of them fails, the word is wrong, not the test.
- **`suggestName` and the floor test** — `TASK-140706`. The product is `5_000` after this ticket and
  the floor is not asserted until the vocabulary is whole.

## Tests

`web-client/src/profile/name-vocabulary.test.ts`, `describe("the name vocabulary")` — the same six,
with one assertion moved.

| Test | Proves |
| --- | --- |
| `holds the lists this story has shipped so far` | the vocabulary is now `[50, 100]`, so a deleted or duplicated word reddens here |
| the other five | unchanged, and now ranging over a hundred and fifty entries |

## Acceptance criteria

- [ ] `NAME_VOCABULARY` has two lists, of 50 and 100 entries, and
      `holds the lists this story has shipped so far` asserts exactly `toEqual([50, 100])`
- [ ] `npx vitest run src/profile/name-vocabulary.test.ts` reports `Tests  6 passed (6)`
- [ ] Deleting the last entry of the new list makes `holds the lists this story has shipped so far`
      — and only it — fail: `Tests  1 failed | 5 passed (6)`, and the file is restored byte-for-byte
- [ ] `name-vocabulary.ts` still holds no `import`
- [ ] The five shape tests are byte-unchanged
- [ ] `cd web-client && npm run check` exits 0
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
