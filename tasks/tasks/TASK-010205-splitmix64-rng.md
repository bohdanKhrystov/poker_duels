---
schema: 2
id: TASK-010205
title: Rng interface and an immutable SplitMix64 implementation
type: task
status: done
parent: STORY-0102
module: poker-engine
estimate: S
tier: sonnet
review: standard
files_touched: 3
labels: [engine, determinism]
depends_on: [TASK-010201]
verify:
  - ./gradlew :poker-engine:test --tests '*SplitMix64RngTest'
  - ./gradlew :poker-engine:check
---

## Goal

The engine has one source of randomness: a value you pass in, whose sequence is fixed by its
seed and by an algorithm written out in this repository rather than inherited from a library.

## Files

| File | Action |
| --- | --- |
| `poker-engine/src/main/kotlin/duels/poker/engine/random/Rng.kt` | create |
| `poker-engine/src/main/kotlin/duels/poker/engine/random/SplitMix64Rng.kt` | create |
| `poker-engine/src/test/kotlin/duels/poker/engine/random/SplitMix64RngTest.kt` | create |

New package `duels.poker.engine.random`. Nothing else in the module exists yet that this needs.

## Scope

- `Rng.kt` — the interface and its result type, nothing more:

  ```kotlin
  public interface Rng {
      /** A uniform value in `0 until bound`, together with the generator that follows it. */
      public fun nextInt(bound: Int): Draw

      public data class Draw(val value: Int, val next: Rng)
  }
  ```

- `SplitMix64Rng.kt` — SplitMix64, transliterated exactly. The constants below are the published
  ones written as Kotlin signed hex literals; use them verbatim.

  ```kotlin
  private const val GOLDEN_GAMMA = -0x61c8864680b583ebL // 0x9E3779B97F4A7C15
  private const val MIX_A = -0x40a7b892e31b1a47L        // 0xBF58476D1CE4E5B9
  private const val MIX_B = -0x6b2fb644ecceee15L        // 0x94D049BB133111EB
  private const val VALUE_SHIFT = 33

  public data class SplitMix64Rng(private val state: Long) : Rng {

      public fun nextLong(): LongDraw {
          val next = state + GOLDEN_GAMMA
          var z = next
          z = (z xor (z ushr 30)) * MIX_A
          z = (z xor (z ushr 27)) * MIX_B
          return LongDraw(z xor (z ushr 31), SplitMix64Rng(next))
      }

      override fun nextInt(bound: Int): Rng.Draw {
          require(bound > 0) { "bound must be positive, was $bound" }
          var source = this
          while (true) {
              val draw = source.nextLong()
              val bits = (draw.value ushr VALUE_SHIFT).toInt() // 31 bits, never negative
              val value = bits % bound
              source = draw.next
              // Rejection sampling: retry when the last, short block of the range was hit,
              // because keeping it would make small values fractionally more likely.
              if (bits - value + (bound - 1) >= 0) return Rng.Draw(value, source)
          }
      }

      public data class LongDraw(val value: Long, val next: SplitMix64Rng)
  }
  ```

- `SplitMix64Rng(seed)` starts with `state == seed`; the seed is the state, no scrambling.
- KDoc on both files stating that the algorithm, the constants and the rejection rule are a
  **durable contract**: a match is reproducible from its seed only for as long as they are
  unchanged, so altering them invalidates every stored replay.
- **Do not write the words `kotlin.random.Random`, `java.util.Random` or `Math.random` anywhere
  in these files**, KDoc and comments included — `TASK-010210` scans the source text for them.
  Say "the platform random source" instead.

## Out of scope

- `Deck` and shuffling — `TASK-010206` and `TASK-010207`.
- A second `Rng` implementation, a test double, or a `nextDouble`/`nextBoolean` surface. Add
  them when something needs them.
- Where a seed comes from and when it may be published: a server concern, `ADR-0002`.
- Carrying the seed in `GameState` — `STORY-0104`.
- Any change to `poker-engine/build.gradle.kts`. This must not add a production dependency.

## Tests

`SplitMix64RngTest`

| Test | Proves |
| --- | --- |
| `matchesTheSplitMix64ReferenceVector` | from seed `0`, the first three `nextLong().value`s are `-2152535657050944081`, `7960286522194355700`, `487617019471545679` — the published vector, so this is really SplitMix64 |
| `sameSeedProducesTheSameSequence` | two `SplitMix64Rng(12345)` yield identical first 100 `nextInt(52)` values |
| `drawingDoesNotMutateTheReceiver` | calling `nextInt(52)` twice on the same instance returns the same `value` and an equal `next` |
| `nextIntStaysWithinItsBound` | kotest property over `Arb.long()` × `Arb.int(1..1000)`: the drawn value is in `0 until bound` |
| `nextIntRejectsANonPositiveBound` | `nextInt(0)` and `nextInt(-1)` throw `IllegalArgumentException` |

Property tests use `io.kotest.property.forAll` inside `runBlocking`, as in the existing
`PropertySmokeTest`.

## Acceptance criteria

- [ ] `SplitMix64RngTest.matchesTheSplitMix64ReferenceVector` passes
- [ ] `SplitMix64RngTest.sameSeedProducesTheSameSequence` passes
- [ ] `SplitMix64RngTest.drawingDoesNotMutateTheReceiver` passes
- [ ] `SplitMix64RngTest.nextIntStaysWithinItsBound` passes
- [ ] `SplitMix64RngTest.nextIntRejectsANonPositiveBound` passes
- [ ] Every command in `verify:` exits 0, including `:poker-engine:checkNoDependencies`

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
