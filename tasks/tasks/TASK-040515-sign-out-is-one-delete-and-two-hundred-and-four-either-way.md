---
schema: 2
id: TASK-040515
title: "POST /api/auth/sign-out: one delete, 204 either way, and no socket closes"
type: task
status: ready
parent: STORY-0405
module: poker-server
estimate: XS
tier: haiku
review: standard
files_touched: 2
labels: [server, http, auth, session]
depends_on: [TASK-040514]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.http.AuthRouteTest'
  - ./gradlew :poker-server:ktlintCheck
  - ./gradlew :poker-server:detekt
---

## Goal

`POST /api/auth/sign-out` deletes the presented session and answers `204` whether or not there was
one to delete.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/http/AuthRoutes.kt` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/http/AuthRouteTest.kt` | modify |

Read `docs/adr/ADR-0030-a-claim-adds-a-credential-and-moves-nothing.md` §3. Nothing else.

## Scope

- Read the bearer token with the `sessionTokenOrNull()` helper `TASK-040511` already added; call
  `sessions.delete(token)` when there is one; answer `204 No Content` with an empty body **in every
  case**, including no `Authorization` header at all.
- **`204` either way is the decision, not laziness.** A `404` for an unknown token tells a caller
  which tokens exist; a `401` for a missing header makes sign-out fail exactly when a client most
  wants it to be total.
- It writes nothing to `player`, `credential`, `duel` or `duel_result`, and it **closes no socket**.
  `ADR-0030` §3: a socket opened as `Q` stays `Q` until it closes, because tearing one down would
  abandon a seat mid-duel and `ADR-0013`'s grace period would then fold it — an authentication
  operation that costs somebody a coin. Put that reason in the route's KDoc; it is the thing a
  later reader will try to "fix".

## Out of scope

- Ending every session a player holds — `ADR-0049`/`ADR-0050`, `STORY-0406`.
- The client discarding its token — `STORY-0412`.
- The document — `TASK-040516`.

## Tests

`AuthRouteTest` — new methods only.

| Test | Proves |
| --- | --- |
| `signingOutAnswersTwoHundredAndFour` | `204`, empty body, and the `AuthSessions` double recorded a `delete` of that exact token |
| `signingOutTwiceAnswersTwoHundredAndFourTwice` | the second call answers `204` and records a second `delete` — idempotent and total |
| `signingOutWithNoHeaderAnswersTwoHundredAndFour` | no `Authorization` at all is still `204`, and `delete` was recorded **zero** times |
| `signingOutDeletesOnlyThePresentedToken` | with two tokens issued, signing out with the first leaves the second resolvable through the double — **the discriminating case, because a `delete` that ignored its argument passes the first three** |

## Acceptance criteria

- [ ] All four test methods above pass
- [ ] `AuthRoutes.kt` contains no `ConnectionDirectory`, `SessionRegistry` or socket type
- [ ] Every test that was in `AuthRouteTest` before this ticket still passes, unedited
- [ ] Every command in `verify:` exits 0

## Proof

Answer `401` when the header is absent and `signingOutWithNoHeaderAnswersTwoHundredAndFour` goes
red. Have the route call `delete` unconditionally with an empty token and the same test goes red on
its call count — which is why that test counts rather than only checking the status.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.
