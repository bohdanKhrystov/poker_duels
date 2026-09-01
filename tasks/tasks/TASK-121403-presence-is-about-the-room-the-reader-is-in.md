---
schema: 2
id: TASK-121403
title: Presence is about the room the reader is sitting in
type: task
status: blocked
parent: STORY-1214
module: poker-server
estimate: S
tier: sonnet
review: deep
files_touched: 2
labels: [bug, blocker, presence, blocked-on-dec]
depends_on: []
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.SeatDeliveryTest'
  - ./gradlew :poker-server:test --tests 'duels.poker.server.room.RoomPresenceTest'
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npm run --silent check
  - python3 .github/scripts/lint_tickets.py
---

## Blocked on `DEC-107`. Do not start it, and do not implement from the *Files* table below.

`DEC-107` is registered open for the **architect** and decides *where* a presence frame is scoped
to the room it is about — which is to say whether this ticket lands in `poker-server`, in
`web-client`, or in both behind a `PROTOCOL_VERSION` bump that would make it `atomic:`.

**The `## Files` table and the `verify:` block below are placeholders, not evidence.** They name
the seam the measurements point at so the ticket is not empty, and nothing more. They were **not**
produced by the `ADR-0069` probe, because the probe needs a change to stub and the change is what
`DEC-107` decides. When the answering ADR merges, the planner **re-cuts this ticket whole** — the
`TASK-121301` and `TASK-121302` precedent — measures the file set by stubbing and running
`.github/workflows/build.yml`'s pull-request gate set in full, and only then does it go `ready`.
A coder who implements the table as written is implementing a guess.

## Goal

A player reads presence about the room they are sitting in, and never about a room they have left.

## The defect

On 2026-09-01 a human played a real duel through two browsers and could not play it. Both tables
rendered correct, agreeing game state and **each seat was told the other one had vanished**. One
screen: *Your rival is away. The duel is paused.* with a countdown running to `0s` and sticking
there. The other: *Your rival did not come back. The duel continues, and the server acts for
them.* Neither had left. Reproduced four times on the live stack at `e1a37a80`.

**The frame arrives before the room exists.** Window A's wire, captured over CDP, epoch ms:

```
1788289017171 IN   {"type":"Welcome","playerId":"d1d1edee-…","protocolVersion":5}
1788289021602 IN   {"type":"OpponentPresence","presence":"AWAY","graceRemainingMillis":60000}
1788289021922 OUT  {"type":"CreateRoom"}
1788289021924 IN   {"type":"RoomJoined","code":"0QA3WCB6","seat":0}
```

A holds no seat anywhere at `021602`. The frame is about the room A **left**, produced when the
*other* player's socket closed there, and delivered to the connection A opened afterwards. A
second capture has the same frame landing 307 ms after `Welcome` on a connection that then sat in
the lobby for ninety seconds. The client stores it, and renders it in whatever room it enters next.

**Controlled negative.** With both profiles roomless — so a reload produces no presence frame at
all — a create-and-join was clean: **zero `OpponentPresence` frames on either wire, and neither
screen marked anything.** Remove the stale frame and the symptom goes, in both directions.

**Two facts that bound what a fix may claim.**

- **The server is not confused about the duel.** Under *The duel is paused.* the acting player's
  `Call 100` was **accepted**: stack 9,950 → 9,900, turn passed. The engine and the room were
  correct throughout. Nothing here is a rules or pot defect.
- **Nobody was folded.** In every reproduction the hand stayed at Hand 1, preflop.
  `RoomRegistry.expireGracePeriods()` folding an expired seat is real code, but a *present* player
  being folded was **not reproduced**. Do not write a test asserting it, and do not repair it here.

## Why this is a decision and not a patch

Read from source, and offered as a reading rather than as a cause:

- `poker-server/src/main/kotlin/duels/poker/server/SeatDelivery.kt` — `deliver` resolves
  `Addressed.seat` to a `PlayerId` through the room's seating, then takes
  `connections.writerFor(player)`: that player's **current** connection, whatever room it is in.
  Its KDoc says it *"reads `Addressed.seat` and nothing else"*, on purpose.
- `poker-server/src/main/kotlin/duels/poker/server/protocol/ServerMessage.kt` —
  `OpponentPresence(presence, graceRemainingMillis)` carries **no room and no seat**. `ADR-0028` §1
  chose that: *"a seat field would be a second thing to get wrong at the one place that already
  decides where a frame goes"*.
- `web-client/src/store/duel-state.ts` — the `OpponentPresence` case applies the frame
  unconditionally.

So there is **no point on the path at which a stale frame could be recognised**, and the four
places it could be given one differ in cost — one of them moves `PROTOCOL_VERSION`. Choosing is
`CLAUDE.md` rule 5's *never guess a decision*, which is why this ticket blocks rather than picks.

## Files

**Placeholder. See the block at the top of this ticket.**

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/SeatDelivery.kt` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/SeatDeliveryTest.kt` | modify |

Evidence to **read**, whatever the answer:
`poker-server/src/main/kotlin/duels/poker/server/room/RoomRegistry.kt` (`disconnect`, `resume`,
`expireGracePeriods`), `.../protocol/ServerMessage.kt` (`OpponentPresence`),
`web-client/src/store/duel-state.ts` (the reducer case) — five at most, and the re-cut will say
which five.

## Scope

Written as properties, because the mechanism is undecided:

- A presence frame a player receives is about the room that player currently holds a seat in.
- A player holding **no** seat — sitting in the lobby, or between rooms — is told nothing about
  anyone's presence, and carries nothing stale into the room they enter next.
- A genuine absence still produces the away marking, the countdown and the return notice: `CORE-18`
  and `CORE-19` describe wanted behaviour and must still pass by hand.

## Out of scope

- **Answering `DEC-107`.** It is the architect's.
- **The action bar's enabled state under a paused notice.** `ADR-0046` §6 declines it by
  name; it is `DEC-108`, the product owner's, and it is a separate change if it is one at all.
- **The grace window's length, `ADR-0023`'s absent-seat action, or what the server does for an
  absent seat.** `RoomTimeouts.DEFAULT_DISCONNECT_GRACE_MILLIS` stays `60_000`.
- **`ADR-0046`'s three sentences.** The words are correct; they were shown to the wrong player.
- **Folding a present player.** Unreproduced — see §The defect.
- **`docs/test-plan.md`.** `TASK-121401` owns the catalogue, and `CORE-21` is the case this repair
  is measured against by hand.

## Tests

To be named by the re-cut, because a test asserting where the frame is stopped **is** the decision.
What is certain: the repair lands a test that is **red before it and green after**, and the PR
body quotes both runs. What must not happen is a suite that passes either way — every unit here
already passes on the broken product, which is precisely how `TASK-120502` came to be dropped.

## Acceptance criteria

To be written by the re-cut. Two hold whatever the answer, and are recorded now so the re-cut
cannot quietly drop them:

- [ ] The reproduction in §The defect is run **before** and **after**, by hand, on the live stack,
      and both runs are pasted into the PR body as text. The before-run must show the away marking
      on a connected player; the after-run must show `CORE-21` passing — neither screen marks the
      other for 75 s.
- [ ] `CORE-18` and `CORE-19` still pass by hand: a real `close` still produces the away marking
      and a real return still clears it. A repair that silences presence altogether satisfies
      `CORE-21` and breaks the feature.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket: `verify` green,
review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into `develop`.
