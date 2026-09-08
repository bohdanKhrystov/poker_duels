---
schema: 2
id: TASK-140708
title: One module owns the key that says this browser skipped the ask
type: task
status: done
parent: STORY-1407
module: web-client
estimate: S
tier: haiku
review: standard
files_touched: 3
labels: [client, profile]
depends_on: [TASK-140707]
verify:
  - cd web-client && npm ci
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npm run --silent check
  - cd web-client && grep -qF 'export const NAME_ASK_SKIPPED_STORAGE_KEY = "pd.nameAskSkipped";' src/profile/name-ask-skipped.ts
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/profile/name-ask-skipped.test.ts > "${TMPDIR:-/tmp}/sk-after.txt" 2>&1; grep -qF 'Tests  5 passed (5)' "${TMPDIR:-/tmp}/sk-after.txt"
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/protocol/one-module-owns-each-storage-key.test.ts > "${TMPDIR:-/tmp}/ok-after.txt" 2>&1; grep -qF 'Tests  4 passed (4)' "${TMPDIR:-/tmp}/ok-after.txt"
  - cd web-client && cp src/profile/name-text.ts "${TMPDIR:-/tmp}/nt.fixed.ts" && printf '\n// pd.nameAskSkipped\n' >> src/profile/name-text.ts && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/protocol/one-module-owns-each-storage-key.test.ts > "${TMPDIR:-/tmp}/k1.txt" 2>&1; cp "${TMPDIR:-/tmp}/nt.fixed.ts" src/profile/name-text.ts; cmp -s "${TMPDIR:-/tmp}/nt.fixed.ts" src/profile/name-text.ts && grep -qF 'Tests  1 failed | 3 passed (4)' "${TMPDIR:-/tmp}/k1.txt" && grep -qF 'only the name-ask-skipped module writes the skipped key' "${TMPDIR:-/tmp}/k1.txt"
  - python3 .github/scripts/lint_tickets.py
---

## Goal

`web-client/src/profile/name-ask-skipped.ts` owns `pd.nameAskSkipped` — the one browser key that
records that this browser answered `ADR-0119` §1's ask by skipping — with the read and the write
beside it and no way back. A fifth row in
`web-client/src/protocol/one-module-owns-each-storage-key.test.ts` holds that no second production
file ever spells the literal.

## Files

| File | Action |
| --- | --- |
| `web-client/src/profile/name-ask-skipped.ts` | create |
| `web-client/src/profile/name-ask-skipped.test.ts` | create |
| `web-client/src/protocol/one-module-owns-each-storage-key.test.ts` | modify |

Read [`ADR-0119`](../../docs/adr/ADR-0119-the-name-is-asked-at-the-first-press-and-skipping-plays.md)
§5 and `web-client/src/result/account-offer-settled.ts` with its test — this module is that module's
sibling, in shape and in failure direction, and `ADR-0119` §5 says so: *"It takes `ADR-0086`'s
shape: one key, owned beside the predicate it feeds, with a row in
`one-module-owns-each-storage-key.test.ts`."* **Nothing outside the table above is changed.**

## Scope

- **Three exports and no fourth:**

  ```ts
  export const NAME_ASK_SKIPPED_STORAGE_KEY = "pd.nameAskSkipped";
  export function readNameAskSkipped(storage: Storage): boolean;
  export function markNameAskSkipped(storage: Storage): void;
  ```

  The sentinel is the string `"1"`, module-private, and carries no information — the key's presence
  is the whole fact. **No export clears it**: `ADR-0119` §5 says *"nothing in the product clears
  it"*, and an un-skip would have to add an export, which is a diff a reviewer sees.
- **`Storage` is a parameter, never a global.** Node 24+ defines an inert `localStorage` that
  shadows jsdom's under Vitest (`DEC-032`), so a module reaching for the global is a module whose
  tests do not test the browser. `main.tsx` binds the real one — `TASK-140713` — and the test hands
  in an in-memory `Storage`, copied from `account-offer-settled.test.ts`.
- **The read is exact and trimmed**: `storage.getItem(key)?.trim() === SENTINEL`. Absent, blank and
  every other value read as `false`, so a browser that cannot read the answer **asks again** — the
  safe direction, because being asked spends nothing and nothing is written unless the player
  answers (`ADR-0119` §5).
- **The fifth row in the ownership gate** reads

  ```ts
  it("only the name-ask-skipped module writes the skipped key", () => {
    expect(productionSourcesContaining("pd.nameAskSkipped")).toEqual([
      "name-ask-skipped.ts",
    ]);
  });
  ```

  and a mutation in `verify:` writes the literal into `name-text.ts` to prove the row bites: that
  gate scans **source text**, so a comment is enough to trip it, which is exactly the sensitivity it
  is built for.

## Out of scope

- **Reading or writing the key from a component.** `Lobby` reaches it through `main.tsx`'s two
  bindings (`TASK-140713`), the shape `offerSettledHere` / `settleOfferHere` already ship in.
- **The predicate that consumes it** — `TASK-140709`.
- **Any second key**, a server column, or anything on `GET /api/me`. `ADR-0119` §5: the skipped bit
  is a fact about this browser and does not travel.

## Tests

`web-client/src/profile/name-ask-skipped.test.ts`,
`describe("the answer this browser gave the name ask")`

| Test | Proves |
| --- | --- |
| `answers that nothing was skipped in a browser that has never answered` | the absent key reads `false` |
| `records the skip under the one key it names, storing the sentinel` | `markNameAskSkipped` writes `"1"` at `pd.nameAskSkipped` and the read agrees |
| `tells the sentinel from every other value in the slot` | `"1"` and `" 1 "` read `true`; `"0"`, `""`, `"   "`, `"true"`, `"11"`, `"01"` read `false` |
| `records the same answer twice without changing what is stored` | idempotent, and `storage.length` stays 1 |
| `exports no way back to an unanswered ask` | the module's exports are exactly the three named above |

`web-client/src/protocol/one-module-owns-each-storage-key.test.ts`

| Test | Proves |
| --- | --- |
| `only the name-ask-skipped module writes the skipped key` | `pd.nameAskSkipped` appears in exactly one production source file |

## Acceptance criteria

- [ ] All five tests in `name-ask-skipped.test.ts` pass:
      `npx vitest run src/profile/name-ask-skipped.test.ts` reports `Tests  5 passed (5)`
- [ ] `npx vitest run src/protocol/one-module-owns-each-storage-key.test.ts` reports
      `Tests  4 passed (4)`, and the three merged rows are byte-unchanged
- [ ] Writing `pd.nameAskSkipped` into `name-text.ts` makes the new row — and only it — fail:
      `Tests  1 failed | 3 passed (4)`, and `name-text.ts` is restored byte-for-byte
- [ ] `name-ask-skipped.ts` exports exactly `NAME_ASK_SKIPPED_STORAGE_KEY`, `readNameAskSkipped` and
      `markNameAskSkipped`, and reaches for no global `Storage`
- [ ] `cd web-client && npm run check` exits 0
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
