---
schema: 2
id: TASK-041709
title: A token from the mailbox, in a body and never in a path
type: task
status: backlog
parent: STORY-0417
module: web-client
estimate: S
tier: haiku
review: standard
files_touched: 2
labels: [client, account, recovery, http, security]
depends_on: [TASK-041706]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test -- src/account/verify-email.test.ts 2>&1 | grep -qE 'Test Files +1 passed \(1\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- src/account/verify-email.test.ts 2>&1 | grep -qE 'Tests +5 passed \(5\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'puts the token in a body and in no path, header or query'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'maps every status the endpoint documents to its own outcome'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'writes the token to no storage and to no log'
  - test "$(grep -oF '?' web-client/src/account/verify-email.ts | wc -l | tr -d ' ')" = 0
  - test "$(grep -oF 'localStorage' web-client/src/account/verify-email.ts | wc -l | tr -d ' ')" = 0
  - test "$(grep -oiE 'setItem|sessionStorage|console\.' web-client/src/account/verify-email.ts | wc -l | tr -d ' ')" = 0
  - cd web-client && npm run check
---

## Goal

`verifyEmail` posts the token from a mailed link to `POST /api/auth/verify-email` in a request body,
maps the three answers that endpoint documents, and never lets the token reach a path, a header, a
query, storage or a log.

## Files

| File | Action |
| --- | --- |
| `web-client/src/account/verify-email.ts` | create |
| `web-client/src/account/verify-email.test.ts` | create |

Read, and do not edit:

- `web-client/src/account/sign-in.ts` — the unauthenticated-call shape: `headers: {}`, one `catch`,
  no retry, no storage read.
- `web-client/src/profile/api.ts` — `ApiFetch` and `ApiResponse`.
- `docs/protocol.md` *Verify email* — three statuses, one field, and the sentence saying `400` covers
  unknown, expired and already-consumed **indistinguishably**.
- [`ADR-0081`](../../docs/adr/ADR-0081-a-mailed-link-is-a-fragment-route-and-the-token-is-the-segment-behind-the-slug.md)
  §2 — *"a recovery link contains no `?` at all"*, and the token is a segment **of the fragment**, so
  it reaches no server, no access log, no proxy record and no `Referer`.
- [`ADR-0031`](../../docs/adr/ADR-0031-an-optional-verified-recovery-email.md) §4 — the token's whole
  point, and §5's `409` case.

## Scope

- **Four outcomes:**

  ```ts
  export type VerifyEmailOutcome =
    | { readonly kind: "verified" } // 204 — the address is now attached
    | { readonly kind: "link-dead" } // 400 — unknown, expired or already used; indistinguishable
    | { readonly kind: "address-taken" } // 409 — already verified to another player
    | { readonly kind: "failed" }; // anything else, or a fetch that rejected
  ```

  `link-dead`'s KDoc records that it covers **three** server states and that the client must not
  claim which, because `ADR-0031` §5 makes them one answer on purpose.
- **The signature takes only what it sends:**

  ```ts
  export async function verifyEmail(request: {
    readonly fetch: ApiFetch;
    readonly token: string;
  }): Promise<VerifyEmailOutcome>;
  ```

  No `storage`. The endpoint takes no authentication, and a token that could be stored is a token
  that will be.
- **Path `"/api/auth/verify-email"`, a fixed literal, with no interpolation of any kind.** Body
  `{ token }`, exactly as read from the fragment. `headers: {}`.
- **One `catch`, returning `failed`, never rethrowing and never retrying.** A retry on a `204` would
  spend a single-use token; a retry on a `400` spends nothing and tells nobody anything.
- **KDoc** naming `ADR-0031` §4 for why the token is in a fragment and never a query, and stating
  that this module writes to storage **not at all** and logs **nothing** — the token is a bearer
  secret and `console.error` is a surface too.

## Out of scope

- **Reading the token from the address.** `tokenFromHash` is `TASK-041701`'s and the screen calls it;
  this module is handed a string and never asks where it came from.
- **Clearing the fragment.** `TASK-041702`'s `clearToken`.
- **Any component or sentence.** `TASK-041716`.
- **Storing the token, even briefly.** A `verify:` line pins `setItem`, `sessionStorage`,
  `localStorage` and `console.` at zero across the whole file.
- **Wiring it anywhere.** `TASK-041711`.

## Tests

`web-client/src/account/verify-email.test.ts`, new. **Five tests.** The token literal is distinctive
and is the one this story's other tests use, so a leak is unambiguous:

```ts
const TOKEN = "zqx-verify-token-zqx";
```

| Test | Proves |
| --- | --- |
| `puts the token in a body and in no path, header or query` | One `204` call. The path is exactly `"/api/auth/verify-email"`; the parsed body is exactly `{ token: TOKEN }` by `toEqual`; the path contains neither `TOKEN` nor `"?"`; and **no header value contains `TOKEN`**, asserted over `Object.values(headers)` rather than over two names somebody remembered. Presence before absence: the body assertion runs first |
| `maps every status the endpoint documents to its own outcome` | Four calls in one test — `204`, `400`, `409`, `500` — asserting `verified`, `link-dead`, `address-taken`, `failed`. All four, because a `switch` with one arm right passes any subset |
| `writes the token to no storage and to no log` | A real in-memory `Storage` is created and handed to **nothing** — then, after the call, `storage.length` is `0`. `console.log`, `console.warn` and `console.error` are spied, and the joined output contains no `TOKEN`. The spies are asserted to have been installed by logging a sentinel through one of them **before** the call and finding it, so the sweep is over something rather than over an inert spy |
| `sends one request and never a second` | A `400` answer, then a `204` answer in a second call. The double records exactly one call per invocation. A single-use token retried is a token spent twice |
| `answers verified without reading a body, and failed without throwing` | A `204` whose `json()` rejects still answers `verified` — the endpoint's body is empty and this module never calls `json`. A `fetch` that rejects answers `failed` and the returned promise does not reject |

**No `try` anywhere in the added test code, and no `expect()` inside one** — a sweep in this
repository shipped with its assertions inside a `try`, and planting a forbidden key left all six of
its tests green.

## Acceptance criteria

- [ ] `cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'puts the token in a body and in no path, header or query'`
      — passes, with the header sweep over `Object.values` and the `"?"` assertion
- [ ] `cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'maps every status the endpoint documents to its own outcome'`
      — passes, over all four statuses in one test
- [ ] `cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'writes the token to no storage and to no log'`
      — passes, with the console spies proven live by a sentinel before the absence is asserted
- [ ] `cd web-client && NO_COLOR=1 npm run --silent test -- src/account/verify-email.test.ts 2>&1 | grep -qE 'Tests +5 passed \(5\)'`
      and `… | grep -qE 'Test Files +1 passed \(1\)'` — **exactly five**. Both lines, because a
      collection error prints a *passing* `Tests` count with no failure line at all
- [ ] `test "$(grep -oF '?' web-client/src/account/verify-email.ts | wc -l | tr -d ' ')" = 0`
      — **no question mark anywhere in the file**: not in a query string, not in an optional
      parameter, not in a ternary, not in optional chaining, and not in a comment. `ADR-0081` §2
      makes the no-`?` rule absolute because a conditional one erodes; write the module without one
- [ ] `test "$(grep -oiE 'setItem|sessionStorage|console\.' web-client/src/account/verify-email.ts | wc -l | tr -d ' ')" = 0`
      and `test "$(grep -oF 'localStorage' web-client/src/account/verify-email.ts | wc -l | tr -d ' ')" = 0`
      — the token is never stored and never logged. Both read the whole file, comments included
- [ ] `cd web-client && npm run check` exits 0. The whole-suite total is deliberately not pinned:
      this ticket and `TASK-041707`, `TASK-041708`, `TASK-041710` have pairwise disjoint `Files`
      tables and may be dispatched in one batch
- [ ] No file outside the two listed differs
- [ ] Every command in `verify:` exits 0

## Proof

Run each step, record what you measured, and revert it. **These are experiments, not changes.**

1. **Move the token into the path** — `` `/api/auth/verify-email/${request.token}` ``. Predict: `puts
   the token in a body and in no path, header or query` reddens on the path equality **and** on the
   contains check. Record both.
2. **Move it into a query** — `"/api/auth/verify-email?token=" + request.token`. Predict: the same
   test reddens, **and** the `?` gate in `verify:` fails. Run the gate as well as the test, and
   record which caught it first.
3. **Put it in a header** — `{ "X-Token": request.token }`. Predict: the same test reddens on its
   header sweep. This is the mutation a two-header-name check would miss, which is why the assertion
   is over `Object.values`.
4. **Collapse `409` into `link-dead`.** Predict: `maps every status the endpoint documents to its own
   outcome` reddens alone. The player would be told to ask for a new link when the real answer is
   that the address belongs to somebody else.
5. **Log the token** — add `console.error("verify", request.token)`. Predict: `writes the token to no
   storage and to no log` reddens, **and** the `console.` gate fails. Both, and record both: the epic
   has shipped a plain-text gate that a comment satisfied and a sweep that a `try` disarmed.
6. **Vacuity check on the log sweep**: delete the sentinel that proves the spies are live, then apply
   step 5 again with the spies mocked to no-ops. Predict: the sweep goes green with the token being
   logged. Restore the sentinel and confirm it reddens again. A green sweep with the mutation in place
   is the finding, not the reassurance.

> **A red run names a prefix, not a set.** Vitest stops reporting past its first hard failure.
>
> **Expect `The 'NO_COLOR' env is ignored due to the 'FORCE_COLOR' env being set.` on stderr.** It is
> wrong — measured, 0 escape bytes with it and 74 without — so every `grep -qE` needs it.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.

## Notes

**The `?` gate forbids more than a query string, and that is intended.** It also forbids a ternary,
an optional parameter and optional chaining anywhere in this file. That is a real constraint on how
the module is written, and it is accepted because `ADR-0081` §2 chose an **absolute** rule over a
conditional one for exactly this reason: *"the edit that breaks it is the deletion of two characters
from a string that still looks entirely ordinary."* A one-character audit is worth a slightly plainer
module. The same gate cannot tell code from prose, so the KDoc must not ask a question either.

**`link-dead` is one outcome for three server states and must stay one.** `ADR-0031` §5 makes unknown,
expired and already-consumed indistinguishable at the endpoint. A client that split them would be
inventing information, and `ADR-0081` §6 says the client *"cannot tell a live token from a spent one,
and it must not learn."*
