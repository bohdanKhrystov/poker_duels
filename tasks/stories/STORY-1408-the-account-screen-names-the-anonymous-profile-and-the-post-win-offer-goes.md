---
id: STORY-1408
title: The account screen names the anonymous profile, the post-win offer goes, and the front door stops describing an absence
type: story
status: ready
parent: EPIC-14
module: web-client
labels: [client, server, design, account, docs]
depends_on: []
---

## Goal

A player who opens `Account` is told, in words, what their profile **is**: anonymous — it holds no
password, it lives in this browser, and giving it a password keeps every duel coin and every duel.
No screen offers an account after a duel any more, and the front door stops printing `No profile
yet.` The screen may say *anonymous* only because the server now tells it so: `GET /api/me` carries
`hasPassword`, and a sign-up that succeeds makes the browser re-read it rather than guess.

## Why

This is `EPIC-14` item 1b, and it is the human's own instruction twice over: *"When you go to
profile it says: 'Annonymus Account'; You can 'promote' your account to real by set email and
password"*, and on `edits4.png` — the result screen after a win — the **entire** account-offer block
struck through with the single word ***remove***.

Two merged ADRs settle it and this story implements them without reopening either:

- [`ADR-0125`](../../docs/adr/ADR-0125-the-account-screen-names-the-anonymous-profile-and-owns-the-door.md)
  — the offer is retired **whole** and **nothing replaces it on any screen**; the account screen
  names the profile *anonymous*; the strip's `No profile yet.` goes with it. It supersedes
  `ADR-0085`, `ADR-0086` and `ADR-0116` **in full** and `ADR-0036`'s offer block, and answers
  `DEC-089` by deletion.
- [`ADR-0132`](../../docs/adr/ADR-0132-the-profile-says-whether-it-holds-a-password.md) —
  `ProfileResponse` gains `hasPassword`, computed by a correlated `EXISTS` **wherever the DTO is
  shaped**, told only to the player it is about, **required** by the client's parser so a missing
  field reads `unavailable` rather than `false`, and refreshed by one re-read of `GET /api/me` after
  sign-up's `signed-up`.

## Design notes

### The deletions are startable now; only the block waits on the field

`ADR-0125` §4 holds §3's block until `DEC-147` is answered — it is, by `ADR-0132` — but §§1, 2, 5
and 6 never needed a new fact at all. The split puts the four deletion tickets first
(`TASK-140801`–`TASK-140804`) so the largest, most mechanical part of the story is merged before the
field's chain begins. Nothing in the deletions reads `hasPassword` and nothing in the field's chain
reads the offer.

### `hasPassword` is computed in **three** statements, never a literal

`ADR-0132` §2: a fourth correlated `EXISTS` in `PostgresProfileReads.PROFILE_OF_SQL`, **and the same
`EXISTS` in both statements in `PostgresProfileWrites`** (`SET_NAME_SQL` and `CURRENT_PROFILE_SQL`),
with the kind **bound** from `CredentialKind.PASSWORD` rather than spelled as SQL. `TASK-140805`
carries all three, and `TASK-140806`/`TASK-140807` read the answer back out of a real database on
each — because a boolean wired to a constant in either direction reddens nothing otherwise
(`TASK-041641` proved exactly that about `hasRecoveryEmail`, by mutation, on this same statement).

### The server lands before the client, and that ordering is the whole coupling

`ADR-0132` §7 calls this *"one diff"*, and names the failure it is protecting against: *"if the
client requires it and the server does not send it, every profile read becomes `unavailable`."* That
failure is **one-directional** — the ADR's own §Consequences says so: *"If the server ships the field
and the client's parser does not require it, nothing breaks."* The split honours the invariant by
**ordering** rather than by one commit: `TASK-140805` (the field, computed) → `TASK-140808` (the
document row) → `TASK-140809` (the client's parser requires it). Every intermediate merge leaves a
working product, and nothing in CI couples the two sides — there is no gate that boots the Kotlin
server against the built client. Both halves are `atomic:` in their own right, sized by the
`ADR-0070` probe against the gate set in `.github/workflows/build.yml`, run in full until it exited
`0`.

### The words are the card's

`ADR-0125` §3 puts the words, the heading, the order and the layout on the design card
(`ADR-0091` §2), inside three obligations (what the profile is, what it costs, the way out and what
it keeps) and three refusals (**not a tier** — no badge, no rank, no *real* account; it **states**
and never urges; the heading stays `Account`). `TASK-140810` draws them on
`design/screens/account.html`'s *No password yet* frame first; `TASK-140811` copies them into
`account-text.ts` character for character.

### `ADR-0119` merged: a player may arrive named, or having skipped

`ADR-0125` §6 — *anonymous* is a state about credentials and never about names. A player who took a
name at the first press and holds no password is **named and anonymous**; a player who skipped and
signed up is **nameless and claimed**. `TASK-140812` renders the block for a profile with a
`displayName` and for one without, on the same `hasPassword: false`, and asserts the block's words
are identical in both — a screen that mentioned the name would fail there.

### Sign-up is the one transition that flips the answer with nothing re-asking

`ProfileProvider` reads `GET /api/me` **once**, at mount. `sign-in` and `sign-out` both `reload`, so
the read is rebuilt; `sign-up.ts` passes `noReload` **on purpose**, and answers `signed-up` on the
`201` whether or not the follow-up sign-in succeeds. So without `ADR-0132` §6 the account screen
would print *anonymous* beside `This profile now has a password.` — two contradictory sentences on
one screen, which is the exact defect `TASK-120601` already found once here.

`TASK-140813` gives the provider a re-read; `TASK-140814` wires it to `signUp`'s `signed-up` and
**tests the transition**, not the field: one render, two different answers from `GET /api/me`
(`hasPassword: false`, then `true`), the form submitted in between, and the block gone afterwards
while `SIGNED_UP` stands.

### `Lobby.test.tsx` has a pre-existing order dependence, and `TASK-140801` inherits it

Measured while splitting, on `develop` at `9dd8571c`, with **production sources untouched**:
deleting only the test `answers from either control, and only Not now takes the offer off the
screen` makes `puts the attach form on the account screen, wired to the account seam` fail. The
account screen renders, then the address loses its `#/account` fragment mid-test and the tree
returns to the front door for good.

The cause is jsdom, not the product: an `<a href="/">` click in an earlier test leaves a refused
navigation that jsdom retires in a **later task**, and that task drops a fragment a later test set
before rendering. Today the deleted offer test absorbs it. `ADR-0125` §1 deletes that test, so
`TASK-140801` owns the repair — and it may not repair it with a literal `setTimeout`, because
`web-client/src/virtual-time.test.ts` fails any test file that names a timer without installing fake
ones. A repair that passes the whole gate set was verified while splitting and is written into the
ticket.

### Serialisation

**`STORY-1408` and `STORY-1410` must not run at the same time.** Both edit `docs/protocol.md`'s
*Profile endpoint* section and `web-client/src/profile/profile.ts` — `ADR-0132`'s `hasPassword` here,
`ADR-0135`'s `GET /api/me/device` there. `EPIC-14` §Scope states the constraint; whichever runs
second rebases.

`STORY-1409` is **not** in tension: `ADR-0134` §Consequences records that
`PostgresProfileReads`/`PostgresProfileWrites` *"change not at all"* under the rename, and it edits
`docs/protocol.md`'s *Set display name* section rather than the profile one.

## Tasks

| ID | Title | Status |
| --- | --- | --- |
| [TASK-140801](../tasks/TASK-140801-the-offer-leaves-the-result-screen.md) | The offer leaves the result screen | ready |
| [TASK-140802](../tasks/TASK-140802-the-offers-modules-and-its-browser-key-leave-the-product.md) | The offer's modules and its browser key leave the product | backlog |
| [TASK-140803](../tasks/TASK-140803-the-app-test-stops-mocking-two-exports-that-are-gone.md) | The app test stops mocking two exports that are gone | backlog |
| [TASK-140804](../tasks/TASK-140804-the-front-door-stops-describing-an-absence.md) | The front door stops describing an absence | backlog |
| [TASK-140805](../tasks/TASK-140805-the-profile-says-whether-it-holds-a-password.md) | The profile says whether it holds a password | backlog |
| [TASK-140806](../tasks/TASK-140806-the-password-flag-is-that-players-and-only-a-password-sets-it.md) | The password flag is that player's, and only a password sets it | backlog |
| [TASK-140807](../tasks/TASK-140807-a-name-write-answers-with-the-password-flag-it-read.md) | A name write answers with the password flag it read | backlog |
| [TASK-140808](../tasks/TASK-140808-the-document-names-the-password-field.md) | The document names the password field | backlog |
| [TASK-140809](../tasks/TASK-140809-the-client-requires-the-password-field-or-reads-nothing.md) | The client requires the password field, or reads nothing | backlog |
| [TASK-140810](../tasks/TASK-140810-the-card-draws-the-anonymous-block.md) | The card draws the anonymous block | backlog |
| [TASK-140811](../tasks/TASK-140811-the-words-the-anonymous-block-says.md) | The words the anonymous block says | backlog |
| [TASK-140812](../tasks/TASK-140812-the-account-screen-names-the-anonymous-profile.md) | The account screen names the anonymous profile | backlog |
| [TASK-140813](../tasks/TASK-140813-the-profile-provider-can-be-told-to-read-again.md) | The profile provider can be told to read again | backlog |
| [TASK-140814](../tasks/TASK-140814-a-confirmed-sign-up-re-reads-the-profile.md) | A confirmed sign-up re-reads the profile | backlog |

## Acceptance criteria

- [ ] No file under `web-client/src` contains `AccountOffer`, `offerAccount`, `offerSettledHere`,
      `settleOfferHere` or `pd.accountOfferSettled`, and no screen renders an account offer after a
      duel.
- [ ] `No profile yet.` appears in no file under `web-client/src`, and `docs/test-plan.md` no longer
      claims a fresh profile's first render is that string.
- [ ] `GET /api/me` answers `hasPassword`, true exactly for a player holding a `password` credential,
      read back out of a real database on all three statements that shape a `ProfileResponse`.
- [ ] A `GET /api/me` body without `hasPassword` makes the profile read `unavailable`, never a
      profile with `hasPassword: false`.
- [ ] The account screen renders `ADR-0125` §3's block for a profile in hand whose `hasPassword` is
      `false`, and for no other state — not for `loading`, `no-profile`, `unavailable`, or a profile
      that holds a password.
- [ ] A sign-up answered `signed-up` re-reads `GET /api/me`, and the block leaves the screen in the
      same render that `SIGNED_UP` arrives on.
- [ ] `./gradlew check -PrequireDocker=true` and, in `web-client`, `npm run check` and
      `npm run build` all exit 0 on every ticket.

## Out of scope

- **The `hasRecoveryEmail` literal in `PostgresProfileWrites.toProfile()`.** It passes a literal
  `false`, and `ProfileProvider.reportNameWrite` adopts that body wholesale, so a player with a
  verified recovery address who sets a display name reads `RECOVERY_OFF` until the tab next boots.
  `ADR-0132` §Residuals names it, calls it *"a ticket for the planner, not a `DEC`"* and deliberately
  does not fix it. It is **not ticketed yet** and does not belong here — the human's instruction on
  this story is to name it and to make sure `hasPassword` does not join it, which `TASK-140805` and
  `TASK-140807` are what enforce.
- **Anything that points a player at the account screen from the result screen.** `ADR-0125` §2:
  no banner, no badge, no toast, no reminder, no second prompt anywhere. A win is two presses from
  the password form and the ADR refuses to shorten it.
- **Re-keying `showPasswordRoute`, `showSignUp`, `showSignInDoor` or `showAttach`.** `ADR-0132` §5
  keeps every one of their predicates, and `ADR-0125` §4 says so in as many words. This story adds
  one block's condition and moves no other.
- **A `hasPassword` on any other DTO, route or list.** `ADR-0132` §3, and nothing enforces it
  structurally — it is a reviewed rule, named in the ADR so it cannot evaporate.
- **Making the whole-client arc's stub server remember a password.** `web-client/src/e2e/account-server.ts`
  answers `hasPassword: false` for every player after `TASK-140809`; an arc that signs up and then
  reads `true` would prove `TASK-140814` end to end and is worth a later ticket. Not ticketed yet.
- **The sign-out half of the account screen** — that is `STORY-1410` and `ADR-0135`, and it edits two
  of the same files, which is why the two stories are serialised.
- **The rename form on the account screen** — `STORY-1409`, `ADR-0130` and `ADR-0134`.
