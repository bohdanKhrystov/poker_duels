---
schema: 2
id: TASK-121405
title: The measured reproduction is a server test
type: task
status: ready
parent: STORY-1214
module: poker-server
estimate: XS
tier: sonnet
review: standard
files_touched: 1
labels: [bug, presence, regression-guard]
depends_on: [TASK-121403]
verify:
  - ./gradlew :poker-server:test --tests duels.poker.server.DuelSocketDisconnectTest && python3 -c "import xml.etree.ElementTree as ET; r = ET.parse('poker-server/build/test-results/test/TEST-duels.poker.server.DuelSocketDisconnectTest.xml').getroot(); assert r.get('tests') == '13' and r.get('failures') == '0'"
  - ./gradlew check -PrequireDocker=true
  - python3 .github/scripts/lint_tickets.py
---

## Goal

The wire trace `STORY-1214` measured by hand is a socket test: a player's own lobby connection is
told nothing about the room that player left.

## Why this is a ticket and not a line in `TASK-121403`

`ADR-0104` §7's first requirement is *"the measured reproduction, as a server test … This is the
one that would have caught it."* `TASK-121403` proves the rule at `deliver`, where the composition
that produced the defect lives, and its own `SeatDeliveryTest` cases are red before it and green
after. This one proves the whole path — `RoomRegistry.disconnect` builds the frame, `deliver`
routes it, a real socket does or does not receive it — and no merged gate drags
`DuelSocketDisconnectTest.kt` into `TASK-121403`'s blast radius, which under `ADR-0068` makes it a
separate ticket rather than a seventh row.

It is a **regression guard**, and the ticket says so rather than pretending otherwise: it is
written after the repair, so it is green when first run. What makes it more than decoration is the
mutation below, which the PR body must show.

## The blind spot it closes

`DuelSocketDisconnectTest` already contains `aThirdSocketInNoRoomIsToldNothing`, and **it passes on
the broken product**. Its third socket handshakes with `deviceId = "third"`, so it resolves to a
*different player*, and `deliver` never looks that player up. The measured defect needs the
roomless connection to belong to **the same player who holds the seat** — under `ADR-0018` a second
socket for one device adopts the seat, and the directory's entry for that player becomes the new,
roomless writer. That is the connection the trace saw an `OpponentPresence` land on 320 ms before
it sent `CreateRoom`.

A universal-sounding name over a case that cannot fail is exactly what `STORY-1214` found in
`CORE-18` and `CORE-06`. This is the same shape, in Kotlin.

## Files

Measured by probing (`ADR-0069`, `ADR-0070`): the test below was written against a tree carrying
`TASK-121403`'s change and `.github/workflows/build.yml`'s gate set was run — `./gradlew check
-PrequireDocker=true` with Docker up (Colima, Engine 29.5.2), exit 0, no suite skipped. No gate
named a second path; in particular the fixtures this test uses (`startDuel`, `completeHandshake`,
`drainServerMessages`, `awaitRoom`, `testRoomRegistry`) are all already private helpers in this
file, so nothing new is imported and no shared fixture file moves.

| File | Action |
| --- | --- |
| `poker-server/src/test/kotlin/duels/poker/server/DuelSocketDisconnectTest.kt` | modify |
| `docs/adr/ADR-0104-a-frame-reaches-the-connection-in-the-room-it-is-about.md` | read |

## Scope

- One new test in `DuelSocketDisconnectTest`, using the helpers already in that file. No new
  helper, no change to any existing test, no change to `testRoomRegistry`.
- The class goes from **12** tests to **13**; the count is gated.

## Out of scope

- **Any production change.** This ticket is a test. If it is red, the repair is wrong and the fix
  belongs in `TASK-121403`, not here.
- **Editing or renaming `aThirdSocketInNoRoomIsToldNothing`.** It is a weak case, not a wrong one —
  a stranger's socket must indeed be told nothing, and that assertion stays exactly as it is.
- **Asserting that a present player is folded.** Unreproduced; `ADR-0104` §9.
- **A `Snapshot`, `Events` or `DuelFinished` crossing at socket level.** `ADR-0104` §7's third
  bullet is honoured at the unit by `TASK-121403`, where `deliver` demonstrably does not read the
  message. Driving a mid-hand act through two sockets to say the same thing again is a bigger test
  than the guard is worth; not ticketed.

## Tests

`DuelSocketDisconnectTest`

| Test | Proves |
| --- | --- |
| `thehostsOwnLobbySocketIsToldNothingAboutTheRoomItLeft` | `startDuel()` seats `"host"` and `"guest"`; the **host's own device** then opens a second `/ws` connection, completes the handshake and enters no room; the guest closes; `awaitRoom(rooms, setup.code) { it.isPaused }` proves the disconnect ran and the frame was built (`ADR-0104` §3); and `drainServerMessages()` on the lobby socket comes back **empty** |

The `awaitRoom` wait is load-bearing and not a sleep: without it the assertion could pass because
nothing had happened yet. The room reaching `isPaused` is the proof that `RoomRegistry.disconnect`
produced an `OpponentPresence(AWAY, …)` addressed to seat 0 — so an empty drain means it was
dropped at delivery, not never made.

**The mutation that gives this test its value.** With the repair in place, change
`ConnectionDirectory.writerFor(player, room)` to ignore its `room` argument — `develop`'s
semantics behind the new signature — and this test fails while every other test in the class
passes. Measured 2026-09-01: `DuelSocketDisconnectTest > thehostsOwnLobbySocketIsToldNothingAboutTheRoomItLeft
FAILED`, one failure. Revert the mutation; it is an experiment, not a change.

## Acceptance criteria

- [ ] `DuelSocketDisconnectTest.thehostsOwnLobbySocketIsToldNothingAboutTheRoomItLeft` passes
- [ ] `DuelSocketDisconnectTest` reports exactly **13** tests and 0 failures — the first `verify`
      command asserts both from the JUnit XML, and exits 1 today at 12
- [ ] The PR body quotes the mutation run described in §Tests, showing that test failing and the
      other twelve passing
- [ ] No file outside the *Files* table is modified
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket: `verify` green,
review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into `develop`.
