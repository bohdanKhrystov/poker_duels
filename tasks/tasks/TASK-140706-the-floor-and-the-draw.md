---
schema: 2
id: TASK-140706
title: The floor and the draw
type: task
status: backlog
parent: STORY-1407
module: web-client
estimate: S
tier: sonnet
review: standard
files_touched: 2
labels: [client, profile]
depends_on: [TASK-140705]
verify:
  - cd web-client && npm ci
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npm run --silent check
  - cd web-client && test "$(grep -c '^import' src/profile/name-suggestion.ts || true)" = "1"
  - cd web-client && test "$(grep -cE 'Math\.random|localStorage|sessionStorage|window\.|document\.' src/profile/name-suggestion.ts || true)" = "0"
  - cd web-client && grep -qF 'export const SUGGESTION_SPACE_FLOOR = 1_000_000;' src/profile/name-suggestion.ts
  - cd web-client && grep -qF 'Math.min(list.length - 1, Math.floor(random() * list.length))' src/profile/name-suggestion.ts
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/profile/name-suggestion.test.ts > "${TMPDIR:-/tmp}/ns-after.txt" 2>&1; grep -qF 'Tests  4 passed (4)' "${TMPDIR:-/tmp}/ns-after.txt"
  - cd web-client && cp src/profile/name-suggestion.ts "${TMPDIR:-/tmp}/ns.fixed.ts" && perl -0pi -e 's{Math\.min\(list\.length - 1, Math\.floor\(random\(\) \* list\.length\)\)}{Math.floor(random() * list.length)}' src/profile/name-suggestion.ts && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/profile/name-suggestion.test.ts > "${TMPDIR:-/tmp}/n1.txt" 2>&1; cp "${TMPDIR:-/tmp}/ns.fixed.ts" src/profile/name-suggestion.ts; cmp -s "${TMPDIR:-/tmp}/ns.fixed.ts" src/profile/name-suggestion.ts && grep -qF 'Tests  1 failed | 3 passed (4)' "${TMPDIR:-/tmp}/n1.txt" && grep -qF 'clamps a source that returns one' "${TMPDIR:-/tmp}/n1.txt"
  - cd web-client && cp src/profile/name-vocabulary.ts "${TMPDIR:-/tmp}/nv.fixed.ts" && perl -0pi -e 's{^    "[^"]+",\n(  \],\n\];)}{$1}m' src/profile/name-vocabulary.ts && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/profile/name-suggestion.test.ts > "${TMPDIR:-/tmp}/n2.txt" 2>&1; cp "${TMPDIR:-/tmp}/nv.fixed.ts" src/profile/name-vocabulary.ts; cmp -s "${TMPDIR:-/tmp}/nv.fixed.ts" src/profile/name-vocabulary.ts && grep -qF 'Tests  1 failed | 3 passed (4)' "${TMPDIR:-/tmp}/n2.txt" && grep -qF 'holds the vocabulary at the floor' "${TMPDIR:-/tmp}/n2.txt"
  - python3 .github/scripts/lint_tickets.py
---

## Goal

`web-client/src/profile/name-suggestion.ts` exists and holds the two things `ADR-0137` §2 declares:
`SUGGESTION_SPACE_FLOOR = 1_000_000`, and `suggestName(random)` — a pure function of a `[0, 1)`
source that draws one entry from each list, in order, and joins them with one `U+0020`. The floor
becomes a **test** rather than a claim, and a scripted source makes the draw exactly reproducible.

## Files

| File | Action |
| --- | --- |
| `web-client/src/profile/name-suggestion.ts` | create |
| `web-client/src/profile/name-suggestion.test.ts` | create |

Read [`ADR-0137`](../../docs/adr/ADR-0137-a-name-suggestion-is-drawn-in-the-browser.md) §§2–4 and
`web-client/src/profile/name-vocabulary.ts`. **Nothing outside the table above is changed** — in
particular `name-vocabulary.ts` is read, never edited, and no component is opened.

## Scope

- **The signature is `ADR-0137` §2's, minus its optional second parameter:**

  ```ts
  export function suggestName(random: () => number): string;
  ```

  `replacing` is `TASK-140707`'s and is **not declared here** — a parameter that exists and is
  ignored is worse than one that does not exist yet.
- **It takes no player fact, and that is why it can encode none.** No `PlayerProfile`, no device id,
  no session, no room code, no clock, no `Storage`. `ADR-0029` §6's refusal of a server-minted
  `Player-3F2A` and `ADR-0067`'s *no id turns into a profile* hold **by the signature**: there is
  nowhere to put a player fact, and putting one there is a parameter a reviewer sees. A `verify:`
  gate pins the module's imports at exactly one — the vocabulary — and refuses `Math.random`,
  `localStorage`, `sessionStorage`, `window.` and `document.` anywhere in the file. **The gate reads
  the whole file, comments included**, so the KDoc says *no browser storage and no clock* in words
  rather than by naming those identifiers.
- **The default source lives at the call site, not here** (`ADR-0137` §§2, 7). `Math.random` is
  named in `TASK-140711`'s component prop and in no other file this story writes.
- **The index is `ADR-0137` §2's expression, written exactly as it prints it:**

  ```ts
  Math.min(list.length - 1, Math.floor(random() * list.length))
  ```

  The clamp is not defensive noise: without it a source returning `1` yields `undefined`, and
  `undefined` would be joined into a string a player can set as their name. A `verify:` gate holds
  the literal expression, and a mutation that removes the clamp reddens exactly one test. **Keep it
  on one line** — measured, `const index = Math.min(list.length - 1, Math.floor(random() * list.length));`
  is 78 columns at one indent level and prettier leaves it alone; nest it any deeper and the
  formatter wraps it and the gate stops matching.
- **`SUGGESTION_SPACE_FLOOR` is exported from this module** and is `1_000_000`, spelled with the
  numeric separator as `ADR-0137` §2 prints it. A `verify:` gate holds that line.
- **Four tests, and the fourth is the one that carries the ADR's strongest claim to the output.**
  Tests over the entries (`TASK-140702`) bound the whole Cartesian product; this one takes three
  scripted draws — the first entries, the last entries, and a middle tuple — and asserts the
  **joined string** is non-empty, equals its own NFC form, is at most 32 code points, matches
  `/^[^\p{Cc}\p{Cf}]+$/u`, holds no whitespace other than `U+0020`, holds no doubled space, and is
  unchanged by `trim()`. That is every branch of the server's `canonicalDisplayNameOrNull` read as
  an assertion about what this function returns.
- **The test's source is scripted, never random.** A helper turns an array of numbers into a
  `() => number`, so every assertion names an exact string built from `NAME_VOCABULARY` itself
  rather than a literal nobody can maintain: `expect(suggestName(scripted([0, 0, 0]))).toBe(first.join(" "))`
  where `first` is each list's first entry. **Two different scripted sources**, not one — a single
  input cannot tell a draw from a constant.

## Out of scope

- **`replacing` and the redraw** — `TASK-140707`.
- **Any call site.** No component, no `Lobby.tsx`, no `main.tsx`: this ticket adds a function nobody
  calls yet, and `TASK-140711` is the first caller.
- **Cryptographic randomness, and the engine's `Rng`.** `ADR-0137` §2 refuses both by name: a
  display name is half of nothing (`ADR-0029` §7), and the engine's source is deterministic, which
  is exactly what a suggestion must not be.
- **Editing `name-vocabulary.ts` or its test.** The floor lives here because the constant does; the
  arity assertion stays where `TASK-140702` put it.

## Tests

`web-client/src/profile/name-suggestion.test.ts`, `describe("suggestName")`

| Test | Proves |
| --- | --- |
| `holds the vocabulary at the floor` | `NAME_VOCABULARY`'s list lengths multiply to at least `SUGGESTION_SPACE_FLOOR`, and that constant is `1_000_000` — the whole of `ADR-0119` §4's rarity property |
| `draws one entry from each list, in order` | two different scripted sources produce two different exact strings, each the join of the entries their indices name |
| `clamps a source that returns one` | a source stuck at `1` returns the last entry of each list, and the result contains no `undefined` |
| `draws a name the server's canonical form accepts` | three scripted draws each satisfy every rule `canonicalDisplayNameOrNull` applies — so a suggestion can be refused with `409` and never with `400` |

## Acceptance criteria

- [ ] `suggestName > holds the vocabulary at the floor` passes, and deleting one entry from
      `name-vocabulary.ts` makes it — and only it — fail: `Tests  1 failed | 3 passed (4)`, with the
      vocabulary restored byte-for-byte afterwards
- [ ] `draws one entry from each list, in order` passes for **two** different scripted sources
- [ ] `clamps a source that returns one` passes, and removing `Math.min(list.length - 1, …)` makes
      it — and only it — fail: `Tests  1 failed | 3 passed (4)`, file restored byte-for-byte
- [ ] `draws a name the server's canonical form accepts` passes for three scripted draws
- [ ] `npx vitest run src/profile/name-suggestion.test.ts` reports `Tests  4 passed (4)`
- [ ] `name-suggestion.ts` has exactly one `import` and contains none of `Math.random`,
      `localStorage`, `sessionStorage`, `window.`, `document.` — comments included
- [ ] `export const SUGGESTION_SPACE_FLOOR = 1_000_000;` appears verbatim
- [ ] `cd web-client && npm run check` exits 0
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
