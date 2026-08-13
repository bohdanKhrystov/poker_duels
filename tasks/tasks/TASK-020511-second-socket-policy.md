---
schema: 2
id: TASK-020511
title: Decide and enforce what a second socket for one device id does
type: task
status: done
parent: STORY-0205
module: poker-server
estimate: S
tier: sonnet
review: deep
files_touched: 3
labels: [server, websocket, session, blocked]
depends_on: [TASK-020508]
verify:
  - ./gradlew :poker-server:test --tests '*DuelSocketSecondSocketTest'
  - ./gradlew :poker-server:test --tests '*SessionRegistryTest'
  - ./gradlew :poker-server:check
---

## Goal

When a device id that already holds a live session opens a second socket, the server does one
defined thing, and a test says which.

## Blocked on DEC-011

**Do not start this ticket.** No ADR answers the question, and guessing it here would decide it
by accident in the least visible place. It is registered as `DEC-011` in
[`docs/adr/README.md`](../../docs/adr/README.md) and noted in
[`STORY-0205`](../stories/STORY-0205-sessions-and-socket-lifecycle.md).

> **DEC-011** — a device id already holds a live session and the same device opens a second
> socket. Does the server refuse the new socket, close the old one and adopt the new, or let both
> live?

Each answer is defensible and they are not equivalent:

- **Refuse the new socket** (`Failure` + close). Simplest, and the old session keeps the duel. But
  a browser tab that crashed and reopened is locked out until the old socket is noticed as dead,
  which is precisely the case `ADR-0013`'s grace period exists to handle.
- **Close the old, adopt the new.** A reconnecting player always gets in, which is what
  `STORY-0208`'s resync wants. But it hands anyone holding a device id the power to evict the
  player using it — and `ADR-0012` says device ids are trivially minted and never authenticated.
- **Allow both.** No eviction and no lockout, but "which connection gets the `YourTurn`?" becomes
  a question every later story has to answer, and two sockets seeing one seat's hole cards widens
  what `ADR-0002` calls the secrecy boundary.

The answer constrains `STORY-0208` (reconnect and resync) and `STORY-0207` (which connection a
duel message goes to), which is why it is due before `STORY-0208` rather than before v0.2.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/DuelSocket.kt` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/DuelSocketSecondSocketTest.kt` | create |
| `poker-server/src/test/kotlin/duels/poker/server/session/SessionRegistryTest.kt` | modify |

`SessionRegistryTest` is in the budget because it currently pins
`sessionsOfListsEverySessionForThatPlayer` — two live sessions for one player. If `DEC-011` says
the registry itself enforces one session per player, that assertion moves and this ticket owns the
move. If the policy lives in the route instead, that file is left alone and `files_touched` drops
to 2.

## Scope

- Written when `DEC-011` is answered. `/plan-story STORY-0205` fills in the `Scope`, `Tests` and
  `Acceptance criteria` sections for the chosen option and flips this ticket to `ready`.
- Whatever the answer, it is enforced in **one place**, it is exercised by
  `DuelSocketSecondSocketTest`, and the losing socket is closed with a named close reason in the
  same style as `HANDSHAKE_REQUIRED`.

## Out of scope

- The disconnect grace period — `ADR-0013`, `STORY-0208`. This ticket decides what happens while
  the first socket is *live*, not what happens after it drops.
- Authenticating the device id so that eviction cannot be abused — `ADR-0012` gives v0.1 no auth
  and `EPIC-04` owns the claim flow.

## Tests

To be named once `DEC-011` is answered. `DuelSocketSecondSocketTest` will assert, at minimum: what
the second socket receives, what the first socket receives, and what `SessionRegistry.size` and
`sessionsOf(playerId)` hold once both attempts have settled.

## Acceptance criteria

- [ ] `DEC-011` is answered and recorded before any code is written
- [ ] The tests named in this ticket after that answer all pass
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
