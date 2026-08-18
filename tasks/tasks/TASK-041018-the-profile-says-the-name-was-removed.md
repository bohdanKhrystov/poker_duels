---
schema: 2
id: TASK-041018
title: The profile says the name was removed, from one correlated EXISTS
type: task
status: ready
parent: STORY-0410
module: poker-server
estimate: S
tier: sonnet
review: deep
files_touched: 3
labels: [server, dto, read-path, protocol, moderation]
depends_on: [TASK-041017]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.protocol.http.ProfileDtosTest'
  - ./gradlew :poker-server:check -PrequireDocker=true
---

## Goal

`ProfileResponse` carries `displayNameRemoved: Boolean`, true exactly when the caller holds no
display name and a name has been retired from them, computed inside the existing profile `SELECT`.

## Five files, and why the cap cannot be met here

`ProfileResponse` takes **no default values by design** (`ADR-0021`, and `ADR-0053` §1 makes it a
rule for this file), so a new field breaks every construction site at compile time. There are five:
the DTO, the test builder, the exact-JSON test, and the two production constructions. `TASK-041017`
already removed the sixth. Four of the five changes are one line; the fifth is the `SELECT`.
Frontmatter says `3` because the linter enforces `3`; the real number is `5` and this paragraph is
the disclosure.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/protocol/http/ProfileDtos.kt` | modify — one field, KDoc |
| `poker-server/src/main/kotlin/duels/poker/server/db/PostgresProfileReads.kt` | modify — `profileOf`'s statement only |
| `poker-server/src/main/kotlin/duels/poker/server/db/PostgresProfileWrites.kt` | modify — one literal |
| `poker-server/src/test/kotlin/duels/poker/server/protocol/http/ProfileDtoFixtures.kt` | modify — one defaulted parameter |
| `poker-server/src/test/kotlin/duels/poker/server/protocol/http/ProfileDtosTest.kt` | modify — one assertion, two new tests |

Read, not edited: `docs/adr/ADR-0053-the-profile-says-the-name-was-removed.md` §§1–4.

## Scope — the field

```kotlin
@Serializable
public data class ProfileResponse(
    val playerId: String,
    val coinBalance: Int,
    val displayName: String?,
    val displayNameRemoved: Boolean,
)
```

- **`Boolean`, not `Boolean?`, and no default value.** The default is the thing to get wrong:
  `Application.module()` installs `ContentNegotiation { json() }` whose `Json` has
  `encodeDefaults = false`, while these DTOs' tests serialise through `protocolJson`, which sets
  `encodeDefaults = true`. A `= false` would be **present in every test's JSON and absent from the
  real response** for the ~100% of players whose answer is `false`. `RecentDuelsResponse.duels`
  already carries this warning; `ADR-0053` §1 makes it a rule for the file.
- KDoc says what the two `null` states are: `displayName == null && !displayNameRemoved` is *never
  set*; `displayName == null && displayNameRemoved` is *removed by an operator*. It is named for the
  product's word (`ADR-0052` §2's shipped copy), not the schema's `RETIRED`.
- Declaration order is exactly as above — the wire's field order is declaration order.

## Scope — the query

`PostgresProfileReads.profileOf`, one statement, one round trip:

```sql
SELECT p.id,
       p.coin_balance,
       p.display_name,
       (p.display_name IS NULL
        AND EXISTS (SELECT 1 FROM name_registry r
                     WHERE r.retired_from = p.id AND r.reason = 'RETIRED')) AS display_name_removed
FROM player p
WHERE p.device_id = ?
```

- **A correlated `EXISTS`, never a `LEFT JOIN`.** A player may hold more than one retired name, so a
  join returns two rows for one profile and `if (rows.next())` silently takes the first. `EXISTS` is
  a semijoin: one boolean per profile row whatever the registry holds.
- **Correlated to `p.id`, never to a second bind parameter.** The statement keeps exactly one `?`.
  Binding the caller's id twice would work today and would let a later edit make the boolean describe
  a different player than the row does.
- `AND r.reason = 'RETIRED'` is redundant under `name_registry_retired_from` and **stays** — the
  statement should read correctly without the constraint in hand.
- The result is read as a fourth column; keep the existing positional reads or move the whole read to
  labels, but do not mix the two styles in one statement.

## Scope — the write path

`PostgresProfileWrites.toProfile()` passes the literal `false`, with the reason beside it:
`SetNameResult.NameSet` describes a player who now holds a name — including `ADR-0051` §2's
idempotent retry — so `displayNameRemoved` is `false` by construction on every `200` from
`PUT /api/me/name`. **Never a second query and never a subquery in `RETURNING`** (`ADR-0053` §6).

`ProfileDtoFixtures.profileResponse` gains `displayNameRemoved: Boolean = false` as its **last**
parameter. The default belongs on the builder, never on the DTO.

## Out of scope

- `ProfileReads`'s port signature. It gains no method and no parameter (`ADR-0053` §6).
- `DuelSummaryResponse`, `RECENT_DUELS_SQL`, `DUEL_LINES` — **named prohibitions**, not omissions.
  That query already holds the *opponent's* `player` row as `p`, so the identical `EXISTS` pasted
  there publishes a takedown to a stranger (`ADR-0053` §4.2). `TASK-041020` asserts it is absent.
- `PROTOCOL_VERSION` and `docs/protocol-versions.md`. `ProfileResponse` is reachable from neither
  `ClientMessage` nor `ServerMessage`, so `ProtocolVersionLedgerTest`'s fingerprint is byte-identical
  (`ADR-0053` §5).
- `docs/protocol.md` — `TASK-041021`. Database-level assertions — `TASK-041019`.

## Tests

`ProfileDtosTest`. One assertion updated, two tests added.

The exact string below was computed from the spec, not read off a run: declaration order is
`playerId, coinBalance, displayName, displayNameRemoved`; `protocolJson` sets `encodeDefaults = true`
and leaves `explicitNulls` at its default `true`; and the new field has no default, so it is emitted
whatever `encodeDefaults` says.

| Test | Proves |
| --- | --- |
| `aProfileEncodesItsPlayerIdAndBalance` | The exact encoding becomes `{"playerId":"p-1","coinBalance":3,"displayName":null,"displayNameRemoved":false}`. This is the one existing assertion that moves; nothing else in the file is weakened, and no other assertion changes |
| `aRemovedNameIsCarriedOnTheProfile` | `profileResponse("p-1", 0, displayName = null, displayNameRemoved = true)` encodes to `{"playerId":"p-1","coinBalance":0,"displayName":null,"displayNameRemoved":true}` and decodes back equal. Fails against a field the encoder omits and against one the decoder cannot read |
| `aProfileWithoutTheFieldIsRefused` | `protocolJson.decodeFromString(ProfileResponse.serializer(), """{"playerId":"p-1","coinBalance":0,"displayName":null}""")` **throws**. This is the assertion that pins *no default value*: with `= false` on the DTO this decode succeeds, both tests above still pass, and the field silently disappears from the wire for every player whose answer is `false`. `ProfileDtosTest` already imports `assertThrows` |

## Acceptance criteria

- [ ] `ProfileDtosTest.aProfileEncodesItsPlayerIdAndBalance` passes against the exact string above
- [ ] `ProfileDtosTest.aRemovedNameIsCarriedOnTheProfile` passes
- [ ] `ProfileDtosTest.aProfileWithoutTheFieldIsRefused` passes
- [ ] `ProfileResponse.displayNameRemoved` is declared `Boolean` with **no** `=` in its declaration
- [ ] `PostgresProfileReads.profileOf`'s statement contains exactly one `?` and the substring
      `EXISTS (SELECT 1 FROM name_registry`, and contains no `JOIN`
- [ ] `PostgresProfileWrites` passes a literal `false` and issues no statement against
      `name_registry` other than `TASK-041003`'s `INSERT`
- [ ] `ProfileReads.kt` and `ProfileWrites.kt` are unmodified
- [ ] `retired_from` appears in exactly one file under `poker-server/src/main/kotlin`, and it is
      `PostgresProfileReads.kt`
- [ ] `ProtocolVersionLedgerTest` passes with no new claim and `docs/protocol-versions.md` is
      unmodified
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
