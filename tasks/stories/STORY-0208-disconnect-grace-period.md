---
id: STORY-0208
title: Disconnect, grace period and reconnect
type: story
status: backlog
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

## Tasks

| ID | Title | Status |
| --- | --- | --- |
| — | *Tickets are produced by `/plan-story STORY-0208`.* | — |

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
