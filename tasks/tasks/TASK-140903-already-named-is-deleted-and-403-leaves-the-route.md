---
schema: 2
id: TASK-140903
title: AlreadyNamed is deleted, and 403 leaves the route
type: task
status: done
parent: STORY-1409
module: poker-server
estimate: XS
tier: sonnet
review: standard
files_touched: 6
atomic:
  - "`:poker-server:compileTestKotlin` — `ProfileRouteTest` names `SetNameResult.AlreadyNamed` twice and fails *Unresolved reference* the moment the object goes"
  - "`:poker-server:compileKotlin` — `ProfileRoutes`'s `when` over the sealed interface stops compiling with a branch for a case that no longer exists"
  - "`:poker-server:test` — `ProfileWritesPortTest` reads `SetNameResult::class.sealedSubclasses` by **reflection** and asserts the count is 3 and that `AlreadyNamed` is among them; no compiler can see it, and it reddens only at test execution"
labels: [server, protocol, account]
depends_on: [TASK-140902]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.http.ProfileWritesPortTest' -PrequireDocker=true
  - grep -q 'tests="3" skipped="0" failures="0" errors="0"' poker-server/build/test-results/test/TEST-duels.poker.server.http.ProfileWritesPortTest.xml
  - ./gradlew :poker-server:test --tests 'duels.poker.server.http.ProfileRouteTest' -PrequireDocker=true
  - grep -q 'tests="48" skipped="0" failures="0" errors="0"' poker-server/build/test-results/test/TEST-duels.poker.server.http.ProfileRouteTest.xml
  - sh -c '! grep -rqF AlreadyNamed poker-server/src'
  - sh -c '! grep -qF Forbidden poker-server/src/main/kotlin/duels/poker/server/http/ProfileRoutes.kt'
  - sh -c '! grep -qF "else ->" poker-server/src/main/kotlin/duels/poker/server/http/ProfileRoutes.kt'
  - ./gradlew :poker-server:ktlintCheck
  - ./gradlew :poker-server:detekt
  - ./gradlew check -PrequireDocker=true
  - python3 .github/scripts/lint_tickets.py
---

## Goal

`SetNameResult` has exactly two subclasses — `NameSet` and `NameTaken` — the `when` in
`ProfileRoutes` is exhaustive over both with **no `else`**, and `403 Forbidden` is answered nowhere
on `PUT /api/me/name`.

## Files

Four, **probed not remembered** (`ADR-0069`, `ADR-0070`). The object and the route branch were
deleted alone and `./gradlew check -PrequireDocker=true` — the command
`.github/workflows/build.yml` runs on a pull request — was run in full and driven to `BUILD
SUCCESSFUL`. Three files were named by the Kotlin compiler; the fourth is invisible to it and
appeared only once the tree compiled.

| File | Action | Why it cannot be fewer |
| --- | --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/http/ProfileWrites.kt` | modify | The `AlreadyNamed` object and the sealed interface's KDoc, which says *three outcomes* and names `403` |
| `poker-server/src/main/kotlin/duels/poker/server/http/ProfileRoutes.kt` | modify | The `when` branch and the `profileRoutes` KDoc paragraph that documents the `403` |
| `poker-server/src/test/kotlin/duels/poker/server/http/ProfileRouteTest.kt` | modify | *Unresolved reference 'AlreadyNamed'* at lines 1135 and 1173 on `compileTestKotlin` |
| `poker-server/src/test/kotlin/duels/poker/server/http/ProfileWritesPortTest.kt` | modify | `assertEquals(3, sealedSubclasses.size, …)` and an `assertContains(classNames, "AlreadyNamed", …)` — **reflection**, so it compiles fine and fails at test execution |
| `poker-server/src/main/kotlin/duels/poker/server/db/PostgresProfileWrites.kt` | modify | **Added at landing, comment only.** Line 102 reads *"unlike the old `AlreadyNamed` branch this replaces"* — a comment `TASK-140902` introduced, naming a type **this** ticket deletes. Reword to keep the explanation without the dangling name; no code changes |
| `poker-server/src/test/kotlin/duels/poker/server/db/NameBlocklistTest.kt` | modify | **Added at landing, comment only.** Line 144 names the same deleted outcome, and predates this ticket's baseline. Same treatment: reword, do not delete — the comment explains why a blocklisted name refuses differently, which is still true |

Read, and do not edit:
[`ADR-0134`](../../docs/adr/ADR-0134-a-rename-spends-before-it-replaces.md) §2 and §5.

## Scope

- **Delete `SetNameResult.AlreadyNamed`** and rewrite the sealed interface's KDoc: two outcomes, two
  statuses, and `ADR-0134` §5 as the reference in place of `ADR-0029` §5's three.
- **Delete the `403` branch in `ProfileRoutes`.** The `when` keeps exactly `NameSet` and `NameTaken`
  and **no `else`**, so a third case added later fails to compile rather than falling through
  (`ADR-0134` §2). Rewrite the `profileRoutes` KDoc sentence that lists the answers.
- **`ProfileRouteTest`**: delete `aPlayerWhoAlreadyHasANameIsForbidden`, and rewrite
  `theTwoRefusalsAreDifferentStatuses` into `theOnlyRefusalTheWriteCanAnswerIsAConflict`, keeping the
  `NameTaken → 409` half and asserting the route answers no `Forbidden` at all. 49 tests become 48.
- **`ProfileWritesPortTest`**: the subclass count becomes 2, and the `assertContains` for
  `AlreadyNamed` becomes an **equality** over the whole name set —
  `assertEquals(setOf("NameSet", "NameTaken"), classNames.toSet())` — so a fourth subclass added
  later fails here too. 3 tests stay 3.

## The whole-tree grep, widened at landing

`verify:` asserts `AlreadyNamed` appears **nowhere** under `poker-server/src`. After the four files
above it survives in exactly two **comments**, neither in the original Files table and one of them
explicitly listed out of scope:

- `PostgresProfileWrites.kt:102`, introduced by `TASK-140902` — which merged an hour before this
  ticket started. The chain created its own obstacle.
- `NameBlocklistTest.kt:144`, predating this ticket's baseline.

The gate is right and the file budget was wrong. A comment naming a type the same commit deletes is a
**dangling reference**, and the alternative — narrowing the gate to the files the ticket owns — would
leave the product referring to something that no longer exists while a green run said otherwise.

Both are reworded, not deleted: each says something true about *why* a refusal behaves as it does, and
only the name of the vanished outcome has to go. `PostgresProfileWrites.kt` remains out of scope for
every purpose except this comment — no SQL, no logic, no test.

## Out of scope

- **The budget and the `429`.** `TASK-140906` adds them; this ticket adds no status.
- **`docs/protocol.md`.** `TASK-140907` deletes the `403` row. Until it lands the document is ahead
  of the server in one direction that breaks nothing: the document names a status the server no
  longer answers, and `HttpEndpointDocumentationTest` only checks *documented ⇒ present in section*.
- **`PostgresProfileWrites`.** `TASK-140902` already stopped it producing this result.

## Tests

`ProfileWritesPortTest` — 3 on `develop` at `1c3c7fd9`, 3 after.

| Test | Proves |
| --- | --- |
| `theResultTypeIsSealedWithTheOutcomesTheRoutesMap` | *(modified)* `SetNameResult` has exactly two sealed subclasses and their names are exactly `NameSet` and `NameTaken` |

`ProfileRouteTest` — 49 on `develop` at `1c3c7fd9`, 48 after.

| Test | Proves |
| --- | --- |
| `theOnlyRefusalTheWriteCanAnswerIsAConflict` | *(rewritten)* a port answering `NameTaken` produces `409`, and the response is not `403` |
| `aNameTakenAnswersConflict` and the other 46 | *(unchanged, must stay green)* |

## What would still pass if the coder got it wrong

- **If `AlreadyNamed` were kept and only the route branch deleted**, the `when` fails to compile —
  the sealed hierarchy is the gate, not a test.
- **If the route grew an `else -> call.respond(Forbidden)`**, every test in the file still passes,
  because no port in the suite can produce a third case. A `verify` command greps `ProfileRoutes.kt`
  for `else ->` for exactly that reason: this is a property of the source, not of any reachable
  behaviour.
- **If `ProfileWritesPortTest` kept `assertContains` and only dropped the `AlreadyNamed` line**, a
  future third subclass would slip in silently. The equality over the name set is what forbids it,
  and swapping `assertEquals` for two `assertContains` calls would be a weakening.

## Acceptance criteria

- [ ] `ProfileWritesPortTest.theResultTypeIsSealedWithTheOutcomesTheRoutesMap` passes and asserts a
      set **equality**, not two containments
- [ ] `ProfileWritesPortTest` reports exactly 3 tests
- [ ] `ProfileRouteTest.theOnlyRefusalTheWriteCanAnswerIsAConflict` passes
- [ ] `ProfileRouteTest` reports exactly 48 tests, `failures="0" errors="0"` — 49 measured on
      `develop` at `1c3c7fd9` minus `aPlayerWhoAlreadyHasANameIsForbidden`
- [ ] `AlreadyNamed` appears in no file under `poker-server/src`
- [ ] `Forbidden` and `else ->` appear nowhere in `ProfileRoutes.kt`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
