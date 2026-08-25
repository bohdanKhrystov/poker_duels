---
schema: 2
id: TASK-041227
title: The sign-in screen at its address, and the door to it from the account screen
type: task
status: blocked
parent: STORY-0412
module: web-client
estimate: S
tier: sonnet
review: standard
files_touched: 3
labels: [client, account, routing, ui, blocked]
depends_on: [TASK-041226]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'reaches the sign-in screen from the account screen, and comes back'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'opens the sign-in screen at the address alone, with no click at all'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'offers the way to sign in to a browser holding an anonymous profile'
  - cd web-client && npm run check
---

## Goal

`STORY-0412`'s goal sentence becomes reachable end to end: a player can sign in on another browser,
because there is a screen at an address with a door to it.

## Blocked on `DEC-077`

**The product owner's**, and the same decision `TASK-041226` carries — the screen's name and
therefore its slug. This ticket needs the address to exist before it can branch on it. Nothing else
in `STORY-0412` waits on it.

## Files

| File | Action |
| --- | --- |
| `web-client/src/lobby/Lobby.tsx` | modify |
| `web-client/src/account/AccountScreen.tsx` | modify |
| `web-client/src/App.test.tsx` | modify |

Read, and do not edit: `web-client/src/account/SignInForm.tsx`;
[`ADR-0076`](../../docs/adr/ADR-0076-a-screen-the-player-chose-has-an-address.md) §3 and §6;
[`ADR-0060`](../../docs/adr/ADR-0060-the-record-is-its-own-screen-and-the-lobby-is-the-door.md) §4.

## Scope

- `Lobby.tsx` gains one branch, after the account screen's and in the same shape: the sign-in screen
  with the in-page *Back* rendered by the swap, never by the screen itself (`ADR-0060` §4).
- `AccountScreen.tsx` gains **one door**, offered only when `signedIn` is false — a browser already
  holding a session has nothing to sign in to, and `ADR-0060` warned about crowding. It calls
  `open(<the sign-in screen>)`.
- **The door is on the account screen and not on the lobby.** The first screen already carries three
  doors; a fourth for a screen whose own parent is one click away is the crowding `ADR-0060` §2
  predicted. A player reaches it as *account → sign in*, which is also the order the words describe.
- **A browser holding an anonymous profile still gets the door.** `ADR-0012` mints a profile on the
  first `Welcome`, so *no profile at all* is a state a player is almost never in; the account screen
  offers the sign-up form for the profile in hand **and** the way to an account they already have,
  and the two are different intents on two screens rather than two password fields on one.
- The store still outranks the address: nothing about the branch order changes.

## Out of scope

- **A door from the lobby.** Refused above, and a criterion counts the first screen's doors.
- **A *forgot password* door.** `STORY-0417`, with `ADR-0081`'s `reset` and `verify`.
- Changing `SignInForm`, `AccountScreen`'s route statement, or any of the four write paths.
- `STORY-0415`'s offer after a first win, which will open `#/account` rather than this address —
  sign-up is the claim of a profile in hand, which is what the offer is about.

## Tests

`web-client/src/App.test.tsx`, in the existing `describe("App")`.

| Test | Proves |
| --- | --- |
| `reaches the sign-in screen from the account screen, and comes back` | From `#/account` with no session: the door is there, clicking it puts the sign-in form on screen and the slug in the address, and the in-page *Back* returns the first screen with an empty hash. Both ends and the address at each |
| `opens the sign-in screen at the address alone, with no click at all` | With the address set before the render, the form is showing and the room-code form is not |
| `offers the way to sign in to a browser holding an anonymous profile` | With a profile in hand and no session: **both** the sign-up form and the sign-in door are on the account screen. The state every real player is in, and the one a *no profile* fixture would never reach |
| `offers no way to sign in to a browser that already holds a session` | With a session held, the door is absent and the sign-out control is present |
| `keeps the first screen's doors at three` | The first screen offers the record, the ladder and the account, and **no fourth** — asserted by the absence of the sign-in heading among the lobby's controls |

## Acceptance criteria

- [ ] `DEC-077` is answered by a **merged** ADR, and this ticket's status is no longer `blocked`
- [ ] `App > reaches the sign-in screen from the account screen, and comes back` passes, asserting
      the address at both ends
- [ ] `App > opens the sign-in screen at the address alone, with no click at all` passes
- [ ] `App > offers the way to sign in to a browser holding an anonymous profile` passes, asserting
      **both** the form and the door
- [ ] `App > offers no way to sign in to a browser that already holds a session` passes
- [ ] `App > keeps the first screen's doors at three` passes
- [ ] Every pre-existing test in `App.test.tsx` passes unchanged
- [ ] No file outside the three listed differs
- [ ] Every command in `verify:` exits 0

## Proof

1. Offer the door regardless of `signedIn`.
   **`offers no way to sign in to a browser that already holds a session` reddens alone.** Every
   other test in this ticket renders a signed-out browser, so that fixture is the only one that can
   see it — and the harm is a signed-in player being invited to sign in again, which under
   `ADR-0030` §6 is how a second identity arrives on one device. Revert.
2. Put the door on the lobby as well.
   **`keeps the first screen's doors at three` reddens alone**, and `reaches the sign-in screen from
   the account screen, and comes back` still passes because it starts at `#/account`. Run it: a
   second door is the change nobody argues with and it is the crowding `ADR-0060` refused a
   navigation bar over.
3. Gate the account screen's sign-up form on the player having no session **and** no profile.
   **`offers the way to sign in to a browser holding an anonymous profile` reddens on the form
   half.** This is the mutation to run against a fixture with `kind: "no-profile"` too — it passes
   there, and *no profile* is the state `ADR-0012` makes almost unreachable, so a suite built on it
   would gate nothing.
4. Put the sign-in branch above the account branch in `Lobby.tsx`.
   **Nothing reddens**, because the two addresses are distinct and each branch tests its own. Record
   it: branch order among *chosen* screens is not load-bearing, and it is only the three store-owned
   branches above them that are (`ADR-0076` §3, gated by `TASK-041222`'s fifth test).

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.
