---
schema: 2
id: TASK-040704
title: A browser never seen signs in, and the socket names no device
type: task
status: done
parent: STORY-0407
module: poker-server
estimate: S
tier: sonnet
review: standard
files_touched: 1
labels: [server, e2e, auth, socket]
depends_on: [TASK-040703]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.e2e.RecoveryOnAFreshBrowserTest' -PrequireDocker=true
  - ./gradlew :poker-server:ktlintCheck
  - ./gradlew :poker-server:detekt
---

## Goal

A device id the server has never seen signs in with the handle and password, opens `/ws` carrying
**both** that device id and the token, and is seated as the account — with `Welcome.deviceId` null,
because this connection's identity did not come from a device id (`ADR-0030` §2).

## Files

| File | Action |
| --- | --- |
| `poker-server/src/test/kotlin/duels/poker/server/e2e/RecoveryOnAFreshBrowserTest.kt` | modify |

Read, and do not edit:
`poker-server/src/test/kotlin/duels/poker/server/e2e/IdentityMovesNoCoinTest.kt` (its `signIn` helper
is the one to copy),
`poker-server/src/test/kotlin/duels/poker/server/e2e/E2eServer.kt` (`completeHandshake`),
`docs/adr/ADR-0027-the-session-outranks-the-device-id.md` §3 and §4.

## Scope

- Three steps appended to `runRecovery()`, in exactly this order:
  1. **The positive control, first.** `client.webSocketSession("/ws").completeHandshake(winner.deviceId)`
     — the winner's own device, no token — and keep the `Welcome` it returns. This must come *before*
     step 3: `ADR-0018` gives a player one live socket and the newest wins, and both handshakes
     resolve to the same player, so the later one evicts this one.
  2. `POST /api/auth/sign-in` with `RECOVERY_HANDLE` and `RECOVERY_PASSWORD`, asserted `200 OK`,
     returning the session token. **The request carries no `X-Device-Id` and no `Authorization`** —
     sign-in reads neither (`ADR-0030` §2, `AuthRoutes.kt`'s own KDoc), which is precisely what lets
     a browser that has never connected recover an account.
  3. `client.webSocketSession("/ws").completeHandshake(FRESH_DEVICE, token)` — **the device id and
     the token together, in one `Hello`** — and keep the `Welcome` it returns.
- `dataSource.assertCoinInvariantHolds(...)` after step 2 and after step 3, with two new distinct
  step strings. `runRecovery()` then holds six calls in total.
- The file-private constant `FRESH_DEVICE = "e2e-fresh-browser"`, declared here because this is the
  first ticket to read it. `RECOVERY_HANDLE` and `RECOVERY_PASSWORD` already exist, from
  `TASK-040703`.
- One private `HttpClient.signIn(handle, password): String` helper, copied from
  `IdentityMovesNoCoinTest`: asserts `200`, decodes `SignInResponse` with `protocolJson`, returns
  `sessionToken`.
- Three new `RecoveryRecord` fields, each with a `@property` line: `sessionToken: String`,
  `originalWelcome: ServerMessage.Welcome` and `freshWelcome: ServerMessage.Welcome` — the whole
  frames, so `playerId` and `deviceId` are both available to the tests below.

## Out of scope

- Reading the profile or the duel list from the fresh browser — `TASK-040705`, same file.
- Any snapshot of `player` or `device_binding` — `TASK-040706`, same file, which brackets everything
  this ticket adds.
- Playing a duel from the fresh browser. `STORY-0406` already proved a duel under a session token
  pays the account; nothing here needs a second one.
- Any file under `poker-server/src/main`.

## Tests

`RecoveryOnAFreshBrowserTest`

| Test | Proves |
| --- | --- |
| `theFreshBrowsersWelcomeNamesTheRecoveredAccount` | `freshWelcome.playerId` equals `winnerProfileAfterDuel.playerId` — the id read over HTTP before either client knew who would win, not a value re-derived later |
| `theFreshBrowsersWelcomeCarriesNoDeviceId` | `freshWelcome.deviceId` is `null`, on a `Hello` that carried a device id |
| `theOriginalDevicesWelcomeStillCarriesItsDeviceId` | `originalWelcome.deviceId` equals `winnerDeviceId`, and is not null. **The positive control the test above cannot do without**: a `Welcome` whose `deviceId` were *always* null would satisfy the null assertion for free, and this is the input that says otherwise |

## Acceptance criteria

- [ ] `RecoveryOnAFreshBrowserTest.theFreshBrowsersWelcomeNamesTheRecoveredAccount` passes
- [ ] `RecoveryOnAFreshBrowserTest.theFreshBrowsersWelcomeCarriesNoDeviceId` passes
- [ ] `RecoveryOnAFreshBrowserTest.theOriginalDevicesWelcomeStillCarriesItsDeviceId` passes
- [ ] The fresh browser's handshake is `completeHandshake(FRESH_DEVICE, token)` — a device id **and**
      a token in the same `Hello`. No call anywhere in the file passes `null` as `completeHandshake`'s
      first argument
- [ ] The sign-in request sets neither `X-Device-Id` nor `Authorization`
- [ ] The original device's handshake is taken before the fresh browser's
- [ ] `runRecovery()` contains exactly six calls to `assertCoinInvariantHolds`, with six different
      step strings
- [ ] Every test method added by `TASK-040702` and `TASK-040703` still passes with its assertions
      unchanged
- [ ] The diff touches exactly one file, and it is the one in the *Files* table
- [ ] Every command in `verify:` exits 0

## Proof

In `poker-server/src/main/kotlin/duels/poker/server/DuelSocket.kt`, change the `Identity.Session` arm
of `serve`'s `when` from `deviceId = null` to `deviceId = hello.deviceId`. It compiles: `hello` is in
scope and `Welcome.deviceId` is `String?`.

**`theFreshBrowsersWelcomeCarriesNoDeviceId` reddens, and it is the only method in this class that
does.** Trace it: that `Welcome` now carries `"e2e-fresh-browser"`, failing *expected null, got
e2e-fresh-browser*. `theFreshBrowsersWelcomeNamesTheRecoveredAccount` reads `playerId`, which the
mutation does not touch. `theOriginalDevicesWelcomeStillCarriesItsDeviceId` goes through the
`Identity.Device` arm, which the mutation does not touch. No row is written either way, so the six
invariant calls pass and `theRecoveryArcMovesNoCoin` stays green. Revert.

**Outside this ticket's `verify:`, and expected:**
`DuelSocketSessionIdentityTest.aTokenSeatsItsPlayerAndNamesNoDevice` reddens on the same mutation.
That is the unit-level gate for the same rule and is not a defect in this Proof.

A second mutation, for the `playerId` half. In the same arm, seat the device instead of the session:

```kotlin
is Identity.Session -> {
    val resolved = deps.directory.resolve(DeviceId(checkNotNull(hello.deviceId)))
    resolved to ServerMessage.Welcome(playerId = resolved.id.value, deviceId = null)
}
```

`DeviceId` is already imported in that file. **`theFreshBrowsersWelcomeNamesTheRecoveredAccount`
reddens alone**: the fresh browser is seated as a freshly minted player, so the `Welcome` names a
UUID that is not the winner's. The `deviceId` is still null, so the second test stays green; the
original device still goes through `Identity.Device`, so the third does; and the minted player has
balance `0` and no `duel_result` rows, so **P1 and P2 both still hold** and the invariant does not
fire. Revert.

That second mutation is the exact defect `TASK-040706` gates from the other side — it mints the
orphan profile this story exists to forbid — and until `TASK-040706` merges, the row it leaves behind
is invisible.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.

## Notes

**The fixture that would look like it covers this and could not.** A `Hello` sent as
`completeHandshake(null, token)` also produces a `Welcome` with a null `deviceId`, and also passes
`theFreshBrowsersWelcomeCarriesNoDeviceId` — while proving nothing about a browser *bearing a
different device id*, which is the story's actual claim. `TASK-040609` shipped the same shape in
reverse: an unresolvable device id paired with a valid token, so the resolver returned before the
device was ever consulted. The device id and the token must arrive together.

**A real client keeps sending its device id whether or not it holds a token** (`ADR-0030` §8), so
step 3 is what a browser actually does, not a contrivance for the test.

**The password stops being decoration here, and the mutation that proves it is asymmetric.**
`TASK-040703` passed `RECOVERY_HANDLE` and `RECOVERY_PASSWORD` to `signUp` and asserted only the
`201`, deferring the real proof to this ticket. The obvious mutation — edit the constant's literal —
is **inert**, because sign-up and sign-in read the same constant and both ends move together; the
coder ran it and reported `BUILD SUCCESSFUL` rather than substituting silently. What reddens is an
asymmetric mismatch: sign-in presenting a literal that sign-up did not use answers `401` inside the
`signIn` helper's status assertion, failing all six tests because `runRecovery()` is shared setup.
The reviewer reproduced both directions. The deferral arrived somewhere real.

**The Proof named no mutation for its own positive control.** It gives one for each of the two new
`Welcome` assertions and then only *argues* that neither touches
`theOriginalDevicesWelcomeStillCarriesItsDeviceId`. An argument is not a gate: without a mutation,
nothing establishes that the positive control can fail, and a control that cannot fail leaves the
null assertion beside it unguarded. The coder devised one — blanking `deviceId` in `DuelSocket`'s
`Identity.Device` arm — and it reddens that test alone; the reviewer reproduced it. Twelfth `## Proof`
in this run found wrong or incomplete when actually executed.

**"The sign-in request sets neither header" is structural, not asserted.** `AuthRoutes` reads neither
`X-Device-Id` nor `Authorization` on this route (its own KDoc, `ADR-0030` §2), so a header added by
mistake would redden nothing. The criterion is held by the helper not writing them, which a reader
verifies by reading it. Recorded rather than tested, on the precedent set at `DELETE /api/me/device`
(`TASK-040609`), where a malformed credential is likewise indistinguishable from an absent one.
