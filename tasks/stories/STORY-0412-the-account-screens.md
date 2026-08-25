---
id: STORY-0412
title: The account screens — sign up, sign in, sign out, and which routes are live
type: story
status: ready
parent: EPIC-04
module: web-client
labels: [client, ui, auth]
depends_on: [STORY-0406, STORY-0411]
---

## Goal

A player can turn the profile they already have into an account, sign in on another browser, sign out
again, and read — on the screen, in words — which routes currently sign in to their profile.

## Why

The server half is useless without it, and `ADR-0037` made the screen part of the decision rather
than decoration: the human accepted *the device keeps signing in* only on the condition that the
player can see it and end it.

## Design notes

- **One credential, and the screen is designed for it**
  ([`ADR-0041`](../../docs/adr/ADR-0041-a-handle-and-a-password-are-the-only-credential.md)): no
  provider row, no *"or continue with…"* divider, no space held open for buttons that do not exist.
- **The handle is not the display name**, and the screens must not imply otherwise — no pre-filling
  one from the other, no copy suggesting they are the same string. `ADR-0031` §1 is explicit about
  why: a leaderboard is a published list of display names, and making the name a handle would make
  it half a credential.
- **The token is stored, the device id is not touched.** `ADR-0030` §8: the stored device id is
  **write-once** — set from the first `Welcome` that carries one, never cleared, never overwritten,
  not on sign-in, not on sign-out, not when a `Welcome` arrives with a null `deviceId`. A client that
  re-mints one abandons a profile. Sign-out clears the token and only the token.
- **The client keeps sending its device id whether or not it holds a token** — the server ignores it
  under a session, so the alternative buys nothing and its bug mode is the abandonment above.
- **Sign-out closes the socket and reconnects**, because identity is fixed at `Hello` for the life of
  a socket. A warning belongs on sign-out during a live duel: the seat is abandoned and may be folded
  (`ADR-0030` §6).
- **The routes-are-live statement is the substance of `ADR-0037`**: a player who has not revoked is
  entitled to know the device still signs in; one who has is entitled to see that it does not. Both
  states are rendered from server-sent facts — the client asserts neither.
- **Nothing gates on having an account** ([`ADR-0036`](../../docs/adr/ADR-0036-an-account-is-offered-never-required.md)):
  every screen reachable anonymously stays reachable anonymously.
- No password, handle or token is ever put in a URL, a query string or a log line.

## Tasks

Split on 2026-08-26 into **27**, in a single linear chain: every ticket after the first depends on
the one before it, because the story's files overlap heavily — `Lobby.tsx` is edited by four tickets,
`screen.ts` by three, `main.tsx` by two and `App.test.tsx` by five.

**How many account screens, and what they are called.** `ADR-0076` §1 left the count to this story
and the count is **two**: the account screen at `#/account`, which claims the profile in hand and
carries `ADR-0037`'s statement, `ADR-0050`'s revoke and sign-out; and a sign-in screen, which is how
a browser that does not hold the profile reaches it. Two rather than one because `ADR-0012` mints an
anonymous profile on the first `Welcome`, so every player arrives holding one — the two intents
(*give this profile a password* and *reach the account I already have*) are always both live, and
putting two handle-and-password forms on one screen is what `ADR-0041` was keeping clean. Two rather
than three because sign-up is a **claim** (`ADR-0030` §1: one endpoint, attaching to the profile this
device already owns), so it belongs on the account screen rather than beside it — which is also where
`STORY-0415`'s offer after a first win will send a player. `account` is not a coined word:
`ADR-0050` §3, `ADR-0036` and `ADR-0056` §2 each say it to a player. The sign-in screen's word is
`DEC-077`.

**What turned out to be already settled, and needed no decision.** *Does the screen need a
`hasCredential` field?* — no: `ADR-0050` §4 says `deviceRouteLive` is the whole of what the screen
reads, and `docs/protocol.md` makes `POST /api/auth/sign-in` the only endpoint that ever issues a
session token, so a browser holding a live session is one whose player has a password, by
construction. *How does an identity change reach the socket?* — by reloading the document:
`ADR-0075` records that `rivalPresence`, `graceRemainingMillis` and `rivalReturned` are cleared at no
store boundary and are unreachable only because a real navigation rebuilds `initialState()`, and
`ADR-0076` §6 keeps two controls as page loads for that exact reason.

**One acceptance criterion is met in a different shape than it was written, deliberately.** *Signing
out during a live duel warns before it acts*: `ADR-0076` §3 makes `Lobby.tsx` branch on `outcome`,
`view` and `roomCode` **before** the address, so the account screen cannot be on display while a
frame has seated this tab and a duel-conditional warning is a branch no fixture can reach.
`TASK-041221` therefore warns on **every** sign-out — true in every state, including the one the
criterion names — and `TASK-041222` gates the structural half.

| ID | Title | Status |
| --- | --- | --- |
| [TASK-041201](../tasks/TASK-041201-the-address-of-a-screen-is-a-pure-function-of-its-fragment.md) | The address of a screen, as a pure function of its fragment | ready |
| [TASK-041202](../tasks/TASK-041202-the-hook-that-carries-the-address-and-the-trap-that-is-silent.md) | The hook that carries the address, and the trap that makes a stale render look like React | backlog |
| [TASK-041203](../tasks/TASK-041203-the-lobby-reads-the-address-instead-of-two-flags.md) | The lobby reads the address instead of two flags, and Back stops leaving the client | backlog |
| [TASK-041204](../tasks/TASK-041204-the-store-outranks-the-address-and-the-address-stops-lying.md) | The store outranks the address, and a seated player's address stops lying | backlog |
| [TASK-041205](../tasks/TASK-041205-the-token-this-browser-holds-lives-under-one-key.md) | The session token this browser holds lives under one key | backlog |
| [TASK-041206](../tasks/TASK-041206-hello-carries-the-session-and-the-device-id-still-never-moves.md) | Hello carries the session this browser holds, and the device id still never moves | backlog |
| [TASK-041207](../tasks/TASK-041207-the-profile-carries-whether-the-device-route-is-live.md) | The profile carries whether the device route is still live | backlog |
| [TASK-041208](../tasks/TASK-041208-a-profile-body-with-no-device-route-is-not-a-profile.md) | A profile body with no device route is not a profile | backlog |
| [TASK-041209](../tasks/TASK-041209-a-fetch-that-carries-the-session-this-browser-holds.md) | A fetch that carries the session this browser holds | backlog |
| [TASK-041210](../tasks/TASK-041210-every-me-read-goes-out-under-the-session.md) | Every read under `/api/me` goes out under the session | backlog |
| [TASK-041211](../tasks/TASK-041211-the-words-the-account-screen-says.md) | The words the account screen says, including the refusal that is about nobody | backlog |
| [TASK-041212](../tasks/TASK-041212-sign-up-and-the-refusal-that-is-about-nobody.md) | Sign-up, seven outcomes, and the one refusal that is about nobody | backlog |
| [TASK-041213](../tasks/TASK-041213-sign-in-stores-the-token-and-one-answer-covers-both-refusals.md) | Sign-in stores the token, carries no credential of its own, and reloads | backlog |
| [TASK-041214](../tasks/TASK-041214-sign-out-clears-the-token-and-only-the-token.md) | Sign-out clears the token and only the token, leaves the room, and reloads | backlog |
| [TASK-041215](../tasks/TASK-041215-stopping-this-device-signing-in-and-the-two-refusals.md) | Stopping this device signing in, and the two refusals that are not failures | backlog |
| [TASK-041216](../tasks/TASK-041216-the-four-account-calls-reach-a-screen-through-one-provider.md) | The four account calls reach a screen through one provider | backlog |
| [TASK-041217](../tasks/TASK-041217-the-account-screen-states-which-routes-sign-in.md) | The account screen states which routes sign in to this profile, in both states | backlog |
| [TASK-041218](../tasks/TASK-041218-the-sign-up-form-on-the-account-screen.md) | The sign-up form — one credential, and the strip is the same profile afterwards | backlog |
| [TASK-041219](../tasks/TASK-041219-a-throttled-sign-up-says-so-keeps-what-was-typed-and-retries-nothing.md) | A throttled sign-up says so, keeps what was typed, and retries nothing | backlog |
| [TASK-041220](../tasks/TASK-041220-stopping-this-device-with-one-confirmation-and-three-facts.md) | Stopping this device signing in, offered only where it is safe, with three facts first | backlog |
| [TASK-041221](../tasks/TASK-041221-signing-out-asks-first-and-says-what-it-costs.md) | Signing out asks first, and says what it costs before it acts | backlog |
| [TASK-041222](../tasks/TASK-041222-the-account-screen-has-an-address-and-the-lobby-has-the-door.md) | The account screen has an address, and the lobby has the door | backlog |
| [TASK-041223](../tasks/TASK-041223-the-account-calls-reach-the-real-transport.md) | The account calls reach the real transport, and sign-in reaches it carrying nothing | backlog |
| [TASK-041224](../tasks/TASK-041224-no-secret-reaches-a-url-and-no-body-carries-a-player-id.md) | No secret reaches a URL, and no request body carries a player id | backlog |
| [TASK-041225](../tasks/TASK-041225-the-sign-in-form.md) | The sign-in form, and one sentence for both ways it can be refused | backlog |
| [TASK-041226](../tasks/TASK-041226-the-sign-in-screens-word-and-its-slug.md) | The sign-in screen's word, and the address that word becomes | **blocked** — `DEC-077` |
| [TASK-041227](../tasks/TASK-041227-the-sign-in-screen-at-its-address-and-the-door-to-it.md) | The sign-in screen at its address, and the door to it from the account screen | **blocked** — `DEC-077` |

## Open decisions

| ID | Question | Blocks |
| --- | --- | --- |
| `DEC-077` | **The product owner's** — what does the product call the screen a player opens to reach an account from a browser that does not hold it, and therefore what is that screen's permanent slug? Registered in [`docs/adr/README.md`](../../docs/adr/README.md#open-decisions) | `TASK-041226`, `TASK-041227` |

## Acceptance criteria

- [ ] Signing up from a browser holding an anonymous profile leaves the strip showing the same coin
      balance and the same name afterwards.
- [ ] The stored device id is unchanged by sign-up, by sign-in, by sign-out and by a `Welcome`
      carrying a null device id — all four asserted.
- [ ] Signing in stores the token; signing out removes the token and nothing else.
- [ ] A wrong password and an unknown handle render the same message, because the server sends the
      same answer.
- [ ] The screen states which routes sign in to this profile, in both the revoked and unrevoked
      states, from server-sent facts.
- [ ] Revoking is offered only when a credential exists.
- [ ] Signing out during a live duel warns before it acts.
- [ ] No secret reaches a URL, and no request body carries a player id.

## Out of scope

- The recovery email and the reset screens — `STORY-0417`.
- The offer after a first win — `STORY-0415`.
- Account deletion — `ADR-0039`: not in v0.1.
- The visual language — `EPIC-06`.
