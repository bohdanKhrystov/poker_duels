---
schema: 2
id: TASK-041507
title: The lobby fills the offer slot, and either control answers it
type: task
status: backlog
parent: STORY-0415
module: web-client
estimate: S
tier: sonnet
review: standard
files_touched: 4
atomic:
  - web-client's npm run check — App.test.tsx's wholesale vi.mock("./main", …) returns a fixed object, so 25 of its 37 merged tests throw `No "offerSettledHere" export is defined on the "./main" mock` the moment Lobby.tsx imports it
  - tsc --noEmit — Lobby.tsx's two new imports from "../main" do not resolve until main.tsx exports them, so the seam and its consumer cannot be separate commits
labels: [client, account, ui, wiring]
depends_on: [TASK-041505, TASK-041506]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test -- src/lobby/Lobby.test.tsx 2>&1 | grep -qE 'Test Files +1 passed \(1\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- src/lobby/Lobby.test.tsx 2>&1 | grep -qE 'Tests +55 passed \(55\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- src/App.test.tsx 2>&1 | grep -qE 'Tests +37 passed \(37\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'offers an account after a win, and after nothing else'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'withholds the offer from a browser that answered, and from one holding a credential'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'answers from either control, and only Not now takes the offer off the screen'
  - test "$(grep -oF 'offerAccount' web-client/src/lobby/Lobby.tsx | wc -l | tr -d ' ')" = 2
  - test "$(grep -oF 'settleOfferHere' web-client/src/lobby/Lobby.tsx | wc -l | tr -d ' ')" = 3
  - test "$(grep -oF 'localStorage' web-client/src/lobby/Lobby.tsx | wc -l | tr -d ' ')" = 0
  - test "$(grep -oF 'pd.' web-client/src/lobby/Lobby.tsx | wc -l | tr -d ' ')" = 0
  - cd web-client && npm run check
---

## Goal

A player who wins a duel in a browser holding no credential and having answered nothing sees the
offer on the result screen; either control answers it, and only *Not now* takes it off the screen.

## Files

Four files, and the fourth is forced — see `atomic:` and `## Proof` step 6.

| File | Action | Why it cannot be fewer |
| --- | --- | --- |
| `web-client/src/main.tsx` | modify | `ADR-0086` §2 puts the reach for the global `localStorage` in this one file, so the offer's two calls can be bound nowhere else |
| `web-client/src/lobby/Lobby.tsx` | modify | the slot `TASK-041504` added is filled here; this is the ticket |
| `web-client/src/lobby/Lobby.test.tsx` | modify | its three tests, and `vi.mock` is hoisted and **file-scoped**, so the seam can be swapped in no other file |
| `web-client/src/App.test.tsx` | modify | `npm run check` — its `vi.mock("./main", …)` replaces the module wholesale for all 37 of its tests, and **25 of them throw** unless the factory gains both bindings in the same commit. Measured, quoted in `## Proof` step 6 |

Read, and do not edit:

- [`ADR-0085`](../../docs/adr/ADR-0085-not-again-is-this-browser-and-an-answer-spends-the-offer.md)
  §3 — the case table this ticket's first two tests are written from, row by row.
- [`ADR-0086`](../../docs/adr/ADR-0086-the-offers-answer-is-one-key-owned-beside-the-predicate-it-feeds.md)
  §2 and §6 — `main.tsx` injects, no component reaches for the global, and the accept-side write runs
  from the anchor's click handler rather than on the account screen's load.
- `web-client/src/result/account-offer.ts` — `offerAccount(input)` and `OfferInput`'s three terms.
  Call it; do not restate the rule.
- `web-client/src/result/outcome-text.ts` — `verdictOf(outcome, mySeat)`, the merged reader of the
  server-sent win. `DuelResult` already calls it; this is the second and last caller.
- `web-client/src/lobby/Lobby.test.tsx` lines 25–47 — the merged `vi.mock("../main", …)` this ticket
  extends, and the comment saying why it is a partial mock over `importOriginal`.

## Scope

- **`main.tsx` gains a storage binding and two exported calls**, beside the merged `signedIn` and
  using the same `localStorage ?? nullStorage` fallback for the same stated reason:

  ```ts
  const offerStorage: Storage = localStorage ?? nullStorage;

  export function offerSettledHere(): boolean {
    return readOfferSettled(offerStorage);
  }

  export function settleOfferHere(): void {
    markOfferSettled(offerStorage);
  }
  ```

  Plain functions rather than a context and a provider: `Lobby.tsx` already imports `useHistory`,
  `useLadder` and `useSignedIn` from `../main`, both test files that render `Lobby` already mock that
  module, and a fourth provider in the tree would buy nothing either of them needs.
- **`Lobby.tsx` holds the answer in state, read once per mount:**

  ```tsx
  const [offerSettled, setOfferSettled] = useState(offerSettledHere);
  ```

  passed as the initialiser, not called — so it runs once per mount and a dismissal takes the offer
  off the screen without waiting for a reload.
- **The slot is filled from `offerAccount`, called exactly once:**

  ```tsx
  offer={
    offerAccount({
      verdict: verdictOf(state.outcome, state.mySeat),
      signedIn,
      settled: offerSettled,
    }) ? (
      <AccountOffer
        onAccept={settleOfferHere}
        onDismiss={() => {
          settleOfferHere();
          setOfferSettled(true);
        }}
      />
    ) : undefined
  }
  ```

  placed after `rematch={…}` and before `onLeave={forgetRoom}`.
- **The two handlers differ on purpose, and the difference is `ADR-0086` §6.** *Not now* settles it
  **and** hides it, because nothing else will. Taking it settles it and stops there, because the page
  load is what replaces the screen — and `ADR-0086` §Consequences names the modified-click case where
  it does not, calling the resulting disagreement correct.
- **`Lobby.test.tsx`'s merged `vi.mock("../main", …)` gains three overrides** — `useSignedIn`,
  `offerSettledHere` and `settleOfferHere` — driven by one `vi.hoisted` object reset in `beforeEach`:

  ```tsx
  const offerWiring = vi.hoisted(() => ({
    signedIn: false,
    settled: false,
    settle: vi.fn(),
  }));
  ```

  A boolean and a spy, deliberately, and not a real `Storage`: what storage does with the answer is
  `account-offer-settled.test.ts`'s, and what a whole browser does with it across two boots is
  `TASK-041508`'s. This file's subject is what `Lobby` asks the seam and what it does with the reply.
- **`App.test.tsx`'s mock factory gains the two bindings**, and nothing else in that file changes:
  `offerSettledHere: () => false` and `settleOfferHere: vi.fn()`. No test in that file reaches a
  finished duel, so both stand in as a browser that has answered nothing.

## Out of scope

- **Any storage literal in `Lobby.tsx`.** Two `verify:` lines pin `localStorage` and `pd.` at zero.
  The key belongs to `account-offer-settled.ts` alone and the merged one-module gate says so.
- **Restating the predicate.** No `verdict === "win"` and no `!signedIn` written here; `offerAccount`
  decides, and a `verify:` line pins it at exactly two occurrences — the import and the one call.
- **Clearing the answer, or offering a way back.** `ADR-0086` §4 exports none.
- **A second `verdictOf` call, or a second `AccountOffer`.** `DuelResult` calls `verdictOf` for its
  own heading; this is the only other caller, and the offer is constructed in one branch.
- **Anything across a page load, a second boot or a real `Storage`.** `TASK-041508`.
- **Touching `ArcWiring` or the two `e2e/` files that build one.** They compile unchanged against
  this diff — measured — and adding the offer to that harness is `TASK-041508`'s.

## Tests

`web-client/src/lobby/Lobby.test.tsx`, appended inside the existing `describe("the lobby")`.
**52 merged tests become 55.** A local helper renders a finished duel this client sat seat 0 of:

```tsx
function renderFinishedDuel(winner: number | null): void { … }
```

| Test | Proves |
| --- | --- |
| `offers an account after a win, and after nothing else` | Three duels over the same seat — `winner: 0` shows the offer, `winner: 1` and `winner: null` do not — with the result panel asserted present in all three, so the absence is a withheld offer and not an empty screen. `STORY-0415`'s *"appears after a won duel and not after a lost or drawn one — all three asserted"*, and the reason it is three renders and not one: a single case cannot tell a decision from a constant. **Then one more assertion, after the loop: the seam's write was called zero times.** `ADR-0085` §2's *"not the prompt merely having been rendered"*, which is the only thing standing between this client and the *being shown spends it* rule that ADR rejects |
| `withholds the offer from a browser that answered, and from one holding a credential` | Two renders, each a **one-field** delta from the passing case above: `settled: true` alone, then `signedIn: true` alone. `ADR-0085` §3's second and last rows. Without this test both terms could be hard-coded and everything else here would still pass |
| `answers from either control, and only Not now takes the offer off the screen` | One render, both controls, in order. Clicking the accept link calls the seam's write **once**, leaves the offer on screen and leaves `window.location.hash` at `""` — no in-page navigation. Then clicking *Not now* calls it a **second** time, removes the offer, and leaves *Victory* standing. The two halves guard **opposite directions**, and the mutations in `## Proof` steps 3 and 4 redden one each with the other green |

**No `try` anywhere in the added code, and no `expect()` inside one** — a failing assertion is itself
a throw, and a `try` around one turns a red test green (`TASK-041409`).

## Acceptance criteria

- [ ] `cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'offers an account after a win, and after nothing else'`
      — passes, over all three outcomes, and asserting the seam's write was called **zero** times
- [ ] `cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'withholds the offer from a browser that answered, and from one holding a credential'`
      — passes, and each case differs from the offered case in exactly one field
- [ ] `cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'answers from either control, and only Not now takes the offer off the screen'`
      — passes, asserting the write **twice** and the two different screen outcomes
- [ ] `cd web-client && NO_COLOR=1 npm run --silent test -- src/lobby/Lobby.test.tsx 2>&1 | grep -qE 'Tests +55 passed \(55\)'`
      and `… | grep -qE 'Test Files +1 passed \(1\)'` — **exactly fifty-five**: the 52 merged plus
      these three. This is how the 52 are pinned — **by a count, not by name**. Both lines, because a
      collection error prints a *passing* `Tests` count with no failure line at all
- [ ] `cd web-client && NO_COLOR=1 npm run --silent test -- src/App.test.tsx 2>&1 | grep -qE 'Tests +37 passed \(37\)'`
      — all 37 still pass. This is the gate the fourth file exists for, and 25 of them fail without it
- [ ] `test "$(grep -oF 'offerAccount' web-client/src/lobby/Lobby.tsx | wc -l | tr -d ' ')" = 2`
      — the import and one call. A second call is a second place able to get the trigger wrong
- [ ] `test "$(grep -oF 'settleOfferHere' web-client/src/lobby/Lobby.tsx | wc -l | tr -d ' ')" = 3`
      — the import and **both** handlers. Two would mean one control answers nothing
- [ ] `test "$(grep -oF 'localStorage' web-client/src/lobby/Lobby.tsx | wc -l | tr -d ' ')" = 0`
      and `test "$(grep -oF 'pd.' web-client/src/lobby/Lobby.tsx | wc -l | tr -d ' ')" = 0`
      — the component reaches for no global and names no key. **Both read the whole file, comments
      included**
- [ ] `cd web-client && npm run check` exits 0. With `TASK-041505` and `TASK-041506` merged the suite
      reads **832 passed (832)** over **107** files
- [ ] Every merged test in `Lobby.test.tsx` and `App.test.tsx` passes unchanged — this diff appends
      three tests to the first and two bindings to the second's mock factory. No assertion in either
      file moves, and none is weakened
- [ ] No file outside the four listed differs
- [ ] Every command in `verify:` exits 0

## Proof

**Every step was run in this worktree**, with `TASK-041505`, `TASK-041506` and this ticket's four
files applied to `develop` at `77c61708`: **832 / 107**, green, plus typecheck, lint and
`prettier --check`. Steps 1–5 were then re-run on the **full projection** (`TASK-041508` also
applied, **835 / 107** green), which is what the counts below name; where a step reddens a test in
`drive-arc.test.tsx` that test does not exist at this ticket's own merge point and is marked. Record
what you actually measure; never record the unmutated state as a step's "actual", and never write
*would*, *if done* or *not testable*.

1. Hard-code the first term — `verdict: "win"` in place of `verdictOf(state.outcome, state.mySeat)`.
   **`offers an account after a win, and after nothing else` reddens alone** — measured,
   `1 failed | 834 passed (835)`. This is the mutation that offers an account after a defeat, and it
   reddens nothing else in the tree. Revert.
2. Hard-code the second term — `signedIn: false`. **`withholds the offer from a browser that
   answered, and from one holding a credential` reddens alone** — measured,
   `1 failed | 834 passed (835)`. Revert.
3. Hard-code the third term — `settled: false`. **Four tests redden** — measured,
   `4 failed | 831 passed (835)`: the `withholds…` test above, `answers from either control…`
   (because with the term hard-coded the offer never leaves the screen), and **two in
   `drive-arc.test.tsx`** that do not exist here. At this ticket's own merge point the two in
   `Lobby.test.tsx` are what a coder sees. Revert.
4. **The opposite-direction pair, and it is the reason the third test asserts two things.** Both were
   measured:
   - Drop `setOfferSettled(true)` from `onDismiss`, leaving the write. **`answers from either
     control…` reddens**, plus `drive-arc.test.tsx > offers the account after a win, and never again
     once this browser has answered` — measured, `2 failed | 833 passed (835)`.
   - **Add** `setOfferSettled(true)` to `onAccept`. **`answers from either control…` reddens alone** —
     measured, `1 failed | 834 passed (835)`, and **no arc test moves**: a page load discards the tree
     either way, so the arc genuinely cannot see this one. It is the only property in this ticket that
     nothing coarser guards.
5. **Spend the offer on being shown** — call `settleOfferHere()` in the render path, beside the
   `AccountOffer` element. This is `ADR-0085` §Alternatives' *"being shown spends it"*, the rule that
   ADR rejects by name. **Three tests redden** — measured, `3 failed | 832 passed (835)`: both of
   this ticket's answering-related tests (`offers an account after a win, and after nothing else` on
   its zero-call assertion, and `answers from either control…` on its counts) plus
   `drive-arc.test.tsx > offers it again to a browser that was shown it and answered nothing`, which
   does not exist here. Revert.
6. **The fourth file, measured rather than assumed.** With `main.tsx` and `Lobby.tsx` applied and
   **`App.test.tsx` left alone**, `npm run check` fails: `Tests 25 failed | 808 passed (833)`, every
   failure in `src/App.test.tsx`, each reading

   ```
   [vitest] No "offerSettledHere" export is defined on the "./main" mock. Did you forget to return it from "vi.mock"?
   ```

   That file's `vi.mock("./main", …)` at line 41 takes no `importOriginal` — it returns a fixed
   object — so every binding `Lobby.tsx` imports from `../main` has to appear in it. Adding
   `offerSettledHere` and `settleOfferHere` to the factory takes the run back to `37 passed (37)` in
   that file. This is the same shape that blocked `TASK-041223` and then `TASK-041229`, and it is why
   this ticket is four files rather than three.
7. **`ArcWiring` is untouched by this diff.** `drive-arc.tsx`, `drive-arc.test.tsx` and
   `claimed-here-recovered-there.test.tsx` compile and pass unchanged at this ticket's merge point —
   measured, part of the green `832 / 107`. Their partial `vi.mock("../main", …)` falls through to
   the real `offerSettledHere`, which reads `nullStorage` under Vitest and answers `false`. Adding the
   offer to that harness is `TASK-041508`'s.

> **A red run names a prefix, not a set.** Gradle and Vitest both stop reporting past their first
> hard failure: a syntax error in a client source file was measured on this repo failing **twelve**
> test files at collection and printing `667 passed` with **no failure count at all**. If a step's
> output looks unrelated to the mutation, check for a collection error before concluding anything.
>
> **Expect `The 'NO_COLOR' env is ignored due to the 'FORCE_COLOR' env being set.` on stderr.** It is
> wrong, and it was checked rather than believed: `cat -v` shows plain bytes with `NO_COLOR=1` and
> escape codes without it, so every `grep -qE` in `verify:` needs it.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.

## Notes

**Why `atomic: 4` and not a split.** The two `atomic:` items are exit codes, not opinions: `tsc`
refuses `Lobby.tsx` importing a binding `main.tsx` does not export, and `npm run check` refuses the
commit that adds the import without the mock. What *would* be a split — a `main.tsx`-only ticket
adding two exports nothing imports — is a ticket with no behaviour, no test and nothing a `verify:`
line could assert, which `tasks/README.md` forbids for a different reason. Everything else here is
the natural three: a seam, its consumer, and its test.

**Measured size: 138 changed lines** — 18 in `main.tsx`, 31 in `Lobby.tsx`, 83 in `Lobby.test.tsx`
and 6 in `App.test.tsx`. That is over the `S` guideline by about a seventh, and it is recorded rather
than rounded off: the alternatives were a test-only follow-up ticket (which would ship two handlers
unasserted for one merge) or dropping the third test (which is the only guard on the one property
`TASK-041508` cannot see — see `## Proof` step 4's second half).

**`useState(offerSettledHere)` passes the function, it does not call it.** A lazy initialiser runs
once per mount. Writing `useState(offerSettledHere())` would read storage on every render — harmless
today, and exactly the kind of thing that stops being harmless quietly.

`grep -c` counts matching **lines** and exits **1** on zero matches, so every zero-expectation is
wrapped as `test "$(… | wc -l | tr -d ' ')" = 0`. `-F` keeps `.` and `(` literal.
