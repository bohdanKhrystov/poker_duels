---
schema: 2
id: TASK-041202
title: The hook that carries the address, and the trap that makes a stale render look like React
type: task
status: backlog
parent: STORY-0412
module: web-client
estimate: S
tier: sonnet
review: standard
files_touched: 2
labels: [client, routing]
depends_on: [TASK-041201]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test -- src/routing/use-screen.test.tsx 2>&1 | grep -qE 'Tests +5 passed \(5\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'opening a screen changes the address and adds an entry to go back to'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'leaving a screen re-renders with no hashchange anywhere'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'leaving a screen replaces the entry rather than stacking another'
  - cd web-client && npm run check
---

## Goal

A component can read which screen the address names and can change it, through the one primitive
`ADR-0032` §3 already chose, with the `pushState` trap `ADR-0076` §5 names closed by a test rather
than by a comment.

## Files

| File | Action |
| --- | --- |
| `web-client/src/routing/use-screen.ts` | create |
| `web-client/src/routing/use-screen.test.tsx` | create |

Read, and do not edit:
[`ADR-0076`](../../docs/adr/ADR-0076-a-screen-the-player-chose-has-an-address.md) §5 and §6;
`web-client/src/routing/screen.ts`; `web-client/src/store/duel-store.ts` (the notify-and-cache
contract this repeats).

## Scope

- Exactly one export, and it is the only React-aware file in `routing/`:

  ```ts
  export function useScreen(): {
    readonly screen: Screen;
    readonly open: (screen: Exclude<Screen, "first">) => void;
    readonly leave: () => void;
  };
  ```

  `open` and `leave` are `ADR-0076` §6's two navigations. `open` is typed to refuse `"first"`
  because nobody pushes their way onto the entry point; `leave` is how the first screen is reached.
- `useSyncExternalStore(subscribe, getSnapshot)` from React core — no library, `ADR-0032` §3.
  `subscribe` adds the callback to a module-level `Set` and adds one `hashchange` listener on
  `window`; unsubscribing removes both. `getSnapshot` returns
  `screenFromHash(window.location.hash)`.
- **`getSnapshot` returns the `Screen` string and never a fresh object.** A snapshot that is a new
  reference on every call makes React loop forever; a string compares by value and needs no cache.
  Carry that as a comment.
- **`open` is an assignment to `window.location.hash`**, which fires `hashchange` and adds a history
  entry. It notifies nothing itself — the event is what notifies, and that is the whole reason this
  module subscribes to `hashchange` rather than to `popstate`.
- **`leave` is `window.history.replaceState(null, "", hashForScreen("first"))` followed by this
  module notifying its own subscribers**, because `replaceState` fires **neither** `popstate` nor
  `hashchange` (`ADR-0076` §5). Carry that sentence as a comment: it is the one bug in this file no
  type checker catches.
- The two module-level pieces — the subscriber set and the listener — are created once at module
  scope and shared by every caller, in the `duel-store.ts` tradition.

## Out of scope

- **Any use of `history.pushState`.** `ADR-0076` §5 rules it out for the push, and the acceptance
  criteria gate the whole file on it by name.
- **Preserving `?room=` across a `leave`.** `hashForScreen("first")` is `"/"`, so a replace drops the
  query as well as the fragment. That is `ADR-0076` §3's and §7's literal instruction and it is
  harmless: `roomCodeFromSearch` is consumed once at boot before the tree exists (`ADR-0032` §2) and
  `ADR-0072` remembers the room. **A refusal, not an oversight** — there is a test below that pins
  it, so a later reader finds a decision rather than a bug.
- Rendering anything, and any knowledge of what a screen contains. `Lobby.tsx` owns the branch
  (`TASK-041203`).
- `beforeunload`, and any confirmation on leaving the document. `ADR-0076` §6's last row refuses one.

## Tests

`web-client/src/routing/use-screen.test.tsx`, describe block `"the screen the address names"`. Use
`renderHook` from `@testing-library/react`, and reset `window.location.hash` and the history depth in
a `beforeEach` so the tests do not inherit each other's address.

| Test | Proves |
| --- | --- |
| `reads the screen the address already names` | With `location.hash` set to `"#/duels"` before the render, the hook's first value is `"duels"`, and with it set to `"#/leaderboard"` it is `"leaderboard"`. **Two different addresses in one test**, because one would pass against a hook that returns a constant |
| `re-renders when the address changes under it` | From `"/"`, set `location.hash = "#/leaderboard"` outside the hook and `await` the hook reporting `"leaderboard"`. Proves the subscription, not the read |
| `opening a screen changes the address and adds an entry to go back to` | `open("duels")`: `window.location.hash` becomes `"#/duels"`, `history.length` is **one greater** than it was before the call (captured, not assumed), and the hook reports `"duels"` after the `hashchange` settles. This is the test the `pushState` mutation reddens |
| `leaving a screen replaces the entry rather than stacking another` | From `"#/duels"`, `leave()`: `window.location.hash` is `""`, the hook reports `"first"`, and `history.length` is **unchanged** from the value captured before the call. `ADR-0076` §6's second row |
| `leaving a screen re-renders with no hashchange anywhere` | A counting `hashchange` listener installed before `leave()` records **zero** events, and the hook still reports `"first"` in the same `act`. This is `ADR-0076` §5's trap gated directly: it fails against a `leave` that trusts an event that never fires |

Five tests in a new file: `npm run test -- src/routing/use-screen.test.tsx` reports **5**.

## Acceptance criteria

- [ ] `the screen the address names > reads the screen the address already names` passes, asserting
      **two** different addresses
- [ ] `the screen the address names > re-renders when the address changes under it` passes
- [ ] `the screen the address names > opening a screen changes the address and adds an entry to go
      back to` passes, asserting the `history.length` delta against a value captured before the call
- [ ] `the screen the address names > leaving a screen replaces the entry rather than stacking
      another` passes, asserting an unchanged `history.length`
- [ ] `the screen the address names > leaving a screen re-renders with no hashchange anywhere` passes
      with a listener count of exactly `0`
- [ ] `grep -c 'pushState' web-client/src/routing/use-screen.ts` returns `0`
- [ ] `grep -c 'beforeunload' web-client/src/routing/use-screen.ts` returns `0`
- [ ] `npm run test -- src/routing/use-screen.test.tsx` reports `Tests  5 passed (5)`
- [ ] No file outside the two listed differs
- [ ] Every command in `verify:` exits 0

## Proof

1. Change `open` to `window.history.pushState(null, "", hashForScreen(screen))`.
   **`opening a screen changes the address and adds an entry to go back to` reddens alone**, and it
   reddens on the hook's value while `window.location.hash` reads correctly — which is exactly the
   failure `ADR-0076` §5 describes as looking like a React bug. `history.length` still grows, so the
   half of the test that would have caught it by counting entries does **not**; the value assertion
   is what catches it. Revert.
2. Delete the `notify()` after `replaceState` in `leave`.
   **`leaving a screen re-renders with no hashchange anywhere` reddens alone.** The other four pass,
   including `leaving a screen replaces the entry rather than stacking another` if it reads
   `window.location.hash` rather than the hook — write that test against the **hook's** value so this
   mutation reddens two tests rather than one. Revert.
3. Subscribe to `popstate` instead of `hashchange`.
   **`re-renders when the address changes under it` and `opening a screen…` both redden**, and the
   two `leave` tests still pass, because `leave` notifies itself. That asymmetry is the shape of this
   module and is worth seeing once.
4. Make `getSnapshot` return `{ screen: screenFromHash(window.location.hash) }`.
   **Every test in the file reddens**, with React's *"The result of getSnapshot should be cached"*
   warning turned into a failure by the render loop. Record what the failure looks like in the PR: it
   names no line in this file, which is why the string return carries a comment.
5. Make `leave` write `"#/"` instead of `hashForScreen("first")`.
   **`leaving a screen replaces the entry rather than stacking another` reddens** on the empty-hash
   assertion, while the hook still reports `"first"` — `screenFromHash("#/")` is `"first"` by
   `TASK-041201`. The address would lie quietly, which is why that test asserts the hash as well as
   the screen.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.
