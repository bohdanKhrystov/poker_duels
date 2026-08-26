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
  that expected one is a client that hangs. That destination is ***Sign in*** at **`#/sign-in`**,
  fixed by [`ADR-0083`](../../docs/adr/ADR-0083-the-second-account-screen-is-sign-in-and-its-address-is-never-refused.md)
  §1 and §2 and built by `STORY-0412`; this story does not name it a second time.
- No secret in a URL, no token in storage, no address in any log or analytics call.

## A test this story must carry, inherited as a debt

**`screenFromHash` must be proved against a slug followed by an opaque segment, and this is the only
story that can do it.** `TASK-041201`'s `## Notes` record the shape of the file it shipped: the whole
first-segment rule of `ADR-0081` §1 rests on **one** test, `names the first segment and ignores
whatever follows it`, and a `screen.ts` that matched the *whole* fragment would still pass the other
three. That one test's two inputs are `#/duels/2026` and `#/leaderboard/anything` — **both put a slug
the switch already knows in front of a segment nothing ever reads.**

The case `ADR-0081` §1 exists for is `#/verify/<token>`: the first segment names the screen and the
second is a secret a server wrote into mail. `TASK-041201` could not reach it, because `verify` is
this story's screen and that ticket deferred the slug. The implementation handles it today; **no test
would notice if a later edit stopped it**, and the failure mode is a player clicking a link in their
mail and landing on the lobby with no message.

Whoever splits this story must therefore produce a ticket that pins, in `web-client/src/routing/`:

- `screenFromHash("#/verify/<an opaque token>")` returns `"verify"`, with a token value that is not a
  word, not a year and not a slug — a long random-looking string, so the assertion cannot pass by the
  second segment resembling something the switch knows.
- The same for `#/reset/<token>`, `ADR-0081` §1's other mailed address — **two** slugs, because one
  cannot tell a general rule from a special case for `verify`.
- `tokenFromHash` returns exactly that segment for both, and `null` for an address with no second
  segment. `ADR-0081` §4 puts the token in the fragment precisely so it crosses no wire, and reading
  it is what makes the choice worth anything.
- A token containing a character that would end a path segment or start a query — the guard against a
  reader that splits on the wrong thing.

This is a **requirement on the split, not a ticket**: it is recorded here so the planner run that
cuts this story cannot miss it, and so a reviewer can check that it was honoured.

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
- [ ] `screenFromHash` and `tokenFromHash` are asserted against `#/verify/<token>` **and**
      `#/reset/<token>` with opaque token values — the debt `TASK-041201` recorded and the section
      above spells out. A split that produces no such ticket has not met this story.
- [ ] The suite's own count is asserted, and no test sleeps on a real clock.

## Out of scope

- Anything the server refuses to expose, including the stored address.
- The mail itself — `STORY-0416` and `EPIC-07`.
- The visual language — `EPIC-06`.
