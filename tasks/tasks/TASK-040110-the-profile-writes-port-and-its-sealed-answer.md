---
schema: 2
id: TASK-040110
title: The ProfileWrites port, its sealed answer, and no lookup by name
type: task
status: done
parent: STORY-0401
module: poker-server
estimate: S
tier: haiku
review: standard
files_touched: 2
labels: [server, http, ports, identity]
depends_on: [TASK-040109]
verify:
  - ./gradlew :poker-server:test --tests '*ProfileWritesPortTest'
  - ./gradlew :poker-server:ktlintCheck
  - ./gradlew :poker-server:detekt
---

## Goal

There is a port for writing a display name, its answer is a sealed type rather than an exception,
and neither profile port offers any function that turns a name into an identity.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/http/ProfileWrites.kt` | create |
| `poker-server/src/test/kotlin/duels/poker/server/http/ProfileWritesPortTest.kt` | create |
| `poker-server/src/main/kotlin/duels/poker/server/http/ProfileReads.kt` | read — the KDoc contract this port exists to preserve |
| `docs/adr/ADR-0029-a-display-name-is-unique-and-permanent.md` | read — §5 for the three answers, §7 for the structural rule |

## Scope

- `public interface ProfileWrites` in `duels.poker.server.http`, with one function:
  `public suspend fun setDisplayName(playerId: PlayerId, canonicalName: String): SetNameResult`.
- `public sealed interface SetNameResult` in the same file:
  `NameSet(val profile: ProfileResponse)`, `NameTaken`, `AlreadyNamed`. Data class for the first,
  objects for the other two.
- KDoc that says what each answer means and, on the interface, **why this is not on `ProfileReads`**:
  that port's contract is that nothing on it creates or mutates, tests rely on it, and the first
  HTTP write gets its own port rather than eroding the guarantee (`ADR-0021`).
- The parameter is named `canonicalName` and its KDoc says the caller has already canonicalised it
  (`canonicalDisplayNameOrNull`). A port that re-canonicalises would be a second place the rule
  lives.
- No implementation in this ticket.

## Out of scope

- `PostgresProfileWrites` — `TASK-040111`.
- The route — `TASK-040115`.
- Wiring into `ServerComponents` — `TASK-040114`.

## Tests

`ProfileWritesPortTest` — reflection over the two ports' public API, in the spirit of `ADR-0029` §7:
the guarantee should not depend on every future author remembering it.

| Test | Proves |
| --- | --- |
| `noPortFunctionTakesANameAndReturnsAnIdentity` | across every public member of `ProfileReads` and `ProfileWrites`, no function whose parameter list contains a `String` named for a display name returns `PlayerId`, `DeviceId` or `ProfileResponse` — enumerated over the members, not spot-checked on one |
| `theResultIsSealedAndHasExactlyThreeCases` | `SetNameResult::class.sealedSubclasses` has exactly the three named cases — a fourth answer added later has to be added here too |
| `settingANameReturnsTheProfileItProduced` | a hand-written test double returning `NameSet` hands back the `ProfileResponse` it was built with, so the answer carries the canonical name rather than a boolean |

## Acceptance criteria

- [ ] All three tests above pass
- [ ] `noPortFunctionTakesANameAndReturnsAnIdentity` enumerates the members of **both** ports and
      fails if a matching function is added to either — assert over the collected list, not with a
      single `assertTrue`
- [ ] `theResultIsSealedAndHasExactlyThreeCases` asserts the exact set of names, not the count alone
- [ ] `ProfileReads.kt` is unchanged
- [ ] `ktlintCheck` and `detekt` pass for `:poker-server`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
