---
schema: 2
id: TASK-140906
title: The write is admitted before it spends, and refunded when it does not
type: task
status: backlog
parent: STORY-1409
module: poker-server
estimate: S
tier: sonnet
review: deep
files_touched: 7
atomic:
  - "`:poker-server:compileTestKotlin` — a fourth parameter with no default on `profileRoutes` fails *No value passed for parameter 'budget'* at 69 call sites across five test files, all at once"
  - "`:poker-server:compileKotlin` — `Application.duelServer` is the shipped call site and fails the same way"
labels: [server, http, account, security]
depends_on: [TASK-140905]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.http.ProfileRouteTest' -PrequireDocker=true
  - grep -q 'tests="52" skipped="0" failures="0" errors="0"' poker-server/build/test-results/test/TEST-duels.poker.server.http.ProfileRouteTest.xml
  - ./gradlew :poker-server:test --tests 'duels.poker.server.http.ProfileEndpointsDatabaseTest' -PrequireDocker=true
  - grep -q 'tests="7" skipped="0" failures="0" errors="0"' poker-server/build/test-results/test/TEST-duels.poker.server.http.ProfileEndpointsDatabaseTest.xml
  - ./gradlew :poker-server:test --tests 'duels.poker.server.http.DuelHistoryFilterDatabaseTest' -PrequireDocker=true
  - grep -q 'tests="5" skipped="0" failures="0" errors="0"' poker-server/build/test-results/test/TEST-duels.poker.server.http.DuelHistoryFilterDatabaseTest.xml
  - ./gradlew :poker-server:test --tests 'duels.poker.server.http.DuelHistoryPagingDatabaseTest' -PrequireDocker=true
  - grep -q 'tests="3" skipped="0" failures="0" errors="0"' poker-server/build/test-results/test/TEST-duels.poker.server.http.DuelHistoryPagingDatabaseTest.xml
  - ./gradlew :poker-server:test --tests 'duels.poker.server.http.ResetPasswordEndsSessionsTest' -PrequireDocker=true
  - grep -q 'tests="5" skipped="0" failures="0" errors="0"' poker-server/build/test-results/test/TEST-duels.poker.server.http.ResetPasswordEndsSessionsTest.xml
  - grep -qF "components.nameWriteBudget" poker-server/src/main/kotlin/duels/poker/server/Application.kt
  - grep -qF "budget.refund" poker-server/src/main/kotlin/duels/poker/server/http/ProfileRoutes.kt
  - grep -qF "TooManyRequests" poker-server/src/main/kotlin/duels/poker/server/http/ProfileRoutes.kt
  - sh -c '! grep -qF "Retry-After" poker-server/src/main/kotlin/duels/poker/server/http/ProfileRoutes.kt'
  - ./gradlew :poker-server:ktlintCheck
  - ./gradlew :poker-server:detekt
  - ./gradlew check -PrequireDocker=true
  - python3 .github/scripts/lint_tickets.py
---

## Goal

`PUT /api/me/name` reserves against a `PlayerId`-keyed `AttemptBudget` immediately before
`writes.setDisplayName`, refunds on every result that is **not** `NameSet`, and answers
`429 Too Many Requests` with an empty body and no `Retry-After` when the reservation is refused —
so the budget meters the writes that **spend** a string and never the ones that fail.

## Files

Seven, **probed not remembered** (`ADR-0069`, `ADR-0070`). The fourth parameter was added to
`profileRoutes` alone and `./gradlew check -PrequireDocker=true` — the command
`.github/workflows/build.yml` runs on a pull request — was run in full. `compileTestKotlin`
reported **69** *No value passed for parameter 'budget'* errors across the five test files below in
one batch; the minimal propagation was applied at each and the command was driven to `BUILD
SUCCESSFUL`. Docker was available and every database suite ran.

| File | Action | Why it cannot be fewer |
| --- | --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/http/ProfileRoutes.kt` | modify | The parameter, the `admit`, the `refund`, the `429` and the KDoc |
| `poker-server/src/main/kotlin/duels/poker/server/Application.kt` | modify | `duelServer`'s call is the shipped wiring — *No value passed for parameter 'budget'* on `compileKotlin` |
| `poker-server/src/test/kotlin/duels/poker/server/http/ProfileRouteTest.kt` | modify | 39 call sites, plus this ticket's own new tests |
| `poker-server/src/test/kotlin/duels/poker/server/http/ProfileEndpointsDatabaseTest.kt` | modify | 7 call sites |
| `poker-server/src/test/kotlin/duels/poker/server/http/DuelHistoryFilterDatabaseTest.kt` | modify | 5 call sites |
| `poker-server/src/test/kotlin/duels/poker/server/http/DuelHistoryPagingDatabaseTest.kt` | modify | 3 call sites |
| `poker-server/src/test/kotlin/duels/poker/server/http/ResetPasswordEndsSessionsTest.kt` | modify | 4 call sites |

Read, and do not edit:
[`ADR-0134`](../../docs/adr/ADR-0134-a-rename-spends-before-it-replaces.md) §5 and §6;
[`ADR-0074`](../../docs/adr/ADR-0074-sign-in-is-ten-wrong-passwords-a-minute-reserved-before-the-hash.md) §2 —
reserve-then-refund, and the sentence about a budget checked too early turning a `401` into a `429`;
`poker-server/src/main/kotlin/duels/poker/server/auth/AttemptBudget.kt` — `admit` records even when
it refuses, and `refund` removes one reservation.

## Scope

- **`profileRoutes` gains a fourth required parameter**, `budget: AttemptBudget`, and
  `Application.duelServer` passes `components.nameWriteBudget`. Required, not defaulted: a route
  whose brake can be forgotten at a call site is not a brake.
- **The check runs last, immediately before the write** — after identity is resolved, after the body
  decodes, after `canonicalDisplayNameOrNull`. A stranger, a malformed body and a refused character
  all keep their existing answers and never reach a `429` (`ADR-0134` §6, `ADR-0074` §2).
- **The key is `profile.playerId`**, the resolved player, never a header, a body field or a remote
  address.
- **Refuse with `429`, empty body, no `Retry-After`** (`ADR-0134` §8, matching `ADR-0056`'s shipped
  sign-up answer), and **no refund on the refusal** — `admit` records an over-budget attempt on
  purpose, so hammering extends the window rather than resetting it (`ADR-0055` §1).
- **Refund on everything that is not `NameSet`.** Today that is `NameTaken` alone, and it is written
  as `if (result !is SetNameResult.NameSet) budget.refund(key)` rather than as a `NameTaken` branch,
  so a third result added later refunds by default instead of silently burning a reservation.
- **The four unrelated test files get a deliberately generous budget** — a file-private helper
  returning `AttemptBudget(AttemptLimits(1_000_000, 60_000L), MutableClock())` — because those suites
  make many requests and must not start failing on a limit they are not about. `ktlint` orders
  `java.*`/`kotlin.*` imports last; add the three new imports in the right group or
  `ktlintTestSourceSetCheck` fails.

## Out of scope

- **`docs/protocol.md`'s `429` row.** `TASK-140907`.
- **The client's `throttled` outcome.** `TASK-140910`.
- **Budgeting any other route**, and changing `AttemptBudget` itself. `ADR-0134` §6 reuses it; it is
  not rebuilt.
- **A `Retry-After` header**, a *changes remaining* counter, or any advance notice on the form.
  `ADR-0134` §6 and §8, and `ADR-0130` §5.

## Tests

`ProfileRouteTest` — 48 tests after `TASK-140903`, 52 after this ticket.

| Test | Proves |
| --- | --- |
| `theWriteAfterTheBudgetIsSpentAnswersTooManyRequests` | with a budget of two, three successful writes from one player answer `200`, `200`, `429`, and the third response body is empty |
| `aRefusedNameCostsNoBudget` | with a budget of **two**, a player sends **six** names that the port answers `NameTaken` and then one the port answers `NameSet`: every `409` is a `409` and the final write is `200`, not `429` — the refunds are what make it so |
| `aStrangerOverTheBudgetIsStillUnauthorized` | a caller with no device id, sent five times against a budget of one, answers `401` every time and never `429` — the budget is consulted after identity, so an unresolved caller never spends it |
| `aRefusedBodyCostsNoBudget` | with a budget of one, a body that fails to decode answers `400`, and a **valid** write immediately afterwards answers `200` — the `400` never reached `admit` |

## What would still pass if the coder got it wrong

- **If the budget metered attempts rather than spending** — that is, if `refund` were never called —
  `theWriteAfterTheBudgetIsSpentAnswersTooManyRequests` still passes, and so does every merged test
  in the file. `aRefusedNameCostsNoBudget` is the only gate on the inversion `ADR-0134` §6 exists
  for, and it needs **more refusals than the budget allows** — six against a budget of two — because
  one refusal against a budget of two would pass whether or not it was refunded.
- **If `refund` were called on every result including `NameSet`**, the budget would meter nothing at
  all; `theWriteAfterTheBudgetIsSpentAnswersTooManyRequests` is what catches that, and its budget of
  two with three writes is the smallest shape that does.
- **If `admit` were called before identity resolved**, `aStrangerOverTheBudgetIsStillUnauthorized`
  fails on the second request — and nothing else in the suite would notice, because every other
  test resolves.
- **If `admit` ran before the body decoded**, `aRefusedBodyCostsNoBudget` fails on the second
  request. A single `400` against a generous budget would pass either way, so the budget is set to
  one and a real write follows.
- **If the key were a constant** — an empty string, the device header, the request path — every one
  of the four passes, because each uses one player. `aRefusedNameCostsNoBudget` therefore drives
  **two different players** against a budget of two: the second player's first write must answer
  `200` after the first player has exhausted theirs. Two keys that disagree is the only shape that
  tells a `PlayerId` key from a constant.
- **If the response carried a `Retry-After`**, no test moves; a `verify` command greps for it.

## Acceptance criteria

- [ ] `ProfileRouteTest.theWriteAfterTheBudgetIsSpentAnswersTooManyRequests` passes
- [ ] `ProfileRouteTest.aRefusedNameCostsNoBudget` passes, and drives two different players
- [ ] `ProfileRouteTest.aStrangerOverTheBudgetIsStillUnauthorized` passes
- [ ] `ProfileRouteTest.aRefusedBodyCostsNoBudget` passes
- [ ] `ProfileRouteTest` reports exactly 52 tests, `failures="0" errors="0"` — 48 after
      `TASK-140903` plus exactly the four above
- [ ] `ProfileEndpointsDatabaseTest` reports 7, `DuelHistoryFilterDatabaseTest` 5,
      `DuelHistoryPagingDatabaseTest` 3 and `ResetPasswordEndsSessionsTest` 5 — every count
      unchanged from `develop` at `1c3c7fd9`, and **no assertion in any of the four is edited**;
      the only change in those files is the argument and the helper that builds it
- [ ] `Retry-After` appears nowhere in `ProfileRoutes.kt`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
