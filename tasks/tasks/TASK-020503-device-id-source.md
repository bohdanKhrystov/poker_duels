---
schema: 2
id: TASK-020503
title: Mint opaque device ids from an injected secure random source
type: task
status: done
parent: STORY-0205
module: poker-server
estimate: S
tier: haiku
review: standard
files_touched: 2
labels: [server, session, identity, security]
depends_on: [TASK-020502]
verify:
  - ./gradlew :poker-server:test --tests '*DeviceIdSourceTest'
  - ./gradlew :poker-server:check
  - test -z "$(grep -rl 'kotlin.random.Random' poker-server/src)"
---

## Goal

The server can issue a device id that a client did not choose: opaque, URL-safe, and drawn from an
injected `java.security.SecureRandom` by default.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/session/DeviceIdSource.kt` | create |
| `poker-server/src/test/kotlin/duels/poker/server/session/DeviceIdSourceTest.kt` | create |

Read, do not modify:
`poker-server/src/main/kotlin/duels/poker/server/session/PlayerDirectory.kt` (for `DeviceId`),
`docs/adr/ADR-0012-device-bound-anonymous-profiles.md`.

## Scope

- One main file, package `duels.poker.server.session`, KDoc on everything public:

  ```kotlin
  private const val ID_BYTES = 16

  public fun interface DeviceIdSource {
      public fun newDeviceId(): DeviceId
  }

  public class RandomDeviceIdSource(private val random: Random = SecureRandom()) : DeviceIdSource {
      public val isSecure: Boolean get() = random is SecureRandom

      override fun newDeviceId(): DeviceId {
          val bytes = ByteArray(ID_BYTES)
          random.nextBytes(bytes)
          return DeviceId(Base64.getUrlEncoder().withoutPadding().encodeToString(bytes))
      }
  }
  ```

  `Random` is `java.util.Random`, `SecureRandom` is `java.security.SecureRandom`, `Base64` is
  `java.util.Base64`. 16 bytes is 128 bits of entropy and encodes to 22 URL-safe characters.
- The source is **injected**, never a global. `isSecure` exists so a test can state, rather than
  assume, that the default draws from `SecureRandom`.
- KDoc must say why this is not the engine's `Rng`: the engine's randomness is deterministic by
  design because a duel must replay identically, and a value that replays is exactly what an
  identifier must not be. Nothing in this file names anything from `duels.poker.engine`.
- No id is ever logged, printed or embedded in an exception message in this file.

## Out of scope

- Deciding which profile an id maps to — `PlayerDirectory`, `TASK-020502`.
- Session ids — `TASK-020504` mints those from `UUID.randomUUID()`.
- Persisting an issued id, or expiring one. `ADR-0012` gives v0.1 no revocation and `EPIC-04` owns
  the claim flow.
- Rate limiting how many ids one caller may mint. `ADR-0012` names unlimited profile creation as a
  known, accepted v0.1 cost and gates it on `EPIC-05`.

## Tests

`DeviceIdSourceTest`, JUnit 5, package `duels.poker.server.session`. `@Timeout(60)` on the
hundred-thousand test.

| Test | Proves |
| --- | --- |
| `issuesUniqueIdsAcrossOneHundredThousand` | 100 000 ids from one `RandomDeviceIdSource()` collected into a `HashSet` give `size == 100_000` |
| `everyIdIsTwentyTwoUrlSafeCharacters` | 1 000 ids each match `Regex("^[A-Za-z0-9_-]{22}$")` |
| `theDefaultSourceIsSecure` | `RandomDeviceIdSource().isSecure` is `true` |
| `anInjectedSourceIsTheOnlySourceOfRandomness` | two `RandomDeviceIdSource(Random(42))` instances issue the same first five ids, so nothing else feeds the id |
| `aNonSecureInjectedSourceIsReportedAsSuch` | `RandomDeviceIdSource(Random(1)).isSecure` is `false` |

## Acceptance criteria

- [ ] `DeviceIdSourceTest.issuesUniqueIdsAcrossOneHundredThousand` passes
- [ ] `DeviceIdSourceTest.everyIdIsTwentyTwoUrlSafeCharacters` passes
- [ ] `DeviceIdSourceTest.theDefaultSourceIsSecure` passes
- [ ] `DeviceIdSourceTest.anInjectedSourceIsTheOnlySourceOfRandomness` passes
- [ ] `DeviceIdSourceTest.aNonSecureInjectedSourceIsReportedAsSuch` passes
- [ ] `DeviceIdSource.kt` contains no occurrence of `duels.poker.engine`
- [ ] No file under `poker-server/src` mentions `kotlin.random.Random`, i.e.
      `test -z "$(grep -rl 'kotlin.random.Random' poker-server/src)"` exits 0
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
