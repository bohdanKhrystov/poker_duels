---
schema: 2
id: TASK-141014
title: The lobby hands the screen the answer the server gave
type: task
status: backlog
parent: STORY-1410
module: web-client
estimate: S
tier: sonnet
review: deep
files_touched: 2
labels: [client, account]
depends_on: [TASK-141013]
verify:
  - cd web-client && npm ci
  - sh -c 'cd web-client && NO_COLOR=1 npx vitest run src/lobby/Lobby.test.tsx 2>&1 | grep -qF "Lobby.test.tsx  (115 tests)"'
  - sh -c 'cd web-client && NO_COLOR=1 npx vitest run src/App.test.tsx 2>&1 | grep -qF "App.test.tsx  (37 tests)"'
  - sh -c 'cd web-client && NO_COLOR=1 npx vitest run src/e2e/claimed-here-recovered-there.test.tsx 2>&1 | grep -qF "claimed-here-recovered-there.test.tsx  (8 tests)"'
  - sh -c 'cd web-client && ! grep -q "signOutHandsANewProfile={false}" src/lobby/Lobby.tsx'
  - sh -c 'cd web-client && test $(grep -c "useSignOutHandsANewProfile" src/lobby/Lobby.tsx) -eq 2'
  - sh -c 'cd web-client && B=$(mktemp) && cp src/lobby/Lobby.tsx "$B" && perl -0pi -e "s|useSignOutHandsANewProfile\(\)|false|" src/lobby/Lobby.tsx && ! cmp -s src/lobby/Lobby.tsx "$B" || { cp "$B" src/lobby/Lobby.tsx; exit 2; }; NO_COLOR=1 npx vitest run src/lobby/Lobby.test.tsx >/dev/null 2>&1; rc=$?; cp "$B" src/lobby/Lobby.tsx; [ "$rc" -ne 0 ]'
  - git diff --exit-code -- web-client/src/account/AccountScreen.tsx web-client/src/account/SignOutControl.tsx web-client/src/account/device-standing-provider.tsx web-client/src/main.tsx web-client/src/account/sign-out.ts
  - cd web-client && npm run check
  - cd web-client && npm run build
  - python3 .github/scripts/lint_tickets.py
---

## Goal

The last literal goes: `Lobby` reads the provider's answer and hands it to `AccountScreen`. This is
the merge at which signing out of your own account hands the browser a new profile **and** the
confirmation says so — behaviour and copy true together, which is what `ADR-0131` §Consequences
asks for.

## Files

| File | Action |
| --- | --- |
| `web-client/src/lobby/Lobby.tsx` | modify |
| `web-client/src/lobby/Lobby.test.tsx` | modify |

Read, do not edit: `web-client/src/account/device-standing-provider.tsx`,
`web-client/src/account/AccountScreen.tsx`,
`docs/adr/ADR-0131-signing-out-of-your-own-account-hands-the-browser-a-new-profile.md` §§1, 5.

## Scope

- `const signOutHandsANewProfile = useSignOutHandsANewProfile();` at the top of `Lobby`, beside the
  other hook reads (`useProfileStrip`, `useSignedIn`, `useAccount`) and **unconditionally**, above
  every early return — the rules of hooks, and the same shape the file's own comment on `signUp`
  spells out.
- The `account` branch passes `signOutHandsANewProfile={signOutHandsANewProfile}`, replacing
  `TASK-141013`'s literal.
- No branch, no `&&`, no fallback: the provider's default already **is** the fallback
  (`ADR-0135` §6 — a client that has not been told reads `false`), and a second place expressing
  that rule would be a second place able to get it wrong.

## Out of scope

- **Everything else in the chain.** `AccountScreen`, `SignOutControl`,
  `device-standing-provider.tsx`, `main.tsx` and `sign-out.ts` all landed and are in the
  `git diff --exit-code` gate. This ticket is one hook read and one prop.
- **`src/e2e/account-server.ts`.** Still not taught the route: its unknown-path `500` makes
  `readDeviceStanding` answer `false`, which is the correct answer for
  `claimed-here-recovered-there.test.tsx`'s arc — browser B signed in to A's account,
  `ADR-0131` §1 **row two**. Its count is pinned at **8** and its
  `expect(readDeviceId(storageB)).toBe(PLAYER_SEAT_1.deviceId)` must still hold: that assertion is
  this story's proof that row two did not move.
- **An end-to-end test of the abandoning case through a real server double.** It would need
  `account-server.ts` to grow the route and `drive-arc.tsx` to mount the provider — worth doing and
  **not yet ticketed**; the property is covered here at the lobby seam and in `sign-out.test.ts` at
  the module seam.

## Tests

`Lobby.test.tsx`, **113 today → 115**. No merged test is edited: every one of the 113 renders
`Lobby` without a `DeviceStandingProvider` above it, so the hook answers its context default
`false` and the account branch behaves exactly as it did.

| Test | Proves |
| --- | --- |
| `the account screen states the sign-out the server described` | two renders in one test, each wrapping `Lobby` in a `DeviceStandingProvider` whose injected read resolves — one to `true`, one to `false`. In each: open the account screen, press `SIGN_OUT_LABEL`, and read the sentence. `true` shows `SIGN_OUT_HANDS_A_NEW_PROFILE` and not `SIGN_OUT_WARNING`; `false` shows the reverse. Two inputs, because one render cannot tell a threaded value from a constant |
| `the press carries that same answer to the account call` | the same two wrappings, with a `vi.fn()` `signOut` in the `AccountCalls` the test already builds: confirming the press calls it with `true` in the first and `false` in the second, asserted with `toHaveBeenCalledWith`. This is the seam where the story's whole chain is observable in one assertion — provider → lobby → screen → control → call |

## What would still pass if the coder were wrong

- **Passing `false`, or passing `signedIn`** — the two shapes that ship today's behaviour — pass
  all 113 merged tests, because none of them mounts a provider. Both new tests fail, and they fail
  on the `true` half specifically, which is why each carries both polarities rather than only the
  new one.
- **Reading the hook inside the `account` branch** works at runtime for every path a test drives
  and is a rules-of-hooks violation the moment an earlier branch returns first. `eslint
  --max-warnings 0` runs `react-hooks` and is inside `npm run check`, which is in `verify:`.
- **A gate that only counted tests** would pass for a coder who wrote the two tests and wired
  nothing, since both would then fail — but a coder who wrote them *and* wired a constant would be
  caught only by the assertions. The `verify:` mutation is the belt: it rewrites
  `useSignOutHandsANewProfile()` to `false`, proves with `! cmp -s` that the file changed, requires
  `Lobby.test.tsx` to **fail**, and restores the file either way. A suite that stayed green under
  that mutation would mean the two new tests never depended on the hook, and that is the finding.
- **Row two moving** would show up in `claimed-here-recovered-there.test.tsx`, pinned at **8**,
  whose arc signs a browser out of somebody else's account and asserts it keeps its own device id.
  If this ticket is right, that number and that assertion do not move — and if they do, the
  `ADR-0135` §*reversal trigger* has fired on its first day.

## Acceptance criteria

- [ ] `Lobby.test.tsx` reports `(115 tests)` and all pass
- [ ] `the account screen states the sign-out the server described` passes
- [ ] `the press carries that same answer to the account call` passes
- [ ] `App.test.tsx` reports `(37 tests)` and `claimed-here-recovered-there.test.tsx` reports
      `(8 tests)`, all passing
- [ ] `! grep -q "signOutHandsANewProfile={false}"` on `Lobby.tsx` exits 0, and
      `grep -c "useSignOutHandsANewProfile"` is 2
- [ ] The mutation gate exits 0: with the hook read replaced by `false`, `Lobby.test.tsx` fails,
      and the file is restored
- [ ] `git diff --exit-code` over the five landed files exits 0
- [ ] `cd web-client && npm run check` and `npm run build` exit 0
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md).
