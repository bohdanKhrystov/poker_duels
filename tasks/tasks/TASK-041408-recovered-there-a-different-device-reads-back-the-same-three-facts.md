---
schema: 2
id: TASK-041408
title: Recovered there — a different device id reads back the same balance, name and duel
type: task
status: backlog
parent: STORY-0414
module: web-client
estimate: S
tier: sonnet
review: deep
files_touched: 1
labels: [client, e2e, test, auth, identity]
depends_on: [TASK-041407]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test -- src/e2e/claimed-here-recovered-there.test.tsx 2>&1 | grep -qE 'Tests +4 passed \(4\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- src/e2e/claimed-here-recovered-there.test.tsx --reporter=verbose 2>&1 | grep -qF 'the second client holds its own device id and its own profile'
  - cd web-client && NO_COLOR=1 npm run --silent test -- src/e2e/claimed-here-recovered-there.test.tsx --reporter=verbose 2>&1 | grep -qF 'signs in from the second client and reads back the same balance name and duel'
  - cd web-client && npm run check
---

## Goal

The assertion this story exists for: a second browser, holding a **different** device id and no
shared storage, signs in and reads the first browser's balance, name and duel.

## Why this exists

This is `STORY-0414`'s definition of done and the epic's. Everything else in the story is how it is
made to pass.

It is also the assertion most easily made vacuous. The second browser must have a **real profile of
its own** — a different balance, a different name, a different duel — so that reading the *first*
browser's facts is a discriminating outcome rather than the only outcome available. That is why
`TASK-041407`'s server holds `player-seat-1` with values that differ in every field.

## Files

| File | Action |
| --- | --- |
| `web-client/src/e2e/claimed-here-recovered-there.test.tsx` | modify |

Read, and do not edit: `web-client/src/e2e/drive-arc.tsx`; `web-client/src/e2e/account-server.ts`;
`web-client/src/account/SignInForm.tsx`; `web-client/src/protocol/session-token.ts`;
`docs/adr/ADR-0027-the-session-outranks-the-device-id.md` §1.

## Scope

- Browser B is a **second `inMemoryStorage()`**, never `storageA` and never a copy of it. It earns its
  device id the same way A did: `driveScriptedDuel({ viewerSeat: 1, storage: storageB })`, which
  writes `device-seat-1` from the script's own `Welcome`.
- Assert the two device ids are **different**, read out of the two storages — the story's criterion
  *asserted, not arranged and assumed*. Assert also that `storageB` holds no session token yet.
- Boot B and assert it renders **its own** profile: `player-seat-1`'s name and balance, each asserted
  not equal to A's.
- Sign in: click `Account`, await it, click the `Sign in` door, await the `sign in to an account`
  form, fill `Handle` and `Password` with the credential `TASK-041407` claimed, submit.
- Assert `storageB` now holds a session token, and that `storageB`'s device id is **still**
  `device-seat-1` — `ADR-0030` §8: sign-in never touches it.
- Boot B again with `wiring.signedIn = true`, which is what a document reload does. Assert the three
  facts:
  - **balance** — equal to the balance read from A's strip in `TASK-041407`, captured into a binding
    and compared for equality, not for being non-zero;
  - **name** — the name A set;
  - **duel** — matched by **identity**: `readDuelPage` through B's authorized fetch answers a page
    whose first row's `duelId` and `outcome` equal A's. The `duelId` is a React `key` in both
    `ProfileStrip` and `HistoryScreen` and reaches no DOM text, so this assertion is over the read.
    The rendered duel line is asserted too, as corroboration.

## Out of scope

- What B *sent* — `TASK-041409` owns the request-log sweep.
- Signing out — `TASK-041410`.
- Mounting A and B at the same time. `use-screen.ts`'s subscriber set and `window.location.hash` are
  module-global, so two mounted clients share one address. `cleanup()` between boots; A's facts
  travel in bindings, not in a live tree.

## Tests

`claimed-here-recovered-there.test.tsx` — two new, on top of `TASK-041407`'s two.

| Test | Proves |
| --- | --- |
| `the second client holds its own device id and its own profile` | Two different device ids, read from two storages; B's rendered name and balance are `player-seat-1`'s and are asserted not equal to A's; `storageB` holds no session token. |
| `signs in from the second client and reads back the same balance name and duel` | The arc's end. After sign-in and a reboot, B renders A's balance and A's name, and `readDuelPage` under B's session answers A's `duelId` and `outcome`. The device id in `storageB` is unchanged. |

## Acceptance criteria

- [ ] `claimed-here-recovered-there.test.tsx` `the second client holds its own device id and its own profile` passes
- [ ] `claimed-here-recovered-there.test.tsx` `signs in from the second client and reads back the same balance name and duel` passes
- [ ] `NO_COLOR=1 npm run --silent test -- src/e2e/claimed-here-recovered-there.test.tsx 2>&1 | grep -qE 'Tests +4 passed \(4\)'` exits 0
- [ ] `TASK-041407`'s two tests pass unchanged — no assertion edited, weakened or removed
- [ ] `npm run check` exits 0
- [ ] Every command in `verify:` exits 0

## Proof

Run every one and report each result, including a prediction that fails.

1. **Invert the precedence in `account-server.ts`** — device id first, token second. `signs in from
   the second client and reads back the same balance name and duel` must redden, and `the second
   client holds its own device id and its own profile` must stay **green**. If the second one
   reddens too, B is carrying a token before it signs in. Restore afterwards.
2. **Give B the same device id as A** (`viewerSeat: 0` for both drives). The different-device-ids
   assertion must redden. If the sign-in test still passes, it is not testing recovery at all — it is
   testing that a browser reads its own profile.
3. **Make sign-in skip `writeSessionToken`** — have the fake server answer `200` with no
   `sessionToken` field. `sign-in.ts:72` maps that to `failed`, so B stays signed out: the test must
   redden with B rendering **Bob's** balance rather than Ada's. A redness that says *no profile* means
   the reboot lost the device id too, which is a different bug — say which one you saw.
4. **Delete the not-equal halves** from `the second client holds its own device id and its own
   profile`, then re-run mutation 1. If the suite is still red, the not-equal halves were redundant;
   if it goes green, they are load-bearing and must stay. Report which.
5. Confirm the balance compared at the end is the binding captured from A's strip in `TASK-041407`
   and not a literal. Two literals with the same value pass every mutation above.

## Definition of done

Standard, per [`tasks/README.md`](../README.md).
