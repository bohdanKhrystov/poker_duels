---
schema: 2
id: TASK-041229
title: A successful sign-in starts the next boot on the account screen, with no way back to sign-in
type: task
status: backlog
parent: STORY-0412
module: web-client
estimate: S
tier: haiku
review: standard
files_touched: 2
labels: [client, account, routing, wiring]
depends_on: [TASK-041227]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'lands the next boot on the account screen after a sign-in that worked'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'leaves a refused sign-in exactly where it was'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'sends sign-in with no credential of its own, even holding a session'
  - cd web-client && npm run check
---

## Goal

A browser that has just signed in never comes back to the sign-in screen: the reload that carries the
new identity starts at `#/account`.

## The answer this ticket applies

[`ADR-0083`](../../docs/adr/ADR-0083-the-second-account-screen-is-sign-in-and-its-address-is-never-refused.md)
§5. *How* it is arranged is this ticket's; *that* it happens is the ADR's. The reason is that the
account screen carries `ADR-0037`'s routes statement (`TASK-041217`, `PASSWORD_ROUTE_LIVE`), which is
**the only confirmation this product has** — `account-text.ts` authors no *you are signed in*
sentence anywhere, so a sign-in that landed on the first screen would be a product that never says it
worked.

## Why this is its own ticket

`TASK-041227` was written at three files — `Lobby.tsx`, `AccountScreen.tsx`, `App.test.tsx` — before
`DEC-077` was answered. §5's landing rule reaches a fourth, `main.tsx`, because `signIn`'s `reload`
is wired there as a module-scope constant (`TASK-041223`). **No merged gate refuses the intermediate
state**: `npm run check` and `npm run build` are both green with the screen and the door but no
landing rule, and green with the landing rule and no screen. Four files with no gate holding them
together is two tickets, not an `atomic:` of four (`ADR-0068`, `ADR-0070`).

## Files

| File | Action |
| --- | --- |
| `web-client/src/main.tsx` | modify |
| `web-client/src/App.test.tsx` | modify |

Read, and do not edit: `web-client/src/account/sign-in.ts` (for the `reload` parameter only);
`web-client/src/routing/screen.ts` (for `hashForScreen`);
[`ADR-0083`](../../docs/adr/ADR-0083-the-second-account-screen-is-sign-in-and-its-address-is-never-refused.md)
§5; [`ADR-0076`](../../docs/adr/ADR-0076-a-screen-the-player-chose-has-an-address.md) §6.

## Scope

- One module-scope constant in `main.tsx`, beside `reload`, and `signIn` is the only call that gets
  it:

  ```ts
  // ADR-0083 §5: a successful sign-in starts the next boot at #/account, never
  // back at the screen it has just finished using. A replace rather than an
  // assignment, because a pushed entry would put the Back button on #/sign-in
  // for a browser that is now signed in.
  const reloadAtAccount = () => {
    window.history.replaceState(null, "", hashForScreen("account"));
    window.location.reload();
  };
  ```

- **`replaceState`, never `window.location.hash = …`.** The assignment adds a history entry
  (`TASK-041202`'s third test measures exactly that), so *Back* would return the player to
  `#/sign-in` — the screen `ADR-0083` §5 says they never come back to. `replaceState` fires neither
  `popstate` nor `hashchange` (`ADR-0076` §5), which is harmless here because the very next statement
  discards the document.
- **`signOut` keeps the plain `reload`.** `ADR-0083` §5 is about sign-in and says nothing about
  sign-out, and `TASK-041214` already decided that sign-out leaves the room. Two constants, one
  changed call site.
- Nothing else in `main.tsx` moves: `plainFetch`, `accountCalls`, `signedIn` and the four bindings
  stay exactly as `TASK-041223` left them.

## Out of scope

- **Changing `sign-in.ts`.** `signIn` takes `reload` injected and calls it once; that is the whole
  reason this rule lands in the wiring and not in the module. A criterion greps it.
- **A *you are signed in* sentence.** `ADR-0083` §5 makes the account screen's routes statement the
  confirmation; adding copy would be new player-facing vocabulary and nothing licenses it.
- **Where a refused sign-in goes.** Nowhere: `ADR-0083` §5's last paragraph keeps the player at
  `#/sign-in` reading `SIGN_IN_REFUSED`. **A refusal, not an omission** — the second test below pins
  it, because a `reloadAtAccount` wired onto the wrong branch would move a player who failed to log
  in.
- **Sign-out's landing.** Untouched, and no test here observes it.
- The screen, the door and the branch — `TASK-041227`, which merges first.

## Tests

`web-client/src/App.test.tsx`, in the existing `describe("App")`, beside `TASK-041223`'s binding
tests and driven the same way: the rendered tree over a stubbed `window.fetch`.

> **Expect a `Not implemented: navigation (except hash changes)` line on stderr.** jsdom prints it
> when `window.location.reload()` is called and **does not fail the test** — measured on this branch.
> It is not a defect and must not be silenced by stubbing `reload` away; stubbing it out would delete
> the only call this ticket exists to place.

| Test | Proves |
| --- | --- |
| `lands the next boot on the account screen after a sign-in that worked` | From `#/sign-in`, with `window.fetch` answering `200` to `/api/auth/sign-in`: after the submit, `window.location.hash` is `"#/account"` **and** `window.history.length` is unchanged from a value captured before the submit. The second assertion is the whole difference between a replace and an assignment, and no other test in the file can see it |
| `leaves a refused sign-in exactly where it was` | The same fixture with `window.fetch` answering `401`: `window.location.hash` is still `"#/sign-in"`, `SIGN_IN_REFUSED` is on screen, and `history.length` is unchanged. **Two different server answers against one fixture**, so a wiring that landed everybody on `#/account` cannot pass both |
| `sends sign-in with no credential of its own, even holding a session` *(existing, `TASK-041223`)* | Still passes unchanged: the request is still naked. Named here because it is the only merged test that observes the `signIn` binding this ticket re-values, and it must not move |

## Acceptance criteria

- [ ] `App > lands the next boot on the account screen after a sign-in that worked` passes,
      asserting **both** the fragment and an unchanged `history.length`
- [ ] `App > leaves a refused sign-in exactly where it was` passes
- [ ] `App > sends sign-in with no credential of its own, even holding a session` passes **unchanged**
      — not one assertion in it is edited
- [ ] The fragment is set by a replace, not an assignment:
      `grep -c 'location.hash =' web-client/src/main.tsx` returns `0`
- [ ] The rule is in the wiring, not the module:
      `grep -c 'hashForScreen' web-client/src/account/sign-in.ts` returns `0`
- [ ] Every pre-existing test in `App.test.tsx` passes unchanged
- [ ] No file outside the two listed differs
- [ ] Every command in `verify:` exits 0

## Proof

1. Write the landing as `window.location.hash = hashForScreen("account")` before the reload.
   **`lands the next boot on the account screen after a sign-in that worked` reddens on the
   `history.length` assertion alone**, while the fragment assertion in the same test stays green.
   That asymmetry is why the test captures the length before the submit rather than asserting the
   fragment and stopping — the address would be right and *Back* would still return a signed-in
   browser to the sign-in screen, which is the exact sentence `ADR-0083` §5 forbids. Revert.
2. Pass `reloadAtAccount` to `signOut` as well as to `signIn`.
   **Nothing in this ticket reddens**, and it is worth writing down why rather than adding a test to
   catch it: no fixture here signs out, and `TASK-041214`'s tests inject their own `reload` double so
   they never see `main.tsx`'s. The guard is the `## Out of scope` line above and the reviewer, not
   an assertion — say so in the PR instead of leaving a reader to assume it is covered.
3. Call `reloadAtAccount` on every `SignInOutcome` rather than only on `signed-in`.
   **`leaves a refused sign-in exactly where it was` reddens** on the fragment, because the refused
   branch would move the address to `#/account`. A ticket that shipped only the success test would
   pass this mutation, which is why there are two server answers and not one.
4. Drop the `window.location.reload()` and keep only the `replaceState`.
   **Nothing reddens.** jsdom's `reload` is a no-op that prints to stderr, so no assertion in this
   file can observe it, and the fragment lands either way. Record it plainly: the reload is what
   `ADR-0075` needs — a real navigation is the only thing that rebuilds `initialState()` and clears
   the three presence fields — and it is held by `TASK-041213`'s `sign-in.test.ts`, which counts the
   injected `reload` call, not by anything here.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.
