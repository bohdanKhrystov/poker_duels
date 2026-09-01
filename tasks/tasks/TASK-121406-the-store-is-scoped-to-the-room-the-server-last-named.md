---
schema: 2
id: TASK-121406
title: The store is scoped to the room the server last named
type: task
status: done
parent: STORY-1214
module: web-client
estimate: XS
tier: sonnet
review: standard
files_touched: 2
labels: [bug, presence]
depends_on: [TASK-121403]
verify:
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npm run --silent test -- --reporter=verbose src/store/duel-state.test.ts 2>&1 | grep -qF "a RoomJoined naming a different room clears what the old room left behind"
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npm run --silent test -- --reporter=verbose src/store/duel-state.test.ts 2>&1 | grep -qF "a RoomJoined naming the room the store already holds clears nothing"
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npm run --silent test -- --reporter=verbose src/store/duel-state.test.ts 2>&1 | grep -qF "the monotone counters carry across a room change"
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npm run --silent test -- src/store/duel-state.test.ts 2>&1 | grep -qE "Tests +67 passed \(67\)"
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npm run --silent check
  - cd web-client && FORCE_COLOR=0 npm run --silent build
  - python3 .github/scripts/lint_tickets.py
---

## Goal

A `RoomJoined` naming a room the store does not already hold re-initialises the store, so nothing
a previous room left behind is rendered in the next one.

## What this is, and what it is not

[`ADR-0104`](../../docs/adr/ADR-0104-a-frame-reaches-the-connection-in-the-room-it-is-about.md) §4,
in its own words: *"This is the client scoping **its own derived state** to the room the server
last named. It is not the client judging a server statement, and it is **not** what makes the
system correct: §1 is. It bounds the one window §1 cannot close."*

That window is §5's third: a frame written while the connection was in the room and read after it
moved to another room **on the same socket**. It is correct at the write and stale at the read, and
no server-side check removes it. `TASK-121403` closes everything else.

**This is why the ticket depends on `TASK-121403` even though it shares no file with it.** Landing
the client reset first would ship, alone, the option `ADR-0104`'s *Alternatives* calls *"the trap in
the option set"* — one line that turns the recorded trace green while the defect stands, because the
stale frame arrives 320 ms *before* `RoomJoined`. Merged in that order the trail would read as if a
reducer had fixed a delivery defect.

## The thing that will rot if this ticket does not carry its own test

`ADR-0104`'s *Consequences* names it: *"The client half is unreachable from the shipped UI … it can
rot with every test green unless a test drives the reducer directly … that is exactly the kind of
code that gets deleted by someone tidying up."*

This was measured, not assumed. The change below was applied to `duel-state.ts` on its own and the
whole client gate set stayed green — `npm run check`, **117 files, 985 tests, 0 failures**. **No
existing test anywhere in `web-client` observes the current unconditional `RoomJoined` case.** The
three tests in §Tests are therefore the only thing that will ever hold this branch up.

Every way out of a room in the shipped client is a real navigation (`ADR-0072` §5 — an
`<a href="/">`, deliberately not a `window.location` call), so the socket dies and the next one
registers with no room. The protocol permits the move — `CreateRoom` is accepted on a connection
already seated — and this is the insurance against a client that one day takes it.

## Files

Measured by probing (`ADR-0069`, `ADR-0070`). The reducer change and the three tests were staged and
`.github/workflows/build.yml`'s client job was run verbatim: `npm ci`, `npm run check` and
`npm run build` in `web-client/`, all exit 0, **117 files / 988 tests** with the tests present.
`./gradlew check -PrequireDocker=true` was run on the same tree and could not be reddened by it —
the only `web-client` paths any Gradle task reads are `src/protocol/protocol.gen.ts` and
`src/e2e/scripted-duel.gen.json`, and neither is touched. The probe was reverted and `git status`
came back empty. No gate named a third path.

| File | Action |
| --- | --- |
| `web-client/src/store/duel-state.ts` | modify |
| `web-client/src/store/duel-state.test.ts` | modify |
| `docs/adr/ADR-0104-a-frame-reaches-the-connection-in-the-room-it-is-about.md` | read |

## Scope

- `applyServerMessage`'s `RoomJoined` case: when `state.roomCode` is non-null **and differs from**
  `message.code`, the state is re-initialised to `initialState()` before this room's `mySeat`,
  `roomCode` and `refusal: null` are applied. Otherwise it behaves exactly as it does today.
- **`rejectionCount` and `presenceCount` carry over rather than reset.** They exist to be
  strictly-increasing change tokens, and resetting them can make two different states compare
  equal.
- A `RoomJoined` naming the room the store already holds clears nothing beyond the `refusal` it
  clears today. That keeps a resume untouched: `ADR-0028` §5 sends the returning seat its
  opponent's current presence immediately after `RoomJoined`, and the `ALREADY_SEATED` branch —
  which sends no presence — must keep whatever the store had.
- The reducer stays pure and total, and the `reveal`-queue guard above the `switch` is not moved or
  altered.

## Out of scope

- **Any other reducer case.** `Snapshot`, `Events`, `OpponentPresence`, `DuelFinished`,
  `RematchOffered`, `Rejected`, `Failure`, `Welcome`, `YourTurn`, `ActedForAbsent` — untouched.
- **`DuelState`'s shape.** No field is added, removed or renamed, so the *starts with nothing the
  server has not sent* deep-equality test and the module's export-list test must both stay green
  unedited.
- **Any server change**, and any claim that this fixes the measured defect. `TASK-121403` does;
  this bounds one window it cannot reach.
- **A `LeaveRoom` message, a room field on the wire, or anything that moves `PROTOCOL_VERSION`.**
  `ADR-0104` §6.
- **`Lobby.tsx`, `DuelTable.tsx` and every other component.** The change is in the reducer and its
  test, and the probe confirmed no component test observes it.

## Tests

`duel-state.test.ts` — three new `it` blocks in the existing `describe("the duel state")`, taking
the file from **64** to **67**. Each is killed by a different wrong implementation, which is how
each earns its place; all three mutations were run on 2026-09-01 and each produced exactly one
failure.

| Test | Proves | Dies under |
| --- | --- | --- |
| `a RoomJoined naming a different room clears what the old room left behind` | after `RoomJoined("ABCD")` and an `OpponentPresence(AWAY, 60000)`, a `RoomJoined("EFGH")` leaves `rivalPresence` and `graceRemainingMillis` null and `roomCode`/`mySeat` set from the new frame | no reset at all — today's behaviour |
| `a RoomJoined naming the room the store already holds clears nothing` | the same setup, then `RoomJoined("ABCD")` again: `rivalPresence` is still `AWAY` and `graceRemainingMillis` still `60000` | an unconditional reset |
| `the monotone counters carry across a room change` | `presenceCount` is `1` before the room change and still `1` after it | a reset that returns bare `initialState()` |

The first two are the two inputs of one assertion: with only the first, nothing could tell a
room-scoped reset from an unconditional one.

## Acceptance criteria

- [ ] All three tests above exist by those exact names and pass — the first three `verify` commands
- [ ] `src/store/duel-state.test.ts` reports `Tests 67 passed (67)` — the fourth `verify` command,
      which exits 1 today at 64
- [ ] `npm run check` and `npm run build` both exit 0
- [ ] The PR body quotes the three mutation runs from §Tests, each showing its one failure
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket: `verify` green,
review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into `develop`.
