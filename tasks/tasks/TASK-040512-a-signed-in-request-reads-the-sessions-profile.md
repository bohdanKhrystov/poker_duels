---
schema: 2
id: TASK-040512
title: A signed-in request reads the session's profile, and the device beside it is ignored
type: task
status: backlog
parent: STORY-0405
module: poker-server
estimate: S
tier: sonnet
review: standard
files_touched: 2
labels: [server, http, identity, auth]
depends_on: [TASK-040511]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.http.ProfileRouteTest'
  - ./gradlew :poker-server:ktlintCheck
---

## Goal

The precedence rule is proven where a player would notice it: `GET /api/me` under a valid session
answers the session's profile even when the `X-Device-Id` beside it owns a different one, and a
session that will not do is a `401` rather than a quiet downgrade.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/test/kotlin/duels/poker/server/http/ProfileReadsDoubles.kt` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/http/ProfileRouteTest.kt` | modify |

Read `docs/adr/ADR-0027-the-session-outranks-the-device-id.md` §4 and
`docs/adr/ADR-0030-a-claim-adds-a-credential-and-moves-nothing.md` §2. Nothing else.

## Scope

- `ProfileReadsDoubles.kt` gains `internal fun identitiesFor(profiles, sessions: AuthSessions)` —
  the same `FixedDirectory` as before, with a caller-supplied `AuthSessions` — and a tiny
  `FixedAuthSessions(private val tokens: Map<String, String>)` whose `playerOf` answers from a map
  and whose `issue` throws. Nothing in `main` changes.
- Four tests added to `ProfileRouteTest`. **No existing test is edited.**
- **The fixture has two profiles and they are not interchangeable.** Device `d-anon` owns
  `p-anon` with a coin balance of `-1`; the token `t-signed` names `p-signed` with a balance of
  `7`. Every assertion below names a balance as well as a player id, because a response is built
  from one row and a test that checks only the id cannot tell *the session's row* from *the row the
  fixture happened to list first*.
- No production file changes. If one has to, this ticket is wrong and `TASK-040511` was incomplete.

## Out of scope

- Sign-in itself, which is what would issue `t-signed` for real — `TASK-040514`. Here the token is
  a fixture.
- The socket — `TASK-040518`.
- `GET /api/me/duels` and `GET /api/standings` under a session. The resolver is shared, so the rule
  is proven once; if a route ever resolves identity its own way, that is the defect and this test
  will not see it — which is why `TASK-040511` put the helper in one place.

## Tests

`ProfileRouteTest`

| Test | Proves |
| --- | --- |
| `aSessionOutranksTheDeviceBesideIt` | `Authorization: Bearer t-signed` **and** `X-Device-Id: d-anon` together answer `p-signed` with balance `7` — not `p-anon`, not `-1`. The two-profile fixture is the whole test: with one profile both answers are the same string |
| `theSameDeviceAloneStillAnswersItsOwnProfile` | `X-Device-Id: d-anon` with no `Authorization` answers `p-anon` with balance `-1` — the control that stops the first test passing for a route that always answers the session fixture |
| `anUnknownSessionIsRefusedEvenWithAGoodDevice` | `Authorization: Bearer nonsense` with `X-Device-Id: d-anon` answers `401` and an empty body — **never** `p-anon`. This is the downgrade `ADR-0027` §4 forbids, and it needs a *usable* device id present to be a real test |
| `aBlankBearerIsTreatedAsNoSessionAtAll` | `Authorization: Bearer ` (nothing after the space) with `X-Device-Id: d-anon` answers `p-anon`, matching how a blank `X-Device-Id` is already treated as absent rather than as an invalid credential |

## Acceptance criteria

- [ ] `ProfileRouteTest.aSessionOutranksTheDeviceBesideIt` passes
- [ ] `ProfileRouteTest.theSameDeviceAloneStillAnswersItsOwnProfile` passes
- [ ] `ProfileRouteTest.anUnknownSessionIsRefusedEvenWithAGoodDevice` passes
- [ ] `ProfileRouteTest.aBlankBearerIsTreatedAsNoSessionAtAll` passes
- [ ] The two profiles in the new fixture differ in **both** `playerId` and `coinBalance`
- [ ] `git diff --name-only` names exactly two files, both under `src/test`
- [ ] Every command in `verify:` exits 0

## Proof

Make `resolvedPlayerOrNull` fall through from `Refused` to the device branch and
`anUnknownSessionIsRefusedEvenWithAGoodDevice` goes red alone. Make it prefer the device over the
session and `aSessionOutranksTheDeviceBesideIt` goes red while the other three stay green.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.
