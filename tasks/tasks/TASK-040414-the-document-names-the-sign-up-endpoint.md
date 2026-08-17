---
schema: 2
id: TASK-040414
title: The document names the sign-up endpoint, and a test agrees with the code
type: task
status: done
parent: STORY-0404
module: poker-server
estimate: S
tier: haiku
review: light
files_touched: 2
labels: [server, auth, http, docs]
depends_on: [TASK-040413]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.http.HttpEndpointDocumentationTest'
  - ./gradlew :poker-server:check -PrequireDocker=true
---

## Goal

`docs/protocol.md` documents `POST /api/auth/sign-up` — its authentication, its two body fields and
all six of its answers — and a test reflects over the DTO so the document cannot drift from the code.

## Files

| File | Action |
| --- | --- |
| `docs/protocol.md` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/http/HttpEndpointDocumentationTest.kt` | modify |
| `poker-server/src/main/kotlin/duels/poker/server/protocol/http/AuthDtos.kt` | read |

## Scope

- A new `### Sign up` section, written in the shape `### Set display name` already uses: method and
  path, authentication, a request-body field table, and a response table with a row per status.
- **Insert it immediately after the `## HTTP endpoints` introduction and before
  `### Profile endpoint`.** This is not a style choice. `HttpEndpointDocumentationTest` slices the
  document with `sectionBetween(start, end)` where `end` is the *following* heading —
  `### Set display name` bounds the profile section, and `## Protocol Errors` bounds the duel
  summary section — so a section inserted anywhere else silently changes what an existing test
  reads. Say so in a comment beside the new tests, or somebody will tidy the ordering later.
- The response table, from `ADR-0048` §6:

  | Status | Meaning |
  | --- | --- |
  | `201 Created` | one `credential` row now points at the profile this request resolved to; **no session is issued** and the client signs in afterwards |
  | `400 Bad Request` | the body could not be decoded, or the handle fails the fold |
  | `401 Unauthorized` | no resolvable identity; nothing is written, and sign-up creates no profile |
  | `409 Conflict` | the handle is taken, **or** this player already holds a `password` credential |
  | `422 Unprocessable Entity` | the password is under 8 or over 128 code points |

  Every body is empty. State the handle rule (`ADR-0031` §1: 3–32 of `[a-z0-9._-]`, first character
  `[a-z0-9]`, stored folded) and the password rule (`ADR-0048` §1: 8–128 code points of the NFC
  form, nothing trimmed, every code point permitted, and no other rule) in the prose beside the
  table.
- Two sentences that exist to stop a false inference: **no address field exists on this endpoint**
  (`ADR-0031` §5 — the recovery email is its own endpoint and costs the current password), and the
  body carries no player id, because the server resolves identity and a client may not assert it.

## Out of scope

- Documenting sign-in, sign-out or the session. `STORY-0405` writes those rows.
- Any rate-limit or `429` row. `ADR-0055` answers `DEC-048` and states one, but `STORY-0405` builds
  it; documenting a route this server does not yet enforce would make the document lie.
- Changing the three existing `sectionBetween` slices. The insertion point is chosen so that none of
  them moves.

## Tests

`HttpEndpointDocumentationTest`, adding a `signUpSection` slice —
`sectionBetween("### Sign up", "### Profile endpoint")` — and three tests.

| Test | Proves |
| --- | --- |
| `theDocumentDescribesTheSignUpEndpoint` | the document contains `POST /api/auth/sign-up` |
| `theSignUpSectionNamesEveryFieldTheRequestHas` | reflects over `SignUpRequest::class.memberProperties`, asserts the set is **non-empty**, and asserts every property name appears in the section's field table. **The wrong document this must fail against is one naming only `handle`** |
| `theSignUpSectionNamesEveryStatusTheRouteCanAnswer` | a `val` list of `"201"`, `"400"`, `"401"`, `"409"`, `"422"`; asserts the list is non-empty and that each appears in the section |

## Acceptance criteria

- [ ] All three tests above pass
- [ ] The existing tests in `HttpEndpointDocumentationTest` pass **unchanged**, and the three
      existing `sectionBetween` calls are byte-identical — the new section is inserted where none of
      them is delimited
- [ ] `theSignUpSectionNamesEveryFieldTheRequestHas` asserts the reflected property set is non-empty
      before iterating it
- [ ] The document states both field rules and says every response body is empty
- [ ] The document says no address field and no player id exists on this endpoint
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
