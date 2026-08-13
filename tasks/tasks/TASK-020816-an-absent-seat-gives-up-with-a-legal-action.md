---
schema: 2
id: TASK-020816
title: An absent seat gives up with an action the engine will accept
type: task
status: done
parent: STORY-0208
module: poker-server
estimate: S
tier: sonnet
review: deep
files_touched: 2
labels: [server, duel, bug]
depends_on: [TASK-020806]
verify:
  - ./gradlew :poker-server:test --tests '*AbsentSeatsTest'
  - ./gradlew :poker-server:test --tests '*RoomPausedTest'
  - ./gradlew :poker-server:check
---

## Goal

`foldAbsent` gives up an absent seat's turn with an action the engine will **accept**, at every
decision point — including the ones where `Fold` is not legal. A duel with an absent player always
progresses.

## The bug, and why it is not a test artifact

`TASK-020806` shipped `foldAbsent`, which always builds `PlayerAction.Fold(seat)`. The engine does
not always allow that. From `poker-engine/.../game/BettingRules.kt`:

```kotlin
if (callTo == committed) {
    add(ActionType.CHECK)
} else {
    add(ActionType.FOLD)
    add(ActionType.CALL)
}
```

**When nothing is owed, `FOLD` is not in the legal set** — only `CHECK` is. So at any "nothing owed"
decision point, `foldAbsent` sends an action the engine rejects with `Rejection.ActionNotAllowed`,
its own no-progress guard then stops the loop, and the fold-through silently does nothing.

The duel stalls **forever**: the absent seat is still on turn, still absent, and nothing will ever
move it.

Reachable spots, all ordinary:

- the big blind's option preflop, when the small blind calls rather than raises;
- first to act on any street with no bet yet — the flop, turn or river after a checked street.

Every case in `AbsentSeatsTest` folds a seat that owes something, which is why the gap survived a
deep review. `TASK-020808`'s fixture — the present seat acts first, then the turn reaches the absent
seat — is what first reached it, and two of that ticket's tests fail on it today.

## Answered by `ADR-0023` (`DEC-020`)

`ADR-0013` is titled *"A dropped connection gets a grace period, then folds"* and says "the player's
current hand is folded". That is not implementable where folding is illegal, so **what an absent
seat does at a free decision point is an open decision**, not an implementation detail:

- **Check when nothing is owed, fold when facing a bet** — the universal poker-room convention for a
  timed-out player. Consequence: an absent player can be checked down to showdown and *win* the
  hand, which contradicts `ADR-0013`'s plain words.
- **Always concede the hand**, which would need the engine to accept a fold at a free spot — a
  change to `poker-engine`'s legal-action rule, pinned by `TASK-020508` and its tests, and a much
  larger blast radius.

**`ADR-0023` chose the first**, and narrowed `ADR-0013` explicitly rather than diverging from it.
The rule is: send `Fold` when `FOLD` is in the engine's `allowed` set, `Check` otherwise — never a
chip into the pot. `BettingRules` adds `CHECK` *xor* `FOLD`, so every non-empty legal set holds
exactly one of them and the rule is total: an all-in and a short stack need no special case.
`poker-engine` does not change.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/duel/AbsentSeats.kt` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/duel/AbsentSeatsTest.kt` | modify |

## Scope

- `foldAbsent` picks the give-up action `DEC-020` names, from the engine's own `legalActions` at that
  decision point — never from a guess about what the spot looks like. Reading the legal set is the
  only way to be right at an all-in, a short stack, or any spot a later rule change introduces.
- The action still goes through the ordinary `act` path. `TASK-020806`'s design holds: no bespoke
  engine call, no hand-built event.
- The loop's existing exits stay as they are. This changes *which action is sent*, never *when the
  loop stops*.

## Out of scope

- `poker-engine`. Unless `DEC-020` says otherwise, the legal-action rule is not this ticket's to
  change.
- `Room.act`'s pause guard (`TASK-020807`), and the wiring in `TASK-020808`.

## Tests

`AbsentSeatsTest`

| Test | Proves |
| --- | --- |
| an absent seat at the big blind's option progresses the hand | the preflop free spot, which stalls today |
| an absent seat first to act on a checked street progresses the hand | the postflop free spot |
| an absent seat facing a bet still folds | the existing behaviour is unchanged where folding is legal |
| a duel with one absent player always reaches its end | the property that matters, under a timeout |

The first two must **fail against `develop`** before this ticket's change. State that in the PR —
a regression test that never went red proves nothing.

## Acceptance criteria

- [ ] `foldAbsent` reads the engine's `legalActions` and sends an action from it
- [ ] The two free-spot tests fail on `develop` and pass here
- [ ] A seat facing a bet still folds, unchanged
- [ ] A duel with an absent player terminates, asserted under a timeout
- [ ] `TASK-020808`'s two failing cases pass once this lands
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
