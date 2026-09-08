---
schema: 2
id: TASK-140814
title: A confirmed sign-up re-reads the profile
type: task
status: done
parent: STORY-1408
module: web-client
estimate: S
tier: sonnet
review: standard
files_touched: 3
labels: [client, account, profile]
depends_on: [TASK-140813]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'a confirmed sign-up takes the anonymous block off the screen'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'a refused sign-up reads nothing again and leaves the block standing'
  - cd web-client && npm run check
  - cd web-client && npm run build
  - python3 .github/scripts/lint_tickets.py
---

## Goal

When `signUp` answers `signed-up`, the browser **asks the server again** what its profile is, so the
account screen never prints *anonymous* beside `This profile now has a password.` — `ADR-0132` §6.

## Why a field alone would not have fixed this

`ProfileProvider` reads `GET /api/me` **once**, at mount. Sign-in and sign-out both `reload`, which
rebuilds the read from scratch. `sign-up.ts` passes `noReload` **on purpose** — its own KDoc defends
it — and answers `signed-up` on the `201` *"whether or not that follow-up succeeds"*, because the
credential exists either way.

So a successful sign-up is **the one transition in this product that flips `hasPassword` with nothing
re-asking**. Without this ticket, `TASK-140812`'s block sits on screen next to `SIGNED_UP` until the
tab next boots — two contradictory sentences on one screen, which is the defect `TASK-120601` already
found here once and repaired only for the case that could hold a token.

The proof this ticket owes is therefore **the transition**, not the field: one mount, two different
answers from `GET /api/me`, and the form submitted in between.

## Files

| File | Action |
| --- | --- |
| `web-client/src/lobby/Lobby.tsx` | modify |
| `web-client/src/lobby/Lobby.test.tsx` | modify |
| `web-client/src/e2e/claimed-here-recovered-there.test.tsx` | modify | Added at landing. Its `meRead` finder selects *the only request in this arc carrying a live session's Authorization header* — a premise **this ticket's own change falsifies**, since the refresh is a second such request and arrives first. Green on `develop`, red with the wiring, confirmed both ways |

Read, and do not edit:
`web-client/src/profile/profile-provider.tsx` — `useRefreshProfile`, landed by `TASK-140813`;
`web-client/src/account/sign-up.ts` — `SignUpOutcome`'s seven cases and the `noReload` KDoc;
`web-client/src/account/account-provider.tsx` — `AccountCalls.signUp`'s signature;
[`ADR-0132`](../../docs/adr/ADR-0132-the-profile-says-whether-it-holds-a-password.md) §6.

## Scope

- In `Lobby.tsx` — the composition that already reads both the provider and the account calls, which
  is where `ADR-0132` §6 puts the wiring — wrap the `signUp` handed to `AccountScreen` so it awaits
  the outcome, calls `useRefreshProfile()`'s function **only** when `outcome.kind === "signed-up"`,
  and returns the outcome **unchanged**. `SignUpForm` must see exactly what `signUp` answered.
- Declare the hook and the wrapped callback **unconditionally**, above every early `return` in
  `Lobby`, and keep the `account !== null ? … : undefined` shape for the prop itself — a hook inside a
  branch is a hook that does not always run.
- The re-read runs **after** `signUp` resolves, never beside it: the follow-up sign-in inside
  `sign-up.ts` has already written its token or failed by then, so the re-read carries whatever the
  server actually holds (`ADR-0132` §6).
- `AccountScreen` and `SignUpForm` stay prop-driven and hook-free (`ADR-0060` §4). Neither file is in
  this ticket.
- No other outcome triggers a re-read. `handle-refused`, `unavailable-handle`, `password-refused`,
  `no-profile`, `throttled` and `failed` change no profile, and `ADR-0132` §6 enumerates no second
  trigger because none exists.

## The e2e repair, added at landing

`web-client/src/e2e/claimed-here-recovered-there.test.tsx`'s *the second client learns who it is only
from an answer* goes red under this ticket's change, and it is **this change that breaks it**:
measured green on `develop` and red with the wiring, three runs each way.

Its `meRead` finder takes the first `GET /api/me` carrying an `Authorization` header, on a comment
that calls it *"the only request in this arc carrying a live session's Authorization header"*. The
refresh `ADR-0132` §6 requires is a second one, and it arrives **first** — so the finder now returns
client A's refresh where the case is about client B's post-reboot mount read.

**The fix, decided here rather than left open**, because the coder was right that it admits more than
one edit and so is not `ADR-0070` §4 propagation:

1. Narrow the finder to B's request by `headers["X-Device-Id"] === PLAYER_SEAT_1.deviceId`.
2. Correct the comment: it is no longer the only authorized `/api/me` in the arc, it is B's.
3. **Do not leave the old `X-Device-Id` assertion standing as-is** — selecting by device id and then
   asserting the device id is a tautology that passes whatever the code does. Replace it with the
   claim it was really making: that **exactly one** authorized `GET /api/me` carries B's device id,
   so A's refresh cannot be mistaken for B's read. The player-id and header-allowlist assertions
   around it are untouched and still discriminate.

## Out of scope

- `sign-up.ts` itself. It keeps `noReload`, and it keeps answering `signed-up` on the `201` whether or
  not the follow-up sign-in succeeded. Both are deliberate and `ADR-0132` §6 depends on them.
- `sign-in.ts` and `sign-out.ts`. Both already `reload`; adding a re-read there would be a second
  mechanism for a case that has one.
- Teaching `web-client/src/e2e/account-server.ts` to remember a password, and the whole-client arc
  assertion that would then be possible in `claimed-here-recovered-there.test.tsx`. Named in
  `STORY-1408`'s *Out of scope*; not ticketed yet.
- Any change to what the block says or when `AccountScreen` renders it — `TASK-140812`.

## Tests

`Lobby.test.tsx`

| Test | Proves |
| --- | --- |
| `a confirmed sign-up takes the anonymous block off the screen` | The address at `#/account`, a `ProfileProvider` whose `read` answers `aProfile({ hasPassword: false })` on the first call and `aProfile({ hasPassword: true })` on the second, and an `AccountCalls.signUp` answering `{ kind: "signed-up" }`. Before the press: the three anonymous sentences are on screen. The handle and password are typed and the form submitted. After: `SIGNED_UP` is on screen **and none of the three sentences is**, and `read` has been called exactly twice. Two different answers, so a passing test cannot be a screen that never showed the block |
| `a refused sign-up reads nothing again and leaves the block standing` | The same setup with `signUp` answering `{ kind: "unavailable-handle" }`: after the press `read` has been called exactly **once**, the three sentences are still on screen, and `HANDLE_UNAVAILABLE` is shown. The counterpart input — without it, a wrapper that re-read on every outcome would pass the test above |

## Acceptance criteria

- [ ] `a confirmed sign-up takes the anonymous block off the screen` passes, and asserts the read
      count is exactly `2`
- [ ] `a refused sign-up reads nothing again and leaves the block standing` passes, and asserts the
      read count is exactly `1`
- [ ] The value `SignUpForm` receives from `signUp` is the outcome `AccountCalls.signUp` returned,
      unchanged — `SIGNED_UP` and `HANDLE_UNAVAILABLE` each appearing in its own case is what shows it
- [ ] Every existing test in `Lobby.test.tsx` passes, unedited
- [ ] `npm run check` and `npm run build` exit 0
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md).
