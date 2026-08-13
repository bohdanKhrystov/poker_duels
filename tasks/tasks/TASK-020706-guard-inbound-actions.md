---
schema: 2
id: TASK-020706
title: A replayed frame is dropped and a frame acting for the opponent is refused, before the engine sees either
type: task
status: backlog
parent: STORY-0207
module: poker-server
estimate: S
tier: sonnet
review: deep
files_touched: 2
labels: [server, duel, authority, security]
depends_on: [TASK-020705]
verify:
  - ./gradlew :poker-server:test --tests '*DuelGuardTest'
  - ./gradlew :poker-server:check
---

## Goal

`guard(state, handEvents, seat, message)` decides, before anything reaches
`DefaultPokerEngine.handle`, whether an inbound `Act` is a replay to drop, an attempt to act out of
turn or for the opponent, or a frame the engine may see.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/duel/ActRefusal.kt` | create |
| `poker-server/src/test/kotlin/duels/poker/server/duel/DuelGuardTest.kt` | create |

Read, do not modify:
`poker-server/src/main/kotlin/duels/poker/server/protocol/ClientMessage.kt` (`Act.handNumber` and
`Act.actionSequence` are *questions*, never facts the server adopts),
`poker-server/src/main/kotlin/duels/poker/server/duel/DuelTurn.kt` (`decisionPointOf`),
`poker-engine/src/main/kotlin/duels/poker/engine/game/GameState.kt`.

## Scope

- Package `duels.poker.server.duel`. Two declarations, KDoc included:

  ```kotlin
  public enum class ActRefusal { NOT_YOUR_TURN, STALE_FRAME }

  public fun guard(
      state: GameState,
      handEvents: List<GameEvent>,
      seat: Int,
      message: Act,
  ): ActRefusal?
  ```

- `seat` is the seat of the **connection the frame arrived on**, established by the server; nothing
  in `message` may be trusted to say who is speaking.
- Checks in exactly this order, first match wins:
  1. `message.handNumber != state.handNumber` → `STALE_FRAME`
  2. `message.actionSequence != decisionPointOf(handEvents)?.sequence` → `STALE_FRAME`
  3. `state.seatToAct != seat` → `NOT_YOUR_TURN`
  4. `message.action.seat != seat` → `NOT_YOUR_TURN`
  Otherwise `null`: the engine may see it.
- **Staleness is checked before whose turn it is, deliberately** — and this is the one place the
  order differs from the story's prose. A replayed frame usually arrives when the *other* seat is on
  turn, so a turn-first order would answer a replay with a `Rejected` instead of silence, and a
  duplicate frame would then be visible to the player who sent it. `ADR-0002` wants a replayed frame
  to be inert; `STALE_FRAME` is dropped by `TASK-020708`, `NOT_YOUR_TURN` is answered.
- Check 4 is the one that matters most: without it a client could send `Fold(seat = opponent)` while
  its opponent is on turn, and the engine — which cannot know who sent the frame — would apply it.
- `require(seat in 0..1)`. `guard` is pure: no clock, no randomness, no logging, no state.

## Out of scope

- What is *done* with a refusal: dropping, replying `Rejected`, and calling the engine are all
  `TASK-020708`.
- Rejections the engine itself owns — an illegal amount, a check facing a bet. `Rejection` already
  names those and `DefaultPokerEngine` produces them; this guard never duplicates a rule of poker.

## Tests

`DuelGuardTest`, JUnit 5, package `duels.poker.server.duel`. Fixture: `val opening = startHand(1, 0,
listOf(1_000, 1_000), 50, 100, SplitMix64Rng(7))`, which leaves seat 0 on turn; `val current =
Act(handNumber = 1, actionSequence = decisionPointOf(opening.events)!!.sequence,
action = PlayerAction.Call(0))`. For the replay test, apply `current.action` with
`DefaultPokerEngine.handle` and guard the *same* `current` against the resulting state and the
concatenated events.

| Test | Proves |
| --- | --- |
| `theSeatOnTurnAnsweringTheCurrentDecisionPointPasses` | `guard(opening.newState, opening.events, 0, current)` is `null` |
| `aFrameFromTheSeatNotOnTurnIsNotYourTurn` | the same frame guarded with `seat = 1` is `NOT_YOUR_TURN` |
| `aFrameActingForTheOpponentIsNotYourTurn` | `seat = 0` sending `action = PlayerAction.Fold(1)` at the current decision point is `NOT_YOUR_TURN` |
| `anotherHandsNumberIsAStaleFrame` | `current.copy(handNumber = 2)` is `STALE_FRAME` |
| `anEarlierActionSequenceIsAStaleFrame` | `current.copy(actionSequence = current.actionSequence - 1)` is `STALE_FRAME` |
| `alaterActionSequenceIsAStaleFrame` | `current.copy(actionSequence = current.actionSequence + 1)` is `STALE_FRAME` |
| `areplayedFrameIsStaleRatherThanOutOfTurn` | after `current` has been applied, guarding `current` again returns `STALE_FRAME`, not `NOT_YOUR_TURN` |
| `aseatOutsideTheTableIsRejected` | `guard(..., seat = 2, current)` throws `IllegalArgumentException` |

## Acceptance criteria

- [ ] `DuelGuardTest.theSeatOnTurnAnsweringTheCurrentDecisionPointPasses` passes
- [ ] `DuelGuardTest.aFrameFromTheSeatNotOnTurnIsNotYourTurn` passes
- [ ] `DuelGuardTest.aFrameActingForTheOpponentIsNotYourTurn` passes
- [ ] `DuelGuardTest.anotherHandsNumberIsAStaleFrame` passes
- [ ] `DuelGuardTest.anEarlierActionSequenceIsAStaleFrame` passes
- [ ] `DuelGuardTest.alaterActionSequenceIsAStaleFrame` passes
- [ ] `DuelGuardTest.areplayedFrameIsStaleRatherThanOutOfTurn` passes
- [ ] `DuelGuardTest.aseatOutsideTheTableIsRejected` passes
- [ ] `ActRefusal.kt` names no `Rejection` subtype and no chip amount — it answers only *which* of
      the two refusals applies
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
