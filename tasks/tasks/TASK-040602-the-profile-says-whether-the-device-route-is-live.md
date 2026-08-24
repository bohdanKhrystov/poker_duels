---
schema: 2
id: TASK-040602
title: The profile says whether the device route is still live
type: task
status: backlog
parent: STORY-0406
module: poker-server
estimate: S
tier: sonnet
review: deep
files_touched: 6
atomic:
  - the Kotlin compiler — ProfileResponse takes no default values by rule (ADR-0021, ADR-0053 §1), so a fifth field stops every construction site compiling at once
  - the poker-server test task — ProfileDtosTest pins two exact JSON strings that gain a field, and PostgresProfileWrites' two statements must return the new column or every write-path test raises PSQLException
  - the ADR-0070 probe — six files measured to exit 0; the first red run named one main source file and hid the other five behind compileKotlin
labels: [server, dto, read-path, protocol, revocation]
depends_on: [TASK-040601]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.protocol.http.ProfileDtosTest'
  - ./gradlew :poker-server:test --tests 'duels.poker.server.db.PostgresProfileReadsTest' -PrequireDocker=true
  - ./gradlew :poker-server:test --tests 'duels.poker.server.db.PostgresProfileWritesTest' -PrequireDocker=true
  - ./gradlew check -PrequireDocker=true
---

## Goal

`ProfileResponse` carries `deviceRouteLive: Boolean`, true exactly when the caller has a
`device_binding` row that has not been revoked, so `STORY-0412` can state which routes are live
without asking the server anything new.

## Files

Six, **measured** by the `ADR-0070` probe on top of `TASK-040601`'s tree: add the field with no
default, run the gate set in full, add each path it names, re-run to `exit 0`. `compileKotlin` fails
before `compileTestKotlin`, so the first run named **one** file and the next three named one or two
more each — a prefix every time.

| File | Action | Why it cannot be fewer |
| --- | --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/protocol/http/ProfileDtos.kt` | modify | the field itself |
| `poker-server/src/main/kotlin/duels/poker/server/db/PostgresProfileReads.kt` | modify | `compileKotlin` — the `ProfileResponse(...)` construction, and `PROFILE_OF_SQL` must return the column it reads |
| `poker-server/src/main/kotlin/duels/poker/server/db/PostgresProfileWrites.kt` | modify | `compileKotlin` — `toProfile()`; and then `:poker-server:test`, because its two statements must return `device_route_live` or the read raises `PSQLException` |
| `poker-server/src/test/kotlin/duels/poker/server/protocol/http/ProfileDtoFixtures.kt` | modify | `compileTestKotlin` — `profileResponse` calls the DTO constructor positionally |
| `poker-server/src/test/kotlin/duels/poker/server/http/StandingsRouteTest.kt` | modify | `compileTestKotlin` — two `ProfileResponse(...)` calls that bypass the builder |
| `poker-server/src/test/kotlin/duels/poker/server/protocol/http/ProfileDtosTest.kt` | modify | `:poker-server:test` — two exact-JSON assertions, plus the new tests below |

Read, and do not edit:
`docs/adr/ADR-0049-a-device-binding-is-a-row-and-revoking-is-final.md` §5,
`docs/adr/ADR-0053-the-profile-says-the-name-was-removed.md` §1 and §6,
`tasks/tasks/TASK-041018-the-profile-says-the-name-was-removed.md` (the field this one copies).

## Scope — the field

```kotlin
public data class ProfileResponse(
    val playerId: String,
    val coinBalance: Int,
    val displayName: String?,
    val displayNameRemoved: Boolean,
    val deviceRouteLive: Boolean,
)
```

- **`Boolean`, no default value**, for `ADR-0053` §1's reason, which this file already carries in
  KDoc: `Application.module()` installs `ContentNegotiation { json() }` whose `Json` has
  `encodeDefaults = false`, while these DTOs' tests serialise through `protocolJson`, which sets it
  `true`. A `= true` would be present in every test's JSON and absent from the real response for the
  ~100% of players whose answer is `true`.
- Declared **last**; the wire's field order is declaration order.
- KDoc says what it means and what it does not: true when a live `device_binding` row exists for
  this player, false when it was revoked **and** false when one was never created — the two are not
  distinguished, exactly as `ADR-0049` §5's uniform `204` does not distinguish them. It carries no
  device id, and the KDoc says that a device id is a bearer credential and never travels.

## Scope — the read

`PostgresProfileReads.PROFILE_OF_SQL` gains a fifth output column, correlated to `p.id` and keeping
the statement's **single** bind parameter:

```sql
EXISTS (SELECT 1 FROM device_binding b
         WHERE b.player_id = p.id AND b.revoked_at IS NULL) AS device_route_live
```

- A correlated `EXISTS`, never a `JOIN`: a player has at most one live binding today but may hold
  several revoked rows, and a join would return one profile row per binding.
- Correlated to `p.id`, never to a second `?`. Binding the caller twice works today and lets a later
  edit make the boolean describe a different player than the row does.
- Read as the fifth column, keeping the existing positional style of that block.

## Scope — the write path

`PostgresProfileWrites` computes it too, and **this is where it differs from `displayNameRemoved`.**
`ADR-0053` §6 says that field is written as the literal `false` *because* `SetNameResult.NameSet`
describes a player who now holds a name, so the answer is known by construction. Nothing of the kind
is true here: a player renaming themselves may or may not still have a live binding, so a literal
would be a lie on the `200` from `PUT /api/me/name`. That prohibition is about `displayNameRemoved`
and does not transfer.

Both statements gain the same correlated `EXISTS`, aliased `device_route_live`:

- `SET_NAME_SQL`'s `RETURNING` list — PostgreSQL allows a scalar subquery there, correlated to
  `player.id`.
- `CURRENT_PROFILE_SQL`'s select list.

Still **one round trip each**; no second query is issued and no conditional read is added.

## Scope — the fixture

`ProfileDtoFixtures.profileResponse` gains `deviceRouteLive: Boolean = true` as its **last**
parameter. The default belongs on the builder and never on the DTO, and it is `true` because that is
the state every player is in until somebody revokes — a test that does not care should read the
ordinary case.

`StandingsRouteTest`'s two direct `ProfileResponse(...)` calls each gain `deviceRouteLive = true`.
Nothing else in that file changes and no assertion in it moves.

## Out of scope

- `ProfileReads`'s and `ProfileWrites`'s port signatures. Neither gains a method or a parameter.
- `DuelSummaryResponse`, `RECENT_DUELS_SQL` and `DUELS_AFTER_SQL` — **named prohibitions.** That
  query already holds the *opponent's* `player` row, and the same `EXISTS` pasted there would tell a
  stranger about someone else's devices.
- `PROTOCOL_VERSION` and `docs/protocol-versions.md`. `ProfileResponse` is reachable from neither
  `ClientMessage` nor `ServerMessage`, so `ProtocolVersionLedgerTest`'s fingerprint and
  `web-client/src/protocol/protocol.gen.ts` are both byte-identical; the probe confirmed
  `verifyProtocolTypes` stays green.
- `docs/protocol.md` — `TASK-040612`.
- Any endpoint that changes the value. Nothing in this ticket writes `revoked_at`.

## Tests

`ProfileDtosTest`. Two existing assertions move; two tests are added, and one existing test is
named below only so a reviewer can see it was left alone. The exact strings below were
computed from the spec — declaration order is `playerId, coinBalance, displayName,
displayNameRemoved, deviceRouteLive`, `protocolJson` sets `encodeDefaults = true`, and the new field
has no default so it is emitted whatever that setting says — and then confirmed by the probe run.

| Test | Proves |
| --- | --- |
| `aProfileEncodesItsPlayerIdAndBalance` | The exact encoding becomes `{"playerId":"p-1","coinBalance":3,"displayName":null,"displayNameRemoved":false,"deviceRouteLive":true}`. **One of the two existing assertions that move**; nothing else in it changes |
| `aRemovedNameIsCarriedOnTheProfile` | Its exact string becomes `{"playerId":"p-1","coinBalance":0,"displayName":null,"displayNameRemoved":true,"deviceRouteLive":true}`. **The second and last existing assertion that moves** |
| `aRevokedDeviceRouteIsCarriedOnTheProfile` | `profileResponse("p-1", 0, deviceRouteLive = false)` encodes to `…,"displayNameRemoved":false,"deviceRouteLive":false}` and decodes back equal. **The second input**: with only the `true` fixture above, a field hard-coded to `true` passes every other test in this class |
| `aProfileWithoutTheDeviceRouteFieldIsRefused` | `protocolJson.decodeFromString(ProfileResponse.serializer(), """{"playerId":"p-1","coinBalance":0,"displayName":null,"displayNameRemoved":false}""")` **throws**. This is the assertion that pins *no default value*: with `= true` on the DTO this decode succeeds, every other test still passes, and the field silently disappears from the real wire |
| `aProfileWithoutTheFieldIsRefused` | **Pre-existing, and unchanged** — its JSON already omits two fields, so it still throws. It is neither renamed nor folded into the new test above |

`PostgresProfileReadsTest` and `PostgresProfileWritesTest` are in `verify:` **unmodified**: they
already build their expected profiles through `profileResponse(...)`, so the builder's default
carries them. If either needs an edit, the ticket stops and reports rather than editing it — that
would mean the value is not `true` by default in a fresh database, which is a finding.

## Acceptance criteria

- [ ] `ProfileDtosTest.aProfileEncodesItsPlayerIdAndBalance` passes against the exact string above
- [ ] `ProfileDtosTest.aRemovedNameIsCarriedOnTheProfile` passes against the exact string above
- [ ] `ProfileDtosTest.aRevokedDeviceRouteIsCarriedOnTheProfile` passes
- [ ] `ProfileDtosTest.aProfileWithoutTheDeviceRouteFieldIsRefused` passes
- [ ] `ProfileResponse.deviceRouteLive` is declared `Boolean` with **no** `=` in its declaration
- [ ] `PROFILE_OF_SQL` contains exactly one `?` and the substring `FROM device_binding b`, and
      contains no `JOIN`
- [ ] `RECENT_DUELS_SQL` and `DUELS_AFTER_SQL` contain no occurrence of `device_binding`
- [ ] `ProfileReads.kt` and `ProfileWrites.kt` are unmodified
- [ ] `ProtocolVersionLedgerTest` passes, `docs/protocol-versions.md` is unmodified, and
      `web-client/src/protocol/protocol.gen.ts` is unmodified
- [ ] The diff against `develop` names exactly the six files in the *Files* table, plus this ticket
      and `tasks/BOARD.md`
- [ ] Every command in `verify:` exits 0

## Proof

Replace `PROFILE_OF_SQL`'s `EXISTS (...)` expression with the literal `true`, changing nothing else.
`PostgresProfileReadsTest` stays **green** — every profile it reads does have a live binding — and so
does every test in `ProfileDtosTest`, which never touches a database. **Nothing reddens**, and that
is the finding this Proof exists to record: the DTO tests cannot see the query, and no merged
database test in this ticket's scope has a revoked binding to read. The assertion that catches a
hard-coded `true` is `TASK-040615`'s, which revokes and then reads the profile back over HTTP, and
this ticket is honest that until that ticket lands the query's correlation rests on review rather
than on a red run.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.
