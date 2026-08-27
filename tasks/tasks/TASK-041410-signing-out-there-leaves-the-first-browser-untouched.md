---
schema: 2
id: TASK-041410
title: Signing out on the second client leaves it with no profile, and the first untouched
type: task
status: done
parent: STORY-0414
module: web-client
estimate: S
tier: haiku
review: standard
files_touched: 1
labels: [client, e2e, test, auth, identity]
depends_on: [TASK-041409]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test -- src/e2e/claimed-here-recovered-there.test.tsx 2>&1 | grep -qE 'Tests +8 passed \(8\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- src/e2e/claimed-here-recovered-there.test.tsx --reporter=verbose 2>&1 | grep -qF 'signing out on the second client returns it to the profile it had'
  - cd web-client && NO_COLOR=1 npm run --silent test -- src/e2e/claimed-here-recovered-there.test.tsx --reporter=verbose 2>&1 | grep -qF 'the first client is unaffected by the second signing out'
  - cd web-client && NO_COLOR=1 npm run --silent test -- src/e2e/whole-duel.test.tsx src/e2e/duel-secrecy.test.tsx src/e2e/scripted-duel.test.ts 2>&1 | grep -qE 'Tests +18 passed \(18\)'
  - cd web-client && npm run check
---

## Goal

Signing out on browser B hands it back to its own anonymous profile, keeps its device id, and changes
nothing at all about browser A.

## Why this exists

`STORY-0414`'s last criterion, and the case a reader assumes is broken. `sign-out.ts` clears the
token and the room code and **nothing else** — the device id is deliberately untouched, because
clearing it would abandon the anonymous profile it names (`ADR-0030` §8). The visible consequence is
that B does not become nobody: it becomes itself again.

## Files

| File | Action |
| --- | --- |
| `web-client/src/e2e/claimed-here-recovered-there.test.tsx` | modify |

Read, and do not edit: `web-client/src/account/sign-out.ts`;
`web-client/src/account/SignOutControl.tsx`; `web-client/src/protocol/session-token.ts`.

## Scope

- Continue from `TASK-041408`'s signed-in B. Open `#/account` with `wiring.signedIn = true`, click
  `SIGN_OUT_LABEL`, and then the confirming control — `SignOutControl` **asks first** and shows
  `SIGN_OUT_WARNING`; a single click signs nobody out (`TASK-041221`).
- Assert `storageB` no longer holds a session token, and **still** holds `device-seat-1`.
- Boot B again with `wiring.signedIn = false` and assert it renders `player-seat-1`'s name and
  balance again — not A's, and not `No profile yet.`
- Browser A is checked **after** all of it, and the honest way: `storageA` still holds
  `device-seat-0` and no session token, and a fresh `bootClient` over `storageA` renders A's name and
  balance unchanged from what `TASK-041407` captured. A is not mounted while B is — the address is
  module-global.

## Out of scope

- What the server does with the session row beyond answering `401` afterwards. `TASK-041405` proved
  the double's half.
- `ADR-0050`'s revoke, which ends **other** devices' sessions. Nothing in `STORY-0414` calls it.
- Signing out during a live duel. `TASK-041221` warns unconditionally and `ADR-0076` §3 makes the
  account screen unreachable while a frame has seated the tab, so there is no fixture that reaches it.

## Tests

`claimed-here-recovered-there.test.tsx` — two new, on top of six.

| Test | Proves |
| --- | --- |
| `signing out on the second client returns it to the profile it had` | The token is gone, the device id is not, and the next boot renders `player-seat-1`'s name and balance — asserted equal to what B showed before it ever signed in, and not equal to A's. |
| `the first client is unaffected by the second signing out` | `storageA`'s device id and absent token are unchanged, and a boot over it renders the same name and balance `TASK-041407` captured. |

## Acceptance criteria

- [ ] `claimed-here-recovered-there.test.tsx` `signing out on the second client returns it to the profile it had` passes
- [ ] `claimed-here-recovered-there.test.tsx` `the first client is unaffected by the second signing out` passes
- [ ] `NO_COLOR=1 npm run --silent test -- src/e2e/claimed-here-recovered-there.test.tsx 2>&1 | grep -qE 'Tests +8 passed \(8\)'` exits 0
- [ ] The six tests from `TASK-041407`–`TASK-041409` pass unchanged — no assertion edited or removed
- [ ] `NO_COLOR=1 npm run --silent test -- src/e2e/whole-duel.test.tsx src/e2e/duel-secrecy.test.tsx src/e2e/scripted-duel.test.ts 2>&1 | grep -qE 'Tests +18 passed \(18\)'` exits 0
- [ ] `npm run check` exits 0
- [ ] Every command in `verify:` exits 0

## Proof

1. **Make sign-out clear the device id too** — add a `removeItem(DEVICE_ID_STORAGE_KEY)` to the
   harness's sign-out wiring, not to `sign-out.ts`. `signing out on the second client returns it to
   the profile it had` must redden, and the redness must be `No profile yet.` — that is the
   abandonment `ADR-0030` §8 forbids, made visible. Revert.
2. **Make sign-out leave the token in place.** The same test must redden showing **A's** profile,
   because B would still be signed in. Two different mutations, two different rednesses, one test:
   confirm both, and if either shows the same failure as the other, the test is only checking that
   something changed.
3. **Confirm one click is not enough.** Click `SIGN_OUT_LABEL` once, without the confirmation, and
   assert the token is still there. If it is gone, `SignOutControl` is not asking first and that is a
   finding against `TASK-041221`, not something to work around — report it.
4. For `the first client is unaffected by the second signing out`, mutate the fake sign-out to delete
   **every** session. The test must stay green — A holds no session at all — and
   `signing out on the second client returns it to the profile it had` must also stay green. Say so:
   this test cannot see a server-side over-deletion, only that A's storage and A's reads are intact.
   That limit belongs in the test's own comment.

## Definition of done

Standard, per [`tasks/README.md`](../README.md).
