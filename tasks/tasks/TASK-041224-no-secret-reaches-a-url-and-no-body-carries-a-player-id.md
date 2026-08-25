---
schema: 2
id: TASK-041224
title: No secret reaches a URL, and no request body carries a player id
type: task
status: backlog
parent: STORY-0412
module: web-client
estimate: S
tier: sonnet
review: deep
files_touched: 2
labels: [client, account, security]
depends_on: [TASK-041223]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test -- src/account/no-secret-in-a-url.test.ts 2>&1 | grep -qE 'Tests +5 passed \(5\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'puts no handle, password or token in any path it requests'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'sends no player id in any body it writes'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'leaves no secret in the address bar after any of the four calls'
  - cd web-client && npm run check
---

## Goal

`STORY-0412`'s last criterion becomes an exit code: driving all four account calls with distinctive
secrets and checking every path, every query, every fragment and every body for them.

## Files

| File | Action |
| --- | --- |
| `web-client/src/account/no-secret-in-a-url.test.ts` | create |
| `web-client/src/account/no-secret-in-a-url.md` | create |

The second file is the sweep's own note: which surfaces it covers, which it cannot, and why — the
same shape `profile-no-derivation.test.tsx` carries in its header comment, promoted to a file because
this one is cited by `STORY-0414`. Read, and do not edit: all four modules under
`web-client/src/account/`; `web-client/src/profile/profile-no-derivation.test.tsx` (the sweep to
follow); `EPIC-04`'s *Non-negotiables*.

## Scope

- One test file that drives `signUp`, `signIn`, `signOut` and `revokeThisDevice` through recording
  doubles, using secrets chosen so a substring search cannot give a false negative:
  `handle = "zqx-handle-zqx"`, `password = "zqx-password-zqx"`, `token = "zqx-token-zqx"`.
- For every recorded call, the assertion is over **`path` in full** — a path carries its own query
  string in this client, so one check covers both — and over `window.location.href` after the call.
  None may contain any of the three secrets.
- For every recorded **body**, the parsed object's keys are checked against a forbidden set:
  `playerId`, `player_id`, `deviceId`, `id`. `EPIC-04`'s non-negotiable is that a request body
  carrying a `playerId` the server did not resolve is a defect.
- The device id and the session token are checked too: neither may appear in a path, a query or the
  address bar. Both are bearer credentials.
- A `console` sweep: a spy on `console.log`, `console.warn` and `console.error` records nothing
  containing any of the three secrets across all four calls.
- The note file states plainly what this **cannot** see: it drives the four modules directly, so it
  says nothing about a future caller that builds its own URL, and it cannot observe what a real
  browser puts in a `Referer`.

## Out of scope

- **The server side.** `TASK-041620` already gates *token in a body, never in a URL* on the server,
  and `docs/protocol.md` fixes the contract. This is the client's half.
- **`?room=`.** It is a room code, not a secret, and `roomLink` is untouched by this story.
- **The recovery links.** `ADR-0081` puts a token in a fragment segment and `STORY-0417` owns the
  screens that read it. A sweep written here would fail the day that lands, for a reason this ticket
  did not decide. **A refusal, not an omission** — the note file says so.
- Changing any production file. If the sweep goes red, the fix is a new ticket, because a change made
  to pass a sweep is a change nobody reviewed against its own criteria.

## Tests

`web-client/src/account/no-secret-in-a-url.test.ts`, describe block `"no secret reaches a URL"`.

| Test | Proves |
| --- | --- |
| `puts no handle, password or token in any path it requests` | All four calls driven; every recorded `path` checked against all three secrets. Twelve checks, asserted so the failure names the call and the secret |
| `sends no player id in any body it writes` | Every recorded body that exists is parsed and its keys checked against the four forbidden names. The two calls that send **no** body are asserted to send none, so a body appearing later is caught |
| `leaves no secret in the address bar after any of the four calls` | `window.location.href` after each call contains none of the three. The fragment is included, which is where `ADR-0081` will later put a token and where this client must never put one of these |
| `puts no secret in anything it logs` | The three `console` spies record nothing containing any of the three secrets |
| `sends the device id and the session token in headers and nowhere else` | With both stored, neither value appears in any recorded `path`, in any body, or in `window.location.href` — and at least one recorded call **does** carry each in a header, so the test proves they travelled rather than that they were never used |

Five tests in a new file: `npm run test -- src/account/no-secret-in-a-url.test.ts` reports **5**.

## Acceptance criteria

- [ ] `no secret reaches a URL > puts no handle, password or token in any path it requests` passes,
      over all four calls and all three secrets
- [ ] `no secret reaches a URL > sends no player id in any body it writes` passes, including the
      assertion that two calls send no body at all
- [ ] `no secret reaches a URL > leaves no secret in the address bar after any of the four calls`
      passes
- [ ] `no secret reaches a URL > puts no secret in anything it logs` passes
- [ ] `no secret reaches a URL > sends the device id and the session token in headers and nowhere
      else` passes, **including** the positive half
- [ ] `web-client/src/account/no-secret-in-a-url.md` names what the sweep cannot see, in three
      bullets: a future caller that builds its own URL, a real browser's `Referer`, and
      `ADR-0081`'s fragment token
- [ ] `git diff --stat web-client/src/account/sign-up.ts web-client/src/account/sign-in.ts web-client/src/account/sign-out.ts web-client/src/account/revoke-device.ts`
      is empty
- [ ] `npm run test -- src/account/no-secret-in-a-url.test.ts` reports `Tests  5 passed (5)`
- [ ] No file outside the two listed differs
- [ ] Every command in `verify:` exits 0

## Proof

1. In `sign-in.ts`, append `?handle=${request.handle}` to the path.
   **`puts no handle, password or token in any path it requests` reddens**, naming sign-in and the
   handle. Revert immediately — this is a mutation, not a fix.
2. In `sign-up.ts`, add `playerId: "p-1"` to the body.
   **`sends no player id in any body it writes` reddens.** Then add `player_id` instead: it reddens
   too, and that second spelling is why the forbidden set has four names rather than one.
3. In `sign-out.ts`, `console.warn("signing out", token)`.
   **`puts no secret in anything it logs` reddens.** A log line is the surface nobody thinks of, and
   `ADR-0077`'s server-side reasoning about a recipient reaching a log is the same argument one tier
   up.
4. Remove the `Authorization` header from `revoke-device.ts` altogether.
   **`sends the device id and the session token in headers and nowhere else` reddens on its positive
   half**, and the three negative sweeps all still pass — a secret that is never sent trivially never
   leaks. That is the whole reason the positive half is in the test: a sweep with only negatives is
   green against a client that does nothing.
5. Set the secrets to short common strings — `handle = "a"`, `password = "b"`.
   **Nothing reddens under mutation 1**, because `"a"` appears in `/api/auth/sign-in` anyway and the
   substring search cannot tell. Run this once and record it: the distinctive `zqx-` prefixes are the
   only thing that makes a substring sweep mean anything, and a later reader will otherwise simplify
   them away.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.
