---
schema: 2
id: TASK-041710
title: A reset takes a token and a password, and comes back with no session
type: task
status: backlog
parent: STORY-0417
module: web-client
estimate: S
tier: haiku
review: deep
files_touched: 2
labels: [client, account, recovery, http, security]
depends_on: [TASK-041706]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test -- src/account/reset-password.test.ts 2>&1 | grep -qE 'Test Files +1 passed \(1\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- src/account/reset-password.test.ts 2>&1 | grep -qE 'Tests +6 passed \(6\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'puts the token and the new password in a body and in no path, header or query'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'tells a refused password from a dead link, and reports each as itself'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'stores no session on success, because the server issues none'
  - test "$(grep -oF '?' web-client/src/account/reset-password.ts | wc -l | tr -d ' ')" = 0
  - test "$(grep -oF 'writeSessionToken' web-client/src/account/reset-password.ts | wc -l | tr -d ' ')" = 0
  - test "$(grep -oiE 'setItem|sessionStorage|localStorage|console\.' web-client/src/account/reset-password.ts | wc -l | tr -d ' ')" = 0
  - cd web-client && npm run check
---

## Goal

`resetPassword` posts a mailed token and a new password to `POST /api/auth/reset-password`, reports
the refused password and the dead link as two different things, and stores **nothing** on success —
because the server issues no session and returns no token, and a client that expected one is a
client that hangs.

## Files

| File | Action |
| --- | --- |
| `web-client/src/account/reset-password.ts` | create |
| `web-client/src/account/reset-password.test.ts` | create |

Read, and do not edit:

- `web-client/src/account/sign-in.ts` — the one module in this client that **does** store a token, so
  that the contrast is deliberate rather than an omission. This ticket does the opposite and says so.
- `web-client/src/profile/api.ts` — `ApiFetch` and `ApiResponse`.
- `docs/protocol.md` *Reset password* — three statuses, two fields, and the two notes: the token is
  accepted only in a request body, and **no session is issued and no token is returned**.
- [`ADR-0080`](../../docs/adr/ADR-0080-the-password-is-judged-before-the-token-is-touched.md) §2 and
  §Consequences — the `422` is answered **without the token being looked at**, so it proves nothing
  about the link, and the same link works on the next submission while it lives.
- [`ADR-0031`](../../docs/adr/ADR-0031-an-optional-verified-recovery-email.md) §4 — every
  `auth_session` row for that player is deleted in the same transaction, and the reset issues none.

## Scope

- **Four outcomes:**

  ```ts
  export type ResetPasswordOutcome =
    | { readonly kind: "reset" } // 204 — the password is changed and every session is gone
    | { readonly kind: "link-dead" } // 400 — unknown, expired or already used; indistinguishable
    | { readonly kind: "password-refused" } // 422 — under 8 or over 128 code points
    | { readonly kind: "failed" }; // anything else, or a fetch that rejected
  ```

- **The signature takes only what it sends, and no `storage`:**

  ```ts
  export async function resetPassword(request: {
    readonly fetch: ApiFetch;
    readonly token: string;
    readonly newPassword: string;
  }): Promise<ResetPasswordOutcome>;
  ```

  **A `storage` parameter is refused here on purpose.** There is nothing to write — no session comes
  back — and a module holding a `Storage` is a module somebody will later have write a token into.
- **Path `"/api/auth/reset-password"`, a fixed literal, no interpolation.** Body
  `{ token, newPassword }`, both exactly as given. `headers: {}`.
- **`reset` does not mean *signed in*.** Its KDoc says the player must sign in afterwards, on this
  device too, and names `ADR-0031` §4. The screen that acts on it is `TASK-041718`'s.
- **`password-refused` does not mean *the link is good*.** Its KDoc names `ADR-0080` §2: the `422` is
  answered before the token is touched, so it is byte-identical for a live token, an expired one and
  a string the caller invented. Nothing here may treat it as evidence about the link.
- **One `catch`, returning `failed`, never rethrowing and never retrying.** The token is single-use by
  construction; a retry after a `204` spends nothing but tells the player the link is dead.

## Out of scope

- **Reading the token from the address, or clearing the fragment.** `TASK-041701` and `TASK-041702`.
- **Sending the player anywhere.** `ADR-0083` §1 fixes the destination at ***Sign in*** / `#/sign-in`
  and `TASK-041719` performs the navigation; this module returns a value.
- **Any component or sentence.** `TASK-041718`.
- **Writing, reading or clearing a session token.** A `verify:` line pins `writeSessionToken`,
  `setItem`, `sessionStorage`, `localStorage` and `console.` at zero.
- **Wiring it anywhere.** `TASK-041711`.

## Tests

`web-client/src/account/reset-password.test.ts`, new. **Six tests.**

```ts
const TOKEN = "zqx-reset-token-zqx";
const NEW_PASSWORD = "zqx-new-password-zqx";
```

| Test | Proves |
| --- | --- |
| `puts the token and the new password in a body and in no path, header or query` | One `204` call. Path exactly `"/api/auth/reset-password"`; body exactly `{ token: TOKEN, newPassword: NEW_PASSWORD }` by `toEqual`; the path contains neither literal nor `"?"`; and **no header value contains either**, over `Object.values(headers)`. Presence before absence |
| `tells a refused password from a dead link, and reports each as itself` | Two calls: `422` → `password-refused`, `400` → `link-dead`, asserted to be **different** outcome objects. `ADR-0080` reversed the order these arrive in, and `STORY-0417`'s criterion is that the two render different, actionable sentences — which is impossible if the transport collapses them |
| `does not read a refused password as a live link` | A `422` answer, then — with **no new token** — a `204` answer for the same token and a longer password, asserting `reset`. `ADR-0080` §Consequences: a `422` costs no link and the same link still works. The test is that this module holds no state between calls and draws no conclusion from the first |
| `maps every status the endpoint documents to its own outcome` | Four calls in one test — `204`, `400`, `422`, `500` — asserting all four outcomes. Every arm, because a `switch` with one arm right passes any subset |
| `stores no session on success, because the server issues none` | A `204` whose body carries `{ "sessionToken": "zqx-token-zqx" }`. A real in-memory `Storage` is created, handed to nothing, and asserted `length === 0` afterwards; the outcome is `reset`. **The fixture body deliberately offers a token the module must ignore** — a module that stored one would pass a test whose response body was empty. This is the story's *never expects a token back* |
| `answers reset without reading a body, and failed without throwing` | A `204` whose `json()` rejects still answers `reset`. A `fetch` that rejects answers `failed` and the returned promise does not reject |

**No `try` anywhere in the added test code, and no `expect()` inside one.**

## Acceptance criteria

- [ ] `cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'puts the token and the new password in a body and in no path, header or query'`
      — passes, with the header sweep over `Object.values` and the `"?"` assertion
- [ ] `cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'tells a refused password from a dead link, and reports each as itself'`
      — passes, asserting the two outcomes differ
- [ ] `cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'does not read a refused password as a live link'`
      — passes, second call succeeding with the **same** token
- [ ] `cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'stores no session on success, because the server issues none'`
      — passes, over a `204` body that **does** carry a `sessionToken`
- [ ] `cd web-client && NO_COLOR=1 npm run --silent test -- src/account/reset-password.test.ts 2>&1 | grep -qE 'Tests +6 passed \(6\)'`
      and `… | grep -qE 'Test Files +1 passed \(1\)'` — **exactly six**. Both lines, because a
      collection error prints a *passing* `Tests` count with no failure line at all
- [ ] `test "$(grep -oF '?' web-client/src/account/reset-password.ts | wc -l | tr -d ' ')" = 0`
      — no question mark anywhere: no query, no ternary, no optional parameter, no optional chaining,
      no question in a comment. `ADR-0081` §2's rule is absolute
- [ ] `test "$(grep -oF 'writeSessionToken' web-client/src/account/reset-password.ts | wc -l | tr -d ' ')" = 0`
      and `test "$(grep -oiE 'setItem|sessionStorage|localStorage|console\.' web-client/src/account/reset-password.ts | wc -l | tr -d ' ')" = 0`
      — nothing is stored and nothing is logged. Both read the whole file, comments included
- [ ] `cd web-client && npm run check` exits 0. The whole-suite total is deliberately not pinned:
      this ticket and `TASK-041707`, `TASK-041708`, `TASK-041709` have pairwise disjoint `Files`
      tables and may be dispatched in one batch
- [ ] No file outside the two listed differs
- [ ] Every command in `verify:` exits 0

## Proof

Run each step, record what you measured, and revert it. **These are experiments, not changes.**

1. **Store the token from the `204` body** — read `sessionToken` and call `writeSessionToken`.
   Predict: it will not compile, because the signature has no `storage`. Record the `tsc` message.
   Then **add** the parameter and store it: predict `stores no session on success, because the server
   issues none` reddens on `storage.length`. Two halves, because the signature is the first guard and
   the test is the second.
2. **Collapse `422` into `link-dead`.** Predict: `tells a refused password from a dead link…` reddens
   **and** `maps every status…` reddens. Record both. This is the mutation that makes a player who
   typed a short password go and ask for a new mail.
3. **Collapse `400` into `password-refused`.** Predict: the same two redden. Run it as well as step 2
   — a single collapse in one direction can be masked by a test that only checks the other.
4. **Remember the `422`** — add a module-level flag that returns `link-dead` on any call after a
   `422`. Predict: `does not read a refused password as a live link` reddens alone. This is the exact
   defect `ADR-0080` §Consequences warns `STORY-0417` about.
5. **Move the token into a query.** Predict: the first test reddens **and** the `?` gate fails.
6. **Vacuity check on the storage assertion**: in the fifth test, delete the `sessionToken` from the
   fixture body and re-run step 1's second half. Predict: the test goes **green** with the module
   storing a token, because there is nothing in the body to store. Restore the fixture and confirm it
   reddens. A fixture value at the boundary the bug leaves unchanged can never detect it.

> **A red run names a prefix, not a set.** Vitest stops reporting past its first hard failure.
>
> **Expect `The 'NO_COLOR' env is ignored due to the 'FORCE_COLOR' env being set.` on stderr.** It is
> wrong — measured, 0 escape bytes with it and 74 without — so every `grep -qE` needs it.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.

## Notes

**`review: deep` on a transport module**, which is unusual and is on purpose. Two of the four
properties here are the kind `CLAUDE.md` reserves that level for: a bearer secret that must reach no
URL, no storage and no log, and a pair of refusals whose collapse is invisible to every test that
does not assert both directions. The neighbouring `TASK-041709` is `standard` because it carries the
first property and not the second.

**Why the fifth test's fixture carries a `sessionToken` the endpoint never sends.** A `204` with an
empty body cannot tell a module that ignores a token from a module that would have stored one — the
value the bug needs simply is not there. Putting one in the fixture is what makes the assertion able
to fail, and step 6 is the measurement that proves it.
