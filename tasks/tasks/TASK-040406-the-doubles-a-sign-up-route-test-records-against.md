---
schema: 2
id: TASK-040406
title: The doubles every sign-up route test records against
type: task
status: done
parent: STORY-0404
module: poker-server
estimate: S
tier: haiku
review: light
files_touched: 2
labels: [server, auth, http, test-fixture]
depends_on: [TASK-040405]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.http.AuthRouteDoublesTest'
  - ./gradlew :poker-server:check -PrequireDocker=true
---

## Goal

One recording `Credentials` and one fixed `ProfileReads` exist as shared test fixtures, so every
later ticket can assert *`create` was never called* as cheaply as it asserts a status code.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/test/kotlin/duels/poker/server/http/AuthRouteDoubles.kt` | create |
| `poker-server/src/test/kotlin/duels/poker/server/http/AuthRouteDoublesTest.kt` | create |
| `poker-server/src/test/kotlin/duels/poker/server/http/ProfileRouteTest.kt` | read — `FakeProfileReads` at the bottom, the shape this file generalises. It is `private` and cannot be reused, which is why this file exists |
| `poker-server/src/test/kotlin/duels/poker/server/protocol/http/ProfileDtoFixtures.kt` | read — `profileResponse(…)`, the builder these doubles hand back |

## Scope

- `internal class RecordingCredentials(private val createResult: CreateCredentialResult = CreateCredentialResult.Created, private val holds: Boolean = false) : Credentials`
  in package `duels.poker.server.http`, with:
  - `val createCalls: MutableList<CreateCall>` and
    `data class CreateCall(val playerId: PlayerId, val kind: CredentialKind, val identifier: String, val secret: PresentedSecret)`
    — the **whole** argument list, because the tickets that follow assert the identifier was folded
    and the player id came from the server, and a recorder that keeps only a count cannot say so.
  - `val holdsCalls: MutableList<Pair<PlayerId, CredentialKind>>`, so *the guard was not even
    consulted* is assertable.
  - `verify` throws `UnsupportedOperationException`. Sign-up never verifies, and a double that
    silently answers `null` would let a ticket pass while calling the wrong function.
- `internal class FixedProfileReads(private val profiles: Map<String, ProfileResponse>) : ProfileReads`,
  recording the device ids it was asked about in `val queried: MutableList<String>`, and answering
  `null` for anything not in the map. `recentDuelsOf` answers an empty list.
- Both are `internal`, live in test source only, and neither is referenced from `src/main`.

## Out of scope

- Any route, and any test of one. `TASK-040408` installs the first route that uses these.
- Touching `ProfileRouteTest`. Its private fakes stay where they are; this ticket does not refactor
  them, and doing so would put a 500-line file into a budget that has no room for it.
- A double for `PostgresCredentials`. The database tests use the real one against the container.

## Tests

`AuthRouteDoublesTest` — a fixture nobody checks is a fixture that lies.

| Test | Proves |
| --- | --- |
| `theRecorderStartsWithNothingRecorded` | a fresh `RecordingCredentials` has `createCalls` and `holdsCalls` both empty, so *never called* is a state that exists rather than one that is assumed |
| `theRecorderKeepsEveryArgumentItWasGiven` | after one `create(PlayerId("p-7"), CredentialKind.PASSWORD, "bob", PresentedSecret("hunter2222"))`, the single recorded call equals all four of those values. **The four values are pairwise distinct strings**, so a recorder that stores the wrong argument in the wrong slot cannot pass |
| `theRecorderAnswersTheResultItWasBuiltWith` | one double built with `Created` and one with `IdentifierTaken` answer differently. **Two inputs**, or the test cannot tell a returned value from a constant |
| `theRecorderAnswersWhetherThePlayerHoldsOne` | one double built with `holds = true` and one with the default answer `true` and `false`, and both record the `(playerId, kind)` pair they were asked |
| `theReadsDoubleAnswersOnlyForDeviceIdsItWasGiven` | a map holding `"alice"` answers a profile for `alice` and `null` for `"mallory"`, recording both device ids in `queried` |

## Acceptance criteria

- [ ] All five tests above pass
- [ ] `CreateCall` carries all four arguments and `theRecorderKeepsEveryArgumentItWasGiven` asserts
      all four against pairwise-distinct values
- [ ] `theRecorderAnswersTheResultItWasBuiltWith` and `theRecorderAnswersWhetherThePlayerHoldsOne`
      each build **two** doubles and assert two different answers
- [ ] `RecordingCredentials.verify` throws; no test in this file calls it expecting an answer
- [ ] Both classes are `internal` and live only under `src/test`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
