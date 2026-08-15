---
schema: 2
id: TASK-030717
title: A frame from an earlier hand is dropped, though its sequence fits
type: task
status: done
parent: STORY-0307
module: poker-server
estimate: XS
tier: sonnet
review: standard
files_touched: 1
labels: [server, duel, test]
depends_on: [TASK-030809]
verify:
  - ./gradlew :poker-server:test --tests '*DuelActionTest.aFrameFromAnEarlierHandIsDroppedThoughItsSequenceFits'
  - ./gradlew :poker-server:test --tests '*DuelActionTest'
---

## Goal

`guard`'s first line stops being untested: a frame naming a **previous hand**, whose
`actionSequence` matches the live decision point exactly, is refused as stale and dropped in
silence.

## Why

`TASK-030716`'s second red edit reported it and a review confirmed it. Delete

```kotlin
        message.handNumber != state.handNumber -> ActRefusal.STALE_FRAME
```

from `ActRefusal.kt` and the whole of `DuelActionTest` stays green. The suite's only staleness
coverage — `areplayedFrameChangesNothingAndSaysNothing` — replays a frame from the **same** hand, so
the `actionSequence` line below catches it single-handed and the hand-number line is dead weight to
the test suite while carrying real duty in production.

The duty is silence. A stale frame is dropped with no outbound frame at all, so a hand-number check
that stopped working would let a frame from a finished hand reach the engine, and the seat that sent
it would learn nothing either way. That is a branch worth one test.

## Files

`files_touched` counts the create/modify rows only.

| File | Action |
| --- | --- |
| `poker-server/src/test/kotlin/duels/poker/server/duel/DuelActionTest.kt` | modify — one test added; **no existing test changes** |
| `poker-server/src/main/kotlin/duels/poker/server/duel/ActRefusal.kt` | read — `guard`, and the order of its branches |
| `poker-server/src/main/kotlin/duels/poker/server/duel/DuelTurn.kt` | read — `decisionPointOf` |

## Scope

- One test in the existing `DuelActionTest`, built from fixtures the class already has: `runner`,
  `seatToAct`, `current` and `seeds`. A fold ends hand 1 and opens hand 2 — the class already proves
  it does in `afoldEndsTheHandAndOpensTheNext` — and hand 2's live decision point supplies the
  sequence number that makes the stale frame otherwise perfect.
- The test carries its own **control**: the same frame with `handNumber = 2` is accepted. Without it
  the test would pass against a `guard` that refused everything, and the assertion would prove
  nothing about which branch fired.
- No import is added. `guard`, `decisionPointOf` and `ActRefusal` share the test's package, `Act` and
  `PlayerAction` are imported at the top of the file, and so are `assertEquals` and `assertTrue`.
- No production file changes. If one seems necessary, that is a finding for the PR body, not a fix
  inside this ticket.

`depends_on: [TASK-030809]` is **sequencing, not a code dependency**: this ticket shares no file with
`STORY-0308` and would pass on today's `develop`. It is last only so that exactly one ticket is
startable at a time, as the run is sequential.

## Out of scope

- The `actionSequence` branch. `areplayedFrameChangesNothingAndSaysNothing` covers it.
- A frame naming a hand that has not happened yet. The same line refuses it, and one test per
  direction would be one more than the branch has.
- Answering a stale frame. `ADR-0002` and `DuelAction.act` drop it deliberately, and a test that
  expected a `Rejected` would contradict both.
- Any change to `guard` itself. This ticket adds coverage to a branch that is already correct.

## Tests

`DuelActionTest`. A block body returning `Unit` — an expression-bodied test that returns a value is
silently never run, and this repo has been bitten by it.

| Test | Proves |
| --- | --- |
| `aFrameFromAnEarlierHandIsDroppedThoughItsSequenceFits` | after a fold opens hand 2, an `Act` naming `handNumber = 1` and hand 2's live `actionSequence` is answered `ActRefusal.STALE_FRAME` by `guard`, leaves the runner identical and sends nothing — while the same frame bearing `handNumber = 2` is applied |

```kotlin
@Test
fun aFrameFromAnEarlierHandIsDroppedThoughItsSequenceFits() {
    val fold = current.copy(action = PlayerAction.Fold(seatToAct))
    val afterFold = act(runner, seatToAct, fold, seeds).runner
    val live = afterFold.hand!!
    assertEquals(2, live.state.handNumber)

    val seat = live.state.seatToAct!!
    // Hand 1's number, hand 2's sequence: every check but the hand number's
    // would let this frame through.
    val stale = Act(
        handNumber = 1,
        actionSequence = decisionPointOf(live.log.events)!!.sequence,
        action = PlayerAction.Call(seat),
    )

    assertEquals(ActRefusal.STALE_FRAME, guard(live.state, live.log.events, seat, stale))

    val dropped = act(afterFold, seat, stale, seeds)
    assertEquals(afterFold, dropped.runner)
    assertTrue(dropped.outbound.isEmpty())

    // The control: one field different, and the server acts on it.
    val applied = act(afterFold, seat, stale.copy(handNumber = 2), seeds)
    assertTrue(applied.outbound.isNotEmpty())
}
```

## Proof

| Command | Proves |
| --- | --- |
| `--tests '*DuelActionTest.aFrameFromAnEarlierHandIsDroppedThoughItsSequenceFits'` | the test exists by that exact name and passes; Gradle fails the task outright when a filter matches nothing |
| `--tests '*DuelActionTest'` | the thirteen tests already in the class still pass beside it |

**Name the edit that makes it red**, and quote it in the PR — on a scratch commit that is reverted:

1. Delete `message.handNumber != state.handNumber -> ActRefusal.STALE_FRAME` from `guard` →
   `aFrameFromAnEarlierHandIsDroppedThoughItsSequenceFits` fails on the first assertion, `expected
   STALE_FRAME but was null`, and the two below it would have failed too. Revert. **This is the edit
   that ships green today**, which is the whole reason for the ticket.

## Acceptance criteria

- [ ] `aFrameFromAnEarlierHandIsDroppedThoughItsSequenceFits` passes
- [ ] It asserts all four: `guard` answers `STALE_FRAME`, the runner is unchanged, the outbound list
      is empty, and the same frame with the live hand number is acted on
- [ ] No production file under `poker-server/src/main/` is changed
- [ ] No existing test in `DuelActionTest` is renamed, deleted or weakened
- [ ] The PR body quotes the red edit above, run and reverted
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
