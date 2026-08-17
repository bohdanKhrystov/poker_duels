---
schema: 2
id: TASK-040314
title: Nothing public returns a hash, and the sweep proves it can tell
type: task
status: ready
parent: STORY-0403
module: poker-server
estimate: S
tier: sonnet
review: deep
files_touched: 1
labels: [server, auth, security, tests]
depends_on: [TASK-040313]
verify:
  - ./gradlew :poker-server:test --tests '*PublicApiHasNoHashTest'
  - ./gradlew :poker-server:check -PrequireDocker=true
---

## Goal

`ADR-0027` §1's structural claim — *no function anywhere returns a hash* — is asserted by reflecting
over every public class compiled into `duels.poker.server.auth` and `duels.poker.server.db`, so a
future author who adds one fails the build rather than having to remember the rule.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/test/kotlin/duels/poker/server/auth/PublicApiHasNoHashTest.kt` | create |
| `poker-server/src/test/kotlin/duels/poker/server/http/ProfileWritesPortTest.kt` | read — the existing reflective-guard test and its idiom |
| `docs/adr/ADR-0027-the-session-outranks-the-device-id.md` | read — §1's last two bullets, the claim being asserted |

## Scope

- Enumerate classes from the **compiled output**, not from a hand-written list:
  `Thread.currentThread().contextClassLoader.getResources("duels/poker/server/auth")` (and the same
  for `.../db`), keep the `file:` URLs whose path contains `classes/kotlin/main` — which drops the
  test source set — and `Class.forName` every `.class` file found, nested classes included.
- Keep only classes whose `KClass.visibility` is `PUBLIC`, then their members whose
  `visibility` is `PUBLIC`, taken from `declaredFunctions` and `declaredMemberProperties`.
  `declaredFunctions` is the one that also reaches top-level functions on a file facade class such
  as `LoginHandleKt`; `declaredMemberFunctions` alone silently returns none of them.
- **A member offends if** its return type classifier is `ByteArray`, **or** its name contains
  `hash`, `digest`, `phc` or `tag`, case-insensitively. The second half is why `SecretHasher`,
  `Argon2Hasher` and `Argon2Phc` are `internal` and stay that way.
- If reflecting over a class throws, the test **fails naming that class**. It never skips one
  quietly: a sweep that swallows what it cannot read is a sweep that passes for the wrong reason.

## Out of scope

- `duels.poker.server.http`, `.protocol`, `.session` and the rest. The two packages named are the
  ones that can hold a hash; widening the sweep to packages that cannot would slow it and say less.
- Changing any production visibility. If the sweep finds an offender, that is a finding to report —
  the ticket that made it public is the ticket that should have been stopped.

## Tests

`PublicApiHasNoHashTest`.

| Test | Proves |
| --- | --- |
| `noPublicMemberOfTheAuthOrDbPackagesReturnsAHash` | the collected offender list is empty, and the failure message names each offender as `Class.member: returnType` |
| `theSweepSawTheApiItIsChecking` | the collected member names contain `verify`, `create` and `loginHandleOrNull`, and the collected class list contains `Credentials`, `PostgresCredentials` and `PostgresProfileReads` — without this, a sweep that enumerates nothing passes forever |
| `theSweepFlagsABaitThatReturnsAByteArray` | the same predicate applied to a bait class declared in this test file, with `fun tagBytes(): ByteArray`, reports it — the return-type half of the rule detects |
| `theSweepFlagsABaitNamedForAHash` | the same predicate applied to a bait with `fun secretHash(): String` reports it — the name half detects, and the two baits together mean neither half can rot unnoticed |

## Acceptance criteria

- [ ] All four tests above pass
- [ ] `theSweepSawTheApiItIsChecking` is present and asserts on **named** members and classes, not
      on a count alone; a count is satisfied by any eight classes
- [ ] Both bait tests are present, one per half of the rule
- [ ] The class enumeration filters on `classes/kotlin/main` and therefore finds no test class; if
      a test class appears in the sweep, the filter is wrong even though the test may still pass
- [ ] A class that cannot be reflected over fails the test by name; no `runCatching { }` discards a
      failure
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
