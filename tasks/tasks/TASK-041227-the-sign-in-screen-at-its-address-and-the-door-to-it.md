---
schema: 2
id: TASK-041227
title: The sign-in screen at its address, and the door to it from the account screen
type: task
status: done
parent: STORY-0412
module: web-client
estimate: S
tier: sonnet
review: standard
files_touched: 3
labels: [client, account, routing, ui]
depends_on: [TASK-041226]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'reaches the sign-in screen from the account screen, and comes back'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'opens the sign-in screen at the address alone, with no click at all'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'offers the way to sign in to a browser holding an anonymous profile'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'offers no way to sign in to a browser that already holds a session'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'keeps the first screen doors at three'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'opens the sign-in screen to a browser that already holds a session token'
  - cd web-client && npm run check
---

## Goal

`STORY-0412`'s goal sentence becomes reachable end to end: a player can sign in on another browser,
because there is a screen at an address with a door to it.

## The answer this ticket applies

`DEC-077` was answered on 2026-08-26 by
[`ADR-0083`](../../docs/adr/ADR-0083-the-second-account-screen-is-sign-in-and-its-address-is-never-refused.md).
Two of its clauses land here. **§3**: the word is said as the screen's heading and as the one door
to it, from `SIGN_IN_HEADING`, and not on the first screen. **§4**: *the address is refused to
nobody* — a browser that already holds a session token and opens `#/sign-in` **gets the sign-in
screen**, with no redirect and no replaced fragment. Holding a token hides the door; it is not a
branch. `ADR-0076` §3's three store-owned branches still outrank the address, unchanged.

**§5's landing rule is not here.** It reaches `main.tsx`, which is a fourth file with no gate holding
it to these three, so it is `TASK-041229`.

## Files

| File | Action |
| --- | --- |
| `web-client/src/lobby/Lobby.tsx` | modify |
| `web-client/src/account/AccountScreen.tsx` | modify |
| `web-client/src/App.test.tsx` | modify |

Read, and do not edit: `web-client/src/account/SignInForm.tsx` (which renders **no** heading — that
is `TASK-041225`'s explicit deferral, so the heading is `Lobby.tsx`'s to render);
[`ADR-0083`](../../docs/adr/ADR-0083-the-second-account-screen-is-sign-in-and-its-address-is-never-refused.md)
§3 and §4;
[`ADR-0076`](../../docs/adr/ADR-0076-a-screen-the-player-chose-has-an-address.md) §3 and §6;
[`ADR-0060`](../../docs/adr/ADR-0060-the-record-is-its-own-screen-and-the-lobby-is-the-door.md) §4.

## Scope

- `Lobby.tsx` gains one branch, after the account screen's and in the same shape: `SIGN_IN_HEADING`
  as the heading, `SignInForm` beneath it, and the in-page *Back* rendered by the swap, never by the
  screen itself (`ADR-0060` §4).
- `AccountScreen.tsx` gains **one door**, labelled `SIGN_IN_HEADING` and offered only when
  `signedIn` is false. It calls `open("sign-in")`. One constant for the door and the destination is
  `ADR-0060` §2, applied a third time, so a rename moves one literal.
- **The door is hidden from a browser holding a token; the address is not refused to it.** These are
  two different statements and `ADR-0083` §4 makes both. There is **no** branch on
  `readSessionToken(...) !== null` anywhere in the render path for `"sign-in"`: the client knows
  this browser holds a string, not that the string is live, and a browser signed out from another
  device under `ADR-0050` §3 still holds its token. A bounce would hide the only screen that fixes
  that behind a *Sign out* control on a browser that is not signed in.
- **The door is on the account screen and not on the lobby.** The first screen already carries three
  doors; a fourth for a screen whose own parent is one click away is the crowding `ADR-0060` §2
  predicted. A player reaches it as *account → sign in*, which is also the order the words describe.
- **A browser holding an anonymous profile still gets the door.** `ADR-0012` mints a profile on the
  first `Welcome`, so *no profile at all* is a state a player is almost never in; the account screen
  offers the sign-up form for the profile in hand **and** the way to an account they already have.
- **Every query for the six characters *Sign in* is by role.** `SIGN_IN_HEADING` and
  `SIGN_IN_LABEL` put the identical string on the heading and on the submit button beneath it
  (`ADR-0083` §Consequences), so `getByText("Sign in")` throws *found multiple elements* on this
  screen. The heading is `getByRole("heading", { name: SIGN_IN_HEADING })`; the submit is
  `getByRole("button", { name: SIGN_IN_LABEL })`.

## Out of scope

- **The landing after a successful sign-in.** `ADR-0083` §5, `TASK-041229` — and no test here drives
  a sign-in to `signed-in`, so nothing in this ticket asserts where the next boot starts.
- **A door from the lobby.** Refused above, and a criterion counts the first screen's doors.
- **A *forgot password* door.** `STORY-0417`, with `ADR-0081`'s `reset` and `verify`.
- Changing `SignInForm`, `AccountScreen`'s route statement, or any of the four write paths.
- `STORY-0415`'s offer after a first win, which will open `#/account` rather than this address —
  sign-up is the claim of a profile in hand, which is what the offer is about.

## Tests

`web-client/src/App.test.tsx`, in the existing `describe("App")`.

| Test | Proves |
| --- | --- |
| `reaches the sign-in screen from the account screen, and comes back` | From `#/account` with no session: the door is there, clicking it puts the sign-in form on screen and `#/sign-in` in the address, and the in-page *Back* returns the first screen with an empty hash. Both ends and the address at each. The heading is reached by role |
| `opens the sign-in screen at the address alone, with no click at all` | With `window.location.hash` set to `"#/sign-in"` before the render, the form is showing and the room-code form is not |
| `offers the way to sign in to a browser holding an anonymous profile` | With a profile in hand and no session: **both** the sign-up form and the sign-in door are on the account screen. The state every real player is in, and the one a *no profile* fixture would never reach |
| `offers no way to sign in to a browser that already holds a session` | With a session held, the door is absent from the account screen and the sign-out control is present |
| `opens the sign-in screen to a browser that already holds a session token` | **`ADR-0083` §4 directly.** Same fixture as the row above — a token in `localStorage` — but the address set to `"#/sign-in"` before the render: the heading and the submit button are both on screen, and `window.location.hash` is still `"#/sign-in"` afterwards. The two rows are the whole of §4: the door is hidden **and** the address works, and only a pair can say both |
| `keeps the first screen doors at three` | The first screen offers the record, the ladder and the account, and **no fourth** — asserted by the absence of a control named `SIGN_IN_HEADING` among the lobby's buttons |

## Acceptance criteria

- [ ] `App > reaches the sign-in screen from the account screen, and comes back` passes, asserting
      the address at both ends
- [ ] `App > opens the sign-in screen at the address alone, with no click at all` passes
- [ ] `App > offers the way to sign in to a browser holding an anonymous profile` passes, asserting
      **both** the form and the door
- [ ] `App > offers no way to sign in to a browser that already holds a session` passes
- [ ] `App > opens the sign-in screen to a browser that already holds a session token` passes, with
      the fragment unchanged after the render
- [ ] `App > keeps the first screen doors at three` passes
- [ ] No branch on the session token guards the `"sign-in"` render path:
      `grep -c 'readSessionToken' web-client/src/lobby/Lobby.tsx` returns `0`
- [ ] The six characters are never queried by text in the tests this ticket adds:
      `grep -c 'getByText("Sign in")' web-client/src/App.test.tsx` returns `0`
- [ ] Every pre-existing test in `App.test.tsx` passes unchanged
- [ ] No file outside the three listed differs
- [ ] Every command in `verify:` exits 0

## Proof

1. Offer the door regardless of `signedIn`.
   **`offers no way to sign in to a browser that already holds a session` reddens alone.** Every
   other test that renders the *account screen* renders a signed-out browser, so that fixture is the
   only one that can see it — and `opens the sign-in screen to a browser that already holds a session
   token` stays green, because it is about the address and not about the door. That the two do not
   move together is the point of having both. Revert.
2. Bounce a token-holding browser from `#/sign-in` to `#/account` — the branch `ADR-0083` §4
   rejects, and the one any reviewer would wave through.
   **`opens the sign-in screen to a browser that already holds a session token` reddens alone**, on
   the heading and on the fragment. Nothing else in this ticket holds a token at that address, which
   is why that test exists rather than being folded into the row above it.
3. Put the door on the lobby as well.
   **`keeps the first screen doors at three` reddens alone**, and `reaches the sign-in screen from
   the account screen, and comes back` still passes because it starts at `#/account`. Run it: a
   second door is the change nobody argues with and it is the crowding `ADR-0060` refused a
   navigation bar over.
4. Gate the account screen's sign-up form on the player having no session **and** no profile.
   **`offers the way to sign in to a browser holding an anonymous profile` reddens on the form
   half.** This is the mutation to run against a fixture with `kind: "no-profile"` too — it passes
   there, and *no profile* is the state `ADR-0012` makes almost unreachable, so a suite built on it
   would gate nothing.
5. Reach the heading with `getByText("Sign in")` instead of by role.
   **`reaches the sign-in screen from the account screen, and comes back` reddens with
   *"Found multiple elements with the text: Sign in"*** — the heading and the submit button carry the
   identical six characters (`ADR-0083` §Consequences). This is not a hypothetical: it is why the
   by-role rule is a criterion rather than a convention, and the failure names neither constant.
6. Put the sign-in branch above the account branch in `Lobby.tsx`.
   **Nothing reddens**, because the two addresses are distinct and each branch tests its own. Record
   it: branch order among *chosen* screens is not load-bearing, and it is only the three store-owned
   branches above them that are (`ADR-0076` §3, gated by `TASK-041222`'s fifth test).

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.
