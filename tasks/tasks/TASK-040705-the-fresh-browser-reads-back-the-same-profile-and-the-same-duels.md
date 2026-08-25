---
schema: 2
id: TASK-040705
title: The fresh browser reads back the same profile and the same duels
type: task
status: done
parent: STORY-0407
module: poker-server
estimate: S
tier: haiku
review: standard
files_touched: 1
labels: [server, e2e, auth, profile]
depends_on: [TASK-040704]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.e2e.RecoveryOnAFreshBrowserTest' -PrequireDocker=true
  - ./gradlew :poker-server:ktlintCheck
  - ./gradlew :poker-server:detekt
---

## Goal

`GET /api/me` and `GET /api/me/duels`, called from the fresh browser under its session token, answer
with exactly what the original device reads — the same player, the same coin, the same name, the
same duel.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/test/kotlin/duels/poker/server/e2e/RecoveryOnAFreshBrowserTest.kt` | modify |

Read, and do not edit:
`poker-server/src/main/kotlin/duels/poker/server/protocol/http/ProfileDtos.kt`,
`docs/adr/ADR-0030-a-claim-adds-a-credential-and-moves-nothing.md` §4.

## Scope

- One private `HttpClient.duelsOf(deviceId: String, token: String? = null): RecentDuelsResponse`
  helper, shaped exactly like `profileOf`: `GET /api/me/duels`, `X-Device-Id` always,
  `Authorization: Bearer $token` only when `token` is non-null, `200` asserted, decoded with
  `protocolJson`. No query string — the endpoint's defaults are what a client sends.
- `originalDuels`, read for the winner's device at the same moment `originalProfile` is read (right
  after the sign-up), and recorded on `RecoveryRecord`.
- Two steps appended to `runRecovery()` after the fresh browser's handshake:
  `recoveredProfile = client.profileOf(FRESH_DEVICE, sessionToken)` and
  `recoveredDuels = client.duelsOf(FRESH_DEVICE, sessionToken)`. **Both carry the fresh device id and
  the token together**, exactly as a real browser would, and exactly as the handshake does.
- `dataSource.assertCoinInvariantHolds(...)` once after the two reads, with a new distinct step
  string. `runRecovery()` then holds seven calls in total.
- Three new `RecoveryRecord` fields with `@property` lines: `originalDuels`, `recoveredProfile`,
  `recoveredDuels`.
- **The profile comparison is whole-value, never field by field**: `assertEquals(originalProfile,
  recoveredProfile)`. `ProfileResponse` is a `data class`, so this covers `playerId`, `coinBalance`,
  `displayName`, `displayNameRemoved` and `deviceRouteLive` at once — and covers a sixth field the
  day somebody adds one, which a hand-written list of four `assertEquals` calls would not.

## Out of scope

- Any snapshot of `player` or `device_binding` — `TASK-040706`, same file.
- The duel-list paging, filters and cursor. `STORY-0408` and `STORY-0409` own those; this ticket
  calls the endpoint with no query string and compares two answers.
- Asserting `deviceRouteLive` separately. It is one of the five fields the whole-value comparison
  already covers, and singling it out would weaken, not strengthen, that comparison.
- Any file under `poker-server/src/main`.

## Tests

`RecoveryOnAFreshBrowserTest`

| Test | Proves |
| --- | --- |
| `theFreshBrowserReadsTheSameProfile` | `recoveredProfile` equals `originalProfile`, whole value. `ADR-0030` §4: the read path follows the resolved player, not the calling device |
| `theFreshBrowserReadsTheSameDuels` | `recoveredDuels` equals `originalDuels`, **and** `originalDuels.duels` has exactly one entry. The size assertion is a count and not `isNotEmpty()`, and it is what stops two empty lists from satisfying the equality for free |
| `aDifferentPlayersProfileDoesNotCompareEqual` | `originalProfile` does **not** equal `loserProfile`. The control for the first test: two `ProfileResponse` values read through this fixture are not equal by default, so the equality above is a fact about recovery rather than about the type |

## Acceptance criteria

- [ ] `RecoveryOnAFreshBrowserTest.theFreshBrowserReadsTheSameProfile` passes
- [ ] `RecoveryOnAFreshBrowserTest.theFreshBrowserReadsTheSameDuels` passes
- [ ] `RecoveryOnAFreshBrowserTest.aDifferentPlayersProfileDoesNotCompareEqual` passes
- [ ] `theFreshBrowserReadsTheSameProfile` compares two whole `ProfileResponse` values in one
      `assertEquals`, and asserts no individual field
- [ ] `theFreshBrowserReadsTheSameDuels` asserts `1` for `originalDuels.duels.size`, and that
      assertion lives in the test method — **not** inside `runRecovery()` or inside `duelsOf`
- [ ] The two reads this ticket adds — `profileOf` and `duelsOf` for the fresh browser — each set
      `X-Device-Id` to `FRESH_DEVICE` **and** `Authorization: Bearer` with the session token
- [ ] `runRecovery()` contains exactly seven calls to `assertCoinInvariantHolds`, with seven
      different step strings
- [ ] Every test method added by `TASK-040702`, `TASK-040703` and `TASK-040704` still passes with its
      assertions unchanged
- [ ] The diff touches exactly one file, and it is the one in the *Files* table
- [ ] Every command in `verify:` exits 0

## Proof

In `poker-server/src/main/kotlin/duels/poker/server/http/ProfileRoutes.kt`, make `GET /api/me` report
`deviceRouteLive` for the **calling device** rather than for the resolved player — the plausible
misreading of that field's own KDoc, and the exact defect class this ticket exists to catch:

```kotlin
val profile = call.resolvedPlayerOrNull(identities)?.let { reads.profileOf(it) }
    ?.copy(deviceRouteLive = identities.resolve(null, call.deviceIdOrNull()) is Identity.Device)
```

`Identity` and `deviceIdOrNull` are already in scope in that file. **`theFreshBrowserReadsTheSameProfile`
reddens, and it is the only method that does.** Trace it: the original device presents a known device
id, resolves to `Identity.Device`, and still reads `true`; the fresh browser presents
`e2e-fresh-browser`, which resolves to `Identity.UnknownDevice`, and now reads `false` — so the two
`ProfileResponse` values differ in one field. `aDifferentPlayersProfileDoesNotCompareEqual` compares
two device-keyed reads, both still `true`, and they were already unequal by `playerId`, so it stays
green; the duels route is untouched; and no row is written, so all seven invariant calls pass.
Revert.

A second mutation, for the duels half. In the same file, inside `respondWithDuels`, ask for a player
nobody is: `reads.recentDuelsOf(PlayerId(UUID.randomUUID().toString()), limit + 1, cursor, filter)`.
`UUID` and `PlayerId` are already imported there. **`theFreshBrowserReadsTheSameDuels` reddens alone**
— and it reddens on the *size* assertion, not the equality one, because both calls now answer an
empty list and two empty lists compare equal. That is the whole reason the count is there. Revert.

**What this pair cannot see, named rather than left for a reader to find.** Two reads compared for
equality are blind to any defect that affects both reads identically. Replace `p.coin_balance` with
`0` in `PostgresProfileReads.PROFILE_OF_SQL` and all three methods above stay green, because both
sides read `0`; the method that reddens is `TASK-040702`'s
`theDuelPaidExactlyOneCoinEachWay`, which asserts two different expected values against two different
players. The two tickets cover different halves and neither is redundant.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.

## Notes

**`ADR-0030` §4 is the rule being proved.** `profileOf` takes a `PlayerId`, not a `DeviceId`,
precisely so that a session whose player has a different device — or, here, a caller whose device has
no player at all — reads the account and not the caller. Before that change a device-keyed
`GET /api/me` would have answered `401` here, and the balance on screen would have been nobody's.

**The vacuity this ticket could have had, and the experiment that ruled it out.** Both reads present
`FRESH_DEVICE`'s device id *and* the session token, which is what a real client does (`ADR-0030` §8)
but also the shape in which a "reads back the same profile" test proves nothing: if the fresh
browser's earlier sign-in and handshake had left a `device_binding` row linking `FRESH_DEVICE` to the
recovered player, a device-routed `GET /api/me` would return the same profile and the assertion could
not tell which credential selected it. The reviewer settled it by experiment rather than by reading —
dropping the `Authorization` header while keeping the device id makes
`theFreshBrowserReadsTheSameProfile` fail. No binding exists at recovery time, so the token is doing
the work. Reading the code alone would not have established this; the two ADRs say the session
outranks the device, not that the device resolves to nothing.

**The coder's report answered two of the five questions it was asked**, omitting whether the loser's
profile could have satisfied the comparison, whether anything claimed to prove no second profile was
created, and whether Postgres genuinely started. All three were sound when the reviewer checked them,
but they were checked by the reviewer, not established by the coder. Recorded because an incomplete
report that happens to be correct is indistinguishable at a glance from one that is not.
