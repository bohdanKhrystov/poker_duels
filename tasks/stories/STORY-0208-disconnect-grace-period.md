---
id: STORY-0208
title: Disconnect, grace period and reconnect
type: story
status: done
parent: EPIC-02
module: poker-server
labels: [server, websocket, resilience]
depends_on: [STORY-0207]
---

## Goal

A dropped connection pauses the duel and holds the seat. Reconnecting inside the window resumes it
exactly where it was, with the returning player seeing only what they are entitled to see. The
window expiring folds their hand and the duel goes on.

## Why

[`ADR-0013`](../../docs/adr/ADR-0013-disconnect-grace-period.md) decided this, and it is the first
genuinely time-dependent behaviour in the whole system. Without it a subway tunnel costs a ranked
duel, and with the wrong version of it one player can freeze a duel forever.

## Design notes

- The window is a value in `ServerConfig` (`STORY-0201`). `ADR-0013` requires it be configuration,
  "not a literal scattered through the code", so it is read in one place and passed down.
- **Time is injected.** A clock/delay abstraction the tests replace with a virtual scheduler —
  `ADR-0013` makes this the owning story's job in as many words: tests inject time rather than
  sleep, or the suite becomes slow and flaky. No `Thread.sleep`, no wall-clock waits, anywhere in
  this story's tests.
- On disconnect the seat is held and the duel pauses: actions from the *opponent* are refused while
  paused, so the game cannot advance past an absent player.
- On expiry the server submits `PlayerAction.Fold(seat)` to the `DuelRunner`. It is an ordinary
  fold, indistinguishable from a played one — `ADR-0013`: the engine stays clock-free and no engine
  file changes in this story.
- After the fold the duel continues if the folded player still has chips, and is forfeited if they
  do not. `outcomeOf` already answers which, and this story asks it rather than deciding.
- **Reconnect resends state through the projection layer**, `PlayerView.of` plus the per-seat event
  filter — never a cached raw payload, never a replayed broadcast. `ADR-0013` names this: a
  reconnect can never reveal the opponent's hole cards.
- The timer is per seat and restarts on each disconnect. Both seats gone past the window ends the
  room, which is `STORY-0206`'s reaping path, not a second mechanism.
- A reconnect after the duel has already finished gets the finished state, not a resumed duel.
- Reconnection is identity, not luck: the returning socket proves who it is with the same device id
  from `STORY-0205`'s handshake (`ADR-0012`), and any other device is a new player who may not take
  the seat. This is the security-critical half of the story.

## Open decisions

**DEC-018 — does anyone see the pause?** *(the human's, not an architect's)* `ADR-0013` holds the
seat and folds it, and never says what the *present* player is told while that happens. This story
ships silence: an action during the pause is refused with `DUEL_PAUSED` and nothing else is sent,
and a timeout fold is indistinguishable on the wire from a chosen one. Anything richer is a new
`ServerMessage`. Registered in [`docs/adr/README.md`](../../docs/adr/README.md); nothing here is
blocked on it, and every ticket keeps the protocol hierarchies closed so the answer stays additive.

**DEC-019 — what drives the sweep?** `expireGracePeriods()` measures the window on `ServerClock`
and is called by its tests, exactly as `RoomRegistry.reap()` has been since `TASK-020612`. Nothing
schedules either in production, so today a window expires only when something asks. Whether that
becomes a ticker coroutine in `Application.module()`, a Ktor plugin or something else — and what
its period, scope and failure behaviour are — is registered in
[`docs/adr/README.md`](../../docs/adr/README.md) and due before `STORY-0212` wires `module()`.
Nothing in this story is blocked on it: every acceptance criterion below is reachable, and proved,
with the sweep driven explicitly.

## Tasks

| ID | Title | Status |
| --- | --- | --- |
| [TASK-020801](../tasks/TASK-020801-room-timeouts-carry-the-grace-window.md) | RoomTimeouts carries the disconnect grace window | ready |
| [TASK-020802](../tasks/TASK-020802-the-grace-window-is-configuration.md) | The grace window is configuration, read once in ServerConfig | backlog |
| [TASK-020803](../tasks/TASK-020803-a-paused-duel-has-its-own-protocol-error.md) | A paused duel has its own protocol error, and the document lists it | backlog |
| [TASK-020804](../tasks/TASK-020804-the-room-records-who-is-gone.md) | The room records which seats are inside a grace window and which are absent | backlog |
| [TASK-020805](../tasks/TASK-020805-disconnect-reconnect-and-expiry-on-the-room.md) | Disconnect starts the window, reconnect clears it, expiry makes the seat absent | backlog |
| [TASK-020806](../tasks/TASK-020806-an-absent-seat-folds-when-the-turn-reaches-it.md) | An absent seat folds, as an ordinary action, whenever the turn reaches it | backlog |
| [TASK-020807](../tasks/TASK-020807-a-paused-room-refuses-an-action.md) | A paused room refuses an action and moves nothing | backlog |
| [TASK-020808](../tasks/TASK-020808-the-room-folds-for-a-seat-nobody-is-in.md) | The room folds for a seat nobody is sitting in, so the duel never stalls | backlog |
| [TASK-020809](../tasks/TASK-020809-the-registry-starts-the-window.md) | The registry starts a seat's window on its own clock and configured limit | backlog |
| [TASK-020810](../tasks/TASK-020810-the-frames-a-returning-player-is-entitled-to.md) | The frames a returning player is entitled to, rebuilt through the projection layer | backlog |
| [TASK-020811](../tasks/TASK-020811-the-registry-resumes-a-returning-player.md) | The registry resumes a returning player, and nobody else | backlog |
| [TASK-020812](../tasks/TASK-020812-the-window-running-out-folds-the-hand.md) | The window running out folds the hand, and both seats gone ends the room | backlog |
| [TASK-020813](../tasks/TASK-020813-a-closing-socket-tells-the-room-its-seat-is-gone.md) | A closing socket tells the room its seat is gone, unless a newer socket took it | backlog |
| [TASK-020814](../tasks/TASK-020814-a-returning-socket-picks-up-where-it-left-off.md) | A returning socket picks up where it left off, and another device does not | backlog |
| [TASK-020815](../tasks/TASK-020815-the-configured-window-decides-the-instant.md) | The configured window decides the instant, on a clock that never sleeps | backlog |

The chain is linear. `TASK-020813` and `TASK-020814` also wait on `STORY-0207`'s `TASK-020734`,
which is the last ticket to rewrite `DuelSocket.kt` before this story touches it.

## Acceptance criteria

- [ ] A disconnect pauses the duel: an action from the connected opponent is refused while paused
      and changes nothing.
- [ ] Reconnecting inside the window resumes the same `GameState`; the returning player receives
      its own hole cards and not the opponent's, asserted against the frames actually sent.
- [ ] The window expiring folds the disconnected seat's hand, the fold reaches the engine as an
      ordinary `PlayerAction.Fold`, and the duel continues.
- [ ] The expiry test runs on virtual time and the story's whole test suite contains no
      `Thread.sleep` and no real-time wait, asserted by a grep-level check in review and by suite
      runtime.
- [ ] Changing the configured window changes the behaviour with no code change, asserted by a test
      that supplies a different `ServerConfig`.
- [ ] A different device id may not reconnect into a held seat.
- [ ] Both seats disconnected past the window ends the room and leaves nothing running.
- [ ] Reconnecting after the duel finished delivers the finished state, not a resumed duel.

## Out of scope

- A per-action turn clock. `ADR-0013` says it fits alongside this rather than replacing it, and
  nobody has asked for it yet.
- Surviving a server restart mid-duel — explicitly not required by `ADR-0011`.
- Any engine change. A timeout is a fold; if that ever stops being true, it is an ADR.
