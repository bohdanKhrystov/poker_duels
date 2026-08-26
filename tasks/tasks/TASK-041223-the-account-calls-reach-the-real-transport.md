---
schema: 2
id: TASK-041223
title: The account calls reach the real transport, and sign-in reaches it carrying nothing
type: task
status: backlog
parent: STORY-0412
module: web-client
estimate: S
tier: haiku
review: standard
files_touched: 2
labels: [client, account, wiring]
depends_on: [TASK-041222]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'binds the four account calls to the browser fetch and the browser storage'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'sends sign-in with no credential of its own, even holding a session'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'signs up as the device and never under the session'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'wires all four reads through the wrapper and names the browser fetch once'
  - test "$(grep -o 'authorizedFetch(' web-client/src/main.tsx | wc -l | tr -d ' ')" = 1
  - test "$(grep -o 'window.fetch(' web-client/src/main.tsx | wc -l | tr -d ' ')" = 2
  - test "$(grep -c 'apiFetch' web-client/src/main.tsx)" = 5
  - cd web-client && npm run check
---

## Goal

The account screen stops being a component nothing can call: sign-up, sign-in, sign-out and
revocation go out through `window.fetch` and `localStorage`, and the two that must carry no bearer
token do not.

## Files

| File | Action |
| --- | --- |
| `web-client/src/main.tsx` | modify |
| `web-client/src/App.test.tsx` | modify |

Read, and do not edit: `web-client/src/account/account-provider.tsx`;
`web-client/src/account/authorized-fetch.ts`; `docs/protocol.md` *Sign up*, *Sign in*, *Sign out*,
*Revoke this device*.

## Scope

- One module-scope constant beside the existing bindings:

  ```ts
  const accountCalls: AccountCalls = {
    signUp: (handle, password) =>
      signUp({ fetch: plainFetch, storage: localStorage, handle, password }),
    signIn: (handle, password) =>
      signIn({ fetch: plainFetch, storage: localStorage, reload, handle, password }),
    signOut: () => signOut({ fetch: plainFetch, storage: localStorage, reload }),
    revokeThisDevice: () =>
      revokeThisDevice({ fetch: plainFetch, storage: localStorage }),
  };
  ```

  with `plainFetch` the un-wrapped `(path, init) => window.fetch(path, init)` and
  `reload = () => window.location.reload()`, both at module scope.
- `AccountProvider` wraps the tree beside the other providers, taking `accountCalls`.
- **None of the four goes through `authorizedFetch`**, and each for its own documented reason:
  sign-in carries no authentication at all; sign-up authenticates as the device with a header it sets
  itself; sign-out and revocation set `Authorization` themselves from the token they read. A wrapper
  over any of them would put a second `Authorization` on a request that already has one, or a first
  one on the request `docs/protocol.md` says must have none.
- **This ticket owns one moved assertion, and it is the only one.** `plainFetch` is a **second**
  `window.fetch(` in the file, so `TASK-041210`'s
  `wires all four reads through the wrapper and names the browser fetch once` — which asserts
  `occurrencesIn(mainSource, "window.fetch(")` is `1` — reddens the moment `plainFetch` lands. Change
  that one expected number from `1` to `2` **in this diff**, and add a comment beside it naming
  `plainFetch` and the reason there are now two: one raw fetch for the reads that are wrapped, one
  for the four calls that must not be. Nothing else in that test moves: its `fetch: apiFetch`
  assertion still expects `4`, because the account calls bind `fetch: plainFetch`.
  `builds that wrapper once, at module scope` is untouched — `authorizedFetch(` stays `1` and the
  column-0 anchor still matches. **No assertion is weakened**: `2` is as exact as `1` was.
- **Do not route `apiFetch` through `plainFetch` to keep the count at `1`.** It would compile,
  behave identically and erase the signal on purpose. `TASK-041210`'s `## Out of scope` says so in
  advance — *"the two counts below are exact, so the day `TASK-041223` adds a fifth binding on a raw
  arrow, [that test] reddens and that ticket must state, in its own diff, which bindings are
  deliberately unauthenticated. That redness is the point; it is not brittleness to design around
  here."* This diff is where that statement gets made.
- `signedIn` is **not** computed here. `TASK-041231` already read it once at module scope and
  `TASK-041222` already consumes it; touching it again would be a second read of a global `DEC-032`
  warns about.
- Nothing else in `main.tsx` moves.

## Out of scope

- **Recomputing `signedIn` when the token changes.** Sign-in and sign-out reload the document, so a
  fresh boot is what recomputes it. There is no subscription to storage and no `storage` event
  listener.
- Any change to the four modules or the provider.
- Any read binding. `TASK-041210` wired those and they stay wrapped.

## Tests

`web-client/src/App.test.tsx`, in the existing `describe("App")`, beside the two existing binding
tests.

| Test | Proves |
| --- | --- |
| `binds the four account calls to the browser fetch and the browser storage` | Driven through the rendered tree with a stubbed `window.fetch`: signing up reaches `/api/auth/sign-up`, signing out reaches `/api/auth/sign-out` and the revoke control reaches `/api/me/device` with method `DELETE`. **Three different paths**, so a provider wired to one function four times cannot pass |
| `sends sign-in with no credential of its own, even holding a session` | With **both** a device id and a session token in `localStorage`, the recorded `/api/auth/sign-in` request carries neither `Authorization` nor `X-Device-Id`. The fixture holds both precisely so the test can see either leak, and this is the one request in the client that must be naked |
| `signs up as the device and never under the session` | With both values held, the recorded `/api/auth/sign-up` request carries `X-Device-Id` and **no** `Authorization` |
| `wires all four reads through the wrapper and names the browser fetch once` *(modified — one number)* | `TASK-041210`'s test, with its `window.fetch(` expectation moved from `1` to `2` and a comment saying why. Its `fetch: apiFetch` expectation stays `4`, which is the half that proves the four reads did not quietly follow the account calls onto the raw arrow |
| `binds the history read to the browser fetch and the browser storage` *(unchanged)* | Still passes, so the read bindings did not move. It asserts only that `main.tsx` *contains* `window.fetch(` and `localStorage`, and a `toMatch` is satisfied by two occurrences as well as by one |

## Acceptance criteria

- [ ] `App > binds the four account calls to the browser fetch and the browser storage` passes,
      asserting **three** distinct paths and the `DELETE` method
- [ ] `App > sends sign-in with no credential of its own, even holding a session` passes, with both
      values in the storage
- [ ] `App > signs up as the device and never under the session` passes
- [ ] `App > wires all four reads through the wrapper and names the browser fetch once` passes with
      its `window.fetch(` expectation changed from `1` to `2` and its `fetch: apiFetch` expectation
      still `4`. That single number is the **only** edit to a pre-existing assertion in the file, and
      a comment beside it names `plainFetch`
- [ ] Every **other** pre-existing test in `App.test.tsx` passes unchanged, `builds that wrapper
      once, at module scope` included — no other expected value moves and none is weakened
- [ ] `grep -o 'authorizedFetch(' web-client/src/main.tsx | wc -l` returns `1` — one **construction**.
      Counted with its call parenthesis and by occurrence, not by line: `grep -c 'authorizedFetch'`
      answers `3` on the merged file (the import, a comment and the call), so the line-counting form
      this criterion used to carry was unsatisfiable. That is `TASK-041210`'s `## Notes` finding,
      reproduced here verbatim and now corrected here too
- [ ] `grep -o 'window.fetch(' web-client/src/main.tsx | wc -l` returns `2` — the wrapper's, and
      `plainFetch`'s
- [ ] `grep -c 'apiFetch' web-client/src/main.tsx` returns `5` — one declaration and the four read
      bindings
- [ ] `plainFetch`, `reload` and `accountCalls` are all declared at module scope
- [ ] No file outside the two listed differs
- [ ] Every command in `verify:` exits 0

## Proof

1. Bind all four account calls to `apiFetch`.
   **`sends sign-in with no credential of its own, even holding a session` reddens** on the
   `Authorization` assertion, and `signs up as the device and never under the session` reddens with
   it. Both only because the fixture holds a token — empty that storage and both mutations pass
   silently, which is the trap and the reason the fixture is specified. **A third test reddens too**,
   and predicting it is the point of owning the file: `wires all four reads through the wrapper and
   names the browser fetch once` sees `fetch: apiFetch` **eight** times against its expected `4`.
   Record all three. This is the exact shortcut `TASK-041210`'s Proof step 3 refused in advance.
   Revert.
2. Wire `revokeThisDevice` to `signOut` in the `accountCalls` object.
   **`binds the four account calls…` reddens** on the `/api/me/device` path. A copy-paste in an
   object literal of four one-line arrows is invisible on the page and is what that test's three
   distinct paths exist for.
3. Build `accountCalls` inside the render, as an object literal in the JSX.
   **Nothing reddens.** Record it: a fresh object on every render re-runs nothing today because
   `AccountProvider` holds no effect, and it becomes a defect the day anything below it does. It is
   the same rule `main.tsx`'s four existing comments carry, and the criterion above is the gate.
4. Build `apiFetch` from `plainFetch` — `const apiFetch = authorizedFetch(plainFetch, localStorage);`
   — and put the `window.fetch(` expectation back to `1`.
   **Everything passes**: `tsc`, eslint, this ticket's three tests and both of `TASK-041210`'s. Run
   it, and then do not ship it. It is the tidiest-looking version of this diff and it silently
   retires the gate `TASK-041210` built **for this ticket**: the count stops being able to notice a
   fifth binding on a raw arrow, and the statement about which requests are deliberately
   unauthenticated stops being forced into anyone's diff. A mutation that breaks nothing is not
   always safe — here it means the test stopped depending on the thing it was watching. Revert, and
   say in the PR that this step was run.
5. Pass `reload` as `window.location.reload` rather than as an arrow.
   **`npm run check` may pass and the browser throws `Illegal invocation`** — an unbound `reload` has
   no receiver. Run it under `npm run build && node` if you can; if you cannot reproduce it under
   jsdom, say so in the PR rather than claiming the suite covers it.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.
