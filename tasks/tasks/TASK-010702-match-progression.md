---
id: TASK-010702
title: Match progression, button alternation and blind levels
type: task
status: backlog
parent: STORY-0107
module: poker-engine
estimate: M
labels: [engine, duel]
depends_on: [TASK-010701]
---

## Goal

Hands become a sequence: stacks carry over, the button alternates, blinds rise on schedule, and
the next hand starts correctly.

## Context

- [`tasks/stories/STORY-0107-duel-format-and-match.md`](../stories/STORY-0107-duel-format-and-match.md).

## Scope

- `MatchState`: the format, hand number, current blind level, both stacks, the button seat, and
  the current `GameState` if a hand is live.
- `startNextHand(match)` — carry stacks forward, swap the button, apply the blind level for the
  new hand number, deal.
- Blind levels change **only** at a hand boundary, never mid-hand.
- Emit `HandStarted` with the blind level, and a `BlindLevelChanged` event when it rises.
- A seat with fewer chips than the big blind still plays, all-in for what it has.

## Out of scope

- Ending the match — `TASK-010703`.
- Rematch, which is a server concern.
- Persisting match state — EPIC-02.

## Files

| File | Action |
| --- | --- |
| `poker-engine/src/main/kotlin/.../duel/MatchState.kt` | create |
| `poker-engine/src/main/kotlin/.../duel/MatchProgression.kt` | create |
| `poker-engine/src/test/kotlin/.../duel/MatchProgressionTest.kt` | create |

## Acceptance criteria

- [ ] Stacks at the start of hand `n+1` equal the stacks at the end of hand `n`.
- [ ] The button alternates every hand.
- [ ] The blind level for hand `n` matches the schedule.
- [ ] A blind level never changes during a hand.
- [ ] `BlindLevelChanged` is emitted exactly once per level increase.
- [ ] A seat shorter than the big blind is dealt in and posts all-in.
- [ ] Total chips across both stacks are constant across the whole match.

## Tests

- `MatchProgressionTest` — stack carry-over, button alternation, level boundaries, short stack.
- Property: chips conserved across a generated multi-hand match.

## Definition of done

Standard, per [`tasks/README.md`](../README.md): build green, tests green, `/code-review` run
with findings fixed or answered, CI green, status `done`, `BOARD.md` updated, and
**squash-merged into `develop`** by a PR linking this ticket. Not done until the PR is merged.
