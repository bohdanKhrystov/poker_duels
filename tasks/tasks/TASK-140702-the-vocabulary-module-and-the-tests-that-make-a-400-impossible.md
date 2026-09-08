---
schema: 2
id: TASK-140702
title: The vocabulary module, its first list, and the tests that make a 400 impossible
type: task
status: done
parent: STORY-1407
module: web-client
estimate: S
tier: sonnet
review: standard
files_touched: 2
labels: [client, profile, minting]
depends_on: [TASK-140701]
verify:
  - cd web-client && npm ci
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npm run --silent check
  - cd web-client && test "$(grep -c '^import' src/profile/name-vocabulary.ts || true)" = "0"
  - cd web-client && test "$(grep -c 'require(' src/profile/name-vocabulary.ts || true)" = "0"
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/profile/name-vocabulary.test.ts > "${TMPDIR:-/tmp}/nv-after.txt" 2>&1; grep -qF 'Tests  6 passed (6)' "${TMPDIR:-/tmp}/nv-after.txt"
  - cd web-client && cp src/profile/name-vocabulary.ts "${TMPDIR:-/tmp}/nv.fixed.ts" && perl -0pi -e 's{^    "[^"]+",}{    "Iron Grey",}m' src/profile/name-vocabulary.ts && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/profile/name-vocabulary.test.ts > "${TMPDIR:-/tmp}/m1.txt" 2>&1; cp "${TMPDIR:-/tmp}/nv.fixed.ts" src/profile/name-vocabulary.ts; cmp -s "${TMPDIR:-/tmp}/nv.fixed.ts" src/profile/name-vocabulary.ts && grep -qF 'Tests  1 failed | 5 passed (6)' "${TMPDIR:-/tmp}/m1.txt" && grep -qF 'holds only entries the server would accept' "${TMPDIR:-/tmp}/m1.txt"
  - cd web-client && cp src/profile/name-vocabulary.ts "${TMPDIR:-/tmp}/nv.fixed.ts" && perl -0pi -e 's{^    "[^"]+",}{    "Ashe\\u0301",}m' src/profile/name-vocabulary.ts && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/profile/name-vocabulary.test.ts > "${TMPDIR:-/tmp}/m2.txt" 2>&1; cp "${TMPDIR:-/tmp}/nv.fixed.ts" src/profile/name-vocabulary.ts; cmp -s "${TMPDIR:-/tmp}/nv.fixed.ts" src/profile/name-vocabulary.ts && grep -qF 'Tests  1 failed | 5 passed (6)' "${TMPDIR:-/tmp}/m2.txt" && grep -qF 'holds only entries that equal their own NFC form' "${TMPDIR:-/tmp}/m2.txt"
  - cd web-client && cp src/profile/name-vocabulary.ts "${TMPDIR:-/tmp}/nv.fixed.ts" && perl -0pi -e 's{^    "[^"]+",}{    "Unbreakable",}m' src/profile/name-vocabulary.ts && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/profile/name-vocabulary.test.ts > "${TMPDIR:-/tmp}/m3.txt" 2>&1; cp "${TMPDIR:-/tmp}/nv.fixed.ts" src/profile/name-vocabulary.ts; cmp -s "${TMPDIR:-/tmp}/nv.fixed.ts" src/profile/name-vocabulary.ts && grep -qF 'Tests  1 failed | 5 passed (6)' "${TMPDIR:-/tmp}/m3.txt" && grep -qF 'holds no entry longer than ten code points' "${TMPDIR:-/tmp}/m3.txt"
  - cd web-client && cp src/profile/name-vocabulary.ts "${TMPDIR:-/tmp}/nv.fixed.ts" && perl -0pi -e 's{^    "[^"]+",\n}{}m' src/profile/name-vocabulary.ts && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/profile/name-vocabulary.test.ts > "${TMPDIR:-/tmp}/m4.txt" 2>&1; cp "${TMPDIR:-/tmp}/nv.fixed.ts" src/profile/name-vocabulary.ts; cmp -s "${TMPDIR:-/tmp}/nv.fixed.ts" src/profile/name-vocabulary.ts && grep -qF 'Tests  1 failed | 5 passed (6)' "${TMPDIR:-/tmp}/m4.txt" && grep -qF 'holds the lists this story has shipped so far' "${TMPDIR:-/tmp}/m4.txt"
  - cd web-client && cp src/profile/name-vocabulary.ts "${TMPDIR:-/tmp}/nv.fixed.ts" && perl -0pi -e 's{^    "[^"]+",\n    "[^"]+",}{    "Quiet",\n    "quiet",}m' src/profile/name-vocabulary.ts && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/profile/name-vocabulary.test.ts > "${TMPDIR:-/tmp}/m5.txt" 2>&1; cp "${TMPDIR:-/tmp}/nv.fixed.ts" src/profile/name-vocabulary.ts; cmp -s "${TMPDIR:-/tmp}/nv.fixed.ts" src/profile/name-vocabulary.ts && grep -qF 'Tests  1 failed | 5 passed (6)' "${TMPDIR:-/tmp}/m5.txt" && grep -qF 'repeats no name inside a list under a case fold' "${TMPDIR:-/tmp}/m5.txt"
  - python3 .github/scripts/lint_tickets.py
---

## Goal

`web-client/src/profile/name-vocabulary.ts` exists, imports nothing, and holds the first of the
three lists a suggestion is drawn from — **fifty qualities**. Beside it, five `O(entries)` tests
make the strongest claim in `ADR-0137` true rather than asserted: **no string this vocabulary can
produce is one the server's own `canonicalDisplayNameOrNull` would refuse with `400`.** A sixth
pins the arity, so a list that shrinks reddens a test rather than quietly shrinking the namespace.

## Files

| File | Action |
| --- | --- |
| `web-client/src/profile/name-vocabulary.ts` | create |
| `web-client/src/profile/name-vocabulary.test.ts` | create |

Read [`ADR-0137`](../../docs/adr/ADR-0137-a-name-suggestion-is-drawn-in-the-browser.md) §§3–4 and
`STORY-1407`'s *Design notes* — the table there maps each of `canonicalDisplayNameOrNull`'s five
refusals to the test below that forecloses it, and it was read off
`poker-server/src/main/kotlin/duels/poker/server/http/DisplayName.kt`, which **this ticket does not
open**. **Nothing outside the table above is changed.**

## Scope

- **The module declares one thing and imports nothing:**

  ```ts
  export const NAME_VOCABULARY: readonly (readonly string[])[] = [
    [ /* fifty qualities */ ],
  ];
  ```

  Bundled and synchronous — not a JSON asset, not a dynamic import, not a fetch (`ADR-0137` §3),
  which is what *"a suggestion cannot fail to arrive"* rests on. Two `verify:` gates pin `import`
  and `require(` at zero.
- **Fifty entries in list 0, each a quality**, in the product's own register — the vision's
  *dark, quiet, minimal*, a duelling vocabulary and not a casino one. `Quiet`, `Cold`, `Ashen`,
  `Level`, `Patient` are the shape. They are drawn first and read first, so they are adjectives that
  sit in front of a substance and a figure: `Quiet Iron Raven`.
- **Each entry is capitalised as it will appear** — the joiner adds nothing but one `U+0020`, so
  what is written here is what a stranger reads on the ladder, forever.
- **Each entry is at most ten code points**, contains no space of any kind, and is a name a person
  would keep. `ADR-0137` §4's arithmetic: three lists of ten-code-point maxima join to exactly 32
  with two separators, which is the ceiling `ADR-0029` §2 sets.
- **This ticket ships words nobody has reviewed for meaning**, and `ADR-0137` says so outright: the
  tests hold shape and never meaning, `ADR-0051` §5's blocklist ships empty, and *"whoever ships a
  vocabulary owns that it says nothing about anybody"*. Nothing here may be a slur, a body part, a
  gambling noun, a brand, a person's name, a place with a war in it, or a word whose second reading
  is worse than its first. That judgement is the human's and it may trail the merge (`ADR-0091` §3).
- **Five tests hold the vocabulary, and one holds its arity** — all six over `NAME_VOCABULARY` as a
  whole, so the two lists that follow are covered the moment they land, with no test edited:

  | Test | Rule |
  | --- | --- |
  | holds only entries the server would accept | every entry matches `/^[^\p{Cc}\p{Cf}\s\p{Z}]+$/u` — non-empty, no control or format character, and **no whitespace at all**, which is what makes the join total |
  | holds only entries that equal their own NFC form | `entry === entry.normalize("NFC")` |
  | holds no entry longer than ten code points | `[...entry].length <= 10`, never `.length`, which over-counts astral characters exactly as `ADR-0029` §2 warns |
  | joins its longest entries into a name that fits thirty-two code points | the sum of each list's longest entry, in code points, plus one per separator, is `<= 32` |
  | repeats no name inside a list under a case fold | `new Set(list.map(e => e.toLowerCase())).size === list.length` — two entries differing only in case are **one** name under `ADR-0029` §1's fold, so without this the arity is a count of strings rather than of reachable names |
  | holds the lists this story has shipped so far | `NAME_VOCABULARY.map(l => l.length)` equals `[50]` |

  Each of the first three collects the offending entries and asserts the collection is empty
  (`expect(bad).toEqual([])`), so a failure **names the word** instead of printing `false`.
- **Five mutations are in `verify:`, and each names the one test it reddens.** They replace the
  first entry with a spaced one, a decomposed one and an eleven-code-point one, delete an entry, and
  make two entries differ only in case; every one restores the file and `cmp`s it afterwards. In the
  decomposed mutation the `\\u0301` is deliberately double-backslashed: perl's replacement would
  otherwise read `\u` as its titlecase operator, and what has to reach the file is the two
  characters `\u` of a TypeScript escape.

## Out of scope

- **The other two lists** — `TASK-140703`, `TASK-140704` and `TASK-140705`. This ticket's arity
  assertion is `[50]` and each of those moves it by one line.
- **`suggestName`, `SUGGESTION_SPACE_FLOOR` and the floor test** — `TASK-140706`. Nothing here
  reads the vocabulary except its own test.
- **Any Kotlin file.** The server's rules are read from `STORY-1407`'s table, not from the source;
  `ADR-0137` §1 keeps this whole story off the wire and out of `poker-server`.
- **A blocklist, a screen, or a second vocabulary.** `ADR-0137` §8.

## Tests

`web-client/src/profile/name-vocabulary.test.ts`, `describe("the name vocabulary")`

| Test | Proves |
| --- | --- |
| `holds only entries the server would accept` | no entry can produce a `400` by character: no `Cc`, no `Cf`, no whitespace, never empty |
| `holds only entries that equal their own NFC form` | the server's `Normalizer.normalize` cannot silently rewrite a name the product offered |
| `holds no entry longer than ten code points` | the rule a curator can break in a single word, measured in code points |
| `joins its longest entries into a name that fits thirty-two code points` | the ceiling `ADR-0029` §2 sets, bounded over the whole Cartesian product without enumerating it |
| `repeats no name inside a list under a case fold` | the arity counts reachable names, not strings |
| `holds the lists this story has shipped so far` | the vocabulary is `[50]` — a deleted word reddens here |

## Acceptance criteria

- [ ] `the name vocabulary > holds only entries the server would accept` passes, and replacing an
      entry with `Iron Grey` makes it — and only it — fail: `Tests  1 failed | 5 passed (6)`
- [ ] `holds only entries that equal their own NFC form` passes, and a decomposed entry makes it —
      and only it — fail with the same totals
- [ ] `holds no entry longer than ten code points` passes, and `Unbreakable` makes it — and only it
      — fail with the same totals
- [ ] `repeats no name inside a list under a case fold` passes, and two entries differing only in
      case make it — and only it — fail with the same totals
- [ ] `holds the lists this story has shipped so far` passes at `[50]`, and deleting one entry makes
      it — and only it — fail with the same totals
- [ ] `joins its longest entries into a name that fits thirty-two code points` passes
- [ ] `npx vitest run src/profile/name-vocabulary.test.ts` reports `Tests  6 passed (6)`
- [ ] `name-vocabulary.ts` holds no `import` and no `require(`
- [ ] Every mutation above restores the file byte-for-byte (`cmp -s` exits 0)
- [ ] `cd web-client && npm run check` exits 0
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
