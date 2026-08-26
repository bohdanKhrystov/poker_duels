---
schema: 2
id: TASK-041213
title: Sign-in stores the token, carries no credential of its own, and reloads the document
type: task
status: done
parent: STORY-0412
module: web-client
estimate: S
tier: sonnet
review: standard
files_touched: 2
labels: [client, account, auth]
depends_on: [TASK-041212]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test -- src/account/sign-in.test.ts 2>&1 | grep -qE 'Tests +7 passed \(7\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'answers a wrong password and an unknown handle identically'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'stores the token and leaves the device id exactly where it was'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'sends no device id and no authorization of its own'
  - cd web-client && npm run check
---

## Goal

`POST /api/auth/sign-in` has a client that stores the one token the server will ever hand it, sends
no credential of its own, and answers a wrong password exactly as it answers an unknown handle.

## Files

| File | Action |
| --- | --- |
| `web-client/src/account/sign-in.ts` | create |
| `web-client/src/account/sign-in.test.ts` | create |

Read, and do not edit: `docs/protocol.md` *Sign in*;
[`ADR-0027`](../../docs/adr/ADR-0027-the-session-outranks-the-device-id.md) §6;
[`ADR-0030`](../../docs/adr/ADR-0030-a-claim-adds-a-credential-and-moves-nothing.md) §8;
[`ADR-0075`](../../docs/adr/ADR-0075-the-mark-lives-as-long-as-the-absence-that-produced-it.md)
Consequences (the three uncleared presence fields);
`web-client/src/protocol/session-token.ts`.

## Scope

- One export:

  ```ts
  export type SignInOutcome =
    | { readonly kind: "signed-in" }  // 200, token stored
    | { readonly kind: "refused" }    // 401 — unknown handle or wrong password, indistinguishable
    | { readonly kind: "failed" };    // 400, anything else, or a fetch that rejected

  export async function signIn(request: {
    readonly fetch: ApiFetch;
    readonly storage: Storage;
    readonly reload: () => void;
    readonly handle: string;
    readonly password: string;
  }): Promise<SignInOutcome>;
  ```

- **No authentication of its own.** No `X-Device-Id`, no `Authorization` — `docs/protocol.md` says
  sign-in *"carries none of its own"*, because it is how a client obtains authentication.
- A `200` whose body carries a string `sessionToken` is written through `writeSessionToken`, and then
  `reload()` is called. A `200` whose body has no usable token is `failed` and **stores nothing**.
- **`reload` is injected**, and `main.tsx` passes `() => window.location.reload()`. It is the seam a
  test binds; a module that called `window.location.reload()` itself would be untestable under jsdom
  and would reach the real global.
- **A reload is how identity changes**, and it is deliberate. Identity is fixed at `Hello` for the
  life of a socket, so the socket must be replaced; and `ADR-0075` records that `rivalPresence`,
  `graceRemainingMillis` and `rivalReturned` are cleared at **no** store boundary and are unreachable
  only because a real navigation rebuilds `initialState()`. Reconnecting in place would carry three
  presence fields across an identity change. Carry that reason as a comment.
- **The stored device id is not touched** — not written, not cleared, not overwritten (`ADR-0030`
  §8). This module must not import `writeDeviceId`.
- One request per call. `401` is never retried: `ADR-0027` §6 makes an over-budget answer identical
  to a wrong password, so a retry spends a budget the client cannot see.

## Out of scope

- **Telling a wrong password from an unknown handle.** The server made them indistinguishable on
  purpose; a client that inferred a difference from timing, or that mapped them to two outcomes,
  would rebuild the enumeration oracle above the wire. **A refusal, not an omission** — the outcome
  union has one `refused` and a test below pins it.
- **Forgetting the remembered room here.** `TASK-041214` does it on sign-out, where `ADR-0030` §6's
  abandoned seat is the case that matters; a sign-in from the account screen cannot happen while this
  tab is seated, because the store outranks the address (`ADR-0076` §3).
- Any form, any field, any message. `TASK-041226` owns the sign-in form and is behind `DEC-077`.
- Refreshing the profile read by hand. The reload re-runs every read from a fresh boot.

## Tests

`web-client/src/account/sign-in.test.ts`, describe block `"signing in"`. A recording `ApiFetch`, an
in-memory `Storage`, and a counting `reload` double.

| Test | Proves |
| --- | --- |
| `stores the token and leaves the device id exactly where it was` | A `200` carrying `{"sessionToken":"tok-9"}`: `readSessionToken` answers `"tok-9"` and `readDeviceId` answers the value the storage held **before** the call. The second of `STORY-0412`'s four device-id assertions |
| `answers a wrong password and an unknown handle identically` | Two calls, both answered `401` with an empty body, produce outcomes that are **deeply equal** — and the module exports no other `401`-reachable kind. Fails against any client-side attempt to tell them apart |
| `sends no device id and no authorization of its own` | With **both** a device id and a session token in storage, the recorded headers contain neither `X-Device-Id` nor `Authorization`. The fixture holds both precisely so the test can see either leak |
| `sends the handle and the password and nothing else` | The recorded body's sorted keys are exactly `["handle", "password"]`, with two different values, sent byte for byte |
| `reloads the document once a session exists, and not before` | `reload` is called exactly once on the `200`, and **zero** times on the `401` and on the rejected fetch — all three in one test |
| `stores nothing when a 200 carries no token` | A `200` with `{}` gives `failed`, `readSessionToken` still answers `null`, and `reload` was not called |
| `sends one request and never a second` | After a `401`, the recording double's call count is exactly `1` |

Seven tests in a new file: `npm run test -- src/account/sign-in.test.ts` reports **7**.

## Acceptance criteria

- [ ] `signing in > stores the token and leaves the device id exactly where it was` passes, asserting
      both storage keys
- [ ] `signing in > answers a wrong password and an unknown handle identically` passes, comparing the
      two outcomes deeply
- [ ] `signing in > sends no device id and no authorization of its own` passes with **both** values
      present in the storage
- [ ] `signing in > sends the handle and the password and nothing else` passes, asserting the sorted
      key list
- [ ] `signing in > reloads the document once a session exists, and not before` passes, asserting
      all three cases
- [ ] `signing in > stores nothing when a 200 carries no token` passes
- [ ] `signing in > sends one request and never a second` passes with a call count of `1`
- [ ] `grep -cE 'writeDeviceId|window\.location' web-client/src/account/sign-in.ts` returns `0`
- [ ] `npm run test -- src/account/sign-in.test.ts` reports `Tests  7 passed (7)`
- [ ] No file outside the two listed differs
- [ ] Every command in `verify:` exits 0

## Proof

1. Send `X-Device-Id` on the request.
   **`sends no device id and no authorization of its own` reddens alone**, and only because the
   fixture writes a device id into the storage. Empty the storage in that test first and watch the
   mutation pass — the fixture holding both values is the whole reason this test can see anything.
   Revert.
2. Call `reload()` on every outcome rather than on `signed-in`.
   **`reloads the document once a session exists, and not before` reddens** on the `401` and the
   rejected-fetch counts. Nothing else moves — every other test in the file ignores `reload`. Revert.
3. Map `401` to a new `unknown-handle` kind when the handle looks well formed.
   **`answers a wrong password and an unknown handle identically` reddens**, because both fixtures
   answer `401` and the outcomes stop being equal. Run it: this is the *helpful* refinement that
   rebuilds the oracle the server spent a design closing.
4. Write the device id from the response — `writeDeviceId(storage, "d-new")` on the `200`.
   **`stores the token and leaves the device id exactly where it was` reddens** on the device id
   assertion alone. It is `ADR-0030` §8's named harm, and the grep criterion is the second gate on
   it.
5. Store the token before checking that it is a string — `writeSessionToken(storage, body.sessionToken as string)`.
   **`stores nothing when a 200 carries no token` reddens**, on the stored value and on the outcome.
   Without that test a `200` with `{}` would store the string `"undefined"` and every later request
   would carry `Bearer undefined`.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.

## Notes

**The pairwise equality assertion is dominated, and a mutation proved it rather than an argument.**
Test 2 asserts each refusal against the literal `{kind:"refused"}` **and** asserts the two outcomes
deeply equal. Proof step 3 — mutating `401` to `unknown-handle` for well-formed handles — moved
**both** fixtures to the same wrong kind, so the pairwise check passed while the two literals
reddened. Reviewer confirmed no mutation catches the pairwise alone: any divergence fails it *and* at
least one literal. It is kept because the ticket's Tests row asks for *deeply equal* as the stated
contract, and recorded here as documentation rather than as a gate.

**A coder caught its own mutation being unrealistic and redid it.** Proof step 1's first attempt
(`?? ""`, header always present) reddened even with empty storage, which is not the bug the step
describes. Rebuilt as the realistic version — header included only when a device id exists — it
reddens with the fixture and is **invisible with storage emptied**. That second measurement is the
finding: the mutation's visibility depends entirely on the fixture holding a device id.

**A ticket prediction that does not reproduce, and why it does not matter.** Proof step 5 says storing
before validating *"would store the string `undefined`"*. It throws a `TypeError` instead: the
in-memory `Storage` fake does not coerce `setItem`'s value the way a browser does. Rerun with the
type check removed entirely, the outcome assertion reddens cleanly. Review confirmed the divergence
touches **only** that prediction's prose — the implementation validates `typeof token !== "string"`
before storing, and no other test depends on coercion.

**Both assertion shapes here are the strong form.** The body is asserted by **key-set equality** on
sorted keys, so an extra field reddens it; the headers are asserted as an **empty key set**, so a
header present with any value — including an empty string — enlarges the array and fails. The weaker
`toBeUndefined()` form shipped in a sibling ticket and is recorded there as the spot to strengthen.

**The non-event half uses call counts, not retries.** *Reloads once a session exists, and not before*
asserts `toHaveBeenCalledTimes(0)` on both the refusal and the rejected-fetch paths. `waitFor` cannot
express *this did not happen* — it passes as soon as any retry succeeds.

**What these tests do not cover, and whose job it is.** None would fail if sign-out could not clear
what sign-in stored: nothing here calls anything sign-out shaped, and storage is only inspected
immediately after `signIn()`. `session-token.ts` owns the round-trip property that forgetting clears
what writing wrote; `TASK-041214` owns asserting sign-out calls that path at all.
