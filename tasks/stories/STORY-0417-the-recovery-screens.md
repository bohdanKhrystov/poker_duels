---
id: STORY-0417
title: The recovery screens — attach an address, and reset a password
type: story
status: backlog
parent: EPIC-04
module: web-client
labels: [client, ui, auth]
depends_on: [STORY-0412, STORY-0416]
---

## Goal

A player can attach a recovery address from the account screen, finish the verification the mail
sent them to, ask for a reset when they have forgotten their password, and set a new one.

## Why

`STORY-0416` builds the mechanism; without these four screens it is reachable only with a shell. It
follows `STORY-0412` because it extends the account screen that story opens.

> Filed alongside `STORY-0416`, which `EPIC-04`'s original story table predates. See that story's
> note.

## Design notes

- **The address is never displayed back.** The server never returns it (`ADR-0031` §6), so the screen
  says *recovery is on* or *recovery is off* from `hasRecoveryEmail` and nothing else. A screen that
  wants to show the address is asking for an endpoint this epic deliberately does not build.
- **The reset token is read from `location.hash`, posted in a body, and the hash is cleared with
  `history.replaceState`.** Never a query string, never a header, never stored. That is `ADR-0031`
  §4's whole reason for choosing a fragment.
- **`forgot-password` always says the same thing**, because the server always answers `202`. The
  screen must not add a distinction the server refused to make — *if that address is registered, mail
  is on its way* is the only honest sentence, and a "no such address" message would reintroduce the
  oracle the server closed.
- **Attaching asks for the current password**, and the screen says why: it is what stops an
  unattended browser from becoming permanent ownership.
- **A successful reset signs you out everywhere, including here.** The screen says so before it acts,
  and afterwards sends the player to sign in — the server issues no session from a reset, so a client
  that expected one is a client that hangs.
- No secret in a URL, no token in storage, no address in any log or analytics call.

## Tasks

| ID | Title | Status |
| --- | --- | --- |
| — | *Not yet split. Run `/plan-story STORY-0417` once `STORY-0412` and `STORY-0416` have merged.* | — |

## Acceptance criteria

- [ ] The account screen states recovery on or off from `hasRecoveryEmail`, in both states, and never
      renders an address.
- [ ] Attaching sends the address and the current password, and renders the same acknowledgement
      whatever the server's reason for `202`.
- [ ] The verification screen reads the token from the fragment, posts it in a body, and clears the
      fragment — asserted, including that no request URL contains the token.
- [ ] `forgot-password` renders one sentence for every outcome.
- [ ] The reset screen warns that every session ends, then sends the player to sign in, and never
      expects a token back.
- [ ] A reset failure (`400`) and a policy failure (`422`) render different, actionable sentences.
- [ ] The suite's own count is asserted, and no test sleeps on a real clock.

## Out of scope

- Anything the server refuses to expose, including the stored address.
- The mail itself — `STORY-0416` and `EPIC-07`.
- The visual language — `EPIC-06`.
