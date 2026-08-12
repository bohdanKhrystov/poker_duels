---
schema: 2
id: TASK-010811
title: The log of a whole duel, and replaying it
type: task
status: blocked
parent: STORY-0108
module: poker-engine
estimate: S
tier: sonnet
review: standard
files_touched: 3
labels: [engine, replay, log, blocked]
depends_on: [TASK-010807]
verify:
  - ./gradlew check
---

## Goal

A duel — many hands, a rising blind schedule, an alternating button — is one log, and replaying
it reproduces every hand in it exactly.

## Blocked on STORY-0107

**Do not implement this ticket.** It needs match-level types that do not exist yet.
[`STORY-0107`](../stories/STORY-0107-duel-format-and-match.md) introduces them — a duel format, a
blind schedule, the state that survives between two hands, and the value a finished duel produces
— and this ticket cannot name its own fields until they have landed. No `depends_on` entry points
at a `STORY-0107` task here on purpose: that story is being split separately and its task ids are
not this planner's to invent.

It is also downstream of `DEC-005`, registered in [`docs/adr/README.md`](../../docs/adr/README.md):
whether the end of a match is a `GameEvent`, a `MatchEvent` of its own, or no event at all decides
whether a match log is a list of hand logs or a single event stream.

Once `STORY-0107` has merged its match types and `DEC-005` is answered, this ticket is
**re-split** by the splitter. The sketch below is a shape, not a sized unit.

## Files (provisional)

| File | Action |
| --- | --- |
| `poker-engine/src/main/kotlin/duels/poker/engine/log/MatchLog.kt` | create |
| `poker-engine/src/main/kotlin/duels/poker/engine/log/MatchReplay.kt` | create |
| `poker-engine/src/test/kotlin/duels/poker/engine/log/MatchReplayTest.kt` | create |

## Scope (provisional)

- `MatchLog`: the match seed, the duel format, and the ordered hand logs — `HandLog` already
  carries each hand's own seed, button, blinds and stacks (`TASK-010804`), so the match layer
  adds progression, not repetition.
- `replayMatch(log)` re-runs every hand through the engine and the match layer, reusing
  `replayHand` per hand rather than reimplementing it, and reproduces the final stacks, the hand
  count and the duel's result.
- Divergence detection is inherited from `TASK-010806`: the first hand whose events do not match
  fails, naming the hand number and the event index.
- Stepping to hand `n` and action `m` gives the state replaying to that point gives — the shape a
  replay viewer needs in EPIC-03.

## Out of scope

- A replay viewer or any UI — EPIC-03.
- Storing a match — EPIC-02.
- Serialising a match log — `TASK-010810` and `DEC-006` decide the format first.

## Acceptance criteria

- [ ] Not startable. `STORY-0107`'s match types have merged and `DEC-005` is answered before this
      ticket is re-split and any code is written.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
