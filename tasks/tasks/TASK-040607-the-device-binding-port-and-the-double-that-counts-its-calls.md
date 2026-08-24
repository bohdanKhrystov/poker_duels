---
schema: 2
id: TASK-040607
title: The device-binding port, and the double that counts what it was asked
type: task
status: done
parent: STORY-0406
module: poker-server
estimate: S
tier: haiku
review: standard
files_touched: 3
labels: [server, auth, port, revocation]
depends_on: [TASK-040606]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.auth.RecordingDeviceBindingsTest'
  - ./gradlew :poker-server:ktlintCheck
  - ./gradlew :poker-server:detekt
---

## Goal

`DeviceBindings` exists as a one-function port with no implementation in `main`, and the test
sources gain the double every route test in this story records against — one that reports a **call
count**, not a boolean.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/auth/DeviceBindings.kt` | create |
| `poker-server/src/test/kotlin/duels/poker/server/auth/RecordingDeviceBindings.kt` | create |
| `poker-server/src/test/kotlin/duels/poker/server/auth/RecordingDeviceBindingsTest.kt` | create |

Read `poker-server/src/main/kotlin/duels/poker/server/auth/AuthSessions.kt` — the port-here,
implementation-there shape and the KDoc register to copy — and
`docs/adr/ADR-0049-a-device-binding-is-a-row-and-revoking-is-final.md` §2 and §5. Nothing else.

## Scope

- The port, in `duels.poker.server.auth`:

  ```kotlin
  public interface DeviceBindings {
      public suspend fun revoke(playerId: PlayerId, keeping: SessionToken)
  }
  ```

- **One function, and it takes no device id.** `ADR-0049` §2: the statement names the player, never a
  device, because there is at most one live binding per player and a client asserts no fact
  (`ADR-0002`). A parameter carrying a device id would be the shape in which one player revokes
  another's, and the KDoc says so.
- **`keeping` is the caller's own token, and it is not optional.** `ADR-0049` §5 requires a session,
  so there is always exactly one row to exclude from the sweep `ADR-0050` §1 adds, and
  `ADR-0037`'s *"revocation does not kill the revoking session"* holds by construction rather than
  by care. A nullable parameter would make "sweep everything, including the caller" reachable.
- **The KDoc states what one call does**, so no reader has to open the implementation: it marks this
  player's live binding revoked if one is live, deletes every `auth_session` row for this player
  except the one `keeping` names, does both in one transaction, and is a no-op with respect to
  `player`, `credential`, `duel` and `duel_result`. It returns `Unit`: a player who was never bound
  and a player whose binding was live are told apart by nothing (`ADR-0049` §5's uniform `204`).
- `internal class RecordingDeviceBindings : DeviceBindings` in the **test** sources, in its own file
  named after it — ktlint's filename rule gives a single top-level declaration the file name it
  matches, so a `DeviceBindingDoubles.kt` holding one class fails `ktlintCheck` and cannot be
  auto-corrected. It appends `RevokeCall(playerId, keeping)` to a `val revokeCalls: MutableList<…>`
  and does nothing else.
- It lives in `duels.poker.server.auth`'s test source set so the `http` suite can reach it.

## Out of scope

- Any implementation against PostgreSQL — `TASK-040608`.
- The route — `TASK-040609`.
- A `holdsLiveBinding` or `isLive` function on this port. The account screen reads
  `ProfileResponse.deviceRouteLive` (`ADR-0049` §5), which `TASK-040602` already ships from
  `PostgresProfileReads`. A second way to ask the same question is a second answer to keep in sync.

## Tests

`RecordingDeviceBindingsTest`

| Test | Proves |
| --- | --- |
| `aFreshDoubleHasRecordedNothing` | `revokeCalls` is empty before anything is called. This is the baseline every "nothing was written" assertion downstream reads against; without it, an emptiness assertion elsewhere could be satisfied by a list that is always empty |
| `oneRevokeRecordsExactlyOneCallWithBothArguments` | After `revoke(PlayerId("p-1"), SessionToken("t-1"))`, `revokeCalls.size` is `1` and its single element equals `RevokeCall(PlayerId("p-1"), SessionToken("t-1"))` |
| `twoRevokesRecordTwoCallsInOrder` | After a second `revoke(PlayerId("p-2"), SessionToken("t-2"))`, `revokeCalls.size` is `2` and the two elements are in call order. **A count, not a boolean** — `ADR-0049` §5's *"writes nothing"* guards are checked downstream by this list's `size`, and a double that collapsed repeats into a flag could not tell one call from two |

## Acceptance criteria

- [ ] `RecordingDeviceBindingsTest.aFreshDoubleHasRecordedNothing` passes
- [ ] `RecordingDeviceBindingsTest.oneRevokeRecordsExactlyOneCallWithBothArguments` passes
- [ ] `RecordingDeviceBindingsTest.twoRevokesRecordTwoCallsInOrder` passes
- [ ] `DeviceBindings` declares exactly one function, and that function has exactly two parameters,
      neither of them a `DeviceId`
- [ ] Nothing under `poker-server/src/main` implements `DeviceBindings`
- [ ] `./gradlew :poker-server:ktlintCheck` and `./gradlew :poker-server:detekt` both exit 0
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.

## Notes

**This double decides what the rest of the story can prove.** `revokeCalls` is an appending
`MutableList<RevokeCall>` carrying **both** arguments — `playerId` and `keeping` — so a later ticket
can assert "exactly one revocation, naming this session" rather than merely that something was
revoked. Three mutations confirm it: removing the recording, swapping the list for a counter, and
clearing before appending all redden.

Two precedents made that explicit rather than incidental. `RecordingAuthSessions` had to be widened
mid-story because it recorded `issued` but not `deleted`. And a permissive `SocketFixtures` default
would have made every socket test pass regardless of identity. Here `revoke()` only records, and the
port has no query method for a permissive default to hide in.

**`keeping` is a `SessionToken`, not a flag.** `ADR-0050` ends every *other* session and keeps the
revoking one, so a boolean could not express which. The port is `suspend`, matching `AuthSessions`
and `PlayerDirectory`, so reaching Postgres later needs no signature change across every caller.

