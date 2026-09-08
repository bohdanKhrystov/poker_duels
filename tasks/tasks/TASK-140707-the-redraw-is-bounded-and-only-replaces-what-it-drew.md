---
schema: 2
id: TASK-140707
title: The redraw is bounded, and a broken source cannot spin
type: task
status: backlog
parent: STORY-1407
module: web-client
estimate: S
tier: sonnet
review: standard
files_touched: 2
labels: [client, profile]
depends_on: [TASK-140706]
verify:
  - cd web-client && npm ci
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npm run --silent check
  - cd web-client && grep -qF 'const MAX_DRAWS = 4;' src/profile/name-suggestion.ts
  - cd web-client && grep -qF 'replacing?: string' src/profile/name-suggestion.ts
  - cd web-client && test "$(grep -c '^import' src/profile/name-suggestion.ts || true)" = "1"
  - cd web-client && test "$(grep -cE 'Math\.random|localStorage|sessionStorage|window\.|document\.' src/profile/name-suggestion.ts || true)" = "0"
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/profile/name-suggestion.test.ts > "${TMPDIR:-/tmp}/ns-after.txt" 2>&1; grep -qF 'Tests  6 passed (6)' "${TMPDIR:-/tmp}/ns-after.txt"
  - cd web-client && cp src/profile/name-suggestion.ts "${TMPDIR:-/tmp}/ns.fixed.ts" && perl -0pi -e 's{const MAX_DRAWS = 4;}{const MAX_DRAWS = 40;}' src/profile/name-suggestion.ts && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/profile/name-suggestion.test.ts > "${TMPDIR:-/tmp}/r1.txt" 2>&1; cp "${TMPDIR:-/tmp}/ns.fixed.ts" src/profile/name-suggestion.ts; cmp -s "${TMPDIR:-/tmp}/ns.fixed.ts" src/profile/name-suggestion.ts && grep -qF 'Tests  1 failed | 5 passed (6)' "${TMPDIR:-/tmp}/r1.txt" && grep -qF 'stops after four draws when every draw repeats' "${TMPDIR:-/tmp}/r1.txt"
  - cd web-client && cp src/profile/name-suggestion.ts "${TMPDIR:-/tmp}/ns.fixed.ts" && perl -0pi -e 's{const MAX_DRAWS = 4;}{const MAX_DRAWS = 1;}' src/profile/name-suggestion.ts && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/profile/name-suggestion.test.ts > "${TMPDIR:-/tmp}/r2.txt" 2>&1; cp "${TMPDIR:-/tmp}/ns.fixed.ts" src/profile/name-suggestion.ts; cmp -s "${TMPDIR:-/tmp}/ns.fixed.ts" src/profile/name-suggestion.ts && grep -qF 'Tests  2 failed | 4 passed (6)' "${TMPDIR:-/tmp}/r2.txt" && grep -qF 'draws again when the draw is the string being replaced' "${TMPDIR:-/tmp}/r2.txt"
  - python3 .github/scripts/lint_tickets.py
---

## Goal

`suggestName` takes `ADR-0137` §2's second parameter: `replacing`, the string being replaced when
there is one. A draw equal to it is drawn again — **at most four draws in total**, then the last is
returned. The bound is the whole point: a source stuck at one value must not spin, and at a
million combinations a genuine repeat is about one in a million.

## Files

| File | Action |
| --- | --- |
| `web-client/src/profile/name-suggestion.ts` | modify |
| `web-client/src/profile/name-suggestion.test.ts` | modify |

Read [`ADR-0137`](../../docs/adr/ADR-0137-a-name-suggestion-is-drawn-in-the-browser.md) §§2, 5.
**Nothing outside the table above is changed** — no component, no vocabulary, no call site.

## Scope

- **The signature becomes `ADR-0137` §2's, whole:**

  ```ts
  export function suggestName(random: () => number, replacing?: string): string;
  ```

  Optional, so `TASK-140706`'s four tests and every one-argument call still compile and still pass
  unchanged.
- **The bound is a named constant, `const MAX_DRAWS = 4;`**, and it counts **whole draws** — one
  draw is one entry from each list — not entries and not retries. A `verify:` gate holds the literal
  so the two mutations below can move it.
- **The loop redraws only while the drawn string equals `replacing`.** With `replacing` absent the
  loop never runs and the function is exactly what `TASK-140706` shipped: one draw, returned.
- **The last draw is returned even when it repeats.** *"Bounded, because a `random` stuck at one
  value must not spin"* — the function has no failure mode and no exception; the worst case is the
  player seeing the same suggestion twice, which is a string in a field they can type over.
- **Two tests, and both count the calls to `random`.** The returned string alone cannot tell one
  draw from four when the source is stuck — only the call count can, which is the same reason a
  retry test needs a request count.
- **Two mutations, and the second is asymmetric on purpose.** Raising the bound to 40 reddens the
  four-draw test alone. Lowering it to 1 reddens **both** new tests — the redraw stops happening at
  all — so its gate names the test it must see fail rather than resting on a total.

## Out of scope

- **Where `replacing` comes from.** The surface passes the last string the generator put in the
  field, on `conflict` alone, and that is `TASK-140712`'s.
- **A set of strings to avoid, a history of names already tried, or any store of past
  suggestions.** `ADR-0137` §8 refuses all three: `replacing` is one string and nothing remembers it.
- **A reroll on any outcome other than `conflict`** — `ADR-0137` §5, and `TASK-140712`'s ground.

## Tests

`web-client/src/profile/name-suggestion.test.ts`, `describe("suggestName")` — the four from
`TASK-140706`, unchanged, plus:

| Test | Proves |
| --- | --- |
| `draws again when the draw is the string being replaced` | a scripted source whose first tuple joins to `replacing` and whose second does not returns the **second**, and calls the source exactly twice per list |
| `stops after four draws when every draw repeats` | a source stuck at one value, with `replacing` equal to what it draws, returns that string and calls the source exactly **four** times per list — bounded, never spinning |

## Acceptance criteria

- [ ] `suggestName > draws again when the draw is the string being replaced` passes and asserts the
      call count as well as the string
- [ ] `stops after four draws when every draw repeats` passes and asserts a call count of exactly
      four draws
- [ ] `npx vitest run src/profile/name-suggestion.test.ts` reports `Tests  6 passed (6)`, and
      `TASK-140706`'s four tests are byte-unchanged
- [ ] Changing `MAX_DRAWS` to 40 fails `stops after four draws when every draw repeats` and nothing
      else: `Tests  1 failed | 5 passed (6)`, file restored byte-for-byte
- [ ] Changing `MAX_DRAWS` to 1 fails both new tests: `Tests  2 failed | 4 passed (6)`, naming
      `draws again when the draw is the string being replaced`, file restored byte-for-byte
- [ ] `suggestName(random)` with one argument behaves exactly as it did before this ticket
- [ ] `cd web-client && npm run check` exits 0
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
