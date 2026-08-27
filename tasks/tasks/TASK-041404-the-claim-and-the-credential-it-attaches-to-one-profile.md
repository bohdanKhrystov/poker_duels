---
schema: 2
id: TASK-041404
title: The claim, and the credential it attaches to exactly one profile
type: task
status: done
parent: STORY-0414
module: web-client
estimate: S
tier: sonnet
review: standard
files_touched: 2
labels: [client, e2e, test, auth]
depends_on: [TASK-041403]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test -- src/e2e/account-server.test.ts 2>&1 | grep -qE 'Tests +14 passed \(14\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- src/e2e/account-server.test.ts --reporter=verbose 2>&1 | grep -qF 'a claim attaches the credential to the device own player'
  - cd web-client && NO_COLOR=1 npm run --silent test -- src/e2e/account-server.test.ts --reporter=verbose 2>&1 | grep -qF 'a claim moves no coin and renames nobody'
  - cd web-client && NO_COLOR=1 npm run --silent test -- src/e2e/account-server.test.ts --reporter=verbose 2>&1 | grep -qF 'sign in issues a token for the player who claimed the handle'
  - cd web-client && NO_COLOR=1 npm run --silent test -- src/e2e/account-server.test.ts --reporter=verbose 2>&1 | grep -qF 'a wrong password and an unknown handle answer the same 401'
  - cd web-client && npm run check
---

## Goal

`POST /api/auth/sign-up` attaches a handle and password to the profile the device already owns, and
`POST /api/auth/sign-in` issues a session token naming that same profile.

## Why this exists

This is the hinge of `STORY-0414`: the credential created on browser A is the only thing that makes
browser B able to become A. If the double attached the credential to the wrong player, or minted a
token naming whoever asked, every later assertion would pass while proving the opposite.

`ADR-0030` §1 — *a claim adds a credential and moves nothing* — is the rule the double must obey and
the story's *"the name is asserted after the claim"* note depends on: sign-up names no column of
`player`, so the balance and the display name are byte-unchanged by it.

## Files

| File | Action |
| --- | --- |
| `web-client/src/e2e/account-server.ts` | modify |
| `web-client/src/e2e/account-server.test.ts` | modify |

Read, and do not edit: `web-client/src/account/sign-up.ts`; `web-client/src/account/sign-in.ts`;
`docs/adr/ADR-0030-a-claim-adds-a-credential-and-moves-nothing.md` §§1–2.

## Scope

- `POST /api/auth/sign-up` resolves the player from **`X-Device-Id` only** — `sign-up.ts:42` sends
  that header and no other, and returns `no-profile` without a request when the device id is absent.
  Answers `201` on success, `401` when no device resolves, `409` when the handle is already claimed.
- The credential is stored as `handle → { password, playerId }`. **The `playerId` is the device's own
  player, read at claim time** — never a parameter, never the handle.
- A claim changes `coinBalance`, `displayName` and `duels` **not at all**.
- `POST /api/auth/sign-in` reads `{ handle, password }` from the body and carries **no** authentication
  of its own (`sign-in.ts:49–56` sends `headers: {}`). A matching credential answers `200` with
  `{ sessionToken }`; anything else answers `401`.
- Tokens are minted from a counter, so two sign-ins are two distinct strings, and stored as
  `token → playerId`. The store is written here and **read** in `TASK-041405`.
- One place mints a token; the string is opaque to every caller and carries no player id in it.

## Out of scope

- A bearer token changing who `GET /api/me` answers for — `TASK-041405`. This ticket issues tokens
  and nothing reads them yet, which is exactly why `TASK-041405` can prove the precedence rule with a
  mutation that reddens.
- `400` and `422` handle/password refusals, and `429` throttling. `sign-up.ts` maps them and
  `TASK-041212`/`TASK-041219` proved that mapping; `STORY-0414` sends one good credential.
- Sign-out — `TASK-041405`.

## Tests

`account-server.test.ts` — five new, on top of nine.

| Test | Proves |
| --- | --- |
| `a claim attaches the credential to the device own player` | Claiming from device A then signing in with that handle yields a token whose stored player id is A's, **not** B's. Both players are claimable, so the right answer is not the only answer. |
| `a claim moves no coin and renames nobody` | `GET /api/me` before and after the claim is deep-equal — `ADR-0030` §1 as an assertion, over the whole body rather than over one field. |
| `a claim of a handle somebody already holds is refused` | A second device claiming the same handle gets `409`, and the credential still names the first player. |
| `sign in issues a token for the player who claimed the handle` | `200` with a `sessionToken` string, and two successive sign-ins return **different** strings — a constant token would satisfy a laxer assertion. |
| `a wrong password and an unknown handle answer the same 401` | Both bodies and both statuses are identical, so nothing downstream could tell them apart even if it tried. |

## Acceptance criteria

- [ ] `account-server.test.ts` `a claim attaches the credential to the device own player` passes
- [ ] `account-server.test.ts` `a claim moves no coin and renames nobody` passes
- [ ] `account-server.test.ts` `a claim of a handle somebody already holds is refused` passes
- [ ] `account-server.test.ts` `sign in issues a token for the player who claimed the handle` passes
- [ ] `account-server.test.ts` `a wrong password and an unknown handle answer the same 401` passes
- [ ] `NO_COLOR=1 npm run --silent test -- src/e2e/account-server.test.ts 2>&1 | grep -qE 'Tests +14 passed \(14\)'` exits 0
- [ ] The nine tests from `TASK-041402` and `TASK-041403` pass unchanged — no assertion edited or removed
- [ ] `npm run check` exits 0
- [ ] Every command in `verify:` exits 0

## Proof

1. **The claim must be attachable to the wrong player.** Make sign-up store the *other* player's id
   and re-run: `a claim attaches the credential to the device own player` must redden. Both fixture
   players must be claimable for this mutation to be meaningful — if only one has a device id in the
   fixture, the mutation has nowhere to go and a green run means nothing.
2. Make sign-up also set `coinBalance` to the same number it already is. `a claim moves no coin and
   renames nobody` must stay **green** — it is asserting a value, not the absence of a write. Then
   set it to a different number: it must redden. Report both results.
3. Make the token a constant string: `sign in issues a token for the player who claimed the handle`
   must redden on its two-tokens-differ half.
4. Answer `403` instead of `401` for a wrong password: `a wrong password and an unknown handle answer
   the same 401` must redden. If it stays green it is comparing the two answers to each other and not
   to the status the merged `sign-in.ts` maps — `sign-in.ts:58` returns `refused` on `401` and
   **returns before** the `!== 200` branch below it, so a status that is neither is a different
   outcome entirely.

## Definition of done

Standard, per [`tasks/README.md`](../README.md).
