---
schema: 2
id: TASK-031201
title: A played duel records the Act it sent, and the seat it sent it from
type: task
status: done
parent: STORY-0312
module: poker-server
estimate: XS
tier: haiku
review: light
files_touched: 2
labels: [server, testing, fixture]
depends_on: [TASK-031112]
verify:
  - ./gradlew :poker-server:test --tests '*RunnerDuelTest'
  - ./gradlew :poker-server:check
---

## Goal

`playDuel` hands back the `Act` frames it sent as well as the frames it received, so a later ticket
can write down *what a client did* as well as *what the server said*.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/test/kotlin/duels/poker/server/duel/PlayedDuel.kt` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/duel/RunnerDuelTest.kt` | modify |

Read, do not modify: `poker-server/src/main/kotlin/duels/poker/server/protocol/ClientMessage.kt`
(`Act`), `poker-server/src/main/kotlin/duels/poker/server/duel/Addressed.kt`.

## Scope

- Added to `PlayedDuel.kt`:

  ```kotlin
  internal data class SentAct(val seat: Int, val act: Act)
  ```

  and one property on `PlayedDuel`: `val acts: List<SentAct>`, in the order they were sent.
- `playDuel`'s loop appends `SentAct(turnFrame.seat, message)` each time round, immediately before
  the `act(...)` call it feeds. Nothing else about the loop changes — not the policy, not the draw
  order, not the `maxActions` guard, not the message it builds.
- `acts` is the **fourth** property, added after `outbound` and before `actions`, and `actions`
  keeps its meaning and its value. `acts.size == actions` is then true by construction, which is
  what the third test below pins.
- The KDoc gains one line for the new property. Nothing else in the file's prose changes.

## Out of scope

- Serialising anything. This ticket produces a Kotlin value; `TASK-031202` turns it into frames.
- Recording the frames the *opponent* saw in a separate structure — `outbound` already carries every
  `Addressed` frame for both seats, which is all `TASK-031202` needs.
- `SocketDuel.kt` and the `e2e` package. That harness has its own loop and is untouched here.

## Tests

`RunnerDuelTest`, three tests added. The four already there keep their bodies; only
`theSameSeedPlaysTheSameDuel` gains one line, and it gains an assertion rather than losing one.

| Test | Proves |
| --- | --- |
| `everyActionSentIsRecorded` | for every seed, `played.acts.size == played.actions`, and `acts` is non-empty |
| `eachRecordedActAnswersItsOwnTurn` | walking `outbound` and `acts` together for one seed, the *n*th `ServerMessage.YourTurn` in `outbound` and the *n*th `SentAct` agree on `seat`, `handNumber` and `actionSequence` |
| `bothSeatsAreRepresentedInTheActions` | for one seed, `acts.map { it.seat }.toSet()` is `setOf(0, 1)` — a recording that always named one seat would satisfy the two tests above |

`theSameSeedPlaysTheSameDuel` gains `assertEquals(first.acts, second.acts)` beside the three
assertions it already makes; none of those three is removed or weakened.

## Acceptance criteria

- [ ] `RunnerDuelTest.everyActionSentIsRecorded` passes
- [ ] `RunnerDuelTest.eachRecordedActAnswersItsOwnTurn` passes
- [ ] `RunnerDuelTest.bothSeatsAreRepresentedInTheActions` passes
- [ ] `RunnerDuelTest.theSameSeedPlaysTheSameDuel` passes and asserts `acts` equality as well as the
      three equalities it already asserted
- [ ] `RunnerDuelTest.everyDuelReachesAnOutcome`, `theOutcomeIsTheEnginesOwn` and
      `everyHandIsRecordedInOrder` pass with their bodies unchanged
- [ ] `PlayedDuel.kt` names no `kotlin.random.Random`, no clock and no I/O
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
