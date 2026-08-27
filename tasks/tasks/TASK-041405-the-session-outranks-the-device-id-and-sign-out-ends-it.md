---
schema: 2
id: TASK-041405
title: The session outranks the device id, and signing out ends it
type: task
status: backlog
parent: STORY-0414
module: web-client
estimate: S
tier: sonnet
review: deep
files_touched: 2
labels: [client, e2e, test, auth, identity]
depends_on: [TASK-041404]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test -- src/e2e/account-server.test.ts 2>&1 | grep -qE 'Tests +19 passed \(19\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- src/e2e/account-server.test.ts --reporter=verbose 2>&1 | grep -qF 'a bearer token outranks the device id on the profile read'
  - cd web-client && NO_COLOR=1 npm run --silent test -- src/e2e/account-server.test.ts --reporter=verbose 2>&1 | grep -qF 'a bearer token outranks the device id on the duels read'
  - cd web-client && NO_COLOR=1 npm run --silent test -- src/e2e/account-server.test.ts --reporter=verbose 2>&1 | grep -qF 'the device id still answers when no token is carried'
  - cd web-client && NO_COLOR=1 npm run --silent test -- src/e2e/account-server.test.ts --reporter=verbose 2>&1 | grep -qF 'signing out returns the browser to the device it holds'
  - cd web-client && npm run check
---

## Goal

A request carrying a live session token is answered for **that session's** player, whatever device id
it also carries; signing out ends the session and hands the browser back to its device.

## Why this exists

This is the single rule `STORY-0414` exists to demonstrate through a browser, and
[`ADR-0027`](../../docs/adr/ADR-0027-the-session-outranks-the-device-id.md) states it by name. It is
also the rule most easily inverted without anything going red, because the two identities usually
agree — which is exactly why the fixture must be built so they **disagree**.

`STORY-0412`'s design note and `connection.ts:40–42` both record that the client *keeps sending its
device id whether or not it holds a token*, so the server sees both on the same request and the
precedence is what decides. `authorized-fetch.ts:31` adds `Authorization` alongside the caller's
headers rather than replacing them, so this is not hypothetical: it is what every read from a
signed-in browser looks like.

## Files

| File | Action |
| --- | --- |
| `web-client/src/e2e/account-server.ts` | modify |
| `web-client/src/e2e/account-server.test.ts` | modify |

Read, and do not edit: `web-client/src/account/authorized-fetch.ts`;
`web-client/src/account/sign-out.ts`;
`docs/adr/ADR-0027-the-session-outranks-the-device-id.md` §1.

## Scope

- The private resolver from `TASK-041402` gains its first branch: an `Authorization: Bearer <token>`
  header naming a **live** session resolves to that session's player; otherwise `X-Device-Id`
  resolves as before; otherwise nothing resolves.
- A token that names no live session does **not** fall back to the device id — it resolves to nothing
  and the request is refused. A dead token is an answer, not an absence.
- The rule applies to `GET /api/me` and `GET /api/me/duels`. It applies to **neither**
  `POST /api/auth/sign-up` (device only, `TASK-041404`) nor `POST /api/auth/sign-in` (no auth at all).
- `POST /api/auth/sign-out` deletes the session its bearer token names and answers `204` — the only
  status `docs/protocol.md` documents for it, per `sign-out.ts`'s own contract comment. A sign-out
  carrying an unknown token also answers `204`.
- Signing out deletes **one** session. A second token for the same player, issued by another sign-in,
  is untouched.

## Out of scope

- Session expiry, the thirty days of `ADR-0027` §1, and any clock. Nothing in this story waits.
- `ADR-0050`'s revoke-this-device route. `revokeThisDevice` is wired into the harness in
  `TASK-041406` because `AccountCalls` requires it, and `STORY-0414` never calls it.
- Changing what `TASK-041404` stores. This ticket reads the token store; it does not rewrite it.

## Tests

`account-server.test.ts` — five new, on top of fourteen.

| Test | Proves |
| --- | --- |
| `a bearer token outranks the device id on the profile read` | One request carrying **both** B's device id and a token naming A answers with **A's** `playerId` and `coinBalance`. The two players differ in both fields, so the wrong answer is a different value, not a missing one. |
| `a bearer token outranks the device id on the duels read` | The same request shape against `/api/me/duels` answers A's `duelId`, not B's — the precedence is a property of the resolver, not of one route. |
| `the device id still answers when no token is carried` | The same browser, same device id, no `Authorization`: B's own player comes back. Without this the precedence test would pass for a double that always answers A. |
| `a token naming no live session is refused rather than falling back` | A made-up bearer token alongside a valid device id answers `401` — it does not quietly become the device's player. |
| `signing out returns the browser to the device it holds` | After `POST /api/auth/sign-out`, the same token answers `401`, and the device id alone answers B's own player again. |

## Acceptance criteria

- [ ] `account-server.test.ts` `a bearer token outranks the device id on the profile read` passes
- [ ] `account-server.test.ts` `a bearer token outranks the device id on the duels read` passes
- [ ] `account-server.test.ts` `the device id still answers when no token is carried` passes
- [ ] `account-server.test.ts` `a token naming no live session is refused rather than falling back` passes
- [ ] `account-server.test.ts` `signing out returns the browser to the device it holds` passes
- [ ] `NO_COLOR=1 npm run --silent test -- src/e2e/account-server.test.ts 2>&1 | grep -qE 'Tests +19 passed \(19\)'` exits 0
- [ ] The fourteen tests from `TASK-041402`–`TASK-041404` pass unchanged — no assertion edited or removed
- [ ] `npm run check` exits 0
- [ ] Every command in `verify:` exits 0

## Proof

Run all four. Report the result of each, including any that does not do what this section predicts —
a failed prediction here is the finding, not a nuisance.

1. **Invert the precedence** — try the device id first and the token second. `a bearer token outranks
   the device id on the profile read` and `a bearer token outranks the device id on the duels read`
   must both redden, and `the device id still answers when no token is carried` must stay **green**.
   If the third reddens as well, its request is carrying a token it should not be.
2. **Make a dead token fall back to the device id.** `a token naming no live session is refused
   rather than falling back` must redden alone.
3. **Make sign-out delete every session rather than one.** No test here reddens — the fixture has one
   session. Before concluding that is unguarded, check the mutation is on a path a fixture reaches:
   add a second sign-in for the same player in a scratch copy and confirm the mutation *then*
   reddens. If it does, say so and leave the scope alone; the second session is `EPIC-04`'s, not this
   story's. Do not add a test to `TASK-041405` to cover it.
4. **Delete the not-equal half** of `a bearer token outranks the device id on the profile read` and
   confirm the remaining assertion still fails under mutation 1. If it passes, the test was resting on
   the deleted line and the two fixture players are too similar.

## Definition of done

Standard, per [`tasks/README.md`](../README.md).
