---
id: STORY-0417
title: The recovery screens — attach an address, and reset a password
type: story
status: in-progress
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
| [TASK-041701](../tasks/TASK-041701-two-mailed-addresses-and-the-opaque-segment-behind-the-slug.md) | Two mailed addresses, and the opaque segment behind the slug — **the inherited debt** | backlog |
| [TASK-041702](../tasks/TASK-041702-the-token-leaves-the-address-bar-and-the-screen-stays-where-it-is.md) | The token leaves the address bar, and the screen stays where it is | backlog |
| [TASK-041703](../tasks/TASK-041703-the-profile-this-client-parses-carries-whether-recovery-is-on.md) | The profile this client parses carries whether recovery is on — **`atomic:`, 6 files** | backlog |
| [TASK-041704](../tasks/TASK-041704-a-body-with-no-recovery-flag-is-not-a-profile.md) | A body with no recovery flag is not a profile, and the flag is that player's | backlog |
| [TASK-041705](../tasks/TASK-041705-the-words-the-account-screen-says-about-recovery.md) | The words the account screen says about recovery, and never the address | backlog |
| [TASK-041706](../tasks/TASK-041706-the-words-the-two-mailed-screens-say.md) | The words the two mailed screens say, including what a reset costs | backlog |
| [TASK-041707](../tasks/TASK-041707-attaching-an-address-costs-the-current-password.md) | Attaching an address costs the current password, and the answer says nothing | backlog |
| [TASK-041708](../tasks/TASK-041708-one-request-one-answer-and-nothing-to-read-into-it.md) | One request, one answer, and nothing to read into it | backlog |
| [TASK-041709](../tasks/TASK-041709-a-token-from-the-mailbox-in-a-body-and-never-in-a-path.md) | A token from the mailbox, in a body and never in a path | backlog |
| [TASK-041710](../tasks/TASK-041710-a-reset-takes-a-token-and-a-password-and-comes-back-with-no-session.md) | A reset takes a token and a password, and comes back with no session | backlog |
| [TASK-041711](../tasks/TASK-041711-four-recovery-calls-on-the-seam-the-account-screens-already-use.md) | Four recovery calls on the seam the account screens already use — **`atomic:`, 5 files** | backlog |
| [TASK-041712](../tasks/TASK-041712-the-account-screen-states-recovery-on-or-off-and-never-an-address.md) | The account screen states recovery on or off, and never an address | backlog |
| [TASK-041713](../tasks/TASK-041713-the-form-that-attaches-an-address-and-says-why-it-asks.md) | The form that attaches an address, and says why it asks for the password | backlog |
| [TASK-041714](../tasks/TASK-041714-the-account-screen-carries-the-attach-form.md) | The account screen carries the attach form, and only where it can be used | backlog |
| [TASK-041715](../tasks/TASK-041715-the-lobby-hands-the-account-screen-its-attach-call.md) | The lobby hands the account screen its attach call | backlog |
| [TASK-041716](../tasks/TASK-041716-the-screen-that-finishes-a-verification.md) | The screen that finishes a verification, from a token it is handed once | backlog |
| [TASK-041717](../tasks/TASK-041717-the-lobby-answers-a-verification-link.md) | The lobby answers a verification link, and the token leaves the address | backlog |
| [TASK-041718](../tasks/TASK-041718-the-screen-that-sets-a-new-password-and-says-what-it-costs.md) | The screen that sets a new password, and says what it costs before it acts | backlog |
| [TASK-041719](../tasks/TASK-041719-the-lobby-answers-a-reset-link-and-sends-the-player-to-sign-in.md) | The lobby answers a reset link, and sends the player to sign in | backlog |
| [TASK-041720](../tasks/TASK-041720-the-secret-sweep-drives-the-four-recovery-calls-too.md) | The secret sweep drives the four recovery calls too | backlog |
| [TASK-041721](../tasks/TASK-041721-the-words-the-forgot-password-flow-says.md) | The words the forgot-password flow says, and the second state it refuses to have | backlog |
| [TASK-041722](../tasks/TASK-041722-the-one-field-form-that-asks-for-a-reset-link.md) | The one-field form that asks for a reset link, and answers everyone the same way | backlog |
| [TASK-041723](../tasks/TASK-041723-the-door-on-the-sign-in-screen-and-the-form-it-opens-in-place.md) | The door on the sign-in screen, and the form it opens in place of the sign-in form | backlog |

**`TASK-041701` is the single startable ticket**, and it is the debt the section above records — put
first on purpose, because every screen below it is reached through `screenFromHash`.

`depends_on` is the sequence, and it is **not** a single chain. Four bundles have pairwise disjoint
`Files` tables and can be dispatched together; everything else is strictly ordered, and the three
tickets that edit `Lobby.tsx` are ordered by that file alone.

```
041701
  ├─ 041702 ─┐
  ├─ 041703 ──→ 041704 ─┐
  └─ 041705 ──→ 041706 ──→ {041707, 041708, 041709, 041710} ──→ 041711
                                                                   ├─ 041712 ─┐
                                                                   ├─ 041713 ─┴→ 041714 → 041715 ─┐
                                                                   ├─ 041716 ────────────────────→ 041717 ─┐
                                                                   └─ 041718 ──────────────────────────────→ 041719 → 041720
```

`ADR-0087` unblocked the last three on 2026-08-28, and they hang off two already-merged ends — the
copy module's, and `Lobby.tsx`'s:

```
041706 ──→ 041721 ──→ 041722 ─┐
041719 ───────────────────────┴→ 041723
```

**Every `verify:` block below the head pins per-file test counts rather than a whole-suite total**,
because an absolute figure is wrong the moment two tickets are dispatched in one batch. The head
pins both, since nothing else is in flight when it runs.

## The three unwritten tickets, and the answer they were waiting for

**Three tickets are not written**, and they are exactly the ones `DEC-081` determined: the *forgot
password* flow's **words**, its **form or screen**, and its **door and wiring**. Their `Files` and
`Tests` tables all depended on whether the flow is a screen with an address of its own or a form on
`#/sign-in`, and on what the product calls it.

**`DEC-081` is answered.**
[`ADR-0087`](../../docs/adr/ADR-0087-forgot-your-password-is-a-door-on-the-sign-in-screen.md), on
2026-08-28: ***Forgot your password?* is a door on the sign-in screen, not a screen of its own.**
What the three tickets are now written from:

- **Four constants in `web-client/src/account/recovery-text.ts`** — `FORGOT_PASSWORD_LABEL =
  "Forgot your password?"`, `FORGOT_PASSWORD_SUBMIT = "Send a link"`,
  `FORGOT_PASSWORD_ACKNOWLEDGED = "If that address is verified on an account here, a link is on its
  way. Follow it to set a new password."` and `FORGOT_PASSWORD_FAILED = "That did not go through.
  Try again."` **`ADDRESS_LABEL` and `CANCEL` are imported, not re-authored** (§1).
- **No slug and no address.** `Screen`, `screenFromHash` and `hashForScreen` are **not edited**, and
  `ADR-0076` §1's table gains no row (§3). Nothing may write `forgot-password` into `screen.ts`.
- **The door is on the sign-in screen, below the sign-in form, conditional on nothing** — not the
  first screen, not the account screen (§4) — and it opens a one-field form **in place of** the
  sign-in form, so there are never two forms in view and no password field in the flow (§5). The
  door's words are that form's heading, **from the one literal** (§2).
- **Accepted renders `FORGOT_PASSWORD_ACKNOWLEDGED` with the form still there and what was typed
  still in it**; failed renders `FORGOT_PASSWORD_FAILED` the same way (§5).
- **An address the product does not hold gets exactly what everybody else gets** — same sentence,
  same controls, same layout, no second state, no hint, no count (§6).

Layout, colour and letter-fitting stay `EPIC-06`'s; which component holds the form is the planner's
and the architect's (§7).

What was **never** held, and was written while the question was open: `TASK-041708`, the transport.
`POST /api/auth/forgot-password` has one field, one status and one merged sentence's worth of
behaviour, and no answer to `DEC-081` moved a line of it. `TASK-041711` puts it on the account seam
beside the other three, where `revokeThisDevice` has already sat without a screen since
`TASK-041220`.

**The three are now written**: `TASK-041721` (the words), `TASK-041722` (the form) and
`TASK-041723` (the door and the wiring). `ADR-0087` §7 left *which component holds the form* to the
planner, and `TASK-041723` answers it: a local `SignInScreenBody` in `Lobby.tsx`, mounted only by the
`sign-in` branch, so leaving the screen unmounts the open/closed mode rather than carrying it across
a navigation. No slug is minted, and `screen.ts` is edited by none of the three.

This was `STORY-0415`'s pattern: write what the answer cannot touch, hold what it determines, and
register the question rather than guessing it. A guessed heading here would have coined player-facing
vocabulary that `ADR-0076` §1 reserves — and `ADR-0087` records that it coined one deliberately,
which is exactly the decision a ticket may not take.

## Open decisions

**None.** `DEC-081` — **the product owner's** — asked what the product calls the *forgot password*
flow, whether it is a screen with its own address or a form on `#/sign-in`, and where its door is.
Registered on 2026-08-28 and answered the same day by
[`ADR-0087`](../../docs/adr/ADR-0087-forgot-your-password-is-a-door-on-the-sign-in-screen.md), whose
holding is transcribed in the section above. It blocked three unwritten tickets and nothing else; the
other twenty held under either answer, and `TASK-041701` was startable throughout.

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

> **The seventh criterion is `TASK-041701`, and it is the story's head.** The eighth is met by every
> ticket's `verify:` block, in the per-file form the split explains above.
>
> **Two criteria are split across tickets.** *"`forgot-password` renders one sentence for every
> outcome"* is met at the transport by `TASK-041708` — two outcomes, asserted identical for every
> failing status — and in the rendering by the tickets `ADR-0087` unblocked, where §6 makes the
> unknown address indistinguishable from every other. The first criterion's *never renders an
> address* is met by `TASK-041712`; the attach form's half is `TASK-041713`'s.

## Out of scope

- Anything the server refuses to expose, including the stored address.
- The mail itself — `STORY-0416` and `EPIC-07`.
- The visual language — `EPIC-06`.
