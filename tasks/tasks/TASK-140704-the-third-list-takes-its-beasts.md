---
schema: 2
id: TASK-140704
title: The third list takes its beasts
type: task
status: done
parent: STORY-1407
module: web-client
estimate: S
tier: sonnet
review: light
files_touched: 2
labels: [client, profile, minting]
depends_on: [TASK-140703]
verify:
  - cd web-client && npm ci
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npm run --silent check
  - cd web-client && test "$(grep -c '^import' src/profile/name-vocabulary.ts || true)" = "0"
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/profile/name-vocabulary.test.ts > "${TMPDIR:-/tmp}/nv-after.txt" 2>&1; grep -qF 'Tests  6 passed (6)' "${TMPDIR:-/tmp}/nv-after.txt"
  - cd web-client && grep -qF 'toEqual([50, 100, 100])' src/profile/name-vocabulary.test.ts
  - cd web-client && cp src/profile/name-vocabulary.ts "${TMPDIR:-/tmp}/nv.fixed.ts" && perl -0pi -e 's{^    "[^"]+",\n(  \],\n\];)}{$1}m' src/profile/name-vocabulary.ts && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/profile/name-vocabulary.test.ts > "${TMPDIR:-/tmp}/m7.txt" 2>&1; cp "${TMPDIR:-/tmp}/nv.fixed.ts" src/profile/name-vocabulary.ts; cmp -s "${TMPDIR:-/tmp}/nv.fixed.ts" src/profile/name-vocabulary.ts && grep -qF 'Tests  1 failed | 5 passed (6)' "${TMPDIR:-/tmp}/m7.txt" && grep -qF 'holds the lists this story has shipped so far' "${TMPDIR:-/tmp}/m7.txt"
  - python3 .github/scripts/lint_tickets.py
---

## Goal

`NAME_VOCABULARY` gains its **third and last list**, opened with **one hundred beasts**, and the
arity assertion moves to `[50, 100, 100]`. This is the word a player is left holding — the last of
the three, the noun in `Quiet Iron Raven` — and it is the half of the list that is not a person.

## Files

| File | Action |
| --- | --- |
| `web-client/src/profile/name-vocabulary.ts` | modify |
| `web-client/src/profile/name-vocabulary.test.ts` | modify |

Read [`ADR-0137`](../../docs/adr/ADR-0137-a-name-suggestion-is-drawn-in-the-browser.md) §§3–4 and
`TASK-140702`, whose *Scope* fixes every rule an entry obeys. **Nothing outside the table above is
changed.**

## Scope

- **A third array inside `NAME_VOCABULARY`**, holding **exactly one hundred** entries: a **beast** —
  a bird, an animal, a fish, a moth. `Raven`, `Wolf`, `Otter`, `Falcon`, `Hare`, `Pike` are the
  shape. `TASK-140705` adds a hundred **offices** to this same list; the two halves are one list
  because a name draws one word from it, and they are two tickets because a hundred entries is a
  hundred lines.
- **Every rule `TASK-140702` set still holds**: capitalised as it will appear, at most ten code
  points, no whitespace, NFC, and no two entries in this list differing only in case — including
  across the half `TASK-140705` will add, which is why that ticket re-runs the fold test rather than
  assuming it.
- **One line of the test file moves**: the arity assertion becomes `toEqual([50, 100, 100])`. The
  product is `500_000` — **half the floor** — and that is correct at this point: the floor is
  `TASK-140706`'s assertion and does not exist yet.
- **The five shape tests are not edited.**

## Out of scope

- **The other hundred figures** — `TASK-140705`.
- **`SUGGESTION_SPACE_FLOOR` and the floor test** — `TASK-140706`. Do not add either here: a floor
  test landing before the vocabulary is whole would ship red.
- **A fourth list.** Three is the arity a three-word name has, and a fourth would put a fourth word
  on a public ladder.

## Tests

`web-client/src/profile/name-vocabulary.test.ts`, `describe("the name vocabulary")` — the same six,
with one assertion moved.

| Test | Proves |
| --- | --- |
| `holds the lists this story has shipped so far` | the vocabulary is now `[50, 100, 100]` |
| the other five | unchanged, and now ranging over two hundred and fifty entries |

## Acceptance criteria

- [ ] `NAME_VOCABULARY` has three lists, of 50, 100 and 100 entries, and
      `holds the lists this story has shipped so far` asserts exactly `toEqual([50, 100, 100])`
- [ ] `npx vitest run src/profile/name-vocabulary.test.ts` reports `Tests  6 passed (6)`
- [ ] Deleting the last entry of the third list makes `holds the lists this story has shipped so
      far` — and only it — fail: `Tests  1 failed | 5 passed (6)`, and the file is restored
      byte-for-byte
- [ ] `joins its longest entries into a name that fits thirty-two code points` still passes with
      three lists and two separators
- [ ] `name-vocabulary.ts` still holds no `import`, and no `SUGGESTION_SPACE_FLOOR` is declared
      anywhere
- [ ] `cd web-client && npm run check` exits 0
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
