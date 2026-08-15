---
schema: 2
id: TASK-030716
title: The server proves a rejection leaves the client's Act identity valid
type: task
status: backlog
parent: STORY-0307
module: poker-server
estimate: S
tier: sonnet
review: standard
files_touched: 1
labels: [server, duel, test]
depends_on: [TASK-030715]
verify:
  - ./gradlew :poker-server:test --tests '*DuelActionTest.aRejectionMovesNoDecisionPoint'
  - ./gradlew :poker-server:test --tests '*DuelActionTest.aFrameTheEngineRejectedStillPassesTheGuard'
  - ./gradlew :poker-server:test --tests '*DuelActionTest.anActionRetriedAfterARejectionIsApplied'
  - ./gradlew :poker-server:test --tests '*DuelActionTest'
---

## Goal

The invariant the web client now leans on is tested on the side that owns it: after the engine
rejects an action, the `handNumber`/`actionSequence` pair the client still holds passes `guard`, and
a second `Act` bearing it reaches the engine and is applied.

## Why

[`ADR-0043`](../../docs/adr/ADR-0043-a-rejection-closes-no-decision-point.md) says so in *What it
costs*: **"The invariant is documented, not tested. Nothing in `poker-server` proves that an `Act`
identity survives a rejection … A future server change that made a rejection append an `ActionOn`,
or advance the hand number, would silently turn every retry into a dropped frame: a player clicking
into total silence. The test that closes it belongs to the server, not the client … and is worth one
ticket."**

Silently is the word that earns the ticket. `guard` answers a stale frame with `ActRefusal.STALE_FRAME`,
and `act` drops it with **no outbound frame at all**. There is no rejection to render, no failure to
show: the client's retry would vanish, and the only symptom would be a bar that does nothing.
`docs/protocol.md` already states the invariant in prose — this makes it fail a build.

`DuelActionTest` today proves a *replay* is dropped (`areplayedFrameChangesNothingAndSaysNothing`)
and that a rejection carries the engine's own reason (`anIllegalActionIsRejectedWithTheEnginesReason`).
Neither pins this: both stop at the frame the server sends back, and say nothing about what the
client may do next.

## Files

`files_touched` counts the create/modify rows only.

| File | Action |
| --- | --- |
| `poker-server/src/test/kotlin/duels/poker/server/duel/DuelActionTest.kt` | modify — three tests added; **no existing test changes** |
| `poker-server/src/main/kotlin/duels/poker/server/duel/DuelAction.kt` | read — the rejection branch returns the runner verbatim |
| `poker-server/src/main/kotlin/duels/poker/server/duel/ActRefusal.kt` | read — `guard`, and what makes a frame stale |
| `poker-server/src/main/kotlin/duels/poker/server/duel/DuelTurn.kt` | read — `decisionPointOf` |
| `docs/adr/ADR-0043-a-rejection-closes-no-decision-point.md` | read — the invariant, in the words it was decided in |

## Scope

- Three tests in the existing `DuelActionTest`, using the fixtures the class already sets up:
  `runner`, `seatToAct`, and `current` (a legal `Call` bearing hand 1 and the live decision point's
  sequence). The illegal frame is `current.copy(action = PlayerAction.Bet(seatToAct, 1))`, the same
  one `anIllegalActionIsRejectedWithTheEnginesReason` uses — a bet below the minimum, which the
  engine rejects and the guard does not.
- `guard` and `decisionPointOf` are in the same package as the test, so no new import is needed for
  either; `assertNull` and `assertTrue` are already imported.
- No production file changes. If one seems necessary, the invariant is already broken and that is a
  finding for the PR body, not a fix inside this ticket.

`depends_on: [TASK-030715]` is **sequencing, not a code dependency**: this ticket shares no file with
the four client tickets ahead of it and would pass on today's `develop`. It is last only so that
exactly one ticket is startable at a time, as the run is sequential.

## Out of scope

- The `guard` rejection path (`NOT_YOUR_TURN`). `anActionFromTheWrongSeatIsRejectedToThatSeatAlone`
  and `anActionForTheOpponentIsRejectedToTheSenderAlone` already assert `assertEquals(runner, step.runner)`
  there, so that branch's "nothing changed" is pinned; this ticket covers the engine's branch, which
  is the one no test reaches past.
- Making the server re-prompt after a rejection. `ADR-0043` decided against it; a test that expected
  a `YourTurn` here would contradict a merged ADR.
- Any protocol or client change. `PROTOCOL_VERSION` does not move and no frame shape changes.
- Rewording `docs/protocol.md`. It already carries the invariant, under *What a `Rejected` does not
  change*.

## Tests

`DuelActionTest`. Every test is a block body returning `Unit` — an expression-bodied test that
returns a value is silently never run, and this repo has been bitten by it.

| Test | Proves |
| --- | --- |
| `aRejectionMovesNoDecisionPoint` | after the illegal frame, all three of `state.handNumber`, `decisionPointOf(hand.log.events)!!.sequence` and `state.seatToAct` equal what they were before it — the three facts `docs/protocol.md` lists, each asserted separately so a failure names which one moved |
| `aFrameTheEngineRejectedStillPassesTheGuard` | `guard(step.runner.hand!!.state, step.runner.hand!!.log.events, seatToAct, current)` returns `null` on the runner the rejection produced. The client's held identity is still one the server would accept |
| `anActionRetriedAfterARejectionIsApplied` | `act` on the rejected step's runner with `current` puts `current.action` in `hand.log.actions` and sends a `Snapshot` to both seats — the retry is not merely permitted, it lands |

## Proof

| Command | Proves |
| --- | --- |
| the three method-filtered `--tests` runs | each new test exists **by that exact name** and passes; Gradle fails the task outright when a filter matches nothing, so a renamed test cannot pass silently |
| `./gradlew :poker-server:test --tests '*DuelActionTest'` | the eleven tests already in the class still pass beside them |

**Name the edit that makes each assertion red**, and quote both in the PR — on a scratch commit that
is reverted, since neither belongs in the diff:

1. In `DuelAction.act`, make the engine-rejection branch return
   `DuelStep(runner.copy(hand = runner.hand!!.copy(state = runner.hand!!.state.copy(handNumber = 99))), …)`
   → `aRejectionMovesNoDecisionPoint` and `aFrameTheEngineRejectedStillPassesTheGuard` both fail, and
   `anActionRetriedAfterARejectionIsApplied` fails with the retry silently dropped. Revert.
2. In `guard`, drop the `message.handNumber != state.handNumber` line → nothing here fails, which is
   the honest result: this ticket pins that the identity stays *valid*, not that staleness is
   detected. Say so, and revert.

## Acceptance criteria

- [ ] `aRejectionMovesNoDecisionPoint` passes, and asserts hand number, decision-point sequence and
      seat to act as three separate assertions
- [ ] `aFrameTheEngineRejectedStillPassesTheGuard` passes, and asserts `guard(...)` is `null`
- [ ] `anActionRetriedAfterARejectionIsApplied` passes, and asserts both that the action is in the
      hand log and that a `Snapshot` reached each seat
- [ ] No production file under `poker-server/src/main/` is changed
- [ ] No existing test in `DuelActionTest` is renamed, deleted or weakened
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
