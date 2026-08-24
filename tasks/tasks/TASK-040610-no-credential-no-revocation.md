---
schema: 2
id: TASK-040610
title: No credential, no revocation — and the refusal writes nothing
type: task
status: backlog
parent: STORY-0406
module: poker-server
estimate: S
tier: sonnet
review: deep
files_touched: 2
labels: [server, http, route, revocation, security]
depends_on: [TASK-040609]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.http.DeviceRouteTest'
  - ./gradlew :poker-server:ktlintCheck
---

## Goal

`DELETE /api/me/device` answers `409 Conflict` with an empty body, and calls
`DeviceBindings.revoke` zero times, when the player holds no password credential — so a profile
whose only route in is the device can never be stranded.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/http/DeviceRoutes.kt` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/http/DeviceRouteTest.kt` | modify |

Read, and do not edit:
`poker-server/src/main/kotlin/duels/poker/server/http/AuthRoutes.kt` — sign-up's
`credentials.holdsCredential(playerId, CredentialKind.PASSWORD)` guard is the same call in the same
shape — and `docs/adr/ADR-0049-a-device-binding-is-a-row-and-revoking-is-final.md` §5.

## Scope

- One guard, between the `401` and the `revoke`, in that order and no other:

  ```kotlin
  if (!credentials.holdsCredential(playerId, CredentialKind.PASSWORD)) {
      call.respond(HttpStatusCode.Conflict)
      return@delete
  }
  ```

- **The order is the scope.** Identity first, then the credential, then the write. Reversed, an
  unauthenticated caller would learn from a `409` that some profile holds no credential; and a
  credential check after the write would revoke first and refuse afterwards.
- **`409`, and the body is empty.** `ADR-0049` §5: the two guards answer different questions — the
  token requirement is about not stranding the caller's *screen*, the credential check is about not
  stranding the *profile* — and the comment beside the guard says which is which.
- The KDoc on `deviceRoutes` gains the `409` row. It already documents `401` and `204`.

## Out of scope

- Any change to `Credentials`, `CredentialKind` or `RecordingCredentials`. All three exist and are
  used as they are.
- Widening the guard to other credential kinds. `ADR-0041` fixes `kind` at `"password"` for v0.1 and
  v0.2; a second kind here would answer `DEC-031` sideways.
- Wiring — `TASK-040611`. `docs/protocol.md` — `TASK-040612`.

## Tests

`DeviceRouteTest` gains three methods. **Every test already in the file keeps passing, and the
`RecordingCredentials` those tests are built with must be constructed with `holds = true`** — that
is the one edit to existing lines this ticket makes, and it is the edit that keeps
`aSessionRevokesAndGetsTwoHundredAndFour` answering `204`. No existing assertion is removed,
weakened or re-pointed.

| Test | Proves |
| --- | --- |
| `aPlayerWithNoCredentialGetsFourHundredAndNine` | With `RecordingCredentials(holds = false)` and a valid `Authorization: Bearer t-1`, the answer is `409`, the body is `""`, and `bindings.revokeCalls` is **empty**. A count, not a status: `409` alone cannot say whether the write happened first |
| `theCredentialIsCheckedWithThisPlayerAndThePasswordKind` | In the same request, `credentials.holdsCalls` has size `1` and its single element is `PlayerId("player-1") to CredentialKind.PASSWORD` — the player the **session** named, never one from a header or a body |
| `anUnauthenticatedCallerNeverReachesTheCredentialCheck` | With no headers at all, the answer is `401` and `credentials.holdsCalls` is **empty**. This is the ordering assertion: a guard placed before the identity check answers `409` here, and `holdsCalls` would have size `1` |

## Acceptance criteria

- [ ] `DeviceRouteTest.aPlayerWithNoCredentialGetsFourHundredAndNine` passes
- [ ] `DeviceRouteTest.theCredentialIsCheckedWithThisPlayerAndThePasswordKind` passes
- [ ] `DeviceRouteTest.anUnauthenticatedCallerNeverReachesTheCredentialCheck` passes
- [ ] Every test method already in `DeviceRouteTest` still passes, and the only edit to an existing
      line is `RecordingCredentials(holds = true)` at the shared fixture
- [ ] `holdsCredential` is called exactly once per request that reaches it, and never on a request
      answered `401`
- [ ] Every command in `verify:` exits 0

## Proof

Move the credential guard **above** the identity check, resolving the player id after it (bind it to
a constant for the probe). `anUnauthenticatedCallerNeverReachesTheCredentialCheck` reddens on
`holdsCalls` being non-empty, and — with `holds = false` on that request's double — on the status
being `409` rather than `401`. `aPlayerWithNoCredentialGetsFourHundredAndNine` stays **green**,
because a caller who *does* hold a session gets `409` under either order. One test reddens, and it
is the only one in the class whose subject is the order.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.
