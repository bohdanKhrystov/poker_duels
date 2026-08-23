---
schema: 2
id: TASK-040516
title: The document names sign-in and sign-out, and the test that reads it keeps its bearings
type: task
status: backlog
parent: STORY-0405
module: poker-server
estimate: S
tier: sonnet
review: standard
files_touched: 2
labels: [server, docs, http, auth]
depends_on: [TASK-040515]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.http.HttpEndpointDocumentationTest'
  - ./gradlew :poker-server:test --tests 'duels.poker.server.protocol.ProtocolDocumentationTest'
---

## Goal

`docs/protocol.md` contracts the two new endpoints, and the test that reads that document section by
section still knows where each section starts and stops.

## Files

| File | Action |
| --- | --- |
| `docs/protocol.md` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/http/HttpEndpointDocumentationTest.kt` | modify |

## Scope

- Two new sections, `### Sign in` and `### Sign out`, placed **immediately after `### Sign up`** so
  the auth endpoints read together.
- **`HttpEndpointDocumentationTest` must move with them, and this is the trap.** That test builds
  each section with `sectionBetween(start, end)`, where every `end` is the heading that *follows* —
  `signUpSection` is `sectionBetween("### Sign up", "### Profile endpoint")`. Inserting a heading
  between those two silently shrinks `signUpSection` to nothing, and `sectionBetween` `require`s its
  end marker, so the failure is loud. Re-chain the markers: sign-up ends at `### Sign in`, sign-in
  ends at `### Sign out`, sign-out ends at `### Profile endpoint`. **Only the marker strings
  change; no existing assertion about sign-up moves.**
- Sign-in's section states: `POST /api/auth/sign-in`, no authentication of any kind, a body of
  `handle` and `password`, `200` with `sessionToken`, `401` with an empty body for **both** a wrong
  password and an unknown handle *"with no way to tell them apart"*, and `400` for a body that does
  not decode. It says in one line that the token is returned here and never again.
- Sign-out's section states: `POST /api/auth/sign-out`, `Authorization: Bearer <token>`, no body,
  `204` whether or not a session was deleted, and that live sockets are not closed.
- The `Authentication` line of `### Profile endpoint`, `### Set display name` and
  `### Recent duels endpoint` gains one sentence: a valid `Authorization: Bearer <token>` outranks
  `X-Device-Id`, and an invalid, expired or unknown token answers `401` rather than falling back.

## Out of scope

- `## Protocol Errors` and the `INVALID_SESSION` bullet — `TASK-040502` already added it.
- The `429` row — `TASK-040522`.
- Any code under `src/main`.

## Tests

`HttpEndpointDocumentationTest` — the six existing section constants are re-chained, and new
methods are added in the shape the file already uses.

| Test | Proves |
| --- | --- |
| `theSignUpSectionIsStillWhereItWas` | `signUpSection` still contains `POST /api/auth/sign-up` and its `201` row — the re-chaining did not empty it |
| `theSignInSectionNamesItsMethodAndPath` | `signInSection` contains `POST /api/auth/sign-in` |
| `theSignInSectionNamesBothRequestFields` | it names `handle` and `password` |
| `theSignInSectionSaysTheTwoFailuresAreIndistinguishable` | it names `401` and says the wrong-password and unknown-handle answers cannot be told apart |
| `theSignOutSectionSaysTwoHundredAndFourEitherWay` | `signOutSection` contains `204` and says it answers so whether or not a session was deleted |
| `theSignOutSectionSaysNoSocketIsClosed` | it says live sockets are not closed |
| `everyAuthenticatedSectionNamesTheBearerHeader` | the profile, set-name and recent-duels sections each contain `Authorization: Bearer` |

## Acceptance criteria

- [ ] All seven test methods above pass
- [ ] Every test that was in `HttpEndpointDocumentationTest` before this ticket still passes, and
      the only edit to an existing line is a `sectionBetween` marker string
- [ ] `ProtocolDocumentationTest` passes
- [ ] Every command in `verify:` exits 0

## Proof

Add the two sections to `docs/protocol.md` **without** touching the test and
`theSignUpSectionIsStillWhereItWas` — or, before it exists, several of the merged sign-up
assertions — go red immediately. That failure is the reason this ticket names the test file at all.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.
