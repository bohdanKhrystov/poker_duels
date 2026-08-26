---
schema: 2
id: TASK-041206
title: Hello carries the session this browser holds, and the device id still never moves
type: task
status: done
parent: STORY-0412
module: web-client
estimate: S
tier: haiku
review: standard
files_touched: 2
labels: [client, auth, protocol]
depends_on: [TASK-041205]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'says hello with the session token it already holds'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'keeps sending the device id under a session, because the server ignores it'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'keeps the device id it holds when a welcome carries none'
  - cd web-client && npm run check
---

## Goal

`Hello` stops hard-coding `sessionToken: null` and carries whatever token this browser holds, so a
signed-in player's socket is the account and not the device — and the write-once rule that stops a
client abandoning a profile gets its first test.

## Files

| File | Action |
| --- | --- |
| `web-client/src/protocol/connection.ts` | modify |
| `web-client/src/protocol/connection.test.ts` | modify |

Read, and do not edit:
[`ADR-0030`](../../docs/adr/ADR-0030-a-claim-adds-a-credential-and-moves-nothing.md) §8;
[`ADR-0027`](../../docs/adr/ADR-0027-the-session-outranks-the-device-id.md) §1 and §5;
`web-client/src/protocol/session-token.ts`.

## Scope

- `openConnection`'s `Hello` sets `sessionToken: readSessionToken(options.storage)` in place of the
  literal `null`. Nothing else about the frame moves: `deviceId`, `protocolVersion` and the field
  order stay as they are.
- **The device id keeps going out whether or not a token exists** (`ADR-0030` §8). There is no
  conditional: the server ignores it under a session (`ADR-0027` §1), so the alternative buys nothing
  and its bug mode is the profile abandonment `ADR-0012` named. Replace the comment above the `Hello`
  with one saying so.
- The existing write-once guard — `if (message.deviceId !== null) writeDeviceId(...)` — stays exactly
  as written. Its comment currently says *"Unreachable until `TASK-040518`"*; that is no longer true
  once a session can resolve with no device, so the comment is corrected and the branch gains the
  test below.
- Identity is fixed at `Hello` for the life of a socket. Nothing here re-reads the token on a live
  socket, and nothing re-sends `Hello`.

## Out of scope

- **Reopening the socket when the token changes.** Sign-in and sign-out reload the document
  (`TASK-041213`, `TASK-041214`), which is the strongest form of *close the socket and reconnect* and
  the only one that also rebuilds `initialState()` — `ADR-0075` records that three presence fields
  are cleared at no boundary, and a reconnect in place would carry them across an identity change.
  **A refusal, not an omission.**
- Any change to `reconnecting.ts`. Each attempt is a fresh `openConnection`, so a reconnect already
  re-reads storage and sends the current token.
- `PROTOCOL_VERSION`. `Hello.sessionToken` has been on the wire since `STORY-0405`; this ticket fills
  a field that already exists (`ADR-0030` §7). A criterion greps for it.

## Tests

`web-client/src/protocol/connection.test.ts`, inside the existing `describe("the connection")`.

| Test | Proves |
| --- | --- |
| `says hello with the session token it already holds` | With `"pd.sessionToken"` set to `"tok-7"` before the socket opens, the decoded `Hello` carries `sessionToken: "tok-7"`. Fails against the shipped literal `null` |
| `keeps sending the device id under a session, because the server ignores it` | With **both** `"pd.deviceId"` and `"pd.sessionToken"` set, the one `Hello` carries both values. `ADR-0030` §8's rule, and the test that catches the tempting conditional |
| `keeps the device id it holds when a welcome carries none` | Storage holds `"d-1"`; a `Welcome` arrives with `deviceId: null`; the stored value is still `"d-1"` afterwards, and the connection's status reports `deviceId: null`. The write-once branch, previously unreachable and untested — and the fourth of `STORY-0412`'s four device-id assertions |
| `says hello with no session token when this browser holds none` | An empty storage still produces `sessionToken: null`, so the field is a read and not a fabrication |

## Acceptance criteria

- [ ] `the connection > says hello with the session token it already holds` passes
- [ ] `the connection > keeps sending the device id under a session, because the server ignores it`
      passes, asserting **both** fields of the same frame
- [ ] `the connection > keeps the device id it holds when a welcome carries none` passes, asserting
      the stored value **and** the reported status
- [ ] `the connection > says hello with no session token when this browser holds none` passes
- [ ] `the connection > says hello with no device id on a first visit` passes **unchanged** — it
      already asserts the whole frame including `sessionToken: null`
- [ ] Every other pre-existing test in `connection.test.ts` passes unchanged
- [ ] `git diff web-client/src/protocol/version.ts` is empty
- [ ] `grep -c 'sessionToken: null' web-client/src/protocol/connection.ts` returns `0`
- [ ] No file outside the two listed differs
- [ ] Every command in `verify:` exits 0

## Proof

1. Restore `sessionToken: null` in the `Hello`.
   **`says hello with the session token it already holds` reddens alone.** `says hello with no
   session token when this browser holds none` still passes, which is why both exist: the second one
   alone is satisfied by the shipped bug. Revert.
2. Send the device id only when no token is held — `deviceId: token === null ? readDeviceId(...) :
   null`.
   **`keeps sending the device id under a session, because the server ignores it` reddens alone**;
   `says hello with the device id it already holds` still passes, because that fixture holds no
   token. Run it: this conditional is the plausible tidy-up, it looks like a privacy improvement, and
   `ADR-0030` §8 says its bug mode is abandoning a profile.
3. Delete the `message.deviceId !== null` guard so a null `Welcome` id is written through.
   **`keeps the device id it holds when a welcome carries none` reddens on the stored value.**
   Nothing else in the file moves — `remembers the device id the server issued` passes either way,
   because its `Welcome` carries an id. That is what makes this branch worth its own test rather than
   an extra assertion on an existing one.
4. Have `openConnection` read the token once at module scope instead of per connection.
   **Nothing reddens**, because each test constructs a fresh module state. Record it: a per-module
   read is wrong for a reconnect after a sign-out and is caught by review here, not by this suite.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.

## Notes

**The title's invariant was checked by counting write sites, not by trusting the tests.** *The device
id never moves* is a claim over every place `connection.ts` could write it, and a test asserting that
`Hello` now carries a session token passes whether or not the device id survived beside it. The
reviewer enumerated the write sites directly rather than inferring coverage: there are **two** — the
outbound `Hello` frame and the inbound `Welcome` handler — and one test gates each. No third path
exists in the file, so the pair is sufficient rather than merely plausible.

**The mutation that matters is the tempting one, and it reddens one test alone.** Making the device
id conditional on there being no session — `deviceId: token === null ? readDeviceId(...) : null`,
which reads like a tidy-up, since the server ignores the device id once a session is present — is
caught by `keeps sending the device id under a session, because the server ignores it`. That test
puts **both** keys in storage and asserts the **complete frame**: a test asserting only
`sessionToken` would pass the mutation, and one asserting only `deviceId` from a session-less fixture
would never reach it.

**The negative claim is asserted synchronously.** `keeps the device id it holds when a welcome
carries none` reads the stored value directly rather than through `waitFor`/`findBy`, which retry
until a condition holds and therefore cannot express *this did not happen*. That distinction has
already produced one blind gate in this story.

**Token reading is not re-implemented here.** `connection.ts` calls `readSessionToken(options.storage)`
lazily and imports it once; the blank-token and byte-for-byte rules stay in `session-token.ts`. A
second copy would be a defect, and the injected-`Storage`, call-time access pattern is what keeps the
module loadable under Vitest in Node, where `localStorage` is `undefined` and an import-time read
fails naming no line of this file.
