---
schema: 2
id: TASK-041707
title: Attaching an address costs the current password, and the answer says nothing
type: task
status: backlog
parent: STORY-0417
module: web-client
estimate: S
tier: haiku
review: standard
files_touched: 2
labels: [client, account, recovery, http]
depends_on: [TASK-041706]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test -- src/account/attach-recovery-email.test.ts 2>&1 | grep -qE 'Test Files +1 passed \(1\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- src/account/attach-recovery-email.test.ts 2>&1 | grep -qE 'Tests +6 passed \(6\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'sends the address and the current password in a body, and neither in the path'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'maps every status the endpoint documents to its own outcome'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'sends nothing at all when this browser holds no device id'
  - test "$(grep -oF 'localStorage' web-client/src/account/attach-recovery-email.ts | wc -l | tr -d ' ')" = 0
  - test "$(grep -oF 'writeSessionToken' web-client/src/account/attach-recovery-email.ts | wc -l | tr -d ' ')" = 0
  - test "$(grep -oF 'catch' web-client/src/account/attach-recovery-email.ts | wc -l | tr -d ' ')" = 1
  - cd web-client && npm run check
---

## Goal

`attachRecoveryEmail` sends an address and the caller's current password to
`POST /api/auth/recovery-email` in a request body, and reports one outcome per status the endpoint
documents — never retrying, never storing anything, and never putting either value in a path.

## Files

| File | Action |
| --- | --- |
| `web-client/src/account/attach-recovery-email.ts` | create |
| `web-client/src/account/attach-recovery-email.test.ts` | create |

Read, and do not edit:

- `web-client/src/account/sign-up.ts` — the module this is shaped after: `readDeviceId`, no id means
  no request, a `switch` over documented statuses, one `catch`, no retry.
- `web-client/src/account/sign-up.test.ts` — the test idiom, including its `fetch` double.
- `web-client/src/profile/api.ts` — `ApiFetch` and `ApiResponse`, the only fetch shapes here.
- `docs/protocol.md` *Recovery email* — the four statuses and the two body fields, verbatim.
- [`ADR-0031`](../../docs/adr/ADR-0031-an-optional-verified-recovery-email.md) §3 — why the current
  password is required even inside a valid session, and §5 — why a `202` says nothing.

## Scope

- **One outcome per documented status, and one for everything else:**

  ```ts
  export type AttachRecoveryOutcome =
    | { readonly kind: "accepted" } // 202
    | { readonly kind: "address-refused" } // 400 — decode failed, or not syntactically an address
    | { readonly kind: "no-profile" } // 401 — absent, blank or unknown identity
    | { readonly kind: "password-refused" } // 403 — the current password is wrong
    | { readonly kind: "failed" }; // anything else, or a fetch that rejected
  ```

- **The signature mirrors `signUp`'s**, taking its dependencies rather than reaching for them:

  ```ts
  export async function attachRecoveryEmail(request: {
    readonly fetch: ApiFetch;
    readonly storage: Storage;
    readonly address: string;
    readonly currentPassword: string;
  }): Promise<AttachRecoveryOutcome>;
  ```

- **`X-Device-Id` from `readDeviceId(request.storage)`, and no id means no request** — the server
  would answer `401` and that answer is already known. `ADR-0027` lets a bearer token outrank the
  device id, and the bearer is added by `authorizedFetch` **above** this module, which is why nothing
  here reads a session token.
- **The body is `{ address, currentPassword }`, exactly as typed.** No trim, no fold, no
  normalisation: `ADR-0078` §Decision has `emailAddressOrNull` return the input unchanged, and a
  client that trimmed would send a different string from the one the player can see.
- **One `catch`, returning `failed`, never rethrowing and never retrying.** `ADR-0079` budgets this
  endpoint at five a minute and an over-budget attempt still counts, so a retry spends a budget this
  caller cannot see.
- **KDoc** naming `ADR-0031` §3 for the password, `ADR-0031` §5 for why `202` is not *recovery is on*,
  and stating that this module writes to storage **not at all**.

## Out of scope

- **Any component, any sentence, any layout.** `TASK-041713`.
- **Reading, writing or clearing a session token or a device id.** A `verify:` line pins
  `writeSessionToken` at zero and `localStorage` at zero.
- **Detaching an address.** `DELETE /api/auth/recovery-email` exists on the server
  (`TASK-041623`) and no screen in this story offers it; not yet ticketed.
- **Distinguishing the three things a `202` can mean.** The server refuses to, and a client that
  guessed would rebuild the oracle it closed.
- **Wiring it anywhere.** `TASK-041711`.

## Tests

`web-client/src/account/attach-recovery-email.test.ts`, new. **Six tests.** The fixture uses two
distinctive literals so a path check cannot pass by coincidence:

```ts
const ADDRESS = "zqx-address-zqx";
const CURRENT = "zqx-current-zqx";
```

| Test | Proves |
| --- | --- |
| `sends the address and the current password in a body, and neither in the path` | One `202` call. The recorded path is exactly `"/api/auth/recovery-email"`; the parsed body is exactly `{ address: ADDRESS, currentPassword: CURRENT }` — asserted with `toEqual`, so a third key fails; the path contains neither literal; and the `X-Device-Id` header carries the stored id. **Presence before absence**: the body assertion proves both values really did travel, so the path assertions are over something |
| `maps every status the endpoint documents to its own outcome` | Five calls in one test — `202`, `400`, `401`, `403` and `500` — asserting `accepted`, `address-refused`, `no-profile`, `password-refused` and `failed`. All five, in one test, because a `switch` with one arm right and four wrong passes any subset |
| `sends nothing at all when this browser holds no device id` | With empty storage: the outcome is `no-profile` and the `fetch` double was called **zero** times. The count is the assertion; the outcome alone would pass for a module that asked and got a `401` |
| `sends the address exactly as it was given` | One call with an address carrying a leading space, an uppercase letter and a trailing dot — asserted byte-for-byte in the body. The guard against a client that trims, folds or normalises what `ADR-0078` says arrives unchanged |
| `answers failed when the fetch rejects, and does not throw` | A `fetch` double that rejects. The returned outcome is `failed` and the call does not reject. Then, in the same test, a double whose `json` rejects — the module never calls `json`, so this must still be `accepted`, which is what proves the module does not read a body the endpoint documents as empty |
| `sends one request and never a second` | A `403` answer. The `fetch` double was called exactly once. `ADR-0079`'s budget counts refusals, so a retry costs the player an attempt they cannot see |

**No `try` anywhere in the added test code, and no `expect()` inside one** — a failing assertion is
itself a throw, and a `try` around one turns a red test green. Use `await expect(...).resolves` or
capture the promise's value; never wrap an assertion in `try`/`catch`.

## Acceptance criteria

- [ ] `cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'sends the address and the current password in a body, and neither in the path'`
      — passes, with the body compared by `toEqual` and the path asserted to contain neither literal
- [ ] `cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'maps every status the endpoint documents to its own outcome'`
      — passes, over all five statuses in one test
- [ ] `cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'sends nothing at all when this browser holds no device id'`
      — passes, asserting the call count is **zero**
- [ ] `cd web-client && NO_COLOR=1 npm run --silent test -- src/account/attach-recovery-email.test.ts 2>&1 | grep -qE 'Tests +6 passed \(6\)'`
      and `… | grep -qE 'Test Files +1 passed \(1\)'` — **exactly six**. Both lines, because a
      collection error prints a *passing* `Tests` count with no failure line at all
- [ ] `test "$(grep -oF 'localStorage' web-client/src/account/attach-recovery-email.ts | wc -l | tr -d ' ')" = 0`
      and `test "$(grep -oF 'writeSessionToken' web-client/src/account/attach-recovery-email.ts | wc -l | tr -d ' ')" = 0`
      — this module reaches for no global and writes no storage. Both read the whole file, comments
      included
- [ ] `test "$(grep -oF 'catch' web-client/src/account/attach-recovery-email.ts | wc -l | tr -d ' ')" = 1`
      — one, around the request. A second is a swallowed failure somewhere it cannot be seen
- [ ] `cd web-client && npm run check` exits 0. The whole-suite total is deliberately not pinned:
      this ticket and `TASK-041708`, `TASK-041709`, `TASK-041710` have pairwise disjoint `Files`
      tables and may be dispatched in one batch
- [ ] No file outside the two listed differs
- [ ] Every command in `verify:` exits 0

## Proof

Run each step, record what you measured, and revert it. **These are experiments, not changes**, and
both files are inside this ticket's budget.

1. **Move the address into the path** — request `` `/api/auth/recovery-email/${request.address}` ``.
   Predict: `sends the address and the current password in a body, and neither in the path` reddens
   on the path-equality assertion **and** on the contains assertion. Record both; if only one moves,
   say which.
2. **Drop `currentPassword` from the body.** Predict: the same test reddens on the `toEqual`. A
   `toMatchObject` would not see this, which is why the ticket specifies `toEqual`.
3. **Collapse `403` into `failed`** — delete that arm. Predict: `maps every status the endpoint
   documents to its own outcome` reddens **alone**, and the screen would show *that did not go
   through* to a player who simply mistyped their password.
4. **Answer `accepted` for `400`.** Predict: the same test reddens. Two mutations on one test because
   a five-status assertion should redden for each arm independently.
5. **Delete the no-device-id early return.** Predict: `sends nothing at all when this browser holds
   no device id` reddens on the **call-count** assertion. Note that the outcome assertion may stay
   green if the double answers `401` — record whether it does, because that is the difference between
   a count and an outcome, and it is the reason both are asserted.
6. **Trim the address** — `request.address.trim()`. Predict: `sends the address exactly as it was
   given` reddens alone.
7. **Retry once on `403`.** Predict: `sends one request and never a second` reddens alone.
8. **Vacuity check on the reject path.** In `answers failed when the fetch rejects, and does not
   throw`, make the double resolve normally instead of rejecting. Predict: the assertion still passes
   for the wrong reason. Restore it and confirm the outcome really is driven by a rejection — the
   epic has shipped a `catch` that turned a throw into an outcome nobody was checking.

> **A red run names a prefix, not a set.** Vitest stops reporting past its first hard failure.
>
> **Expect `The 'NO_COLOR' env is ignored due to the 'FORCE_COLOR' env being set.` on stderr.** It is
> wrong — measured, 0 escape bytes with it and 74 without — so every `grep -qE` needs it.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.

## Notes

**Why this module sends only `X-Device-Id` when the endpoint accepts a bearer token too.** That is the
merged composition: `readFromApi` sends the device id and `main.tsx` wraps it in `authorizedFetch`,
which reads the session token on **every** call and adds `Authorization` when one is held. `ADR-0027`
makes the bearer outrank the device id on the server. Building the branch here would put a second
copy of that rule in a module that cannot see whether the token is live. `TASK-041711` binds this call
through `apiFetch` — the wrapped one — and says so explicitly, because binding it through
`plainFetch` would leave a signed-in browser authenticating as its device.

`grep -c` counts matching **lines** and exits **1** on zero matches, so every zero-expectation above
is wrapped as `test "$(… | wc -l | tr -d ' ')" = 0`.
