---
schema: 2
id: TASK-041502
title: Whether the offer is made — a win, no credential, and not already settled
type: task
status: done
parent: STORY-0415
module: web-client
estimate: S
tier: haiku
review: standard
files_touched: 2
labels: [client, account, logic]
depends_on: [TASK-041501]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test -- src/result/account-offer.test.ts 2>&1 | grep -qE 'Tests +3 passed \(3\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'is made on a win nobody has settled, and on nothing else'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'is withheld from a browser holding a credential'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'is withheld once it has been settled'
  - test "$(grep -oF 'coinBalance' web-client/src/result/account-offer.ts | wc -l | tr -d ' ')" = 0
  - test "$(grep -oF 'finalStacks' web-client/src/result/account-offer.ts | wc -l | tr -d ' ')" = 0
  - test "$(grep -oF 'length' web-client/src/result/account-offer.ts | wc -l | tr -d ' ')" = 0
  - test "$(grep -oF 'localStorage' web-client/src/result/account-offer.ts | wc -l | tr -d ' ')" = 0
  - test "$(grep -oF 'fetch' web-client/src/result/account-offer.ts | wc -l | tr -d ' ')" = 0
  - cd web-client && npm run check
---

## Goal

One function decides whether the account offer is made, from three booleans it is handed and
nothing it works out for itself.

## Files

| File | Action |
| --- | --- |
| `web-client/src/result/account-offer.ts` | create |
| `web-client/src/result/account-offer.test.ts` | create |

Read, and do not edit:

- `web-client/src/result/outcome-text.ts` — `Verdict` and `verdictOf`. `verdictOf` is the merged
  function that reads a win off `outcome.winner` and `mySeat`, and the reason this ticket takes a
  `Verdict` rather than a `DuelOutcome`: the server-sent fact has already been read once, and a
  second reader is a second place able to get it wrong.
- [`ADR-0036`](../../docs/adr/ADR-0036-an-account-is-offered-never-required.md) §Decision — the
  trigger is the first duel **won**, and an account is never required.
- `web-client/src/history/history-text.ts` — `emptyLine`, the house precedent for *one place that
  branches*, and the KDoc shape to copy.
- `web-client/src/main.tsx` lines 197–217 — `useSignedIn`, the merged boolean this function's
  second argument is fed from. Read it to know what `signedIn` means; do **not** import it here.

## Scope

- The whole module, and it is this small on purpose:

  ```ts
  import type { Verdict } from "./outcome-text";

  export interface OfferInput {
    /** The verdict `verdictOf` read off the server's `DuelOutcome` and this client's seat. */
    readonly verdict: Verdict;
    /** Whether this browser holds a session token (`useSignedIn`). */
    readonly signedIn: boolean;
    /** Whether this offer has already been made and answered. Its source is `DEC-079`'s. */
    readonly settled: boolean;
  }

  export function offerAccount(input: OfferInput): boolean {
    return input.verdict === "win" && !input.signedIn && !input.settled;
  }
  ```

- **Three terms, and each one has its own test.** `verdict === "win"` is `ADR-0036`'s trigger;
  `!signedIn` is *"It does not appear for a player who already holds a credential"*; `!settled` is
  what a later ticket feeds.
- **`settled` is an input and this function never obtains it.** Where it is read from and written to
  is `DEC-079`'s and `DEC-080`'s, both open. Five `verify:` lines gate that this file names no
  storage, no fetch, no coin balance, no stack list and no `length` — so it cannot quietly acquire
  a source, and cannot derive the trigger from a count. That last prohibition is
  `STORY-0415`'s acceptance criterion *"no test asserts it from a derived count"*, made mechanical.
  **Those five greps read the whole file, comments included** — a KDoc sentence explaining why this
  module never fetches contains the word `fetch` and breaks the line that has nothing to do with the
  comment. Say *obtains nothing* in prose; never write the needle. This is the rule
  `TASK-041229`'s `## Scope` already applies to four needles in `main.tsx`.
- KDoc on `offerAccount` saying **why** each term is there, citing `ADR-0036`. Comment why, never
  what.

## Out of scope

- **Reading `settled` from anywhere.** Blocked on `DEC-079`. A version of this ticket that reached
  for `localStorage` would be answering it.
- **Calling `verdictOf`.** The caller does that; this function takes the answer. `DuelResult`
  already calls `verdictOf` and a second call is a second reader.
- **Any React.** No hook, no component, no `.tsx`. `TASK-041503` and the wiring ticket after it.
- **Deciding what *spends* the offer** — whether being shown spends it, or only *"Not now"* does.
  `DEC-079`. This function is given the answer as a boolean and has no opinion.

## Tests

`web-client/src/result/account-offer.test.ts` — a new file, three tests, one per term. Names must
match `verify:` exactly.

| Test | Proves |
| --- | --- |
| `is made on a win nobody has settled, and on nothing else` | All four `Verdict` values against the same other two inputs: `win` → `true`, and `loss`, `draw` and `unknown` → `false`. `STORY-0415`'s *"appears after a won duel and not after a lost or drawn one — all three asserted"*, plus the fourth the type admits. **Four inputs, not one**: a single case cannot tell a decision from a constant |
| `is withheld from a browser holding a credential` | `{ verdict: "win", signedIn: true, settled: false }` → `false`. The one input that differs from the passing case is `signedIn` |
| `is withheld once it has been settled` | `{ verdict: "win", signedIn: false, settled: true }` → `false`. The one input that differs from the passing case is `settled` |

Each of the last two is a **one-field** delta from the `true` case in the first test, which is what
makes it attributable: a test whose fixture differs in two fields cannot say which one mattered.

**No `try` anywhere in this file, and no `expect()` inside one** — a failing assertion is a throw,
and a `try` around one turns a red test green (`TASK-041409`).

## Acceptance criteria

- [ ] `cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'is made on a win nobody has settled, and on nothing else'`
      — passes, asserting all four verdicts
- [ ] `cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'is withheld from a browser holding a credential'`
      — passes
- [ ] `cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'is withheld once it has been settled'`
      — passes
- [ ] `cd web-client && NO_COLOR=1 npm run --silent test -- src/result/account-offer.test.ts 2>&1 | grep -qE 'Tests +3 passed \(3\)'`
      — exactly three tests in the new file
- [ ] `test "$(grep -oF 'coinBalance' web-client/src/result/account-offer.ts | wc -l | tr -d ' ')" = 0`,
      `test "$(grep -oF 'finalStacks' web-client/src/result/account-offer.ts | wc -l | tr -d ' ')" = 0`
      and `test "$(grep -oF 'length' web-client/src/result/account-offer.ts | wc -l | tr -d ' ')" = 0`
      — the trigger is not derived from a balance, a stack or a count
- [ ] `test "$(grep -oF 'localStorage' web-client/src/result/account-offer.ts | wc -l | tr -d ' ')" = 0`
      and `test "$(grep -oF 'fetch' web-client/src/result/account-offer.ts | wc -l | tr -d ' ')" = 0`
      — the module obtains nothing; every input is handed to it
- [ ] `cd web-client && npm run check` exits 0. The suite reads **815 passed (815)** once
      `TASK-041501` and this ticket are both in, and **814 passed (814)** if this lands first
- [ ] Every pre-existing test passes unchanged — this diff adds two files and edits none
- [ ] No file outside the two listed differs
- [ ] Every command in `verify:` exits 0

## Proof

**Every step below was run in this worktree, against this ticket's own two files applied to
`develop` at `922d57fc`** — the exact state a coder taking this ticket first is in. Baseline
**811 tests / 103 files**; with these two files **814 tests / 104 files**, green, plus `npm run
check`. If `TASK-041501` has already merged, add one to every total. Each mutation was applied to
`account-offer.ts` and the **whole suite** was run, never a filtered one — a filtered run cannot
see a test in another file that the mutation also reddens.

1. Drop the `input.verdict === "win"` term, leaving `!input.signedIn && !input.settled`.
   **`is made on a win nobody has settled, and on nothing else` reddens alone** — measured,
   `1 failed | 813 passed (814)`. This is the mutation that offers an account after a defeat.
   Revert.
2. Drop the `!input.signedIn` term. **`is withheld from a browser holding a credential` reddens
   alone** — measured, `1 failed | 813 passed (814)`. This is the mutation that offers an account
   to a player who already has one. Revert.
3. Drop the `!input.settled` term. **`is withheld once it has been settled` reddens alone** —
   measured, `1 failed | 813 passed (814)`. This is the mutation that asks a second time. Revert.

Three mutations, three tests, one each: no test here is dominated by another, and none of the three
terms is unguarded.

> **Two notes on how a probe here can lie, both measured elsewhere in `STORY-0414`.** A mutation on
> a branch the fixture never drives reddens nothing and looks exactly like an unguarded property —
> which is why every term above is mutated with an input that reaches it. And a mutation confined
> to the diff budget is not the only mutation available: **the budget governs the diff, not the
> probe.** If you need to mutate `outcome-text.ts` or another file to convince yourself of
> something, do it, measure it, and revert it — that is an experiment, not a change.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.

## Notes

**Why this ticket is not blocked when the story's two open decisions are.** `DEC-079` and `DEC-080`
decide where `settled` comes from. They do not decide the predicate: under every candidate answer
the client-side test is still *this duel was won* **and** *this browser holds no credential*
**and** *one boolean the wiring supplies*. `verdict` cannot move to the server — it needs `mySeat`,
which is this client's — and `signedIn` is already a merged client fact. So the shape above holds
whichever way the two decisions land, and only the name and origin of the third input can change.

`grep -c` counts matching **lines** and exits **1** on zero matches, so every zero-expectation is
wrapped as `test "$(… | wc -l | tr -d ' ')" = 0`. `-F` keeps each needle literal.
