---
schema: 2
id: TASK-041505
title: The one key the offer's answer lives under, and the gate row that owns it
type: task
status: done
parent: STORY-0415
module: web-client
estimate: S
tier: haiku
review: standard
files_touched: 3
labels: [client, account, storage]
depends_on: [TASK-041504]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test -- src/result/account-offer-settled.test.ts 2>&1 | grep -qE 'Test Files +1 passed \(1\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- src/result/account-offer-settled.test.ts 2>&1 | grep -qE 'Tests +5 passed \(5\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- src/protocol/one-module-owns-each-storage-key.test.ts 2>&1 | grep -qE 'Tests +3 passed \(3\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'answers that nothing is settled in a browser that has never answered'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'settles the offer under the one key it names, storing the sentinel'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'tells the sentinel from every other value in the slot'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'records the same answer twice without changing what is stored'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'exports no way back to an unanswered offer'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'only the account-offer-settled module writes the offer-settled key'
  - test "$(grep -oF 'pd.accountOfferSettled' web-client/src/result/account-offer-settled.ts | wc -l | tr -d ' ')" = 1
  - test "$(grep -oF 'export ' web-client/src/result/account-offer-settled.ts | wc -l | tr -d ' ')" = 3
  - test "$(grep -oF 'removeItem' web-client/src/result/account-offer-settled.ts | wc -l | tr -d ' ')" = 0
  - test "$(grep -oF 'localStorage' web-client/src/result/account-offer-settled.ts | wc -l | tr -d ' ')" = 0
  - cd web-client && npm run check
---

## Goal

`pd.accountOfferSettled` exists, one module owns it, and the merged key-ownership gate carries the
row that says so — in the same diff, because *"add the row next time"* is what left `pd.roomCode`
unguarded.

## Files

| File | Action |
| --- | --- |
| `web-client/src/result/account-offer-settled.ts` | create |
| `web-client/src/result/account-offer-settled.test.ts` | create |
| `web-client/src/protocol/one-module-owns-each-storage-key.test.ts` | modify |

Read, and do not edit:

- [`ADR-0086`](../../docs/adr/ADR-0086-the-offers-answer-is-one-key-owned-beside-the-predicate-it-feeds.md)
  — §1 the key, §2 the module and its three exports, §3 the sentinel and the failure direction, §4
  the absent clearing function, §5 the gate row verbatim, §7 this ticket's own file list. This ADR
  was written to be the whole input for this ticket; nothing here is invented.
- [`ADR-0085`](../../docs/adr/ADR-0085-not-again-is-this-browser-and-an-answer-spends-the-offer.md)
  §2 and §4 — an answer spends the offer, nothing clears it, and it survives everything
  `localStorage` survives.
- `web-client/src/protocol/session-token.ts` — the house shape for a key module: an exported
  literal, a read that trims before testing, and a write. Copy its shape, **not** its
  `forget…` export.
- `web-client/src/protocol/session-token.test.ts` lines 11–45 — `inMemoryStorage()` and the comment
  saying why the global is not used. Copy both; this file needs its own copy, exactly as that one
  does.
- `web-client/src/account/account-text.test.ts` lines 1–40 — the `Object.keys(module).sort()`
  export-set assertion. It is the mechanism that makes `ADR-0086` §4 mechanical: a `toBe` on each
  function cannot see a fourth export appearing.

## Scope

- The whole module, and it is this small on purpose:

  ```ts
  export const ACCOUNT_OFFER_SETTLED_STORAGE_KEY = "pd.accountOfferSettled";

  /** The sentinel a settled offer stores. It carries no information — `ADR-0086` §3. */
  const SETTLED = "1";

  export function readOfferSettled(storage: Storage): boolean {
    return storage.getItem(ACCOUNT_OFFER_SETTLED_STORAGE_KEY)?.trim() === SETTLED;
  }

  export function markOfferSettled(storage: Storage): void {
    storage.setItem(ACCOUNT_OFFER_SETTLED_STORAGE_KEY, SETTLED);
  }
  ```

  Three exports, no fourth, and `SETTLED` stays module-private so the export set is exactly the
  three `ADR-0086` §2 names.
- **The key literal stays one string literal on one line.** The gate reads source text, so a
  constant assembled from parts, or split across a line break, silently empties the row's match set
  (`ADR-0086` §Consequences' last cost).
- **The gate's third row is `ADR-0086` §5, written out verbatim** and appended after
  `the scan tells two keys apart`:

  ```ts
  it("only the account-offer-settled module writes the offer-settled key", () => {
    expect(productionSourcesContaining("pd.accountOfferSettled")).toEqual([
      "account-offer-settled.ts",
    ]);
  });
  ```

  The literal is written out rather than imported. The walk skips `*.test.ts`, so the row does not
  match its own file — measured in `## Proof`, not assumed. That file's header comment names a
  different limit — *a key **assembled from constants** escapes it* — and that is about how a key is
  written, not about how the scan excludes itself. Do not conflate the two.
- **KDoc citing `ADR-0086` on the constant and both functions**, saying *why*: why the read fails
  toward *not settled*, and why there is no way back.
- **Four `verify:` greps read the whole file, comments included** — the key literal exactly once,
  `export ` exactly three times, `removeItem` and `localStorage` zero times. This bites: a KDoc
  sentence reading *"a missing **export** rather than a promise"* was measured here to push the
  `export ` count to **4** and fail the step it had nothing to do with. Say *a declaration a
  reviewer sees* in prose, and never write a needle. Same rule `TASK-041502` §Scope applies to its
  own five.

## Out of scope

- **Any clearing function**, under any name, and any argument that unsets. `ADR-0086` §4, and the
  export-set test plus the `export ` count are what enforce it.
- **Touching `sign-out.ts`.** `ADR-0086` §4 leaves `signOut` unchanged, so the signed-out
  account-holder case stays where `ADR-0085` §Consequences left it — named, not solved.
- **Reaching for the global `localStorage`.** Every function takes the `Storage`; `main.tsx` injects
  it, and that wiring is `TASK-041507`'s.
- **Reading or writing this key from anywhere else** — the predicate `account-offer.ts` takes
  `settled` as an input and five of its own merged `verify:` lines forbid it acquiring a source.
- **A row for `pd.roomCode`.** It has been unguarded since it was added and `ADR-0086` §*What this
  does not settle* names it; it is `TASK-041509`'s, filed alongside these, and not this diff.

## Tests

`web-client/src/result/account-offer-settled.test.ts` — a new file, five tests inside
`describe("the answer this browser gave the account offer")`. Names must match `verify:` exactly.

| Test | Proves |
| --- | --- |
| `answers that nothing is settled in a browser that has never answered` | A fresh `Storage` reads `false`. The failure direction `ADR-0086` §3 chose: absent is *not settled*, so the player is still told |
| `settles the offer under the one key it names, storing the sentinel` | After `markOfferSettled`, `readOfferSettled` is `true` **and** `storage.getItem("pd.accountOfferSettled")` is `"1"`. The **literal**, not the exported constant — a constant asserted against itself proves nothing, and this string is what the browser must match |
| `tells the sentinel from every other value in the slot` | Values written **by the test, not by the module**: `"1"` and `" 1 "` read `true`; `"0"`, `""`, `"   "`, `"true"`, `"11"` and `"01"` read `false`. `ADR-0086` §Alternatives' whole case against presence-only — there must exist a value that is present and still reads *not settled*, or the read cannot be told from a constant `true` |
| `records the same answer twice without changing what is stored` | Two `markOfferSettled` calls leave `readOfferSettled` `true` and `storage.length` **1**. `ADR-0085` §2 lets either control run it and does not say which ran first; the length assertion is what forbids the timestamp or counter `ADR-0086` §3 refuses |
| `exports no way back to an unanswered offer` | `Object.keys(module).sort()` is exactly `["ACCOUNT_OFFER_SETTLED_STORAGE_KEY", "markOfferSettled", "readOfferSettled"]`. This is `ADR-0086` §4 as a diff a reviewer sees rather than a promise |

`web-client/src/protocol/one-module-owns-each-storage-key.test.ts` — **2 merged tests become 3.**
The two merged ones are pinned by the count `Tests 3 passed (3)`, never by name.

**No `try` anywhere in either file, and no `expect()` inside one** — a failing assertion is itself a
throw, and a `try` around one turns a red test green (`TASK-041409`).

## Acceptance criteria

- [ ] `cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'answers that nothing is settled in a browser that has never answered'`
      — passes
- [ ] `cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'settles the offer under the one key it names, storing the sentinel'`
      — passes, asserting the literal `"pd.accountOfferSettled"` and the value `"1"`
- [ ] `cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'tells the sentinel from every other value in the slot'`
      — passes, over **eight** stored values the module did not write, in both directions
- [ ] `cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'records the same answer twice without changing what is stored'`
      — passes, asserting `storage.length` is 1
- [ ] `cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'exports no way back to an unanswered offer'`
      — passes, asserting the export set
- [ ] `cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'only the account-offer-settled module writes the offer-settled key'`
      — passes, and the expected array **names the owner** rather than being empty
- [ ] `cd web-client && NO_COLOR=1 npm run --silent test -- src/result/account-offer-settled.test.ts 2>&1 | grep -qE 'Tests +5 passed \(5\)'`
      and `… | grep -qE 'Test Files +1 passed \(1\)'` — **both**, because a collection error prints a
      passing `Tests` line with no failure count at all: a syntax error was measured on this repo
      dropping twelve files and printing `667 passed`
- [ ] `cd web-client && NO_COLOR=1 npm run --silent test -- src/protocol/one-module-owns-each-storage-key.test.ts 2>&1 | grep -qE 'Tests +3 passed \(3\)'`
      — **exactly three**: the two merged rows plus this one. A deleted merged row reads 2 and fails.
      This is how the merged rows are pinned — **by a count, not by name**
- [ ] `test "$(grep -oF 'pd.accountOfferSettled' web-client/src/result/account-offer-settled.ts | wc -l | tr -d ' ')" = 1`
      — the key is one literal, on one line, in one place
- [ ] `test "$(grep -oF 'export ' web-client/src/result/account-offer-settled.ts | wc -l | tr -d ' ')" = 3`
      — three exports, no fourth. **Comments included**: see `## Scope` for the KDoc phrasing that
      broke this exact step when it was measured
- [ ] `test "$(grep -oF 'removeItem' web-client/src/result/account-offer-settled.ts | wc -l | tr -d ' ')" = 0`
      and `test "$(grep -oF 'localStorage' web-client/src/result/account-offer-settled.ts | wc -l | tr -d ' ')" = 0`
      — no way to clear the key, and no reach for the global
- [ ] `cd web-client && npm run check` exits 0. The suite reads **828 passed (828)** over **107**
      files, up from the merged **822 / 106**
- [ ] Every merged test in `one-module-owns-each-storage-key.test.ts` passes unchanged — this diff
      **appends** one row and edits neither of the two, and no assertion is weakened
- [ ] No file outside the three listed differs
- [ ] Every command in `verify:` exits 0

## Proof

**Every step below was run in this worktree, with this ticket's three files and nothing else applied
to `develop` at `77c61708`.** Baseline measured there: **822 tests / 106 files**, green. With this
ticket: **828 / 107**, green, plus typecheck, lint and `prettier --check`. Every mutation was
applied and the **whole suite** run — a filtered run cannot see a test in another file that the
mutation also reddens. Record what you actually measure; never record the unmutated state as a
step's "actual", and never write *would*, *if done* or *not testable*.

1. Make the read default to *settled* when the slot is empty —
   `(storage.getItem(KEY) ?? SETTLED).trim() === SETTLED`. **`answers that nothing is settled in a
   browser that has never answered` reddens alone** — measured, `1 failed | 827 passed (828)`. This
   is the mutation that silently suppresses the warning `ADR-0036` says a player otherwise learns by
   losing their coins. Revert.
2. Make the read presence-only — `storage.getItem(KEY) !== null`. **`tells the sentinel from every
   other value in the slot` reddens alone** — measured, `1 failed | 827 passed (828)`. This is
   `ADR-0086` §Alternatives' rejected shape, and the step that shows the eight-value test is the one
   assertion presence-only cannot pass. Revert.
3. Have `markOfferSettled` also write `storage.setItem("pd.accountOfferSettledAt", "when")`.
   **`records the same answer twice without changing what is stored` reddens alone** — measured,
   `1 failed | 827 passed (828)`. Two things worth knowing from this step: it is the counter
   `ADR-0086` §3 forbids, and the **gate row stays green**, because the second literal sits in the
   *same file* and the scan collects file names. That is the substring property from its harmless
   direction. Revert.
4. Add `export function clearOfferSettled(storage: Storage): void { storage.removeItem(KEY); }`.
   **`exports no way back to an unanswered offer` reddens alone** — measured,
   `1 failed | 827 passed (828)`. `ADR-0086` §4 is therefore mechanical. Revert.
5. **A second production writer, run outside this ticket's three files** — the budget governs the
   diff, not the probe. Put `const PROBE = "pd.accountOfferSettled";` at the top of
   `web-client/src/lobby/Lobby.tsx`. **`only the account-offer-settled module writes the
   offer-settled key` reddens alone** — measured, `1 failed | 827 passed (828)`, with
   `AssertionError: expected [ 'Lobby.tsx', …(1) ] to deeply equal [ 'account-offer-settled.ts' ]`.
   The row catches the defect it exists for, and it catches it in a *different directory* from the
   owner. Revert `Lobby.tsx` completely.
6. Rename the module's key literal to `"pd.accountOffer"` — the short name `ADR-0086` §1 refuses.
   **Two tests redden, both about the key** — measured, `2 failed | 826 passed (828)`: `settles the
   offer under the one key it names…` on `expected null to be '1'`, and the gate row on
   `expected [] to deeply equal [ 'account-offer-settled.ts' ]`. The second half is the **presence**
   property `ADR-0086` §5 asks for: a row that matched nothing would be red, so `toEqual([])` would
   have proved nothing. Revert.

> **The `.test.ts` exclusion was measured, not assumed.** The gate row writes
> `"pd.accountOfferSettled"` verbatim and passes with the owner as the *only* match, so the scan
> does not see the test file that contains it. That is what the walk's
> `entry.name.endsWith(".test.ts")` skip does.
>
> **Expect `The 'NO_COLOR' env is ignored due to the 'FORCE_COLOR' env being set.` on stderr.** The
> warning is misleading and was checked rather than believed: `cat -v` on the summary line shows
> **plain bytes** with `NO_COLOR=1` and `^[[2m      Tests ^[[22m ^[[1m^[[32m5 passed…` **without**
> it, so `grep -qE 'Tests +5 passed \(5\)'` silently stops matching if the variable is dropped.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.

## Notes

**Why the module and the row must land together.** `ADR-0086` §*The deadline*: a key that lives in
`develop` for even one merged PR without its row is a key a second writer can be added to unnoticed,
and `pd.roomCode` is the evidence that the follow-up ticket which adds the row afterwards does not
get written. Three files, one diff.

**Measured size: 128 changed lines** — 34 in the module, 88 in its test (34 of them the
`inMemoryStorage()` fixture the house duplicates per file), 6 in the gate. Recorded so nobody is
surprised by an `S` that is a little over the guideline rather than a little under it.

`grep -c` counts matching **lines** and exits **1** on zero matches, so every zero-expectation is
wrapped as `test "$(… | wc -l | tr -d ' ')" = 0`. `-F` keeps each needle literal.
