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
- `signedIn` is computed at module scope from `readSessionToken(localStorage)` and passed to the
  tree, as `TASK-041222` requires.
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
| `binds the history read to the browser fetch and the browser storage` *(unchanged)* | Still passes, so the read bindings did not move |

## Acceptance criteria

- [ ] `App > binds the four account calls to the browser fetch and the browser storage` passes,
      asserting **three** distinct paths and the `DELETE` method
- [ ] `App > sends sign-in with no credential of its own, even holding a session` passes, with both
      values in the storage
- [ ] `App > signs up as the device and never under the session` passes
- [ ] Every pre-existing test in `App.test.tsx` passes unchanged
- [ ] `grep -c 'authorizedFetch' web-client/src/main.tsx` returns `1` — the reads, and nothing else
- [ ] `grep -c 'apiFetch' web-client/src/main.tsx` returns `5` — one declaration and the four read
      bindings
- [ ] `plainFetch`, `reload`, `accountCalls` and `signedIn` are all declared at module scope
- [ ] No file outside the two listed differs
- [ ] Every command in `verify:` exits 0

## Proof

1. Bind all four account calls to `apiFetch`.
   **`sends sign-in with no credential of its own, even holding a session` reddens** on the
   `Authorization` assertion, and `signs up as the device and never under the session` reddens with
   it. Both only because the fixture holds a token — empty that storage and both mutations pass
   silently, which is the trap and the reason the fixture is specified. This is the exact shortcut
   `TASK-041210`'s Proof step 3 refused in advance. Revert.
2. Wire `revokeThisDevice` to `signOut` in the `accountCalls` object.
   **`binds the four account calls…` reddens** on the `/api/me/device` path. A copy-paste in an
   object literal of four one-line arrows is invisible on the page and is what that test's three
   distinct paths exist for.
3. Build `accountCalls` inside the render, as an object literal in the JSX.
   **Nothing reddens.** Record it: a fresh object on every render re-runs nothing today because
   `AccountProvider` holds no effect, and it becomes a defect the day anything below it does. It is
   the same rule `main.tsx`'s four existing comments carry, and the criterion above is the gate.
4. Pass `reload` as `window.location.reload` rather than as an arrow.
   **`npm run check` may pass and the browser throws `Illegal invocation`** — an unbound `reload` has
   no receiver. Run it under `npm run build && node` if you can; if you cannot reproduce it under
   jsdom, say so in the PR rather than claiming the suite covers it.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.
