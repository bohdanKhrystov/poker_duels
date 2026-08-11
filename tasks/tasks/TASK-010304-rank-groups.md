---
schema: 2
id: TASK-010304
title: Group ranks by count into tiebreak order
type: task
status: backlog
parent: STORY-0103
module: poker-engine
estimate: S
tier: haiku
review: standard
files_touched: 2
labels: [engine, rules]
depends_on: [TASK-010303]
verify:
  - ./gradlew :poker-engine:test --tests '*RankGroupsTest'
  - ./gradlew :poker-engine:check
---

## Goal

Five ranks collapse into groups ordered by how many of each there are and then by rank, which is
exactly the tiebreak order every paired category needs.

## Files

| File | Action |
| --- | --- |
| `poker-engine/src/main/kotlin/duels/poker/engine/hand/HandPatterns.kt` | modify |
| `poker-engine/src/test/kotlin/duels/poker/engine/hand/RankGroupsTest.kt` | create |

Read `card/Rank.kt`. Leave `straightHighOrNull` exactly as it is.

## Scope

- Add to `HandPatterns.kt`, alongside `straightHighOrNull`:

  ```kotlin
  internal data class RankGroup(val rank: Rank, val size: Int)

  internal fun rankGroups(ranks: List<Rank>): List<RankGroup>
  ```

- The returned list is sorted by **size descending, then rank descending**. So `K K 7 7 A`
  returns `[(KING, 2), (SEVEN, 2), (ACE, 1)]` — note the ace kicker sorts last despite being the
  highest card, because size wins.
- Sizes sum to the input size, and every input rank appears in exactly one group.
- The ordering must be total and deterministic: two ranks never tie on both size and rank, so no
  stability caveat is needed.
- KDoc stating why this order exists: `groups.map { it.rank }` is the tiebreak list for three of
  a kind, two pair and one pair, unchanged.

## Out of scope

- Straights and flushes — `straightHighOrNull` already exists; do not touch it.
- Deciding a `HandCategory` from the groups — `TASK-010305`.
- Any input size other than five. The function need not reject other sizes, but nothing tests
  them.

## Tests

`RankGroupsTest`

| Test | Proves |
| --- | --- |
| `quadsComeBeforeTheKicker` | `9-9-9-9-A` gives `[(NINE, 4), (ACE, 1)]` |
| `fullHouseOrdersTripsBeforeThePair` | `3-3-3-K-K` gives `[(THREE, 3), (KING, 2)]` |
| `twoPairOrdersHighPairThenLowPairThenKicker` | `K-K-7-7-A` gives `[(KING, 2), (SEVEN, 2), (ACE, 1)]` |
| `onePairPutsThePairFirstAndKickersDescending` | `5-5-A-K-2` gives `[(FIVE, 2), (ACE, 1), (KING, 1), (TWO, 1)]` |
| `unpairedRanksAreFiveSingletonsDescending` | `A-Q-9-5-2` gives five groups of size 1 in descending rank order |
| `everyCardLandsInExactlyOneGroup` | for each of the five hands above, the group sizes sum to 5 and the ranks are distinct |

## Acceptance criteria

- [ ] `RankGroupsTest.quadsComeBeforeTheKicker` passes
- [ ] `RankGroupsTest.fullHouseOrdersTripsBeforeThePair` passes
- [ ] `RankGroupsTest.twoPairOrdersHighPairThenLowPairThenKicker` passes
- [ ] `RankGroupsTest.onePairPutsThePairFirstAndKickersDescending` passes
- [ ] `RankGroupsTest.unpairedRanksAreFiveSingletonsDescending` passes
- [ ] `RankGroupsTest.everyCardLandsInExactlyOneGroup` passes
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
