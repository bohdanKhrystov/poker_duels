---
schema: 2
id: TASK-121404
title: A connection's room becomes a session type the directory can read
type: task
status: ready
parent: STORY-1214
module: poker-server
estimate: S
tier: haiku
review: standard
files_touched: 3
labels: [bug, presence, blocker]
depends_on: []
verify:
  - ./gradlew :poker-server:test --tests duels.poker.server.session.RoomMembershipTest && python3 -c "import xml.etree.ElementTree as ET; r = ET.parse('poker-server/build/test-results/test/TEST-duels.poker.server.session.RoomMembershipTest.xml').getroot(); assert r.get('tests') == '1' and r.get('failures') == '0'"
  - ./gradlew check -PrequireDocker=true
  - python3 .github/scripts/lint_tickets.py
---

## Goal

`RoomMembership` is a public type in `duels.poker.server.session`, and its `code` is `@Volatile`,
so `ConnectionDirectory` can be handed one in [`TASK-121403`](TASK-121403-presence-is-about-the-room-the-reader-is-in.md).

## Why this is its own ticket, and lands before the repair

[`ADR-0104`](../../docs/adr/ADR-0104-a-frame-reaches-the-connection-in-the-room-it-is-about.md) §2
moves this class and makes its field volatile as one clause of a larger change. It is split out
because the `ADR-0069` probe found this half **green on its own**: the move compiles, `./gradlew
check -PrequireDocker=true` exits 0 with it applied and nothing else, and `ADR-0068` is explicit
that a change with a green intermediate state is two tickets, not one declared `atomic:`.

It also gives the ADR's *"single most important word"* a diff of its own. §2 says of `@Volatile`:
*"getting it wrong fails invisibly and never on one thread"*, and Consequences says *"nothing in
the type system asks for it; only §2 and a reviewer do."* That is now false — this ticket adds the
gate that asks for it.

## Files

Measured by probing, not remembered (`ADR-0069`, `ADR-0070`). The move was stubbed in one tree and
`.github/workflows/build.yml`'s pull-request gate set was run in full: `./gradlew check
-PrequireDocker=true` with Docker up (Colima, Engine 29.5.2), **40 tasks, no suite skipped**,
`verifyProtocolTypes` and `verifyDuelScript` both executed, **exit 0**, 2 453 tests, 0 failures;
then `npm ci`, `npm run check` and `npm run build` in `web-client/`, all exit 0. The probe was
reverted and `git status` came back empty. No gate named a fourth path.

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/session/RoomMembership.kt` | create |
| `poker-server/src/main/kotlin/duels/poker/server/DuelSocket.kt` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/session/RoomMembershipTest.kt` | create |
| `docs/adr/ADR-0104-a-frame-reaches-the-connection-in-the-room-it-is-about.md` | read |

`DuelSocket.kt` is large; the only parts to open are the `private class RoomMembership` at line
~418 and the import block at the top.

## Scope

- `RoomMembership` moves out of `DuelSocket.kt` into a new file
  `duels/poker/server/session/RoomMembership.kt`, as a `public class` with a single
  `public var code: RoomCode?` initialised to `null`.
- That property carries `@Volatile`.
- `DuelSocket.kt` deletes its `private class RoomMembership`, imports the new one, and changes
  nothing else. Every existing `room.code` read and write stays exactly where it is.
- The moved class's KDoc replaces the reason the old one gave. The old KDoc says `code` is *"a
  plain `var`, not behind a lock, because every read and write of it happens inside the single
  coroutine [serveUntilEvictedOrClosed] runs in"*. That sentence stops being true the moment
  `TASK-121403` hands this object to `ConnectionDirectory`, which is read from the ticker's sweep
  and from the other seat's socket. The new KDoc says that instead, and cites `ADR-0104` §2.

## Out of scope

- **`ConnectionDirectory`.** It gains nothing here — no parameter, no lookup, no `RoomCode`. That
  is `TASK-121403`, and this ticket must not anticipate it.
- **`SeatDelivery.kt` and `deliver`.** Untouched. Delivery still crosses rooms after this merges,
  and that is expected: this ticket makes the repair possible, it is not the repair.
- **Making `RoomMembership` a `data class`, giving it an `equals`, or adding any member beyond
  `code`.** `ADR-0104` §2 needs one object shared by reference; value semantics would defeat it.
- **Any behaviour change at all.** The full suite must stay at its current count and stay green.

## Tests

`RoomMembershipTest` — a new file at
`poker-server/src/test/kotlin/duels/poker/server/session/RoomMembershipTest.kt`, holding exactly
one test.

| Test | Proves |
| --- | --- |
| `theroomAConnectionIsInIsAVolatileField` | `RoomMembership::class.java.getDeclaredField("code")` has the JVM `volatile` modifier — `java.lang.reflect.Modifier.isVolatile(field.modifiers)` is `true` |

This asserts an annotation on purpose. No single-threaded test can observe a missing `@Volatile`,
and `ADR-0104` names its omission as a failure that *"never [happens] on one thread"*; the
reflection check is the only instrument that fails an exit code. It was run both ways during the
probe: green with `@Volatile`, and `AssertionError at RoomMembershipTest.kt` with the annotation
deleted and nothing else changed.

## Acceptance criteria

- [ ] `RoomMembershipTest.theroomAConnectionIsInIsAVolatileField` passes, and that class reports
      exactly **1** test — the first `verify` command asserts both from the JUnit XML
- [ ] `DuelSocket.kt` declares no `RoomMembership` class; the type it uses is the imported one
- [ ] `./gradlew check -PrequireDocker=true` exits 0 with no suite skipped
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket: `verify` green,
review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into `develop`.
