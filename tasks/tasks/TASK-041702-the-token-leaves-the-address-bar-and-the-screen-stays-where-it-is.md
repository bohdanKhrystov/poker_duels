---
schema: 2
id: TASK-041702
title: The token leaves the address bar, and the screen stays where it is
type: task
status: backlog
parent: STORY-0417
module: web-client
estimate: S
tier: sonnet
review: standard
files_touched: 2
labels: [client, routing, recovery]
depends_on: [TASK-041701]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test -- src/routing/use-screen.test.tsx 2>&1 | grep -qE 'Test Files +1 passed \(1\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- src/routing/use-screen.test.tsx 2>&1 | grep -qE 'Tests +9 passed \(9\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'takes the token out of the address without moving the screen'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'tells its subscribers the address changed, because replaceState will not'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'leaves an address with no token exactly as it found it'
  - test "$(grep -oF 'pushState' web-client/src/routing/use-screen.ts | wc -l | tr -d ' ')" = 0
  - test "$(grep -oF 'location.hash =' web-client/src/routing/use-screen.ts | wc -l | tr -d ' ')" = 1
  - cd web-client && npm run check
---

## Goal

`useScreen()` gains `clearToken()`, which replaces the fragment with the current screen's own
address — so the mailed secret leaves the address bar and the current history entry, and the player
stays on the screen the link opened.

## Files

| File | Action |
| --- | --- |
| `web-client/src/routing/use-screen.ts` | modify |
| `web-client/src/routing/use-screen.test.tsx` | modify |

Read, and do not edit:

- [`ADR-0081`](../../docs/adr/ADR-0081-a-mailed-link-is-a-fragment-route-and-the-token-is-the-segment-behind-the-slug.md)
  §5 — *"the fragment is then replaced with `hashForScreen(screen)` through the same replace path
  `ADR-0076` §5 specifies, so the module's cache stays honest and the screen does not move"*, and the
  sentence after it: a screen that re-derives its token from the address after the replace finds
  nothing.
- [`ADR-0076`](../../docs/adr/ADR-0076-a-screen-the-player-chose-has-an-address.md) §5 — the
  `hashchange`/`replaceState` trap, which the merged comment inside `leave()` already states.
- `web-client/src/routing/screen.ts` — `hashForScreen` and the `Screen` union `TASK-041701` widened.
  Call it; do not rebuild an address from parts.

## Scope

- **`useScreen()`'s return type gains one member**, beside `screen`, `open` and `leave`:

  ```ts
  readonly clearToken: () => void;
  ```

- **Its body is `leave()`'s, with one word changed**:

  ```ts
  clearToken: () => {
    window.history.replaceState(null, "", hashForScreen(screen));
    notify();
  },
  ```

  `replaceState` rather than an assignment to `location.hash`, because an assignment **pushes a
  history entry** — which would leave the mailed secret one *Back* press away, and `ADR-0031` §4
  spends the fragment precisely so it is not there.
- **`notify()` is not optional and is the bug no type checker catches.** `history.replaceState` fires
  neither `popstate` nor `hashchange` (`ADR-0076` §5), so without it this module's own subscribers
  keep the stale snapshot. The merged comment inside `leave()` says this; do not copy the comment,
  state instead why `clearToken` differs from `leave` — it stays put, and `leave` goes home.
- **KDoc on the returned member** naming `ADR-0081` §5, and saying that the token must already be in
  component state before this runs: after it, the address holds nothing to re-read.

## Out of scope

- **Reading the token.** `tokenFromHash` is `TASK-041701`'s and is not called here — this hook never
  learns what the second segment was, which is `ADR-0081` §5's *the token never enters the routing
  module's state*.
- **Any screen component, any request, any copy.** Later tickets.
- **Touching `leave`, `open`, `subscribe`, `getSnapshot` or the module-scope listener set.** All four
  are merged and this diff adds a member beside them.
- **A `clearToken` that takes a `Screen` argument.** It replaces with the screen the hook already
  computed; a caller able to pass a different one is a caller able to move the player.

## Tests

`web-client/src/routing/use-screen.test.tsx`, appended in the idiom the six merged tests already use.
**6 merged tests become 9.**

| Test | Proves |
| --- | --- |
| `takes the token out of the address without moving the screen` | Start at `#/verify/Xk93qQz7aa4bbCC1ddEE8ff2gg`. Render a probe that reads `useScreen()`, assert the screen is `"verify"` **before**, call `clearToken()` inside `act`, then assert `window.location.hash` is exactly `"#/verify"` **and** the screen is still `"verify"`. Both halves, because a replace to `"/"` would satisfy the first alone. **Measured in this worktree**: the hash lands at `"#/verify"` and `href` at `"http://localhost:3000/#/verify"` |
| `tells its subscribers the address changed, because replaceState will not` | A component that renders the screen string and a render counter. After `clearToken()` the component has re-rendered, proven by the snapshot the subscriber received rather than by the DOM alone — this is the `notify()` half, and `ADR-0076` §5's silent trap. Drive it from a **second** hook instance mounted in the same tree, so the assertion is about the module's subscriber set and not about one component's own state |
| `leaves an address with no token exactly as it found it` | Start at `#/reset` — the address `ADR-0081` §6 says a reload lands on. `clearToken()` leaves `window.location.hash` at `"#/reset"` and the screen at `"reset"`. Idempotent, and the case that runs on every re-render of a screen the player refreshed |

**No `try` anywhere in the added code, and no `expect()` inside one.** **No `history.pushState`
anywhere in the file**, and `verify:` pins it at zero.

## Acceptance criteria

- [ ] `cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'takes the token out of the address without moving the screen'`
      — passes, asserting the hash **and** the screen, with the screen asserted before as well as
      after
- [ ] `cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'tells its subscribers the address changed, because replaceState will not'`
      — passes, driven through a second mounted consumer of the same module
- [ ] `cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'leaves an address with no token exactly as it found it'`
      — passes, from `#/reset`, asserting the address is unchanged
- [ ] `cd web-client && NO_COLOR=1 npm run --silent test -- src/routing/use-screen.test.tsx 2>&1 | grep -qE 'Tests +9 passed \(9\)'`
      and `… | grep -qE 'Test Files +1 passed \(1\)'` — **exactly nine**: the six merged plus these
      three. The merged six are pinned by this **count**, never by their names
- [ ] `test "$(grep -oF 'pushState' web-client/src/routing/use-screen.ts | wc -l | tr -d ' ')" = 0`
      — nothing in this module pushes an entry. Reads the whole file, comments included
- [ ] `test "$(grep -oF 'location.hash =' web-client/src/routing/use-screen.ts | wc -l | tr -d ' ')" = 1`
      — the one merged assignment, inside `open`. `clearToken` did not become a second one
- [ ] `cd web-client && npm run check` exits 0. The whole-suite total is deliberately **not** pinned
      here: this ticket and `TASK-041703` have disjoint `Files` tables and may be dispatched in one
      batch, which moves any absolute figure. The nine-and-one pair above does the collection-error
      job for the one file this ticket owns
- [ ] Every merged test in `use-screen.test.tsx` passes unchanged — this diff appends three and edits
      none. No assertion moves and none is weakened
- [ ] No file outside the two listed differs
- [ ] Every command in `verify:` exits 0

## Proof

Run each step, record what you measured, and revert it. **A mutation here is an experiment, not a
change**; both mutated files are inside this ticket's budget.

1. **Replace with `"/"` instead of the current screen** — `hashForScreen("first")` in `clearToken`.
   Predict: `takes the token out of the address without moving the screen` reddens on the screen
   half, and `leaves an address with no token exactly as it found it` reddens too. Record both.
   This is the mutation that sends a player who clicked a link in their mail to the lobby.
2. **Drop `notify()`.** Predict: `tells its subscribers the address changed, because replaceState
   will not` reddens **alone**. `ADR-0076` §5 calls this the one bug no type checker catches, and
   `TASK-041228` measured that the neighbouring `hashchange`/`popstate` swap reddens **nothing** — so
   check this one really does move, and if it does not, say so plainly rather than assuming the
   guard works.
3. **Use `window.location.hash = hashForScreen(screen)` instead of `replaceState`.** Predict: the
   hash assertion still passes and the `pushState`/history property is **not** covered by any test.
   Record the result. If every test stays green, that is the finding: write it in `## Notes` on
   landing, because it means the *replace, never push* rule is carried by the `verify:` grep alone.
4. **Vacuity check on the third test** — make `clearToken` a no-op body. Predict: `leaves an address
   with no token exactly as it found it` stays **green** (it is idempotent by construction) while the
   other two redden. That is expected and is why the third test is not the ticket's evidence; record
   it so a reviewer does not read that test as proving more than it does.

> **A red run names a prefix, not a set.** Vitest stops reporting past its first hard failure: a
> syntax error in a client source file was measured on this repo failing **twelve** test files at
> collection and printing `667 passed` with **no failure count at all**.
>
> **Expect `The 'NO_COLOR' env is ignored due to the 'FORCE_COLOR' env being set.` on stderr.** It is
> wrong — measured, 0 escape bytes with it and 74 without — so every `grep -qE` needs it.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.

## Notes

**Why this is a hook member and not a `history.replaceState` call inside the screen component.**
`use-screen.ts` keeps a module-scope subscriber set and a cached snapshot. A component that called
`replaceState` itself would change the address without telling that set, and every mounted
`useScreen()` would keep answering `"verify"` from a stale read — right by accident here, wrong the
first time anything depends on it. `ADR-0076` §5 already made this module the only one that touches
`window` for navigation, and `ADR-0081` §5 says the replace goes through *the same replace path*.

**Measured before this ticket was written.** With a throwaway `clearToken` in place and a placeholder
`verify` branch in `Lobby.tsx`, opening `#/verify/Xk93-QQ_z7~aa.bb` rendered the verify screen, and
clicking a control bound to `clearToken` left `window.location.hash` at `"#/verify"` and
`window.location.href` at `"http://localhost:3000/#/verify"` with the screen still on the page. The
whole merged suite stayed at `836 passed (836)`. So the shape works and costs no merged test.

`grep -c` counts matching **lines** and exits **1** on zero matches, so both zero-and-one expectations
above are wrapped as `test "$(… | wc -l | tr -d ' ')" = N`.
