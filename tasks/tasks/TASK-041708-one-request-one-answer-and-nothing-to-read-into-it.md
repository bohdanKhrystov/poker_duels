---
schema: 2
id: TASK-041708
title: One request, one answer, and nothing to read into it
type: task
status: done
parent: STORY-0417
module: web-client
estimate: XS
tier: haiku
review: standard
files_touched: 2
labels: [client, account, recovery, http]
depends_on: [TASK-041706]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test -- src/account/forgot-password.test.ts 2>&1 | grep -qE 'Test Files +1 passed \(1\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- src/account/forgot-password.test.ts 2>&1 | grep -qE 'Tests +4 passed \(4\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'sends the address in a body and never in the path'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'has exactly two outcomes, and every documented status is the first of them'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'carries no device id and no authorization header'
  - test "$(grep -oiE 'unknown|registered|no such|not found' web-client/src/account/forgot-password.ts | wc -l | tr -d ' ')" = 0
  - test "$(grep -oF 'X-Device-Id' web-client/src/account/forgot-password.ts | wc -l | tr -d ' ')" = 0
  - test "$(grep -oF 'kind:' web-client/src/account/forgot-password.ts | wc -l | tr -d ' ')" = 4
  - cd web-client && npm run check
---

## Goal

`forgotPassword` posts an address to `POST /api/auth/forgot-password` and can report exactly two
things — *the request was accepted* and *the request did not go through* — because the server
answers `202` in every case and a client that modelled more would rebuild the oracle it closed.

## Files

| File | Action |
| --- | --- |
| `web-client/src/account/forgot-password.ts` | create |
| `web-client/src/account/forgot-password.test.ts` | create |

Read, and do not edit:

- `web-client/src/account/sign-in.ts` — the module this is shaped after for an **unauthenticated**
  call: `headers: {}`, no device id, no bearer, one `catch`, no retry.
- `web-client/src/profile/api.ts` — `ApiFetch` and `ApiResponse`.
- `docs/protocol.md` *Forgot password* — one field, one documented status, and the note saying a
  client may not infer anything from it.
- [`ADR-0031`](../../docs/adr/ADR-0031-an-optional-verified-recovery-email.md) §5 — the `202` is
  written before any mail work and delivery runs detached, so latency does not vary with whether an
  address matched.

## Scope

- **Two outcomes, and there is no third:**

  ```ts
  export type ForgotPasswordOutcome =
    | { readonly kind: "accepted" } // 202 — and 202 is the only thing this endpoint says
    | { readonly kind: "failed" }; // any other status, or a fetch that rejected
  ```

  `failed` means *this request did not reach the server*, never *that address is not known*. The KDoc
  says so in those words.
- **The signature takes only what it sends:**

  ```ts
  export async function forgotPassword(request: {
    readonly fetch: ApiFetch;
    readonly address: string;
  }): Promise<ForgotPasswordOutcome>;
  ```

  **No `storage` parameter.** The endpoint takes no authentication, so this module has nothing to
  read; a `storage` it never used would invite somebody to use it.
- **`headers: {}` and a body of `{ address }`, exactly as typed.** No `X-Device-Id`, no
  `Authorization` — a `verify:` line pins the first at zero, and `TASK-041711` binds this call
  through `plainFetch` for the same reason `signIn` is bound that way.
- **One `catch`, returning `failed`.** No retry: `ADR-0079` admits ten attempts a minute per remote
  address and an over-budget attempt still counts.
- **KDoc naming `ADR-0031` §5** and stating the rule this module exists to keep: the client learns
  nothing about the address from the answer, and neither does anyone watching the response time.

## Out of scope

- **The form that calls this, its words and its door.** `DEC-081` was the product owner's and
  `ADR-0087` answered it: a door on the sign-in screen, **no screen and no slug**. All of it belongs
  to the ticket that ADR unblocked. This module was written before the answer because it is settled
  either way: one endpoint, one field, one status, all in merged documentation.
- **Any sentence.** The acknowledgement is copy and belongs with that form.
- **Retrying, or reporting a `429`.** The endpoint has no `429`; `ADR-0079` puts the budget behind an
  answer that never changes.
- **Wiring it anywhere.** `TASK-041711` puts it on the seam; nothing calls it until the held ticket.

## Tests

`web-client/src/account/forgot-password.test.ts`, new. **Four tests.**

```ts
const ADDRESS = "zqx-address-zqx";
```

| Test | Proves |
| --- | --- |
| `sends the address in a body and never in the path` | One `202` call. The path is exactly `"/api/auth/forgot-password"`; the parsed body is exactly `{ address: ADDRESS }` by `toEqual`; and the path does not contain `ADDRESS`. Presence before absence |
| `has exactly two outcomes, and every documented status is the first of them` | Four calls: `202` → `accepted`; `400`, `429` and `500` → `failed`. The three non-`202` cases are asserted to produce **identical** outcome objects by `toEqual`, so nothing downstream can tell them apart. `ADR-0031` §5's rule, held in the type and in the test |
| `carries no device id and no authorization header` | The recorded headers object has **no** `X-Device-Id` key and **no** `Authorization` key — asserted by key, not by value, so an empty string would fail too. Then, in the same test, storage is seeded with a device id and a session token and the call is repeated: the headers are still bare. Two inputs, because a module that never reads storage and a module whose storage happened to be empty are indistinguishable from one run |
| `answers accepted without reading a body, and failed without throwing` | A `202` whose `json()` rejects still answers `accepted` — the module never calls it, and the endpoint's body is empty. A `fetch` that rejects answers `failed` and the returned promise does not reject |

**No `try` anywhere in the added test code, and no `expect()` inside one.**

## Acceptance criteria

- [ ] `cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'sends the address in a body and never in the path'`
      — passes, body by `toEqual`, path asserted twice
- [ ] `cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'has exactly two outcomes, and every documented status is the first of them'`
      — passes, with the three failing statuses asserted **identical to each other**
- [ ] `cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'carries no device id and no authorization header'`
      — passes, over both an empty storage and a seeded one
- [ ] `cd web-client && NO_COLOR=1 npm run --silent test -- src/account/forgot-password.test.ts 2>&1 | grep -qE 'Tests +4 passed \(4\)'`
      and `… | grep -qE 'Test Files +1 passed \(1\)'` — **exactly four**. Both lines, because a
      collection error prints a *passing* `Tests` count with no failure line at all
- [ ] `test "$(grep -oF 'kind:' web-client/src/account/forgot-password.ts | wc -l | tr -d ' ')" = 4`
      — two in the type, two in the returns. A fifth is a third outcome, which is the whole defect
      this ticket forecloses
- [ ] `test "$(grep -oiE 'unknown|registered|no such|not found' web-client/src/account/forgot-password.ts | wc -l | tr -d ' ')" = 0`
      — no identifier, comment or KDoc sentence in this module describes an address as known or
      unknown. Reads the whole file, so write the KDoc without those words
- [ ] `test "$(grep -oF 'X-Device-Id' web-client/src/account/forgot-password.ts | wc -l | tr -d ' ')" = 0`
      — the endpoint takes no authentication and this module sends none
- [ ] `cd web-client && npm run check` exits 0. The whole-suite total is deliberately not pinned:
      this ticket and `TASK-041707`, `TASK-041709`, `TASK-041710` have pairwise disjoint `Files`
      tables and may be dispatched in one batch
- [ ] No file outside the two listed differs
- [ ] Every command in `verify:` exits 0

## Proof

Run each step, record what you measured, and revert it. **These are experiments, not changes.**

1. **Add a third outcome** — map `400` to `{ kind: "unknown-address" }`. Predict: `has exactly two
   outcomes…` reddens on the identity assertion, and the `kind:` count gate goes to 5. Record both.
   This is the mutation the whole ticket exists to make impossible.
2. **Move the address into the path.** Predict: `sends the address in a body and never in the path`
   reddens on both its path assertions.
3. **Add `X-Device-Id`** read from a storage the module would have to take. Predict: it will not
   compile, because the signature has no `storage`. Record the `tsc` message — that is the design,
   not an obstacle. Then add the parameter as well and confirm `carries no device id and no
   authorization header` reddens on the **seeded** half and not on the empty half. That asymmetry is
   why the test drives two inputs.
4. **Retry once on a rejected fetch.** Predict: nothing reddens, because no test counts calls here.
   Record that: it is a real hole, and the honest response is a follow-up ticket, not a silent fix
   outside this budget.
5. **Vacuity check**: make the `202` case return `failed`. Predict: three of the four tests redden.
   If fewer do, say which and why.

> **Expect `The 'NO_COLOR' env is ignored due to the 'FORCE_COLOR' env being set.` on stderr.** It is
> wrong — measured, 0 escape bytes with it and 74 without — so every `grep -qE` needs it.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.

## Notes

**This module was written while its screen was held, and that is deliberate.** `DEC-081` asked what
the product calls the *ask for a reset* flow, where its door is, and therefore its slug; `ADR-0087`
answered it with a door on the sign-in screen and **no slug at all**. None of that changes one line
of this file: the endpoint, its single field and its single status are merged in `docs/protocol.md`
and `ADR-0031` §5. Writing the safe half first was `STORY-0415`'s pattern — write what the answer
cannot touch, hold what it determines — and the prediction held.

**Step 4's predicted hole is recorded rather than closed.** `TASK-041707` counts calls because
`ADR-0079` budgets that endpoint at five a minute; this one is budgeted at ten and every attempt
counts too, so a call-count assertion would be worth having. It is not added here because the fourth
test already carries the reject path and a fifth test on an `XS` ticket is the kind of growth that
turns an `XS` into an unreviewed `S`. If step 4 shows something other than *nothing reddens*, say so.
