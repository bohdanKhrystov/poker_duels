---
schema: 2
id: TASK-021412
title: Disconnection's KDoc says what outbound carries, not that it is empty
type: task
status: done
parent: STORY-0214
module: poker-server
estimate: XS
tier: haiku
review: light
labels: [server, rooms, docs]
files_touched: 1
depends_on: [TASK-021411]
verify:
  - ./gradlew :poker-server:compileKotlin
  - ./gradlew :poker-server:ktlintCheck
  - "! grep -q 'currently always empty' poker-server/src/main/kotlin/duels/poker/server/room/Disconnection.kt"
---

## Goal

`Disconnection.kt`'s KDoc still says `outbound` is *"currently always empty — a later ticket will
emit `OpponentPresence` here"*. `TASK-021404` was that later ticket. The sentence has been false
since it merged.

## Why it needs a ticket at all

Nothing catches it. No gate fails on stale prose, so `ADR-0070`'s probe cannot find it and
`ADR-0070` §4's exception does not cover it — a coder that edited the file on those grounds would be
widening its ticket, not completing a *Files* table. `TASK-021405`'s coder was asked to fix it in
passing and correctly refused, because `Disconnection.kt` is listed in that ticket as *read, not
edited*. The refusal was right and this ticket is the consequence.

A comment that describes behaviour the code no longer has is worse than no comment: it is read as
current, and the reader who believes it stops looking.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/room/Disconnection.kt` | modify |

## Scope

- Rewrite the `outbound` KDoc to say what it now carries: the frames the disconnect produced, which
  `TASK-021404` made real — at most one `Addressed(otherSeat, OpponentPresence(...))`, and none at
  all when no other seat is occupied.
- Comment **why**, not what, per `CLAUDE.md`. The fact worth recording is the one a reader cannot
  get from the type: that the list is empty when nobody else is seated, so an empty `outbound` is a
  normal outcome rather than a failure to build a frame.
- Do not restate `presenceOf`'s three states here; `Room.presenceOf` documents itself.

## Out of scope

- Any change to `Disconnection`'s shape, its fields, or their types.
- `RoomRegistry.disconnect`, its callers, and every test.
- The KDoc on any other type.

## Tests

None. This is a comment. The `verify` block's `grep` is the gate: the false sentence must be gone,
and the module must still compile and lint.

## Acceptance criteria

- [ ] `Disconnection.kt` no longer contains the string `currently always empty`
- [ ] The `outbound` KDoc describes the frames it carries and says why an empty list is normal
- [ ] No file other than `Disconnection.kt` changes
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
