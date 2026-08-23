---
schema: 2
id: TASK-021301
title: OfferRematch and RematchOffered reach the wire, and PROTOCOL_VERSION takes its step
type: task
status: ready
parent: STORY-0213
module: poker-server
estimate: S
tier: sonnet
review: standard
labels: [server, protocol, rooms, version-bump]
files_touched: 15
atomic:
  - ProtocolVersionLedgerTest — a wire shape whose fingerprint no ledger row claims fails it
  - ProtocolDocumentationTest — a live type with no row, and a row with no live type, both fail
  - the Kotlin compiler — two exhaustive when expressions in DuelSocket
  - the Kotlin compiler again — two more in test sources, SocketDuel and SocketSecrecyTest
  - verifyProtocolTypes and verifyDuelScript — byte comparisons run on every check
  - tsc TS1360 — the satisfies table in frames.ts, and the ProtocolVersion alias in version.ts
  - vitest — connection.test.ts feeds a Welcome through the client's own version comparison
depends_on: []
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.protocol.ProtocolDocumentationTest'
  - ./gradlew :poker-server:test --tests 'duels.poker.server.protocol.ProtocolVersionLedgerTest'
  - ./gradlew :poker-server:test --tests 'duels.poker.server.protocol.ProtocolPayloadTest'
  - ./gradlew :poker-server:test --tests 'duels.poker.server.protocol.ProtocolDiscriminatorTest'
  - ./gradlew :poker-server:test --tests 'duels.poker.server.protocol.ProtocolJsonTest'
  - ./gradlew :poker-server:check
  - cd web-client && npm run check
---

## Goal

A seated player can put `OfferRematch` on the wire, both seats can be told `RematchOffered(seat)`,
a rematch that cannot be taken yet is refused as `REMATCH_UNAVAILABLE`, and `PROTOCOL_VERSION`
names the new shape — all in one commit, because every gate below refuses any smaller one.

[`ADR-0044`](../../docs/adr/ADR-0044-a-rematch-is-one-intent-and-one-room-fact.md) §§1–4 and §6 are
the specification. Read them and `ADR-0047` §§1–6 before starting; this ticket adds nothing to
either.

## Files

**Fifteen, declared as fifteen.** This is an `atomic:` ticket under
[`ADR-0068`](../../docs/adr/ADR-0068-an-atomic-ticket-names-the-gate-that-forbids-splitting-it.md)
§§3 and 5 as amended by
[`ADR-0069`](../../docs/adr/ADR-0069-the-blast-radius-is-probed-not-remembered.md): the *Why it
cannot be fewer* column names the merged gate holding each file, which is what earns the exemption
from the three-file cap. **A file this table does not name is a decision, not a bigger ticket, so
stop and raise one** — that rule is the count-independent one (`ADR-0069` §2), and it is what
produced `ADR-0069` rather than a silent fifteen-file diff. Two files are produced by a Gradle task
and must never be hand-edited.

The last three rows were found by an implementation attempt, not by planning, and are why `ADR-0069`
exists: the first twelve are what a reading of the protocol directories finds, and the last three are
what running the gates finds. Nothing in them is new behaviour.

| File | Action | Why it cannot be fewer |
| --- | --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/protocol/ClientMessage.kt` | modify | `OfferRematch` |
| `poker-server/src/main/kotlin/duels/poker/server/protocol/ServerMessage.kt` | modify | `RematchOffered` |
| `poker-server/src/main/kotlin/duels/poker/server/protocol/ProtocolError.kt` | modify | `REMATCH_UNAVAILABLE` |
| `poker-server/src/main/kotlin/duels/poker/server/protocol/Protocol.kt` | modify | the bump — `ProtocolVersionLedgerTest` fails on a wire shape whose fingerprint no ledger row claims, so the wire cannot move without it |
| `poker-server/src/main/kotlin/duels/poker/server/DuelSocket.kt` | modify | two `when`s over sealed hierarchies stop compiling the moment either gains a variant |
| `poker-server/src/test/kotlin/duels/poker/server/protocol/ProtocolJsonTest.kt` | modify | `theProtocolVersionIsTwo` asserts the literal `2` |
| `docs/protocol.md` | modify | `everyClientMessageHasARowSayingClientToServer` and `everyServerMessageHasARowSayingServerToClient` fail on a live type with no row; `theDocumentNamesNoMessageThatDoesNotExist` fails on a row with no type — so the document can move neither before nor after the Kotlin. `theDocumentListsEveryProtocolError` and `theDocumentStatesTheCurrentProtocolVersion` add the bullet and the version line |
| `docs/protocol-versions.md` | modify | one ledger row (`ADR-0047` §1) |
| `web-client/src/protocol/protocol.gen.ts` | **regenerate** | `:poker-server:verifyProtocolTypes` runs on every `check` |
| `web-client/src/protocol/version.ts` | modify | the literal is typed against the generated `ProtocolVersion` alias, so `tsc` fails until it moves (`ADR-0020`) |
| `web-client/src/protocol/frames.ts` | modify | `SERVER_MESSAGE_TABLE ... satisfies Record<ServerMessage["type"], true>` — a missing key is TS1360. The file's own comment says this edit is what a new server message wants, and `docs/protocol.md` already documents it |
| `web-client/src/e2e/scripted-duel.gen.json` | **regenerate** | it embeds `"protocolVersion":2` from a `Welcome`, and `:poker-server:verifyDuelScript` runs on every `check` |
| `poker-server/src/test/kotlin/duels/poker/server/e2e/SocketDuel.kt` | modify | an exhaustive `when (message)` over `ServerMessage` with **no `else`**, ending in an ignore-group. `:poker-server:compileTestKotlin` — and so `check` — fails the moment the hierarchy gains a variant |
| `poker-server/src/test/kotlin/duels/poker/server/e2e/SocketSecrecyTest.kt` | modify | the same shape in `leaks()`; its ignore-group ends `is ServerMessage.DuelFinished, -> Unit`. Same compiler failure |
| `web-client/src/protocol/connection.test.ts` | modify | `vitest`, which `npm run check` runs. Four of its tests feed `protocolVersion: 2` through `connection.ts`'s `message.protocolVersion === PROTOCOL_VERSION`, and its *outdated version* case writes the absolute literal `3`, which this bump would turn into the current version |

Read, not edited: `ADR-0044` §§1–4, §6; `ADR-0047` §§1–6; `poker-server/src/main/kotlin/duels/poker/server/room/RematchResult.kt`.

## Scope

### 1. The two messages and the error value

```kotlin
@Serializable
@SerialName("OfferRematch")
public data object OfferRematch : ClientMessage
```

```kotlin
@Serializable
@SerialName("RematchOffered")
public data class RematchOffered(val seat: Int) : ServerMessage {
    init { require(seat in 0..1) { "seat must be 0 or 1, was $seat" } }
}
```

- `RematchOffered` is a member of the `ServerMessage` sealed interface, beside `RoomJoined`.
- KDoc `OfferRematch` with the one fact that makes it field-free: the socket's `RoomMembership`
  names the room and the session names the player, so a client cannot offer a rematch in a room it
  never entered — structurally, not by a check (`ADR-0044` §1). KDoc `RematchOffered` with what it
  means (*an offer from this seat stands*) and that it goes to both seats.
- `ProtocolError` gains `REMATCH_UNAVAILABLE`, KDoc'd as **transient**: nothing was recorded and the
  same offer may be sent again. Add it last, after `FRAME_LIMIT_EXCEEDED`.

### 2. `DuelSocket`

- `serve()`'s `error("handshake() returned …")` branch lists every `ServerMessage` a handshake may
  not return. Add `is ServerMessage.RematchOffered,` to it. One line.
- `replyTo`'s `when` over `ClientMessage` gains `is OfferRematch -> replyToOfferRematch(deps, session, room)`.
- Add `replyToOfferRematch`, which is the whole of `ADR-0044` §§3, 4 and 6:

  | Case | Answer |
  | --- | --- |
  | `room.code` is `null` | `Failure(UNKNOWN_ROOM)` to this socket |
  | `RematchResult.Offered` | `RematchOffered(seat)` to **both** seats through `deliver`, where `seat` is the offering player's seat in the room the registry returned |
  | `RematchResult.Agreed` | `deliver(result.outbound, result.room, deps.connections)` — nothing else, and no frame of this ticket's own |
  | `Refused(UNKNOWN_ROOM)`, `Refused(NOT_A_PLAYER)` | `Failure(UNKNOWN_ROOM)` to this socket |
  | `Refused(NOT_FINISHED)` | `Failure(REMATCH_UNAVAILABLE)` to this socket |
  | `Refused(ALREADY_OFFERED)` | `RematchOffered(seat)` to **this socket only**, with `seat` read off a room fetched fresh from `deps.rooms`; if that room or that seat is gone, `Failure(UNKNOWN_ROOM)` |

- Both seats are addressed by building `listOf(Addressed(0, offered), Addressed(1, offered))` and
  passing it to `deliver` — the same function `replyToAct` and `replyToJoinRoom` use, so a seat with
  no live writer is skipped rather than broadcast to the other.
- The repeat offer answers **only the offering socket**, because that is what `replyToJoinRoom`'s
  `ALREADY_SEATED` branch does with the seat a player already holds, and `ADR-0044` §3 names it as
  the precedent. The opponent was already told when the offer was first recorded.
- The seat is **never** taken from the request — `OfferRematch` carries none — and never cached; it
  comes off the `Room` the registry just returned, for `TASK-020731`'s reason.
- `RematchOffered` is built here, in transport, beside where `RoomJoined` is built. It carries no
  card, no `PlayerView` and nothing derived from engine state, so it does **not** go through the
  projection layer (`ADR-0044` §2).

### 3. The version step, last, and in this same commit

Follow `ADR-0045` §4 and `ADR-0047` §§5–6 exactly:

- Rebase on `develop` immediately before making the change. `PROTOCOL_VERSION` becomes whatever
  `develop` says **plus one**. **This ticket does not name the number**, and neither does any ADR or
  story — a number written down in advance is stale the moment another bump lands.
- Extend `PROTOCOL_VERSION`'s KDoc with one sentence naming what this version is.
- `docs/protocol.md`: the `Protocol version: **N**` line; one `client → server` row for
  `OfferRematch` (fields: *(none)*); one `server → client` row for `RematchOffered` (field: `seat`);
  the `REMATCH_UNAVAILABLE` bullet under *Protocol Errors*, saying it is transient and the offer may
  be re-sent; and one short paragraph stating `ADR-0044` §4's rule — **after a `DuelFinished`, a
  `Snapshot` means a new duel has begun in that room: the rematch**, true because `resumeFrames`
  gives a finished duel `finishedFrames` alone.
- `docs/protocol-versions.md`: append one row. **Do not invent the fingerprint** — write the row
  with a placeholder, run `ProtocolVersionLedgerTest`, and paste the value from its failure message.
  *Claimed by* is `STORY-0213`.
- Regenerate both generated files and commit them:
  `./gradlew :poker-server:generateProtocolTypes` and `./gradlew :poker-server:generateDuelScript`.
- `web-client/src/protocol/version.ts`: the literal moves with the alias.
- `web-client/src/protocol/frames.ts`: add `RematchOffered: true,` to `SERVER_MESSAGE_TABLE`, in
  alphabetical position. Nothing else in the client changes.
- `ProtocolJsonTest.theProtocolVersionIsTwo`: rename to match the new number and move its expected
  value. Change nothing else in that file — `defaultValuesReachTheWire` and `unknownKeysAreRefused`
  keep every assertion they have. **This literal stays a literal** (`ADR-0069` §4 rule 1): it is the
  one Kotlin test whose subject *is* the number, and referencing `PROTOCOL_VERSION` here would make
  it assert nothing.

### 4. The three files a gate names and the twelve above do not

**No behaviour changes in any of them.** Each is a merged gate refusing the smaller commit.

- `SocketDuel.kt`: the `when (message)` at ~line 232 is exhaustive over `ServerMessage` with no
  `else`. Add `is ServerMessage.RematchOffered,` to the existing ignore-group at ~274–278, in the
  order the group already uses. One line. Nothing else in the file is touched — the group already
  means *"a frame this driver does not act on"*, and a rematch offer is one.
- `SocketSecrecyTest.kt`: the same, in `leaks()` at ~line 54. Add
  `is ServerMessage.RematchOffered,` to the ignore-group at ~81–87. One line. `RematchOffered(seat)`
  carries no card and cannot leak one, so the checker has nothing to say about it; the inner
  `when (event)` over engine events is not touched.
- `connection.test.ts`: **convert, do not re-number** (`ADR-0069` §4). Import `PROTOCOL_VERSION`
  from `./version` and use it in every fixture in the file:
  - the four tests whose `Welcome` must be *accepted* — at ~lines 86, 190, 204/207, 218 — take
    `PROTOCOL_VERSION`, interpolated into the JSON string where the fixture is a raw frame. The
    number is scenery there; each of those tests is about a device id, not about a version.
  - *"refuses to trust a welcome at another version"* at ~line 239 takes `PROTOCOL_VERSION + 1`,
    which is what `reconnecting.test.ts` and `HandshakeTest.kt` already do. Its absolute `3` is
    **wrong today**, not merely stale: this bump makes it name the current version while the test
    still claims to test an outdated one.

  Every assertion in the file keeps its meaning and none is weakened or deleted. After this the file
  is in no future bump's blast radius, which is the point.

## Out of scope

- **Every socket-level test of the behaviour wired here.** `TASK-021302`–`TASK-021306` prove it,
  starting the moment this merges. This ticket's own gates are the protocol suites, which cover both
  new types the day they exist (`ADR-0044` §10, last bullet).
- **The resume path's restatement of a standing offer** — `ADR-0044` §5, and `TASK-021307`.
  `replyToJoinRoom` is not touched here.
- **Any rendering.** No client file outside `web-client/src/protocol/` is opened, and nothing a
  player sees changes. That is `STORY-0309`.
- `Room`, `RoomRegistry`, `RematchResult`, `RematchRefusal`, `RoomTimeouts`, `SeatDelivery` and
  `poker-engine`: untouched (`ADR-0044` §9).
- Correcting `docs/protocol.md`'s artifact count. **Already done** by `ADR-0068` §6 and `ADR-0069`
  §3: the document states the probe rather than a number or a list, and this ticket only adds the
  rows and the version line that `ProtocolDocumentationTest` demands.
- **The five client fixtures that carry `protocolVersion: 2` and do not break.**
  `web-client/src/lobby/Lobby.test.tsx`, `src/store/duel-state.test.ts`,
  `src/store/duel-provider.test.tsx`, `src/store/duel-store.test.ts` and
  `src/protocol/frames.test.ts` never reach `connection.ts`'s version comparison and
  `Welcome.protocolVersion` is generated as `number`, so `tsc` does not see them either. After this
  bump they assert a version that no longer exists and **nothing fails**. That is known, filed, and
  deliberately not fixed here: no gate holds them, so their *why it cannot be fewer* cell would have
  to say something false (`ADR-0068` §4, `ADR-0069` §5). Do not open them.

## Tests

No new test class. Five existing suites become the gate, and each covers something specific:

`ProtocolDocumentationTest`

| Test | Proves |
| --- | --- |
| `everyClientMessageHasARowSayingClientToServer` | `OfferRematch` has its row |
| `everyServerMessageHasARowSayingServerToClient` | `RematchOffered` has its row |
| `theDocumentNamesNoMessageThatDoesNotExist` | neither row names a type that does not exist |
| `theDocumentListsEveryProtocolError` | `REMATCH_UNAVAILABLE` is documented |
| `theDocumentStatesTheCurrentProtocolVersion` | the document's version line is the new constant |

`ProtocolVersionLedgerTest`

| Test | Proves |
| --- | --- |
| `everyTableRowParses` | the appended row has the shape `ADR-0047` §1 requires |
| `versionsAscendByExactlyOneFromRowToRow` | the step is exactly one, and no number is repeated |
| `theLastRowsVersionEqualsProtocolVersion` | the constant and the claim agree |
| `theLastRowsFingerprintEqualsTheComputedFingerprint` | the number claims **this** wire shape |

`ProtocolPayloadTest`, `ProtocolDiscriminatorTest` — the descriptor walks. They pick both new types
up automatically: no card, no seed and no engine state reaches either frame, and both discriminators
are unique.

`ProtocolJsonTest` — the renamed version test, and the two that must not change.

## Acceptance criteria

- [ ] `ProtocolDocumentationTest` passes with all five of its tests, `ProtocolDocumentationTest.kt` unchanged
- [ ] `ProtocolVersionLedgerTest` passes with all four of its tests, `ProtocolVersionLedgerTest.kt` unchanged
- [ ] `ProtocolPayloadTest` and `ProtocolDiscriminatorTest` pass with both files unchanged
- [ ] `ProtocolJsonTest` passes, `theProtocolVersionIsTwo` no longer exists, and the file's other two
      tests keep every assertion they had
- [ ] `./gradlew :poker-server:check` exits 0 — which is `verifyProtocolTypes` and `verifyDuelScript`
      agreeing that both generated files were regenerated and committed
- [ ] `cd web-client && npm run check` exits 0
- [ ] `docs/protocol-versions.md` gained exactly one row, and no existing row changed
- [ ] `SocketDuel.kt` and `SocketSecrecyTest.kt` each gained exactly one line, in an existing
      ignore-group, and no assertion or behaviour in either file changed
- [ ] `connection.test.ts` contains **no** numeric protocol version at all: every fixture reads
      `PROTOCOL_VERSION`, and the *another version* case reads `PROTOCOL_VERSION + 1`. Its test
      count and its assertions are unchanged
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
