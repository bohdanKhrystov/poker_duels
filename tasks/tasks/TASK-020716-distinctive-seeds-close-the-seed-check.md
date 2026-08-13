---
schema: 2
id: TASK-020716
title: Distinctive seeds close the hand-one hole in the seed-leak check
type: task
status: done
parent: STORY-0207
module: poker-server
estimate: XS
tier: haiku
review: standard
files_touched: 2
labels: [server, test-coverage, security]
depends_on: [TASK-020712]
verify:
  - ./gradlew :poker-server:test --tests '*RunnerLeakTest'
  - ./gradlew :poker-server:test --tests '*RunnerDuelTest'
  - ./gradlew :poker-server:check
---

## Goal

`RunnerLeakTest.noEncodedFrameContainsAHandSeed` asserts that no encoded frame contains the decimal
text of any hand's seed — a predictable seed is a predictable shuffle, so a leaked one lets a player
know the deck.

**It skips hand 1, on every duel.** `playDuel(seed)` passes its own seed argument straight into
`startDuel`, and `openHand` records it verbatim as `hands[0].seed`. `TASK-020712` mandated the seeds
`1L..20L`, whose decimal text (`1`, `2`, …) appears in every frame anyway as `"sequence":1`, a seat
number, a hand number. The check could not pass without excluding that value, so it excludes it.

The exclusion was correct inside that ticket — it fixed the seed range and made `PlayedDuel.kt`
read-only — but it leaves a real hole. In production hand 1's seed comes from the injected
`HandSeedSource` and **is** secret; a leak of it would be a genuine vulnerability, and this test
could not see it.

Note the hole is narrow: the *card*-leak checks cover hand 1 in full. Only the seed check is blind.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/test/kotlin/duels/poker/server/duel/RunnerLeakTest.kt` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/duel/RunnerDuelTest.kt` | modify |

`PlayedDuel.kt` stays **unmodified** — the harness is right; only the values fed to it change.

## Scope

- Replace the seeds with values whose decimal text cannot collide with a sequence number, seat,
  hand number, chip amount or stack — something like `0x5EED_000000000001 + i`, which renders as a
  long distinctive decimal. State in a comment **why** the seeds look odd, or the next person will
  simplify them back to `1..20` and silently reopen this.
- **Delete the exclusion.** Every hand's seed, hand 1 included, is checked.
- Keep the seed count the ticket used, and keep the seeds explicit so a failure names one and
  reproduces.
- `RunnerDuelTest` is in the Files table only because it also plays duels by seed; adjust it only
  if the seed change requires it. If it does not, leave it alone and say so.

## Tests

No new test file. The existing `noEncodedFrameContainsAHandSeed` gains hand 1.

Prove the check is real once the exclusion is gone: **temporarily** make `Addressed.kt` include a
hand's seed in an outbound frame, confirm the test fails, then restore it and confirm green and a
clean `git status`. Report the observed output of all three steps. Without that, "the exclusion is
gone" is a claim about the diff rather than about what the test detects.

## Done

All three `verify:` commands exit 0, no seed value is excluded from the check, and a deliberately
leaked seed is shown to fail the test.
