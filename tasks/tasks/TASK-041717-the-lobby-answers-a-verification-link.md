---
schema: 2
id: TASK-041717
title: The lobby answers a verification link, and the token leaves the address
type: task
status: backlog
parent: STORY-0417
module: web-client
estimate: S
tier: sonnet
review: standard
files_touched: 2
labels: [client, account, recovery, routing, wiring]
depends_on: [TASK-041715, TASK-041716]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test -- src/lobby/Lobby.test.tsx 2>&1 | grep -qE 'Test Files +1 passed \(1\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- src/lobby/Lobby.test.tsx 2>&1 | grep -qE 'Tests +61 passed \(61\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- src/App.test.tsx 2>&1 | grep -qE 'Tests +37 passed \(37\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'opens a mailed verification link and sends the token behind the slug'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'takes the token out of the address and leaves the player on the screen'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'renders the verification screen with nothing in hand at the bare address'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'lets a frame that seats this tab outrank a mailed link'
  - test "$(grep -oF 'tokenFromHash' web-client/src/lobby/Lobby.tsx | wc -l | tr -d ' ')" = 2
  - test "$(grep -oF 'clearToken' web-client/src/lobby/Lobby.tsx | wc -l | tr -d ' ')" = 2
  - cd web-client && npm run check
---

## Goal

A mailed `#/verify/<token>` link opens the verification screen, the token is read once and submitted,
the fragment is replaced with `#/verify` so the secret leaves the address bar, and the player stays
on the screen.

## Files

| File | Action |
| --- | --- |
| `web-client/src/lobby/Lobby.tsx` | modify |
| `web-client/src/lobby/Lobby.test.tsx` | modify |

Read, and do not edit:

- `web-client/src/routing/screen.ts` — `tokenFromHash`, and `screenFromHash`'s first-segment rule.
- `web-client/src/routing/use-screen.ts` — `clearToken`, and why it notifies its own subscribers.
- `web-client/src/account/VerifyScreen.tsx` — its two props.
- `web-client/src/lobby/Lobby.tsx`'s merged `screen === "account"` and `screen === "sign-in"`
  branches, and the `seatedByAFrame` effect above them — `ADR-0076` §3, which this branch sits under
  unchanged.
- [`ADR-0081`](../../docs/adr/ADR-0081-a-mailed-link-is-a-fragment-route-and-the-token-is-the-segment-behind-the-slug.md)
  §5 and §Consequences' third bullet — *"a recovery link opened in a tab that is already seated is
  destroyed"*, which is accepted and is not carved out here.

## Scope

- **The token is read once, into state, at the top of the component:**

  ```tsx
  const [verifyToken] = useState(() => tokenFromHash(window.location.hash));
  ```

  A lazy initialiser, so it runs once per mount. `ADR-0081` §5: *"a screen that re-derives its token
  from the address after that replace finds nothing"*, and no type checker catches it.
- **A branch beside the merged ones**, after `sign-in` and before the first screen's return:

  ```tsx
  if (screen === "verify" && account !== null) { … }
  ```

  It renders `<VerifyScreen token={verifyToken} verify={account.verifyEmail} />` inside the same
  `<section>` wrapper the account and sign-in branches use, with the same *Back* control bound to
  `leave`. `account === null` falls through to the first screen, exactly as the `sign-in` branch does.
- **`clearToken()` runs in an effect once the branch is live**, not during render — writing to history
  during render is a side effect in a render path and React may run it twice. Guard it on
  `screen === "verify" && verifyToken !== null`, so a bare `#/verify` does not replace an address that
  is already correct.
- **Nothing new is imported from `../main`.** The call comes through `useAccount()`, which this file
  already imports.

## Out of scope

- **The reset branch.** `TASK-041719`, in this same file, strictly after this one.
- **Rescuing a link opened in a seated tab.** `ADR-0076` §3 has the store outrank the address and
  `ADR-0081` §Consequences accepts the cost by name. The fourth test below **asserts** that behaviour
  rather than fixing it.
- **Any sentence.** All copy is `recovery-text.ts`'s and is rendered by `VerifyScreen`.
- **Reading the token anywhere but this one lazy initialiser.** A `verify:` line pins `tokenFromHash`
  at exactly two occurrences — the import and that call.

## Tests

`web-client/src/lobby/Lobby.test.tsx`, appended inside the existing `describe("the lobby")`.
**57 merged tests become 61.** Set `window.location.hash` before rendering, and reset it in the
existing `afterEach`.

| Test | Proves |
| --- | --- |
| `opens a mailed verification link and sends the token behind the slug` | Address `#/verify/zqx-verify-token-zqx`, an `AccountProvider` whose `verifyEmail` is a spy answering `verified`. The verification heading is on screen and the spy was called once with exactly that token. The token literal is opaque, so a branch that passed the whole fragment or the slug fails |
| `takes the token out of the address and leaves the player on the screen` | The same render. After the effects flush, `window.location.hash` is exactly `"#/verify"`, the heading is **still** on screen, and `window.location.href` contains no part of the token. Three assertions: the address changed, the screen did not, and nothing was left behind. **Measured in this worktree**: this shape lands the hash at `"#/verify"` and keeps the screen |
| `renders the verification screen with nothing in hand at the bare address` | Address `#/verify`, no second segment. The heading is on screen, `VERIFY_NO_LINK` is on screen, the spy was called **zero** times, and the hash is still `"#/verify"` — nothing was replaced, because nothing needed to be. `ADR-0081` §6's *a missing token is not an error address* |
| `lets a frame that seats this tab outrank a mailed link` | Address `#/verify/zqx-verify-token-zqx`, and a store already holding a `view`. The duel table is on screen, the verification heading is **not**, the hash is `"/"`, and the spy was called **zero** times. `ADR-0076` §3, and `ADR-0081` §Consequences' accepted cost, asserted rather than assumed — this is the behaviour a reader will one day want to change, and it should redden when they do |

**No `try` anywhere in the added code, and no `expect()` inside one.** No test sleeps on a real
clock.

## Acceptance criteria

- [ ] `cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'opens a mailed verification link and sends the token behind the slug'`
      — passes, with the spy asserted called once with the opaque token
- [ ] `cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'takes the token out of the address and leaves the player on the screen'`
      — passes, all three assertions: hash exactly `"#/verify"`, heading still present, `href` free of
      the token
- [ ] `cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'renders the verification screen with nothing in hand at the bare address'`
      — passes, with the call count **zero**
- [ ] `cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'lets a frame that seats this tab outrank a mailed link'`
      — passes, with the call count **zero** and the hash `"/"`
- [ ] `cd web-client && NO_COLOR=1 npm run --silent test -- src/lobby/Lobby.test.tsx 2>&1 | grep -qE 'Tests +61 passed \(61\)'`
      and `… | grep -qE 'Test Files +1 passed \(1\)'` — **exactly sixty-one**: `TASK-041715`'s 57 plus
      these four. Both lines, because a collection error prints a *passing* `Tests` count with no
      failure line at all
- [ ] `cd web-client && NO_COLOR=1 npm run --silent test -- src/App.test.tsx 2>&1 | grep -qE 'Tests +37 passed \(37\)'`
      — unmoved. This diff adds no `../main` import, so that file's wholesale `vi.mock` needs none. If
      it reddens, stop and report rather than editing it — it has already forced three tickets here
- [ ] `test "$(grep -oF 'tokenFromHash' web-client/src/lobby/Lobby.tsx | wc -l | tr -d ' ')" = 2`
      — the import and **one** call, inside a lazy initialiser. A second call is a second read of an
      address the first read has already emptied
- [ ] `test "$(grep -oF 'clearToken' web-client/src/lobby/Lobby.tsx | wc -l | tr -d ' ')" = 2`
      — the destructure and one call, inside an effect
- [ ] `cd web-client && npm run check` exits 0
- [ ] Every merged test in `Lobby.test.tsx` passes unchanged. No assertion moves and none is weakened
- [ ] No file outside the two listed differs
- [ ] Every command in `verify:` exits 0

## Proof

Run each step, record what you measured, and revert it. **These are experiments, not changes**, and
both files are inside this ticket's budget.

1. **Read the token on every render** — `tokenFromHash(window.location.hash)` inline instead of the
   lazy initialiser. Predict: `opens a mailed verification link and sends the token behind the slug`
   reddens, because the effect that clears the fragment runs before the second render and the token
   becomes `null`. **This is `ADR-0081` §5's silent trap and the whole reason the read is once** — if
   it stays green, say so loudly, because it means nothing in this client guards that rule.
2. **Replace with `"/"` instead of `hashForScreen(screen)`** — call `leave()` in place of
   `clearToken()`. Predict: `takes the token out of the address and leaves the player on the screen`
   reddens on the hash **and** on the heading. Record both.
3. **Drop the `verifyToken !== null` guard** on the effect. Predict: `renders the verification screen
   with nothing in hand at the bare address` stays green on the hash (it is already `#/verify`) and
   nothing else moves. Record that: it means the guard is an optimisation, not a gated property, and
   say so on landing rather than claiming it is guarded.
4. **Pass the whole fragment** — `window.location.hash` in place of `verifyToken`. Predict: the first
   test reddens on the spy's argument. The opaque token literal is what makes this visible.
5. **Move the branch above the `seatedByAFrame` effect's screens** — put it before the `state.view`
   check. Predict: `lets a frame that seats this tab outrank a mailed link` reddens on the table being
   absent and on the call count. This is `ADR-0076` §3's branch order, and it is the one thing in this
   ticket a well-meaning later edit would get wrong.
6. **Vacuity check on the fourth test**: with the branch left where it belongs, delete its `view` from
   the store fixture. Predict: the test reddens — it should no longer see the table. Confirm the
   fixture really does seat the tab, because an unseated fixture makes that test assert nothing.

> **A red run names a prefix, not a set.** Vitest stops reporting past its first hard failure.
>
> **Expect `The 'NO_COLOR' env is ignored due to the 'FORCE_COLOR' env being set.` on stderr.** It is
> wrong — measured, 0 escape bytes with it and 74 without — so every `grep -qE` needs it.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.

## Notes

**Measured before this ticket was written.** With a throwaway `verify` branch in `Lobby.tsx`, a
throwaway `clearToken` in `use-screen.ts` and `verify`/`reset` in `screen.ts`, opening
`#/verify/Xk93-QQ_z7~aa.bb` rendered the branch, and a control bound to `clearToken` left
`window.location.hash` at `"#/verify"` and `href` at `"http://localhost:3000/#/verify"` with the
screen still on the page. The whole merged suite stayed at `836 passed (836)` and `tsc` was clean —
so this branch costs no merged test, and `App.test.tsx` in particular does not move.

**The fourth test asserts a cost rather than a feature.** `ADR-0081` §Consequences: a recovery link
opened in a tab holding a live duel loses its token, and fifteen minutes plus a second request is the
whole recovery. That is deliberate and `ADR-0076` §3 is not being carved out for it. Pinning it means
the day somebody decides to carve it out, they have to change a test that says why they should not.
