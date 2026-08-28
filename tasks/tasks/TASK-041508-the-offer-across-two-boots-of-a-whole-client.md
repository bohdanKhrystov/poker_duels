---
schema: 2
id: TASK-041508
title: The offer across two boots of a whole client, answered and unanswered
type: task
status: done
parent: STORY-0415
module: web-client
estimate: S
tier: sonnet
review: standard
files_touched: 3
labels: [client, account, e2e]
depends_on: [TASK-041507]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test -- src/e2e/drive-arc.test.tsx 2>&1 | grep -qE 'Test Files +1 passed \(1\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- src/e2e/drive-arc.test.tsx 2>&1 | grep -qE 'Tests +7 passed \(7\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- src/e2e/claimed-here-recovered-there.test.tsx 2>&1 | grep -qE 'Tests +8 passed \(8\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'offers the account after a win, and never again once this browser has answered'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'offers it again to a browser that was shown it and answered nothing'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'spends the offer on the way to the account screen too'
  - test "$(grep -oF 'readOfferSettled' web-client/src/e2e/drive-arc.tsx | wc -l | tr -d ' ')" = 2
  - test "$(grep -oF 'markOfferSettled' web-client/src/e2e/drive-arc.tsx | wc -l | tr -d ' ')" = 2
  - test "$(grep -oF 'pd.' web-client/src/e2e/drive-arc.test.tsx | wc -l | tr -d ' ')" = 0
  - cd web-client && npm run check
---

## Goal

A real client boots over a real `Storage`, wins a duel and is offered an account: answer it, either
way, and a later boot of that browser never asks again; leave it unanswered, and the next win asks.

## Files

| File | Action |
| --- | --- |
| `web-client/src/e2e/drive-arc.tsx` | modify |
| `web-client/src/e2e/drive-arc.test.tsx` | modify |
| `web-client/src/e2e/claimed-here-recovered-there.test.tsx` | modify |

The third file is in the budget because this diff invalidates it: it builds an `ArcWiring` literal,
so two new required fields stop it typechecking. `tsc --noEmit` names it — see `## Proof` step 4. It
gains those two fields and the two matching mock overrides, and **not one assertion moves**.

Read, and do not edit:

- [`ADR-0085`](../../docs/adr/ADR-0085-not-again-is-this-browser-and-an-answer-spends-the-offer.md)
  §3 and §4 — the case table, and what the dismissal survives. Rows 1, 2, 3 and 7 are the three tests
  below.
- [`ADR-0086`](../../docs/adr/ADR-0086-the-offers-answer-is-one-key-owned-beside-the-predicate-it-feeds.md)
  §2 and §6 — the storage is injected, and taking the offer settles it from the anchor's click
  handler.
- `web-client/src/e2e/drive-arc.tsx` — `ArcWiring`'s doc comment and `bootClient`, in particular the
  two lines that bind `wiring.history` and `wiring.signedIn` to `options.storage` on every boot. The
  offer's two calls go beside them, bound the same way.
- `web-client/src/e2e/drive-arc.test.tsx` lines 10–32 — the hoisted `wiring` object and the partial
  `vi.mock("../main", …)` this ticket extends by two fields and two overrides.
- `web-client/src/e2e/drive-duel.tsx` — `inMemoryStorage()`, already imported by both test files.
  One call to it is one browser; two `bootClient` calls over that same value are two boots of it,
  which is what every test below turns on.

## Scope

- **`ArcWiring` gains two required fields**, and `bootClient` binds them to `options.storage`
  alongside the two it already binds:

  ```ts
  offerSettled: () => boolean;
  settleOffer: () => void;
  ```

  ```ts
  wiring.offerSettled = () => readOfferSettled(storage);
  wiring.settleOffer = () => markOfferSettled(storage);
  ```

  **Required, not optional.** The harness's whole subject is what a second boot over the same
  `Storage` sees; a field a boot could forget to bind is a harness that can silently answer *not
  settled* forever, which is the one failure this file exists to catch.
- **The real persistence module, not a stand-in.** `drive-arc.tsx` imports `readOfferSettled` and
  `markOfferSettled` from `../result/account-offer-settled`. Two `verify:` lines pin each at exactly
  two occurrences — the import and the binding. `Lobby.test.tsx` uses a boolean and a spy because its
  subject is the component; this file's subject is the round trip, so it must not.
- **`drive-arc.test.tsx`'s mock gains two overrides**, `offerSettledHere: () => wiring.offerSettled()`
  and `settleOfferHere: () => wiring.settleOffer()`, and its hoisted literal gains the two fields with
  inert defaults (`() => false` and `() => {}`) that `bootClient` overwrites on every boot.
- **Two local helpers**, both small:

  ```ts
  function winFrames(): readonly string[]   // RoomJoined seat 0, then DuelFinished winner 0
  function bootAndWin(storage: Storage, server: AccountServer): HTMLElement
  ```

  `winFrames` builds each frame as a typed `ServerMessage` and `JSON.stringify`s it, exactly as the
  merged `welcomeFrame` in the same file does — so the frames are the ones the real server would
  send, not hand-written strings.
- **`claimed-here-recovered-there.test.tsx` gains four lines and nothing else**: the two `ArcWiring`
  fields with the same inert defaults, and the two mock overrides. Its eight merged tests keep every
  assertion; a `verify:` line pins the file at exactly eight.

## Out of scope

- **A real page load.** Nothing in jsdom reloads a document, and this harness's own KDoc already says
  what a reload is here: calling `bootClient` again. The accept test asserts the *storage* the next
  boot reads, which is the durable half and the only half that matters.
- **Any storage literal in the test file.** A `verify:` line pins `pd.` at zero there. The key belongs
  to `account-offer-settled.ts`, whose ownership the merged one-module gate asserts.
- **Asserting a lost or drawn duel.** `Lobby.test.tsx`'s `offers an account after a win, and after
  nothing else` covers all three verdicts against the same seat, and `account-offer.test.ts` covers
  all four the type admits. Repeating it here buys a slower copy.
- **A second browser.** `ADR-0085` §3's new-browser row follows from the key being per-`Storage`,
  which `account-offer-settled.test.ts` establishes directly and cheaply. The three tests below spend
  their length on the row nothing else can reach: the same browser across two boots.
- **The result screen's contents.** `DuelResult.test.tsx` and `AccountOffer.test.tsx` own those; the
  tests below query the two `region`s by name and nothing inside them.
- **`drive-duel.tsx`, `whole-duel.test.tsx` and `duel-secrecy.test.tsx`.** None builds an `ArcWiring`
  and none is touched — measured, `## Proof` step 4.

## Tests

`web-client/src/e2e/drive-arc.test.tsx` — **4 merged tests become 7**, appended before the merged
`the account screen is reachable from the first screen`.

| Test | Proves |
| --- | --- |
| `offers the account after a win, and never again once this browser has answered` | Boot over a fresh `Storage`, win, see the offer, press *Not now*: the offer leaves the screen. Then **boot again over that same `Storage`** and win again: the result panel is there and the offer is not. `ADR-0085` §3 rows 1 and 2, and §4's *"survives a reload"*, through a real `Storage` and the real key module |
| `offers it again to a browser that was shown it and answered nothing` | Boot, win, **see the offer and press neither control**, boot again over that same `Storage`, win again: the offer is there again. `ADR-0085` §3's fourth row and `STORY-0415`'s fourth acceptance criterion, which nothing else in the tree asserts across a boot. It is also the **control** on the test above: without it, an offer that never appeared at all would satisfy that one's second half |
| `spends the offer on the way to the account screen too` | Boot, win, click the **accept link**, boot again over the same `Storage`, win again: no offer. `ADR-0085` §2's *"Both controls are answers"* and `ADR-0086` §6's click handler, end to end. Nothing shorter than a second boot can see this, because the accept path writes and then leaves |

`web-client/src/e2e/claimed-here-recovered-there.test.tsx` — **8 tests, unchanged**, pinned by count.

**No `try` anywhere in the added code, and no `expect()` inside one** — a failing assertion is itself
a throw, and a `try` around one turns a red test green (`TASK-041409`).

## Acceptance criteria

- [ ] `cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'offers the account after a win, and never again once this browser has answered'`
      — passes, asserting the offer present in boot one, gone after *Not now*, and gone in boot two
- [ ] `cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'offers it again to a browser that was shown it and answered nothing'`
      — passes, with **no control pressed** between the two boots
- [ ] `cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'spends the offer on the way to the account screen too'`
      — passes, driven by a click on the accept **link**
- [ ] `cd web-client && NO_COLOR=1 npm run --silent test -- src/e2e/drive-arc.test.tsx 2>&1 | grep -qE 'Tests +7 passed \(7\)'`
      and `… | grep -qE 'Test Files +1 passed \(1\)'` — **exactly seven in exactly one file**: the
      four merged plus these three. Both lines, because a collection error prints a *passing* `Tests`
      count with no failure line at all
- [ ] `cd web-client && NO_COLOR=1 npm run --silent test -- src/e2e/claimed-here-recovered-there.test.tsx 2>&1 | grep -qE 'Tests +8 passed \(8\)'`
      — the other `ArcWiring` builder is still eight tests. This is how its assertions are pinned —
      **by a count, not by name**
- [ ] `test "$(grep -oF 'readOfferSettled' web-client/src/e2e/drive-arc.tsx | wc -l | tr -d ' ')" = 2`
      and `test "$(grep -oF 'markOfferSettled' web-client/src/e2e/drive-arc.tsx | wc -l | tr -d ' ')" = 2`
      — the real module, imported once and bound once each. A harness that stubbed either would make
      every test below vacuous
- [ ] `test "$(grep -oF 'pd.' web-client/src/e2e/drive-arc.test.tsx | wc -l | tr -d ' ')" = 0`
      — the test names no storage key. **Reads the whole file, comments included**
- [ ] `cd web-client && npm run check` exits 0. With `TASK-041505`–`TASK-041507` merged the suite
      reads **835 passed (835)** over **107** files, up from the merged **822 / 106**
- [ ] Every merged test in both e2e files passes unchanged — this diff appends three tests to one and
      four lines to the other's fixture. No assertion in either file moves, and none is weakened
- [ ] No file outside the three listed differs
- [ ] Every command in `verify:` exits 0

## Proof

**Every step was run in this worktree**, with `TASK-041505`–`TASK-041507` and this ticket's three
files applied to `develop` at `77c61708`: **835 / 107**, green, plus typecheck, lint and
`prettier --check`. Baseline on `develop` alone: **822 / 106**. Record what you actually measure;
never record the unmutated state as a step's "actual", and never write *would*, *if done* or
*not testable*.

1. Make the harness lose the write — `wiring.settleOffer = () => {}`, leaving the read real.
   **Two tests redden** — measured, `2 failed | 833 passed (835)`: `offers the account after a win,
   and never again once this browser has answered` and `spends the offer on the way to the account
   screen too`. **`offers it again to a browser that was shown it and answered nothing` stays
   green**, and that is correct — it asserts the offer's *presence*, which a lost write cannot
   affect. Revert.
2. Make the harness report every browser as settled — `wiring.offerSettled = () => true`, leaving the
   write real. **All three redden** — measured, `3 failed | 832 passed (835)`, including the second
   test, which is how that test is shown not to be vacuous: it can fail. Steps 1 and 2 are the
   opposite-direction pair for this file. Revert.
3. **A cross-ticket check, run outside this ticket's three files** — the budget governs the diff, not
   the probe. Call `settleOfferHere()` in `Lobby.tsx`'s render path, beside the `AccountOffer`
   element: `ADR-0085` §Alternatives' *"being shown spends it"*, the rule that ADR rejects by name.
   **Three tests redden** — measured on this projection, `3 failed | 832 passed (835)` — and one of
   them is `offers it again to a browser that was shown it and answered nothing`. That is the product
   rule this file guards and the component tests cannot reach across a boot. Revert `Lobby.tsx`
   completely.

   The other cross-ticket check for this file is recorded where it was run: `TASK-041506` §Proof
   step 1 removes `onClick={props.onAccept}` and reddens `spends the offer on the way to the account
   screen too` among three, one per level.
4. **The third file, measured rather than assumed.** With the two new `ArcWiring` fields added and
   `claimed-here-recovered-there.test.tsx` left alone, `npm run check` stops at typecheck:

   ```
   src/e2e/claimed-here-recovered-there.test.tsx(36,45): error TS2739: Type '{ history: null; signedIn: false; }' is missing the following properties from type 'ArcWiring': offerSettled, settleOffer
   ```

   A repository-wide search for `ArcWiring` names **exactly two** builders — that file and
   `drive-arc.test.tsx` — so three files is the whole radius, and `drive-duel.tsx`,
   `whole-duel.test.tsx` and `duel-secrecy.test.tsx` are genuinely untouched.
5. **Its partial mock is why that file needs the two overrides and not just the two fields.**
   `claimed-here-recovered-there.test.tsx` mocks `../main` with `importOriginal`, so without the
   overrides its `Lobby` calls the real `offerSettledHere`, which reads `main.tsx`'s
   `localStorage ?? nullStorage` — inert under Vitest — and answers `false` forever, silently. Adding
   the two overrides binds it to that file's own storage instead. Both shapes are green today; the
   overrides are what stop it becoming a second, quieter harness.

> **A red run names a prefix, not a set.** `npm run check` runs typecheck, then lint, then
> `prettier --check`, then the suite, and stops at the first failure — step 4's `tsc` error hid every
> test result behind it. Do not conclude anything about coverage from a run that stopped early.
>
> **Expect `The 'NO_COLOR' env is ignored due to the 'FORCE_COLOR' env being set.` on stderr.** It is
> wrong: `cat -v` shows plain bytes with `NO_COLOR=1` and escape codes without it, so every
> `grep -qE` in `verify:` needs it.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.

## Notes

**Why this is not `atomic:`.** Three files is within the cap, so no declaration is needed — but the
third one is there for a gate rather than for tidiness, and the ticket says which. The two new fields
could have been made optional, which would leave `claimed-here-recovered-there.test.tsx` compiling
untouched; that was rejected on the same ground `TASK-041506` makes `onAccept` required, and rejected
deliberately rather than by omission.

**Measured size: 105 changed lines** — 88 in `drive-arc.test.tsx` (48 of them the three tests, 27 the
two helpers), 13 in `drive-arc.tsx`, 4 in `claimed-here-recovered-there.test.tsx`.

**The offer does not need a profile read.** `bootClient`'s KDoc warns that a fresh `Storage` mints a
device id and reads no profile in the same boot; that is about the profile strip. The offer's three
terms are the store's outcome, the session token and the key, and none of them waits on a request —
so `bootAndWin` needs no `await` and the tests below use none.

`grep -c` counts matching **lines** and exits **1** on zero matches, so every zero-expectation is
wrapped as `test "$(… | wc -l | tr -d ' ')" = 0`. `-F` keeps `.` literal.
