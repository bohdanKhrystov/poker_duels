---
schema: 2
id: TASK-041212
title: Sign-up, seven outcomes, and the one refusal that is about nobody
type: task
status: done
parent: STORY-0412
module: web-client
estimate: S
tier: haiku
review: standard
files_touched: 2
labels: [client, account, auth]
depends_on: [TASK-041211]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test -- src/account/sign-up.test.ts 2>&1 | grep -qE 'Tests +7 passed \(7\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'maps every status the endpoint documents to its own outcome'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'keeps a throttled refusal apart from a broken server'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'sends the handle and the password and nothing else'
  - cd web-client && npm run check
---

## Goal

`POST /api/auth/sign-up` has a client, it sends exactly two fields, and a `429` is its own outcome
rather than the generic failure `ADR-0056` §1 was written to prevent.

## Files

| File | Action |
| --- | --- |
| `web-client/src/account/sign-up.ts` | create |
| `web-client/src/account/sign-up.test.ts` | create |

Read, and do not edit: `docs/protocol.md` *Sign up*; `web-client/src/profile/set-name.ts` (the
outcome-union shape and the no-device-id guard to follow);
[`ADR-0056`](../../docs/adr/ADR-0056-a-throttled-sign-up-says-so-and-keeps-what-was-typed.md) §1.

## Scope

- One export, in `set-name.ts`'s shape:

  ```ts
  export type SignUpOutcome =
    | { readonly kind: "signed-up" }        // 201
    | { readonly kind: "handle-refused" }   // 400
    | { readonly kind: "unavailable-handle" } // 409
    | { readonly kind: "password-refused" } // 422
    | { readonly kind: "no-profile" }       // 401
    | { readonly kind: "throttled" }        // 429
    | { readonly kind: "failed" };          // anything else, or a fetch that rejected

  export async function signUp(request: {
    readonly fetch: ApiFetch;
    readonly storage: Storage;
    readonly handle: string;
    readonly password: string;
  }): Promise<SignUpOutcome>;
  ```

- Authenticates with `X-Device-Id` read from the injected storage. **No device id, no request** — the
  server would answer `401` and that answer is already known, exactly as `setDisplayName` does.
- The body is `JSON.stringify({ handle, password })` and carries **nothing else**: no player id, no
  device id, no display name, no address. `docs/protocol.md` says so, and `EPIC-04`'s
  non-negotiables make a body carrying a `playerId` a defect.
- Both strings go out **exactly as typed** — no trim, no case fold, no normalisation. The server
  folds the handle (`ADR-0031` §1) and stores the password as-is (`ADR-0048` §1), and a client that
  pre-folded would refuse strings the server accepts.
- `429` maps to `throttled` and **nothing else does**: a `500`, a `503` and a rejected `fetch` are
  `failed`. `ADR-0056` §1 is explicit that this applies to sign-up only.
- One request per call: no retry of any status, ever (`ADR-0056` §3).
- KDoc naming each status and the ADR that fixed it.

## Out of scope

- **Rendering anything.** `TASK-041218` and `TASK-041219` own the form and the throttled state.
- **Signing in afterwards.** `docs/protocol.md`: `201` issues no session and the player signs in
  afterwards. A client that chained the two would hold a password in memory across two requests and
  would invent a flow no ADR describes. **A refusal, not an omission.**
- **Any `Authorization` header.** Sign-up authenticates as the device; `TASK-041223` binds it to the
  plain `window.fetch` for that reason.
- Validating the handle or the password before sending. `ADR-0048` §7 publishes the rules so a screen
  can *state* them; the verdict is the server's, and a second implementation of the fold is a second
  thing to keep true.

## Tests

`web-client/src/account/sign-up.test.ts`, describe block `"signing up"`. A recording `ApiFetch`
double and an in-memory `Storage`, as `set-name.test.ts` uses.

| Test | Proves |
| --- | --- |
| `maps every status the endpoint documents to its own outcome` | `201`, `400`, `401`, `409`, `422`, `429` each give their kind, asserted one by one in a table so the failure names which. Six statuses, and a `new Set` over the six kinds has size 6 |
| `keeps a throttled refusal apart from a broken server` | `429` is `throttled` while `500`, `503` and a `fetch` that rejects are each `failed` — **all four in one test**, which is `ADR-0056` §6's first criterion and which a mapping returning one constant cannot pass |
| `sends the handle and the password and nothing else` | The recorded body parses to an object whose sorted keys are exactly `["handle", "password"]`, with a handle and a password that differ from each other. No `playerId`, no `deviceId` |
| `sends what was typed, byte for byte` | A handle and a password each carrying surrounding whitespace and mixed case arrive unchanged in the body. **Two fields, two values**, so a trim applied to one is visible |
| `authenticates as the device this browser holds` | The recorded headers carry `X-Device-Id` equal to the stored id, and carry no `Authorization` key |
| `asks nothing of the server when this browser has no device id` | With an empty storage the outcome is `no-profile` and the recording double was **never called** |
| `sends one request and never a second` | After a `429`, the double's call count is exactly `1`. A count, not a spy on a timer: the retry `ADR-0056` §3 forbids is the one action that lengthens the wait |

Seven tests in a new file: `npm run test -- src/account/sign-up.test.ts` reports **7**.

## Acceptance criteria

- [ ] `signing up > maps every status the endpoint documents to its own outcome` passes over all six
      statuses, with a kind set of size 6
- [ ] `signing up > keeps a throttled refusal apart from a broken server` passes, asserting all four
      cases together
- [ ] `signing up > sends the handle and the password and nothing else` passes, asserting the **sorted
      key list**
- [ ] `signing up > sends what was typed, byte for byte` passes for both fields
- [ ] `signing up > authenticates as the device this browser holds` passes, asserting the absence of
      an `Authorization` key
- [ ] `signing up > asks nothing of the server when this browser has no device id` passes with a call
      count of `0`
- [ ] `signing up > sends one request and never a second` passes with a call count of `1`
- [ ] `grep -ci 'playerid' web-client/src/account/sign-up.ts` returns `0`
- [ ] `npm run test -- src/account/sign-up.test.ts` reports `Tests  7 passed (7)`
- [ ] No file outside the two listed differs
- [ ] Every command in `verify:` exits 0

## Proof

1. Delete the `429` branch so it falls to `default`.
   **`keeps a throttled refusal apart from a broken server` reddens** on the `429` row and `maps
   every status…` reddens on the set size. This is the missing branch `ADR-0056` §Context calls
   *"invisible in review"*, and it is what these two tests exist for. Revert.
2. Map `503` to `throttled` as well.
   **`keeps a throttled refusal apart from a broken server` reddens on the `503` row alone.** The
   `429` row still passes, which is why that test asserts all four together rather than one per `it`.
3. `handle: request.handle.trim().toLowerCase()`.
   **`sends what was typed, byte for byte` reddens on the handle** and the password half still
   passes. Run it: pre-folding looks like helpfulness and it refuses strings the server would accept,
   which `ADR-0048` §1 spent a section on.
4. Add `playerId` to the body.
   **`sends the handle and the password and nothing else` reddens** on the sorted key list. A test
   asserting `body.handle === …` and `body.password === …` **passes** under this mutation — run that
   variant, because it is the shape this test is usually written in and it cannot see an extra field.
5. Retry once on `429`.
   **`sends one request and never a second` reddens alone**, and the outcome is still `throttled`, so
   nothing else in the file moves.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.

## Notes

**The `Set(...).size` check is dominated here, and the reasoning matters more than the verdict.** The
coder argued the per-status assertions *"would all pass under collision"*, leaving the set size as the
only real gate. That is backwards, and the review established why: the fixture's expected kinds are
**literals**, one distinct kind per status. So any collision makes at least one status return
something other than its written expectation, and that per-status assertion reddens directly.

Given six distinct literal expectations for six statuses, **no collision exists that the literals miss
and the set catches**. The reviewer offered 400 and 409 both answering `"unavailable-handle"` as the
case the set would catch alone — but the fixture expects `"handle-refused"` for 400, so that
assertion reddens too. The set assertion is kept because the ticket names it and it costs nothing;
it is recorded here as **dominated** so nobody later reads it as the thing holding the property up.

The claim would have been true under one condition — expectations read from the mapping under test —
and that condition is exactly what would have made the seven tests tautological. Worth keeping
straight: *the coder's claim being right would have been the defect.*

**The security property is structural, not textual.** No player-facing string appears in this diff at
all; outcomes are type discriminants and every message lives in `account-text.ts`, which carries its
own register-independent gate against a second field-specific constant. `409` maps to one
`unavailable-handle` outcome per `ADR-0031` §2, so a caller cannot tell a taken handle from any other
refusal.

**Two assertions carry more than they appear to.** `sends the handle and the password and nothing
else` is a **key-set equality** on sorted keys, so an extra field — a device id, a token — reddens it,
where a containment check would not. `sends what was typed, byte for byte` varies whitespace **and**
case across **both** fields; a single-field fixture cannot tell a trim from a pass-through.

**One weaker spot, non-blocking.** `authenticates as the device this browser holds` asserts
`headers["Authorization"]` is `undefined` rather than asserting the key set. A header present with an
empty value would pass. The acceptance criterion asks for absence of the key and this satisfies it as
written; the stronger form is the one `authorized-fetch.test.ts` uses, and it is the form to prefer
when this file is next touched.
