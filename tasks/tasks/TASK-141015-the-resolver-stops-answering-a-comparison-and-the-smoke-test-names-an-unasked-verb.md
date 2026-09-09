---
schema: 2
id: TASK-141015
title: The resolver stops answering a comparison, and the smoke test names a verb nobody asked for
type: task
status: done
parent: STORY-1410
module: poker-server
estimate: XS
tier: sonnet
review: standard
files_touched: 3
labels: [server, account, auth]
depends_on: [TASK-141002]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.auth.IdentityResolverTest' -PrequireDocker=true
  - grep -q 'tests="8" skipped="0" failures="0" errors="0"' poker-server/build/test-results/test/TEST-duels.poker.server.auth.IdentityResolverTest.xml
  - ./gradlew :poker-server:test --tests 'duels.poker.server.DuelServerRoutesTest' -PrequireDocker=true
  - grep -q 'tests="9" skipped="0" failures="0" errors="0"' poker-server/build/test-results/test/TEST-duels.poker.server.DuelServerRoutesTest.xml
  - sh -c '! grep -rqF "namesPlayer" poker-server/src'
  - grep -c 'public suspend fun ' poker-server/src/main/kotlin/duels/poker/server/auth/IdentityResolver.kt | grep -qx 1
  - grep -c 'players.findOrNull' poker-server/src/main/kotlin/duels/poker/server/auth/IdentityResolver.kt | grep -qx 1
  - grep -c 'public data class \|public data object ' poker-server/src/main/kotlin/duels/poker/server/auth/IdentityResolver.kt | grep -qx 5
  - awk 'index($0,"client.put(\"/api/me/device\")"){n++} END{exit (n!=1)}' poker-server/src/test/kotlin/duels/poker/server/DuelServerRoutesTest.kt
  - awk 'index($0,"client.delete(\"/api/me/device\")"){n++} END{exit (n!=1)}' poker-server/src/test/kotlin/duels/poker/server/DuelServerRoutesTest.kt
  - awk 'index($0,"client.get(\"/api/me/device\")"){n++} END{exit (n!=0)}' poker-server/src/test/kotlin/duels/poker/server/DuelServerRoutesTest.kt
  - awk 'index($0,"theDeviceRouteIsNotInstalledOnAnyOtherVerb"){n++} END{exit (n!=0)}' poker-server/src/test/kotlin/duels/poker/server/DuelServerRoutesTest.kt
  - ./gradlew :poker-server:ktlintCheck
  - ./gradlew :poker-server:detekt
  - ./gradlew check -PrequireDocker=true
  - python3 .github/scripts/lint_tickets.py
---

## Goal

The two merged claims that contradict `ADR-0135` are gone before the route arrives: `IdentityResolver`
stops offering a comparison that cannot answer `ADR-0135` §5's table, and `DuelServerRoutesTest`'s
negative control names a verb nobody has asked for instead of the `GET` that `ADR-0135` §4 requires.

Both are removals of claims [`ADR-0144`](../../docs/adr/ADR-0144-a-negative-route-test-is-a-control-and-the-predicate-resolves-the-device-once.md)
supersedes. **Nothing a player can see changes.** This ticket ships no behaviour; it clears
`TASK-141003`, which is blocked behind it.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/auth/IdentityResolver.kt` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/auth/IdentityResolverTest.kt` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/DuelServerRoutesTest.kt` | modify |

Read, do not edit: `docs/adr/ADR-0144-a-negative-route-test-is-a-control-and-the-predicate-resolves-the-device-once.md`
§§2, 4 and 5.

## Scope

- **`IdentityResolver.namesPlayer` is deleted**, together with the class-KDoc paragraph that
  introduces it — *"This class also answers a comparison question via [namesPlayer], which is **not**
  a second resolution path…"*. The file returns to exactly what it was before `TASK-141002`: one
  public suspend function (`resolve`), one `players.findOrNull` site, five `Identity` cases. The three
  `grep -c` gates in `verify:` are `TASK-141002`'s own three, read in the other direction.

  `ADR-0144` §5 is why: the function answers one boolean, and `ADR-0135` §5 requires *resolves to
  nobody* and *resolves to somebody else* to give **different** answers, which one boolean cannot do.
  `TASK-141003` computes the predicate from `resolve` instead. Its KDoc claim — *"used by the session
  restoration endpoint per `ADR-0135` §4"* — describes a use that will now never happen.

- **`IdentityResolverTest` loses its four `namesPlayer` tests**, `aDeviceResolvingToThatPlayerNamesThem`,
  `aDeviceResolvingToAnotherPlayerDoesNotNameThem`, `aDeviceResolvingToNobodyNamesNobody` and
  `namesPlayerAsksTheDirectoryOnceAndNeverMints`. **12 → 8.**
  - **`CountingPlayerDirectory` stays.** `aValidSessionDoesNotEvenLookAtTheDevice` (line 87 today)
    uses it, and deleting it would redden a test this ticket does not touch.
  - Imports left unused by the deletion go with them — `./gradlew :poker-server:ktlintCheck` is in
    `verify:` and is the thing that decides.
  - **The never-mints property is not lost with the test that named it.** The merged
    `resolvingCreatesNoProfile` already calls `resolve(token = null, deviceId = knownDevice)` and
    `resolve(token = null, deviceId = DeviceId("d-ghost"))` and asserts the directory's profile count
    is unchanged — the exact two calls `TASK-141003`'s predicate makes.

- **`DuelServerRoutesTest`'s negative control is re-pointed, not retired.** It is renamed
  `theDeviceRouteIsNotInstalledOnUnaskedVerbs` and issues `client.put("/api/me/device")`. Its body is
  otherwise unchanged, **including both inequalities and the reason for them**: Ktor answers `404` or
  `405` depending on how the path matched, and the assertion is deliberately agnostic between the two.
  - `PUT` is the replacement because `PUT` is installed elsewhere under `/api/me/` — `PUT
    /api/me/name` — so the control also refuses a path-matching mistake that a verb absent from the
    whole server could not see. It occurs nowhere else in this file, which is what makes the `awk`
    gate exact.
  - **The comment states the rule, not a roster**, so there is nothing in it to go stale when a third
    verb arrives. Replace the `// Only DELETE was asked for (ADR-0049 §5)` line with the substance of
    `ADR-0144` §2: this is the negative control for `theDeviceRouteIsInstalled` — it names a verb no
    merged ADR has asked for, so that a handler typed beside the installed ones cannot be invisible —
    and it is **not** a prohibition on the path. Cite `ADR-0144` §2, not `ADR-0049` §5.
  - `theDeviceRouteIsInstalled` is **not edited**: it still `delete`s and still asserts `401`
    specifically, which is what makes an uninstalled route (`404`) falsify it. The `awk` gate counting
    exactly one `client.delete("/api/me/device")` says so.
  - The class count stays **9**, so the count gate does not discriminate here and is not asked to —
    the three `awk` gates are what pin the change.

## Out of scope

- **`DeviceRoutes.kt`, `ProfileDtos.kt`, `DeviceRouteTest.kt`.** The route is `TASK-141003`, which
  depends on this ticket. Nothing here installs, names or tests a `GET`.
- **A positive smoke row for `GET /api/me/device`.** `ADR-0144` §4 decides against one and gives the
  argument: `theDeviceRouteIsInstalled` already proves `duelServer` calls `deviceRoutes`, and
  `DeviceRouteTest` installs `deviceRoutes` alone, so a `get` placed in any other route file fails six
  tests there. Adding one here would assert a route that does not exist yet.
- **`docs/protocol.md`.** `TASK-141004` owns the device-standing section.
- **Any other test in either test file.** Both counts are pinned: the eight surviving resolver tests
  and the eight `DuelServerRoutesTest` methods this ticket does not name are untouched.

## Tests

No test is added. Two are edited by deletion and one by re-pointing, and the ticket's proof is that
the two files still pass at their new and unchanged counts.

| Change | Proves |
| --- | --- |
| `IdentityResolverTest` **12 → 8** | the four deleted tests are the four `namesPlayer` ones and no other. A coder who deleted a fifth, or who deleted the class instead of the tests, misses the count |
| `DuelServerRoutesTest` **9 → 9** | nothing was dropped while the verb moved. A control deleted rather than re-pointed reads `8` |
| `client.put("/api/me/device")` present exactly once, `client.get("/api/me/device")` absent, `client.delete("/api/me/device")` present exactly once | the control moved to `PUT`, the `GET` assertion is gone, and the **positive** row was not moved with it. The third gate is the one that catches a coder who re-pointed the wrong test |
| `theDeviceRouteIsNotInstalledOnAnyOtherVerb` absent | the old name went with the old verb, so no reader finds a method whose name promises *any other verb* while it tests one |
| `! grep -rqF "namesPlayer" poker-server/src` | the function, its KDoc paragraph, its four tests **and** any leftover reference anywhere under `src` are gone together — a partial deletion that left a comment behind fails |
| `public suspend fun ` = **1**, `players.findOrNull` = **1**, `public data class \|public data object ` = **5** | `TASK-141002`'s three structural gates read backwards. `resolve` and all five `Identity` cases are byte-unchanged, which is what says the deletion did not turn into a refactor |

## What would still pass if the coder were wrong

- **Deleting `theDeviceRouteIsNotInstalledOnAnyOtherVerb` outright and adding nothing** passes the
  `GET`-absent gate and the old-name gate. The class count of **9** and the `client.put` gate are what
  refuse it — the control must be re-pointed, and `ADR-0144` §2 is why.
- **Re-pointing to a verb that is installed elsewhere on that exact path** — there is none today, but
  a coder choosing `DELETE` would make the test assert the opposite of `theDeviceRouteIsInstalled` and
  redden it, loudly.
- **Deleting `CountingPlayerDirectory` along with its last `namesPlayer` use** reddens
  `IdentityResolverTest` at compile time, because a surviving test still constructs it. That is a
  compile failure, not a silent pass.
- **Weakening the control to `assert(status != HttpStatusCode.OK)` alone** — true before and after the
  route lands, and therefore no longer a statement about installation at all. Nothing greps for the
  second inequality; the reviewer is what catches this, and `ADR-0144` §2 is the sentence to check it
  against.
- **Removing `namesPlayer` from the source but leaving its tests** fails compilation. The reverse —
  leaving the function and deleting the tests — passes compilation and fails the `grep -rqF` gate and
  both `grep -c` gates.

## Acceptance criteria

- [ ] `IdentityResolverTest` reports `tests="8" skipped="0" failures="0" errors="0"`
- [ ] `DuelServerRoutesTest` reports `tests="9" skipped="0" failures="0" errors="0"`
- [ ] `namesPlayer` appears nowhere under `poker-server/src`
- [ ] `IdentityResolver.kt` has one `public suspend fun `, one `players.findOrNull` and five
      `Identity` cases
- [ ] The four `awk` gates over `DuelServerRoutesTest.kt` exit 0
- [ ] `./gradlew :poker-server:ktlintCheck`, `:poker-server:detekt` and `check -PrequireDocker=true`
      exit 0
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md).

## Notes

**`TASK-141002`'s `verify:` block stops passing when this ticket merges** — its three structural gates
count *two* public suspend functions and *two* `players.findOrNull` sites, and its count gate expects
`tests="12"`. Nothing re-runs a merged ticket's gates, so no build breaks. It is named here, and in
`ADR-0144` §Consequences, so that the trail carries the reason rather than leaving a future reader to
find a done ticket whose recorded proof is false against the tree.
