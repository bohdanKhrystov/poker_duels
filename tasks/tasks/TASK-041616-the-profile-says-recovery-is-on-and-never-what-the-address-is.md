---
schema: 2
id: TASK-041616
title: The profile says recovery is on, and never what the address is
type: task
status: ready
parent: STORY-0416
module: poker-server
estimate: S
tier: sonnet
review: standard
files_touched: 6
atomic:
  - ':poker-server:compileKotlin — a non-defaulted field on a public data class breaks its two main-source construction sites'
  - ':poker-server:compileTestKotlin — the same, for the fixture builder and the one test that bypasses it'
  - ':poker-server:test — ProfileDtosTest pins three golden JSON strings that gain a key'
labels: [server, http, api, security]
depends_on: [TASK-041615]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.protocol.http.ProfileDtosTest'
  - ./gradlew :poker-server:test --tests 'duels.poker.server.db.PostgresProfileReadsTest' -PrequireDocker=true
  - ./gradlew :poker-server:test --tests 'duels.poker.server.http.StandingsRouteTest'
  - ./gradlew :poker-server:test --tests 'duels.poker.server.db.PostgresProfileWritesTest' -PrequireDocker=true
  - ./gradlew :poker-server:ktlintCheck
  - ./gradlew :poker-server:detekt
---

## Goal

`GET /api/me` carries `hasRecoveryEmail: Boolean` — enough for a client to say *recovery is on*, and
never enough to display the address.

## Files

Every row carries the gate that forbids splitting it out.

| File | Action | Why it cannot be fewer |
| --- | --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/protocol/http/ProfileDtos.kt` | modify | The field itself. Everything below exists because this line does |
| `poker-server/src/main/kotlin/duels/poker/server/db/PostgresProfileReads.kt` | modify | `:poker-server:compileKotlin` — *No value passed for parameter 'hasRecoveryEmail'* at line 40. Also carries the `EXISTS` that computes it |
| `poker-server/src/main/kotlin/duels/poker/server/db/PostgresProfileWrites.kt` | modify | `:poker-server:compileKotlin` — the same error at line 118. Takes the literal `false`, since a `NameSet` result describes a write path that cannot change an address |
| `poker-server/src/test/kotlin/duels/poker/server/protocol/http/ProfileDtoFixtures.kt` | modify | `:poker-server:compileTestKotlin` — `profileResponse(...)` gains a defaulted parameter. This builder exists to keep the next field a one-line change, and it is doing its job |
| `poker-server/src/test/kotlin/duels/poker/server/http/StandingsRouteTest.kt` | modify | `:poker-server:compileTestKotlin` — two `ProfileResponse(...)` calls bypass the builder above |
| `poker-server/src/test/kotlin/duels/poker/server/protocol/http/ProfileDtosTest.kt` | modify | `:poker-server:test` — three golden JSON literals gain `,"hasRecoveryEmail":false`. Also holds this ticket's new assertions |

Read, and do not edit:
`docs/adr/ADR-0053-the-profile-says-the-name-was-removed.md` — the correlated-`EXISTS`-never-a-
`LEFT JOIN` argument and the `encodeDefaults` argument, both of which apply here unchanged;
`docs/adr/ADR-0031-an-optional-verified-recovery-email.md` §6.3.

## Scope

- `ProfileResponse` gains `val hasRecoveryEmail: Boolean` as the **last** declared property, with
  **no default value** — `ContentNegotiation`'s `Json` has `encodeDefaults = false` while
  `protocolJson` has it `true`, so a defaulted field is present in every test's JSON and absent from
  the wire for the ~100% of players whose answer is `false`. `ADR-0053` records this trap; do not
  rediscover it.
- `PostgresProfileReads`' `PROFILE_OF_SQL` gains **one correlated `EXISTS`**:
  `EXISTS (SELECT 1 FROM recovery_email r WHERE r.player_id = p.id)`, correlated to `p.id` and not
  to a second bind parameter. One round trip. **Never a `LEFT JOIN`** — `recovery_email` is
  `player_id`-keyed so a join returns at most one row today, but `ADR-0053`'s rule is the house
  rule and a semijoin is what the answer is.
- `PostgresProfileWrites.toProfile()` takes the literal `false`, beside the `false` already there
  for `displayNameRemoved`: a name write neither reads nor changes an address.
- `ProfileDtoFixtures.profileResponse` gains `hasRecoveryEmail: Boolean = false`.
- **And nothing more.** `ADR-0031` §6.3: *"`ProfileResponse` gains `hasRecoveryEmail: Boolean` and
  nothing more — the client can say recovery is on and can never display the address."* No
  `recoveryEmail`, no masked form, no `verifiedAt`.

## Out of scope

- `docs/protocol.md` — `TASK-041617`. It is not in the *Files* table because **no gate names it**:
  `HttpEndpointDocumentationTest` checks documented ⇒ exists, not the reverse, so an undocumented
  field fails nothing. Adding it here would be a row with no gate, which `ADR-0069` forbids.
- `DuelSummaryResponse`. It carries the **opponent's** row, and the same `EXISTS` pasted into
  `RECENT_DUELS_SQL` publishes a stranger's recovery status. It gains nothing.
- The client. `web-client/src/profile/profile.ts` narrows the body to the three fields it uses and
  ignores the rest — confirmed by running the `client` CI job against this change, which passed
  untouched. `STORY-0417` decides what a screen does with the field.
- `PROTOCOL_VERSION`. `ProfileResponse` is reachable from neither `ClientMessage` nor
  `ServerMessage`, so the ledger fingerprint does not move.

## Tests

`ProfileDtosTest` (existing file, new methods)

| Test | Proves |
| --- | --- |
| `aProfileWithRecoveryOnSaysSo` | `profileResponse("p-1", 0, hasRecoveryEmail = true)` encodes to a golden literal ending `,"hasRecoveryEmail":true}` |
| `aProfileWithNoRecoveryEmailSaysSo` | The same with `false`. Two values, so a hard-coded constant fails one |
| `theProfileNeverCarriesAnAddress` | `Json.encodeToString(profileResponse(...))` for a profile with recovery on contains no `"@"` and no key whose name contains `address` or `email` other than `hasRecoveryEmail` |

`PostgresProfileReadsTest` (existing file, new methods)

| Test | Proves |
| --- | --- |
| `theProfileReadsTrueForAPlayerWithAVerifiedAddress` | Two players in **one database**, one verified and one not: the first reads `true`, the second `false`. The uncorrelated `EXISTS (SELECT 1 FROM recovery_email)` makes every caller read `true` once anybody is verified, and passes two tests that each hold one fixture |
| `aPendingAddressIsNotARecoveryEmail` | A player with a pending claim and no verified row reads `false` — the same answer as a player who never claimed, asserted against both in one test |

## Acceptance criteria

- [ ] `ProfileDtosTest.aProfileWithRecoveryOnSaysSo` passes
- [ ] `ProfileDtosTest.aProfileWithNoRecoveryEmailSaysSo` passes
- [ ] `ProfileDtosTest.theProfileNeverCarriesAnAddress` passes
- [ ] `PostgresProfileReadsTest.theProfileReadsTrueForAPlayerWithAVerifiedAddress` passes
- [ ] `PostgresProfileReadsTest.aPendingAddressIsNotARecoveryEmail` passes
- [ ] The three pre-existing `ProfileDtosTest` golden strings gain `,"hasRecoveryEmail":false` and
      **nothing else changes in that file's existing assertions** — no assertion is weakened, no
      expected value other than those three literals moves
- [ ] `theProfileReadsTrueForAPlayerWithAVerifiedAddress` holds **two players in one database**
- [ ] `PROFILE_OF_SQL` contains `EXISTS` and does not contain `LEFT JOIN recovery_email`
- [ ] `RECENT_DUELS_SQL` is byte-unchanged
- [ ] `ProfileResponse.hasRecoveryEmail` has no default value and is declared last
- [ ] The diff touches exactly the six files in the *Files* table
- [ ] Every command in `verify:` exits 0

## Proof

1. Change the `EXISTS` to the uncorrelated `EXISTS (SELECT 1 FROM recovery_email)`.
   **`theProfileReadsTrueForAPlayerWithAVerifiedAddress` reddens alone**, *expected false, got true*
   for the unverified player. `aPendingAddressIsNotARecoveryEmail` **also reddens** if its fixture
   shares a database with a verified player — it must not, so write it against a database holding
   no verified row and confirm it stays green here. That contrast is the whole reason the first test
   holds two players.
2. Change the `EXISTS` to read `email_verification` instead.
   **`aPendingAddressIsNotARecoveryEmail` reddens alone**, *expected false, got true*.
   `theProfileReadsTrueForAPlayerWithAVerifiedAddress` verifies its player, which deletes the
   pending row, so its `true` becomes `false` — **it reddens too**. Two, and if only one does, the
   verified fixture is not going through `verifyPending`. Revert.
3. Give the field a default of `false` in `ProfileResponse`.
   **Nothing reddens.** Record it: `protocolJson` sets `encodeDefaults = true`, so the golden
   strings are unaffected, and the wire divergence `ADR-0053` names is invisible to every test in
   this repository. The no-default rule is held by the criterion above and by review, not by a gate,
   and saying so is better than implying otherwise. Revert.
4. Hard-code `true` in `PostgresProfileReads`' construction, ignoring the column.
   **Both `PostgresProfileReadsTest` methods redden**, and `ProfileDtosTest` is unaffected because
   it never touches the database. Revert.
5. Add `val recoveryEmail: String?` to `ProfileResponse` and populate it from a second column.
   **`theProfileNeverCarriesAnAddress` reddens alone** on the `"@"` assertion, and the three
   pre-existing golden strings redden on their literals. This is the mutation §6.3 exists to
   prevent, and the golden strings are half of what catches it. Revert.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.

## Notes

**Sized by probe, not by memory.** `hasRecoveryEmail: Boolean` was added to `ProfileResponse` with
no default and `./gradlew check -PrequireDocker=true` was run to completion four times, applying the
minimal propagation each red run named, until it exited 0; then `npm ci && npm run check && npm run
build` was run in `web-client` and exited 0 untouched. The run order was the prefix `ADR-0070`
warns about: `compileKotlin` named two files, `compileTestKotlin` then named two more that the first
run could not see, and `:poker-server:test` then named a sixth — 1565 tests completed, 3 failed, all
three in `ProfileDtosTest`. Six is the count at green, and it is a fact about this change and no
other.
