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

- **Any test asserting what the `EXISTS` answers against a real database — `TASK-041641`.** It is
  not in the *Files* table for the same reason `docs/protocol.md` is not: **no gate names it.**
  `PostgresProfileReadsTest` constructs no `ProfileResponse` and reads no boolean it does not
  already read, so it compiles and passes untouched through every one of this ticket's edits — the
  probe recorded in `## Notes` ran the full gate set to green without it. A `## Files` row needs a gate
  (`ADR-0069` §1), and a fourth `atomic:` item would have to name a merged gate that fails on the
  smaller commit; there is none, and the green probe run is the evidence. So the assertion is a
  separate ticket rather than a fabricated coupling.
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

**No test in `PostgresProfileReadsTest` is this ticket's.** That class runs in `verify:` as a
regression gate on the `PROFILE_OF_SQL` edit — the existing reads must survive a new column — and
gains no method here. What the `EXISTS` actually answers, for whom, is `TASK-041641`.

## Acceptance criteria

- [ ] `ProfileDtosTest.aProfileWithRecoveryOnSaysSo` passes
- [ ] `ProfileDtosTest.aProfileWithNoRecoveryEmailSaysSo` passes
- [ ] `ProfileDtosTest.theProfileNeverCarriesAnAddress` passes
- [ ] The three pre-existing `ProfileDtosTest` golden strings gain `,"hasRecoveryEmail":false` and
      **nothing else changes in that file's existing assertions** — no assertion is weakened, no
      expected value other than those three literals moves
- [ ] `PostgresProfileReadsTest` is **not edited**: every one of its tests passes untouched, and
      `git status` shows no change to that file. Its two new methods are `TASK-041641`
- [ ] `PROFILE_OF_SQL` contains `EXISTS` and does not contain `LEFT JOIN recovery_email`
- [ ] `RECENT_DUELS_SQL` is byte-unchanged
- [ ] `ProfileResponse.hasRecoveryEmail` has no default value and is declared last
- [ ] The diff touches exactly the six files in the *Files* table
- [ ] Every command in `verify:` exits 0

## Proof

1. In `ProfileDtoFixtures.profileResponse`, ignore the `hasRecoveryEmail` parameter and pass the
   literal `false` down to `ProfileResponse`.
   **`aProfileWithRecoveryOnSaysSo` reddens alone**, on a golden literal ending
   `,"hasRecoveryEmail":true}` against an encoder now writing `false`.
   `aProfileWithNoRecoveryEmailSaysSo` and the three pre-existing goldens **stay green**, because
   `false` is what they already expect. Predict both halves: if the second test reddens too, the
   pair is not passing two different values through one builder and one of them is decorative.
   Revert.
2. Add `val recoveryEmail: String?` to `ProfileResponse` and populate it from a second column.
   **`theProfileNeverCarriesAnAddress` reddens alone** on the `"@"` assertion, and the three
   pre-existing golden strings redden on their literals. This is the mutation §6.3 exists to
   prevent, and the golden strings are half of what catches it. Revert.
3. Give the field a default of `false` in `ProfileResponse`.
   **Nothing reddens.** Record it: `protocolJson` sets `encodeDefaults = true`, so the golden
   strings are unaffected, and the wire divergence `ADR-0053` names is invisible to every test in
   this repository. The no-default rule is held by the criterion above and by review, not by a gate,
   and saying so is better than implying otherwise. Revert.
4. **The database layer, four ways, and none of them reddens anything this ticket ships.** Wire
   `PostgresProfileReads`' construction to a constant `false`; then to a constant `true`; then
   change the `EXISTS` to the uncorrelated `EXISTS (SELECT 1 FROM recovery_email)`; then point it at
   `email_verification` instead of `recovery_email`. **All four build and run fully green**,
   including `PostgresProfileReadsTest`, because nothing in this repository reads
   `ProfileResponse.hasRecoveryEmail` back out of a real database. Run them anyway and record the
   four green runs in the PR body: this is not a gap to close here — closing it needs a new test
   method, which `ADR-0070` §4 condition 3 puts outside any coder's licence — it is
   **`TASK-041641`**, which depends on this ticket and whose whole content is those two methods.
   Revert each before applying the next.

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

**Corrected on 2026-08-25, after this ticket had run once.** As written it named two
`PostgresProfileReadsTest` methods in *Tests* and in two acceptance criteria while its `atomic:`
*Files* table — which is the whole change — did not list that file, so the criteria required a file
the ticket forbade touching. The coder refused to widen and was right to: `ADR-0070` §4 condition 3
excludes *"adds a test"* from the propagation exception by name. Both methods moved to
`TASK-041641`. **The count stays six and `atomic:` stays three items** — the probe's green run *is*
the proof that no gate couples those tests to this change, and a fourth item invented to keep them
here would assert a coupling the probe had already disproved. The gap that discovery opened is real
and is written down in `## Proof` §4 rather than papered over: it stands open from this ticket's
merge until `TASK-041641`'s.
