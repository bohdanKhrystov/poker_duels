---
schema: 2
id: TASK-010420
title: Reflective immutability test over the domain types
type: task
status: done
parent: STORY-0104
module: poker-engine
estimate: S
tier: haiku
review: light
files_touched: 1
labels: [engine, domain, test]
depends_on: [TASK-010419]
verify:
  - ./gradlew :poker-engine:test --tests '*DomainImmutabilityTest'
  - ./gradlew :poker-engine:check
---

## Goal

A `var` added to a domain type in six months' time fails the build the same day it is written.

## Files

| File | Action |
| --- | --- |
| `poker-engine/src/test/kotlin/duels/poker/engine/game/DomainImmutabilityTest.kt` | create |

Read the class names from `poker-engine/src/main/kotlin/duels/poker/engine/game/` — no main
source is modified by this ticket.

## Scope

- A test-only file holding one list and three tests:

  ```kotlin
  // Every value type in duels.poker.engine.game. Adding a domain type means adding it here;
  // the JVM erases Kotlin's read-only/mutable list distinction, so this list is the only
  // handle a test has on the package.
  private val domainTypes: List<Class<*>> = listOf(
      GameState::class.java, Seat::class.java, Board::class.java, LegalActions::class.java,
      EngineResult::class.java,
      HandStarted::class.java, BlindPosted::class.java, HoleCardsDealt::class.java,
      ActionOn::class.java,
      PlayerFolded::class.java, PlayerChecked::class.java, PlayerCalled::class.java,
      PlayerBet::class.java, PlayerRaised::class.java, PlayerAllIn::class.java,
      BettingRoundEnded::class.java, StreetDealt::class.java, ShowdownReached::class.java,
      HandRevealed::class.java, UncalledBetReturned::class.java, PotAwarded::class.java,
      HandFinished::class.java,
      PlayerAction.Fold::class.java, PlayerAction.Check::class.java,
      PlayerAction.Call::class.java, PlayerAction.Bet::class.java,
      PlayerAction.Raise::class.java, PlayerAction.AllIn::class.java,
      Rejection.NotYourTurn::class.java, Rejection.ActionNotAllowed::class.java,
      Rejection.AmountTooSmall::class.java, Rejection.AmountTooLarge::class.java,
  )
  ```

  Use `java.lang.reflect.Modifier` and filter out synthetic and static members before asserting.
- Assertion failures must name the offending class and member, otherwise the failure is
  unreadable at the point it fires.

## Out of scope

- `Street`, `ActionType` (enums), `Rejection.HandComplete` (a `data object`), and the card and
  hand packages — other stories own those and enums have no constructor properties.
- Deep immutability of the `List` properties: the JVM erases `MutableList` to `java.util.List`,
  so reflection cannot tell them apart. That guarantee comes from the compiler and from review.

## Tests

`DomainImmutabilityTest`, JUnit 5.

| Test | Proves |
| --- | --- |
| `noDomainTypeHasANonFinalField` | for every class in `domainTypes`, every declared non-static, non-synthetic field is `final` — a `var` property would fail |
| `noDomainTypeExposesASetter` | no declared, non-synthetic public method is named `set…` with exactly one parameter |
| `everyDomainTypeDefinesValueEquality` | every class in `domainTypes` declares its own `equals` and `hashCode` — which a `data class` generates and a plain class does not |
| `theDomainTypeListIsNotEmpty` | `domainTypes.size >= 30`, so a bad refactor cannot make this file vacuously green |

## Acceptance criteria

- [ ] `DomainImmutabilityTest.noDomainTypeHasANonFinalField` passes
- [ ] `DomainImmutabilityTest.noDomainTypeExposesASetter` passes
- [ ] `DomainImmutabilityTest.everyDomainTypeDefinesValueEquality` passes
- [ ] `DomainImmutabilityTest.theDomainTypeListIsNotEmpty` passes
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
