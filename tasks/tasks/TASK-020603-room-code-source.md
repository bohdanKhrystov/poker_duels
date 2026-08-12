---
schema: 2
id: TASK-020603
title: Mint room codes from an injected secure source, never from the engine Rng
type: task
status: backlog
parent: STORY-0206
module: poker-server
estimate: S
tier: haiku
review: standard
files_touched: 2
labels: [server, rooms, security]
depends_on: [TASK-020602]
verify:
  - ./gradlew :poker-server:test --tests '*RoomCodeSourceTest'
  - ./gradlew :poker-server:check
---

## Goal

Room codes come from a `SecureRandom`-backed port with an injectable random source, and a test
states — rather than assumes — that the engine's reproducible `Rng` is nowhere near them.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/room/RoomCodeSource.kt` | create |
| `poker-server/src/test/kotlin/duels/poker/server/room/RoomCodeSourceTest.kt` | create |

Read, do not modify:
`poker-server/src/main/kotlin/duels/poker/server/room/RoomCode.kt`,
`poker-server/src/main/kotlin/duels/poker/server/session/DeviceIdSource.kt` (the precedent this
file copies — same shape, same `isSecure` property, same reasoning).

## Scope

- One file, package `duels.poker.server.room`, KDoc on everything public:

  ```kotlin
  public fun interface RoomCodeSource {
      public fun newRoomCode(): RoomCode
  }

  public class RandomRoomCodeSource(private val random: Random = SecureRandom()) : RoomCodeSource {
      public val isSecure: Boolean get() = random is SecureRandom
      override fun newRoomCode(): RoomCode
  }
  ```

- `newRoomCode` draws `RoomCode.LENGTH` characters, each `RoomCode.ALPHABET[random.nextInt(32)]`.
  The alphabet size is a power of two, so `nextInt` is unbiased across it — say that in the KDoc,
  because a modulo over a non-power-of-two alphabet would quietly skew the codes.
- KDoc must state the entropy (32^8 = 2^40 ≈ 1.1 × 10^12) and that a code is an **invite**: whoever
  holds it can take the open seat. Whether that alone is the right authorisation model, and whether
  join attempts need rate limiting, is `DEC-012` — reference it, do not answer it here.
- The engine's `Rng` is banned by construction: this file imports nothing from `duels.poker.engine`.
  Engine randomness replays identically from a seed, which is the exact opposite of what an invite
  code needs.
- A `java.util.Random` may be injected so tests are reproducible; production takes the default.

## Out of scope

- Uniqueness *across live rooms* — collision retry belongs to the registry, `TASK-020609`.
- Rate limiting, lockout, or host confirmation of a guest — `DEC-012`, not ticketed.

## Tests

`RoomCodeSourceTest`, JUnit 5, package `duels.poker.server.room`.

| Test | Proves |
| --- | --- |
| `everyCodeIsEightCharactersFromTheAlphabet` | 1 000 draws each match `Regex("^[${RoomCode.ALPHABET}]{8}$")` (build the pattern from the constants, not a literal) |
| `theDefaultSourceIsSecure` | `RandomRoomCodeSource().isSecure` is `true` |
| `aNonSecureInjectedSourceIsReportedAsSuch` | `RandomRoomCodeSource(Random(1)).isSecure` is `false` |
| `anInjectedSourceIsTheOnlySourceOfRandomness` | two `RandomRoomCodeSource(Random(42))` yield five identical codes in step |
| `noDuplicateInOneHundredThousandDraws` | 100 000 codes collected into a `HashSet` give `size == 100_000`; annotate `@Timeout(60)` |
| `mintsFromJavaRandomAndNeverTheEngineRng` | no `RandomRoomCodeSource::class.java.declaredFields` has a type whose name starts with `duels.poker.engine`, and the class does declare a `java.util.Random` field |

## Acceptance criteria

- [ ] `RoomCodeSourceTest.everyCodeIsEightCharactersFromTheAlphabet` passes
- [ ] `RoomCodeSourceTest.theDefaultSourceIsSecure` passes
- [ ] `RoomCodeSourceTest.aNonSecureInjectedSourceIsReportedAsSuch` passes
- [ ] `RoomCodeSourceTest.anInjectedSourceIsTheOnlySourceOfRandomness` passes
- [ ] `RoomCodeSourceTest.noDuplicateInOneHundredThousandDraws` passes
- [ ] `RoomCodeSourceTest.mintsFromJavaRandomAndNeverTheEngineRng` passes
- [ ] `RoomCodeSource.kt` contains no `import duels.poker.engine`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
