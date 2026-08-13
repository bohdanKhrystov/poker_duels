---
schema: 2
id: TASK-021101
title: Declare the profile and duel-summary response types
type: task
status: ready
parent: STORY-0211
module: poker-server
estimate: S
tier: haiku
review: light
files_touched: 2
labels: [server, http, protocol, coins]
depends_on: []
verify:
  - ./gradlew :poker-server:test --tests '*ProfileDtosTest'
  - ./gradlew :poker-server:check -PrequireDocker=true
---

## Goal

The three kotlinx.serialization response types the read path answers with — a profile with its
balance, one duel summary, and a list of them — exist and round-trip through JSON, including a
negative balance.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/protocol/http/ProfileDtos.kt` | create |
| `poker-server/src/test/kotlin/duels/poker/server/protocol/http/ProfileDtosTest.kt` | create |

Read, do not modify:
`poker-server/src/main/kotlin/duels/poker/server/protocol/Protocol.kt` (`protocolJson` and why each
of its flags is set),
`poker-server/src/main/kotlin/duels/poker/server/protocol/ServerMessage.kt` (the house DTO and KDoc
style to copy),
`docs/adr/ADR-0014-duel-coin-economy.md`.

## Scope

- New package `duels.poker.server.protocol.http` — the protocol package family, one level down, so
  these HTTP types never join the `ClientMessage`/`ServerMessage` sealed hierarchies. They are not
  socket frames and must not appear in `ProtocolCodec` or in the discriminated union that
  `ProtocolDocumentationTest` walks.
- One file, four public declarations, each with KDoc:

  ```kotlin
  @Serializable
  public data class ProfileResponse(val playerId: String, val coinBalance: Int)

  @Serializable
  public enum class DuelOutcomeLabel { WON, LOST, DREW }

  @Serializable
  public data class DuelSummaryResponse(
      val duelId: String,
      val opponentPlayerId: String,
      val outcome: DuelOutcomeLabel,
      val coinDelta: Int,
      val handsPlayed: Int?,
      val finishedAt: String,
  )

  @Serializable
  public data class RecentDuelsResponse(val duels: List<DuelSummaryResponse>)
  ```

- **No property declares a default value.** `Application.module()` installs
  `ContentNegotiation { json() }`, whose `Json` has `encodeDefaults = false`, while `protocolJson`
  sets it to `true`; a defaulted property would therefore be present in the tests' JSON and absent
  on the wire. Say that in a `why` comment.
- `coinBalance` and `coinDelta` are plain signed `Int`s. No `UInt`, no `coerceAtLeast`, no
  `maxOf(0, …)`, no `abs`: a balance is `wins − losses` and `−3` is a real answer, not an error
  (`ADR-0014`). KDoc on `coinBalance` says so.
- `finishedAt` is the ISO-8601 instant as text (`2026-08-13T10:00:00Z`), not a `java.time.Instant`:
  kotlinx.serialization has no built-in serializer for `Instant`, and a hand-written one would be a
  type this story does not need. KDoc says the text is UTC and produced by `Instant.toString()`.
- `handsPlayed` is nullable and KDoc'd as **null until `DEC-014` is answered** — the `duel` table
  has no `hands_played` column, `DEC-014` decides whether it gains one, and a nullable field is the
  shape that lets the answer be additive either way. No ticket in this story fills it.
- `opponentPlayerId` is the opponent's `player.id`, never their `device_id`: a device id is the
  sole authentication token in v0.1 (see `DeviceIdSource`'s KDoc), so handing one to the other
  player would be handing over their account. KDoc says exactly that. `DEC-016` asks whether a
  human-readable name is added later; adding one would be a new field, not a change to this one.

## Out of scope

- Any route, any SQL, any `DataSource` — `TASK-021104` onwards.
- Deriving `outcome` from `coinDelta` — `TASK-021102` owns that function.
- Adding these types to `ClientMessage`, `ServerMessage`, `ProtocolCodec` or `docs/protocol.md`.
  The document is `TASK-021112`.

## Tests

`ProfileDtosTest`, JUnit 5, package `duels.poker.server.protocol.http`. Encode and decode with the
house `protocolJson` instance. No coroutines, no Ktor, no database.

| Test | Proves |
| --- | --- |
| `aProfileEncodesItsPlayerIdAndBalance` | `protocolJson.encodeToString(ProfileResponse("p-1", 3))` is `{"playerId":"p-1","coinBalance":3}` |
| `aNegativeBalanceSurvivesTheRoundTrip` | `ProfileResponse("p-1", -3)` encodes with `"coinBalance":-3` and decodes back to `-3` — unclamped, per `ADR-0014` |
| `aDuelSummaryRoundTripsEveryField` | a summary with `outcome = WON`, `coinDelta = 1`, `handsPlayed = null` decodes back equal to the original |
| `aNullHandsPlayedIsWrittenAsNull` | that summary's JSON contains `"handsPlayed":null` |
| `aDrawnSummaryCarriesDrewAndAZeroDelta` | a summary with `outcome = DREW`, `coinDelta = 0` round-trips with both values intact |
| `anEmptyRecentDuelsListEncodesAsAnEmptyArray` | `protocolJson.encodeToString(RecentDuelsResponse(emptyList()))` is `{"duels":[]}` |

## Acceptance criteria

- [ ] `ProfileDtosTest.aProfileEncodesItsPlayerIdAndBalance` passes
- [ ] `ProfileDtosTest.aNegativeBalanceSurvivesTheRoundTrip` passes
- [ ] `ProfileDtosTest.aDuelSummaryRoundTripsEveryField` passes
- [ ] `ProfileDtosTest.aNullHandsPlayedIsWrittenAsNull` passes
- [ ] `ProfileDtosTest.aDrawnSummaryCarriesDrewAndAZeroDelta` passes
- [ ] `ProfileDtosTest.anEmptyRecentDuelsListEncodesAsAnEmptyArray` passes
- [ ] No property in `ProfileDtos.kt` declares a default value (no `=` in any constructor parameter)
- [ ] `ProfileDtos.kt` contains no `coerceAtLeast`, `coerceIn`, `maxOf`, `abs`, `absoluteValue` or
      `UInt`
- [ ] `ProfileDtos.kt` names no `ServerMessage`, `ClientMessage`, `DataSource`, `Connection` or SQL
      string
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
