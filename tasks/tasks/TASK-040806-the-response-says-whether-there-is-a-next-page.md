---
schema: 2
id: TASK-040806
title: The response says whether there is a next page, as null and not as absent
type: task
status: ready
parent: STORY-0408
module: poker-server
estimate: XS
tier: haiku
review: light
files_touched: 3
labels: [server, http, protocol, history, paging]
depends_on: [TASK-040805]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.protocol.http.ProfileDtosTest'
  - ./gradlew :poker-server:check -PrequireDocker=true
---

## Goal

`RecentDuelsResponse` carries the next page's cursor, and it is on the wire as `null` on the last
page rather than missing.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/protocol/http/ProfileDtos.kt` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/protocol/http/ProfileDtosTest.kt` | modify |
| `poker-server/src/main/kotlin/duels/poker/server/http/ProfileRoutes.kt` | modify — the one production construction site |
| `docs/adr/ADR-0053-the-profile-says-the-name-was-removed.md` | read — the `encodeDefaults` trap, stated for `ProfileResponse` and identical here |

## Scope

- `public data class RecentDuelsResponse(val duels: List<DuelSummaryResponse>, val nextCursor: String?)`,
  with `nextCursor` **last** in declaration order and **with no default value**.
- The no-default rule is not taste. `Application.module()` installs `ContentNegotiation { json() }`,
  whose `Json` has `encodeDefaults = false`, while `protocolJson` has it `true` — so a defaulted
  property is present in every test's JSON and absent from the wire, for the last page of every
  request that ever reaches it. The file's own KDoc on `duels` already records this; extend it to
  the new property.
- `ProfileRoutes.kt` gains exactly `nextCursor = null` at its single construction site. The
  endpoint does not page yet — `TASK-040809` computes the value.
- **`PROTOCOL_VERSION` does not move and `protocol.gen.ts` is not regenerated.** The ledger
  fingerprint hashes `protocolDeclarations()`, rooted at `ClientMessage`/`ServerMessage` only, and
  `RecentDuelsResponse` is reachable from neither. No ledger row, no version line, no bump.

## This ticket owns one existing assertion

`ProfileDtosTest.anEmptyRecentDuelsListEncodesAsAnEmptyArray` asserts the exact string
`{"duels":[]}`. It becomes `{"duels":[],"nextCursor":null}` — the new property is declared last, so
that is the order `protocolJson` emits. It stays an exact-string equality; nothing about it is
weakened, and no other assertion in that file changes.

## Out of scope

- Computing the cursor, the `+ 1` probe, the route's paging — `TASK-040809`.
- `docs/protocol.md` — `TASK-040811`.
- The client. `web-client/src/profile/recent-duels.ts` reads `duels` and ignores every other key, so
  it needs nothing; the history screen is `STORY-0413`.

## Tests

`ProfileDtosTest`

| Test | Proves |
| --- | --- |
| `anEmptyRecentDuelsListEncodesAsAnEmptyArray` (**modified**) | the encoded string is exactly `{"duels":[],"nextCursor":null}` — the field is present and null, not omitted |
| `aNextCursorEncodesAsTheStringItWasGiven` (**new**) | `RecentDuelsResponse(emptyList(), "abc")` encodes containing `"nextCursor":"abc"`, so the property is carried and not swallowed |

## Acceptance criteria

- [ ] `ProfileDtosTest.anEmptyRecentDuelsListEncodesAsAnEmptyArray` passes against the exact string
      `{"duels":[],"nextCursor":null}`
- [ ] `ProfileDtosTest.aNextCursorEncodesAsTheStringItWasGiven` passes
- [ ] `RecentDuelsResponse.nextCursor` is declared `String?` with no default value
- [ ] Every other test in `ProfileDtosTest` passes with its assertions unchanged
- [ ] `./gradlew :poker-server:verifyProtocolTypes` still passes and `protocol.gen.ts` is unchanged
      in the diff
- [ ] `docs/protocol-versions.md` gains no row and `PROTOCOL_VERSION` is unchanged
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
