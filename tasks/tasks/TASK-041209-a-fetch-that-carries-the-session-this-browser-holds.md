---
schema: 2
id: TASK-041209
title: A fetch that carries the session this browser holds, and adds nothing when it holds none
type: task
status: backlog
parent: STORY-0412
module: web-client
estimate: S
tier: haiku
review: standard
files_touched: 2
labels: [client, auth]
depends_on: [TASK-041208]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test -- src/account/authorized-fetch.test.ts 2>&1 | grep -qE 'Tests +5 passed \(5\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'adds a bearer header when this browser holds a session'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'sends exactly the headers it was given when this browser holds none'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'keeps the device id header the caller built'
  - cd web-client && npm run check
---

## Goal

One wrapper puts `Authorization: Bearer …` on a read that would otherwise go out as the device, so
every `/api/me` read answers for whoever is signed in — with **one** edited call site rather than
four.

## Files

| File | Action |
| --- | --- |
| `web-client/src/account/authorized-fetch.ts` | create |
| `web-client/src/account/authorized-fetch.test.ts` | create |

Read, and do not edit: `web-client/src/profile/api.ts` (the `ApiFetch` type this wraps);
`web-client/src/protocol/session-token.ts`; `docs/protocol.md` *Profile endpoint* (the precedence
rule).

## Scope

- One export:

  ```ts
  export function authorizedFetch(fetch: ApiFetch, storage: Storage): ApiFetch;
  ```

  It returns an `ApiFetch` that calls the wrapped one with the same `path`, the same `method`, the
  same `body`, and headers that are the caller's plus `Authorization: "Bearer " + token` **when and
  only when** `readSessionToken(storage)` answers with one.
- **The token is read per call, not captured.** A wrapper built at module scope outlives a sign-out,
  and a captured token would keep a signed-out browser signed in until the next reload.
- The caller's headers are never removed or rewritten — in particular `X-Device-Id` goes out beside
  the bearer, because `ADR-0030` §8 says the client keeps sending it and `docs/protocol.md` says a
  valid token outranks it server-side. Precedence is the server's to apply, not this wrapper's.
- The header name and scheme are literals here: `"Authorization"` and the `"Bearer "` prefix with its
  single space.
- KDoc naming what it is for and what it must never wrap (below).

## Out of scope

- **Wrapping `POST /api/auth/sign-in`.** `docs/protocol.md` is explicit: sign-in *"carries none of
  its own — no `X-Device-Id`, no `Authorization` header"*, because it is how a client obtains
  authentication in the first place. `TASK-041210` binds it to the plain `window.fetch`, and this
  module's KDoc says so. **A refusal, not an omission.**
- **Wrapping sign-up.** It authenticates with `X-Device-Id` and `TASK-041212` sets that header
  itself.
- Reading `localStorage` from module scope — the storage is injected, for `DEC-032`'s reason.
- Retrying, refreshing or reacting to a `401`. Nothing in this client refreshes a session.

## Tests

`web-client/src/account/authorized-fetch.test.ts`, describe block `"a fetch under this browser's
session"`. Use a recording double for `ApiFetch` and an in-memory `Storage`.

| Test | Proves |
| --- | --- |
| `adds a bearer header when this browser holds a session` | With `"pd.sessionToken"` set to `"tok-7"`, the wrapped fetch is called with `headers.Authorization === "Bearer tok-7"`, asserted against the **literal** string so a missing space or a lower-case scheme fails |
| `sends exactly the headers it was given when this browser holds none` | With an empty storage, `Object.keys(headers)` on the recorded call equals exactly what the caller passed — **no `Authorization` key at all**, asserted by key set rather than by an undefined value |
| `keeps the device id header the caller built` | With a token held **and** the caller passing `{"X-Device-Id": "d-1"}`, the recorded call carries both headers. Fails against a wrapper that replaces the header object instead of extending it |
| `passes the path, the method and the body through untouched` | A `PUT` with a body and a query-carrying path is recorded verbatim. Fails against a wrapper that rebuilds the init and drops a field |
| `reads the token on every call, not once` | Two calls through **one** wrapper: the first with no token, then the token is written, and the second carries it. Fails against a wrapper that captured the token when it was built — the defect that keeps a signed-out browser signed in |

Five tests in a new file: `npm run test -- src/account/authorized-fetch.test.ts` reports **5**.

## Acceptance criteria

- [ ] `a fetch under this browser's session > adds a bearer header when this browser holds a session`
      passes, asserting the literal `"Bearer tok-7"`
- [ ] `a fetch under this browser's session > sends exactly the headers it was given when this
      browser holds none` passes, asserting the **key set**
- [ ] `a fetch under this browser's session > keeps the device id header the caller built` passes
- [ ] `a fetch under this browser's session > passes the path, the method and the body through
      untouched` passes
- [ ] `a fetch under this browser's session > reads the token on every call, not once` passes with
      **one** wrapper and **two** calls
- [ ] `grep -c 'localStorage' web-client/src/account/authorized-fetch.ts` returns `0`
- [ ] `npm run test -- src/account/authorized-fetch.test.ts` reports `Tests  5 passed (5)`
- [ ] No file outside the two listed differs
- [ ] Every command in `verify:` exits 0

## Proof

1. Read the token once, when the wrapper is built.
   **`reads the token on every call, not once` reddens alone.** Every other test in the file passes,
   because each builds a fresh wrapper after writing its storage — which is exactly how this defect
   ships. Run it first. Revert.
2. Build the headers as `{ Authorization: … }` rather than `{ ...headers, Authorization: … }`.
   **`keeps the device id header the caller built` reddens alone.** Revert.
3. Emit `Authorization: token` with no `"Bearer "` prefix.
   **`adds a bearer header when this browser holds a session` reddens**, and only because it asserts
   the literal. Assert `headers.Authorization !== undefined` instead and watch it pass under the
   mutation — that variant is worth running once, because a presence check is the shape this test
   usually gets written in.
4. Set `Authorization: undefined` instead of omitting the key when no token is held.
   **`sends exactly the headers it was given when this browser holds none` reddens**, and only
   because it compares the key set. A value assertion (`toBeUndefined`) passes under the mutation,
   and a header whose value is the string `"undefined"` is what some fetch implementations then send.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.
