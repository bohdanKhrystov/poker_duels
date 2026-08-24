---
schema: 2
id: TASK-040621
title: The scenario ends with a revocation, and no identity endpoint escapes it
type: task
status: done
parent: STORY-0406
module: poker-server
estimate: S
tier: sonnet
review: deep
files_touched: 1
labels: [server, e2e, coins, invariant, revocation]
depends_on: [TASK-040620]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.e2e.IdentityMovesNoCoinTest' -PrequireDocker=true
---

## Goal

Revocation is the scenario's last step — P1, P2 and the byte-identical `player` table asserted
across it — and a test fails the build if anyone adds an HTTP endpoint the scenario does not
exercise.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/test/kotlin/duels/poker/server/e2e/IdentityMovesNoCoinTest.kt` | modify |

Read, and do not edit: `poker-server/src/main/kotlin/duels/poker/server/http/` (the four route
files, as **text**, for the enumeration below),
`docs/adr/ADR-0050-revoking-the-device-signs-out-everywhere-but-here.md` §1.

## Scope — the last step

- `theWholeScenarioMovesNoCoin` gains step 12: sign in again as the host's account for a token, then
  `DELETE /api/me/device` with it, `204`, then a thirteenth and final
  `assertCoinInvariantHolds("after revoking")`. Fourteen calls in the method in total, since step 12
  needs its own sign-in step asserted too.
- `playerTableSnapshot()` before and after the `DELETE`, byte-identical, **with no permitted
  exception**. `ADR-0049` §7 and `ADR-0050` §1 make this structural rather than careful: revocation's
  two statements touch `device_binding` and `auth_session` and nothing else.
- A `device_binding` snapshot before and after, asserted to differ in exactly one row and, within
  it, exactly one column — `revoked_at`. The `player` assertion says nothing moved; this one says
  something did, so the `204` is not being satisfied by a no-op.

## Scope — the enumeration

One test that reads the four files under
`poker-server/src/main/kotlin/duels/poker/server/http/` whose names end in `Routes.kt`, extracts
every string literal matching `/api/…` with a regular expression, and asserts that set equals a
`private val SCENARIO_ENDPOINTS` declared in this test file — the endpoints the scenario actually
calls.

This is what makes `ADR-0030` §5's *"total over the schema"* claim have teeth at the endpoint level.
The properties themselves are total over `player` and `duel_result`, but somebody has to *reach* the
state, and a new endpoint nobody adds to this scenario is a write path the invariant never sees.
**With this test, adding `POST /api/me/anything` to a route file fails the build** until either the
scenario calls it or somebody writes down, in the same commit, that it does not move coins.

Two honest limits, stated in the test's KDoc rather than left for a reader to find:

- It reads source text, so an endpoint whose path is assembled from constants rather than written as
  a literal escapes it. Every route in the repository today writes its path as a literal, and this
  test is the reason to keep doing so.
- It says the scenario *calls* each path, not that it calls it in a state where a defect would show.
  That is the reviewer's judgement and the ticket says so.

`GET /api/standings` is in the expected set and the scenario reads it once, after step 11: it is a
read that writes nothing, and including it costs one line and closes the hole a narrower list would
leave. There is no `/api/standings/me` literal — the self-standing rides on `/api/standings` — and
the expected set is written from what the sources say, never from what an ADR says they should say.

**On `develop` today the route sources hold seven paths**: `/api/auth/sign-up`,
`/api/auth/sign-in`, `/api/auth/sign-out`, `/api/me`, `/api/me/duels`, `/api/me/name` and
`/api/standings`. `TASK-040609` adds `/api/me/device`, so `SCENARIO_ENDPOINTS` has **eight**
entries. That count is a fact about today and is not a criterion — the criterion is the set
equality, which stays right when the count moves.

## Out of scope

- `/health`, which is not under `/api/`.
- Any file under `poker-server/src/main`.
- `HttpEndpointDocumentationTest`'s own known gap — that the *document* enumerates nothing — which is
  `TASK-040516`'s recorded finding and still unticketed.

## Tests

`IdentityMovesNoCoinTest`

| Test | Proves |
| --- | --- |
| `theWholeScenarioMovesNoCoin` | Fourteen `assertCoinInvariantHolds` calls, all passing, the last one after the revocation. The story's first acceptance criterion, complete |
| `revokingLeavesThePlayerTableByteIdentical` | The step-12 `player` snapshots are equal, with no exception. The story's second acceptance criterion, for revocation |
| `revokingChangesExactlyOneBindingColumn` | The `device_binding` snapshots differ in exactly one row and one column, and that column is `revoked_at`. The positive control for the test above: without it, a `DELETE` that did nothing would satisfy the byte-identical claim perfectly |
| `everyApiPathInTheRouteSourcesIsExercisedByTheScenario` | The set of `/api/…` literals in the four `*Routes.kt` files equals `SCENARIO_ENDPOINTS`. Asserted with `assertEquals` on two sets, so a path in the sources and missing from the scenario **and** a path in the scenario that no longer exists both fail, and the failure message prints both sides |
| `theEnumerationFoundTheEndpointsItIsChecking` | The set read **out of the sources** is non-empty and contains `/api/me/device`, `/api/auth/sign-in` and `/api/me`. The vacuity guard, and it must assert against the *scanned* set rather than against `SCENARIO_ENDPOINTS`: a regular expression that matched nothing would otherwise make the test above pass with both sides empty |

## Acceptance criteria

- [ ] All five test methods above pass
- [ ] `theWholeScenarioMovesNoCoin` contains exactly fourteen calls to `assertCoinInvariantHolds`,
      with fourteen different step strings
- [ ] `revokingLeavesThePlayerTableByteIdentical` asserts equality with **no** permitted-exception
      branch of any kind
- [ ] `everyApiPathInTheRouteSourcesIsExercisedByTheScenario` compares two sets with `assertEquals`,
      never `assertTrue(a.containsAll(b))`
- [ ] `theEnumerationFoundTheEndpointsItIsChecking` names at least three concrete paths
- [ ] Every test method `TASK-040618` and `TASK-040620` added still passes, and none of their
      assertions is edited
- [ ] The diff against `develop` touches exactly one file under `poker-server/`, and it is the
      one in the *Files* table
- [ ] Every command in `verify:` exits 0

## Proof

Two mutations.

1. **Add `get("/api/me/devices") { call.respond(HttpStatusCode.OK) }` to `DeviceRoutes.kt`.**
   `everyApiPathInTheRouteSourcesIsExercisedByTheScenario` reddens: the source set gains a path the
   scenario set does not hold. Nothing else in the class moves. This is the whole claim of the
   enumeration, and it is the answer to *"what fails when someone adds an endpoint later?"*
2. **Comment out the `UPDATE device_binding SET revoked_at = now()` statement in
   `PostgresDeviceBindings`, leaving the `DELETE`.** `revokingChangesExactlyOneBindingColumn`
   reddens — the two `device_binding` snapshots are now equal.
   `revokingLeavesThePlayerTableByteIdentical` and `theWholeScenarioMovesNoCoin` both stay
   **green**, because a revocation that revoked nothing still touches no `player` row and mints no
   coin. That asymmetry is exactly why the third test exists beside the second. Revert both.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.

## Notes

**This closes the enumeration gap `TASK-040511` left open.** That ticket said plainly that nothing
enumerated "every route", so a new route file reading headers ad hoc would fail no compiler check, no
test and no lint rule. `apiPathLiteralsInRouteSources()` now reads the four `*Routes.kt` files with
`File.readText()` **at every test run** and asserts **set equality** against `SCENARIO_ENDPOINTS` — so
a path added *or removed* fails the build until someone exercises it or records why it moves no coin.
A test rather than a `verify:` grep, deliberately: a grep gate runs once at landing, because CI runs
`lint backlog`, `client` and `check` and never a ticket's verify block.

**The vacuity guard was tested, not assumed.** A scanner that finds nothing would make the comparison
trivially satisfiable. The reviewer mutated the path regex to match nothing: both the enumeration test
and `theEnumerationFoundTheEndpointsItIsChecking` reddened, and the guard asserts against the
**scanned** set (`sourcePaths.isNotEmpty()`), not the constant.

**Two limits, both in the KDoc.** The test proves the scenario *calls* a path, not that the call would
expose a defect; and a path built from constants rather than a literal escapes the scanner. No route
file builds one today, so the second is a future risk rather than a live hole.

**Most of the suite is indifferent to the binding write, by design.** Commenting out
`revokeLiveBinding` reddens only `revokingChangesExactlyOneBindingColumn` — which is precisely why
that test exists beside the byte-identical one, as the ticket's Proof states.

