---
id: STORY-0412
title: The account screens — sign up, sign in, sign out, and which routes are live
type: story
status: backlog
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

| ID | Title | Status |
| --- | --- | --- |
| — | *Not yet split. Run `/plan-story STORY-0412` once `STORY-0406` and `STORY-0411` have merged.* | — |

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
