---
schema: 2
id: TASK-040511
title: The profile read follows the resolved player, and every route resolves the same way
type: task
status: done
parent: STORY-0405
module: poker-server
estimate: S
tier: sonnet
review: standard
files_touched: 25
atomic:
  - the Kotlin compiler — ProfileReads.profileOf changes its parameter type, so all four implementations and all five call sites stop compiling at once
  - the Kotlin compiler again — profileRoutes, authRoutes and standingsRoutes each gain a required parameter, and every caller in main and test must pass it
  - ProfileRouteTest.aKnownDeviceGetsItsProfile — a merged assertion on what the reads double was asked for, which this change re-keys
  - ktlintMainSourceSetCheck and ktlintTestSourceSetCheck — import order and unused imports in every file the two above touch
labels: [server, http, identity, routes]
depends_on: [TASK-040510]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.http.ProfileRouteTest'
  - ./gradlew :poker-server:test --tests 'duels.poker.server.http.StandingsRouteTest'
  - ./gradlew :poker-server:test --tests 'duels.poker.server.http.AuthRouteTest'
  - ./gradlew :poker-server:test --tests 'duels.poker.server.db.PostgresProfileReadsTest' -PrequireDocker=true
  - ./gradlew :poker-server:check -PrequireDocker=true
---

## Goal

`ProfileReads.profileOf` takes a `PlayerId`, and every HTTP route gets that player from
`IdentityResolver` instead of reading a header itself — so a request under a session reads the
session's profile rather than whatever profile the device beside it happens to own.

> **Why one commit.** `ADR-0030` §4 requires the end state to have **no** device-keyed read on
> `ProfileReads`; a transitional overload would be exactly the function that ADR forbids, sitting
> on the port for several tickets with nothing stopping a coder from binding to it. And the port's
> parameter type cannot change without its four implementations and five call sites changing in
> the same commit — that is the compiler, not a preference.

## Files

Twenty-four **measured** by the `ADR-0070` probe (see *Notes*), plus one new test-support file this
ticket creates.

| File | Action | Why it cannot be fewer |
| --- | --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/http/ProfileReads.kt` | modify | the signature itself |
| `poker-server/src/main/kotlin/duels/poker/server/db/PostgresProfileReads.kt` | modify | `compileKotlin` — the override, and `PROFILE_OF_SQL`'s `WHERE` |
| `poker-server/src/main/kotlin/duels/poker/server/http/ProfileRoutes.kt` | modify | `compileKotlin` — three call sites, and the resolver parameter |
| `poker-server/src/main/kotlin/duels/poker/server/http/AuthRoutes.kt` | modify | `compileKotlin` — one call site, and the resolver parameter |
| `poker-server/src/main/kotlin/duels/poker/server/http/StandingsRoutes.kt` | modify | `compileKotlin` — one call site, and the resolver parameter |
| `poker-server/src/main/kotlin/duels/poker/server/Application.kt` | modify | `compileKotlin` — *No value passed for parameter 'identities'* on all three installs |
| `poker-server/src/main/kotlin/duels/poker/server/ServerComponents.kt` | modify | `compileKotlin` — the resolver has to be built somewhere the composition root can reach |
| `poker-server/src/test/kotlin/duels/poker/server/http/ProfileReadsDoubles.kt` | create | the `FixedDirectory` and `identitiesFor(...)` every route test below needs; a second declaration in an existing doubles file would trip ktlint's filename rule |
| `poker-server/src/test/kotlin/duels/poker/server/http/AuthRouteDoubles.kt` | modify | `compileTestKotlin` — `FixedProfileReads` does not implement the new member |
| `poker-server/src/test/kotlin/duels/poker/server/http/ProfileRouteTest.kt` | modify | `compileTestKotlin` on its own `FakeProfileReads` and four `profileRoutes(...)` shapes; **and** `aKnownDeviceGetsItsProfile` at test runtime |
| `poker-server/src/test/kotlin/duels/poker/server/http/StandingsRouteTest.kt` | modify | `compileTestKotlin` — its `RecordingProfileReads` and six `standingsRoutes(...)` call sites |
| `poker-server/src/test/kotlin/duels/poker/server/http/AuthRouteTest.kt` | modify | `compileTestKotlin` — sixteen `authRoutes(reads, credentials)` call sites |
| `poker-server/src/test/kotlin/duels/poker/server/http/SignUpSecrecyTest.kt` | modify | `compileTestKotlin` — the same call |
| `poker-server/src/test/kotlin/duels/poker/server/http/AuthRouteDoublesTest.kt` | modify | `compileTestKotlin` — five `reads.profileOf(DeviceId(...))` calls, and the `queried` assertions that follow them |
| `poker-server/src/test/kotlin/duels/poker/server/http/ProfileEndpointsDatabaseTest.kt` | modify | `compileTestKotlin` — seven `profileRoutes(...)` call sites |
| `poker-server/src/test/kotlin/duels/poker/server/http/DuelHistoryFilterDatabaseTest.kt` | modify | `compileTestKotlin` — five `profileRoutes(...)` call sites |
| `poker-server/src/test/kotlin/duels/poker/server/http/DuelHistoryPagingDatabaseTest.kt` | modify | `compileTestKotlin` — three `profileRoutes(...)` call sites |
| `poker-server/src/test/kotlin/duels/poker/server/http/StandingsSelfDatabaseTest.kt` | modify | `compileTestKotlin` — two `standingsRoutes(...)` call sites |
| `poker-server/src/test/kotlin/duels/poker/server/http/StandingsWalkDatabaseTest.kt` | modify | `compileTestKotlin` — five `standingsRoutes(...)` call sites |
| `poker-server/src/test/kotlin/duels/poker/server/ServerComponentsTest.kt` | modify | `compileTestKotlin` — two `components.reads.profileOf(DeviceId(...))` calls |
| `poker-server/src/test/kotlin/duels/poker/server/db/PostgresProfileReadsTest.kt` | modify | `compileTestKotlin` — thirteen `profileOf(DeviceId(...))` calls |
| `poker-server/src/test/kotlin/duels/poker/server/db/PostgresSelfStandingTest.kt` | modify | `compileTestKotlin` — one call |
| `poker-server/src/test/kotlin/duels/poker/server/db/PostgresStandingsReadsTest.kt` | modify | `compileTestKotlin` — two calls |
| `poker-server/src/test/kotlin/duels/poker/server/e2e/SocketDuelTest.kt` | modify | `compileTestKotlin` — two calls |
| `poker-server/src/test/kotlin/duels/poker/server/e2e/SocketLadderTest.kt` | modify | `compileTestKotlin` — two calls; its own `HttpClient.profileOf(deviceId)` helper is an HTTP call and does **not** change |

Read `docs/adr/ADR-0030-a-claim-adds-a-credential-and-moves-nothing.md` §4 and `ADR-0027` §4.
Nothing else.

## Scope

- `ProfileReads.profileOf(playerId: PlayerId): ProfileResponse?`. **The port gains no function that
  takes a `DeviceId`**, now or later — `ADR-0030` §4 — so `recentDuelsOf` and this are both
  player-keyed and nothing on the port can key on a device again by accident.
- `PostgresProfileReads`: `WHERE p.id = ?`, bound with `setObject(1, UUID.fromString(playerId.value))`
  the way `PostgresProfileWrites` already binds a player id. The join and the four selected
  expressions do not otherwise change.
- Each of `profileRoutes`, `authRoutes` and `standingsRoutes` gains a final
  `identities: IdentityResolver` parameter, and `duelServer` passes `components.identities`.
  `serverComponents` builds `IdentityResolver(PostgresAuthSessions(dataSource, wallClock), directory)`
  and exposes it as `ServerComponents.identities`.
- Two `internal` helpers in `ProfileRoutes.kt`, used by all three route files:
  `ApplicationCall.sessionTokenOrNull()` reading `Authorization: Bearer <token>` (blank after the
  prefix is treated as absent, the way `deviceIdOrNull` treats a blank header), and
  `ApplicationCall.resolvedPlayerOrNull(identities)` mapping `Session`/`Device` to their player and
  `UnknownDevice`/`Refused`/`Anonymous` to `null`. **Exhaustive `when`, never `as?`** — a new
  `Identity` case must break the build rather than silently become a `401`.
- Every route's identity step keeps its position: still resolved **before** a body is read and
  before a query string is parsed, so none of the existing ordering guarantees move.
- **`ProfileRouteTest.aKnownDeviceGetsItsProfile` is the one merged assertion this change
  invalidates.** Its last line asserts the reads double was asked about `"alice"`; the double is now
  asked about a player id, so it becomes `"p-alice"` — the id that test's own fixture already
  names. Nothing else in that file changes, no assertion is deleted or weakened, and no `contains`
  becomes a `isNotEmpty`.
- The test doubles keep their device-keyed **constructors** — `FixedProfileReads(mapOf("alice" to
  profileResponse("p-alice", …)))` — and index by `it.playerId` internally, so ~40 call sites keep
  their fixtures verbatim and `identitiesFor(profiles)` gives the resolver the same map's
  device→player edges. That is what keeps this ticket mechanical.

## Out of scope

- Any *new* assertion about session precedence over HTTP — `TASK-040512`. This ticket proves the
  existing behaviour is unchanged; nothing here issues a token, so `Identity.Session` is
  unreachable in every test it touches.
- Sign-in and sign-out — `TASK-040514`, `TASK-040515`.
- The socket, which still resolves its device itself — `TASK-040517`, `TASK-040518`.

## Tests

No new test file. The suites in `verify:` are the gate: they are the merged behaviour of five
endpoints, and this ticket's claim is that **none of it changes**.

`PostgresProfileReadsTest` gains one method, because the port's contract moved:

| Test | Proves |
| --- | --- |
| `readingAnUnknownPlayerIsNullAndCreatesNothing` | `profileOf(PlayerId(<a random UUID>))` is `null` and `SELECT count(*) FROM player` is unchanged — the replacement for `readingAnUnknownDeviceCreatesNoProfile`, which had a device id to be unknown about and now does not |

## Acceptance criteria

- [ ] `ProfileRouteTest`, `StandingsRouteTest`, `AuthRouteTest`, `AuthRouteDoublesTest`,
      `ProfileEndpointsDatabaseTest`, `DuelHistoryFilterDatabaseTest`,
      `DuelHistoryPagingDatabaseTest`, `StandingsSelfDatabaseTest`, `StandingsWalkDatabaseTest` and
      `PostgresProfileReadsTest` all pass
- [ ] `PostgresProfileReadsTest.readingAnUnknownPlayerIsNullAndCreatesNothing` passes
- [ ] `grep -rn "profileOf(DeviceId(" poker-server/src` finds nothing — the loose form
      `profileOf(deviceId` also matches four unrelated `HttpClient.profileOf(deviceId: String)`
      test helpers, one of which this ticket's own Files table preserves
- [ ] `ProfileReads` declares no function taking a `DeviceId`
- [ ] Exactly one assertion changed in `ProfileRouteTest`, and it changed a string rather than a
      predicate — no `assertEquals` became an `assertTrue`, no count became a `>=`
- [ ] `git diff --name-only` lists exactly the twenty-five rows of the *Files* table
- [ ] Every command in `verify:` exits 0

## Proof

Make `PostgresProfileReads` select `WHERE p.device_id = ?` again and `PostgresProfileReadsTest`
goes red — eleven of forty-eight, exactly the tests that call `profileOf`. Run, confirmed.

Make `sessionTokenOrNull` return `null` for a header that does not start with the bearer prefix, so
a malformed credential arrives as *absent* rather than invalid, and
`aMalformedAuthorizationHeaderIsRefusedNotDowngradedToTheDevice` goes red — **alone**, out of
eighty-five tests across five classes. No pre-existing test catches this class. Run, confirmed.

This Proof previously offered a third mutation — answer `Identity.UnknownDevice`'s device id as a
player id — and claimed `ProfileRouteTest`'s unknown-device tests go red. **They do not.** Every
unknown-device fixture there is `FakeProfileReads(emptyMap())` with no positive control, so the
leaked value fails to find a profile either way and the response stays 401. The property holds; that
falsification does not exercise it. Found by running it.

## Notes

**`TASK-040510`'s deep review left this ticket a warning.** The no-fall-back property — an invalid
session is `Refused`, never downgraded to the device — is proven today **only at the `IdentityResolver`
unit**. The reviewer confirmed it by writing the realistic downgrade bug (fall back only when the
device lookup *succeeds*) and watching exactly one test go red. That test's power rests on one seeded
line: without `directory.resolve(deviceId)` before the call, the same bug passes all eight tests.

A caller that mis-parses a malformed `Authorization` header into *no token presented*, rather than a
present-but-invalid `SessionToken`, reintroduces that exact bug one layer up — outside the diff that
proved it. An empty token is already correct (`resolve()` gates on `!= null`, so `SessionToken("")`
takes the invalid path), but a header the parser drops entirely is not the same case. **Where this
ticket wires a caller to the resolver, assert that a malformed credential reaches the resolver as
present-and-invalid, with a resolvable device beside it.**


**Twenty-four was measured** (`ADR-0069`, `ADR-0070`). On a clean tree, `PlayerDirectory.findOrNull`
(`TASK-040509`), `AuthSessions` (`TASK-040505`), `IdentityResolver` (`TASK-040510`) and a stub
`PostgresAuthSessions` (`TASK-040506`) were added — those six paths belong to those tickets and are
**not** counted here — and then this ticket's own change was made. The commands
`.github/workflows/build.yml` runs were run in full: `./gradlew check -PrequireDocker=true`, then
`npm ci`, `npm run check` and `npm run build` in `web-client/`. Six iterations; the loop stopped on
**exit 0**, 1338 Kotlin tests and 571 client tests run, none skipped, Testcontainers included. The
probe was then reverted. **No client file moved at all** — this change is invisible to the wire.

Two findings the compiler could not give: `ProfileRouteTest.aKnownDeviceGetsItsProfile` failed only
at test *execution*, and `ktlintMainSourceSetCheck` failed only after every test passed, on unused
imports and import order in ten of these files. A red run names a prefix.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.
