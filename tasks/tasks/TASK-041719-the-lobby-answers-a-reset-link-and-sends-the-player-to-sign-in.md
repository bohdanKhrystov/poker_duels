---
schema: 2
id: TASK-041719
title: The lobby answers a reset link, and sends the player to sign in
type: task
status: backlog
parent: STORY-0417
module: web-client
estimate: S
tier: sonnet
review: standard
files_touched: 2
labels: [client, account, recovery, routing, wiring]
depends_on: [TASK-041717, TASK-041718]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test -- src/lobby/Lobby.test.tsx 2>&1 | grep -qE 'Test Files +1 passed \(1\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- src/lobby/Lobby.test.tsx 2>&1 | grep -qE 'Tests +65 passed \(65\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- src/App.test.tsx 2>&1 | grep -qE 'Tests +37 passed \(37\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'opens a mailed reset link and sends the token behind the slug'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'lands the player on the sign-in screen once the password is set'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'leaves the player on the reset screen when the server refuses'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'reads one token for whichever mailed screen the address named'
  - test "$(grep -oF 'tokenFromHash' web-client/src/lobby/Lobby.tsx | wc -l | tr -d ' ')" = 2
  - test "$(grep -oF 'open("sign-in")' web-client/src/lobby/Lobby.tsx | wc -l | tr -d ' ')" = 2
  - test "$(grep -oiE 'sessionToken|writeSessionToken' web-client/src/lobby/Lobby.tsx | wc -l | tr -d ' ')" = 0
  - cd web-client && npm run check
---

## Goal

A mailed `#/reset/<token>` link opens the reset screen with its token, the fragment is cleared, and a
successful reset sends the player to ***Sign in*** at `#/sign-in` — because the server issued no
session and a client that waited for one would hang.

## Files

| File | Action |
| --- | --- |
| `web-client/src/lobby/Lobby.tsx` | modify |
| `web-client/src/lobby/Lobby.test.tsx` | modify |

Read, and do not edit:

- `web-client/src/lobby/Lobby.tsx`'s `screen === "verify"` branch and its token read, as
  `TASK-041717` left them. This branch is its mirror and **shares the one `tokenFromHash` call**.
- `web-client/src/account/ResetScreen.tsx` — its three props.
- `web-client/src/routing/use-screen.ts` — `open` and `clearToken`.
- [`ADR-0083`](../../docs/adr/ADR-0083-the-second-account-screen-is-sign-in-and-its-address-is-never-refused.md)
  §1, §2 and §4 — the destination, its slug, and the fact that the address is refused to nobody, so
  no branch here checks whether this browser holds a token.
- [`ADR-0031`](../../docs/adr/ADR-0031-an-optional-verified-recovery-email.md) §4 — no session is
  issued, which is why the destination is a form and not the account screen.

## Scope

- **One token read serves both mailed screens.** `TASK-041717`'s lazy initialiser is renamed to
  `mailedToken` if it is not already general, and this branch uses the same value — the fragment has
  one second segment and only one of the two screens can be showing. A `verify:` line keeps
  `tokenFromHash` at exactly two occurrences.
- **A branch beside the verify one:**

  ```tsx
  if (screen === "reset" && account !== null) { … }
  ```

  It renders
  `<ResetScreen token={mailedToken} reset={account.resetPassword} onDone={() => open("sign-in")} />`
  inside the same `<section>` wrapper, with the same *Back* control bound to `leave`.
- **`clearToken()`'s effect covers both screens**: its guard widens from `screen === "verify"` to
  `(screen === "verify" || screen === "reset") && mailedToken !== null`.
- **`open("sign-in")` and not a page reload.** `ADR-0083` §5's reload is `signIn`'s, carrying a new
  identity; a reset carries none — no token was issued, the store is untouched, and this browser's
  session was deleted server-side along with every other. An in-page navigation is correct and
  cheaper, and `ADR-0076` §6 requires a page load only of the two controls that rebuild
  `initialState()`.
- **Nothing new is imported from `../main`.**

## Out of scope

- **Clearing this browser's session token after a reset.** `TASK-041209` states the client's whole
  policy: nothing here refreshes or reacts to a session. `ADR-0083` §4 relies on a browser being able
  to reach `#/sign-in` while still holding a dead string. A `verify:` line pins `sessionToken` at zero
  occurrences in this file.
- **Any sentence.** All copy is `recovery-text.ts`'s and `account-text.ts`'s.
- **The *forgot password* flow's door and address.** Held on `DEC-081`.

## Tests

`web-client/src/lobby/Lobby.test.tsx`, appended inside the existing `describe("the lobby")`.
**61 merged tests become 65.**

| Test | Proves |
| --- | --- |
| `opens a mailed reset link and sends the token behind the slug` | Address `#/reset/zqx-reset-token-zqx`, an `AccountProvider` whose `resetPassword` is a spy. The reset heading and the session warning are on screen; type a password, submit; the spy was called once with exactly that token and that password. Then `window.location.hash` is `"#/reset"` and `href` holds no part of the token |
| `lands the player on the sign-in screen once the password is set` | The same render, the spy answering `reset`. Afterwards the sign-in heading is on screen, the reset heading is not, and `window.location.hash` is `"#/sign-in"`. **And this browser's stored session token is still exactly what it was** — asserted from the injected storage, because `ADR-0083` §4 depends on the address working for a browser holding a dead string |
| `leaves the player on the reset screen when the server refuses` | Two renders, answering `link-dead` and `password-refused`. In each: the reset heading is still on screen, the sign-in heading is not, and the hash is still `"#/reset"`. The navigation is bound to one outcome, and two refusals prove it is not bound to *any* outcome |
| `reads one token for whichever mailed screen the address named` | Two renders from two addresses — `#/verify/zqx-verify-token-zqx` and `#/reset/zqx-reset-token-zqx` — each asserting **its own** spy received **its own** token and the other spy was called **zero** times. Two distinct token literals, so a branch that read the wrong one, or a shared read that leaked across, reddens |

**No `try` anywhere in the added code, and no `expect()` inside one.** No test sleeps on a real
clock.

## Acceptance criteria

- [ ] `cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'opens a mailed reset link and sends the token behind the slug'`
      — passes, with the spy's two arguments and the cleared fragment
- [ ] `cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'lands the player on the sign-in screen once the password is set'`
      — passes, including the assertion that the stored session token is **unchanged**
- [ ] `cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'leaves the player on the reset screen when the server refuses'`
      — passes, over **both** refusals
- [ ] `cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'reads one token for whichever mailed screen the address named'`
      — passes, each spy asserted called and the other asserted **zero**
- [ ] `cd web-client && NO_COLOR=1 npm run --silent test -- src/lobby/Lobby.test.tsx 2>&1 | grep -qE 'Tests +65 passed \(65\)'`
      and `… | grep -qE 'Test Files +1 passed \(1\)'` — **exactly sixty-five**: `TASK-041717`'s 61
      plus these four. Both lines, because a collection error prints a *passing* `Tests` count with no
      failure line at all
- [ ] `cd web-client && NO_COLOR=1 npm run --silent test -- src/App.test.tsx 2>&1 | grep -qE 'Tests +37 passed \(37\)'`
      — unmoved. No `../main` import is added
- [ ] `test "$(grep -oF 'tokenFromHash' web-client/src/lobby/Lobby.tsx | wc -l | tr -d ' ')" = 2`
      — still the import and **one** call. This branch shares the verify branch's read rather than
      adding a second
- [ ] `test "$(grep -oF 'open("sign-in")' web-client/src/lobby/Lobby.tsx | wc -l | tr -d ' ')" = 2`
      — the merged account-screen door and this one. A third is a second place deciding where a reset
      lands
- [ ] `test "$(grep -oiE 'sessionToken|writeSessionToken' web-client/src/lobby/Lobby.tsx | wc -l | tr -d ' ')" = 0`
      — the lobby does not touch this browser's session after a reset. Reads the whole file
- [ ] `cd web-client && npm run check` exits 0
- [ ] Every merged test in `Lobby.test.tsx` passes unchanged. No assertion moves and none is weakened
- [ ] No file outside the two listed differs
- [ ] Every command in `verify:` exits 0

## Proof

Run each step, record what you measured, and revert it. **These are experiments, not changes**, and
both files are inside this ticket's budget.

1. **Bind `onDone` to `leave` instead of `open("sign-in")`.** Predict: `lands the player on the
   sign-in screen once the password is set` reddens on the hash and on the sign-in heading.
2. **Call `onDone` for every outcome** — wrap `account.resetPassword` so it always resolves `reset`.
   Predict: `leaves the player on the reset screen when the server refuses` reddens on both of its
   renders. Two refusals rather than one, because a navigation bound to *any settled outcome* passes
   a single-refusal test half the time.
3. **Clear the stored session token in the `onDone` handler.** Predict: `lands the player on the
   sign-in screen once the password is set` reddens on its storage assertion **alone** — the
   navigation still works. That is the point: `ADR-0083` §4 needs the address to work for a browser
   holding a dead string, and nothing else in this client would notice the clear.
4. **Swap the two branches' tokens** — give `ResetScreen` the verify branch's screen check. Predict:
   `reads one token for whichever mailed screen the address named` reddens on both halves. Two
   distinct literals make this visible; one shared literal makes it invisible.
5. **Narrow `clearToken`'s effect back to `screen === "verify"`.** Predict: `opens a mailed reset link
   and sends the token behind the slug` reddens on the hash assertion. Record it — this is the one
   line of `TASK-041717`'s work this ticket has to widen, and forgetting it leaves a reset token in
   the address bar and in the history entry.
6. **Vacuity check on the storage assertion.** Seed the injected storage with **no** token, then run
   step 3 again. Predict: the assertion passes with the clear in place, because there was nothing to
   clear. Confirm the fixture seeds a token before asserting it survived — a fixture value at the
   boundary the bug leaves unchanged can never detect it.

> **A red run names a prefix, not a set.** Vitest stops reporting past its first hard failure.
>
> **Expect `The 'NO_COLOR' env is ignored due to the 'FORCE_COLOR' env being set.` on stderr.** It is
> wrong — measured, 0 escape bytes with it and 74 without — so every `grep -qE` needs it.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.

## Notes

**Why `open("sign-in")` and not the reload `TASK-041213` uses.** That reload exists because a
sign-in changes identity and `ADR-0075` names three presence fields no store action clears, so the
socket has to be rebuilt. A reset changes no identity in this browser: no token is issued
(`ADR-0031` §4), the store is untouched, and the session this browser held was deleted on the server
rather than replaced here. An in-page navigation is the smaller change and `ADR-0076` §6 asks for a
page load only where `initialState()` must be rebuilt.

**The storage assertion in the second test is the surprising one, and it is deliberate.** It looks
like a test about nothing — the diff does not touch storage. It is there because the obvious
"tidy-up" a later reader will reach for is clearing the dead token on the way to sign-in, and
`ADR-0083` §4 spent an entire alternative explaining why a browser holding a dead string must still
reach that screen. Step 3 measures that nothing else in this client would catch it.
