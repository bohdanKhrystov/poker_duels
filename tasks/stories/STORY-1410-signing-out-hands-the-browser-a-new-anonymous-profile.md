---
id: STORY-1410
title: Signing out hands the browser a new anonymous profile, and the confirmation says so
type: story
status: done
parent: EPIC-14
module: web-client
labels: [client, server, account, design]
depends_on: []
---

## Goal

A player who signs out of **their own** account gets a new, empty anonymous profile, and the
confirmation says so **before** they press. A player who signs out of **somebody else's** account
gets back the profile this browser already owned, and the confirmation says the sentence it says
today. Nothing is revoked, deleted, moved or destroyed on either path, and nothing crosses the
socket.

## Why

**It is `EPIC-14` item 1d**, the human's own words on 2026-09-06: *"if logout you comeback to new
annonumus profile"*. Today *Sign out* leaves the browser inside the account it just left — the same
player, the same coins, no password asked for — which is the defect
[`ADR-0131`](../../docs/adr/ADR-0131-signing-out-of-your-own-account-hands-the-browser-a-new-profile.md)
exists to fix.

**Both halves are answered and merged.** `ADR-0131` is the product half — `DEC-132`, the product
owner's — and
[`ADR-0135`](../../docs/adr/ADR-0135-the-server-says-which-sign-out-this-is-and-the-browser-forgets-one-key.md)
is the mechanism — `DEC-152`, the architect's. This story registers nothing and waits on nothing.

**The serialisation against `STORY-1408` is discharged.** `ADR-0135`'s own §Consequences named it:
*"Two tickets are serialised that would otherwise be parallel. `DEC-147`'s answer and this one both
edit `docs/protocol.md`'s `/api/me` section and the client's profile module."* `DEC-147` was
answered by [`ADR-0132`](../../docs/adr/ADR-0132-the-profile-says-whether-it-holds-a-password.md)
and `STORY-1408` has landed it: at `c6a41e6d`, `TASK-140808` has written the `hasPassword` row into
`docs/protocol.md`'s *Profile endpoint* table and `TASK-140809` has made `profileFromBody` require
the boolean in `web-client/src/profile/profile.ts`. **Neither file is opened again by this story** —
this story's document change is a **new section**, `### Device standing`, chained after *Revoke this
device*, and its client read is a **new module**, because `ADR-0135` §Alternatives 1 rejected a
seventh `ProfileResponse` field on a fact rather than a preference. So the conflict that forced the
serialisation cannot recur here, and the constraint is spent.

## Design notes

Everything below is either merged or was **measured in this worktree on `develop` at `c6a41e6d`
on 2026-09-08** against probes that were reverted. No ticket re-litigates any of it.

### The mechanism, in one paragraph

`GET /api/me/device` — new, session-required — answers
`DeviceStandingResponse(signOutHandsANewProfile)`, `true` **exactly when the `X-Device-Id` presented
on that same request does not resolve, through a live binding, to a player other than the caller**
(`ADR-0135` §4). The comparison is one new `IdentityResolver.namesPlayer`, and `resolve` and all
five `Identity` cases stay byte-unchanged. The browser reads that bit when the account screen reads
its profile, hands it to the confirmation, and `signOut` forgets `pd.deviceId` on `true` and on
nothing else, before `reload()` and regardless of what the server answered. The replacement profile
is minted by `Identity.Anonymous` on the **next `Hello`** — shipped, tested and not opened here.

**Nothing crosses the socket.** `Hello`, `Welcome`, `ProtocolCodec`, `protocol.gen.ts` and
`PROTOCOL_VERSION` are byte-unchanged, `ADR-0047`'s ledger gains no row, and no ticket here is
`atomic:` on a protocol gate. **No schema, no migration, no write** on either path.

### Where the fact is read, and why that is a choice this split made

`ADR-0135` §7 names three ends of the wiring — `SignOutControl` gains the boolean as a prop,
`signOut` gains it as a **required** field, `main.tsx` wires the value and passes `false` whenever
the read has not answered — and §8 leaves the rest to the planner: *"The implementing ticket is a
plain ticket under the `files_touched: 1..3` cap, split as the planner sees fit."*

The shape chosen here is **a second provider, mounted beside `ProfileProvider` in `main.tsx`**:
`DeviceStandingProvider` holds `false` until its injected read answers, exactly as `ProfileProvider`
holds `null`. `Lobby` reads it through a hook and hands it to `AccountScreen`, which hands it to
`SignOutControl`. This is a **choice, not a necessity**, and the two alternatives were weighed:

- **Folding it into `readProfileStrip`.** Refused: `ProfileStripState` is all-or-nothing by design
  (*"a balance shown beside a duel list that failed to load reads as 'you have no duels', which is a
  lie about the ledger"*), so an unavailable standing read would blank the front door's whole strip.
  It would also put a request-scoped fact into a state the front-door strip consumes, which is
  `ADR-0135` §Alternatives 1's own line — `ProfileResponse` describes the **player** — applied one
  layer up. `ADR-0135` §Consequences settles it in the other direction anyway: *"The account screen
  makes a second request."* A second request is the shape the ADR priced.
- **`SignOutControl` or `AccountScreen` reading it themselves.** Refused: `ADR-0060` §4 makes
  `AccountScreen` a prop-driven presentation with no hook, no fetch and no provider read, and
  `App.test.tsx` spies on the component on that assumption.

The provider lives in `web-client/src/account/device-standing-provider.tsx`, **not** in `main.tsx`,
for a measured reason: `App.test.tsx`'s `vi.mock("./main")` is an **explicit factory**, so the first
new export `Lobby` imports from `./main` fails 24 of that file's 36 tests (`TASK-140714` measured
this and it has not changed). A provider module of its own costs `main.tsx` a mount and an import
and costs `App.test.tsx` nothing.

### The read must not go through `readFromApi`, and it short-circuits on the other credential

`ADR-0135` §6 names one trap by name: `readFromApi` answers `no-profile` **without making a
request** when the browser holds no device id — *"which is precisely the browser whose answer is
`true`"*. So `readDeviceStanding` is its own module over `plainFetch`, setting
`Authorization: Bearer` itself, exactly as `revoke-device.ts` does.

It **does** short-circuit on the *other* credential: with no session token in storage it makes no
request and answers `false`. That is not the trap inverted — the route is session-required and
answers `401` to a caller with no session, and `401` is a keep (§6), so the short circuit and the
round trip have the same answer. `revoke-device.ts` already takes exactly this shape. `TASK-141006`
carries the control that tells the two apart: a browser with a token and **no** device id must still
send the request.

### Unknown is a keep, and the keep is what every intermediate ticket ships

`ADR-0135` §6: an absent field, a `401`, an unavailable read, a rejected `fetch`, a read that has
not returned and a client too old to ask all mean `false` — the browser keeps `pd.deviceId` and the
confirmation states the returning sentence.

That is also the property that lets this story land in fourteen small tickets without ever shipping
a half-built abandonment. **Every ticket from `TASK-141007` to `TASK-141013` passes a literal
`false` at the seam it has not yet wired**, so the abandoning branch is unreachable — no browser can
lose a device id and no confirmation can state the new-profile sentence — until `TASK-141014`
connects the provider's answer to the screen. `ADR-0131` §Consequences requires behaviour and copy
to ship *"in one diff"*; here they become **true** in one merge, which is the same guarantee, and it
is the reason the order below is the order it is.

### `SIGN_OUT_WARNING` keeps its value and loses its unconditionality

`ADR-0131` §5 is precise about this and the two halves read as if they conflict:

> In the returning case, the shipped second sentence — *"This browser goes back to the profile it
> had before."* — is already exactly right and stays.

and, in §Consequences:

> **`SIGN_OUT_WARNING` is false the day the behaviour lands.**

Both are true, because what is false is the sentence being said **unconditionally**. So
`SIGN_OUT_WARNING`'s literal does not move: `TASK-141008` adds the abandoning sentences as a second
export and a `signOutWarning(handsANewProfile)` selector — the shape `deviceRouteLine(live)` already
uses, and for the same stated reason, that *a component choosing between the two sentences inline
would be a second place able to get it wrong*.

**Measured consequence of keeping the literal:** `web-client/src/e2e/claimed-here-recovered-there.test.tsx`
asserts `getByText(SIGN_OUT_WARNING)` twice, and both are browser B signed in to A's account —
`ADR-0131` §1 **row two**, the returning case, which the file pins with
`expect(readDeviceId(storageB)).toBe(PLAYER_SEAT_1.deviceId)`. That file is therefore **not opened
by this story**, and neither is `src/e2e/account-server.ts`: its unknown-path branch answers `500`,
`readDeviceStanding` turns that into `false`, and its request log filters on
`request.path === "/api/me"` by exact equality, so `/api/me/device` matches no assertion. All eight
of its tests were green under the staged probe.

### The words are the card's, and the card comes first

`ADR-0131` §5 owes the abandoning confirmation **two obligations** — that this browser will be a
new, empty profile afterwards and not the one it is now; that the profile being left keeps its duel
coins and its duels and is reached again by signing in with the password — inside `ADR-0125` §3's
**three refusals**: it states and does not urge, it is not a warning about loss, and *the words are
the card's*. `ADR-0135` mints no string and says so.

`design/screens/account.html` draws the account screen and has **never drawn the sign-out
confirmation step**: it draws the `Sign out` button and stops. So `TASK-141001` is the card
(`ADR-0091` §2) and it is first in the chain; `TASK-141008` transcribes.

### What was measured, and where the numbers come from

All at `c6a41e6d`, with `npm ci` run in this worktree. Per-file counts are **measured, never
computed** — every ticket below that pins one states the current number and the tests it adds.

| Fact | Measured |
| --- | --- |
| `src/account/sign-out.test.ts` | 6 tests |
| `src/account/SignOutControl.test.tsx` | 5 tests |
| `src/account/account-text.test.ts` | 7 tests |
| `src/account/AccountScreen.test.tsx` | 20 tests |
| `src/account/no-secret-in-a-url.test.ts` | 5 tests |
| `src/account/account-provider.test.tsx` | measured by its own ticket; the call site is line 198 |
| `src/protocol/device-id.test.ts` | 4 tests |
| `src/lobby/Lobby.test.tsx` | 113 tests |
| `src/App.test.tsx` | 36 tests |
| `src/e2e/claimed-here-recovered-there.test.tsx` | 8 tests |
| whole client suite | 126 files, 1220 tests |
| `IdentityResolverTest` | 8 tests |
| `DeviceRouteTest` | 10 tests |
| `HttpEndpointDocumentationTest` | 44 tests |
| `ProfileDtosTest` | 27 tests, and it enumerates no DTO — a new one reddens nothing |
| `App.test.tsx`'s `occurrencesIn(mainSource, "fetch: plainFetch")` | **7**, and `TASK-141012` moves it to 8 |
| `deviceRoutes` installation | `Application.kt:103` already passes `components.identities`, so no wiring file moves |

### Two `atomic:` tickets, both probed to `exit 0`, and neither is a grouping of convenience

`ADR-0070`'s loop was run — stub, run the gate set `build.yml` runs on a pull request in full, apply
minimal propagation, run again, stop at `exit 0`, revert. Both arms ran `cd web-client && npm run
check` (typecheck, lint, format, `vitest run`) and `npm run build` to `exit 0`, and
`./gradlew check -PrequireDocker=true` was run to `exit 0` as well, which is only worth saying
because it names no file on this path: **not one Kotlin source is in either arm**.

- **`TASK-141007` — 5 files.** `signOut`'s request object gains `handsANewProfile` as a **required**
  field, which `ADR-0135` §7 requires in as many words: *"not an optional one with a safe default.
  `false` is the safe value, but an omitted argument is a feature that silently does not exist."*
  `tsc --noEmit` then names, in one run, `src/main.tsx`, `src/e2e/drive-arc.tsx`,
  `src/account/sign-out.test.ts` (seven call sites) and `src/account/no-secret-in-a-url.test.ts`.
  The gate is `npm run check`, in `build.yml`'s `client` job.
- **`TASK-141010` — 4 files.** `AccountCalls.signOut` gains the parameter, and `tsc` names
  `src/main.tsx`, `src/e2e/drive-arc.tsx` and — the one a reading would have missed —
  `src/account/account-provider.test.tsx:198`, which calls `receivedCalls!.signOut()` with no
  argument. It cannot be staged: the interface and its three dependents break in the same edit.

**Everything else is a split, and three groupings that looked atomic are not.** Probing found that
adding a required prop to `SignOutControl` names exactly three files, that adding one to
`AccountScreen` names exactly three, and that the provider's mount names two — because the module
can land unused first. Each of those would have been an `atomic:` claim taken from memory, and each
is false.

## Tasks

| ID | Title | Status |
| --- | --- | --- |
| [TASK-141001](../tasks/TASK-141001-the-account-card-draws-both-sign-out-confirmations.md) | The account card draws both sign-out confirmations | backlog |
| [TASK-141002](../tasks/TASK-141002-the-resolver-answers-whether-a-device-names-a-player.md) | The resolver answers whether a device names a player | backlog |
| [TASK-141015](../tasks/TASK-141015-the-resolver-stops-answering-a-comparison-and-the-smoke-test-names-an-unasked-verb.md) | The resolver stops answering a comparison, and the smoke test names a verb nobody asked for — *inserted by `ADR-0144`; numbered fifteenth, runs third* | ready |
| [TASK-141003](../tasks/TASK-141003-the-server-says-which-sign-out-this-is.md) | The server says which sign-out this is | backlog |
| [TASK-141004](../tasks/TASK-141004-the-document-names-the-device-standing-route.md) | The document names the device-standing route | backlog |
| [TASK-141005](../tasks/TASK-141005-the-device-id-can-be-forgotten-by-its-own-module.md) | The device id can be forgotten, by its own module and no other | backlog |
| [TASK-141006](../tasks/TASK-141006-the-browser-asks-which-sign-out-this-is.md) | The browser asks the server which sign-out this is | backlog |
| [TASK-141007](../tasks/TASK-141007-signing-out-forgets-the-device-id-when-it-is-told-to.md) | Signing out forgets the device id when it is told to, and never otherwise | backlog |
| [TASK-141008](../tasks/TASK-141008-the-confirmation-has-a-sentence-for-each-sign-out.md) | The confirmation has a sentence for each of the two sign-outs | backlog |
| [TASK-141009](../tasks/TASK-141009-the-confirmation-states-the-sign-out-it-is-about-to-make.md) | The confirmation states the sign-out it is about to make | backlog |
| [TASK-141010](../tasks/TASK-141010-the-account-calls-carry-which-sign-out-this-is.md) | The account calls carry which sign-out this is | backlog |
| [TASK-141011](../tasks/TASK-141011-a-provider-holds-the-answer-and-false-until-it-lands.md) | A provider holds the answer, and `false` until it lands | backlog |
| [TASK-141012](../tasks/TASK-141012-the-boot-reads-the-device-standing-once.md) | The boot reads the device standing once, beside the profile | backlog |
| [TASK-141013](../tasks/TASK-141013-the-account-screen-is-told-which-sign-out-it-offers.md) | The account screen is told which sign-out it offers | backlog |
| [TASK-141014](../tasks/TASK-141014-the-lobby-hands-the-screen-the-answer-the-server-gave.md) | The lobby hands the screen the answer the server gave | backlog |

## Acceptance criteria

- [ ] `GET /api/me/device` answers `401` with an empty body to every caller without a session, and
      `200` with `{"signOutHandsANewProfile":…}` to one with a session.
- [ ] The bit is `true` for a session-identified caller whose presented device id resolves to
      themselves, `true` when no device id is presented, `true` when the presented binding is
      revoked, and `false` only when a live binding names another player.
- [ ] `IdentityResolver.resolve` and all five `Identity` cases are byte-unchanged.
- [ ] `signOut` forgets `pd.deviceId` when and only when it is told the sign-out hands a new
      profile, before `reload()` and whatever the server answered; `pd.sessionToken` and
      `pd.roomCode` move as they do today and no other key moves.
- [ ] The confirmation states the abandoning sentences when the answer is `true` and the shipped
      returning sentence when it is `false`, and `signedIn` decides only whether it is offered.
- [ ] `PROTOCOL_VERSION`, `protocol.gen.ts`, `docs/protocol-versions.md`, every migration and every
      `poker-engine` file are untouched.
- [ ] `web-client/src/e2e/claimed-here-recovered-there.test.tsx` and `src/e2e/account-server.ts` are
      untouched and green: the arc they drive is `ADR-0131` §1 row two, and row two does not move.

## Out of scope

- **`ADR-0142`'s `web-client/src/design/card-text.test.ts`.** The ADR is merged; its implementing
  ticket is not written and the file does not exist at `c6a41e6d`. `TASK-141008` therefore owes no
  register row. If that ticket lands first, whichever lands second owes the classification of this
  story's new `account-text.ts` exports — a one-line entry, not a redesign.
- **Deleting or revoking anything.** `ADR-0131` §3: the `device_binding` row the browser walks away
  from stays live, and `ADR-0037`'s revoke is still the only thing that kills the route.
- **An undo, a grace window, or a *sign back in as you were* path.** `ADR-0135` §Consequences
  refuses all three by name.
- **Folding `GET /api/me/device` into `GET /api/me`.** Deliberately deferred by `ADR-0135`
  §*What this does not settle*; not yet ticketed.
- **Clearing `ADR-0119` §5's skipped bit.** `ADR-0131` §6 weighs it and leaves it unamended: a new
  profile is not an answer, so a player who skipped plays as `No name`.
- **`ADR-0125` §1's `pd.accountOfferSettled`.** `STORY-1408` retires it; this story only promises
  not to sweep it.
