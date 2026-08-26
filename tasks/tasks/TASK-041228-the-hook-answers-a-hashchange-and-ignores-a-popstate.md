---
schema: 2
id: TASK-041228
title: The hook answers a hashchange and ignores a popstate, which no test can currently tell apart
type: task
status: done
parent: STORY-0412
module: web-client
estimate: XS
tier: sonnet
review: standard
files_touched: 1
labels: [client, routing, test-gap]
depends_on: [TASK-041203]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test -- src/routing/use-screen.test.tsx 2>&1 | grep -qE 'Tests +6 passed \(6\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'ignores a popstate, and still answers the hashchange it subscribed to'
  - cd web-client && npm run check
---

## Goal

Swapping `useScreen`'s subscription from `hashchange` to `popstate` reddens a test. Today it reddens
nothing, and the shipped hook is correct only by nobody having edited it.

## Why this is a ticket and not a comment

`TASK-041202` shipped `use-screen.ts` with a `## Proof` step 3 — *subscribe to `popstate` instead of
`hashchange`, and two tests redden* — **whose prediction is false**. Coder and reviewer each read
this repo's installed jsdom and found the cause, recorded in full in that ticket's `## Notes`:
assigning `window.location.hash` fires neither event synchronously and then fires **both** on a
microtask, where a real browser fires only `hashchange` for a same-document fragment change. So every
existing test in the file passes under either subscription.

That is the same defect the story exists to prevent, one layer up. `ADR-0076` §5 names the
`pushState` trap **because it is silent**; the gate on it is silent too, and a later edit swapping the
two events would merge green.

**Reproduced on this branch before the ticket was written**, so the numbers below are measured rather
than predicted: with `hashchange` replaced by `popstate` in `use-screen.ts`, all five tests in
`use-screen.test.tsx` pass, and the one test this ticket adds fails with
`expected 'duels' to be 'first'`.

## Files

| File | Action |
| --- | --- |
| `web-client/src/routing/use-screen.test.tsx` | modify |

`use-screen.ts` is **not** in the budget and must not change: the shipped hook is correct, and this
ticket buys the gate it never had. Read, and do not edit: `web-client/src/routing/use-screen.ts`;
[`ADR-0076`](../../docs/adr/ADR-0076-a-screen-the-player-chose-has-an-address.md) §5;
`tasks/tasks/TASK-041202-the-hook-that-carries-the-address-and-the-trap-that-is-silent.md` `## Notes`.

## Scope

- One test added to the existing `describe("the screen the address names")`, taking the file to
  **six**. Nothing else in the file moves and no assertion in it is weakened.
- **The isolating channel is `history.pushState`**, which fires *neither* `popstate` nor
  `hashchange` — the one way to move the address in this jsdom without triggering both. The test
  then dispatches a bare `new Event("popstate")` on `window` and asserts the hook's value is
  **unchanged**.
- **The same test then dispatches a bare `new Event("hashchange")` and asserts the value now
  changes.** Without that second half the test is vacuous: a hook that returned a constant, or one
  subscribed to nothing at all, would pass the negative assertion. One address change, two events,
  opposite outcomes — that pair is the whole gate.
- **No re-render may be forced between the `pushState` and the negative assertion.**
  `useSyncExternalStore` re-reads `getSnapshot` on every render, so a render triggered in between
  would move the value for a reason that has nothing to do with the subscription and the test would
  pass for the wrong reason. Carry that as a comment beside the assertion.
- The existing `beforeEach` already does `history.replaceState(null, "", "/")`, which fires no event,
  so the entry this test pushes does not leak into the next test's address. Nothing new is needed.

## Out of scope

- **Changing `use-screen.ts`.** It is right. A criterion pins it: no file outside the one listed
  differs.
- **Asserting `history.length` around the `pushState`.** This test is about *which event notifies*,
  and `TASK-041202`'s two `leave` tests already own the entry-count question. **A refusal, not an
  omission** — adding it here would make the mutation in `## Proof` step 1 redden for a second,
  unrelated reason and hide which assertion did the work.
- **Fixing jsdom, or asserting anything about it.** The environment's behaviour is the *reason* for
  the test's shape, recorded in the comment; it is not itself under test.
- **`TASK-041202`'s own `verify:` block**, which reads `Tests 5 passed (5)`. It is a merged ticket's
  historical record and is not re-run; this ticket's block reads `6` and is the live one.

## Tests

`web-client/src/routing/use-screen.test.tsx`, in the existing describe block
`"the screen the address names"`.

| Test | Proves |
| --- | --- |
| `ignores a popstate, and still answers the hashchange it subscribed to` | From `/`, `history.pushState(null, "", "#/duels")` moves the address firing neither event; a dispatched bare `popstate` leaves the hook reporting `"first"`; a dispatched bare `hashchange` then makes it report `"duels"`. The negative half fails against a `popstate` subscription; the positive half fails against a hook that never updates. **The only channel under this jsdom that separates the two events**, because assigning `location.hash` fires both |

Six tests in the file after this: `npm run test -- src/routing/use-screen.test.tsx` reports **6**.

## Acceptance criteria

- [ ] `the screen the address names > ignores a popstate, and still answers the hashchange it
      subscribed to` passes
- [ ] That test asserts `"first"` **after** the `popstate` and `"duels"` **after** the `hashchange`
      — both, in one test
- [ ] `npm run test -- src/routing/use-screen.test.tsx` reports `Tests  6 passed (6)`
- [ ] The five pre-existing tests in the file pass unchanged, and no assertion in any of them is
      edited or removed
- [ ] `grep -c 'pushState' web-client/src/routing/use-screen.ts` still returns `0` — the production
      file is untouched
- [ ] No file outside the one listed differs
- [ ] Every command in `verify:` exits 0

## Proof

Steps 1, 2 and 4 were run on this branch against the real files; the quoted failures are what the
runner actually printed. Step 4's prediction was written, measured, found **false**, and rewritten —
which is the only reason it says something useful.

1. In `use-screen.ts`, replace both `"hashchange"` occurrences with `"popstate"` — the exact mutation
   `TASK-041202` step 3 predicted and that reddened nothing.
   **The new test reddens alone**, with `AssertionError: expected 'duels' to be 'first'` at the
   assertion after the dispatched `popstate`. The five pre-existing tests in the file **all still
   pass** — which is the finding this ticket exists for, now visible in one run. Revert.
2. In `use-screen.ts`, make `getSnapshot` return the constant `"first"`.
   **The new test reddens** with `expected 'first' to be 'duels'` — the *positive* half, after the
   dispatched `hashchange`. This is why the test does not stop at the negative assertion: a hook that
   answers nothing at all satisfies *"the value does not change"* perfectly. Revert.
3. Delete the `dispatchEvent(new Event("hashchange"))` and its assertion, keeping only the negative
   half.
   **Nothing reddens**, and that is the point — the test is now vacuous and passes against every
   mutation in step 2. Record it in the PR: this is the shape `TASK-041202`'s step 3 had, and the
   reason it took a coder and a reviewer to notice.
4. Replace the `history.pushState(null, "", "#/duels")` with `window.location.hash = "#/duels"`,
   inside the same `act()`.
   **Nothing reddens — and this step's prediction was rewritten because the first one was wrong.**
   Measured both ways on this branch: the assignment variant passes against the shipped hook *and*
   still fails against step 1's mutant, at the same assertion. So `pushState` is not required to make
   the mutation visible **today**; it is required to make the test's inputs *determined*. Assigning
   the hash queues **both** events on a microtask in this jsdom, so whether they have fired by the
   time the synchronous assertion runs depends on what `act` happens to flush — a test whose two
   inputs arrive from somewhere other than its own `dispatchEvent` calls. `pushState` fires neither
   event, so the only `popstate` and the only `hashchange` in the test are the two it sends on
   purpose. Record that reasoning in the PR rather than the shorter *"the assignment fails"*, which
   is false.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.

## Notes

**This ticket exists because a gate was proved blind, not because a bug shipped.** `TASK-041202`'s
Proof predicted that subscribing to `popstate` instead of `hashchange` would redden a test; it
reddened nothing. Coder and reviewer each read this repo's installed jsdom and found why — assigning
`window.location.hash` fires neither event synchronously and then fires **both** on a microtask,
where a real browser fires only `hashchange`. The hook was correct throughout. Nothing would have
noticed if a later edit made it wrong, and `ADR-0076` §5 names that trap precisely because it is
silent.

**The isolating channel is `history.pushState`, because it fires neither event** — in jsdom and in a
real browser alike. Move the address that way, dispatch a bare `popstate`, and the hook's value must
not move.

**Asserting a non-event needs a different primitive, and that is the whole craft of this test.**
`waitFor` and `findBy` retry until a condition holds, so they pass the moment any retry succeeds —
useless for *"this did not happen"*. Here the assertion is a single synchronous `expect` immediately
after a synchronous `act()`. `dispatchEvent` is synchronous per the DOM spec, unlike the hash
assignment the file's other tests need `waitFor` for, and `act()` flushes any resulting React update
before returning. A hook that responded would run `notify()` during the dispatch and already read
`"duels"` when the assertion executes — there is no transient for a retry to wait past.

**It catches the break that would actually happen.** Two mutations were run: `hashchange` → `popstate`
reddens **this test alone** while the five older ones stay green, which is what shows it uniquely
gates the distinction; a constant `getSnapshot` reddens all six, which only shows it is alive. The
reviewer then reasoned through the case neither covers — a hook subscribing to **both** events, the
plausible way someone breaks this while fixing a perceived bug — and confirmed the first assertion
fails there too.

**Both halves are asserted.** The ignored `popstate` *and* a subsequent honoured `hashchange`. A test
that only proved the hook ignores things would pass against a hook that ignores everything.
