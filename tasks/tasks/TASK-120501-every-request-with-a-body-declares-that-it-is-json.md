---
schema: 2
id: TASK-120501
title: Every request with a body declares that it is JSON
type: task
status: ready
parent: STORY-1205
module: web-client
estimate: S
tier: sonnet
review: standard
files_touched: 3
labels: [qa, bug, high]
depends_on: []
verify:
  - cd web-client && ! grep -qF "(path, init) => window.fetch(path, init)" src/main.tsx
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF "declares application/json on a request that carries a body"
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF "sends no content type on a request with no body"
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF "does not overwrite a content type the caller set"
  - cd web-client && NO_COLOR=1 npm run --silent check
  - python3 .github/scripts/lint_tickets.py
---

## Goal

Every request this client sends with a body arrives at the server labelled `application/json`, so
`call.receive<T>()` decodes it instead of throwing — which today turns **every write in the
product** into an empty-bodied `400`.

## The defect

Round 1 of `/qa-cycle regression`, 2026-08-29, commit `fe4bbf2a`. Reported as `04-02`; reproduced
by hand by `qa-manager`; recorded in
[`STORY-1205`](../stories/STORY-1205-round-1-the-identity-write-path-and-the-presence-line.md).

`web-client/src/account/sign-up.ts` sends:

```ts
const response = await request.fetch("/api/auth/sign-up", {
  method: "POST",
  headers: { "X-Device-Id": deviceId },
  body: JSON.stringify({ handle: request.handle, password: request.password }),
});
```

No `Content-Type`. `fetch` therefore labels the body `text/plain;charset=UTF-8`, Ktor's
`call.receive<SignUpRequest>()` refuses it, and `AuthRoutes.kt` maps *every* decode failure to a
bare `400` — by design, so a stranger learns nothing. `sign-up.ts` maps `400` to `handle-refused`,
so the player is told their handle is malformed when it was not.

**All seven body-carrying calls have it**, so this is one defect with one fix and not seven
tickets: `sign-up.ts`, `sign-in.ts`, `verify-email.ts`, `attach-recovery-email.ts`,
`forgot-password.ts`, `reset-password.ts`, `profile/set-name.ts`.

**The mechanism is proven, not inferred.** Probes against the running server, each chosen so it
could not write a row:

| request | status |
| --- | --- |
| `POST /api/auth/sign-up`, real device id, no `Content-Type`, 5-char password | **`400`** |
| `POST /api/auth/sign-up`, real device id, `application/json`, same password | `422` — decoded, then judged |
| `POST /api/auth/sign-in`, no `Content-Type`, unknown handle | **`400`** |
| `POST /api/auth/sign-in`, `application/json`, unknown handle | `401` — decoded, then judged |
| `PUT /api/me/name`, real device id, `application/json` | `200` — the name was written |

**The server is correct. Do not change it.** The fix is entirely inside `web-client/`.

## Files

| File | Action |
| --- | --- |
| `web-client/src/profile/json-body-fetch.ts` | create |
| `web-client/src/profile/json-body-fetch.test.ts` | create |
| `web-client/src/main.tsx` | modify |

## Scope

- Add `jsonBodyFetch(fetch: ApiFetch): ApiFetch`, a wrapper in the shape `authorizedFetch` already
  established in `web-client/src/account/authorized-fetch.ts`. It adds
  `"Content-Type": "application/json"` **when, and only when, `init.body` is present**, and it
  spreads the caller's headers so nothing existing is lost.
- Wrap **both** `ApiFetch` values in `main.tsx`: the one inside `authorizedFetch(...)` at line ~55
  and `plainFetch` at line ~98. Both are the same raw `(path, init) => window.fetch(path, init)`
  and both must go through the wrapper. The `verify:` gate is the disappearance of that raw form.
- A caller that already set a content type keeps it — the wrapper adds, it does not overrule.

## Out of scope

- **Any file under `poker-server/`.** The server's `400` is deliberate and its tests pass; the
  probe table above shows it answering correctly the moment the header arrives.
- **Editing the seven request modules.** The whole point of the wrapper is that the transport
  declares the body once. If a reviewer prefers per-call-site headers, that is a different design
  and a different ticket — do not do half of each.
- **`SignUpForm.tsx`'s refusal copy.** It maps `400` to `handle-refused`, which is what
  `docs/protocol.md` says `400` means; once this lands, a `400` really is a bad handle. Not a
  defect, and not touched here.
- **The dead display-name path, the sign-in path and the recovery paths as separate work.** They
  are the same defect and this ticket repairs them all at once.

## Tests

`json-body-fetch.test.ts`. Three tests, and the names are load-bearing — the `verify:` block greps
for them exactly.

| Test | Proves |
| --- | --- |
| `declares application/json on a request that carries a body` | the header reaches the underlying fetch's `init.headers` |
| `sends no content type on a request with no body` | a `GET` with no body is untouched — **the second input, without which the first cannot tell a wrapper from a constant** |
| `does not overwrite a content type the caller set` | the wrapper adds, it does not overrule |

Assert the **literal** `"application/json"` in the test. Do not import a constant from the module
under test and compare it to itself: a wire format has to stay literal in the assertion or the
test proves only that a name equals itself.

Record the recorded `init` from a fake `ApiFetch`, the same way `authorized-fetch.test.ts` does.

## Acceptance criteria

- [ ] `json-body-fetch.test.ts.declares application/json on a request that carries a body` passes
- [ ] `json-body-fetch.test.ts.sends no content type on a request with no body` passes
- [ ] `json-body-fetch.test.ts.does not overwrite a content type the caller set` passes
- [ ] `src/main.tsx` contains no bare `(path, init) => window.fetch(path, init)`
- [ ] `npm run check` is green in `web-client/`
- [ ] Every command in `verify:` exits 0

**Manual reproduction, for the reviewer.** With the stack up, on a browser profile that has never
claimed: *Account* → *Give this profile a password* → a 3–32 char handle and an 8+ char password →
*Give this profile a password*. Before this ticket the screen answers with the handle-rule
sentence and `SELECT count(*) FROM credential` stays `0`. After it, the screen reads
`This profile now has a password.` and the row exists. This is not in `verify:` because
`ADR-0089` §2b forbids a `verify:` that waits on a QA case, and CI has no stack.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
