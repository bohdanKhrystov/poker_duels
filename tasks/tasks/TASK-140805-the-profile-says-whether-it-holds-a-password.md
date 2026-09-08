---
schema: 2
id: TASK-140805
title: The profile says whether it holds a password
type: task
status: done
parent: STORY-1408
module: poker-server
estimate: S
tier: sonnet
review: deep
files_touched: 6
atomic:
  - the Kotlin compiler — a seventh property with no default on `ProfileResponse` fails every construction site at once, and two of them are in main sources
  - "`:poker-server:compileTestKotlin` — `ProfileDtoFixtures.profileResponse` and `StandingsRouteTest`'s two literal `ProfileResponse(...)` calls fail *No value passed for parameter*"
  - "`:poker-server:test` — five `ProfileDtosTest` cases assert the encoded JSON character for character and a seventh field breaks every one of them"
labels: [server, db, protocol, account]
depends_on: [TASK-140804]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.protocol.http.ProfileDtosTest' -PrequireDocker=true
  - ./gradlew :poker-server:test --tests 'duels.poker.server.protocol.http.ProfileDtosTest.aProfileHoldingAPasswordSaysSo' -PrequireDocker=true
  - ./gradlew :poker-server:test --tests 'duels.poker.server.protocol.http.ProfileDtosTest.aProfileWithoutThePasswordFieldIsRefused' -PrequireDocker=true
  - grep -qF CredentialKind.PASSWORD poker-server/src/main/kotlin/duels/poker/server/db/PostgresProfileReads.kt
  - grep -qF CredentialKind.PASSWORD poker-server/src/main/kotlin/duels/poker/server/db/PostgresProfileWrites.kt
  - awk 'index($0, "hasPassword") { n++ } END { exit (n < 1) }' poker-server/src/main/kotlin/duels/poker/server/protocol/http/ProfileDtos.kt
  - ./gradlew :poker-server:ktlintCheck
  - ./gradlew :poker-server:detekt
  - ./gradlew check -PrequireDocker=true
  - python3 .github/scripts/lint_tickets.py
---

## Goal

`ProfileResponse` carries `hasPassword: Boolean`, `true` exactly when a `credential` row exists for
this player with `kind = CredentialKind.PASSWORD` — computed by a correlated `EXISTS` in **all three**
statements that shape a `ProfileResponse`, and by a literal in none of them (`ADR-0132` §1, §2).

## Files

Six, **probed not remembered** (`ADR-0069`, `ADR-0070`): `val hasPassword: Boolean` was added to
`ProfileResponse` alone and `./gradlew check -PrequireDocker=true` — the command
`.github/workflows/build.yml` runs on a pull request — was run until it exited `0`, with the minimal
propagation applied at each named path. Three rounds: two main sources, then two test sources, then
five golden JSON strings that no compiler could see. Docker was available and the database suites
ran, so this enumeration is complete rather than a prefix.

| File | Action | Why it cannot be fewer |
| --- | --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/protocol/http/ProfileDtos.kt` | modify | The field and its KDoc. It has no default (`ADR-0132` §1), which is what makes every row below fail |
| `poker-server/src/main/kotlin/duels/poker/server/db/PostgresProfileReads.kt` | modify | `PROFILE_OF_SQL` and `profileOf`'s construction — *No value passed for parameter 'hasPassword'* at `PostgresProfileReads.kt:41` |
| `poker-server/src/main/kotlin/duels/poker/server/db/PostgresProfileWrites.kt` | modify | Both statements and `toProfile()` — the same error at `PostgresProfileWrites.kt:125` |
| `poker-server/src/test/kotlin/duels/poker/server/protocol/http/ProfileDtoFixtures.kt` | modify | `profileResponse(...)` fails *None of the following candidates is applicable* on `compileTestKotlin` |
| `poker-server/src/test/kotlin/duels/poker/server/http/StandingsRouteTest.kt` | modify | Builds two `ProfileResponse(...)` by hand rather than through the fixture, and fails the same way |
| `poker-server/src/test/kotlin/duels/poker/server/protocol/http/ProfileDtosTest.kt` | modify | Five cases assert the whole encoded object as one string; **invisible to the compiler**, and reached only once the four rows above were green |

Read, and do not edit:
[`ADR-0132`](../../docs/adr/ADR-0132-the-profile-says-whether-it-holds-a-password.md) §1, §2, §3 and
§8; `poker-server/src/main/kotlin/duels/poker/server/auth/Credentials.kt` — `CredentialKind.PASSWORD`
and its value; `poker-server/src/main/kotlin/duels/poker/server/db/PostgresCredentials.kt` — the
`credential` table's columns as its own statements name them.

## Scope

- **`ProfileDtos.kt`:** add `val hasPassword: Boolean` **last**, after `hasRecoveryEmail`, with **no
  default value** — the same `encodeDefaults` reasoning the three fields before it already state in
  their KDoc, and `ADR-0132` §1 turns that reasoning into a rule: a default is what collapses *not
  told* into *told anonymous* on the client. The KDoc says what it means (a `credential` row of kind
  `password` exists for this player) and **why it is named for the kind rather than the concept** —
  `ADR-0027` §1 keeps `credential` open to a passphrase or a third-party subject, so a
  `hasCredential` would one day read `true` for a player whose only stated way out is the password
  form.
- **`PostgresProfileReads.kt`:** a **fourth correlated `EXISTS`** in `PROFILE_OF_SQL`, over
  `credential`, correlated to `p.id` and reading the row's kind, aliased `has_password`; read it back
  as the seventh column. The three `EXISTS` already there are the shape to copy, and the comment
  above the constant explains each — add the fourth in the same register.
- **`PostgresProfileWrites.kt`:** the **same `EXISTS` in both statements** — `SET_NAME_SQL`'s
  `RETURNING` list and `CURRENT_PROFILE_SQL`'s select list — and read it in `toProfile()`. `ADR-0132`
  §2 is explicit that this field is **not** given `hasRecoveryEmail`'s literal treatment here, so
  `PUT /api/me/name`'s `200` cannot turn a claimed player anonymous. Extract the fragment to a
  `private const val` beside `DEVICE_ROUTE_LIVE_EXISTS`, and correlate it to `player.id` exactly as
  that one does.
- **Bind the kind, never spell it.** Both classes bind `CredentialKind.PASSWORD.value` as a
  statement parameter. A SQL literal `'password'` compiles, runs, and answers `false` for every
  player in the product the day the constant is renamed (`ADR-0132` §2), which is why the two
  `verify:` greps forbid the literal outright.
- **Mind the parameter positions — they move, and JDBC will not tell you.** The player id is `?` 1 in
  `PROFILE_OF_SQL` today; a bound kind inside a select-list `EXISTS` sits **before** the `WHERE`, so
  in `PROFILE_OF_SQL` and in `CURRENT_PROFILE_SQL` the kind is `?` 1 and the id moves to `?` 2, while
  in `SET_NAME_SQL` the new `?` sits in `RETURNING`, **after** the `WHERE`, and becomes `?` 3.
  `PROFILE_OF_SQL`'s comment says the id is bound *"exactly once"* and *"never to a second `?`"* —
  that rule is about the **player id** and a bound kind does not weaken it (`ADR-0132` §2); say so
  where the parameter is bound, so the next reader does not undo it.
- **`ProfileDtoFixtures.kt`:** `profileResponse` gains `hasPassword: Boolean = false` as its last
  parameter, and passes it through. The default is the fixture's, not the DTO's.
- **`StandingsRouteTest.kt`:** add `hasPassword = false` to the two literal constructions. Nothing
  else in that file changes, and no assertion moves.
- **`ProfileDtosTest.kt`:** extend the five golden strings with `,"hasPassword":false` — written out
  as a literal, never assembled from a constant, because a golden string that references the encoder
  cannot catch the encoder (`aProfileEncodesItsPlayerIdAndBalance`, `aRemovedNameIsCarriedOnTheProfile`,
  `aRevokedDeviceRouteIsCarriedOnTheProfile`, `aProfileWithRecoveryOnSaysSo`,
  `aProfileWithNoRecoveryEmailSaysSo`). Then add the two new cases below.

## Out of scope

- **`hasRecoveryEmail`'s literal `false` in `toProfile()`.** It is a real, reachable defect —
  `ProfileProvider.reportNameWrite` adopts that body wholesale, so a player with a verified recovery
  address who sets a display name reads `RECOVERY_OFF` until the tab next boots — and `ADR-0132`
  §Residuals names it, calls it *"a ticket for the planner, not a `DEC`"* and deliberately leaves it.
  Do **not** repair it here, do not widen either statement to carry it, and do not change the comment
  that defends it beyond what the new field needs. The point of this ticket's §2 wording is that
  `hasPassword` does not join it.
- **Reading the field back out of Postgres.** `TASK-140806` (the read statement) and `TASK-140807`
  (both write statements) do that, against the container. Neither file is named by any gate this
  ticket's edits fail, which is why they are separate (the same shape `TASK-041641` took).
- **`docs/protocol.md`.** `TASK-140808`. Nothing in the gate set fails while the row is missing:
  `HttpEndpointDocumentationTest.theDocumentedFieldNamesAllExist` checks *documented ⇒ exists*, not
  the converse — verified on the probe tree, which was fully green with the field undocumented.
- **Any other DTO, route, log line, `ServerMessage` or engine type.** `ADR-0132` §3, and nothing
  enforces it structurally today.
- **A migration, an index or a new port.** `ADR-0132` §2: no new collaborator, one statement, one
  round trip. `ProfileRoutes` does not gain `Credentials`.

## Tests

`ProfileDtosTest`

| Test | Proves |
| --- | --- |
| `aProfileHoldingAPasswordSaysSo` | `profileResponse("p-1", 0, hasPassword = true)` encodes to a JSON object whose `hasPassword` is `true`, asserted as the whole golden string. Paired with the five edited cases, which all carry `false`, so the field is read from the argument and not printed as a constant |
| `aProfileWithoutThePasswordFieldIsRefused` | Decoding a body carrying the other six fields and not `hasPassword` throws `IllegalArgumentException` — the field has no default on the wire, exactly as `aProfileWithoutTheDeviceRouteFieldIsRefused` already asserts for its own |

## Acceptance criteria

- [ ] `ProfileDtosTest.aProfileHoldingAPasswordSaysSo` passes
- [ ] `ProfileDtosTest.aProfileWithoutThePasswordFieldIsRefused` passes
- [ ] Every other case in `ProfileDtosTest` passes, with its golden string extended by
      `,"hasPassword":false` and no assertion weakened or deleted
- [ ] `grep -F CredentialKind.PASSWORD` finds a match in **both** `PostgresProfileReads.kt` and
      `PostgresProfileWrites.kt`, and neither file contains the SQL literal `'password'` — the kind
      is bound as a statement parameter, never spelled inside a statement
- [ ] `PostgresProfileWrites.toProfile()` reads `hasPassword` off the `ResultSet` and passes no
      literal for it, while `hasRecoveryEmail`'s literal `false` is left exactly as it is
- [ ] `./gradlew check -PrequireDocker=true` exits 0
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md).
